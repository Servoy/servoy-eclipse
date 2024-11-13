import { Observable } from 'rxjs';
import { IComponentCache } from './services/servoy_public.service';
import { LoggerFactory, LoggerService } from './logger.service';
import { Injectable } from '@angular/core';

export type CustomObjectTypeConstructor = (new() => ICustomObjectValue);

@Injectable({
  providedIn: 'root'
})
export class SpecTypesService {

    private log: LoggerService;
    private registeredCustomObjectTypes = new Map<string, CustomObjectTypeConstructor>();

    constructor(logFactory: LoggerFactory) {
        this.log = logFactory.getLogger('SpecTypesService');
    }

    /**
     * This method is DEPRECATED, as now, more .spec info about components/services is being sent to client (types + pushToServer info), and
     * for pushToServer SHALLOW or DEEP in .spec file (for custom objects/custom arrays), a proxy object will be used
     * on client to automatically detect reference changes and send them to server as needed.
     *
     * So there isn't a need anymore to provide 'watched' properties list by extending BaseCustomObject or manually pushing them. Just make sure that, if it
     * has pushToServer > ALLOW and if you assign a new full value (array or obj) to the property on client, read it back from the model after it's pushed to server (as that will be
     * updated/wrapped into a Proxy of your new value) and use that one instead.
     * 
     * If you still need/want to use a custom class for your custom objects (maybe you want to have methods on them or do instanceof Checks in code...) you
     * can do that by calling registerCustomObjectType(...) instead; the constructor's class you give there does not need to be a BaseCustomObject. For backwards
     * compatibility, this method will call the new registerCustomObjectType(...).
     *
     * @deprecated
     */
    registerType(customObjectTypeName: string, customObjectTypeConstructor: typeof BaseCustomObject) {
        this.log.info(this.log.buildMessage(() => ('SpecTypesService * SpecTypesService.registerTypeupdates and extending BaseCustomObject is no longer necessary. Code that registers it for "'
                    + customObjectTypeName + '", "' + customObjectTypeConstructor + '" should be safe to remove/clean up (as long as the servoy component definition .spec file is ok). If you really need a custom class for these custom objects client-side '
                    + ' do call registerCustomObjectType() instead, so that the no-arg constructor that you give there can be used for new objects that come from the server.')));
        this.registerCustomObjectType(customObjectTypeName, customObjectTypeConstructor);
    }

    /**
     * Registers a custom object type constructor to the system for components that want to use specialized classes for custom objects that come from the server.
     * 
     * Calling this IS NOT MANDATORY. You can just use simple interfaces (or interfaces that extend as well ICustomObjectValue) for your custom objects now. See .registerType(...) deprecation comment for more information.
     * 
     * @param customObjectTypeName the type name as defined in the .spec of the component/service, prefixed with the name of the
     *          component/service + ".". For example "bootstrapextracomponents-navbar.menuItem". 
     * @param customObjectTypeConstructor a constructor that custom object type should use when receiving new values from the server for
     *          the given customObjectTypeName. Be aware though that the values of the given class constructor will be augmented (via prototype chain)
     *          with some internal implementation of the custom object type.
     */
    registerCustomObjectType(customObjectTypeName: string, customObjectTypeConstructor: CustomObjectTypeConstructor) {
        this.registeredCustomObjectTypes.set(customObjectTypeName, customObjectTypeConstructor);
    }
    
    getRegisteredCustomObjectTypeConstructor(customObjectTypeName: string) {
        return this.registeredCustomObjectTypes.get(customObjectTypeName);
    }

//  the old methods
//    enhanceArrayType(...)
//        and
//    createType(...)
//  were public but as far as I can tell they were never meant to be called by component impl. code; only the custom array/object converter used them actually
//  so that is why they are removed completely, not deprecated

}

