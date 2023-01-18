import { ConverterService, IChangeAwareValue, instanceOfChangeAwareValue,
            ChangeListenerFunction, ChangeAwareState, SoftProxyRevoker } from '../../sablo/converter.service';
import { IType, IPropertyContext, ITypeFactory, PushToServerEnum,
            PushToServerEnumServerValue, ICustomTypesFromServer, ITypeFromServer, ITypesRegistryForTypeFactories,
            PushToServerUtils, PropertyContext } from '../../sablo/types_registry';
import { BaseCustomObjectState } from './json_object_converter';
import { ICustomArrayValue, ICustomArray, ArrayState as DeprecatedArrayState, getDeprecatedCustomArrayState, LoggerService, LoggerFactory } from '@servoy/public';

export class CustomArrayTypeFactory implements ITypeFactory<CustomArrayValue<any>> {

    public static readonly TYPE_FACTORY_NAME = 'JSON_arr';

    private customArrayTypes: Map<IType<any>, Map<PushToServerEnum, CustomArrayType<any>>> = new Map(); // allows any keys, even undefined
    private logger: LoggerService;

    constructor(private readonly typesRegistry: ITypesRegistryForTypeFactories,
            private readonly converterService: ConverterService, logFactory: LoggerFactory) {
        this.logger = logFactory.getLogger('ArrayConverter');
    }

    getOrCreateSpecificType(specificElementInfo: ITypeFromServer | { t: ITypeFromServer; s: PushToServerEnumServerValue }, webObjectSpecName: string): CustomArrayType<any> {
        const elementTypeWithNoPushToServer = (specificElementInfo instanceof Array || typeof specificElementInfo == 'string') || specificElementInfo === null;
        const elementTypeFromSrv: ITypeFromServer = (elementTypeWithNoPushToServer) ?
                    specificElementInfo as ITypeFromServer : (specificElementInfo as { t: ITypeFromServer; s: PushToServerEnumServerValue }).t;
        const pushToServer: PushToServerEnum = (elementTypeWithNoPushToServer) ?
                    undefined : PushToServerUtils.valueOf((specificElementInfo as { t: ITypeFromServer; s: PushToServerEnumServerValue }).s);

        // a custom array could have an element type that is not a client side type; but it still needs to be an array type
        const staticElementType = (elementTypeFromSrv ? this.typesRegistry.processTypeFromServer(elementTypeFromSrv, webObjectSpecName) : undefined);

        let cachedArraysByType = this.customArrayTypes.get(staticElementType);
        if (!cachedArraysByType) {
            cachedArraysByType = new Map();
            this.customArrayTypes.set(staticElementType, cachedArraysByType);
        }
        let cachedArraysByTypeAndPushToServerOnElem = cachedArraysByType.get(pushToServer);
        if (!cachedArraysByTypeAndPushToServerOnElem) {
            cachedArraysByTypeAndPushToServerOnElem = new CustomArrayType<any>(staticElementType, pushToServer, this.converterService, this.logger);
            cachedArraysByType.set(pushToServer, cachedArraysByTypeAndPushToServerOnElem);
        }

        return cachedArraysByTypeAndPushToServerOnElem;
    }

    registerDetails(_details: ICustomTypesFromServer, _webObjectSpecName: string) {
        // arrays don't need to pre-register stuff
    }

}


/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json array converter interface. Do not use externally otherwise. */
export class CustomArrayType<T> implements IType<CustomArrayValue<T>> {

    constructor(private readonly staticElementType: IType<any>,
                private readonly pushToServerForElements: PushToServerEnum,
                private converterService: ConverterService,
                private logger: LoggerService) {
    }

