import { IConverter, ConverterService } from '../../sablo/converter.service'

export class JSONObjectConverter implements IConverter {
    private static readonly UPDATES = "u";
    private static readonly KEY = "k";
    private static readonly INITIALIZE = "in";
    private static readonly VALUE = "v";
    private static readonly PUSH_TO_SERVER = "w"; // value is undefined when we shouldn't send changes to server, false if it should be shallow watched and true if it should be deep watched
    private static readonly CONTENT_VERSION = "vEr"; // server side sync to make sure we don't end up granular updating something that has changed meanwhile server-side
    private static readonly NO_OP = "n";

    constructor( private converterService: ConverterService ) {
    }

    fromServerToClient( serverJSONValue, currentClientValue?, componentScope?, componentModelGetter?) {
        var newValue = currentClientValue;
        // remove old watches (and, at the end create new ones) to avoid old watches getting triggered by server side change
        // TODO  removeAllWatches(currentClientValue);
        if ( serverJSONValue && serverJSONValue[JSONObjectConverter.VALUE] ) {
            // full contents
            newValue = serverJSONValue[JSONObjectConverter.VALUE];
           this. initializeNewValue( newValue, serverJSONValue[JSONObjectConverter.CONTENT_VERSION] );
            var internalState = newValue[ConverterService.INTERNAL_IMPL];
            if ( typeof serverJSONValue[JSONObjectConverter.PUSH_TO_SERVER] !== 'undefined' ) internalState[JSONObjectConverter.PUSH_TO_SERVER] = serverJSONValue[JSONObjectConverter.PUSH_TO_SERVER];

            for ( var c in newValue ) {
                var elem = newValue[c];
                var conversionInfo = null;
                if ( serverJSONValue[ConverterService.TYPES_KEY] ) {
                    conversionInfo = serverJSONValue[ConverterService.TYPES_KEY][c];
                }

                if ( conversionInfo ) {
                    internalState.conversionInfo[c] = conversionInfo;
                    newValue[c] = elem = this.converterService.convertFromServerToClient( elem, conversionInfo, currentClientValue ? currentClientValue[c] : undefined, componentScope, componentModelGetter );
                }

                if ( elem && elem[ConverterService.INTERNAL_IMPL] && elem[ConverterService.INTERNAL_IMPL].setChangeNotifier ) {
                    // child is able to handle it's own change mechanism
                    elem[ConverterService.INTERNAL_IMPL].setChangeNotifier( this.getChangeNotifier( newValue, c ) );
                }
            }
        } else if ( serverJSONValue && serverJSONValue[JSONObjectConverter.UPDATES] ) {
            // granular updates received;

            if ( serverJSONValue[JSONObjectConverter.INITIALIZE] ) this.initializeNewValue( currentClientValue, serverJSONValue[JSONObjectConverter.CONTENT_VERSION] ); // this can happen when an object value was set completely in browser and the child values need to instrument their browser values as well in which case the server sends 'initialize' updates for both this array and 'smart' child values

            var internalState = currentClientValue[ConverterService.INTERNAL_IMPL];

            // if something changed browser-side, increasing the content version thus not matching next expected version,
            // we ignore this update and expect a fresh full copy of the object from the server (currently server value is leading/has priority because not all server side values might support being recreated from client values)
            if ( internalState[JSONObjectConverter.CONTENT_VERSION] == serverJSONValue[JSONObjectConverter.CONTENT_VERSION] ) {
                var updates = serverJSONValue[JSONObjectConverter.UPDATES];
                var conversionInfos = serverJSONValue[ConverterService.TYPES_KEY];
                var i;
                for ( i in updates ) {
                    var update = updates[i];
                    var key = update[JSONObjectConverter.KEY];
                    var val = update[JSONObjectConverter.VALUE];

                    var conversionInfo = null;
                    if ( conversionInfos && conversionInfos[i] && conversionInfos[i][JSONObjectConverter.VALUE] ) {
                        conversionInfo = conversionInfos[i][JSONObjectConverter.VALUE];
                    }

                    if ( conversionInfo ) {
                        internalState.conversionInfo[key] = conversionInfo;
                        currentClientValue[key] = val = this.converterService.convertFromServerToClient( val, conversionInfo, currentClientValue[key], componentScope, componentModelGetter );
                    } else currentClientValue[key] = val;

                    if ( val && val[ConverterService.INTERNAL_IMPL] && val[ConverterService.INTERNAL_IMPL].setChangeNotifier ) {
                        // child is able to handle it's own change mechanism
                        val[ConverterService.INTERNAL_IMPL].setChangeNotifier( this.getChangeNotifier( currentClientValue, key ) );
                    }
                }
            }
            //else {
            // else we got an update from server for a version that was already bumped by changes in browser; ignore that, as browser changes were sent to server
            // and server will detect the problem and send back a full update
            //}
        } else if ( serverJSONValue && serverJSONValue[JSONObjectConverter.INITIALIZE] ) {
            // only content version update - this happens when a full object value is set on this property client side; it goes to server
            // and then server sends back the version and we initialize / prepare the existing newValue for being watched/handle child conversions
            this.initializeNewValue( currentClientValue, serverJSONValue[JSONObjectConverter.CONTENT_VERSION] ); // here we can count on not having any 'smart' values cause if we had
            // updates would have been received with this initialize as well (to initialize child elements as well to have the setChangeNotifier and internal things)
        } else if ( !serverJSONValue || !serverJSONValue[JSONObjectConverter.NO_OP] ) newValue = null; // anything else would not be supported... // TODO how to handle null values (special watches/complete object set from client)? if null is on server and something is set on client or the other way around?

        return newValue;
    }

