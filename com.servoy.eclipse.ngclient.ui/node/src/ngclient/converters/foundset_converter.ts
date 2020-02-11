import { Injectable } from '@angular/core';
import { IConverter, ConverterService } from '../../sablo/converter.service'
import { LoggerService, LoggerFactory } from '../../sablo/logger.service'
import { IFoundset, ChangeListener, ChangeEvent, FoundsetTypeConstants, ViewPort } from '../../sablo/spectypes.service'
import { SabloService } from '../../sablo/sablo.service';
import { SabloDeferHelper, IDeferedState } from '../../sablo/defer.service';
import { Deferred } from '../../sablo/util/deferred';
import { SabloUtils } from '../../sablo/websocket.service';
import { ViewportService } from '../services/viewport.service';


@Injectable()
export class FoundsetConverter implements IConverter {
    
    static readonly UPDATE_PREFIX = "upd_"; // prefixes keys when only partial updates are send for them
    static readonly SERVER_SIZE = "serverSize";
    static readonly FOUNDSET_ID = "foundsetId";
    static readonly SORT_COLUMNS = "sortColumns";
    static readonly SELECTED_ROW_INDEXES = "selectedRowIndexes";
    static readonly USER_SET_SELECTION = "userSetSelection";
    static readonly MULTI_SELECT = "multiSelect";
    static readonly HAS_MORE_ROWS = "hasMoreRows";
    static readonly VIEW_PORT = "viewPort";
    static readonly START_INDEX = "startIndex";
    static readonly SIZE = "size";
    static readonly ROWS = "rows";
    static readonly COLUMN_FORMATS = "columnFormats";
    static readonly HANDLED_CLIENT_REQUESTS = "handledClientReqIds";
    static readonly ID_KEY = "id";
    static readonly VALUE_KEY = "value";
    static readonly DATAPROVIDER_KEY = "dp";
    static readonly CONVERSIONS = "viewportConversions"; // data conversion info
    static readonly PUSH_TO_SERVER = "w";
    static readonly NO_OP = "n";
    
    private log: LoggerService;
    
    constructor(private converterService: ConverterService, private sabloService: SabloService, private sabloDeferHelper: SabloDeferHelper, private viewportService: ViewportService, private logFactory:LoggerFactory) {
        this.log = logFactory.getLogger("FoundsetPropertyValue");
    }
    