    fromServerToClient(serverJSONValue: ICATDataFromServer, currentClientValue: CustomArrayValue<T>, propertyContext: IPropertyContext): CustomArrayValue<T> {
        let newValue: CustomArrayValue<T> = currentClientValue;
        let internalState: ArrayState = null;

        const elemPropertyContext = propertyContext ? new PropertyContext(propertyContext.getProperty,
                PushToServerUtils.combineWithChildStatic(propertyContext.getPushToServerCalculatedValue(), this.pushToServerForElements)) : undefined;

        try {
            if (instanceOfFullValueFromServer(serverJSONValue)) {
                // full contents
                newValue = this.initArrayValue(serverJSONValue.v, serverJSONValue.vEr, propertyContext?.getPushToServerCalculatedValue());
                internalState = newValue.getInternalState();
                internalState.ignoreChanges = true;

                // remove change listeners from any smart el. values that are now obsolete
                if (currentClientValue) {
                    for (const obsoleteElemValue of currentClientValue) {
                        if (instanceOfChangeAwareValue(obsoleteElemValue)) {
                            obsoleteElemValue.getInternalState().setChangeListener(undefined);
                        }
                    }
                }

                // convert value received from server, store and add change listeners if needed for elements
                if (newValue.length) {
                    for (let c = 0; c < newValue.length; c++) {
                        let elem = newValue[c];

                        newValue[c] = elem = this.converterService.convertFromServerToClient(elem, this.staticElementType, currentClientValue ? currentClientValue[c] : undefined,
                                internalState.dynamicPropertyTypesHolder, '' + c, elemPropertyContext);

                        if (instanceOfChangeAwareValue(elem)) {
                            // child is able to handle it's own change mechanism
                            elem.getInternalState().setChangeListener(this.getChangeListener(newValue, c));
                        }
                    }
                }
            } else if (instanceOfGranularUpdatesFromServer(serverJSONValue)) {
                // granular updates received;
                internalState = currentClientValue.getInternalState();
                internalState.ignoreChanges = true;

                // for example if a custom object value is initially received through a return value from server side api/handler call and
                // not as a normal model property and then the component/service assigns it to model, the push to server of it might have
                // changed; use the one received here as arg
                internalState.calculatedPushToServerOfWholeProp = propertyContext?.getPushToServerCalculatedValue();
                if (!internalState.calculatedPushToServerOfWholeProp) internalState.calculatedPushToServerOfWholeProp = PushToServerEnum.REJECT;

                // if something changed browser-side, increasing the content version thus not matching next expected version,
                // we ignore this update and expect a fresh full copy of the array from the server (currently server value is
                // leading/has priority because not all server side values might support being recreated from client values)
                if (internalState.contentVersion === serverJSONValue.vEr) {
                    for (const granularOp of serverJSONValue.g) {
                        const startIndex_endIndex_opType = granularOp.op; // it's an array of 3 elements in the order given in name
                        const startIndex: number = startIndex_endIndex_opType[0];
                        const endIndex: number = startIndex_endIndex_opType[1];
                        const opType: number = startIndex_endIndex_opType[2];

                        if (opType === ICATOpTypeEnum.CHANGED) {
                            const changedData = granularOp.d;
                            for (let i = startIndex; i <= endIndex; i++) {
                                const relIdx = i - startIndex;
                                // remove change listeners from any smart el. values that are now obsolete
                                const oldElemValue = currentClientValue[i];
                                if (instanceOfChangeAwareValue(oldElemValue)) {
                                    // child is able to handle it's own change mechanism
                                    oldElemValue.getInternalState().setChangeListener(undefined);
                                }

                                // apply the conversions, update value and kept conversion info for changed indexes
                                currentClientValue[i] = this.converterService.convertFromServerToClient(changedData[relIdx], this.staticElementType,
                                                            currentClientValue[i], internalState.dynamicPropertyTypesHolder, '' + i, elemPropertyContext);

                                const val = currentClientValue[i];
                                if (instanceOfChangeAwareValue(val)) {
                                    // child is able to handle it's own change mechanism
                                    val.getInternalState().setChangeListener(this.getChangeListener(currentClientValue, i));
                                }
                            }
                        } else if (opType === ICATOpTypeEnum.INSERT) {
                            const insertedData = granularOp.d;
                            const numberOfInsertedRows = insertedData.length;

                            // shift right by "numberOfInsertedRows" all dynamicTypes after or equal to idx as we are going to insert new values there
                            for (let idxToShift = currentClientValue.length - 1; idxToShift >= startIndex; idxToShift--)
                                if (internalState.dynamicPropertyTypesHolder['' + idxToShift]) {
                                    internalState.dynamicPropertyTypesHolder['' + (idxToShift + numberOfInsertedRows)] = internalState.dynamicPropertyTypesHolder['' + idxToShift];
                                    delete internalState.dynamicPropertyTypesHolder['' + idxToShift];
                                } else delete internalState.dynamicPropertyTypesHolder['' + (idxToShift + numberOfInsertedRows)];

                            // apply conversions
                            for (let i = numberOfInsertedRows - 1; i >= 0 ; i--) {
                                const addedRow = this.converterService.convertFromServerToClient(insertedData[i], this.staticElementType, undefined,
                                                internalState.dynamicPropertyTypesHolder, '' + (startIndex + i), elemPropertyContext);
                                currentClientValue.splice(startIndex, 0, addedRow);
                            }

                            // update any affected change notifiers
                            for (let i = startIndex; i < currentClientValue.length; i++) {
                                const childEl = currentClientValue[i];
                                if (instanceOfChangeAwareValue(childEl)) {
                                    // child is able to handle it's own change mechanism
                                    childEl.getInternalState().setChangeListener(this.getChangeListener(currentClientValue, i));
                                }
                            }
                        } else if (opType === ICATOpTypeEnum.DELETE) {
                            const numberOfDeletedRows = endIndex - startIndex + 1;

                            // shift left by "numberOfDeletedRows" all dynamicTypes after "startIndex + numberOfDeletedRows" as we are going to delete that interval
                            for (let idxToShift = startIndex + numberOfDeletedRows; idxToShift < currentClientValue.length; idxToShift++)
                                if (internalState.dynamicPropertyTypesHolder['' + idxToShift]) {
                                    internalState.dynamicPropertyTypesHolder['' + (idxToShift - numberOfDeletedRows)] = internalState.dynamicPropertyTypesHolder['' + idxToShift];
                                    delete internalState.dynamicPropertyTypesHolder['' + idxToShift];
                                } else delete internalState.dynamicPropertyTypesHolder['' + (idxToShift - numberOfDeletedRows)];

                            // remove change listeners from any smart el. values that are now obsolete
                            for (let c = startIndex; c <= endIndex; c++) {
                                const deletedElem = currentClientValue[c];
                                if (instanceOfChangeAwareValue(deletedElem)) {
                                    deletedElem.getInternalState().setChangeListener(undefined);
                                }
                            }

                            // actual delete
                            currentClientValue.splice(startIndex, numberOfDeletedRows);

                            // update any affected change notifiers
                            for (let i = startIndex; i < currentClientValue.length; i++) {
                                const childEl = currentClientValue[i];
                                if (instanceOfChangeAwareValue(childEl)) {
                                    // child is able to handle it's own change mechanism
                                    childEl.getInternalState().setChangeListener(this.getChangeListener(currentClientValue, i));
                                }
                            }
                        }
                    }
                }
                // else {
                // else we got an update from server for a version that was already bumped by changes in browser; ignore that, as browser changes were sent to server
                // and server will detect the problem and send back a full update
                // }
            } else if (!instanceOfNoOpFromServer(serverJSONValue))
                // anything else would not be supported... // TODO how to handle null values (special watches/complete array set from client)?
                // if null is on server and something is set on client or the other way around?
                newValue = null;
        } finally {
            if (internalState) internalState.ignoreChanges = false;
        }

        return newValue;
    }