/**
 * This type is DEPRECATED.
 * 
 * You can remove extends BaseCustomObject from any custom types that you have.
 * 
 * You can even turn your custom types into interfaces (that can also - optionally - implement ICustomObjectValue - if you need that; see details below). The
 * system will automatically generate the correct object for that interface (as long as it follows what is defined in the .spec file for this custom
 * object) - and you can use it.
 * 
 * For example you can use something like ('extends ICustomObjectValue' is only needed for sending deeply nested changes to server in case of plain 'object'
 * typed subproperties):
 *      @Input() myCustomObjectProperty: IMyCustomObject;
 * 
 *      interface IMyCustomObject extends ICustomObjectValue {
 *          p1: string; // for example
 *          (... any properties that are defined in the component's spec file for this custom object ...)
 *      }
 *
 * Now, more .spec info about components/services is being automatically sent to client (types + pushToServer info), and
 * for pushToServer SHALLOW/DEEP or even for ALLOW in .spec file (for custom objects/custom arrays), a proxy object will be used
 * on client to automatically detect reference changes in subproperties (and send them to server if it is SHALLOW/DEEP) (note: for deep nested JSON changes
 * in plain 'object' subproperties - if any - you need to use ICustomObjectValue interface in order to tell the system that it changed, but
 * that is probably rarely needed).
 *
 * So there isn't a need anymore to provide 'watched' properties list by extending BaseCustomObject or to manually push them to server by using
 * getter/setter pair; that is done automatically via use of Proxy objects. Just make sure that, if it has pushToServer >= ALLOW, and if you assign
 * a new full value (full custom object value - although it is the same for arrays from .spec as well) to the property on client-side, you read it
 * back from the model when you need to use it (as that reference will be updated to a newly created Proxy of your new value - after being sent to server)
 * and use the new reference instead.
 *
 * @deprecated
 */
export class BaseCustomObject {

    /**
     * DEPRECATED: See class jsdoc for more information.
     *
     * @deprecated
     */
    getStateHolder(): BaseCustomObjectState {
        return getDeprecatedCustomObjectState(); // dummy; just not to generate compile errors for older code
    }

    /**
     * DEPRECATED: See class jsdoc for more information.
     *
     * @deprecated
     */
    getWatchedProperties(): Array<string> {
        return null;
    }

}

/**
 * DEPRECATED: See BaseCustomObject jsdoc for more information.
 *
 * @deprecated
 */
export class BaseCustomObjectState {

    /**
     * DEPRECATED: See BaseCustomObject jsdoc for more information.
     *
     * @deprecated
     */
    getChangedKeys(): Set<string | number> {
        return new Set<string | number>(); // dummy; just not to generate compile errors for older code
    }

    /**
     * DEPRECATED: See BaseCustomObject jsdoc for more information.
     *
     * @deprecated
     */
    markAllChanged(_notifyListener: boolean): void {}

    /**
     * DEPRECATED: See BaseCustomObject jsdoc for more information.
     *
     * @deprecated
     */
    setPropertyAndHandleChanges(thisBaseCustoomObject: any, internalPropertyName: any, _propertyName: any, value: any) {
        thisBaseCustoomObject[internalPropertyName] = value;
    }

    /**
     * DEPRECATED: See BaseCustomObject jsdoc for more information.
     *
     * @deprecated
     */
    notifyChangeListener(): void {}

}

/**
 * This type is DEPRECATED (you can stop using ICustomArray for arrays declared as such in component/service .spec files; just use them as Array<T> or 
 * ICustomArrayValue<T>).
 *
 * You can expect these custom arrays to implement ICustomArrayValue (if you need that; see details below). The system
 * will automatically generate the correct value implementing that interface for typed arrays (as long as it follows what is defined in the .spec file)
 * - and you can use it.
 *
 * Now, more .spec info about components/services is being sent to client (types + pushToServer info, ...), and
 * for pushToServer SHALLOW/DEEP or even for ALLOW in .spec file (for custom objects/custom arrays), a proxy object will be used
 * on client to automatically detect reference changes in elements (and send them to server if it is SHALLOW/DEEP) (note: for deep nested JSON changes
 * in plain 'object' elements of an array - if any - you need to use the ICustomArrayValue interface in order to tell the system that it changed, but
 * that is probably rarely needed).
 *
 * So there isn't a need anymore mark as changed manually (except for the deep neste JSON elements case mentioned above) or to manually push them to server
 * by using getter/setter pair; that is done automatically via use of Proxy objects. Just make sure that, if it has pushToServer >= ALLOW, and if you assign
 * a new full value (full custom array value - although it is the same for custom objects from .spec as well) to the property on client-side, you read it
 * back from the model when you need to use it (as that reference will be updated to a newly created Proxy of your new value, that also implements ICustomArrayValue
 *  - after being sent to server) and use the new reference instead.
 *
 * @deprecated
 */
