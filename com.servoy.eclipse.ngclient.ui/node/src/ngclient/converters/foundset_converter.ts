import { Injectable } from '@angular/core';
import { IConverter, ConverterService, PropertyContext } from '../../sablo/converter.service';
import { Deferred, LoggerService, LoggerFactory, RequestInfoPromise, IFoundset, ChangeListener, ViewPort, ViewportRowUpdates, FoundsetChangeEvent, FoundsetChangeListener,
    IFoundsetFieldsOnly, ColumnRef, ChangeAwareState, IChangeAwareValue } from '@servoy/public';
import { SabloService } from '../../sablo/sablo.service';
import { SabloDeferHelper, IDeferedState } from '../../sablo/defer.service';
import { SabloUtils } from '../../sablo/websocket.service';
import { ViewportService, FoundsetViewportState } from '../services/viewport.service';

@Injectable()
export class FoundsetConverter implements IConverter {

    static readonly UPDATE_PREFIX = 'upd_'; // prefixes keys when only partial updates are send for them
    static readonly SERVER_SIZE = 'serverSize';
    static readonly FOUNDSET_ID = 'foundsetId';
    static readonly SORT_COLUMNS = 'sortColumns';
    static readonly SELECTED_ROW_INDEXES = 'selectedRowIndexes';
    static readonly USER_SET_SELECTION = 'userSetSelection';
    static readonly MULTI_SELECT = 'multiSelect';
    static readonly HAS_MORE_ROWS = 'hasMoreRows';
    static readonly VIEW_PORT = 'viewPort';
    static readonly START_INDEX = 'startIndex';
    static readonly SIZE = 'size';
    static readonly ROWS = 'rows';
    static readonly COLUMN_FORMATS = 'columnFormats';
    static readonly HANDLED_CLIENT_REQUESTS = 'handledClientReqIds';
    static readonly ID_KEY = 'id';
    static readonly VALUE_KEY = 'value';
    static readonly DATAPROVIDER_KEY = 'dp';
    static readonly PUSH_TO_SERVER = 'w';
    static readonly NO_OP = 'n';

    private log: LoggerService;

    constructor(private converterService: ConverterService, private sabloService: SabloService, private sabloDeferHelper: SabloDeferHelper,
        private viewportService: ViewportService, private logFactory: LoggerFactory) {
        this.log = logFactory.getLogger('FoundsetPropertyValue');
    }