    fromClientToServer(newClientData: any, oldClientData: CustomArrayValue<any>, propertyContext: IPropertyContext): [ICATDataToServer, CustomArrayValue<any>] | null {
        // note: oldClientData could be uninitialized (so not yet instance of CustomArrayValue) if a parent obj/array decides to send itself fully when an
        // element/subproperty was added - in which case it will be the same as newClientData... at least until SVY-17854 gets implemented and then old would be null
        // as expected in that scenario

        const elemPropertyContext = propertyContext ? new PropertyContext(propertyContext.getProperty,
                PushToServerUtils.combineWithChildStatic(propertyContext.getPushToServerCalculatedValue(), this.pushToServerForElements)) : undefined;
        let newClientDataInited: CustomArrayValue<any>;

        let internalState: ArrayState;
        try {
            if (newClientData) {
                // test if this was an array created fully on the client.
                if (!instanceOfCustomArray(newClientData)) {
                    // this can happen when an array value was set completely in browser
                    // any 'smart' child elements will initialize in their fromClientToServer conversion;
                    // set it up, make it 'smart' and mark it as all changed to be sent to server...
                    newClientDataInited = newClientData = this.initArrayValue(newClientData, oldClientData?.getInternalState ? oldClientData.getInternalState().contentVersion : 0,
                              propertyContext?.getPushToServerCalculatedValue());
                    internalState = newClientDataInited.getInternalState();
                    internalState.markAllChanged(false);
                    internalState.ignoreChanges = true;
                } else if (newClientData !== oldClientData) {
                    // if a different smart value from the browser is assigned to replace old value it is a full value change; also adjust the version to it's new location

                    // clear old internal state proxy and get non-proxied value in order to re-initialize/start fresh in the new location (old proxy would send change notif to wrong place)
                    // we only need from the old internal state the dynamic types
                    const oldDynamicTypesHolder = newClientData.getInternalState().dynamicPropertyTypesHolder;
                    newClientData = newClientData.getInternalState().destroyAndDeleteMeAndGetNonProxiedValueOfProp();
                    delete newClientData[ChangeAwareState.INTERNAL_STATE_MEMBER_NAME];

                    newClientDataInited = newClientData = this.initArrayValue(newClientData, oldClientData?.getInternalState ? oldClientData.getInternalState().contentVersion : 0,
                              propertyContext?.getPushToServerCalculatedValue());
                    internalState = newClientDataInited.getInternalState();
                    internalState.dynamicPropertyTypesHolder = oldDynamicTypesHolder;
                    internalState.markAllChanged(false);
                    internalState.ignoreChanges = true;
                } else {
                    newClientDataInited = newClientData; // it was already initialized in the past (not a new client side created value)
                    internalState = newClientData.getInternalState();
                    internalState.ignoreChanges = true;

                    // for example if a custom array value is initially received through a return value from server side api/handler call and not as a
                    // normal model property and then the component/service assigns it to model, the push to server of it might have changed; use the one
                    // received here as arg
                    internalState.calculatedPushToServerOfWholeProp = propertyContext?.getPushToServerCalculatedValue();
                    if (!internalState.calculatedPushToServerOfWholeProp) internalState.calculatedPushToServerOfWholeProp = PushToServerEnum.REJECT;
                }
            } else newClientDataInited = newClientData; // null/undefined

            if (newClientDataInited) {
                const arrayChanges = internalState.changedKeys;
                if (arrayChanges.size > 0 || internalState.allChanged) {
                    const changes = {} as (ICATFullArrayToServer | ICATGranularUpdatesToServer);
                    changes.vEr = internalState.contentVersion;

                    if (internalState.allChanged) {
                        const fullChange = changes as ICATFullArrayToServer;
                        // structure might have changed; increase version number
                        ++internalState.contentVersion; // we also increase the content version number - server should only be expecting updates for the next version number
                        // send all
                        const toBeSentArray = fullChange.v = [];
                        for (let idx = 0; idx < newClientDataInited.length; idx++) {
                            const val = newClientDataInited[idx];
                            if (instanceOfChangeAwareValue(val)) {
                                // even if child value has only partial changes that we want to send, do send the full value as we are sending full array value here
                                val.getInternalState().markAllChanged(false);
                            }
                            const converted = this.converterService.convertFromClientToServer(val, this.getElementType(internalState, idx),
                                                    oldClientData ? oldClientData[idx] : undefined, elemPropertyContext);
                            // TODO although this is a full change, we give oldClientData[idx] (oldvalue) because server side does the same for some reason,
                            // but normally both should use undefined/null for old value of elements as this is a full change; SVY-17854 is created for looking into this

                            if (val !== converted[1]) newClientDataInited[idx] = converted[1];
                            // if it's a nested obj/array or other smart prop that just got smart in convertFromClientToServer call in line above,
                            // do attach the change notifier to it
                            if (instanceOfChangeAwareValue(converted[1]))
                                converted[1].getInternalState().setChangeListener(this.getChangeListener(newClientDataInited, idx));


                            // do not send to server if elem pushToServer is reject
                            if (!elemPropertyContext || elemPropertyContext.getPushToServerCalculatedValue() > PushToServerEnum.REJECT) toBeSentArray[idx] = converted[0];
                        }

                        if (internalState.calculatedPushToServerOfWholeProp === PushToServerEnum.REJECT) {
                            // if whole value is reject, don't sent anything
                            internalState.clearChanges(); // they are never going to be sent anyway so clear them
                            return [{ n: true }, newClientDataInited];
                        }
                    } else {
                        const granularUpdateChanges = changes as ICATGranularUpdatesToServer;
                        // send only changed indexes
                        const changedElements = granularUpdateChanges.u = [] as ICATGranularOpToServer[];

                        for (const [idx, oldVal] of arrayChanges) {
                            const newVal = newClientDataInited[idx];

                            let changed = (newVal !== oldVal);

                            if (!changed) {
                                if (instanceOfChangeAwareValue(newVal)) changed = newVal.getInternalState().hasChanges();
                                // else it's a dumb value change but we do not know the oldVal (it was marked as changed
                                // via markElementAsHavingDeepChanges probably (doesn't know old value), because proxies (if >= SHALLOW)
                                // know the oldValue and do check for changes in case of dumb values and don't mark it as changed if it was not)
                                // (assume the caller of markElementAsHavingDeepChanges knows what he is doing)
                                else changed = true;
                            }

                            if (changed) {
                               const ch = {} as ICATGranularOpToServer;
                               ch.i = idx;

                                const wasSmartBeforeConversion = instanceOfChangeAwareValue(newVal);
                                const converted = this.converterService.convertFromClientToServer(newVal, this.getElementType(internalState, idx), oldVal, elemPropertyContext);
                                ch.v = converted[0];
                                if (newVal !== converted[1]) newClientDataInited[idx] = converted[1];

                                if (!wasSmartBeforeConversion && instanceOfChangeAwareValue(converted[1]))
                                    // if it was a new object/array set at this index which was initialized by convertFromClientToServer call, do add the change notifier to it
                                    converted[1].getInternalState().setChangeListener(this.getChangeListener(newClientDataInited, idx));

                                changedElements.push(ch);
                            }
                        }
                    }

                    internalState.clearChanges();
                    return [changes, newClientDataInited];
                } else {
                    return [{ n: true }, newClientDataInited];
                }
            }
        } finally {
            internalState.ignoreChanges = false;
        }

        return null; // newClientDataInitedshould be undefined / null if we reach this code
    }