    fromServerToClient( serverJSONValue, currentClientValue?: Foundset, propertyContext?:(propertyName: string)=>any ) : IFoundset {
        let newValue : Foundset = currentClientValue;

        // see if someone is listening for changes on current value; if so, prepare to fire changes at the end of this method
        let hasListeners = (currentClientValue && currentClientValue.state.changeListeners.length > 0);
        let notificationParamForListeners : FoundsetChangeEvent = hasListeners ? {fullValueChanged: undefined, serverFoundsetSizeChanged: undefined, hasMoreRowsChanged: undefined, multiSelectChanged: undefined, columnFormatsChanged: undefined, sortColumnsChanged: undefined, selectedRowIndexesChanged: undefined, viewPortStartIndexChanged: undefined, viewPortSizeChanged: undefined, viewportRowsCompletelyChanged:undefined, viewportRowsUpdated: undefined } : undefined;//TODO ASK was {}
        
        // remove watches so that this update won't trigger them
        //TODO no watches anymore!!! removeAllWatches(currentClientValue);
        
        // see if this is an update or whole value and handle it
        if (!serverJSONValue) {
            newValue = serverJSONValue; // set it to nothing
            if (hasListeners) notificationParamForListeners[FoundsetTypeConstants.NOTIFY_FULL_VALUE_CHANGED] = { oldValue : currentClientValue, newValue : serverJSONValue };
            let oldInternalState = currentClientValue ? currentClientValue.state : undefined; // internal state / $sabloConverters interface
            if (oldInternalState) this.sabloDeferHelper.cancelAll(oldInternalState);

        } else {
            // check for updates
            let updates = false;
            if (serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.SERVER_SIZE] !== undefined) {
                if (hasListeners) notificationParamForListeners[FoundsetTypeConstants.NOTIFY_SERVER_SIZE_CHANGED] = { oldValue : currentClientValue[FoundsetConverter.SERVER_SIZE], newValue : serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.SERVER_SIZE] };
                currentClientValue[FoundsetConverter.SERVER_SIZE] = serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.SERVER_SIZE]; // currentClientValue should always be defined in this case
                updates = true;
            }
            if (serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.PUSH_TO_SERVER] !== undefined) {
                let internalState = currentClientValue.state;
                internalState[FoundsetConverter.PUSH_TO_SERVER] = serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.PUSH_TO_SERVER];
                updates = true;
            }
            if (serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.HAS_MORE_ROWS]!== undefined) {
                if (hasListeners) notificationParamForListeners[FoundsetTypeConstants.NOTIFY_HAS_MORE_ROWS_CHANGED] = { oldValue : currentClientValue[FoundsetConverter.HAS_MORE_ROWS], newValue : serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.HAS_MORE_ROWS] };
                currentClientValue[FoundsetConverter.HAS_MORE_ROWS] = serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.HAS_MORE_ROWS];
                updates = true;
            }    
            if (serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.MULTI_SELECT] !== undefined) {
                if (hasListeners) notificationParamForListeners[FoundsetTypeConstants.NOTIFY_MULTI_SELECT_CHANGED] = { oldValue : currentClientValue[FoundsetConverter.MULTI_SELECT], newValue : serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.MULTI_SELECT] };
                currentClientValue[FoundsetConverter.MULTI_SELECT] = serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.MULTI_SELECT];
                updates = true;
            }
            if (serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.FOUNDSET_ID] !== undefined) {
                currentClientValue[FoundsetConverter.FOUNDSET_ID] = serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.FOUNDSET_ID] ? serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.FOUNDSET_ID] : undefined;
                updates = true;
            }
            if (serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.COLUMN_FORMATS] !== undefined) {
                if (hasListeners) notificationParamForListeners[FoundsetTypeConstants.NOTIFY_COLUMN_FORMATS_CHANGED] = { oldValue : currentClientValue[FoundsetConverter.COLUMN_FORMATS], newValue : serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.COLUMN_FORMATS] };
                currentClientValue[FoundsetConverter.COLUMN_FORMATS] = serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.COLUMN_FORMATS];
                updates = true;
            }
            
            if (serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.SORT_COLUMNS] !== undefined) {
                if (hasListeners) notificationParamForListeners[FoundsetTypeConstants.NOTIFY_SORT_COLUMNS_CHANGED] = { oldValue : currentClientValue[FoundsetConverter.SORT_COLUMNS], newValue : serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.SORT_COLUMNS] };
                currentClientValue[FoundsetConverter.SORT_COLUMNS] = serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.SORT_COLUMNS];
                updates = true;
            }
            
            if (serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.SELECTED_ROW_INDEXES] !== undefined) {
                if (hasListeners) {
                    notificationParamForListeners[FoundsetTypeConstants.NOTIFY_SELECTED_ROW_INDEXES_CHANGED] = { oldValue : currentClientValue[FoundsetConverter.SELECTED_ROW_INDEXES], newValue : serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.SELECTED_ROW_INDEXES] };
                    if (serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.USER_SET_SELECTION] !== undefined) {
                        notificationParamForListeners[FoundsetTypeConstants.NOTIFY_USER_SET_SELECTION] = true;
                    }
                }
                currentClientValue[FoundsetConverter.SELECTED_ROW_INDEXES] = serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.SELECTED_ROW_INDEXES];
                updates = true;
            }
            
            if (serverJSONValue[FoundsetConverter.HANDLED_CLIENT_REQUESTS] !== undefined) {
                let handledRequests = serverJSONValue[FoundsetConverter.HANDLED_CLIENT_REQUESTS]; // array of { id: ...int..., value: ...boolean... } which says if a req. was handled successfully by server or not
                let internalState = currentClientValue.state;
                
                handledRequests.forEach((handledReq) => { 
                     let defer = this.sabloDeferHelper.retrieveDeferForHandling(handledReq[FoundsetConverter.ID_KEY], internalState);
                     if (defer) {
                         if (defer === internalState.selectionUpdateDefer) {
                             if (handledReq[FoundsetConverter.VALUE_KEY]) defer.resolve(currentClientValue[FoundsetConverter.SELECTED_ROW_INDEXES]);
                             else defer.reject(currentClientValue[FoundsetConverter.SELECTED_ROW_INDEXES]);
                             
                             delete internalState.selectionUpdateDefer;
                         } else {
                             if (handledReq[FoundsetConverter.VALUE_KEY]) defer.resolve("");//TODO ask: before it was resolve/reject with no values
                             else defer.reject("");
                         }
                     }
                });
                
                updates = true;
            }
            
            if (serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.VIEW_PORT] !== undefined) {
                updates = true;
                let viewPortUpdate = serverJSONValue[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.VIEW_PORT];
                
                let internalState = currentClientValue.state;
                
                let oldStartIndex = currentClientValue.viewPort.startIndex;
                let oldSize = currentClientValue.viewPort.size;

                if (viewPortUpdate[FoundsetConverter.START_INDEX] !== undefined && currentClientValue[FoundsetConverter.VIEW_PORT][FoundsetConverter.START_INDEX] != viewPortUpdate[FoundsetConverter.START_INDEX]) {
                    if (hasListeners) notificationParamForListeners[FoundsetTypeConstants.NOTIFY_VIEW_PORT_START_INDEX_CHANGED] = { oldValue : currentClientValue.viewPort.startIndex, newValue : viewPortUpdate[FoundsetConverter.START_INDEX] };
                    currentClientValue.viewPort.startIndex = viewPortUpdate[FoundsetConverter.START_INDEX];
                }
                if (viewPortUpdate[FoundsetConverter.SIZE] !== undefined && currentClientValue[FoundsetConverter.VIEW_PORT][FoundsetConverter.SIZE] != viewPortUpdate[FoundsetConverter.SIZE]) {
                    if (hasListeners) notificationParamForListeners[FoundsetTypeConstants.NOTIFY_VIEW_PORT_SIZE_CHANGED] = { oldValue : currentClientValue.viewPort.size, newValue : viewPortUpdate[FoundsetConverter.SIZE] };
                    currentClientValue.viewPort.size = viewPortUpdate[FoundsetConverter.SIZE];
                }
                if (viewPortUpdate[FoundsetConverter.ROWS] !== undefined) {
                    let oldRows = currentClientValue.viewPort.rows;
                    this.viewportService.updateWholeViewport(currentClientValue.viewPort, FoundsetConverter.ROWS, internalState, viewPortUpdate[FoundsetConverter.ROWS],
                            viewPortUpdate[ConverterService.TYPES_KEY] && viewPortUpdate[ConverterService.TYPES_KEY][FoundsetConverter.ROWS] ? viewPortUpdate[ConverterService.TYPES_KEY][FoundsetConverter.ROWS] : undefined, propertyContext);
                    
                    // new rows; set prototype for each row
                    let rows = currentClientValue.viewPort.rows;
                    for (let i = rows.length - 1; i >= 0; i--) {
                        rows[i] = SabloUtils.cloneWithDifferentPrototype(rows[i], internalState.rowPrototype);
                    }
                    
                    if (hasListeners) notificationParamForListeners[FoundsetTypeConstants.NOTIFY_VIEW_PORT_ROWS_COMPLETELY_CHANGED] = { oldValue : oldRows, newValue : currentClientValue.viewPort.rows };
                } else if (viewPortUpdate[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.ROWS] !== undefined) {
                    this.viewportService.updateViewportGranularly(currentClientValue[FoundsetConverter.VIEW_PORT][FoundsetConverter.ROWS], internalState, viewPortUpdate[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.ROWS], viewPortUpdate[ConverterService.TYPES_KEY] && viewPortUpdate[ConverterService.TYPES_KEY][FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.ROWS] ? viewPortUpdate[ConverterService.TYPES_KEY][FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.ROWS] : undefined, propertyContext, false, internalState.rowPrototype);

                    if (hasListeners) {
                        notificationParamForListeners[FoundsetTypeConstants.NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED] = { updates : viewPortUpdate[FoundsetConverter.UPDATE_PREFIX + FoundsetConverter.ROWS] }; // viewPortUpdate[UPDATE_PREFIX + ROWS] was already prepared for listeners by $viewportModule.updateViewportGranularly
                        notificationParamForListeners[FoundsetTypeConstants.NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED][FoundsetTypeConstants.NOTIFY_VIEW_PORT_ROW_UPDATES_OLD_VIEWPORTSIZE] = oldSize; // deprecated since 8.4 where granular updates are pre-processed server side and can be applied directed on client - making this not needed
                    }
                }
            }
            
            // if it's a no-op, ignore it (sometimes server asks a prop. to send changes even though it has none to send)
            if (!updates && !serverJSONValue[FoundsetConverter.NO_OP]) {
               // not updates - so whole thing receive
               // conversion to server in case it is sent to handler or server side internalAPI calls as argument of type "foundsetRef"
               //TODO ?? newValue = SabloUtils.cloneWithDifferentPrototype(serverJSONValue, Foundset.prototype);
                newValue = new Foundset(this.sabloService, this.sabloDeferHelper, this.logFactory, this.converterService, currentClientValue && currentClientValue.state ? currentClientValue.state : new FoundsetState() );
                newValue[SabloUtils.DEFAULT_CONVERSION_TO_SERVER_FUNC] = () => {   return this[FoundsetConverter.FOUNDSET_ID]; };
                Object.keys(serverJSONValue).forEach((prop) => {  
                    newValue[prop] = serverJSONValue[prop];
                });
               if (hasListeners) notificationParamForListeners[FoundsetTypeConstants.NOTIFY_FULL_VALUE_CHANGED] = { oldValue : currentClientValue, newValue : newValue };      
               let internalState = newValue.state;
                
                // conversion of rows to server in case it is sent to handler or server side internalAPI calls as argument of type "foundsetRef"
                internalState.rowPrototype[SabloUtils.DEFAULT_CONVERSION_TO_SERVER_FUNC] = () => {
                    if (this[FoundsetTypeConstants.ROW_ID_COL_KEY])
                    {
                        let recordRef = {};
                        recordRef[FoundsetTypeConstants.ROW_ID_COL_KEY] = this[FoundsetTypeConstants.ROW_ID_COL_KEY];
                        recordRef[FoundsetConverter.FOUNDSET_ID] = newValue[FoundsetConverter.FOUNDSET_ID];
                        return recordRef;
                    }
                    return null
                };
                let rows = newValue.viewPort.rows;
                if (typeof newValue[FoundsetConverter.PUSH_TO_SERVER] !== 'undefined') {
                    internalState[FoundsetConverter.PUSH_TO_SERVER] = newValue[FoundsetConverter.PUSH_TO_SERVER];
                    delete newValue[FoundsetConverter.PUSH_TO_SERVER];
                }

                internalState.requests = [];
                if (currentClientValue && currentClientValue.state)
                {   
                    this.sabloDeferHelper.initInternalStateForDeferringFromOldInternalState(internalState, currentClientValue.state);
                }
                else
                {
                    this.sabloDeferHelper.initInternalStateForDeferring(internalState, "svy foundset * ");
                }   
                // convert data if needed - specially done for Date send/receive as the rest are primitives anyway in case of foundset
                this.viewportService.updateAllConversionInfo(rows, internalState, newValue.viewPort[ConverterService.TYPES_KEY] ? newValue[FoundsetConverter.VIEW_PORT][ConverterService.TYPES_KEY][FoundsetConverter.ROWS] : undefined);
                if (newValue[FoundsetConverter.VIEW_PORT][ConverterService.TYPES_KEY]) {
                    // relocate conversion info in internal state and convert
                    this.converterService.convertFromServerToClient(rows, newValue.viewPort[ConverterService.TYPES_KEY][FoundsetConverter.ROWS], propertyContext);
                    delete newValue[FoundsetConverter.VIEW_PORT][ConverterService.TYPES_KEY];
                }
                // do set prototype after rows are converted
                for (let i = rows.length - 1; i >= 0; i--) {
                    rows[i] = SabloUtils.cloneWithDifferentPrototype(rows[i], internalState.rowPrototype);
                }

                //initialize the property value; make it 'smart'  
                // even if it's a completely new value, keep listeners from old one if there is an old value
                internalState.setChangeListeners(currentClientValue);
                
            }

            // restore/add watches
            //TODO watches????? addBackWatches(newValue, componentScope);
            
            this.log.spam(this.log.buildMessage(() => ("svy foundset * updates or value received from server; new viewport and server size (" + (newValue ? newValue[FoundsetConverter.VIEW_PORT][FoundsetConverter.START_INDEX] + ", " + newValue[FoundsetConverter.VIEW_PORT][FoundsetConverter.SIZE] + ", " + newValue[FoundsetConverter.SERVER_SIZE] + ", " + JSON.stringify(newValue[FoundsetConverter.SELECTED_ROW_INDEXES]) : newValue) + ")")));
            if (notificationParamForListeners && Object.keys(notificationParamForListeners).length > 0) {
                this.log.spam(this.log.buildMessage(() => ("svy foundset * firing founset listener notifications...")));
                // use previous (current) value as newValue might be undefined/null and the listeners would be the same anyway
                currentClientValue.state.fireChanges(notificationParamForListeners);
            }

            return newValue;
        }
    }
    
    fromClientToServer( newClientData: Foundset, oldClientData? ) { 
        if (newClientData) {
            let newDataInternalState = newClientData.state;
            if (newDataInternalState.isChanged()) {
                var tmp = newDataInternalState.requests;
                newDataInternalState.requests = [];
                return tmp;
            }
        }
        return [];
    }
}

