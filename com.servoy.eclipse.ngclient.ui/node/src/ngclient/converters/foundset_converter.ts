import { ConverterService, IChangeAwareValue, IUIDestroyAwareValue } from '../../sablo/converter.service';
import { IType, IPropertyContext } from '../../sablo/types_registry';
import { Deferred, LoggerService, LoggerFactory, RequestInfoPromise, IFoundset, ViewPort, FoundsetChangeEvent, FoundsetChangeListener,
    IFoundsetFieldsOnly} from '@servoy/public';
import { SabloService } from '../../sablo/sablo.service';
import { SabloDeferHelper, IDeferedState } from '../../sablo/defer.service';
import { ViewportService, FoundsetViewportState, ConversionInfoFromServerForViewport, RowUpdate, IPropertyContextCreatorForRow } from '../services/viewport.service';
import { RecordRefType, RecordRefForServer } from './record_ref_converter';

export class FoundsetType implements IType<FoundsetValue> {

    public static readonly TYPE_NAME = 'foundset';

    private log: LoggerService;

    constructor(private sabloService: SabloService, private sabloDeferHelper: SabloDeferHelper,
            private viewportService: ViewportService, logFactory: LoggerFactory) {
        this.log = logFactory.getLogger('FoundsetType');
    }

    public fromServerToClient(serverJSONValue: ServerSentJSONForFoundset, currentClientValue: FoundsetValue, propertyContext: IPropertyContext): FoundsetValue {
        let newValue: FoundsetValue = currentClientValue;

        // see if someone is listening for changes on current value; if so, prepare to fire changes at the end of this method
        const hasListeners = (currentClientValue && currentClientValue.getInternalState().changeListeners.length > 0);
        const notificationParamForListeners: FoundsetChangeEvent = hasListeners ? { } : undefined;
        let requestInfos: any[]; // these will end up in notificationParamForListeners but only if there is another change that
        // need to be announced; otherwise, they should not trigger the listener just by themselves - the requestInfos

        // remove smart notifiers and proxy notification effects for changes that come from server
        if (currentClientValue) currentClientValue.getInternalState().ignoreChanges = true;

        // see if this is an update or whole value and handle it
        if (!serverJSONValue) {
            newValue = undefined;
            if (hasListeners) notificationParamForListeners.fullValueChanged = { oldValue : currentClientValue, newValue };

            const oldInternalState: FoundsetTypeInternalState = currentClientValue?.getInternalState(); // internal state / this.sabloConverters interface
            if (oldInternalState) this.sabloDeferHelper.cancelAll(oldInternalState);
        } else {
            // check for updates
            if (serverJSONValue.upd_serverSize !== undefined) {
                if (hasListeners) notificationParamForListeners.serverFoundsetSizeChanged = {
                    oldValue: currentClientValue.serverSize,
                    newValue: serverJSONValue.upd_serverSize
                };

                currentClientValue.serverSize = serverJSONValue.upd_serverSize; // currentClientValue should always be defined in this case
            }
            if (serverJSONValue.upd_foundsetDefinition !== undefined) {
                if (hasListeners) notificationParamForListeners.foundsetDefinitionChanged = true;
            }

            if (serverJSONValue.upd_hasMoreRows !== undefined) {
                if (hasListeners) notificationParamForListeners.hasMoreRowsChanged = {
                    oldValue : currentClientValue.hasMoreRows,
                    newValue : serverJSONValue.upd_hasMoreRows
                };

                currentClientValue.hasMoreRows = serverJSONValue.upd_hasMoreRows;
            }

            if (serverJSONValue.upd_multiSelect !== undefined) {
                if (hasListeners) notificationParamForListeners.multiSelectChanged = {
                    oldValue : currentClientValue.multiSelect,
                    newValue : serverJSONValue.upd_multiSelect
                };

                currentClientValue.multiSelect = serverJSONValue.upd_multiSelect;
            }

            if (serverJSONValue.upd_foundsetId !== undefined) {
                currentClientValue.foundsetId = serverJSONValue.upd_foundsetId ? serverJSONValue.upd_foundsetId : undefined;
            }

            if (serverJSONValue.upd_columnFormats !== undefined) {
                if (hasListeners) notificationParamForListeners.columnFormatsChanged = {
                    oldValue : currentClientValue.columnFormats,
                    newValue : serverJSONValue.upd_columnFormats
                };

                currentClientValue.columnFormats = serverJSONValue.upd_columnFormats;
            }

            if (serverJSONValue.upd_sortColumns !== undefined) {
                if (hasListeners) notificationParamForListeners.sortColumnsChanged = {
                    oldValue : currentClientValue.sortColumns,
                    newValue : serverJSONValue.upd_sortColumns
                };

                currentClientValue.sortColumns = serverJSONValue.upd_sortColumns;
            }

            if (serverJSONValue.upd_selectedRowIndexes !== undefined) {
                if (hasListeners) {
                    notificationParamForListeners.selectedRowIndexesChanged = {
                        oldValue : currentClientValue.selectedRowIndexes,
                        newValue : serverJSONValue.upd_selectedRowIndexes
                    };
                    if (serverJSONValue.upd_userSetSelection !== undefined) {
                        notificationParamForListeners.userSetSelection = true;
                    }
                }
                currentClientValue.selectedRowIndexes = serverJSONValue.upd_selectedRowIndexes;
            }

            if (serverJSONValue.upd_viewPort !== undefined) {
                const viewPortUpdate = serverJSONValue.upd_viewPort;
                const internalState: FoundsetTypeInternalState = currentClientValue.getInternalState();

                if (viewPortUpdate.startIndex !== undefined && currentClientValue.viewPort.startIndex !== viewPortUpdate.startIndex) {
                    if (hasListeners) notificationParamForListeners.viewPortStartIndexChanged = {
                            oldValue : currentClientValue.viewPort.startIndex,
                            newValue : viewPortUpdate.startIndex
                    };

                    currentClientValue.viewPort.startIndex = viewPortUpdate.startIndex;
                }
                if (viewPortUpdate.size !== undefined && currentClientValue.viewPort.size !== viewPortUpdate.size) {
                    if (hasListeners) notificationParamForListeners.viewPortSizeChanged = {
                        oldValue : currentClientValue.viewPort.size,
                        newValue : viewPortUpdate.size
                    };

                    currentClientValue.viewPort.size = viewPortUpdate.size;
                }
                if (viewPortUpdate.rows !== undefined) {
                    const oldRows = currentClientValue.viewPort.rows.slice(); // create shallow copy of old rows as ref. will be the same otherwise
                    currentClientValue.viewPort.rows =
                        this.viewportService.updateWholeViewport(currentClientValue.viewPort.rows, internalState, viewPortUpdate.rows,
                                                        viewPortUpdate[ConverterService.CONVERSION_CL_SIDE_TYPE_KEY], undefined, internalState.propertyContextCreator,
                                                        false, () => new RowValue(newValue));

                    if (hasListeners) notificationParamForListeners.viewportRowsCompletelyChanged = {
                        oldValue : oldRows,
                        newValue : currentClientValue.viewPort.rows
                    };
                } else if (viewPortUpdate.upd_rows !== undefined) {
                    this.viewportService.updateViewportGranularly(currentClientValue.viewPort.rows, internalState,
                                            viewPortUpdate.upd_rows, undefined, internalState.propertyContextCreator, false,
                                            () => new RowValue(newValue));

                    if (hasListeners) {
                        // viewPortUpdate[UPDATE_PREFIX + ROWS] was already prepared for listeners by this.viewportModule.updateViewportGranularly
                        notificationParamForListeners.viewportRowsUpdated = viewPortUpdate.upd_rows;
                    }
                }
            }

            const handledClientRequests = serverJSONValue.handledClientReqIds;
            if (handledClientRequests !== undefined) delete serverJSONValue.handledClientReqIds; // make sure it does not end up in the actual value if this is a full value update

            // if it's a no-op ('n' below), ignore it (sometimes server asks a prop. to send changes even though it has none to send);
            // if it has serverJSONValue.serverSize !== undefined that means a full value has been sent from server; so no granular updates above
            if (!serverJSONValue.n && serverJSONValue.serverSize !== undefined) {
                // not updates - so the whole thing was received

                let internalState: FoundsetTypeInternalState;
                let oldValueShallowCopy: FoundsetFieldsOnly;

                if (!newValue /* newValue is now already currentValue, see code above, so we are checking current value here */) {
                    newValue = new FoundsetValue(propertyContext, this.sabloDeferHelper, this.viewportService, this.log, this.sabloService);
                    internalState = newValue.getInternalState();
                } else {
                    // reuse old value; but make a shallow copy of the old value to give as oldValue to the listener
                    internalState = newValue.getInternalState();
                    oldValueShallowCopy = new FoundsetFieldsOnly(newValue);
                }

                for (const propName of Object.keys(serverJSONValue)) {
                    newValue[propName] = serverJSONValue[propName];
                }

                // convert data if needed - specially done for Date send/receive as the rest are primitives anyway in case of foundset
                // relocate conversion info in internal state and convert
                newValue.viewPort.rows = this.viewportService.updateWholeViewport([] /* this is a full viewport replace; no need to give old/currentClientValue rows here I think */,
                        internalState, newValue.viewPort.rows, newValue.viewPort[ConverterService.CONVERSION_CL_SIDE_TYPE_KEY],
                        undefined, internalState.propertyContextCreator, false, () => new RowValue(newValue));
                delete newValue.viewPort[ConverterService.CONVERSION_CL_SIDE_TYPE_KEY];

                if (hasListeners) notificationParamForListeners.fullValueChanged = { oldValue : oldValueShallowCopy, newValue };
            }

            if (handledClientRequests != undefined) {
                // array of { id: ...int..., value: ...boolean... } which says if a req. was handled successfully by server or not
                const internalState = currentClientValue.getInternalState();

                handledClientRequests.forEach((handledReq) => {
                    const defer = this.sabloDeferHelper.retrieveDeferForHandling(handledReq.id, internalState);
                    if (defer) {
                        const promise: RequestInfoPromise<any> = defer.promise;
                        if (hasListeners && promise.requestInfo) {
                            if (!requestInfos) requestInfos = [];
                            requestInfos.push(promise.requestInfo);
                        }
                        if (defer === internalState.selectionUpdateDefer) {
                            this.sabloService.resolveDeferedEvent(handledReq.id, currentClientValue.selectedRowIndexes, handledReq.value);
                            delete internalState.selectionUpdateDefer;
                        } else {
                            this.sabloService.resolveDeferedEvent(handledReq.id, undefined, handledReq.value);
                        }
                    }
                });
            }
        }

        // restore smart watches and proxy notifiers; server side send changes are now applied
        if (newValue) newValue.getInternalState().ignoreChanges = false;

        this.log.spam(this.log.buildMessage(() => ('svy foundset * updates or value received from server; new viewport and server size (' +
                        (newValue ? newValue.viewPort.startIndex + ', ' + newValue.viewPort.size + ', ' +
                        newValue.serverSize + ', ' + JSON.stringify(newValue.selectedRowIndexes) : newValue) + ')')));
        if (notificationParamForListeners && Object.keys(notificationParamForListeners).length > 0) {
            this.log.spam(this.log.buildMessage(() => ('svy foundset * firing founset listener notifications...')));

            const currentRequestInfo = this.sabloService.getCurrentRequestInfo();
            if (currentRequestInfo) {
                if (!requestInfos) requestInfos = [];
                requestInfos.push(currentRequestInfo);
            }
            if (requestInfos) notificationParamForListeners.requestInfos = requestInfos;

            // use previous (current) value as newValue might be undefined/null and the listeners would be the same anyway
            currentClientValue.getInternalState().fireChanges(notificationParamForListeners);
        }

        return newValue;
    }

