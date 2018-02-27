import { IConverter, ConverterService } from '../../sablo/converter.service'

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

    constructor( private converterService: ConverterService ) {
    }

    fromServerToClient( serverJSONValue, currentClientValue?, componentScope?, componentModelGetter?) {
        var newValue = currentClientValue;
     // remove old watches (and, at the end create new ones) to avoid old watches getting triggered by server side change
      // TODO  removeAllWatches(currentClientValue);
        try
        {
            if (serverJSONValue && serverJSONValue[JSONArrayConverter.VALUE]) {
                // full contents
                newValue = serverJSONValue[JSONArrayConverter.VALUE];
                this.initializeNewValue(newValue, serverJSONValue[JSONArrayConverter.CONTENT_VERSION]);
                var internalState = newValue[ConverterService.INTERNAL_IMPL];
                if (typeof serverJSONValue[JSONArrayConverter.PUSH_TO_SERVER] !== 'undefined') internalState[JSONArrayConverter.PUSH_TO_SERVER] = serverJSONValue[JSONArrayConverter.PUSH_TO_SERVER];

                if(newValue.length)
                {
                    for (var c = 0; c < newValue.length; c++) {
                        var elem = newValue[c];
                        var conversionInfo = null;
                        if (serverJSONValue[ConverterService.TYPES_KEY]) {
                            conversionInfo = serverJSONValue[ConverterService.TYPES_KEY][c];
                        }

                        if (conversionInfo) {
                            internalState.conversionInfo[c] = conversionInfo;
                            newValue[c] = elem = this.converterService.convertFromServerToClient(elem, conversionInfo, currentClientValue ? currentClientValue[c] : undefined, componentScope, componentModelGetter);
                        }

                        if (elem && elem[ConverterService.INTERNAL_IMPL] && elem[ConverterService.INTERNAL_IMPL].setChangeNotifier) {
                            // child is able to handle it's own change mechanism
                            elem[ConverterService.INTERNAL_IMPL].setChangeNotifier(this.getChangeNotifier(newValue, c));
                        }
                    }
                }
            } else if (serverJSONValue && (serverJSONValue[JSONArrayConverter.UPDATES] || serverJSONValue[JSONArrayConverter.REMOVES] || serverJSONValue[JSONArrayConverter.ADDITIONS])) {
                // granular updates received;

                if (serverJSONValue[JSONArrayConverter.INITIALIZE]) this.initializeNewValue(currentClientValue, serverJSONValue[JSONArrayConverter.CONTENT_VERSION]); // this can happen when an array value was set completely in browser and the child elements need to instrument their browser values as well in which case the server sends 'initialize' updates for both this array and 'smart' child elements

                var internalState = currentClientValue[ConverterService.INTERNAL_IMPL];

                // if something changed browser-side, increasing the content version thus not matching next expected version,
                // we ignore this update and expect a fresh full copy of the array from the server (currently server value is leading/has priority because not all server side values might support being recreated from client values)
                if (internalState[JSONArrayConverter.CONTENT_VERSION] == serverJSONValue[JSONArrayConverter.CONTENT_VERSION]) {
                    if (serverJSONValue[JSONArrayConverter.REMOVES])
                    {
                        var removes = serverJSONValue[JSONArrayConverter.REMOVES];
                        for (var idx in removes)
                        {
                            currentClientValue.splice(removes[idx], 1 );
                        }
                    }
                    if (serverJSONValue[JSONArrayConverter.ADDITIONS])
                    {
                        var additions = serverJSONValue[JSONArrayConverter.ADDITIONS];
                        var conversionInfos = serverJSONValue[ConverterService.TYPES_KEY];
                        for (let i in additions) {
                            let element = additions[i];
                            let idx = element[JSONArrayConverter.INDEX];
                            let val = element[JSONArrayConverter.VALUE];

                            var conversionInfo = null;
                            if (conversionInfos && conversionInfos[i] && conversionInfos[i][JSONArrayConverter.VALUE]) {
                                conversionInfo = conversionInfos[i][JSONArrayConverter.VALUE];
                            }

                            if (conversionInfo) {
                                internalState.conversionInfo[idx] = conversionInfo;
                                val = this.converterService.convertFromServerToClient(val, conversionInfo, currentClientValue[idx], componentScope, componentModelGetter);
                            }
                            currentClientValue.splice(idx, 0, val);

                            if (val && val[ConverterService.INTERNAL_IMPL] && val[ConverterService.INTERNAL_IMPL].setChangeNotifier) {
                                val[ConverterService.INTERNAL_IMPL].setChangeNotifier(this.getChangeNotifier(currentClientValue, idx));
                            }
                        }
                    }
                    if (serverJSONValue[JSONArrayConverter.UPDATES])
                    {
                        var updates = serverJSONValue[JSONArrayConverter.UPDATES];
                        var conversionInfos = serverJSONValue[ConverterService.TYPES_KEY];
                        for (let i in updates) {
                            let update = updates[i];
                            let idx = update[JSONArrayConverter.INDEX];
                            let val = update[JSONArrayConverter.VALUE];

                            var conversionInfo = null;
                            if (conversionInfos && conversionInfos[i] && conversionInfos[i][JSONArrayConverter.VALUE]) {
                                conversionInfo = conversionInfos[i][JSONArrayConverter.VALUE];
                            }

                            if (conversionInfo) {
                                internalState.conversionInfo[idx] = conversionInfo;
                                currentClientValue[idx] = val = this.converterService.convertFromServerToClient(val, conversionInfo, currentClientValue[idx], componentScope, componentModelGetter);
                            } else currentClientValue[idx] = val;

                            if (val && val[ConverterService.INTERNAL_IMPL] && val[ConverterService.INTERNAL_IMPL].setChangeNotifier) {
                                // child is able to handle it's own change mechanism
                                val[ConverterService.INTERNAL_IMPL].setChangeNotifier(this.getChangeNotifier(currentClientValue, idx));
                            }
                        }
                    }
                }
                //else {
                // else we got an update from server for a version that was already bumped by changes in browser; ignore that, as browser changes were sent to server
                // and server will detect the problem and send back a full update
                //}
            } else if (serverJSONValue && serverJSONValue[JSONArrayConverter.INITIALIZE]) {
                // only content version update - this happens when a full array value is set on this property client side; it goes to server
                // and then server sends back the version and we initialize / prepare the existing newValue for being watched/handle child conversions
                this.initializeNewValue(currentClientValue, serverJSONValue[JSONArrayConverter.CONTENT_VERSION]); // here we can count on not having any 'smart' values cause if we had
                // updates would have been received with this initialize as well (to initialize child elements as well to have the setChangeNotifier and internal things)
            } else if (!serverJSONValue || !serverJSONValue[JSONArrayConverter.NO_OP]) newValue = null; // anything else would not be supported... // TODO how to handle null values (special watches/complete array set from client)? if null is on server and something is set on client or the other way around?
        } finally {
            // TODO how to replace the add back watches if needed
//            addBackWatches(newValue, componentScope);
        }

        return newValue;
    }

    fromClientToServer( newClientData, oldClientData?) {
        // TODO how to handle null values (special watches/complete array set from client)? if null is on server and something is set on client or the other way around?

        var internalState;
        if (newClientData && (internalState = newClientData[ConverterService.INTERNAL_IMPL])) {
            if (internalState.isChanged()) {
                var changes = {};
                changes[JSONArrayConverter.CONTENT_VERSION] = internalState[JSONArrayConverter.CONTENT_VERSION];
                if (internalState.allChanged) {
                    // structure might have changed; increase version number
                    ++internalState[JSONArrayConverter.CONTENT_VERSION]; // we also increase the content version number - server should only be expecting updates for the next version number
                    // send all
                    var toBeSentArray = changes[JSONArrayConverter.VALUE] = [];
                    for (let idx = 0; idx < newClientData.length; idx++) {
                        var val = newClientData[idx];
                        if (internalState.conversionInfo[idx]) toBeSentArray[idx] = this.converterService.convertFromClientToServer(val, internalState.conversionInfo[idx], oldClientData ? oldClientData[idx] : undefined);
                        else toBeSentArray[idx] = this.converterService.convertClientObject(val);
                    }
                    internalState.allChanged = false;
                    internalState.changedIndexes = {};
                    return changes;
                } else {
                    // send only changed indexes
                    var changedElements = changes[JSONArrayConverter.UPDATES] = [];
                    for (let idx in internalState.changedIndexes) {
                        var newVal = newClientData[idx];
                        var oldVal = oldClientData ? oldClientData[idx] : undefined;

                        var changed = (newVal !== oldVal);
                        if (!changed) {
                            if (internalState.elUnwatch[idx]) {
                                var oldDumbVal = internalState.changedIndexes[idx].old;
                                // it's a dumb value - watched; see if it really changed acording to sablo rules
                                if (oldDumbVal !== newVal) {
                                    if (typeof newVal == "object") {
                                        if (this.converterService.isChanged(newVal, oldDumbVal, internalState.conversionInfo[idx])) {
                                            changed = true;
                                        }
                                    } else {
                                        changed = true;
                                    }
                                }
                            } else changed = newVal && newVal[ConverterService.INTERNAL_IMPL].isChanged(); // must be smart value then; same reference as checked above; so ask it if it changed
                        }

                        if (changed) {
                            var ch = {};
                            ch[JSONArrayConverter.INDEX] = idx;

                            if (internalState.conversionInfo[idx]) ch[JSONArrayConverter.VALUE] = this.converterService.convertFromClientToServer(newVal, internalState.conversionInfo[idx], oldVal);
                            else ch[JSONArrayConverter.VALUE] = this.converterService.convertClientObject(newVal);

                            changedElements.push(ch);
                        }
                    }
                    internalState.allChanged = false;
                    internalState.changedIndexes = {};
                    return changes;
                }
            } else if (newClientData ==  oldClientData) { // TODO do we need to compare differently?
                //if (angular.equals(newClientData, oldClientData)) {
                var x = {}; // no changes
                x[JSONArrayConverter.NO_OP] = true;
                return x;
            }
        }

        if (internalState) delete newClientData[ConverterService.INTERNAL_IMPL]; // some other new value was set; it's internal state is useless and will be re-initialized from server

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
        internalState[JSONArrayConverter.CONTENT_VERSION] = contentVersion; // being full content updates, we don't care about the version, we just accept it

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
//            internalState.objStructureUnwatch = null;
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