    fromServerToClient(serverJSONValue: object, currentClientValue?: Foundset, propertyContext?: PropertyContext): IFoundset {
        let newValue: Foundset = currentClientValue;

        // see if someone is listening for changes on current value; if so, prepare to fire changes at the end of this method
        const hasListeners = (currentClientValue && currentClientValue.state.changeListeners.length > 0);
        const notificationParamForListeners: FoundsetChangeEvent = hasListeners ? {} : undefined;
        let requestInfos: any[]; // these will end up in notificationParamForListeners but only if there is another change that
        // need to be announced; otherwise, they should not trigger the listener just by themselves - the requestInfos

        // see if this is an update or whole value and handle it
        if (!serverJSONValue) {
            newValue = undefined; // set it to nothing
            if (hasListeners) notificationParamForListeners.fullValueChanged = { oldValue: currentClientValue, newValue };
            const oldInternalState = currentClientValue ? currentClientValue.state : undefined; // internal state
            if (oldInternalState) this.sabloDeferHelper.cancelAll(oldInternalState);

        } else {
            // check for updates
            let updates = false;
            if (serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.SERVER_SIZE] !== undefined) {
                if (hasListeners) notificationParamForListeners.serverFoundsetSizeChanged = {
                    oldValue: currentClientValue.serverSize,
                    newValue: serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.SERVER_SIZE]
                };
                currentClientValue.serverSize = serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.SERVER_SIZE]; // currentClientValue should always be defined in this case
                updates = true;
            }
            if (serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.PUSH_TO_SERVER] !== undefined) {
                currentClientValue.state.push_to_server = serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.PUSH_TO_SERVER];
                updates = true;
            }
            if (serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.HAS_MORE_ROWS] !== undefined) {
                if (hasListeners) notificationParamForListeners.hasMoreRowsChanged = {
                    oldValue: currentClientValue.hasMoreRows,
                    newValue: serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.HAS_MORE_ROWS]
                };
                currentClientValue.hasMoreRows = serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.HAS_MORE_ROWS];
                updates = true;
            }
            if (serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.MULTI_SELECT] !== undefined) {
                if (hasListeners) notificationParamForListeners.multiSelectChanged = {
                    oldValue: currentClientValue.multiSelect,
                    newValue: serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.MULTI_SELECT]
                };
                currentClientValue.multiSelect = serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.MULTI_SELECT];
                updates = true;
            }
            if (serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.FOUNDSET_ID] !== undefined) {
                currentClientValue.foundsetId = serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.FOUNDSET_ID] ?
                    serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.FOUNDSET_ID] : undefined;
                updates = true;
            }
            if (serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.COLUMN_FORMATS] !== undefined) {
                if (hasListeners) notificationParamForListeners.columnFormatsChanged = {
                    oldValue: currentClientValue.columnFormats,
                    newValue: serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.COLUMN_FORMATS]
                };
                currentClientValue.columnFormats = serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.COLUMN_FORMATS];
                updates = true;
            }

            if (serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.SORT_COLUMNS] !== undefined) {
                if (hasListeners) notificationParamForListeners.sortColumnsChanged = {
                    oldValue: currentClientValue.sortColumns,
                    newValue: serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.SORT_COLUMNS]
                };
                currentClientValue.sortColumns = serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.SORT_COLUMNS];
                updates = true;
            }

            if (serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.SELECTED_ROW_INDEXES] !== undefined) {
                if (hasListeners) {
                    notificationParamForListeners.selectedRowIndexesChanged = {
                        oldValue: currentClientValue.selectedRowIndexes,
                        newValue: serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.SELECTED_ROW_INDEXES]
                    };
                    if (serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.USER_SET_SELECTION] !== undefined) {
                        notificationParamForListeners.userSetSelection = true;
                    }
                }
                currentClientValue.selectedRowIndexes = serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.SELECTED_ROW_INDEXES];
                updates = true;
            }

            if (serverJSONValue[FoundsetConverter.HANDLED_CLIENT_REQUESTS] !== undefined) {
                // array of { id: ...int..., value: ...boolean... } which says if a req. was handled successfully by server or not
                const handledRequests = serverJSONValue[FoundsetConverter.HANDLED_CLIENT_REQUESTS];
                const internalState = currentClientValue.state;

                handledRequests.forEach((handledReq) => {
                    const defer = this.sabloDeferHelper.retrieveDeferForHandling(handledReq[FoundsetConverter.ID_KEY], internalState);
                    if (defer) {
                        const promise: RequestInfoPromise<any> = defer.promise;
                        if (hasListeners && promise.requestInfo) {
                            if (!requestInfos) requestInfos = [];
                            requestInfos.push(promise.requestInfo);
                        }
                        if (defer === internalState.selectionUpdateDefer) {
                            this.sabloService.resolveDeferedEvent(handledReq[FoundsetConverter.ID_KEY], currentClientValue.selectedRowIndexes, handledReq[FoundsetConverter.VALUE_KEY]);
                            delete internalState.selectionUpdateDefer;
                        } else {
                            this.sabloService.resolveDeferedEvent(handledReq[FoundsetConverter.ID_KEY], null, handledReq[FoundsetConverter.VALUE_KEY]);
                        }
                    }
                });

                updates = true;
            }

            if (serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.VIEW_PORT] !== undefined) {
                updates = true;
                const viewPortUpdate = serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.VIEW_PORT];
                const internalState = currentClientValue.state;

                if (viewPortUpdate.startIndex !== undefined && currentClientValue.viewPort.startIndex !== viewPortUpdate.startIndex) {
                    if (hasListeners) notificationParamForListeners.viewPortStartIndexChanged = { oldValue: currentClientValue.viewPort.startIndex, newValue: viewPortUpdate.startIndex };
                    currentClientValue.viewPort.startIndex = viewPortUpdate.startIndex;
                }
                if (viewPortUpdate.size !== undefined && currentClientValue.viewPort.size !== viewPortUpdate.size) {
                    if (hasListeners) notificationParamForListeners.viewPortSizeChanged = { oldValue: currentClientValue.viewPort.size, newValue: viewPortUpdate.size };
                    currentClientValue.viewPort.size = viewPortUpdate.size;
                }
                if (viewPortUpdate.rows !== undefined) {
                    const oldRows = currentClientValue.viewPort.rows;
                    currentClientValue.viewPort.rows = this.viewportService.updateWholeViewport(currentClientValue.viewPort.rows, internalState, viewPortUpdate.rows,
                        viewPortUpdate[ConverterService.TYPES_KEY] && viewPortUpdate[ConverterService.TYPES_KEY].rows ? viewPortUpdate[ConverterService.TYPES_KEY].rows : undefined,
                        propertyContext, false);

                    // new rows; set prototype for each row
                    const rows = currentClientValue.viewPort.rows;
                    for (let i = rows.length - 1; i >= 0; i--) {
                        rows[i] = SabloUtils.cloneWithDifferentPrototype(rows[i], internalState.rowPrototype);
                    }

                    if (hasListeners) notificationParamForListeners.viewportRowsCompletelyChanged = { oldValue: oldRows, newValue: currentClientValue.viewPort.rows };
                } else if (viewPortUpdate[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.ROWS] !== undefined) {
                    const types = viewPortUpdate[ConverterService.TYPES_KEY] && viewPortUpdate[ConverterService.TYPES_KEY][FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.ROWS] ?
                        viewPortUpdate[ConverterService.TYPES_KEY][FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.ROWS] : undefined;
                    this.viewportService.updateViewportGranularly(currentClientValue.viewPort.rows, internalState,
                        viewPortUpdate[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.ROWS], types, propertyContext, false, internalState.rowPrototype);

                    if (hasListeners) {
                        const upd: ViewportRowUpdates = viewPortUpdate[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.ROWS];
                        notificationParamForListeners.viewportRowsUpdated = upd; // viewPortUpdate[UPDATE_PREFIX + ROWS] was already prepared for listeners by viewportService.updateViewportGranularly
                    }
                }
            }

            // if it's a no-op, ignore it (sometimes server asks a prop. to send changes even though it has none to send)
            if (!updates && !serverJSONValue[FoundsetConverter.NO_OP]) {
                // not updates - so whole thing receive
                // conversion to server in case it is sent to handler or server side internalAPI calls as argument of type "foundsetRef"
                let oldValue: IFoundsetFieldsOnly;
                let internalState: FoundsetState;
                if (!newValue /* newValue is now already currentValue, see code above, so we are checking current value here */) {
                    newValue = new Foundset(this.sabloService, this.sabloDeferHelper,
                        this.logFactory, this.converterService, this.viewportService, new FoundsetState());

                    internalState = newValue.state;
                    this.sabloDeferHelper.initInternalStateForDeferring(internalState, 'svy foundset * ');
                    internalState.rowPrototype = {};
                    // conversion of rows to server in case it is sent to handler or server side internalAPI calls as argument of type "foundsetRef"
                    internalState.rowPrototype[ConverterService.DEFAULT_CONVERSION_TO_SERVER_FUNC] = (value: any) => {
                        if (value._svyRowId) {
                            const r: FoundsetRow = { _svyRowId: value._svyRowId, foundsetId: newValue.foundsetId };
                            return r;
                        }
                        return null;
                    };
                } else {
                    oldValue = new FoundsetFieldsOnly(newValue);
                    internalState = newValue.state;
                }

                Object.keys(serverJSONValue).forEach((prop) => {
                    newValue[prop] = serverJSONValue[prop];
                });
                if (hasListeners) notificationParamForListeners.fullValueChanged = { oldValue, newValue };

                const rows = newValue.viewPort.rows;
                if (typeof newValue[FoundsetConverter.PUSH_TO_SERVER] !== 'undefined') {
                    internalState.push_to_server = newValue[FoundsetConverter.PUSH_TO_SERVER];
                    delete newValue[FoundsetConverter.PUSH_TO_SERVER];
                }

                // convert data if needed - specially done for Date send/receive as the rest are primitives anyway in case of foundset
                this.viewportService.updateAllConversionInfo(rows, internalState, newValue.viewPort[ConverterService.TYPES_KEY] ?
                    newValue.viewPort[ConverterService.TYPES_KEY][FoundsetConverter.ROWS] : undefined);
                if (newValue.viewPort[ConverterService.TYPES_KEY]) {
                    // relocate conversion info in internal state and convert
                    this.converterService.convertFromServerToClient(rows, newValue.viewPort[ConverterService.TYPES_KEY][FoundsetConverter.ROWS], null, propertyContext);
                    delete newValue.viewPort[ConverterService.TYPES_KEY];
                }
                // do set prototype after rows are converted
                for (let i = rows.length - 1; i >= 0; i--) {
                    rows[i] = SabloUtils.cloneWithDifferentPrototype(rows[i], internalState.rowPrototype);
                }
            }

            this.log.spam(this.log.buildMessage(() => ('svy foundset * updates or value received from server; new viewport and server size (' +
                (newValue ? newValue.viewPort.startIndex + ', ' + newValue.viewPort.size + ', ' + newValue.serverSize + ', ' +
                    JSON.stringify(newValue.selectedRowIndexes) : newValue) + ')')));
            if (notificationParamForListeners && Object.keys(notificationParamForListeners).length > 0) {
                this.log.spam(this.log.buildMessage(() => ('svy foundset * firing founset listener notifications...')));

                if (requestInfos) notificationParamForListeners.requestInfos = requestInfos;

                // use previous (current) value as newValue might be undefined/null and the listeners would be the same anyway
                currentClientValue.state.fireChanges(notificationParamForListeners);
            }

            return newValue;
        }
    }

    fromClientToServer(newClientData: Foundset, oldClientData?) {
        if (newClientData) {
            const newDataInternalState = newClientData.state;
            if (newDataInternalState.isChanged()) {
                const tmp = newDataInternalState.requests;
                newDataInternalState.requests = [];
                return tmp;
            }
        }
        return [];
    }
}