    public fromClientToServer(newClientData: FoundsetValue, _oldClientData: FoundsetValue, _propertyContext: IPropertyContext): [any, FoundsetValue] {
        if (newClientData) {
            const newDataInternalState = newClientData.getInternalState();
            if (newDataInternalState.hasChanges()) {
                const tmp = newDataInternalState.requests;
                newDataInternalState.clearChanges();
                return [tmp, newClientData];
            }
        }
        return [[], newClientData];
    }

}

export class FoundsetValue implements IChangeAwareValue, IFoundset, IUIDestroyAwareValue {

    /**
     * An identifier that allows you to use this foundset via the 'foundsetRef' and
     * 'record' types.
     *
     * 'record' and 'foundsetRef' .spec types use it to be able to send RowValue
     * and FoundsetValue instances as record/foundset references on server (so
     * if an argument or property is typed as one of those in .spec file).
     *
     * In reverse, if a 'foundsetRef' type sends a foundset from server to client
     * (for example as a return value of callServerSideApi) it will translate to
     * this identifier on client (so you can use it to find the actual foundset
     * property in the model, if server side script put it in the model as well).
     */
    public foundsetId: number;

    /**
     * the size of the foundset on server (so not necessarily the total record count
     * in case of large DB tables)
     */
    public serverSize: number;

