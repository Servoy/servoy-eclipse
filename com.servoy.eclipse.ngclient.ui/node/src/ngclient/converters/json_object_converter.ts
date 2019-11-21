import { IConverter, ConverterService } from '../../sablo/converter.service'

import { BaseCustomObject, IChangeAwareValue } from '../../sablo/spectypes.service'

import {SpecTypesService, instanceOfChangeAwareValue} from '../../sablo/spectypes.service';

export class JSONObjectConverter implements IConverter {
    private static readonly UPDATES = "u";
    private static readonly KEY = "k";
    private static readonly INITIALIZE = "in";
    private static readonly VALUE = "v";
    private static readonly PUSH_TO_SERVER = "w"; // value is undefined when we shouldn't send changes to server, false if it should be shallow watched and true if it should be deep watched
    private static readonly CONTENT_VERSION = "vEr"; // server side sync to make sure we don't end up granular updating something that has changed meanwhile server-side
    private static readonly NO_OP = "n";
    private static readonly REAL_TYPE = "rt";

    constructor( private converterService: ConverterService, private specTypesService: SpecTypesService ) {
    }

    fromServerToClient( serverJSONValue, currentClientValue?: BaseCustomObject, propertyContext?:(propertyName: string)=>any) {
        let newValue = currentClientValue;
        // remove old watches (and, at the end create new ones) to avoid old watches getting triggered by server side change
        // TODO  removeAllWatches(currentClientValue);
        if ( serverJSONValue && serverJSONValue[JSONObjectConverter.VALUE] ) {
            // full contents
            newValue = serverJSONValue[JSONObjectConverter.VALUE];
            let customObjectPropertyContext = this.getCustomObjectPropertyContext(newValue, propertyContext);
            const clientObject = this.specTypesService.createType( serverJSONValue[JSONObjectConverter.REAL_TYPE] );
            const internalState = clientObject.getStateHolder();
            internalState.ignoreChanges = true;
            if ( typeof serverJSONValue[JSONObjectConverter.PUSH_TO_SERVER] !== 'undefined' ) internalState[JSONObjectConverter.PUSH_TO_SERVER] = serverJSONValue[JSONObjectConverter.PUSH_TO_SERVER];
            internalState[JSONObjectConverter.CONTENT_VERSION] = serverJSONValue[JSONObjectConverter.CONTENT_VERSION];

            for ( var c in newValue ) {
                let elem = newValue[c];
                let conversionInfo = null;
                if ( serverJSONValue[ConverterService.TYPES_KEY] ) {
                    conversionInfo = serverJSONValue[ConverterService.TYPES_KEY][c];
                }

                if ( conversionInfo ) {
                    internalState.conversionInfo[c] = conversionInfo;
                    newValue[c] = elem = this.converterService.convertFromServerToClient( elem, conversionInfo, currentClientValue ? currentClientValue[c] : undefined, customObjectPropertyContext);
                }
                clientObject[c] = elem;
            }
            internalState.ignoreChanges = false;
            internalState.clearChanges();
            newValue = clientObject;
        } else if ( serverJSONValue && serverJSONValue[JSONObjectConverter.UPDATES] ) {
            // granular updates received;

            const internalState = currentClientValue.getStateHolder();
            let customObjectPropertyContext = this.getCustomObjectPropertyContext(currentClientValue, propertyContext);

            // this can happen when an object value was set completely in browser and the child values need to instrument their browser values as well in which case the server sends 'initialize' updates for both this array and 'smart' child values
            if ( serverJSONValue[JSONObjectConverter.INITIALIZE] ) internalState[JSONObjectConverter.CONTENT_VERSION] = serverJSONValue[JSONObjectConverter.CONTENT_VERSION];

            // if something changed browser-side, increasing the content version thus not matching next expected version,
            // we ignore this update and expect a fresh full copy of the object from the server (currently server value is leading/has priority because not all server side values might support being recreated from client values)
            if ( internalState[JSONObjectConverter.CONTENT_VERSION] == serverJSONValue[JSONObjectConverter.CONTENT_VERSION] ) {
                internalState.ignoreChanges = true;
                let updates = serverJSONValue[JSONObjectConverter.UPDATES];
                let conversionInfos = serverJSONValue[ConverterService.TYPES_KEY];
                let i;
                for ( i in updates ) {
                    const update = updates[i];
                    const key = update[JSONObjectConverter.KEY];
                    let val = update[JSONObjectConverter.VALUE];

                    let conversionInfo = null;
                    if ( conversionInfos && conversionInfos[i] && conversionInfos[i][JSONObjectConverter.VALUE] ) {
                        conversionInfo = conversionInfos[i][JSONObjectConverter.VALUE];
                    }

                    if ( conversionInfo ) {
                        internalState.conversionInfo[key] = conversionInfo;
                        currentClientValue[key] = val = this.converterService.convertFromServerToClient( val, conversionInfo, currentClientValue[key], customObjectPropertyContext );
                    } else currentClientValue[key] = val;
                }
                internalState.ignoreChanges = false;
                internalState.clearChanges();
            }
            //else {
            // else we got an update from server for a version that was already bumped by changes in browser; ignore that, as browser changes were sent to server
            // and server will detect the problem and send back a full update
            //}
        } else if ( serverJSONValue && serverJSONValue[JSONObjectConverter.INITIALIZE] ) {
            // only content version update - this happens when a full object value is set on this property client side; it goes to server
            // and then server sends back the version
            newValue.getStateHolder()[JSONObjectConverter.CONTENT_VERSION] = serverJSONValue[JSONObjectConverter.CONTENT_VERSION];
            // here we can count on not having any 'smart' values cause if we had
            // updates would have been received with this initialize as well (to initialize child elements as well to have the setChangeNotifier and internal things)
        } else if ( !serverJSONValue || !serverJSONValue[JSONObjectConverter.NO_OP] ) newValue = null; // anything else would not be supported... // TODO how to handle null values (special watches/complete object set from client)? if null is on server and something is set on client or the other way around?

        return newValue;
    }

