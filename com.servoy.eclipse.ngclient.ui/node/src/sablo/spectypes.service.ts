import { Injectable } from '@angular/core';

import { ConverterService } from './converter.service'

import { IterableDiffers, IterableDiffer } from '@angular/core';


@Injectable()
export class SpecTypesService {

    private registeredTypes = new Map<string, typeof BaseCustomObject>();

    createType( name: string ): BaseCustomObject {
        const classRef = this.registeredTypes.get( name );
        if ( classRef ) {
            return new classRef();
        }
        console.log( "returning just the basic custom object for  " + name + " none of the properties will be monitored" );
        return new BaseCustomObject();
    }

    enhanceArrayType<T>( array: Array<T>, iterableDiffers: IterableDiffers ): ICustomArray<T> {
        if ( !instanceOfChangeAwareValue( array ) ) {
            array["stateHolder"] = new ArrayState( array, iterableDiffers );
            Object.defineProperty( array, 'getStateHolder', {
                enumerable: false,
                value: function() { return this.stateHolder }
            } );
            Object.defineProperty( array, 'markForChanged', {
                enumerable: false,
                value: function() { this.stateHolder.notifyChangeListener() }
            } );
            array["stateHolder"].initDiffer();
        }
        return <ICustomArray<T>>array;
    }

    registerType( name: string, classRef: typeof BaseCustomObject, specProperties: Array<string> ) {
        this.registeredTypes.set( name, classRef );
        classRef["__specProperties"] = specProperties;
    }

    getProperties( classRef ): Array<string> {
        return classRef["__specProperties"];
    }

    guessType( val: any ): string {
        let guess = null;

        if ( instanceOfCustomArray( val ) ) {
            guess = "JSON_arr";
        } else if ( instanceOfBaseCustomObject( val ) ) {
            guess = "JSON_obj";
        } // else TODO do any other types need guessing? 
        //        else { // try to find it in types?
        //            this.registeredTypes.forEach(function(typeConstructorValue, typeNameKey) {
        //                if (val instanceof typeConstructorValue) guess = typeNameKey; // this wouldn't return the converter name like 'JSON_obj' but rather the actual name from spec of the custom type like "(...).tab"
        //            });
        //        }
        return guess;
    }
}

export function instanceOfChangeAwareValue( obj: any ): obj is IChangeAwareValue {
    return obj != null && ( <IChangeAwareValue>obj ).getStateHolder instanceof Function;
}

export function instanceOfCustomArray<T>( obj: any ): obj is ICustomArray<T> {
    return instanceOfChangeAwareValue( obj ) && ( <ICustomArray<T>>obj ).markForChanged instanceof Function;
}

export function instanceOfBaseCustomObject( obj: any ): obj is BaseCustomObject {
    return instanceOfChangeAwareValue( obj ) && ( <BaseCustomObject>obj ).getStateHolder() instanceof BaseCustomObjectState;
}

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

export interface IValuelist extends Array<any>  {
    filterList(filterString:string): Promise<any>;
    getDisplayValue(realValue:any): Promise<any>;
    hasRealValues(): boolean;
}

export interface IFoundset {
    /**
     * Request a change of viewport bounds from the server; the requested data will be loaded
     * asynchronously in 'viewPort'
     *
     * @param startIndex the index that you request the first record in "viewPort.rows" to have in
     *                   the real foundset (so the beginning of the viewPort).
     * @param size the number of records to load in viewPort.
     *
     * @return a $q promise that will get resolved when the requested records arrived browser-
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
     * @return a $q promise that will get resolved when the requested records arrived browser-
     *                   side. As with any promise you can register success, error callbacks, finally, ...
     *                   That allows custom component to make sure that loadExtra/loadLess calls from
     *                   client do not stack on not yet updated viewports to result in wrong bounds.
     */
     loadExtraRecordsAsync(negativeOrPositiveCount: number, dontNotifyYet: boolean): Promise<any>;
     
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
     * @return a $q promise that will get resolved when the requested records arrived browser
     *                   -side. As with any promise you can register success, error callbacks, finally, ...
     *                   That allows custom component to make sure that loadExtra/loadLess calls from
     *                   client do not stack on not yet updated viewports to result in wrong bounds.
     */
     loadLessRecordsAsync(negativeOrPositiveCount: number, dontNotifyYet: boolean): Promise<any>;
     