    private getChangeListener(arrayVal: CustomArrayValue<T>, c: number): ChangeListenerFunction {
        return (doNotPushNow?: boolean) => {
            const internalState = arrayVal.getInternalState();
            internalState.changedKeys.set(c, arrayVal[c]);
            internalState.notifyChangeListener(doNotPushNow);
        };
    }

    private getElementType(internalState: ArrayState, idx: number | string) {
        return this.staticElementType ? this.staticElementType : internalState.dynamicPropertyTypesHolder['' + idx];
    }

    private initArrayValue(arrayToInitialize: Array<any>, contentVersion: number,
                                pushToServerCalculatedValue: PushToServerEnum, force?: boolean): CustomArrayValue<T> {

        let proxiedArray: CustomArrayValue<T>;
        if (!instanceOfCustomArray(arrayToInitialize) || force) {
            // this setPrototypeOf seems to be faster (did some benchmarking) then creating a new CustomArrayValue and copying all elements over to it
            // and it is better then having just an interface instead of CustomArrayValue and using Object.defineProperties(...) to define methods of
            // that interface, because a lot more stuff is typed/checked at compile-time this way then the latter (with which it is comparable in performance)
            if (Object.getPrototypeOf(arrayToInitialize) !== CustomArrayValue.prototype) Object.setPrototypeOf(arrayToInitialize, CustomArrayValue.prototype);
            // TODO if initial array is not a standard array but something that extends Array, we could include the custom prototypes in the prototype chain between CustomObjectValue and array...

            const array = arrayToInitialize as CustomArrayValue<any>;

            array.initialize(contentVersion, (typeof pushToServerCalculatedValue != 'undefined' ? pushToServerCalculatedValue : PushToServerEnum.REJECT), this.pushToServerForElements);
            const internalState = array.getInternalState();

            // if elements have SHALLOW or DEEP pushToServer (either inherited from parent, anyway not REJECT), add a Proxy obj to intercept client side changes by ref to array elements
            // and send them to server;
            // the proxy is added in case of ALLOW as well but it will not trigger a send to server right away when it detects a change but it will just remember the changes
            // and tell parent to do the same for a later time, when it is possible that a manual dataChange/emit(for component/service root prop) is requested
            if (PushToServerUtils.combineWithChildStatic(internalState.calculatedPushToServerOfWholeProp, this.pushToServerForElements) >= PushToServerEnum.ALLOW) {
                // TODO should we add the proxy in all cases? just in order to detect reference changes for any smart children and make sure we update their changeListener?;
                //      but that would only be needed if ppl. start moving around properties with APIs (like foundsets) that are nested in arrays/objects - and have REJECT

                proxiedArray = new Proxy(array as CustomArrayValue<T>, this.getProxyHandler(internalState));
            } else proxiedArray = array as CustomArrayValue<T>;
        } else proxiedArray = arrayToInitialize as CustomArrayValue<T>;

        return proxiedArray;
    }