    /**
     * this is the data you need to have loaded on client (just request what you need via provided
     * loadRecordsAsync loadExtraRecordsAsync, etc)
     */
    public viewPort: Viewport;

    /**
     * array of selected records in foundset; indexes can be out of current
     * viewPort as well
     */
    public selectedRowIndexes: number[];

    /**
     * sort string of the foundset, the same as the one used in scripting for
     * foundset.sort and foundset.getCurrentSort. Example: 'orderid asc'.
     */
    public sortColumns: string;

    /**
     * the multiselect mode of the server's foundset; if this is false,
     * selectedRowIndexes can only have one item in it
     */
    public multiSelect = false;

    /**
     * the findMode state of the server's foundset
     */    
    public findMode = false;

    /**
     * if the foundset is large and on server-side only part of it is loaded (so
     * there are records in the foundset beyond 'serverSize') this is set to true;
     * in this way you know you can load records even after 'serverSize' (requesting
     * viewport to load records at index serverSize-1 or greater will load more
     * records in the foundset)
     */
    public hasMoreRows: boolean;

    /**
     * columnFormats is only present if you specify
     * "provideColumnFormats": true inside the .spec file for this foundset property;
     * it gives the default column formatting that Servoy would normally use for
     * each column of the viewport - which you can then also use in the
     * browser yourself; keys are the dataprovider names and values are objects that contain
     * the format contents
     */
    public columnFormats?: Record<string, any>;