export interface ICustomArray<T> extends Array<T> {
    getStateHolder(): ArrayState;
    markForChanged(): void;
}

/**
 * This type is DEPRECATED - see ICustomArray jsdoc for more information.
 *
 * @deprecated
 */
 export class ArrayState extends BaseCustomObjectState {

    /**
     * DEPRECATED: See ICustomArray jsdoc for more information.
     *
     * @deprecated
     */
    public initDiffer() {
    }

    /**
     * DEPRECATED: See ICustomArray jsdoc for more information.
     *
     * @deprecated
     */
     public clearChanges() {
     }

    /**
     * DEPRECATED: See ICustomArray jsdoc for more information.
     *
     * @deprecated
     */
     public getChangedKeys(): Set<string | number> {
         return super.getChangedKeys();
     }

}

let deprecatedCustomObjectState: BaseCustomObjectState;
let deprecatedCustomArrayState: ArrayState;

/** @deprecated see BaseCustomObjectState jsdoc */
const getDeprecatedCustomObjectState = (): BaseCustomObjectState => {
    if (!deprecatedCustomObjectState) deprecatedCustomObjectState = new BaseCustomObjectState();
    return deprecatedCustomObjectState;
}

/** @deprecated see ArrayState jsdoc */
export const getDeprecatedCustomArrayState = (): ArrayState => {
    if (!deprecatedCustomArrayState) deprecatedCustomArrayState = new ArrayState();
    return deprecatedCustomArrayState;
}

/**
 * Value present for properties in model of Servoy components/services if they are declared in the .spec file as custom arrays. (someType[])
 * 
 * For example you can use:
 *     @Input() myCustomArrayProperty: Array<MyArrayElementType>;
 * 
 * or, if you needed to send deeply nested changes to server in case of plain 'object' array element type (see markElementAsHavingDeepChanges(...) method jsdoc):
 *     @Input() myCustomArrayProperty: ICustomArrayValue<MyArrayElementType>;
 * 
 */
export interface ICustomArrayValue<T> extends Array<T> {

    /**
     * Calling this method is (rarely) only needed if both:
     *   1. you want to send client side changes back to the server (calculated pushToServer .spec config for the elements of this array is >= ALLOW)
     *   2. AND you store in the array simple json values that are nested, and type elements simply as 'object' in the .spec file. So if the array
     *      is declared in the .spec file as 'object[]' and you change an element/index in it client-side not by reference, but
     *      by content nested inside the array element value (e.g. myArray[5].subProp = 15) instead. Calling this method is not needed if you do
     *      for example myArray[5] = { subProp: 14, otherSubProp: true }, so a change by reference of index 5.
     *
     * In these case (1 and 2), in order for the updated element value to be marked as needing to be sent to server, call this method.
     *
     * Calling this is generally not needed because, if pushToServer is ALLOW or greater (SHALLOW / DEEP) on elements, the array will mark (as needing
     * to be sent to server) automatically any inserts/deletes and changes by reference in the array. It will mark an element as changed as well in case of
     * nested changes that happen in other array or custom object types ONLY if they are declared as such in the component/service Servoy .spec file
     * (so they are not typed as simple 'object'). That is why you do not need to call this method for those situations. Call it only for
     * nested JSON element values typed as 'object' in the .spec file, that have changes in them but are not changed by reference.
     *
     * These marked elements, if they have ALLOW pushToServer, not higher, will not be sent to server right away, but whenever a send/emit of the
     * component's/service's root property (that contains this value - can be even this value directly) is sent to server. This can be triggered directly via
     * an .emit(...) on the @Output() of a component's root property (and in case of services, ServoyPublicService.sendServiceChanges(...)) or automatically
     * by some other SHALLOW or DEEP pushToServer value change somewhere else in the same root property.
     *
     * IMPORTANT: This method is available for custom arrays received from server and for ones created client - side by components, but, for the latter, only after
     * they were sent once to server (you have to get them again from model or parent obj/array as they will be replaced by a new instance - that has this method).
     * DO NOT add this method yourself in newly created custom array instances client-side, as that will not help!
     */
    markElementAsHavingDeepChanges?(index: number): void; // marked optional - "?" - to allow client side code that is typed to this interface to assign plain array values to it

}