export class Foundset implements IChangeAwareValue, IFoundset {

    /**
     * An identifier that allows you to use this foundset via the 'foundsetRef' type;
     * when a 'foundsetRef' type sends a foundset from server to client (for example
     * as a return value of callServerSideApi) it will translate to this identifier
     * on client (so you can use it to find the actual foundset property in the model if
     * server side script put it in the model as well); internally when sending a
     * 'foundset' typed property to server through a 'foundsetRef' typed argument or prop,
     * it will use this foundsetId as well to find it on server and give a real Foundset
     */
    foundsetId: number;

    /**
     * the size of the foundset on server (so not necessarily the total record count
     * in case of large DB tables)
     */
    serverSize: number;

    /**
     * this is the data you need to have loaded on client (just request what you need via provided
     * loadRecordsAsync or loadExtraRecordsAsync)
     */
    viewPort: ViewPort;

    /**
     * array of selected records in foundset; indexes can be out of current
     * viewPort as well
     */
    selectedRowIndexes: number[];

    /**
     * sort string of the foundset, the same as the one used in scripting for
     * foundset.sort and foundset.getCurrentSort. Example: 'orderid asc'.
     */
    sortColumns: string;

    /**
     * the multiselect mode of the server's foundset; if this is false,
     * selectedRowIndexes can only have one item in it
     */
    multiSelect = false;

