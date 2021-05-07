import { Injectable } from '@angular/core';

import { IterableDiffers, IterableDiffer } from '@angular/core';
import { Observable } from 'rxjs';
import { LoggerFactory, LoggerService } from '@servoy/public';


@Injectable()
export class SpecTypesService {

    private registeredTypes = new Map<string, typeof BaseCustomObject>();
    private log: LoggerService;

    constructor(logFactory: LoggerFactory) {
        this.log = logFactory.getLogger('SpecTypesService');
    }
    createType(name: string): BaseCustomObject {
        const classRef = this.registeredTypes.get(name);
        if (classRef) {
            return new classRef();
        }
        this.log.warn('returning just the basic custom object for  ' + name + ' none of the properties will be monitored');
        return new BaseCustomObject();
    }

    enhanceArrayType<T>(array: Array<T>, iterableDiffers: IterableDiffers): ICustomArray<T> {
        if (!instanceOfChangeAwareValue(array)) {
            array['stateHolder'] = new ArrayState(array, iterableDiffers);
            Object.defineProperty(array, 'getStateHolder', {
                enumerable: false,
                value() {
                    return this.stateHolder;
                }
            });
            Object.defineProperty(array, 'markForChanged', {
                enumerable: false,
                value() {
                    this.stateHolder.notifyChangeListener();
                }
            });
            array['stateHolder'].initDiffer();
        }
        return array as ICustomArray<T>;
    }

    registerType(name: string, classRef: typeof BaseCustomObject) {
        this.registeredTypes.set(name, classRef);
    }

    guessType(val: any): string {
        let guess = null;

        if (instanceOfCustomArray(val)) {
            guess = 'JSON_arr';
        } else if (instanceOfBaseCustomObject(val)) {
            guess = 'JSON_obj';
        } // else TODO do any other types need guessing?
        //        else { // try to find it in types?
        //            this.registeredTypes.forEach(function(typeConstructorValue, typeNameKey) {
        //                if (val instanceof typeConstructorValue) guess = typeNameKey; // this wouldn't return the converter name like 'JSON_obj' but rather the actual name from spec
        //                of the custom type like "(...).tab"
        //            });
        //        }
        return guess;
    }
}