/**
 * Value present for properties in model of Servoy components/services if they are declared in the .spec file as custom object types. (sp in the 'types' section).
 * 
 * You can declare your custom types as simple interfaces (that can also - optionally - implement ICustomObjectValue - if you need that; see details in jsdoc of
 * markSubPropertyAsHavingDeepChanges(...) method). The system will automatically generate the correct object for that interface (as long as it follows what is
 * defined in the .spec file for this custom object) - and you can use it.
 * 
 * For example you can use something like ('extends ICustomObjectValue' is only needed for sending deeply nested changes to server in case of plain 'object'
 * typed subproperties):
 * <pre><code>
 *      @Input() myCustomObjectProperty: IMyCustomObject;
 * 
 *      interface IMyCustomObject extends ICustomObjectValue { // extends ICustomObjectValue is optional; the ICustomObjectValue.markSubPropertyAsHavingDeepChanges method will be added automatically
 *          p1: string; // for example
 *          (... all other properties that are defined in the component's spec file for this custom object type ...)
 *      }
 * </code></pre>
 * If you feel like the interface is not enough and your custom types need to have some client-side special class with client side only methods, for example if you
 * have a tab-panel component with some 'tab' type, that is possible. You have to declare your class:
 * <pre><code>
 *     class Tab implements ICustomObjectValue { ...your_fields_and_methods... } // implements ICustomObjectValue is optional; do not declare the markSubPropertyAsHavingDeepChanges - that will be added automatically (when from/to server happens)
 * </code></pre>
 * then call:
 * <pre><code>
 *     specTypesService.registerCustomObjectType("mycomponent.tab", Tab);
 * </code></pre>
 * The custom objects sent from server for 'mycomponent.tab' type will have Tab added to their prototype chain automatically. Both those comming from server
 * and any new Tab instances created on client (after they are sent to the server the first time) will have the markSubPropertyAsHavingDeepChanges method added
 * to them automatically via the prototype chain as well. So you can use then something like this (if you ever need to):
 * <pre><code>
 *     (tab as ICustomObjectValue).markSubPropertyAsHavingDeepChanges(nameOfSomePlainObjectPropertyWithNestedJSONContentInIt);
 * </code></pre>
 */
export interface ICustomObjectValue extends Record<string, any> {

    /**
     * If you extend/implement this interface, DO NOT IMPLEMENT THIS METHOD YOURSELF; it will be added under the hood by the custom object type code.
     * 
     * Calling this method is (rarely) only needed if both:
     *   1. you want to send client side changes back to the server (calculated pushToServer .spec config for the elements of this array is >= ALLOW)
     *   2. AND you store in the custom object's subproperties simple json values that are nested, and type subproperties simply as 'object' in the .spec
     *      file. So if the subprop. is declared in the .spec file as 'object' and you change that subproperty in it client-side not by reference, but
     *      by content nested inside the subproperty value (e.g. myCustomObject.subProp[3].x = 15) instead. Calling this method is not needed if you do
     *      for example myCustomObject.subProp = [ { x: [14, 0], y: true } ], so a change by reference of subProp.
     *
     * In that case (1 and 2), in order for the updated subproperty value to be marked as needing to be sent to server, call this method.
     *
     * Calling this is generally not needed because, if pushToServer is ALLOW or greater (SHALLOW / DEEP) on a subproperty, the custom object will mark
     * automatically as needing to be sent to server any new/deleted subproperties and changes by reference in the object. It will mark an element as changed
     * as well in case of nested changes that happen in other custom array or custom object types ONLY if they are declared as such in the .spec file (so not
     * simple 'object's), so you do not need to call this method for those situations. Only for nested JSON subproperty values that change, are typed as 'object' in
     * the component/service .spec file, but are not changed by reference.
     *
     * These marked subproperties, if they have ALLOW pushToServer, not higher, will not be sent to server right away, but whenever a send of the
     * component's/service's root property (that contains this value - can be even this value directly) is sent to server. This can be triggered directly via
     * an .emit(...) on the @Output() of a component's root property (and in case of services, ServoyPublicService.sendServiceChanges(...)) or automatically by
     * some other SHALLOW or DEEP pushToServer value change somewhere else in the same root property.
     *
     * IMPORTANT: This method is available for custom objects received from server and for ones created client - side by components, but, for the latter, only after
     * they were sent once to server (you have to get them again from model or parent obj/array as they will be replaced by a new instance - that has this method).
     * DO NOT add this method yourself in newly created custom object instances client-side, as that will not help!
     */
    markSubPropertyAsHavingDeepChanges?(subPropertyName: string): void; // marked optional - "?" - to allow client side code that is typed to this interface to assign plain array values to it

}