    private __internalState: FoundsetTypeInternalState;

    constructor(propertyContext: IPropertyContext, sabloDeferHelper: SabloDeferHelper,
            viewportService: ViewportService, private log: LoggerService, protected sabloService: SabloService) {
        this.__internalState = new FoundsetTypeInternalState(propertyContext, log, sabloDeferHelper, viewportService, sabloService);
    }

    // PUBLIC API to components follows; make it 'smart'

    /** Normally you do not need this in component code - it is used internally by the foundsetRef spec. type. */
    public getId(): number {
        // conversion to server needs this in case it is sent to handler or server side internalAPI calls as argument of type "foundsetRef"
        return this.foundsetId;
    }

    public loadRecordsAsync(startIndex: number, size: number): RequestInfoPromise<any> {
        this.log.spam(this.log.buildMessage(() => ('svy foundset * loadRecordsAsync requested with (' + startIndex + ', ' + size + ')')));
        if (isNaN(startIndex) || isNaN(size)) throw new Error('loadRecordsAsync: start or size are not numbers (' + startIndex + ',' + size + ')');

        const req = {newViewPort: {startIndex, size}};
        const requestID = this.__internalState.sabloDeferHelper.getNewDeferId(this.__internalState);
        req[ViewportService.ID_KEY] = requestID;
        this.__internalState.requests.push(req);

        this.__internalState.notifyChangeListener();
        return this.__internalState.deferred[requestID].defer.promise;
    }