export class Foundset implements IFoundset {
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
    multiSelect: boolean;
    
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
    columnFormats: object;
    
    private log: LoggerService;
    
    constructor(private sabloService: SabloService, private sabloDeferHelper: SabloDeferHelper, private logFactory:LoggerFactory, private converterService: ConverterService, public state: FoundsetState, private values?: Array<Object>) {
        this.log = logFactory.getLogger("Foundset");
        //see https://blog.simontest.net/extend-array-with-typescript-965cc1134b3
        //set prototype, since adding a create method is not really working if we have the values
        Object.setPrototypeOf(this, Object.create(Foundset.prototype)); 
    }
    
    public loadRecordsAsync(startIndex:number, size:number): Promise<any> {
        this.log.spam(this.log.buildMessage(() => ("svy foundset * loadRecordsAsync requested with (" + startIndex + ", " + size + ")")));
        if (isNaN(startIndex) || isNaN(size)) throw new Error("loadRecordsAsync: start or size are not numbers (" + startIndex + "," + size + ")");

        let req = {newViewPort: {startIndex : startIndex, size : size}};
        let requestID = this.sabloDeferHelper.getNewDeferId(this.state);
        req[FoundsetConverter.ID_KEY] = requestID;
        this.state.requests.push(req);
        
        if (this.state.changeNotifier) this.state.changeNotifier();
        return this.state.deferred[requestID].defer.promise;
    }
    