export interface IValuelist extends Array<{ displayValue: string; realValue: any }> {
    filterList(filterString: string): Observable<any>;
    getDisplayValue(realValue: any): Observable<any>;
    hasRealValues(): boolean;
    isRealValueDate(): boolean;
}

export interface IFoundsetTree extends Array<any> {
    getChildren(parentID: string, level: number): Promise<any>;
    updateSelection(idarray: Array<string>): void;
    updateCheckboxValue(id: string, value: boolean): void;
    getAndResetNewChildren(): {key: any};
    getAndResetUpdatedCheckboxValues(): {key: boolean};
}

export interface IJSMenu{
    items : IJSMenuItem[];
    name: string;
    styleClass: string;
    pushDataProviderValue(category: string, propertyName: string, itemIndex: number, dataproviderValue: any): void;
}

export interface IJSMenuItem{
    items : IJSMenuItem[];
    menuText: string;
    styleClass: string;
    itemID: string;
    enabled: boolean;
    iconStyleClass: string;
    tooltipText: string;
    isSelected: boolean;
    extraProperties: {Navbar:any, Sidenav:any};
}

export interface IPopupSupportComponent {
    closePopup();
}

export interface IFoundsetFieldsOnly {

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
     * the findMode state of the server's foundset
     */    
    findMode: boolean;

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
    columnFormats?: Record<string, any>;
}

/**
 * Interface for client side values of 'foundset' typed properties in .spec files.
 */
export interface IFoundset extends IFoundsetFieldsOnly {

    /**
     * Request a change of viewport bounds from the server; the requested data will be loaded
     * asynchronously in 'viewPort'.
     *
     * @param startIndex the index that you request the first record in "viewPort.rows" to have in
     *                   the real foundset (so the beginning of the viewPort).
     * @param size the number of records to load in viewPort.
     *
     * @return a promise that will get resolved when the requested records arrived browser-
     *                   side. As with any promise you can register success, error callbacks, finally, ...
     *                   See JSDoc of RequestInfoPromise.requestInfo and FoundsetChangeEvent.requestInfos
     *                   for more information about determining if a listener event was caused by this call.
     */
    loadRecordsAsync(startIndex: number, size: number): RequestInfoPromise<any>;

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
     *                   See JSDoc of RequestInfoPromise.requestInfo and FoundsetChangeEvent.requestInfos
     *                   for more information about determining if a listener event was caused by this call.
     */
    loadExtraRecordsAsync(negativeOrPositiveCount: number, dontNotifyYet?: boolean): RequestInfoPromise<any>;

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
     *                   See JSDoc of RequestInfoPromise.requestInfo and FoundsetChangeEvent.requestInfos
     *                   for more information about determining if a listener event was caused by this call.
     */
    loadLessRecordsAsync(negativeOrPositiveCount: number, dontNotifyYet?: boolean): RequestInfoPromise<any>;

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
     * @return a promise that will get resolved when the new sort
     *                   will arrive browser-side. As with any promise you can register success, error
     *                   and finally callbacks.
     *                   See JSDoc of RequestInfoPromise.requestInfo and FoundsetChangeEvent.requestInfos
     *                   for more information about determining if a listener event was caused by this call.
     */
    sort(sortColumns: Array<{ name: string; direction: ('asc' | 'desc') }>): RequestInfoPromise<any>;

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
     *
     * @return a promise that will get resolved when the requested selection update arrived back browser-
     *                   side. As with any promise you can register success, error callbacks, finally, ...
     *                   See JSDoc of RequestInfoPromise.requestInfo and FoundsetChangeEvent.requestInfos
     *                   for more information about determining if a listener event was caused by this call.
     */
    requestSelectionUpdate(selectedRowIdxs: number[]): RequestInfoPromise<any>;