    /**
     * if the foundset is large and on server-side only part of it is loaded (so
     * there are records in the foundset beyond 'serverSize') this is set to true;
     * in this way you know you can load records even after 'serverSize' (requesting
     * viewport to load records at index serverSize-1 or greater will load more
     * records in the foundset)
     */
    hasMoreRows: boolean;

    /**
     * columnFormats is only present if you specify
     * "provideColumnFormats": true inside the .spec file for this foundset property;
     * it gives the default column formatting that Servoy would normally use for
     * each column of the viewport - which you can then also use in the
     * browser yourself; keys are the dataprovider names and values are objects that contain
     * the format contents
     */
    columnFormats: Record<string, object>;

    private log: LoggerService;

    constructor(private sabloService: SabloService, private sabloDeferHelper: SabloDeferHelper, logFactory: LoggerFactory,
        private converterService: ConverterService, private viewportService: ViewportService, public state: FoundsetState) {

        this.log = logFactory.getLogger('Foundset');
        this.viewPort = { startIndex: undefined, size: undefined, rows: [] };
    }

    getStateHolder(): ChangeAwareState {
        return this.state;
    }

    public loadRecordsAsync(startIndex: number, size: number): RequestInfoPromise<any> {
        this.log.spam(this.log.buildMessage(() => ('svy foundset * loadRecordsAsync requested with (' + startIndex + ', ' + size + ')')));
        if (isNaN(startIndex) || isNaN(size)) throw new Error('loadRecordsAsync: start or size are not numbers (' + startIndex + ',' + size + ')');

        const req = { newViewPort: { startIndex, size } };
        const requestID = this.sabloDeferHelper.getNewDeferId(this.state);
        req[FoundsetConverter.ID_KEY] = requestID;
        this.state.requests.push(req);

        this.state.notifyChangeListener();
        return this.state.deferred[requestID].defer.promise;
    }