    public loadLessRecordsAsync(negativeOrPositiveCount:number, dontNotifyYet:boolean): Promise<any> {
        this.log.spam(this.log.buildMessage(() => ("svy foundset * loadLessRecordsAsync requested with (" + negativeOrPositiveCount + ", " + dontNotifyYet + ")")));
        if (isNaN(negativeOrPositiveCount)) throw new Error("loadLessRecordsAsync: lessrecords is not a number (" + negativeOrPositiveCount + ")");

        let req = { loadLessRecords: negativeOrPositiveCount };
        let requestID = this.sabloDeferHelper.getNewDeferId(this.state);
        req[FoundsetConverter.ID_KEY] = requestID;
        this.state.requests.push(req);

        if (this.state.changeNotifier && !dontNotifyYet) this.state.changeNotifier();
        return this.state.deferred[requestID].defer.promise;
    }
    
    public loadExtraRecordsAsync(negativeOrPositiveCount:number, dontNotifyYet:boolean): Promise<any>{
        this.log.spam(this.log.buildMessage(() => ("svy foundset * loadExtraRecordsAsync requested with (" + negativeOrPositiveCount + ", " + dontNotifyYet + ")")));
        if (isNaN(negativeOrPositiveCount)) throw new Error("loadExtraRecordsAsync: extrarecords is not a number (" + negativeOrPositiveCount + ")");

        let req = { loadExtraRecords: negativeOrPositiveCount };
        let requestID = this.sabloDeferHelper.getNewDeferId(this.state);
        req[FoundsetConverter.ID_KEY] = requestID;
        this.state.requests.push(req);
        
        if (this.state.changeNotifier && !dontNotifyYet) this.state.changeNotifier();
        return this.state.deferred[requestID].defer.promise;
    }
    
