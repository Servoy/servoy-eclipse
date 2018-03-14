import { IConverter, ConverterService } from '../../sablo/converter.service'

import { IterableDiffers, IterableDiffer } from '@angular/core';

import { SpecTypesService, ArrayState, ICustomArray, instanceOfChangeAwareValue, instanceOfCustomArray } from '../../sablo/spectypes.service'

export class JSONArrayConverter implements IConverter {
    private static readonly UPDATES = "u";
    private static readonly REMOVES = "r";
    private static readonly ADDITIONS = "a";
    private static readonly INDEX = "i";
    private static readonly INITIALIZE = "in";
    private static readonly VALUE = "v";
    private static readonly PUSH_TO_SERVER = "w"; // value is undefined when we shouldn't send changes to server, false if it should be shallow watched and true if it should be deep watched
    private static readonly CONTENT_VERSION = "vEr"; // server side sync to make sure we don't end up granular updating something that has changed meanwhile server-side
    private static readonly NO_OP = "n";
    
    constructor( private converterService: ConverterService, private specTypesService:SpecTypesService, private iterableDiffers: IterableDiffers ) {
    }

    fromServerToClient( serverJSONValue, currentClientValue?:ICustomArray<any>, componentScope?, componentModelGetter?) {
        let newValue = currentClientValue;
        let  state:ArrayState = null;
        // remove old watches (and, at the end create new ones) to avoid old watches getting triggered by server side change
        // TODO  removeAllWatches(currentClientValue);
        try {
            if ( serverJSONValue && serverJSONValue[JSONArrayConverter.VALUE] ) {
                // full contents
                newValue = serverJSONValue[JSONArrayConverter.VALUE];
                const newValueCA = this.specTypesService.enhanceArrayType( newValue, this.iterableDiffers );
                state = newValueCA.getStateHolder();
                state[JSONArrayConverter.CONTENT_VERSION] = serverJSONValue[JSONArrayConverter.CONTENT_VERSION];
                if ( typeof serverJSONValue[JSONArrayConverter.PUSH_TO_SERVER] !== 'undefined' ) state[JSONArrayConverter.PUSH_TO_SERVER] = serverJSONValue[JSONArrayConverter.PUSH_TO_SERVER];

                if ( newValue.length ) {
                    for ( var c = 0; c < newValue.length; c++ ) {
                        let  elem = newValue[c];
                        let conversionInfo = null;
                        if ( serverJSONValue[ConverterService.TYPES_KEY] ) {
                            conversionInfo = serverJSONValue[ConverterService.TYPES_KEY][c];
                        }

                        if ( conversionInfo ) {
                            state.conversionInfo[c] = conversionInfo;
                            newValue[c] = elem = this.converterService.convertFromServerToClient( elem, conversionInfo, currentClientValue ? currentClientValue[c] : undefined, componentScope, componentModelGetter );
                        }

                        if ( instanceOfChangeAwareValue( elem ) ) {
                            // child is able to handle it's own change mechanism
                            elem.getStateHolder().setChangeListener(() => {
                                state.notifyChangeListener();
                            } );
                        }
                    }
                }
            } else if ( serverJSONValue && ( serverJSONValue[JSONArrayConverter.UPDATES] || serverJSONValue[JSONArrayConverter.REMOVES] || serverJSONValue[JSONArrayConverter.ADDITIONS] ) ) {
                // granular updates received;

                state = currentClientValue.getStateHolder();

                if ( serverJSONValue[JSONArrayConverter.INITIALIZE] )
                    state[JSONArrayConverter.CONTENT_VERSION] = serverJSONValue[JSONArrayConverter.CONTENT_VERSION];
                
                // if something changed browser-side, increasing the content version thus not matching next expected version,
                // we ignore this update and expect a fresh full copy of the array from the server (currently server value is leading/has priority because not all server side values might support being recreated from client values)
                if ( state[JSONArrayConverter.CONTENT_VERSION] == serverJSONValue[JSONArrayConverter.CONTENT_VERSION] ) {
                    if ( serverJSONValue[JSONArrayConverter.REMOVES] ) {
                        const removes = serverJSONValue[JSONArrayConverter.REMOVES];
                        for ( var idx in removes ) {
                            currentClientValue.splice( removes[idx], 1 );
                        }
                    }
                    if ( serverJSONValue[JSONArrayConverter.ADDITIONS] ) {
                        const additions = serverJSONValue[JSONArrayConverter.ADDITIONS];
                        const conversionInfos = serverJSONValue[ConverterService.TYPES_KEY];
                        for ( let i in additions ) {
                            const element = additions[i];
                            const idx = element[JSONArrayConverter.INDEX];
                            let val = element[JSONArrayConverter.VALUE];

                            let conversionInfo = null;
                            if ( conversionInfos && conversionInfos[i] && conversionInfos[i][JSONArrayConverter.VALUE] ) {
                                conversionInfo = conversionInfos[i][JSONArrayConverter.VALUE];
                            }

                            if ( conversionInfo ) {
                                state.conversionInfo[idx] = conversionInfo;
                                val = this.converterService.convertFromServerToClient( val, conversionInfo, currentClientValue[idx], componentScope, componentModelGetter );
                            }
                            currentClientValue.splice( idx, 0, val );

                            if ( instanceOfChangeAwareValue( val ) ) {
                                // child is able to handle it's own change mechanism
                                val.getStateHolder().setChangeListener(() => {
                                    state.notifyChangeListener();
                                } );
                            }
                        }
                    }
                    if ( serverJSONValue[JSONArrayConverter.UPDATES] ) {
                        const updates = serverJSONValue[JSONArrayConverter.UPDATES];
                        const conversionInfos = serverJSONValue[ConverterService.TYPES_KEY];
                        for ( let i in updates ) {
                            const update = updates[i];
                            const idx = update[JSONArrayConverter.INDEX];
                            let val = update[JSONArrayConverter.VALUE];

                            let conversionInfo = null;
                            if ( conversionInfos && conversionInfos[i] && conversionInfos[i][JSONArrayConverter.VALUE] ) {
                                conversionInfo = conversionInfos[i][JSONArrayConverter.VALUE];
                            }

                            if ( conversionInfo ) {
                                state.conversionInfo[idx] = conversionInfo;
                                currentClientValue[idx] = val = this.converterService.convertFromServerToClient( val, conversionInfo, currentClientValue[idx], componentScope, componentModelGetter );
                            } else currentClientValue[idx] = val;

                            if ( instanceOfChangeAwareValue( val ) ) {
                                // child is able to handle it's own change mechanism
                                val.getStateHolder().setChangeListener(() => {
                                    state.notifyChangeListener();
                                } );
                            }
                        }
                    }
                }
                //else {
                // else we got an update from server for a version that was already bumped by changes in browser; ignore that, as browser changes were sent to server
                // and server will detect the problem and send back a full update
                //}
            } else if ( serverJSONValue && serverJSONValue[JSONArrayConverter.INITIALIZE] ) {
                // only content version update - this happens when a full array value is set on this property client side; it goes to server
                // and then server sends back the version and we initialize / prepare the existing newValue for being watched/handle child conversions
                state = currentClientValue.getStateHolder();
                state[JSONArrayConverter.CONTENT_VERSION] = serverJSONValue[JSONArrayConverter.CONTENT_VERSION];// here we can count on not having any 'smart' values cause if we had
                // updates would have been received with this initialize as well (to initialize child elements as well to have the setChangeNotifier and internal things)
            } else if ( !serverJSONValue || !serverJSONValue[JSONArrayConverter.NO_OP] ) newValue = null; // anything else would not be supported... // TODO how to handle null values (special watches/complete array set from client)? if null is on server and something is set on client or the other way around?
        } finally {
            state.clearChanges();
        }

        return newValue;
    }