    /**
     * Handler for the Proxy object that will detect reference changes in the array where it is needed + any add/remove operations.
     * This implements the shallow PushToServer behavior.
     */
    private getProxyHandler(internalState: ArrayState): ProxyHandler<CustomArrayValue<T>> {
        const softProxyRevoker = new SoftProxyRevoker(this.logger);
        internalState.proxyRevokerFunc = softProxyRevoker.getRevokeFunction();
        // note - if a proxy was set, we know push to server for elements is ALLOW or higher, otherwise the proxy handler would not be registered
        return {
            set: (underlyingArray: CustomArrayValue<T>, prop: any, v: any) => {
                if (softProxyRevoker.isProxyDisabled() || internalState.shouldIgnoreChangesBecauseFromOrToServerIsInProgress()) return Reflect.set(underlyingArray, prop, v);

                // eslint-disable-next-line radix
                const i = Number.parseInt(prop);
                if (Number.isInteger(i)) {
                    const dontPushNow = PushToServerUtils.combineWithChildStatic(internalState.calculatedPushToServerOfWholeProp, this.pushToServerForElements) === PushToServerEnum.ALLOW;
                    if (i >= underlyingArray.length) {
                        const ret = Reflect.set(underlyingArray, prop, v);
                        // TODO make this smarter to be able to send this as well as granular updates - so a javascript port of java class ArrayGranularChangeKeeper
                        internalState.markAllChanged(true, dontPushNow); // element added
                        return ret;
                    } else {
                        internalState.setPropertyAndHandleChanges(underlyingArray, i, v, dontPushNow); // 1 element has changed by ref
                        return true;
                    }
                } else if ('length' === prop && underlyingArray.length !== v) {
                    // TODO make this smarter to be able to send this as well as granular updates - so a javascript port of java class ArrayGranularChangeKeeper
                    const dontPushNow = PushToServerUtils.combineWithChildStatic(internalState.calculatedPushToServerOfWholeProp, this.pushToServerForElements) === PushToServerEnum.ALLOW;
                    const ret = Reflect.set(underlyingArray, prop, v);
                    internalState.markAllChanged(true, dontPushNow); // length of array changed
                    return ret;
                }

                return Reflect.set(underlyingArray, prop, v);
            },

            deleteProperty: (underlyingArray: CustomArrayValue<T>, prop: any) => {
                if (softProxyRevoker.isProxyDisabled() || internalState.shouldIgnoreChangesBecauseFromOrToServerIsInProgress()) return Reflect.deleteProperty(underlyingArray, prop);

                // eslint-disable-next-line radix
                const i = Number.parseInt(prop);
                if (Number.isInteger(i) && i < underlyingArray.length) {
                    // in JS, delete arr[4] for example will not modify the length of the array, just set it to undefined...
                    const dontPushNow = PushToServerUtils.combineWithChildStatic(internalState.calculatedPushToServerOfWholeProp, this.pushToServerForElements) === PushToServerEnum.ALLOW;
                    internalState.setPropertyAndHandleChanges(underlyingArray, i, undefined, dontPushNow); // 1 element deleted
                    return true;
                }

                return Reflect.deleteProperty(underlyingArray, prop);
            }
        } as ProxyHandler<CustomArrayValue<T>>;
    }

}