    /**
     * It will send a data update for a cell (a column in a row) in the foundset to the server.
     * Please make sure to adjust the viewport value as well not just call this method.
     *
     * This method is useful if you do not want to use push-to-server SHALLOW in .spec file but do it manually instead, or if
     * you have nested JSON object/arrays as cell values and you need to tell the foundset that something nested has changed (DEEP watches are not available in NG2) or
     * if you just need a promise to know when the change was done or failed on server.
     *
     * The calculated pushToServer for the foundset property should be set to 'allow' if you use this method. Then the server will accept
     * data changes from this property, but there will be no automatic proxies to detect the changes-by-reference to cell values, so the component uses this call
     * to send cell changes instead.
     *
     * @param rowID the _svyRowId column of the client side row
     * @param columnName the name of the column to be updated on server (in that row).
     * @param newValue the new data in that cell
     * @param oldValue the old data that used to be in that cell
     * @return (first versions of this method didn't return anything; more recent ones return this) a promise that will get resolved when the new cell value
     *                   update is done server-side (resolved if ok, rejected if it failed). As with any promise you can register success, error
     *                   and finally callbacks.
     */
    columnDataChangedByRowId(rowID: string, columnName: string, newValue: any, oldValue: any): Promise<any>;

    /**
     * Convenience method that does exactly what #columnDataChangedByRowId does, but based on a row index from the viewport not on that row's ID.
     */
    columnDataChanged(rowIndex: number, columnName: string, newValue: any, oldValue: any): Promise<any>;

    /**
     * Please use columnDataChangedByRowId(...) instead.
     *
     * @deprecated please use columnDataChangedByRowId(...) instead.
     */
    updateViewportRecord(rowID: string, columnID: string, newValue: any, oldValue: any): void;

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
    // eslint-disable-next-line @typescript-eslint/ban-types
    getRecordRefByRowID(rowId: string): object;

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
     * See also the description of "foundsetInitialPageSize" property type for a way to set this
     * at design-time (via properties view) or before the form is shown for components that 'page' data.
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
     * Adds a change listener that will get triggered when server sends changes for this foundset.
     *
     * @param changeListener the listener to register.
     * @return a function to unregister the listener
     */
    addChangeListener(changeListener: FoundsetChangeListener): () => void;
    removeChangeListener(changeListener: FoundsetChangeListener): void;

}

export interface ViewPort {
    startIndex: number;
    size: number;
    rows: ViewPortRow[];
};

export interface ViewPortRow extends Record<string, any> {

    _svyRowId: string;
    
    /** components that use the foundset property are free to use this property however they like; it is not set/used by the foundset impl. */
    _cache?: Map<string, any>;

}

/**
 * Besides working like a normal Promise that you can use to get notified when some action is done (success/error/finally), chain etc., this promise also
 * contains field "requestInfo" which can be set by the user and could later be reported in some listener events back to the user (in case this same action
 * is going to trigger those listeners as well).
 *
 * @since 2021.09
 */
export interface RequestInfoPromise<T> extends Promise<T> {

    /**
     * You can assign any value to it. The value that you assign - if any - will be given back in the
     * event object of any listener that will be triggered as a result of the promise's action. So in
     * case the same action, when done, will trigger both the "then" of the Promise and a separate
     * listener, that separate listener will contain this "requestInfo" value.
     *
     * This is useful for some components that want to know if some change (reported by the listener)
     * happened due to an action that the component requested or due to changes in the outside world.
     * (eg: FoundsetPropertyValue.loadRecordsAsync(...) returns RequestInfoPromise and 
     * FoundsetChangeEvent.requestInfos array can return that RequestInfoPromise.requestInfo on the
     * event that was triggered by that loadRecordsAsync).
     */
    requestInfo?: any;

}

export interface FoundsetChangeEvent extends ViewportChangeEvent {

    /**
     * If this change event is caused by one or more calls (by the component) on the IFoundset obj
     * (like loadRecordsAsync requestSelectionUpdate and so on), and the caller then assigned a value to
     * the returned RequestInfoPromise's "requestInfo" field, then that value will be present in this array.
     *
     * This is useful for some components that want to know if some change (reported in this FoundsetChangeEvent)
     * happened due to an action that the component requested or due to changes in the outside world. (eg:
     * IFoundset.loadRecordsAsync(...) returns RequestInfoPromise and FoundsetChangeEvent.requestInfos array can
     * contain that RequestInfoPromise.requestInfo on the event that was triggered by that loadRecordsAsync).
     *
     * @since 2021.09
     */
    requestInfos?: any[];