    public loadExtraRecordsAsync(negativeOrPositiveCount: number, dontNotifyYet?: boolean): RequestInfoPromise<any> {
        this.log.spam(this.log.buildMessage(() => ('svy foundset * loadExtraRecordsAsync requested with (' + negativeOrPositiveCount + ', ' + dontNotifyYet + ')')));
        if (isNaN(negativeOrPositiveCount)) throw new Error('loadExtraRecordsAsync: extrarecords is not a number (' + negativeOrPositiveCount + ')');

        const req = { loadExtraRecords: negativeOrPositiveCount };
        const requestID = this.__internalState.sabloDeferHelper.getNewDeferId(this.__internalState);
        req[ViewportService.ID_KEY] = requestID;
        this.__internalState.requests.push(req);

        if (!dontNotifyYet) this.__internalState.notifyChangeListener();
        return this.__internalState.deferred[requestID].defer.promise;
    }

    public loadLessRecordsAsync(negativeOrPositiveCount: number, dontNotifyYet?: boolean): RequestInfoPromise<any> {
        this.log.spam(this.log.buildMessage(() => ('svy foundset * loadLessRecordsAsync requested with (' + negativeOrPositiveCount + ', ' + dontNotifyYet + ')')));
        if (isNaN(negativeOrPositiveCount)) throw new Error('loadLessRecordsAsync: lessrecords is not a number (' + negativeOrPositiveCount + ')');

        const req = { loadLessRecords: negativeOrPositiveCount };
        const requestID = this.__internalState.sabloDeferHelper.getNewDeferId(this.__internalState);
        req[ViewportService.ID_KEY] = requestID;
        this.__internalState.requests.push(req);

        if (!dontNotifyYet) this.__internalState.notifyChangeListener();
        return this.__internalState.deferred[requestID].defer.promise;
    }

    public notifyChanged() {
        this.log.spam(this.log.buildMessage(() => ('svy foundset * notifyChanged called')));
        if (this.__internalState.hasChanges()) this.__internalState.notifyChangeListener();
    }

    public sort(columns: any): RequestInfoPromise<any> {
        this.log.spam(this.log.buildMessage(() => ('svy foundset * sort requested with ' + JSON.stringify(columns))));
        const req = { sort: columns };
        const requestID = this.__internalState.sabloDeferHelper.getNewDeferId(this.__internalState);
        req[ViewportService.ID_KEY] = requestID;
        this.__internalState.requests.push(req);
        this.__internalState.notifyChangeListener();
        return this.__internalState.deferred[requestID].defer.promise;
    }

    public setPreferredViewportSize(size: number, sendSelectionViewportInitially?: boolean, initialSelectionViewportCentered?: boolean): void {
        this.log.spam(this.log.buildMessage(() => ('svy foundset * setPreferredViewportSize called with (' + size + ', ' + sendSelectionViewportInitially + ', '
                                                        + initialSelectionViewportCentered + ')')));
        if (isNaN(size)) throw new Error('setPreferredViewportSize(...): illegal argument; size is not a number (' + size + ')');
        const request: any = { preferredViewportSize: size };
        if (sendSelectionViewportInitially !== undefined) request.sendSelectionViewportInitially = !!sendSelectionViewportInitially;
        if (initialSelectionViewportCentered !== undefined) request.initialSelectionViewportCentered = !!initialSelectionViewportCentered;
        this.__internalState.requests.push(request);
        this.__internalState.notifyChangeListener();
    }