     /**
      * If you queue multiple loadExtraRecordsAsync and loadLessRecordsAsync by using dontNotifyYet = true
      * then you can - in the end - send all these requests to server (if any are queued) by calling
      * this method. If no requests are queued, it calling this method will have no effect.
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
     * @return (added in Servoy 8.2.1) a $q promise that will get resolved when the new sort
     *                   will arrive browser-side. As with any promise you can register success, error
     *                   and finally callbacks.
     */
     sort(sortColumns: Array<{ name: string, direction: ("asc" | "desc") }>): Promise<any>;
     
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
     setPreferredViewportSize(preferredSize: number, sendViewportWithSelection: boolean, centerViewportOnSelected: boolean): void;
     
     /**
     * Receives a client side rowID (taken from myFoundsetProp.viewPort.rows[idx]
     * [$foundsetTypeConstants.ROW_ID_COL_KEY]) and gives a Record reference, an object
     * which can be resolved server side to the exact Record via the 'record' property type;
     * for example if you call a handler or a $scope.svyServoyapi.callServerSideApi(...) and want
     * to give it a Record as parameter and you have the rowID and foundset in your code,
     * you can use this method. E.g: $scope.svyServoyapi.callServerSideApi("doSomethingWithRecord",
     *                     [$scope.model.myFoundsetProp.getRecordRefByRowID(clickedRowId)]);
     *
     * NOTE: if in your component you know the whole row (so myFoundsetProp.viewPort.rows[idx])
     * already - not just the rowID - that you want to send you can just give that directly to the
     * handler/serverSideApi; you do not need to use this method in that case. E.g:
     * // if you have the index inside the viewport
     * $scope.svyServoyapi.callServerSideApi("doSomethingWithRecord",
     *           [$scope.model.myFoundsetProp.viewPort.rows[clickedRowIdx]]);
     * // or if you have the row directly
     * $scope.svyServoyapi.callServerSideApi("doSomethingWithRecord", [clickedRow]);
     *
     * This method has been added in Servoy 8.3.
     */
     getRecordRefByRowID(rowId: string): void;
     
     /**
      * Adds a change listener that will get triggered when server sends changes for this foundset.
      * 
      * @see $webSocket.addIncomingMessageHandlingDoneTask if you need your code to execute after all properties that were linked to this foundset get their changes applied you can use $webSocket.addIncomingMessageHandlingDoneTask.
      * @param changeListener the listener to register.
      */
     addChangeListener(changeListener : ChangeListener) : () => void;
     removeChangeListener(changeListener : ChangeListener) : void;

}

export interface ChangeEvent {
    // the following keys appear if each of these got updated from server; the names of those
    // constants suggest what it was that changed; oldValue and newValue are the values for what changed
    // (e.g. new server size and old server size) so not the whole foundset property new/old value
    viewportRowsCompletelyChanged:  { oldValue: number, newValue: number },
 
    // if we received add/remove/change operations on a set of rows from the viewport, this key
    // will be set; as seen below, it contains "updates" which is an array that holds a sequence of
    // granular update operations to the viewport; the array will hold one or more granular add or remove
    // or update operations;
    // all the "startIndex" and "endIndex" values below are relative to the viewport's state after all
    // previous updates in the array were already processed (so they are NOT relative to the initial state);
    // indexes are 0 based
    viewportRowsUpdated: { updates: ViewportRowUpdates, oldViewportSize: number }
}

export type ChangeListener = (changeEvent: ChangeEvent) => void;

type ViewportRowUpdate = RowsChanged | RowsInserted | RowsDeleted;
export type ViewportRowUpdates = ViewportRowUpdate[];

type RowsChanged = { type: 0, startIndex: number, endIndex: number };

/**
 * When an INSERT happened but viewport size remained the same, it is
 * possible that some of the rows that were previously at the end of the viewport
 * slided out of it; "removedFromVPEnd" gives the number of such rows that were removed
 * from the end of the viewport due to the insert operation;
 * NOTE: insert signifies an insert into the client viewport, not necessarily
 * an insert in the foundset itself; for example calling "loadExtraRecordsAsync"
 * can result in an insert notification + bigger viewport size notification,
 * with removedFromVPEnd = 0
 */
type RowsInserted = { type: 1, startIndex: number, endIndex: number, removedFromVPEnd: number };