    fromClientToServer( newClientData: BaseCustomObject, oldClientData?: BaseCustomObject ) {

        // TODO how to handle null values (special watches/complete array set from client)? if null is on server and something is set on client or the other way around?

        if ( newClientData ) {
            const internalState = newClientData.getStateHolder();
            if ( internalState.getChangedKeys().length > 0 || internalState.allChanged ) {
                var changes = {};

                let noContentVersionYet = false; // if we have changed keys on an object that was fully set from client but it didn't get a CONTENT_VERSION back from server
                // (it's probably pending) we have to send a full value again to server beacuse it will be treated as such anyway

                if ( internalState[JSONObjectConverter.CONTENT_VERSION] ) changes[JSONObjectConverter.CONTENT_VERSION] = internalState[JSONObjectConverter.CONTENT_VERSION];
                else noContentVersionYet = true;

                if ( internalState.allChanged || noContentVersionYet ) {
                    // structure might have changed; increase version number
                    if ( !noContentVersionYet )++internalState[JSONObjectConverter.CONTENT_VERSION]; // we also increase the content version number - server should only be expecting updates for the next version number
                    // send all
                    var toBeSentObj = changes[JSONObjectConverter.VALUE] = {};
                    let specProperties = this.specTypesService.getProperties( newClientData.constructor );
                    if ( !specProperties ) specProperties = Object.keys( newClientData );
                    specProperties.forEach(( key ) => {
                        var val = newClientData[key];

                        if ( internalState.conversionInfo[key] ) {
                            if (instanceOfChangeAwareValue(val)) val.getStateHolder().markAllChanged(false);
                            toBeSentObj[key] = this.converterService.convertFromClientToServer( val, internalState.conversionInfo[key], oldClientData ? oldClientData[key] : undefined );
                        } else {
                            // no conversion info, but this could still be a JSON_obj or JSON_array ....
                            const guessedType: string = this.specTypesService.guessType( val );
                            if ( guessedType != null ) {
                                if (instanceOfChangeAwareValue(val)) val.getStateHolder().markAllChanged(false);
                                toBeSentObj[key] = this.converterService.convertFromClientToServer( val, guessedType, oldClientData ? oldClientData[key] : undefined );
                            }
                            else toBeSentObj[key] = this.converterService.convertClientObject( val ); // default conversion
                        }
                    } );
                    internalState.clearChanges();
                    return changes;
                } else {
                    // send only changed keys
                    var changedElements = changes[JSONObjectConverter.UPDATES] = [];
                    internalState.getChangedKeys().forEach(( key ) => {
                        var newVal = newClientData[key];
                        var oldVal = oldClientData ? oldClientData[key] : undefined;
                        var ch = {};
                        ch[JSONObjectConverter.KEY] = key;

                        if ( internalState.conversionInfo[key] ) ch[JSONObjectConverter.VALUE] = this.converterService.convertFromClientToServer( newVal, internalState.conversionInfo[key], oldVal );
                        else {
                            // no conversion info, but this could still be a JSON_obj or JSON_array ....
                            const guessedType: string = this.specTypesService.guessType( newVal );
                        
                            if ( guessedType != null ) ch[JSONObjectConverter.VALUE] = this.converterService.convertFromClientToServer( newVal, guessedType, oldVal );
                            else ch[JSONObjectConverter.VALUE] = this.converterService.convertClientObject( newVal ); // default conversion
                        }

                        changedElements.push( ch );
                    } )
                    internalState.clearChanges();
                    return changes;
                }
            } else if ( newClientData == oldClientData ) {
                // TODO angular equals?? if (angular.equals(newClientData, oldClientData)) { // can't use === because oldClientData is an angular clone not the same ref.
                var x = {}; // no changes
                x[JSONObjectConverter.NO_OP] = true;
                return x;
            }
        }
        
        // except if newClientData is undefined/null we should never reach this code in case of property type-script usage; some object that is of the wrong type was set to this property;
        // it would probably work if it's some JSON who's structure matches what is expected by server but most of the time this happens due to unintended ERRORS
        return newClientData;

    }
    
    getCustomObjectPropertyContext(currentClientValue: BaseCustomObject, parentPropertyContext:(propertyName: string)=>any) : any {
        // property context that we pass here should search first in the current custom object value and only fallback to "parentPropertyContext" if needed
        return propertyName => {
            if (currentClientValue.hasOwnProperty(propertyName)) return currentClientValue[propertyName]; // can even be null or undefined as long as it is set on this object
            else return parentPropertyContext ? parentPropertyContext(propertyName) : undefined; // fall back to parent object nesting context
        }
    }
}