    public requestSelectionUpdate(tmpSelectedRowIdxs: number[]): RequestInfoPromise<any> {
        this.log.spam(this.log.buildMessage(() => ('svy foundset * requestSelectionUpdate called with ' + JSON.stringify(tmpSelectedRowIdxs))));
        if (this.__internalState.selectionUpdateDefer) {
            this.__internalState.selectionUpdateDefer.reject('Selection change defer cancelled because we are already sending another selection to server.');
        }
        delete this.__internalState.selectionUpdateDefer;

        const msgId = this.__internalState.sabloDeferHelper.getNewDeferId(this.__internalState);
        this.__internalState.selectionUpdateDefer = this.__internalState.deferred[msgId].defer;

        const req = {newClientSelectionRequest: tmpSelectedRowIdxs, selectionRequestID: msgId};
        req[ViewportService.ID_KEY] = msgId;
        this.__internalState.requests.push(req);
        this.__internalState.notifyChangeListener();

        return this.__internalState.selectionUpdateDefer.promise.finally(() => {
            delete this.__internalState.selectionUpdateDefer;
        })
    }
    
    /**
     * @deprecated please use columnDataChangedByRowId(...) instead.
     */
    public updateViewportRecord(rowID: string, columnID: string, newValue: any, oldValue: any): void {
        this.columnDataChangedByRowId(rowID, columnID, newValue, oldValue);
    }

    public columnDataChangedByRowId(rowID: string, columnName: string, newValue: any, oldValue: any): RequestInfoPromise<any> {
        this.log.spam(this.log.buildMessage(() => ('svy foundset * columnDataChangedByRowId requested with ("' + rowID + '", ' + columnName + ', ' + newValue)));
        return this.__internalState.viewportService.sendCellChangeToServerBasedOnRowId(this.viewPort.rows, this.__internalState, this.__internalState, rowID, columnName,
                                        this.__internalState.propertyContextCreator, newValue, oldValue);
    }

    public columnDataChanged(rowIndex: number, columnName: string, newValue: any, oldValue?: any): RequestInfoPromise<any> {
        this.log.spam(this.log.buildMessage(() => ('svy foundset * columnDataChanged requested with (' + rowIndex + ', ' + columnName + ', ' + newValue)));
        return this.columnDataChangedByRowId(this.viewPort.rows[rowIndex]._svyRowId, columnName, newValue, oldValue);
    }

    // eslint-disable-next-line @typescript-eslint/ban-types
    public getRecordRefByRowID(rowID: string): RecordRefForServer {
        if (rowID) {
            return RecordRefType.generateRecordRef(rowID, this.foundsetId);
        }

        return null;
    }

    /**
     * Adds a change listener that will get triggered when server sends changes for this foundset.
     *
     * @see SabloService.addIncomingMessageHandlingDoneTask if you need your code to execute after
     * all properties that were linked to this foundset get their changes applied
     * you can use WebsocketSession.addIncomingMessageHandlingDoneTask.
     * @param listener the listener to register.
     * @return a listener unregister function
     */
    public addChangeListener(listener: (change: FoundsetChangeEvent) => void): () => void {
        return this.__internalState.addChangeListener(listener);
    }

    public removeChangeListener(listener: (change: FoundsetChangeEvent) => void) {
        this.__internalState.removeChangeListener(listener);
    }

    /** do not call this methods from component/service impls.; this state is meant to be used only by the property type impl. */
    getInternalState(): FoundsetTypeInternalState {
        return this.__internalState;
    }

    uiDestroyed(): void{
        this.__internalState.sabloDeferHelper.cancelAll(this.getInternalState());
        delete this.__internalState.selectionUpdateDefer;
    }
}

class FoundsetTypeInternalState extends FoundsetViewportState implements IDeferedState {

    deferred: { [key: string]: { defer: Deferred<any>; timeoutId: number } };
    timeoutRejectLogPrefix: string;
    selectionUpdateDefer: Deferred<any>;
    propertyContextCreator: IPropertyContextCreatorForRow;

    unwatchSelection: () => void;

    constructor(propertyContext: IPropertyContext, log: LoggerService, public readonly sabloDeferHelper: SabloDeferHelper, public readonly viewportService: ViewportService,
        protected sabloService: SabloService) {
        super(undefined, log, sabloService);

        this.propertyContextCreator = {
            // currently foundset prop columns always have foundset prop's pushToServer so only one property context needed
            withRowValueAndPushToServerFor: (_rowValue: any, _propertyName: string): IPropertyContext => propertyContext
        } as IPropertyContextCreatorForRow;

        this.sabloDeferHelper.initInternalStateForDeferring(this, 'svy foundset * ');
    }

