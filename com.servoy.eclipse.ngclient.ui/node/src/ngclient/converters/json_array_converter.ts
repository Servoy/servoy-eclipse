import { IConverter, ConverterService, PropertyContext } from '../../sablo/converter.service';

import { IterableDiffers } from '@angular/core';

import { SpecTypesService, ArrayState, ICustomArray, instanceOfChangeAwareValue, instanceOfCustomArray } from '@servoy/public';

export class JSONArrayConverter implements IConverter {
    /* eslint-disable */
    private static readonly GRANULAR_UPDATES = "g";
    private static readonly GRANULAR_UPDATE_DATA = "d";
    private static readonly OP_ARRAY_START_END_TYPE = "op";
    private static readonly CHANGED = 0;
    private static readonly INSERT = 1;
    private static readonly DELETE = 2;
        
    private static readonly UPDATES = 'u';
    private static readonly INDEX = 'i';
    private static readonly INITIALIZE = 'in';
    private static readonly VALUE = 'v';
    private static readonly PUSH_TO_SERVER = 'w'; // value is undefined when we shouldn't send changes to server, false if it should be shallow watched and true if it should be deep watched
    private static readonly CONTENT_VERSION = 'vEr'; // server side sync to make sure we don't end up granular updating something that has changed meanwhile server-side
    private static readonly NO_OP = 'n';
    /* eslint-enable */

    constructor(private converterService: ConverterService, private specTypesService: SpecTypesService, private iterableDiffers: IterableDiffers) {
    }