export class ArrayState extends BaseCustomObjectState<number, Array<any>> {

    constructor(originalNonProxiedInstanceOfCustomObject: Array<any>, public readonly pushToServerForElements: PushToServerEnum) {
        super(originalNonProxiedInstanceOfCustomObject);
    }

}

const instanceOfCustomArray = <T>(obj: any): obj is CustomArrayValue<T> =>
    instanceOfChangeAwareValue(obj) && obj.getInternalState() instanceof ArrayState;


class CustomArrayValue<T> extends Array<T> implements ICustomArrayValue<T>, IChangeAwareValue, ICustomArray<T> {

    // NOTE: constructor and field initializers pf this class will never be called as this class is never instantiated;
    // instead, it is set on exiting arrays as a prototype (to avoid server JSON creating an array and then creating another new instance and copying over the elements...)
    // so to avoid this double array creation + copy, this class is used via Object.setPrototypeOf(arrayToInitialize, CustomArrayValue.prototype);
    // that means unfortunately that fields don't work properly, especially new ECMA private class fields so we can't use #internalState to
    // avoid iteration/enumeration/public access to/on it
    private __internalState: ArrayState; // ChangeAwareState.INTERNAL_STATE_MEMBER_NAME === "__internalState"

    initialize(contentVersion: number, calculatedPushToServerOfWholeProp: PushToServerEnum, pushToServerForElements: PushToServerEnum) {
        Object.defineProperty(this, ChangeAwareState.INTERNAL_STATE_MEMBER_NAME, {
            configurable: true,
            enumerable: false,
            writable: false,
            value: new ArrayState(this, pushToServerForElements)
        }); // we use Object.defineProperty to make the internal state not enumerable (so that it does not appear when iterating using 'in' or 'of')
        this.__internalState.contentVersion = contentVersion; // being full content updates, we don't care about the version, we just accept it
        this.__internalState.calculatedPushToServerOfWholeProp = calculatedPushToServerOfWholeProp;
        this.__internalState.dynamicPropertyTypesHolder = {};
    }