    init(deferred: { [key: string]: { defer: Deferred<any>; timeoutId: number } }, timeoutRejectLogPrefix: string) {
        this.deferred = deferred;
        this.timeoutRejectLogPrefix = timeoutRejectLogPrefix;
    }

    addChangeListener(listener: FoundsetChangeListener): () => void {
        return super.addChangeListener(listener);
    }

    removeChangeListener(listener: FoundsetChangeListener) {
        super.removeChangeListener(listener);
    }

    fireChanges(changes: FoundsetChangeEvent): void {
        super.fireChanges(changes);
    }

}

export class RowValue {

   [columnName: string]: any;
   _svyRowId: string;
   private readonly _foundset: FoundsetValue;

    constructor(foundset: FoundsetValue) {
        // make foundset private member non-iterable in JS world
        if (Object.defineProperty) {
            // try to avoid unwanted iteration/non-intended interference over the private property state
            Object.defineProperty(this, '_foundset', {
                configurable: false,
                enumerable: false,
                writable: false,
                value: foundset
            });
        } else this._foundset = foundset;
    }

    public getId(): string {
        // conversion to server needs this in case it is sent to handler or server side internalAPI calls as argument of type "recordRef"
        return this._svyRowId;
    }

    public getFoundset(): FoundsetValue {
        // conversion to server needs this in case it is sent to handler or server side internalAPI calls as argument of type "recordRef"
        return this._foundset;
    }

}

interface Viewport {
    startIndex: number;
    size: number;
    rows: RowValue[];
}

class FoundsetFieldsOnly implements IFoundsetFieldsOnly {

    foundsetId: number;
    serverSize: number;
    viewPort: ViewPort;
    selectedRowIndexes: number[];
    sortColumns: string;
    multiSelect: boolean;
    findMode: boolean;
    hasMoreRows: boolean;
    columnFormats?: Record<string, any>;

    constructor(foundsetToShallowCopy: FoundsetValue) {
        this.foundsetId = foundsetToShallowCopy.foundsetId;
        this.serverSize = foundsetToShallowCopy.serverSize;
        this.viewPort = foundsetToShallowCopy.viewPort;
        this.selectedRowIndexes = foundsetToShallowCopy.selectedRowIndexes;
        this.sortColumns = foundsetToShallowCopy.sortColumns;
        this.multiSelect = foundsetToShallowCopy.multiSelect;
        this.findMode = foundsetToShallowCopy.findMode;
        this.hasMoreRows = foundsetToShallowCopy.hasMoreRows;
        this.columnFormats = foundsetToShallowCopy.columnFormats;
    }

}

interface ServerSentJSONForFoundset {

    serverSize?: number;
    foundsetId?: number;
    sortColumns?: string;
    selectedRowIndexes?: number[];
    multiSelect?: boolean;
    hasMoreRows?: boolean;
    columnFormats?: Record<string, any>;
    handledClientReqIds?: [{ id: number; value: any }];
    n?: boolean; // NO_OP
    viewPort?: {
        startIndex?: number;
        size?: number;
        rows?: any[];
        _T?: ConversionInfoFromServerForViewport;
    };

    upd_serverSize?: number;
    upd_hasMoreRows?: boolean;
    upd_multiSelect?: boolean;
    upd_foundsetId?: number;
    upd_foundsetDefinition?: boolean;
    upd_columnFormats?: Record<string, any>;
    upd_sortColumns?: string;
    upd_selectedRowIndexes?: number[];
    upd_userSetSelection?: boolean;
    upd_viewPort?: {
        startIndex?: number;
        size?: number;
        rows?: any[];
        upd_rows?: RowUpdate[];
        _T?: ConversionInfoFromServerForViewport;
    };

}