    public notifyChanged() {
        this.log.spam(this.log.buildMessage(() => ("svy foundset * notifyChanged called")));
        if (this.state.changeNotifier && this.state.requests.length > 0) this.state.changeNotifier();
    }
    
    public sort(columns: Array<{ name: string, direction: ("asc" | "desc")}> ): Promise<any> {
        this.log.spam(this.log.buildMessage(() => ("svy foundset * sort requested with " + JSON.stringify(columns))));
        let req = {sort: columns};
        let requestID = this.sabloDeferHelper.getNewDeferId(this.state);
        req[FoundsetConverter.ID_KEY] = requestID;
        this.state.requests.push(req);
        if (this.state.changeNotifier) this.state.changeNotifier();
        return this.state.deferred[requestID].defer.promise;
    }
    
    public setPreferredViewportSize(size: number, sendSelectionViewportInitially, initialSelectionViewportCentered) {
        this.log.spam(this.log.buildMessage(() => ("svy foundset * setPreferredViewportSize called with (" + size + ", " + sendSelectionViewportInitially + ", " + initialSelectionViewportCentered + ")")));
        if (isNaN(size)) throw new Error("setPreferredViewportSize(...): illegal argument; size is not a number (" + size + ")");
        let request = { "preferredViewportSize" : size };
        if (sendSelectionViewportInitially !== undefined) request["sendSelectionViewportInitially"] = !!sendSelectionViewportInitially;
        if (initialSelectionViewportCentered !== undefined) request["initialSelectionViewportCentered"] = !!initialSelectionViewportCentered;
        this.state.requests.push(request);
        if (this.state.changeNotifier) this.state.changeNotifier();
    }
    