    fromClientToServer( newClientData, oldClientData?) {

        // TODO how to handle null values (special watches/complete array set from client)? if null is on server and something is set on client or the other way around?

        var internalState;
        if ( newClientData && ( internalState = newClientData[ConverterService.INTERNAL_IMPL] ) ) {
            if ( internalState.isChanged() ) {
                var changes = {};
                changes[JSONObjectConverter.CONTENT_VERSION] = internalState[JSONObjectConverter.CONTENT_VERSION];
                if ( internalState.allChanged ) {
                    // structure might have changed; increase version number
                    ++internalState[JSONObjectConverter.CONTENT_VERSION]; // we also increase the content version number - server should only be expecting updates for the next version number
                    // send all
                    var toBeSentObj = changes[JSONObjectConverter.VALUE] = {};
                    for ( var key in newClientData ) {
                        var val = newClientData[key];
                        if ( internalState.conversionInfo[key] ) toBeSentObj[key] = this.converterService.convertFromClientToServer( val, internalState.conversionInfo[key], oldClientData ? oldClientData[key] : undefined );
                        else toBeSentObj[key] = this.converterService.convertClientObject( val );
                    }
                    internalState.allChanged = false;
                    internalState.changedKeys = {};
                    return changes;
                } else {
                    // send only changed keys
                    var changedElements = changes[JSONObjectConverter.UPDATES] = [];
                    for ( var key in internalState.changedKeys ) {
                        var newVal = newClientData[key];
                        var oldVal = oldClientData ? oldClientData[key] : undefined;

                        var changed = ( newVal !== oldVal );
                        if ( !changed ) {
                            if ( internalState.elUnwatch[key] ) {
                                var oldDumbVal = internalState.changedKeys[key].old;
                                // it's a dumb value - watched; see if it really changed acording to sablo rules
                                if ( oldDumbVal !== newVal ) {
                                    if ( typeof newVal == "object" ) {
                                        if ( this.converterService.isChanged( newVal, oldDumbVal, internalState.conversionInfo[key] ) ) {
                                            changed = true;
                                        }
                                    } else {
                                        changed = true;
                                    }
                                }
                            } else changed = newVal && newVal[ConverterService.INTERNAL_IMPL].isChanged(); // must be smart value then; same reference as checked above; so ask it if it changed
                        }

                        if ( changed ) {
                            var ch = {};
                            ch[JSONObjectConverter.KEY] = key;

                            if ( internalState.conversionInfo[key] ) ch[JSONObjectConverter.VALUE] = this.converterService.convertFromClientToServer( newVal, internalState.conversionInfo[key], oldVal );
                            else ch[JSONObjectConverter.VALUE] = this.converterService.convertClientObject( newVal );

                            changedElements.push( ch );
                        }
                    }
                    internalState.allChanged = false;
                    internalState.changedKeys = {};
                    return changes;
                }
            } else if ( newClientData == oldClientData ) {
                // TODO angular equals?? if (angular.equals(newClientData, oldClientData)) { // can't use === because oldClientData is an angular clone not the same ref.
                var x = {}; // no changes
                x[JSONObjectConverter.NO_OP] = true;
                return x;
            }
        }

        if ( internalState ) delete newClientData[ConverterService.INTERNAL_IMPL]; // some other new value was set; it's internal state is useless and will be re-initialized from server

        return newClientData;

    }
    
    /** Initializes internal state on a new object value */
    private initializeNewValue(newValue, contentVersion) {
        var newInternalState = false; // TODO although unexpected (internal state to already be defined at this stage it can happen until SVY-8612 is implemented and property types change to use that
        if (!newValue.hasOwnProperty(ConverterService.INTERNAL_IMPL)) {
            newInternalState = true;
            this.converterService.prepareInternalState(newValue);
        } // else: we don't try to redefine internal state if it's already defined

        var internalState = newValue[ConverterService.INTERNAL_IMPL];
        internalState[JSONObjectConverter.CONTENT_VERSION] = contentVersion; // being full content updates, we don't care about the version, we just accept it

        if (newInternalState) {
            // implement what $sabloConverters need to make this work
            internalState.setChangeNotifier = function(changeNotifier) {
                internalState.notifier = changeNotifier; 
            }
            internalState.isChanged = function() {
                var hasChanges = internalState.allChanged;
                if (!hasChanges) for (var x in internalState.changedKeys) { hasChanges = true; break; }
                return hasChanges;
            }

            // private impl
            internalState.modelUnwatch = [];
            internalState.objStructureUnwatch = null;
            internalState.conversionInfo = {};
            internalState.changedKeys = {};
            internalState.allChanged = false;
        } // else don't reinitilize it - it's already initialized
    }
    
    private getChangeNotifier(propertyValue, key) {
        return ()=> {
            var internalState = propertyValue[ConverterService.INTERNAL_IMPL];
            internalState.changedKeys[key] = true;
            internalState.notifier();
        }
    }

}