    /** do not call this method from component/service impls.; this state is meant to be used only by the property type impl. */
    getInternalState(): ArrayState {
        return this.__internalState;
    }

    markElementAsHavingDeepChanges(index: number): void {
        // this can be called by user for >= ALLOW pushToServer combined with 'object' type,
        // so simple JSON elements, that change by content, not reference
        const pushToServerOnElements = PushToServerUtils.combineWithChildStatic(this.__internalState.calculatedPushToServerOfWholeProp,
                        this.__internalState.pushToServerForElements);

        if (pushToServerOnElements >= PushToServerEnum.ALLOW) {
            this.__internalState.changedKeys.set(index, this[index]); // we do not really know what the old value was...

            // notify parent that changes are present, but trigger an actual push-to-server only if pushToServer is DEEP
            // SHALLOW will work/trigger push-to-server automatically through proxy array impl., and ALLOW doesn't need to trigger push right away
            this.__internalState.notifyChangeListener(pushToServerOnElements <= PushToServerEnum.SHALLOW);
        }
    }

    // deprecated stuff follows for backwards compatibility

    /** @deprecated see ICustomArray jsdoc */
    markForChanged(): void {
        // changes should automatically be noticed via the Proxy if spec has pushToServer >= ALLOW; for deep watched elements there's the new markElementAsHavingDeepChanges();
    }