    public loadLessRecordsAsync(negativeOrPositiveCount: number, dontNotifyYet?: boolean): RequestInfoPromise<any> {
        this.log.spam(this.log.buildMessage(() => ('svy foundset * loadLessRecordsAsync requested with (' + negativeOrPositiveCount + ', ' + dontNotifyYet + ')')));
        if (isNaN(negativeOrPositiveCount)) throw new Error('loadLessRecordsAsync: lessrecords is not a number (' + negativeOrPositiveCount + ')');

        const req = { loadLessRecords: negativeOrPositiveCount };
        const requestID = this.sabloDeferHelper.getNewDeferId(this.state);
        req[FoundsetConverter.ID_KEY] = requestID;
        this.state.requests.push(req);

        if (!dontNotifyYet) this.state.notifyChangeListener();
        return this.state.deferred[requestID].defer.promise;
    }

    public loadExtraRecordsAsync(negativeOrPositiveCount: number, dontNotifyYet?: boolean): RequestInfoPromise<any> {
        this.log.spam(this.log.buildMessage(() => ('svy foundset * loadExtraRecordsAsync requested with (' + negativeOrPositiveCount + ', ' + dontNotifyYet + ')')));
        if (isNaN(negativeOrPositiveCount)) throw new Error('loadExtraRecordsAsync: extrarecords is not a number (' + negativeOrPositiveCount + ')');

        const req = { loadExtraRecords: negativeOrPositiveCount };
        const requestID = this.sabloDeferHelper.getNewDeferId(this.state);
        req[FoundsetConverter.ID_KEY] = requestID;
        this.state.requests.push(req);

        if (!dontNotifyYet) this.state.notifyChangeListener();
        return this.state.deferred[requestID].defer.promise;
    }

    public notifyChanged() {
        this.log.spam(this.log.buildMessage(() => ('svy foundset * notifyChanged called')));
        if (this.state.requests.length > 0) this.state.notifyChangeListener();
    }

    public sort(columns: Array<{ name: string; direction: ('asc' | 'desc') }>): RequestInfoPromise<any> {
        this.log.spam(this.log.buildMessage(() => ('svy foundset * sort requested with ' + JSON.stringify(columns))));
        const req = { sort: columns };
        const requestID = this.sabloDeferHelper.getNewDeferId(this.state);
        req[FoundsetConverter.ID_KEY] = requestID;
        this.state.requests.push(req);
        this.state.notifyChangeListener();
        return this.state.deferred[requestID].defer.promise;
    }

    public setPreferredViewportSize(size: number, sendSelectionViewportInitially?: boolean, initialSelectionViewportCentered?: boolean) {
        this.log.spam(this.log.buildMessage(() => ('svy foundset * setPreferredViewportSize called with (' + size + ', ' +
            sendSelectionViewportInitially + ', ' + initialSelectionViewportCentered + ')')));
        if (isNaN(size)) throw new Error('setPreferredViewportSize(...): illegal argument; size is not a number (' + size + ')');
        const request = { preferredViewportSize: size };
        if (sendSelectionViewportInitially !== undefined) request['sendSelectionViewportInitially'] = !!sendSelectionViewportInitially;
        if (initialSelectionViewportCentered !== undefined) request['initialSelectionViewportCentered'] = !!initialSelectionViewportCentered;
        this.state.requests.push(request);
        this.state.notifyChangeListener();
    }

    public requestSelectionUpdate(tmpSelectedRowIdxs: Array<number>): RequestInfoPromise<any> {
        this.log.spam(this.log.buildMessage(() => ('svy foundset * requestSelectionUpdate called with ' + JSON.stringify(tmpSelectedRowIdxs))));
        if (this.state.selectionUpdateDefer) {
            this.state.selectionUpdateDefer.reject('Selection change defer cancelled because we are already sending another selection to server.');
        }
        delete this.state.selectionUpdateDefer;

        const msgId = this.sabloDeferHelper.getNewDeferId(this.state);
        this.state.selectionUpdateDefer = this.state.deferred[msgId].defer;

        const req = { newClientSelectionRequest: tmpSelectedRowIdxs, selectionRequestID: msgId };
        req[FoundsetConverter.ID_KEY] = msgId;
        this.state.requests.push(req);
        this.state.notifyChangeListener();

        return this.state.selectionUpdateDefer.promise;
    }

    public getRecordRefByRowID(rowID: string) {
        if (rowID) {
            return { _svyRowId: rowID, foundsetId: this.foundsetId };
        }
        return null;
    }