export function isChanged(now, prev, conversionInfo) {
    if ((typeof conversionInfo === 'string' || typeof conversionInfo === 'number') && instanceOfChangeAwareValue(now)) {
        return now.getStateHolder().hasChanges();
    }

    if (now === prev) return false;
    if (now && prev) {
        if (now instanceof Array) {
            if (prev instanceof Array) {
                if (now.length !== prev.length) return true;
            } else {
                return true;
            }
        }
        if (now instanceof Date) {
            if (prev instanceof Date) {
                return now.getTime() !== prev.getTime();
            }
            return true;
        }

        if ((now instanceof Object) && (prev instanceof Object)) {
            // first build up a list of all the properties both have.
            const fulllist = this.getCombinedPropertyNames(now, prev);
            for (const prop in fulllist) {
                // ng repeat creates a child scope for each element in the array any scope has a $$hashKey property which must be ignored since it is not part of the model
                if (prev[prop] !== now[prop]) {
                    if (prop === '$$hashKey') continue;
                    if (typeof now[prop] === 'object') {
                        if (isChanged(now[prop], prev[prop], conversionInfo ? conversionInfo[prop] : undefined)) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            }
            return false;
        }
    }
    return true;
}

export const instanceOfChangeAwareValue = (obj: any): obj is IChangeAwareValue =>
    obj != null && (obj).getStateHolder instanceof Function;


export const instanceOfCustomArray = <T>(obj: any): obj is ICustomArray<T> =>
    instanceOfChangeAwareValue(obj) && (obj as ICustomArray<T>).markForChanged instanceof Function;


export const instanceOfBaseCustomObject = (obj: any): obj is BaseCustomObject =>
    instanceOfChangeAwareValue(obj) && (obj).getStateHolder() instanceof BaseCustomObjectState;


export interface IChangeAwareValue {
    getStateHolder(): ChangeAwareState;
}

export interface ICustomObject extends IChangeAwareValue {
    getStateHolder(): BaseCustomObjectState;
}

export interface ICustomArray<T> extends Array<T>, IChangeAwareValue {
    getStateHolder(): ArrayState;
    markForChanged(): void;
}

export interface IValuelist extends Array<{ displayValue: string; realValue: any }> {
    filterList(filterString: string): Observable<any>;
    getDisplayValue(realValue: any): Observable<any>;
    hasRealValues(): boolean;
    isRealValueDate(): boolean;
}

export interface IFoundsetFieldsOnly {

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
    columnFormats: Record<string, any>;
}

export interface IFoundset extends IFoundsetFieldsOnly {

    /**
     * Request a change of viewport bounds from the server; the requested data will be loaded
     * asynchronously in 'viewPort'
     *
     * @param startIndex the index that you request the first record in "viewPort.rows" to have in
     *                   the real foundset (so the beginning of the viewPort).
     * @param size the number of records to load in viewPort.
     *
     * @return a promise that will get resolved when the requested records arrived browser-
     *                   side. As with any promise you can register success, error callbacks, finally, ...
     */
    loadRecordsAsync(startIndex: number, size: number): Promise<any>;

    /**
     * Request more records for your viewPort; if the argument is positive more records will be
     * loaded at the end of the 'viewPort', when negative more records will be loaded at the beginning
     * of the 'viewPort' - asynchronously.
     *
     * @param negativeOrPositiveCount the number of records to extend the viewPort.rows with before or
     *                                after the currently loaded records.
     * @param dontNotifyYet if you set this to true, then the load request will not be sent to server
     *                      right away. So you can queue multiple loadLess/loadExtra before sending them
     *                      to server. If false/undefined it will send this (and any previously queued
     *                      request) to server. See also notifyChanged(). See also notifyChanged().
     *
     * @return a promise that will get resolved when the requested records arrived browser-
     *                   side. As with any promise you can register success, error callbacks, finally, ...
     *                   That allows custom component to make sure that loadExtra/loadLess calls from
     *                   client do not stack on not yet updated viewports to result in wrong bounds.
     */
    loadExtraRecordsAsync(negativeOrPositiveCount: number, dontNotifyYet?: boolean): Promise<any>;

    /**
     * Request a shrink of the viewport; if the argument is positive the beginning of the viewport will
     * shrink, when it is negative then the end of the viewport will shrink - asynchronously.
     *
     * @param negativeOrPositiveCount the number of records to shrink the viewPort.rows by before or
     *                                after the currently loaded records.
     * @param dontNotifyYet if you set this to true, then the load request will not be sent to server
     *                      right away. So you can queue multiple loadLess/loadExtra before sending them
     *                      to server. If false/undefined it will send this (and any previously queued
     *                      request) to server. See also notifyChanged(). See also notifyChanged().
     *
     * @return a promise that will get resolved when the requested records arrived browser
     *                   -side. As with any promise you can register success, error callbacks, finally, ...
     *                   That allows custom component to make sure that loadExtra/loadLess calls from
     *                   client do not stack on not yet updated viewports to result in wrong bounds.
     */
    loadLessRecordsAsync(negativeOrPositiveCount: number, dontNotifyYet?: boolean): Promise<any>;

    /**
     * If you queue multiple loadExtraRecordsAsync and loadLessRecordsAsync by using dontNotifyYet = true
     * then you can - in the end - send all these requests to server (if any are queued) by calling
     * this method. If no requests are queued, calling this method will have no effect.
     */
    notifyChanged(): void;

    /**
     * Sort the foundset by the dataproviders/columns identified by sortColumns.
     *
     * The name property of each sortColumn can be filled with the dataprovider name the foundset provides
     * or specifies. If the foundset is used with a component type (like in table-view) then the name is
     * the name of the component on who's first dataprovider property the sort should happen. If the
     * foundset is used with another foundset-linked property type (dataprovider/tagstring linked to
     * foundsets) then the name you should give in the sortColumn is that property's 'idForFoundset' value
     * (for example a record 'dataprovider' property linked to the foundset will be an array of values
     * representing the viewport, but it will also have a 'idForFoundset' prop. that can be used for
     * sorting in this call; this 'idForFoundset' was added in version 8.0.3).
     *
     * @param sortColumns an array of JSONObjects { name : dataprovider_id,
     *                    direction : sortDirection }, where the sortDirection can be "asc" or "desc".
     * @return (added in Servoy 8.2.1) a promise that will get resolved when the new sort
     *                   will arrive browser-side. As with any promise you can register success, error
     *                   and finally callbacks.
     */
    sort(sortColumns: Array<{ name: string; direction: ('asc' | 'desc') }>): Promise<any>;

    /**
     * Request a selection change of the selected row indexes. Returns a promise that is resolved
     * when the client receives the updated selection from the server. If successful, the array
     * selectedRowIndexes will also be updated. If the server does not allow the selection change,
     * the reject function will get called with the 'old' selection as parameter.
     *
     * If requestSelectionUpdate is called a second time, before the first call is resolved, the
     * first call will be rejected and the caller will receive the string 'canceled' as the value
     * for the parameter serverRows.
     * E.g.: foundset.requestSelectionUpdate([2,3,4]).then(function(serverRows){},function(serverRows){});
     */
    requestSelectionUpdate(selectedRowIdxs: number[]): Promise<any>;

    /**
     * Sets the preferred viewPort options hint on the server for this foundset, so that the next
     * (initial or new) load will automatically return that many rows, even without any of the loadXYZ
     * methods above being called.
     *
     * You can use this when the component size is not known initially and the number of records the
     * component wants to load depends on that. As soon as the component knows how many it wants
     * initially it can call this method.
     *
     * These can also be specified initially using the .spec options "initialPreferredViewPortSize" and
     * "sendSelectionViewportInitially". But these can be altered at runtime via this method as well
     * because they are used/useful in other scenarios as well, not just initially: for example when a
     * related foundset changes parent record, when a search/find is performed and so on.
     *
     * @param preferredSize the preferred number or rows that the viewport should get automatically
     *                      from the server.
     * @param sendViewportWithSelection if this is true, the auto-sent viewport will contain
     *                                            the selected row (if any).
     * @param centerViewportOnSelected if this is true, the selected row will be in the middle
     *                                           of auto-sent viewport if possible. If it is false, then
     *                                           the foundset property type will assume a 'paging'
     *                                           strategy and will send the page that contains the
     *                                           selected row (here the page size is assumed to be
     *                                           preferredSize).
     */
    setPreferredViewportSize(preferredSize: number, sendViewportWithSelection?: boolean, centerViewportOnSelected?: boolean): void;

    /**
     * Receives a client side rowID (taken from myFoundsetProp.viewPort.rows[idx]._svyRowId)
     * and gives a Record reference, an object
     * which can be resolved server side to the exact Record via the 'record' property type;
     * for example if you call a handler or a servoyapi.callServerSideApi(...) and want
     * to give it a Record as parameter and you have the rowID and foundset in your code,
     * you can use this method. E.g: servoyapi.callServerSideApi("doSomethingWithRecord",
     *                     [this.myFoundsetProperty.getRecordRefByRowID(clickedRowId)]);
     *
     * NOTE: if in your component you know the whole row (so myFoundsetProp.viewPort.rows[idx])
     * already - not just the rowID - that you want to send you can just give that directly to the
     * handler/serverSideApi; you do not need to use this method in that case. E.g:
     * // if you have the index inside the viewport
     * servoyapi.callServerSideApi("doSomethingWithRecord",
     *           [this.myFoundsetProperty.viewPort.rows[clickedRowIdx]]);
     * // or if you have the row directly
     * servoyapi.callServerSideApi("doSomethingWithRecord", [clickedRow]);
     *
     * This method has been added in Servoy 8.3.
     */
    getRecordRefByRowID(rowId: string): void;

    /**
     * Adds a change listener that will get triggered when server sends changes for this foundset.
     *
     * @see WebsocketSession.addIncomingMessageHandlingDoneTask if you need your code to execute after all properties that were linked to this foundset
                 get their changes applied you can use WebsocketSession.addIncomingMessageHandlingDoneTask.
     * @param changeListener the listener to register.
     */
    addChangeListener(changeListener: FoundsetChangeListener): () => void;
    removeChangeListener(changeListener: FoundsetChangeListener): void;

    /**
     * Mark the foundset data as changed on the client.
     * If push to server is allowed for this foundset then the changes will be sent to the server, othwerwise the changes are ignored.
     *
     * @param index is the row index (relative to the viewport) where the data change occurred
     * @param columnID the name of the column
     * @param newValue the new value
     * @param oldValue the old value, is optional; the change is ignored if the oldValue is the same as the newValue
     */
    columnDataChanged(index: number, columnID: string, newValue: any, oldValue?: any): void;

}

export interface ViewportChangeEvent {
    // the following keys appear if each of these got updated from server; the names of those
    // keys suggest what it was that changed; oldValue and newValue are the values for what changed
    // (e.g. new server size and old server size) so not the whole foundset property new/old value
    viewportRowsCompletelyChanged?: { oldValue: any[]; newValue: any[] };

    // if we received add/remove/change operations on a set of rows from the viewport, this key
    // will be set; as seen below, it contains "updates" which is an array that holds a sequence of
    // granular update operations to the viewport; the array will hold one or more granular add or remove
    // or update operations;
    // all the "startIndex" and "endIndex" values below are relative to the viewport's state after all
    // previous updates in the array were already processed (so they are NOT relative to the initial state);
    // indexes are 0 based
    viewportRowsUpdated?: ViewportRowUpdates;
}

export interface FoundsetChangeEvent extends ViewportChangeEvent {
    /**
     * If the previous value was non-null, had listeners and a full value update was
     * received from server, this key is set on the change event; if newValue is non-null, it will keep the same reference as before;
     * it will just update it's contents; oldValue will be a dummy shallow copy of old value contents
     */
    fullValueChanged?: { oldValue: IFoundsetFieldsOnly; newValue: IFoundset };

    // the following keys appear if each of these got updated from server; the names of those
    // keys suggest what it was that changed; oldValue and newValue are the values for what changed
    // (e.g. new server size and old server size) so not the whole foundset property new/old value
    serverFoundsetSizeChanged?: { oldValue: number; newValue: number };
    hasMoreRowsChanged?: { oldValue: boolean; newValue: boolean };
    multiSelectChanged?: { oldValue: boolean; newValue: boolean };
    columnFormatsChanged?: { oldValue: Record<string, object>; newValue: Record<string, object> };
    sortColumnsChanged?: { oldValue: string; newValue: string };
    selectedRowIndexesChanged?: { oldValue: number[]; newValue: number[] };
    viewPortStartIndexChanged?: { oldValue: number; newValue: number };
    viewPortSizeChanged?: { oldValue: number; newValue: number };
    userSetSelection?: boolean;
}

export type ChangeListener = (changeEvent: ViewportChangeEvent) => void;
export type FoundsetChangeListener = (changeEvent: FoundsetChangeEvent) => void;

export interface ViewportRowUpdate { type: ChangeType; startIndex: number; endIndex: number }
export type ViewportRowUpdates = ViewportRowUpdate[];

export enum ChangeType  {
    ROWS_CHANGED = 0,

    /**
     * When an INSERT happened but viewport size remained the same, it is
     * possible that some of the rows that were previously at the end of the viewport
     * slided out of it;
     * NOTE: insert signifies an insert into the client viewport, not necessarily
     * an insert in the foundset itself; for example calling "loadExtraRecordsAsync"
     * can result in an insert notification + bigger viewport size notification
     */
    ROWS_INSERTED,

    /**
     * When a DELETE happened inside the viewport but there were more rows available in the
     * foundset after current viewport, it is possible that some of those rows
     * slided into the viewport;
     * NOTE: delete signifies a delete from the client viewport, not necessarily
     * a delete in the foundset itself; for example calling "loadLessRecordsAsync" can
     * result in a delete notification + smaller viewport size notification
     */
    ROWS_DELETED
}

export class BaseCustomObject implements ICustomObject {
    private state = new BaseCustomObjectState();

    constructor() {
        this.state.allChanged = true;
    }

    public getStateHolder() {
        return this.state;
    }

    /**
     *  subclasses can override this to give back the properties that needs to be watched.
     */
    getWatchedProperties(): Array<string> {
        return null;
    }

}

export interface ViewPort {
    startIndex: number;
    size: number;
    rows: ViewPortRow[];
};

export interface ViewPortRow extends Record<string, any> {
    _svyRowId: string;
    _cache?: Map<string, any>;
}

export class ChangeAwareState {

    public allChanged = false;

    private changeListener: () => void;
    private inNotify = false;

    markAllChanged(notifyListener: boolean): void {
        this.allChanged = true;
        if (notifyListener) this.notifyChangeListener();
    }

    hasChanges(): boolean {
        return this.allChanged || this.inNotify;
    }

    setChangeListener(callback: () => void): void {
        this.changeListener = callback;
    }

    public notifyChangeListener(): void {
        this.inNotify = true;
        if (this.changeListener) this.changeListener();
        this.inNotify = false;
    }

}

export class BaseCustomObjectState extends ChangeAwareState {

    // provide a hash that lets arrays that contain custom objects know that the object has changed or not
    private static counter = 0;

    public conversionInfo = {};
    public ignoreChanges = false;


    private change = 0;
    private hash = BaseCustomObjectState.counter++;

    private changedKeys = new Set<string | number>();

    hasChanges() {
        return super.hasChanges() || this.getChangedKeys().size > 0; // leave this as a method call as some subclasses might compute the changedKeys inside getChangedKeys()
    }

    public setPropertyAndHandleChanges(_thisBaseCustoomObject, internalPropertyName, propertyName, value) {
        const oldValue = _thisBaseCustoomObject[internalPropertyName];

        // if the value of this property is changed, mark it as such and notify if needed
        this.markIfChanged(propertyName, value, oldValue);

        // unregister as listener to old value if needed
        this.setChangeListenerToSubValueIfNeeded(oldValue, undefined);

        _thisBaseCustoomObject[internalPropertyName] = value;

        // register as listener to new value if needed
        this.setChangeListenerToSubValueIfNeeded(value, () => {
            this.markIfChanged(propertyName, value, value);
        });

        // this value has changed by reference; so it needs to be fully sent to server - except for when it now arrived from the server and is being set (in which case ignoreChanges is true)
        if (!this.ignoreChanges && instanceOfChangeAwareValue(value)) value.getStateHolder().markAllChanged(false);
    }

    public getChangedKeys(): Set<string | number> {
        return this.changedKeys;
    }

    public clearChanges() {
        this.changedKeys.clear();
        this.allChanged = false;
    }

    public getHashKey(): string {
        return this.hash + '_' + this.change;
    }

    protected markIfChanged(propertyName: string | number, newObject: any, oldObject: any) {
        if (this.testChanged(propertyName, newObject, oldObject)) {
            this.pushChange(propertyName);
            return true;
        }
        return false;
    }

    private setChangeListenerToSubValueIfNeeded(value: any, changeListener: () => void): void {
        if (instanceOfChangeAwareValue(value)) {
            // child is able to handle it's own change mechanism
            value.getStateHolder().setChangeListener(changeListener);
        }
    }

    private pushChange(propertyName) {
        if (this.ignoreChanges) return;
        if (this.changedKeys.size === 0) this.change++;

        if (!this.changedKeys.has(propertyName)) {
            this.changedKeys.add(propertyName);
            this.notifyChangeListener();
        }
    }

    private testChanged(propertyName: string | number, newObject: any, oldObject: any) {
        if (newObject !== oldObject) return true;
        if (typeof newObject == 'object') {
            if (instanceOfChangeAwareValue(newObject)) {
                return newObject.getStateHolder().hasChanges();
            } else {
                return isChanged(newObject, oldObject, this.conversionInfo[propertyName]);
            }
        }
        return false;
    }
}

export class ArrayState extends BaseCustomObjectState {
    private differ: IterableDiffer<Array<any>>;

    constructor(private array: Array<any>, private iterableDiffers: IterableDiffers) {
        super();
        this.allChanged = true;
    }

    public initDiffer() {
        this.differ = this.iterableDiffers.find(this.array).create((index: number, item: any) => {
            if (instanceOfBaseCustomObject(item)) {
                return item.getStateHolder().getHashKey();
            }
            return item;
        });
        this.differ.diff(this.array);
    }

    public clearChanges() {
        super.clearChanges();
        this.initDiffer();
    }

    public getChangedKeys(): Set<string | number> {
        let changes = super.getChangedKeys();
        const arrayChanges = this.differ.diff(this.array);
        if (arrayChanges) {
            let addedOrRemoved = 0;
            arrayChanges.forEachAddedItem((record) => {
                addedOrRemoved++;
                changes.add(record.currentIndex);
            });
            arrayChanges.forEachRemovedItem((record) => {
                addedOrRemoved--;
                changes.add(record.previousIndex);
            });

            arrayChanges.forEachMovedItem((record) => {
                if (instanceOfChangeAwareValue(record.item)) {
                    return record.item.getStateHolder().markAllChanged(false);
                }
            });
            if (addedOrRemoved !== 0) {
                // size changed, for now send whole array
                this.markAllChanged(false);
            } else {
                const changesArray = Array.from(changes);
                changesArray.sort((a: number, b: number) => a - b);
                changes = new Set(changesArray);
            }
        }
        return changes;
    }

}

export interface ColumnRef {
    _svyRowId: string;
    dp: string;
    value: string;
};