    public requestSelectionUpdate(tmpSelectedRowIdxs): Promise<any> {
        this.log.spam(this.log.buildMessage(() => ("svy foundset * requestSelectionUpdate called with " + JSON.stringify(tmpSelectedRowIdxs))));
        if (this.state.selectionUpdateDefer) {
            this.state.selectionUpdateDefer.reject("Selection change defer cancelled because we are already sending another selection to server.");
        }
        delete this.state.selectionUpdateDefer;

        let msgId = this.sabloDeferHelper.getNewDeferId(this.state);
        this.state.selectionUpdateDefer = this.state.deferred[msgId].defer;
        
        let req = {newClientSelectionRequest: tmpSelectedRowIdxs, selectionRequestID: msgId};
        req[FoundsetConverter.ID_KEY] = msgId;
        this.state.requests.push(req);
        if (this.state.changeNotifier) this.state.changeNotifier();

        return this.state.selectionUpdateDefer.promise;
    }
    
    public getRecordRefByRowID(rowID) {
        if (rowID)
        {
            let recordRef = {};
            recordRef[FoundsetTypeConstants.ROW_ID_COL_KEY] = rowID;
            recordRef[FoundsetConverter.FOUNDSET_ID] = this.state[FoundsetConverter.FOUNDSET_ID];
            return recordRef;
        }
        return null;
    }
    
    public updateViewportRecord(rowID, columnID, newValue, oldValue) {
        this.log.spam(this.log.buildMessage(() => ("svy foundset * updateRecord requested with (" + rowID + ", " + columnID + ", " + newValue)));
        let r = {};
        r[FoundsetTypeConstants.ROW_ID_COL_KEY] = rowID;
        r[FoundsetConverter.DATAPROVIDER_KEY] = columnID;
        r[FoundsetConverter.VALUE_KEY] = newValue;

        // convert new data if necessary
        let conversionInfo = undefined;
        if(this.state[FoundsetConverter.CONVERSIONS]) {
            for(let idx in this.viewPort.rows) {
                if(this.viewPort.rows[idx][FoundsetTypeConstants.ROW_ID_COL_KEY] == rowID) {
                    conversionInfo = this.state[FoundsetConverter.CONVERSIONS][idx];
                    break;
                }
            }
        }
        if (conversionInfo && conversionInfo[columnID]) r[FoundsetConverter.VALUE_KEY] = this.converterService.convertFromClientToServer(r[FoundsetConverter.VALUE_KEY], conversionInfo[columnID], oldValue);
        else r[FoundsetConverter.VALUE_KEY] = this.converterService.convertClientObject(r[FoundsetConverter.VALUE_KEY]);

        this.state.requests.push({viewportDataChanged: r});
        if (this.state.changeNotifier) this.state.changeNotifier();
    }
    