    public updateViewportRecord(rowID: string, columnID: string, newValue: any, oldValue: any) {
        this.log.spam(this.log.buildMessage(() => ('svy foundset * updateRecord requested with (' + rowID + ', ' + columnID + ', ' + newValue)));
        const r: ColumnRef = { _svyRowId: rowID, dp: columnID, value: newValue };
        // convert new data if necessary
        let conversionInfo;
        if (this.state.viewportConversions) {
            for (const idx in this.viewPort.rows) {
                if (this.viewPort.rows[idx]._svyRowId === rowID) {
                    conversionInfo = this.state.viewportConversions[idx];
                    break;
                }
            }
        }
        if (conversionInfo && conversionInfo[columnID]) r.value = this.converterService.convertFromClientToServer(r.value, conversionInfo[columnID], oldValue);
        else r.value = this.converterService.convertClientObject(r.value);

        this.state.requests.push({ viewportDataChanged: r });
        this.state.notifyChangeListener();
    }

    /**
     * Adds a change listener that will get triggered when server sends changes for this foundset.
     *
     * @see WebsocketSession.addIncomingMessageHandlingDoneTask if you need your code to execute after
     * all properties that were linked to this foundset get their changes applied
     * you can use WebsocketSession.addIncomingMessageHandlingDoneTask.
     * @param listener the listener to register.
     * @returns the deregister function.
     */
    public addChangeListener(listener: FoundsetChangeListener): () => void {
        return this.state.addChangeListener(listener);
    }
    public removeChangeListener(listener: FoundsetChangeListener) {
        this.state.removeChangeListener(listener);
    }

    // was SabloUtils.DEFAULT_CONVERSION_TO_SERVER_FUNC, but we can't use a constant as a function name
    public _dctsf() {
        return this.foundsetId;
    }

    public columnDataChanged(index: number, columnID: string, newValue: any, oldValue?: any) {
        if (this.state.push_to_server === undefined) return;
        if (this.viewPort.rows && newValue !== oldValue) {
            if (newValue === undefined) newValue = null;
            this.viewportService.queueChange(this.viewPort.rows, this.state, this.state.push_to_server, index, columnID, newValue, oldValue);
        }
    }
}

// TODO can we further improve this to also have the conversion function??
interface FoundsetRow {
    _svyRowId?: string;
    foundsetId?: number;
}

class FoundsetState extends FoundsetViewportState implements IDeferedState {

    deferred: { [key: string]: { defer: Deferred<any>; timeoutId: any } };
    timeoutRejectLogPrefix: string;
    changeListeners: FoundsetChangeListener[] = [];
    rowPrototype: FoundsetRow;
    selectionUpdateDefer: Deferred<any>;
    push_to_server: any = undefined;

    init(deferred: { [key: string]: { defer: Deferred<any>; timeoutId: any } }, timeoutRejectLogPrefix: string) {
        this.deferred = deferred;
        this.timeoutRejectLogPrefix = timeoutRejectLogPrefix;
    }

    public addChangeListener(listener: (change: FoundsetChangeEvent) => void): () => void {
        this.changeListeners.push(listener);
        return () => this.removeChangeListener(listener);
    }

    public removeChangeListener(listener: (change: FoundsetChangeEvent) => void) {
        const index = this.changeListeners.indexOf(listener);
        if (index > -1) {
            this.changeListeners.splice(index, 1);
        }
    }

    public fireChanges(foundsetChanges: FoundsetChangeEvent) {
        for (const cl of this.changeListeners) {
            cl(foundsetChanges);
        }
    }

    public isChanged() {
        return this.requests && (this.requests.length > 0);
    }
}

class FoundsetFieldsOnly implements IFoundsetFieldsOnly {

    foundsetId: number;
    serverSize: number;
    viewPort: ViewPort;
    selectedRowIndexes: number[];
    sortColumns: string;
    multiSelect: boolean;
    hasMoreRows: boolean;
    columnFormats: Record<string, any>;

    constructor(foundsetToShallowCopy: Foundset) {
        this.foundsetId = foundsetToShallowCopy.foundsetId;
        this.serverSize = foundsetToShallowCopy.serverSize;
        this.viewPort = foundsetToShallowCopy.viewPort;
        this.selectedRowIndexes = foundsetToShallowCopy.selectedRowIndexes;
        this.sortColumns = foundsetToShallowCopy.sortColumns;
        this.multiSelect = foundsetToShallowCopy.multiSelect;
        this.hasMoreRows = foundsetToShallowCopy.hasMoreRows;
        this.columnFormats = foundsetToShallowCopy.columnFormats;
    }

}