    fromServerToClient(serverJSONValue: any, currentClientValue?: ICustomArray<any>, propertyContext?: PropertyContext) {
        let newValue = currentClientValue;
        let state: ArrayState = null;

        try {
            if (serverJSONValue && serverJSONValue[JSONArrayConverter.VALUE]) {
                // full contents
                newValue = serverJSONValue[JSONArrayConverter.VALUE];
                const newValueCA = this.specTypesService.enhanceArrayType(newValue, this.iterableDiffers);
                state = newValueCA.getStateHolder();
                state[JSONArrayConverter.CONTENT_VERSION] = serverJSONValue[JSONArrayConverter.CONTENT_VERSION];
                if (typeof serverJSONValue[JSONArrayConverter.PUSH_TO_SERVER] !== 'undefined') state[JSONArrayConverter.PUSH_TO_SERVER] = serverJSONValue[JSONArrayConverter.PUSH_TO_SERVER];

                if (newValue.length) {
                    for (let c = 0; c < newValue.length; c++) {
                        let elem = newValue[c];
                        let conversionInfo = null;
                        if (serverJSONValue[ConverterService.TYPES_KEY]) {
                            conversionInfo = serverJSONValue[ConverterService.TYPES_KEY][c];
                        }

                        if (conversionInfo) {
                            state.conversionInfo[c] = conversionInfo;
                            newValue[c] = elem = this.converterService.convertFromServerToClient(elem, conversionInfo, currentClientValue ? currentClientValue[c] : undefined, propertyContext);
                        }

                        if (instanceOfChangeAwareValue(elem)) {
                            // child is able to handle it's own change mechanism
                            elem.getStateHolder().setChangeListener(() => {
                                state.getChangedKeys().add(c);
                                state.notifyChangeListener();
                            });
                        }
                    }
                }
            } else if (serverJSONValue && serverJSONValue[JSONArrayConverter.GRANULAR_UPDATES]) {
                // granular updates received;

                state = currentClientValue.getStateHolder();

                if (serverJSONValue[JSONArrayConverter.INITIALIZE])
                    state[JSONArrayConverter.CONTENT_VERSION] = serverJSONValue[JSONArrayConverter.CONTENT_VERSION];

                // if something changed browser-side, increasing the content version thus not matching next expected version,
                // we ignore this update and expect a fresh full copy of the array from the server (currently server value is
                // leading/has priority because not all server side values might support being recreated from client values)
                if (state[JSONArrayConverter.CONTENT_VERSION] === serverJSONValue[JSONArrayConverter.CONTENT_VERSION]) {
                    let i;
                    for (const granularOp of serverJSONValue[JSONArrayConverter.GRANULAR_UPDATES]) {
                        const startIndex_endIndex_opType = granularOp[JSONArrayConverter.OP_ARRAY_START_END_TYPE]; // it's an array of 3 elements in the order given in name
                        const startIndex: number = startIndex_endIndex_opType[0];
                        const endIndex: number = startIndex_endIndex_opType[1];
                        const opType: number = startIndex_endIndex_opType[2];

                        if (opType === JSONArrayConverter.CHANGED) {
                            const granularOpConversionInfo = granularOp[ConverterService.TYPES_KEY];
                            const changedData = granularOp[JSONArrayConverter.GRANULAR_UPDATE_DATA];
                            for (i = startIndex; i <= endIndex; i++) {
                                const relIdx = i - startIndex;

                                // apply the conversions, update value and kept conversion info for changed indexes
                                state.conversionInfo[i] = granularOpConversionInfo ? granularOpConversionInfo[relIdx] : undefined;
                                if (state.conversionInfo[i]) {
                                    currentClientValue[i] = this.converterService.convertFromServerToClient(changedData[relIdx], state.conversionInfo[i],
                                                                                currentClientValue[i], propertyContext);
                                } else currentClientValue[i] = changedData[relIdx];

                                const val = currentClientValue[i];
                                if (instanceOfChangeAwareValue(val)) {
                                    // child is able to handle it's own change mechanism
                                    val.getStateHolder().setChangeListener(() => {
                                        state.getChangedKeys().add(i);
                                        state.notifyChangeListener();
                                    });
                                }
                            }
                        } else if (opType === JSONArrayConverter.INSERT) {
                            const granularOpConversionInfo = granularOp[ConverterService.TYPES_KEY];
                            let changedData = granularOp[JSONArrayConverter.GRANULAR_UPDATE_DATA];
                            const numberOfInsertedRows = changedData.length;
                            const oldLength = currentClientValue.length;

                            // apply conversions
                            if (granularOpConversionInfo) changedData = this.converterService.convertFromServerToClient(changedData, granularOpConversionInfo, undefined, propertyContext);

                            // shift conversion info after insert to the right
                            if (state.conversionInfo) {
                                for (i = oldLength - 1; i >= startIndex; i--) {
                                    state.conversionInfo[i + numberOfInsertedRows] = state.conversionInfo[i];
                                    delete state.conversionInfo[i];
                                }
                            }

                            // do insert the new data, remember conversion info
                            currentClientValue.splice.apply(currentClientValue, [startIndex, 0].concat(changedData));
                            if (granularOpConversionInfo) {
                                for (i = 0; i < changedData.length ; i++) {
                                    if (granularOpConversionInfo[i]) state.conversionInfo[startIndex + i] = granularOpConversionInfo[i];
                                }
                            }

                            // update any affected change notifiers
                            for (i = startIndex; i < currentClientValue.length; i++) {
                                if (instanceOfChangeAwareValue(currentClientValue[i])) {
                                    // child is able to handle it's own change mechanism
                                    currentClientValue[i].getStateHolder().setChangeListener(() => {
                                        state.getChangedKeys().add(i);
                                        state.notifyChangeListener();
                                    });
                                }
                            }
                        } else if (opType === JSONArrayConverter.DELETE) {
                            const oldLength = currentClientValue.length;
                            const numberOfDeletedRows = endIndex - startIndex + 1;

                            if (state.conversionInfo) {
                                // delete conversion info for deleted rows and shift left what is after deletion
                                for (i = startIndex; i <= endIndex; i++)
                                    delete state.conversionInfo[i];
                                for (i = endIndex + 1; i < oldLength; i++) {
                                    state.conversionInfo[i - numberOfDeletedRows] = state.conversionInfo[i];
                                    delete state.conversionInfo[i];
                                }
                            }
                            currentClientValue.splice(startIndex, numberOfDeletedRows);

                            // update any affected change notifiers
                            for (i = startIndex; i < currentClientValue.length; i++) {
                                if (instanceOfChangeAwareValue(currentClientValue[i])) {
                                    // child is able to handle it's own change mechanism
                                    currentClientValue[i].getStateHolder().setChangeListener(() => {
                                        state.getChangedKeys().add(i);
                                        state.notifyChangeListener();
                                    });
                                }
                            }
                        }
                    }
                }
                // else {
                // else we got an update from server for a version that was already bumped by changes in browser; ignore that, as browser changes were sent to server
                // and server will detect the problem and send back a full update
                // }
            } else if (serverJSONValue && serverJSONValue[JSONArrayConverter.INITIALIZE]) {
                // only content version update - this happens when a full array value is set on this property client side; it goes to server
                // and then server sends back the version and we initialize / prepare the existing newValue for being watched/handle child conversions
                state = currentClientValue.getStateHolder();
                state[JSONArrayConverter.CONTENT_VERSION] = serverJSONValue[JSONArrayConverter.CONTENT_VERSION]; // here we can count on not having any 'smart' values cause if we had
                // updates would have been received with this initialize as well (to initialize child elements as well to have the setChangeNotifier and internal things)
            } else if (!serverJSONValue || !serverJSONValue[JSONArrayConverter.NO_OP])
                // anything else would not be supported... // TODO how to handle null values (special watches/complete array set from client)?
                // if null is on server and something is set on client or the other way around?
                newValue = null;
        } finally {
            state.clearChanges();
        }

        return newValue;
    }