/**
 * When a DELETE happened inside the viewport but there were more rows available in the
 * foundset after current viewport, it is possible that some of those rows
 * slided into the viewport; "appendedToVPEnd " gives the number of such rows
 * that were appended to the end of the viewport due to the DELETE operation
 * NOTE: delete signifies a delete from the client viewport, not necessarily
 * a delete in the foundset itself; for example calling "loadLessRecordsAsync" can
 * result in a delete notification + smaller viewport size notification,
 * with appendedToVPEnd = 0
 */                                
type RowsDeleted = { type: 2, startIndex : number, endIndex : number, appendedToVPEnd : number }

export class BaseCustomObject implements ICustomObject {
    private state = new BaseCustomObjectState();

    public getStateHolder() {
        return this.state;
    }

}

export class FoundsetTypeConstants {
    //if you change any of these please also update ChangeEvent and other types in foundset.d.ts and or component.d.ts
      public static readonly ROW_ID_COL_KEY: string = '_svyRowId';
      public static readonly FOR_FOUNDSET_PROPERTY: string = 'forFoundset';

      // listener notification constants follow; prefixed just to separate them a bit from other constants
      public static readonly NOTIFY_FULL_VALUE_CHANGED: string ="fullValueChanged";
      public static readonly NOTIFY_SERVER_SIZE_CHANGED: string ="serverFoundsetSizeChanged";
      public static readonly NOTIFY_HAS_MORE_ROWS_CHANGED: string = "hasMoreRowsChanged";
      public static readonly NOTIFY_MULTI_SELECT_CHANGED: string ="multiSelectChanged";
      public static readonly NOTIFY_COLUMN_FORMATS_CHANGED:string = "columnFormatsChanged";
      public static readonly NOTIFY_SORT_COLUMNS_CHANGED: string ="sortColumnsChanged";
      public static readonly NOTIFY_SELECTED_ROW_INDEXES_CHANGED:string = "selectedRowIndexesChanged";
      public static readonly NOTIFY_USER_SET_SELECTION:string = "userSetSelection";
      public static readonly NOTIFY_VIEW_PORT_START_INDEX_CHANGED: string = "viewPortStartIndexChanged";
      public static readonly NOTIFY_VIEW_PORT_SIZE_CHANGED: string = "viewPortSizeChanged";
      public static readonly NOTIFY_VIEW_PORT_ROWS_COMPLETELY_CHANGED: string = "viewportRowsCompletelyChanged";
      public static readonly NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED: string ="viewportRowsUpdated";
      public static readonly NOTIFY_VIEW_PORT_ROW_UPDATES_OLD_VIEWPORTSIZE:string = "oldViewportSize"; // deprecated since 8.4 where granular updates are pre-processed server side and can be applied directed on client - making this not needed
      public static readonly NOTIFY_VIEW_PORT_ROW_UPDATES: string = "updates";

      // row update types for listener notifications - in case NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED is triggered
      public static readonly ROWS_CHANGED: number = 0;
      public static readonly ROWS_INSERTED: number = 1;
      public static readonly ROWS_DELETED: number = 2;   
  }

export class FoundsetLinkedTypeConstants {
    public static readonly ID_FOR_FOUNDSET = "idForFoundset";
    public static readonly RECORD_LINKED = "recordLinked";
} 

export class ViewPort {
    startIndex: number;
    size: number;
    rows: object[];
}

export class ChangeAwareState {

    private changeListener: () => void;

    protected _allChanged = true;

    markAllChanged( notifyListener: boolean ): void {
        this._allChanged = true;
        if ( notifyListener ) this.notifyChangeListener();
    }

    hasChanges(): boolean {
        return this._allChanged;
    }

    get allChanged() {
        return this._allChanged;
    }

    setChangeListener( callback: () => void ): void {
        this.changeListener = callback;
    }

    public notifyChangeListener(): void {
        if ( this.changeListener ) this.changeListener();
    }

}

export class BaseCustomObjectState extends ChangeAwareState {

    // provide a hash that lets arrays that contain custom objects know that the object has changed or not
    private static counter = 0;
    private change = 0;
    private hash = BaseCustomObjectState.counter++;

    private changedKeys = new Array<string | number>();

    public conversionInfo = {};
    public ignoreChanges = false;