    /**
     * Adds a change listener that will get triggered when server sends changes for this foundset.
     * 
     * @see $webSocket.addIncomingMessageHandlingDoneTask if you need your code to execute after all properties that were linked to this foundset get their changes applied you can use $webSocket.addIncomingMessageHandlingDoneTask.
     * @param listener the listener to register.
     */
    public addChangeListener(listener: ChangeListener) {
        return this.state.addChangeListener(listener);
    }
    public removeChangeListener(listener: ChangeListener) {
       this.state.removeChangeListener(listener);
    }
}

class FoundsetState implements IDeferedState {
    
    deferred: Object;
    currentMsgId: number;
    timeoutRejectLogPrefix: string;
    changeListeners: ChangeListener[];
    requests = [];
    rowPrototype = {};
    changeNotifier: Function;
    selectionUpdateDefer: Deferred<any>;
    
    init(deferred: Object, currentMsgId: number, timeoutRejectLogPrefix: string) {
        this.deferred = deferred;
        this.currentMsgId = currentMsgId;
        this.timeoutRejectLogPrefix = timeoutRejectLogPrefix;
    }
    
    public addChangeListener(listener: (change: FoundsetChangeEvent) => void) {
        this.changeListeners.push(listener);
        return () => this.removeChangeListener(listener);
    }
    
    public removeChangeListener(listener) {
        let index = this.changeListeners.indexOf(listener);
        if (index > -1) {
            this.changeListeners.splice(index, 1);
        }
    }
    
    public setChangeListeners(currentClientValue: Foundset)
    {
        this.changeListeners = currentClientValue && currentClientValue.state ? currentClientValue.state.changeListeners : [];
    }
    
    public fireChanges(foundsetChanges: FoundsetChangeEvent) {
        for(let i = 0; i < this.changeListeners.length; i++) {
            //TODO needed?? what is componentScope? $webSocket.setIMHDTScopeHintInternal(componentScope);
            this.changeListeners[i](foundsetChanges);
            //TODO $webSocket.setIMHDTScopeHintInternal(undefined);
        }
    }
    
    public setChangeNotifier(changeNotifier) {
        this.changeNotifier = changeNotifier;
    }
    
    public isChanged() {
        return this.requests && (this.requests.length > 0);
    }
}

interface FoundsetChangeEvent extends ChangeEvent {
    // if value was non-null and had a listener attached before, and a full value update was
    // received from server, this key is set; if newValue is non-null, it will automatically get the old
    // value's listeners registered to itself
    // NOTE: it might be easier to rely just on a shallow $watch of the foundset value (to catch even the
    // null to non-null scenario that you still need) and not use NOTIFY_FULL_VALUE_CHANGED at all
    fullValueChanged: { oldValue: object, newValue: object };
 
    // the following keys appear if each of these got updated from server; the names of those
    // constants suggest what it was that changed; oldValue and newValue are the values for what changed
    // (e.g. new server size and old server size) so not the whole foundset property new/old value
    serverFoundsetSizeChanged: { oldValue: number, newValue: number };
    hasMoreRowsChanged:  { oldValue: boolean, newValue: boolean };
    multiSelectChanged:  { oldValue: boolean, newValue: boolean };
    columnFormatsChanged:  { oldValue: object, newValue: object };
    sortColumnsChanged:  { oldValue: string, newValue: string };
    selectedRowIndexesChanged:  { oldValue: number[], newValue: number[] };
    viewPortStartIndexChanged:  { oldValue: number, newValue: number };
    viewPortSizeChanged:  { oldValue: number, newValue: number }
}