    /** @deprecated see ICustomArray jsdoc */
    getStateHolder(): DeprecatedArrayState {
        return getDeprecatedCustomArrayState();
    }

}

// ------------------- typed JSON that will be received from server - START -------------------
/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json array converter type. Do not use. CAT abbrev. comes from CustomArrayType */
export type ICATDataFromServer = ICATFullValueFromServer | ICATGranularUpdatesFromServer | ICATNoOpFromServer;

const instanceOfFullValueFromServer = (obj: any): obj is ICATFullValueFromServer =>
            (obj && !!((obj as ICATFullValueFromServer).v));

const instanceOfGranularUpdatesFromServer = (obj: any): obj is ICATGranularUpdatesFromServer =>
            (obj && !!((obj as ICATGranularUpdatesFromServer).g));

const instanceOfNoOpFromServer = (obj: any): obj is ICATNoOpFromServer =>
            (obj && !!((obj as ICATNoOpFromServer).n));

/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json array converter interface. Do not use. CAT abbrev. comes from CustomArrayType */
export interface ICATNoOpFromServer {
    /** NO_OP */
    n: boolean;
}

/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json array converter interface. Do not use. CAT abbrev. comes from CustomArrayType */
export interface ICATFullValueFromServer {

    /** VALUE */
    v: any[];

    /** CONTENT_VERSION */
    vEr: number;

}

/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json array converter interface. Do not use. CAT abbrev. comes from CustomArrayType */
export interface ICATGranularUpdatesFromServer {

    /** GRANULAR_UPDATES */
    g: ICATGranularOpFromServer[];

    /** CONTENT_VERSION */
    vEr: number;

}

/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json array converter interface. Do not use. CAT abbrev. comes from CustomArrayType */
export interface ICATGranularOpFromServer {

    /** OP_ARRAY_START_END_TYPE an array of these three values */
    op: [startIndex: number, endIndex: number, opType: ICATOpTypeEnum];

    /** GRANULAR_UPDATE_DATA **/
    d: any[];

}

/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json array converter enum. Do not use. CAT abbrev. comes from CustomArrayType */
export enum ICATOpTypeEnum {
    CHANGED = 0,
    INSERT = 1,
    DELETE = 2
}
// ------------------- typed JSON that will be received from server - END -------------------

// ------------------- typed JSON that will be sent to server - START -------------------
/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json array converter type. Do not use. CAT abbrev. comes from CustomArrayType */
export type ICATDataToServer = ICATFullArrayToServer | ICATGranularUpdatesToServer | ICATNoOpToServer;

/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json array converter interface. Do not use. CAT abbrev. comes from CustomArrayType */
export interface ICATFullArrayToServer {
    /** VALUE */
    v: any[];

    /** CONTENT_VERSION server side <-> client side sync to make sure we don't end up granular updating something that has changed meanwhile */
    vEr: number;
}
/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json array converter interface. Do not use. CAT abbrev. comes from CustomArrayType */
export interface ICATGranularUpdatesToServer {
    /** CONTENT_VERSION server side <-> client side sync to make sure we don't end up granular updating something that has changed meanwhile */
    vEr: number;

    /** UPDATES */
    u: ICATGranularOpToServer[];
}
/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json array converter interface. Do not use. CAT abbrev. comes from CustomArrayType */
export interface ICATNoOpToServer {
    /** NO_OP */
    n: boolean;
}
interface ICATGranularOpToServer {

    /** INDEX */
    i: number;

    /** VALUE */
    v: any;

}
// ------------------- typed JSON that will be sent to server - END -------------------