    hasChanges() {
        return super.hasChanges() || this.getChangedKeys().length > 0; // leave this as a method call as some subclasses might compute the changedKeys inside getChangedKeys()
    }

    private setChangeListenerToSubValueIfNeeded( value: any, changeListener: () => void ): void {
        if ( instanceOfChangeAwareValue( value ) ) {
            // child is able to handle it's own change mechanism
            value.getStateHolder().setChangeListener( changeListener );
        }
    }

    public setPropertyAndHandleChanges( _thisBaseCustoomObject, internalPropertyName, propertyName, value ) {
        const oldValue = _thisBaseCustoomObject[internalPropertyName];

        // if the value of this property is changed, mark it as such and notify if needed
        this.markIfChanged( propertyName, value, oldValue );

        // unregister as listener to old value if needed
        this.setChangeListenerToSubValueIfNeeded( oldValue, undefined );

        _thisBaseCustoomObject[internalPropertyName] = value;

        // register as listener to new value if needed
        this.setChangeListenerToSubValueIfNeeded( value, () => {
            this.markIfChanged( propertyName, value, value );
        } );

        // this value has changed by reference; so it needs to be fully sent to server - except for when it now arrived from the server and is being set (in which case ignoreChanges is true)
        if ( !this.ignoreChanges && instanceOfChangeAwareValue( value ) ) value.getStateHolder().markAllChanged( false );
    }


    protected markIfChanged( propertyName: string | number, newObject: any, oldObject: any ) {
        if ( this.testChanged( propertyName, newObject, oldObject ) ) {
            this.pushChange( propertyName );
            return true;
        }
        return false;
    }

    public getChangedKeys(): Array<string | number> {
        return this.changedKeys;
    }

    public clearChanges() {
        this.changedKeys = new Array<string | number>();
        this._allChanged = false;
    }

    private pushChange( propertyName ) {
        if ( this.ignoreChanges ) return;
        if ( this.changedKeys.length == 0 ) this.change++;

        if (this.changedKeys.indexOf(propertyName) < 0) {
            this.changedKeys.push( propertyName );
            this.notifyChangeListener();
        }
    }

    private testChanged( propertyName: string | number, newObject: any, oldObject: any ) {
        if ( newObject !== oldObject ) return true;
        if ( typeof newObject == "object" ) {
            if ( instanceOfChangeAwareValue( newObject ) ) {
                return newObject.getStateHolder().hasChanges();
            } else {
                return ConverterService.isChanged( newObject, oldObject, this.conversionInfo[propertyName] );
            }
        }
        return false;
    }

    public getHashKey(): string {
        return this.hash + "_" + this.change;
    }

}

export class ArrayState extends BaseCustomObjectState {
    private differ: IterableDiffer<Array<any>>;

    constructor( private array: Array<any>, private iterableDiffers: IterableDiffers ) {
        super();
    }

    public initDiffer() {
        this.differ = this.iterableDiffers.find( this.array ).create(( index: number, item: any ) => {
            if ( instanceOfBaseCustomObject( item ) ) {
                return item.getStateHolder().getHashKey();
            }
            return item;
        } );
        this.differ.diff( this.array );
    }

    public clearChanges() {
        super.clearChanges();
        this.initDiffer()
    }

    public getChangedKeys(): Array<string | number> {
        const changes = super.getChangedKeys();
        const arrayChanges = this.differ.diff( this.array );
        if ( arrayChanges ) {
            let addedOrRemoved = 0;
            arrayChanges.forEachAddedItem(( record ) => {
                addedOrRemoved++;
                if ( changes.indexOf( record.currentIndex ) == -1 ) {
                    changes.push( record.currentIndex );
                }
            } );
            arrayChanges.forEachRemovedItem(( record ) => {
                addedOrRemoved--;
                if ( changes.indexOf( record.previousIndex ) == -1 ) {
                    changes.push( record.previousIndex );
                }
            } );

            arrayChanges.forEachMovedItem(( record ) => {
                if ( instanceOfChangeAwareValue( record.item ) ) {
                    return record.item.getStateHolder().markAllChanged( false );
                }
            } )
            if ( addedOrRemoved != 0 ) {
                // size changed, for now send whole array
                this.markAllChanged( false );
            }
            else {
                changes.sort(( a: number, b: number ) => {
                    return a - b;
                } );
            }
        }
        return changes;
    }
    
}