    fromClientToServer( newClientData:ICustomArray<any>, oldClientData?) {

        // test if this was an array created fully on the client.
        if (!instanceOfCustomArray(newClientData) ) {
            this.specTypesService.enhanceArrayType(newClientData, this.iterableDiffers);
        }
        let internalState: ArrayState
        if ( newClientData && ( internalState = newClientData.getStateHolder()) ) {
            let arrayChanges = internalState.getChangedKeys();
            if ( arrayChanges.length > 0 || internalState.allChanged) {
                const changes = {};
            
                if (internalState[JSONArrayConverter.CONTENT_VERSION]) changes[JSONArrayConverter.CONTENT_VERSION] = internalState[JSONArrayConverter.CONTENT_VERSION];
                if ( internalState.allChanged) {
                    // structure might have changed; increase version number
                    ++internalState[JSONArrayConverter.CONTENT_VERSION]; // we also increase the content version number - server should only be expecting updates for the next version number
                    // send all
                    var toBeSentArray = changes[JSONArrayConverter.VALUE] = [];
                    for ( let idx = 0; idx < newClientData.length; idx++ ) {
                        const val = newClientData[idx];
                        if ( instanceOfChangeAwareValue( val ) ) {
                            val.getStateHolder().markAllChanged(false);
                        }
                        toBeSentArray[idx] = this.convert(val, internalState.conversionInfo[idx]);
                    }
                } else {
                    // send only changed indexes
                    var changedElements = changes[JSONArrayConverter.UPDATES] = [];
                    arrayChanges.forEach((idx) => {
                        var newVal = newClientData[idx];
                        var ch = {};
                        ch[JSONArrayConverter.INDEX] = idx;
                        ch[JSONArrayConverter.VALUE] = this.convert(newVal, internalState.conversionInfo[idx]);
                        changedElements.push( ch );
                    })
                }
                internalState.clearChanges();
                return changes;
            } else if ( newClientData == oldClientData ) { // TODO do we need to compare differently?
                //if (angular.equals(newClientData, oldClientData)) {
                var x = {}; // no changes
                x[JSONArrayConverter.NO_OP] = true;
                return x;
            }
        }

        return newClientData;
    }
    
    private convert(newVal, conversionInfo) {
        if (!conversionInfo) {
            conversionInfo = this.specTypesService.guessType(newVal);
        }
        if ( conversionInfo) return  this.converterService.convertFromClientToServer( newVal, conversionInfo );
        return this.converterService.convertClientObject( newVal );
    }
 
}