    /**
     * If a full value update was received from server, this key is set; if both the newValue and
     * the oldValue are non-null, the oldValue's reference will be reused (so the reference of the
     * foundset property doesn't change, just it's contents are updated) and oldValue given below is
     * actually a shallow-copy of the old value's properties/keys; this can help in some component
     * implementations.
     */
    fullValueChanged?: { oldValue: IFoundsetFieldsOnly; newValue: IFoundset };

    // the following keys appear if each of these got updated from server; the names of those
    // keys suggest what it was that changed; oldValue and newValue are the values for what changed
    // (e.g. new server size and old server size) so not the whole foundset property new/old value
    serverFoundsetSizeChanged?: { oldValue: number; newValue: number };
    foundsetDefinitionChanged?: boolean;
    hasMoreRowsChanged?: { oldValue: boolean; newValue: boolean };
    multiSelectChanged?: { oldValue: boolean; newValue: boolean };
    columnFormatsChanged?: { oldValue: Record<string, Record<string, unknown>>; newValue: Record<string, Record<string, unknown>> };
    sortColumnsChanged?: { oldValue: string; newValue: string };
    selectedRowIndexesChanged?: { oldValue: number[]; newValue: number[] };
    viewPortStartIndexChanged?: { oldValue: number; newValue: number };
    viewPortSizeChanged?: { oldValue: number; newValue: number };
    viewportRowsCompletelyChanged?: { oldValue: ViewPortRow[]; newValue: ViewPortRow[] };
    userSetSelection?: boolean;
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

export type ViewportChangeListener = (changeEvent: ViewportChangeEvent) => void;
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

/**
 * Interface for client side values of 'component' typed properties in .spec files. (so used as child components within the .spec of a component)
 */
export interface IChildComponentPropertyValue extends IComponentCache {

    name: string;
    /** this is the shared part of the model; you might want to use modelViewport (which uses this as prototype) instead if the child component has foundset-linked properties */
    model: { [property: string]: any };
    handlers: any;
    foundsetConfig?: {
        recordBasedProperties?: Array<string>;
        apiCallTypes?: Array<any>;
    };

    /** this is the true cell viewport which is already composed inside IChildComponentPropertyValue of shared (non foundset dependent) part and row specific (foundset dependent props) part */
    modelViewport: any[];

    /**
     * This function has to be set/provided by the ng2 component that uses this child "component" typed property, because
     * that one controls the UI of the child component represented by this property (or child components in case of
     * foundset linked components like in list form component for example). Then when there are changes comming from server, the 'component'
     * property type will call this function as needed.
     */
    triggerNgOnChangeWithSameRefDueToSmartPropertyUpdate: (propertiesChangedButNotByRef: {propertyName: string; newPropertyValue: any}[], rowIndex: number) => void;

    /**
     * This gives a way to trigger handlers.
     *
     * In case this component is not foundset-linked, use mappedHandlers directly (which is a function).
     * In case this component is foundset-linked, use mappedHandlers.selectRecordHandler(rowId) to trigger the handler for the component of the correct row in the foundset.
     */
    mappedHandlers: Map<string, { (): Promise<any>; selectRecordHandler(rowId: any): () => Promise<any> }>;

    /**
     * Parent components that use this child 'component' typed value will need to give a svyServoyApi to the actual child components (whether it's a viewport of them
     * for this child component value (foundset linked components similar to list form component child usage) or only one). They can use this startEdit to provide
     * that to child components based on specific rows in the foundset (via rowId param).
     */
    startEdit(propertyName: string, rowId: any): void;

    /**
     * Parent components that use this child 'component' typed value will need to give a svyServoyApi to the actual child components (whether it's a viewport of them
     * for this child component value (foundset linked components similar to list form component child usage) or only one). They can use this sendChanges to provide
     * 'apply' of dataproviders to child components based on specific rows in the foundset (via rowId param) or even in order to provide non-dataprovider send change
     * API or emit() implementation based on specific rows in the foundset (via rowId param).
     */
    sendChanges(propertyName: string, modelOfComponent: Record<string, any>, oldValue: any, rowId: any, isDataprovider?: boolean): void;

    /**
     * Adds a change listener that will get triggered when server sends granular or full modelViewport changes for this component.
     *
     * @param viewportChangeListener the listener to register.
     */
    addViewportChangeListener(viewportChangeListener: ViewportChangeListener): () => void;
    removeViewportChangeListener(viewportChangeListener: ViewportChangeListener): void;

}