    fromClientToServer(newClientData: ICustomArray<any>, oldClientData?) {

        // test if this was an array created fully on the client.
        if (!instanceOfCustomArray(newClientData)) {
            this.specTypesService.enhanceArrayType(newClientData, this.iterableDiffers);
        }
        let internalState: ArrayState;
        if (newClientData && (internalState = newClientData.getStateHolder())) {
            const arrayChanges = internalState.getChangedKeys();
            if (arrayChanges.size > 0 || internalState.allChanged) {
                const changes = {};

                if (internalState[JSONArrayConverter.CONTENT_VERSION]) changes[JSONArrayConverter.CONTENT_VERSION] = internalState[JSONArrayConverter.CONTENT_VERSION];
                if (internalState.allChanged) {
                    // structure might have changed; increase version number
                    ++internalState[JSONArrayConverter.CONTENT_VERSION]; // we also increase the content version number - server should only be expecting updates for the next version number
                    // send all
                    const toBeSentArray = changes[JSONArrayConverter.VALUE] = [];
                    for (let idx = 0; idx < newClientData.length; idx++) {
                        const val = newClientData[idx];
                        if (instanceOfChangeAwareValue(val)) {
                            val.getStateHolder().markAllChanged(false);
                        }
                        toBeSentArray[idx] = this.convert(val, internalState.conversionInfo[idx]);
                    }
                } else {
                    // send only changed indexes
                    const changedElements = changes[JSONArrayConverter.UPDATES] = [];
                    arrayChanges.forEach((idx) => {
                        const newVal = newClientData[idx];
                        const ch = {};
                        ch[JSONArrayConverter.INDEX] = idx;
                        ch[JSONArrayConverter.VALUE] = this.convert(newVal, internalState.conversionInfo[idx]);
                        changedElements.push(ch);
                    });
                }
                internalState.clearChanges();
                return changes;
            } else if (newClientData === oldClientData) { // TODO do we need to compare differently?
                // if (angular.equals(newClientData, oldClientData)) {
                const x = {}; // no changes
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
        if (conversionInfo) return this.converterService.convertFromClientToServer(newVal, conversionInfo);
        return this.converterService.convertClientObject(newVal);
    }

}
