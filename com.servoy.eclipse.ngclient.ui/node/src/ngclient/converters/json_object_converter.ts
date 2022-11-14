import { ConverterService, ChangeAwareState, instanceOfChangeAwareValue,
            SubpropertyChangeByReferenceHandler, IParentAccessForSubpropertyChanges, IChangeAwareValue, ChangeListenerFunction } from '../../sablo/converter.service';
import { IType, IPropertyContext, ITypeFactory, PushToServerEnum, IPropertyDescription,
            ICustomTypesFromServer, ITypesRegistryForTypeFactories,
            PushToServerUtils, PropertyContext, ChildPropertyContextCreator, IPropertyContextGetterMethod } from '../../sablo/types_registry';
import { LoggerFactory, LoggerService, BaseCustomObject  } from '@servoy/public';
import { ICustomObjectValue } from '@servoy/public';

export class CustomObjectTypeFactory implements ITypeFactory<CustomObjectValue> {

    public static readonly TYPE_FACTORY_NAME = 'JSON_obj';

    private customTypesBySpecName: {
        [webObjectSpecName: string]: {
            [customTypeName: string]: CustomObjectType;
        };
    } = {};
    private readonly logger: LoggerService;

    constructor(private readonly typesRegistry: ITypesRegistryForTypeFactories,
            private readonly converterService: ConverterService,
            logFactory: LoggerFactory) {
        this.logger = logFactory.getLogger('CustomObjectTypeFactory');
    }

    getOrCreateSpecificType(specificTypeInfo: string, webObjectSpecName: string): CustomObjectType {
        const customTypesForThisSpec = this.customTypesBySpecName[webObjectSpecName];
        if (customTypesForThisSpec) {
            const coType = customTypesForThisSpec[specificTypeInfo];
            if (!coType) this.logger.error('[CustomObjectTypeFactory] cannot find custom object client side type "'
                + specificTypeInfo + '" for spec "' + webObjectSpecName
                + '"; no such custom object type was registered for that spec.; ignoring...');
            return coType;
        } else {
            this.logger.error('[CustomObjectTypeFactory] cannot find custom object client side type "'
                + specificTypeInfo + '" for spec "' + webObjectSpecName
                + '"; that spec. didn\'t register any client side types; ignoring...');
            return undefined;
        }
    }

    registerDetails(details: ICustomTypesFromServer, webObjectSpecName: string) {
        // ok we got the custom types section of a .spec file in details; do it similarly to what we do server-side:
        //   - first create empty shells for all custom types (because they might need to reference each other)
        //   - go through each custom type's sub-properties and assign the correct type to them (could be one of the previously created "empty shells")

        this.customTypesBySpecName[webObjectSpecName] = {};
        const customTypesForThisSpec = this.customTypesBySpecName[webObjectSpecName];

        // create empty CustomObjectType instances for all custom types in this .spec
        for (const customTypeName of Object.keys(details)) {
             // create just an empty type reference that will be populated below with child property types
            customTypesForThisSpec[customTypeName] = new CustomObjectType(this.converterService);
        }

        // set the sub-properties of each CustomObjectType to the correct IType
        for (const customTypeName of Object.keys(details)) {
            const customTypeDetails = details[customTypeName];
            const properties: { [propName: string]: IPropertyDescription } = {};
            for (const propertyName of Object.keys(customTypeDetails)) {
                properties[propertyName] = this.typesRegistry.processPropertyDescriptionFromServer(customTypeDetails[propertyName], webObjectSpecName);
            }
            customTypesForThisSpec[customTypeName].setPropertyDescriptions(properties);
        }
    }

}

/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json array converter interface. Do not use externally otherwise. */
export class CustomObjectType implements IType<CustomObjectValue> {

    private propertyDescriptions: { [propName: string]: IPropertyDescription };

    constructor(private converterService: ConverterService) {}

    // this will always get called once with a non-null param before the CustomObjectType is used for conversions;
    // it can't be given in constructor as types might depend on each other and they are all created before being 'linked'; see factory code above
    setPropertyDescriptions(propertyDescriptions: { [propertyName: string]: IPropertyDescription } ): void {
        this.propertyDescriptions = propertyDescriptions;
    }

    fromServerToClient(serverJSONValue: ICOTDataFromServer, currentClientValue: CustomObjectValue, propertyContext: PropertyContext): CustomObjectValue {
        let newValue: CustomObjectValue = currentClientValue;
        // remove old watches (and, at the end create new ones) to avoid old watches getting triggered by server side change
        let internalState: CustomObjectState = null;

        try {
            if (instanceOfFullValueFromServer(serverJSONValue)) {
                // full contents
                newValue = this.initCustomObjectValue(serverJSONValue.v, serverJSONValue.vEr, propertyContext?.getPushToServerCalculatedValue());
                internalState = newValue.getInternalState();
                internalState.ignoreChanges = true;

                const propertyContextCreator = new ChildPropertyContextCreator(
                        this.getCustomObjectPropertyContextGetter(newValue, propertyContext),
                        this.propertyDescriptions, propertyContext?.getPushToServerCalculatedValue());

                internalState.contentVersion = serverJSONValue.vEr;

                // remove change listeners from any smart subprop. values that are now obsolete
                if (currentClientValue) {
                    for (const k of Object.keys(currentClientValue)) {
                        const obsoleteSubProp = currentClientValue[k];
                        if (instanceOfChangeAwareValue(obsoleteSubProp)) {
                            obsoleteSubProp.getInternalState().setChangeListener(undefined);
                        }
                    }
                }

                // convert value received from server, store and add change listeners if needed for subproperties
                for (const c of Object.keys(newValue)) {
                    let subPropValue = newValue[c];

                    // if it is a typed prop. use the type to convert it, if it is a dynamic type prop (for example dataprovider of type date) do the same and store the type
                    newValue[c] = subPropValue = this.converterService.convertFromServerToClient(subPropValue, this.getStaticPropertyType(c), currentClientValue ? currentClientValue[c] : undefined,
                            internalState.dynamicPropertyTypesHolder, c, propertyContextCreator.withPushToServerFor(c));

                    if (instanceOfChangeAwareValue(subPropValue)) {
                        // child is able to handle it's own change mechanism
                        subPropValue.getInternalState().setChangeListener(this.getChangeListener(newValue, c));
                    }
                }
            } else if (instanceOfGranularUpdatesFromServer(serverJSONValue)) {
                // granular updates received;
                internalState = currentClientValue.getInternalState();
                internalState.ignoreChanges = true;

                // for example if a custom object value is initially received through a return value from server side api/handler call
                // and not as a normal model property and then the component/service assigns it to model, the push to server of it might have changed; use the one received here as arg
                internalState.calculatedPushToServerOfWholeProp = propertyContext?.getPushToServerCalculatedValue();
                if (!internalState.calculatedPushToServerOfWholeProp) internalState.calculatedPushToServerOfWholeProp = PushToServerEnum.REJECT;

                // if something changed browser-side, increasing the content version thus not matching next expected version,
                // we ignore this update and expect a fresh full copy of the object from the server (currently server value is leading/has priority
                // because not all server side values might support being recreated from client values)
                if (internalState.contentVersion === serverJSONValue.vEr) {
                    const updates = serverJSONValue.u;

                    const propertyContextCreator = new ChildPropertyContextCreator(
                            this.getCustomObjectPropertyContextGetter(currentClientValue, propertyContext),
                            this.propertyDescriptions, propertyContext?.getPushToServerCalculatedValue());

                    for (const update of updates) {
                        const key = update.k;
                        let val = update.v;

                        // remove change listeners from any smart el. values that are now obsolete
                        const oldElemValue = currentClientValue[key];
                        if (instanceOfChangeAwareValue(oldElemValue)) {
                            // child is able to handle it's own change mechanism
                            oldElemValue.getInternalState().setChangeListener(undefined);
                        }

                        currentClientValue[key] = val = this.converterService.convertFromServerToClient(val, this.getStaticPropertyType(key), currentClientValue[key],
                                internalState.dynamicPropertyTypesHolder, key, propertyContextCreator.withPushToServerFor(key));

                        if (instanceOfChangeAwareValue(val)) {
                            // child is able to handle it's own change mechanism
                            val.getInternalState().setChangeListener(this.getChangeListener(newValue, key));
                        }
                    }
                }
                //else {
                // else we got an update from server for a version that was already bumped by changes in browser; ignore that, as browser changes were sent to server
                // and server will detect the problem and send back a full update
                //}
            } else if (!instanceOfNoOpFromServer(serverJSONValue))
                // anything else would not be supported...
                newValue = null;
        } finally {
            if (internalState) internalState.ignoreChanges = false;
        }

        return newValue;
    }

    fromClientToServer(newClientData: CustomObjectValue, oldClientData: CustomObjectValue, propertyContext: IPropertyContext): [ICOTDataToServer, CustomObjectValue] | null {
        let internalState: CustomObjectState;
        let newClientDataInited: CustomObjectValue;

        try {
            if (newClientData) {
                if (!instanceOfCustomObject(newClientData)) {
                    // this can happen when a new obj. value was set completely in browser
                    // any 'smart' child elements will initialize in their fromClientToServer conversion;
                    // set it up, make it 'smart' and mark it as all changed to be sent to server...
                    newClientDataInited = newClientData = this.initCustomObjectValue(newClientData, oldClientData ? oldClientData.getInternalState().contentVersion : 0,
                              propertyContext?.getPushToServerCalculatedValue());
                    internalState = newClientDataInited.getInternalState();
                    internalState.markAllChanged(false);
                    internalState.ignoreChanges = true;
                } else if (newClientData !== oldClientData) {
                    // if a different smart value from the browser is assigned to replace old value it is a full value change; also adjust the version to it's new location

                    // clear old internal state and get non-proxied value in order to re-initialize/start fresh in the new location
                    newClientData = newClientData.getInternalState().destroyAndDeleteMeAndGetNonProxiedValueOfProp();

                    newClientDataInited = newClientData = this.initCustomObjectValue(newClientData, oldClientData ? oldClientData.getInternalState().contentVersion : 0,
                              propertyContext?.getPushToServerCalculatedValue());
                    internalState = newClientDataInited.getInternalState();
                    internalState.markAllChanged(false);
                    internalState.ignoreChanges = true;
                } else {
                    // new the same as old and already initialized
                    newClientDataInited = newClientData;
                    internalState = newClientDataInited.getInternalState();
                    internalState.ignoreChanges = true;

                    // for example if a custom array value is initially received through a return value from server side api/handler call and not as a
                    // normal model property and then the component/service assigns it to model, the push to server of it might have changed; use the one
                    // received here as arg
                    internalState.calculatedPushToServerOfWholeProp = propertyContext?.getPushToServerCalculatedValue();
                    if (!internalState.calculatedPushToServerOfWholeProp) internalState.calculatedPushToServerOfWholeProp = PushToServerEnum.REJECT;
                }
            } else newClientDataInited = newClientData; // null/undefined

            if (newClientDataInited) {
                const propertyContextCreator = new ChildPropertyContextCreator(
                        this.getCustomObjectPropertyContextGetter(newClientDataInited, propertyContext),
                        this.propertyDescriptions, propertyContext?.getPushToServerCalculatedValue());

                if (internalState.hasChanges()) {
                    const changes = {} as ICOTFullObjectToServer | ICOTGranularUpdatesToServer;
                    changes.vEr = internalState.contentVersion;
                    if (internalState.allChanged) {
                        const fullChange = changes as ICOTFullObjectToServer;
                        // structure might have changed; increase version number
                        ++internalState.contentVersion; // we also increase the content version number - server will bump version number on full value update
                        // send all
                        const toBeSentObj = fullChange.v = {};
                        for (const key of Object.keys(newClientDataInited)) {
                            const val = newClientDataInited[key];
                            if (instanceOfChangeAwareValue(val)) val.getInternalState().markAllChanged(false); // we are sending a full value to server so subprops must be full as well

                            const converted = this.converterService.convertFromClientToServer(val, this.getPropertyType(internalState, key),
                                                undefined, propertyContextCreator.withPushToServerFor(key));
                            toBeSentObj[key] = converted[0];

                            if (val !== converted[1]) newClientDataInited[key] = converted[1];
                            // if it's a nested obj/array or other smart prop that just got smart in convertFromClientToServer, attach the change notifier
                            if (instanceOfChangeAwareValue(converted[1]))
                                converted[1].getInternalState().setChangeListener(this.getChangeListener(newClientDataInited, key));

                            if (PushToServerUtils.combineWithChildStatic(internalState.calculatedPushToServerOfWholeProp, this.propertyDescriptions[key]?.getPropertyDeclaredPushToServer())
                                 === PushToServerEnum.REJECT) delete toBeSentObj[key]; // don't send to server pushToServer reject keys
                        }

                        internalState.clearChanges();

                        if (internalState.calculatedPushToServerOfWholeProp === PushToServerEnum.REJECT) {
                            // if whole value is reject, don't sent anything
                            return [{ n: true }, newClientDataInited];
                        } else return [changes, newClientDataInited];
                    } else {
                        const granularUpdateChanges = changes as ICOTGranularUpdatesToServer;
                        // send only changed keys
                        const changedElements = granularUpdateChanges.u = [] as ICOTGranularOpToServer[];
                        for (const key of internalState.changedKeys.keys()) {
                            const newVal = newClientDataInited[key];
                            const oldVal = internalState.changedKeys.get(key);

                            let changed = (newVal !== oldVal);
                            if (!changed) {
                                if (instanceOfChangeAwareValue(newVal)) changed = newVal.getInternalState().hasChanges();
                                // else it's a dumb value change but we do not know the oldVal (it was marked as changed
                                // via markSubPropertyAsHavingDeepChanges probably (doesn't know old value), because proxies (if >= SHALLOW)
                                // know the oldValue and do check for changes in case of dumb values and don't mark it as changed if it was not)
                                // (assume the caller of markSubPropertyAsHavingDeepChanges knows what he is doing)
                                else changed = true;
                            }

                            if (changed) {
                                const ch = {} as ICOTGranularOpToServer;
                                ch.k = key;

                                const wasSmartBeforeConversion = instanceOfChangeAwareValue(newVal);
                                const converted = this.converterService.convertFromClientToServer(newVal, this.getPropertyType(internalState, key), oldVal,
                                                        propertyContextCreator.withPushToServerFor(key));

                                ch.v = converted[0];
                                if (newVal !== converted[1]) newClientDataInited[key] = converted[1];
                                if (!wasSmartBeforeConversion && instanceOfChangeAwareValue(converted[1]))
                                    // if it was a new object/array set in this key, which was initialized by convertFromClientToServer call above, do add the change notifier to it
                                    converted[1].getInternalState().setChangeListener(this.getChangeListener(newClientDataInited, key));

                                changedElements.push(ch);
                            }
                        }
                        internalState.clearChanges();
                        return [changes, newClientDataInited];
                    }
                } else {
                    // no changes
                    return [{ n: true }, newClientDataInited];
                }
            }
        } finally {
            if (internalState) internalState.ignoreChanges = false;
        }

        return null; // newClientDataInited should be undefined / null if we reach this code
    }

    private getChangeListener(customObject: CustomObjectValue, prop: string | number): ChangeListenerFunction {
        return (doNotPushNow?: boolean) => {
            const internalState = customObject.getInternalState();
            internalState.changedKeys.set(prop, customObject[prop]);
            internalState.notifyChangeListener(doNotPushNow);
        };
    }

    private getCustomObjectPropertyContextGetter(customObjectValue: CustomObjectValue, parentPropertyContext: IPropertyContext): IPropertyContextGetterMethod {
        // property context that we pass here should search first in the current custom object value and only fallback to "parentPropertyContext" if needed
        return (propertyName) => {
            if (customObjectValue.hasOwnProperty(propertyName)) return customObjectValue[propertyName]; // can even be null or undefined as long as it is set on this object
            else return parentPropertyContext ? parentPropertyContext.getProperty(propertyName) : undefined; // fall back to parent object nesting context
        };
    }

    private getStaticPropertyType(propertyName: string) {
        return this.propertyDescriptions[propertyName]?.getPropertyType();
    }

    private getPropertyType(internalState: CustomObjectState, propertyName: string) {
        let propType = this.getStaticPropertyType(propertyName);
        if (!propType) propType = internalState.dynamicPropertyTypesHolder[propertyName];
        return propType;
    }

    private initCustomObjectValue(objectToInitialize: any, contentVersion: number,
                                pushToServerCalculatedValue: PushToServerEnum): CustomObjectValue {

        let proxiedCustomObject: CustomObjectValue;
        if (!instanceOfChangeAwareValue(objectToInitialize)) {
            // this setPrototypeOf seems to be faster then creating a new CustomObjectValue and copying all elements over to it
            // and it is better then having just an interface for CustomObjectValue and adding via Object.defineProperties(...) all (deprecated or valid)
            // methods of that interface, because a lot more stuff is typed/checked at compile-time this way then the latter (with which it is comparable in performance)
            Object.setPrototypeOf(objectToInitialize, CustomObjectValue.prototype);

            const object = objectToInitialize as CustomObjectValue;

            object.initialize(contentVersion, (typeof pushToServerCalculatedValue != 'undefined' ? pushToServerCalculatedValue : PushToServerEnum.REJECT), this.propertyDescriptions);
            const internalState = object.getInternalState();

            // if object & elements have SHALLOW or DEEP pushToServer, add a Proxy obj to intercept client side changes to object and send them to server;
            // the proxy is added in case of ALLOW or higher on the object itself as well because:
            //     - even for ALLOW it needs to be aware of what changed; but it will not trigger a send to server right away when it detects a change for
            //       an (just) ALLOW subprop, only for higher pushToServer; for ALLOW it will just remember the changes and tell parent to do the same
            //     - subProperties that have for example 'object' type and no declared pushToServer in .spec are not sent from server in client-side-spec, but
            //       they are still sendable to server as they inherit the pushToServer >= ALLOW from the custom object itself
            if (this.hasSubPropsWithAllowOrShallowOrDeep(internalState.calculatedPushToServerOfWholeProp)) {
                // TODO should we add the proxy in all cases? just in order to detect reference changes for any smart children and make sure we update their changeListener?;
                //      but that would only be needed if ppl. start moving around properties with APIs (like foundsets) that are nested in arrays/objects - and have REJECT

                const proxyRevoke = Proxy.revocable(object as CustomObjectValue, this.getProxyHandler(internalState));
                proxiedCustomObject = proxyRevoke.proxy;
                internalState.proxyRevokerFunc = proxyRevoke.revoke;
            } else proxiedCustomObject = object as CustomObjectValue;
        } else proxiedCustomObject = objectToInitialize as CustomObjectValue;

        return proxiedCustomObject;
    }

    private hasSubPropsWithAllowOrShallowOrDeep(pushToServerCalculatedValue: PushToServerEnum): boolean {
        return pushToServerCalculatedValue >= PushToServerEnum.ALLOW;

        // see comment above, in initCustomObjectValue; it is possible that server doesn't send any subprop from spec because they have neither client side
        // type nor subprop-level pushToServer; but still, those do inherit from parent custom object and we cannot check only the client-side-known subprops;
        // so it is enough here to check only the custom object pushToServer calculated value in order to decide if it needs a proxy or not
        // if (pushToServerCalculatedValue === PushToServerEnum.REJECT) return false;
        // for (const propertyDescription of Object.values(this.propertyDescriptions)) {
        //     if (PushToServerUtils.combineWithChildStatic(pushToServerCalculatedValue,
        //             propertyDescription.getPropertyDeclaredPushToServer()) >= PushToServerEnum.ALLOW) return true;
        // }
        // return false;
    }

    /**
     * Handler for the Proxy object that will detect reference changes in the object where it is needed
     * This implements the shallow PushToServer behavior.
     */
    private getProxyHandler(internalState: CustomObjectState) {
        return {
            set: (underlyingCustomObject: CustomObjectValue, prop: any, v: any) => {
                if (internalState.shouldIgnoreChangesBecauseFromOrToServerIsInProgress()) return Reflect.set(underlyingCustomObject, prop, v);

                const subPropCalculatedPushToServer = PushToServerUtils.combineWithChildStatic(internalState.calculatedPushToServerOfWholeProp,
                                                                    this.propertyDescriptions[prop]?.getPropertyDeclaredPushToServer());
                if (subPropCalculatedPushToServer > PushToServerEnum.REJECT) {
                    const dontPushNow = subPropCalculatedPushToServer === PushToServerEnum.ALLOW;
                    internalState.setPropertyAndHandleChanges(underlyingCustomObject, prop, v, dontPushNow); // 1 element has changed by ref
                    return true;
                } else return Reflect.set(underlyingCustomObject, prop, v);
            },

            deleteProperty: (underlyingCustomObject: CustomObjectValue, prop: any) => {
                if (internalState.shouldIgnoreChangesBecauseFromOrToServerIsInProgress()) return Reflect.deleteProperty(underlyingCustomObject, prop);

                const subPropCalculatedPushToServer = PushToServerUtils.combineWithChildStatic(internalState.calculatedPushToServerOfWholeProp,
                                                                    this.propertyDescriptions[prop]?.getPropertyDeclaredPushToServer());
                if (subPropCalculatedPushToServer > PushToServerEnum.REJECT) {
                    const dontPushNow = subPropCalculatedPushToServer === PushToServerEnum.ALLOW;
                    internalState.setPropertyAndHandleChanges(underlyingCustomObject, prop, undefined, dontPushNow); // 1 element deleted
                    return true;
                } else return Reflect.deleteProperty(underlyingCustomObject, prop);
            }
        };
    }

}

const instanceOfCustomObject = (obj: any): obj is CustomObjectValue =>
    instanceOfChangeAwareValue(obj) && obj.getInternalState() instanceof CustomObjectState;

/** implementers of this interface are generated via initCustomObjectValue */
interface CustomObjectValue extends IChangeAwareValue, ICustomObjectValue {

    /** do not call this methods from component/service impls.; this state is meant to be used only by the property type impl. */
    getInternalState(): CustomObjectState;

}

class CustomObjectValue extends BaseCustomObject implements IChangeAwareValue, ICustomObjectValue {

    #internalState: CustomObjectState; // private class member (really not visible when iterating using 'in' or 'of' or when trying to access it from the outside of this class)
    #propertyDescriptions: { [propName: string]: IPropertyDescription };

    initialize(contentVersion: number, calculatedPushToServerOfWholeProp: PushToServerEnum, propertyDescriptions: { [propName: string]: IPropertyDescription }) {
        this.#internalState = new CustomObjectState(this);
        this.#internalState.contentVersion = contentVersion; // being full content updates, we don't care about the version, we just accept it
        this.#internalState.calculatedPushToServerOfWholeProp = calculatedPushToServerOfWholeProp;
        this.#internalState.dynamicPropertyTypesHolder = {};
        this.#propertyDescriptions = propertyDescriptions;
    }

    /** do not call this method from component/service impls.; this state is meant to be used only by the property type impl. */
    getInternalState(): CustomObjectState {
        return this.#internalState;
    }

    markSubPropertyAsHavingDeepChanges(subPropertyName: string): void {
        // this can be called by user for >= ALLOW pushToServer combined with 'object' type,
        // so simple JSON subproperties, that change by content, not reference
        const pushToServerOnSubProp = PushToServerUtils.combineWithChildStatic(this.#internalState.calculatedPushToServerOfWholeProp,
                                                    this.#propertyDescriptions[subPropertyName]?.getPropertyDeclaredPushToServer());

        if (pushToServerOnSubProp >= PushToServerEnum.ALLOW) {
            this.#internalState.changedKeys.set(subPropertyName, this[subPropertyName]);

            // notify parent that changes are present, but trigger an actual push-to-server oonly if pushToServer is DEEP
            // SHALLOW will work/trigger push-to-server automatically through proxy obj. impl., and ALLOW doesn't need to trigger push right away
            this.#internalState.notifyChangeListener(pushToServerOnSubProp <= PushToServerEnum.SHALLOW);
        }
    }

}

export class BaseCustomObjectState<KeyT extends number | string, VT> extends ChangeAwareState implements IParentAccessForSubpropertyChanges<KeyT> {

    public contentVersion: number;

    public calculatedPushToServerOfWholeProp: PushToServerEnum;
    public dynamicPropertyTypesHolder: Record<string, any>;
    public ignoreChanges = false;

    public changedKeys = new Map<KeyT, any>(); // changed key/index -> oldValue of that subprop
    public proxyRevokerFunc?: () => void;

    private readonly subPropertyChangeByReferenceHandler: SubpropertyChangeByReferenceHandler = new SubpropertyChangeByReferenceHandler(this);

    constructor(private readonly originalNonProxiedInstanceOfCustomObject: VT) {
        super();
    }

    hasChanges() {
        return super.hasChanges() || this.changedKeys.size > 0;
    }

    /**
     * @param doNotPushNow if this is true, then the change notification is not meant to trigger the push to server right away (but just notify that there are changes
     *                  in case the implementor needs to know that). Otherwise, if it is false of undefined, it should trigger an actual push to server as well.
     */
    markAllChanged(notifyListener: boolean, doNotPushNow?: boolean) {
        if (!this.ignoreChanges) super.markAllChanged(notifyListener, doNotPushNow);
    }

    public clearChanges() {
        super.clearChanges();
        this.changedKeys.clear();
    }

    public destroyAndDeleteMeAndGetNonProxiedValueOfProp(): VT {
        // this basically makes sure that the original thing will no longer be updated via the old proxy (which would notify changes to a wrong location...)
        if (this.proxyRevokerFunc) this.proxyRevokerFunc();

        return this.originalNonProxiedInstanceOfCustomObject;
    }

    public shouldIgnoreChangesBecauseFromOrToServerIsInProgress(): boolean {
        return this.ignoreChanges;
    }

    public changeNeedsToBePushedToServer(key: KeyT, oldValue: any, doNotPushNow?: boolean): void {
        if (!this.changedKeys.has(key)) {
            this.changedKeys.set(key, oldValue);
            this.notifyChangeListener(doNotPushNow);
        }
    }

    public setPropertyAndHandleChanges(customObjectOrArray: any, propertyName: KeyT, value: any, doNotPushNow?: boolean) {
        this.subPropertyChangeByReferenceHandler.setPropertyAndHandleChanges(customObjectOrArray, propertyName, value, doNotPushNow);
    }

}

class CustomObjectState extends BaseCustomObjectState<any, any> {

}

// ------------------- typed JSON that will be received from server - START -------------------
/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json object converter type. Do not use. COT abbrev. comes from CustomObjectType */
export type ICOTDataFromServer = ICOTFullValueFromServer | ICOTGranularUpdatesFromServer | ICOTNoOpFromServer;

const instanceOfFullValueFromServer = (obj: any): obj is ICOTFullValueFromServer =>
            (obj && !!((obj as ICOTFullValueFromServer).v));

const instanceOfGranularUpdatesFromServer = (obj: any): obj is ICOTGranularUpdatesFromServer =>
            (obj && !!((obj as ICOTGranularUpdatesFromServer).u));

const instanceOfNoOpFromServer = (obj: any): obj is ICOTNoOpFromServer =>
            (obj && !!((obj as ICOTNoOpFromServer).n));

/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json object converter interface. Do not use. COT abbrev. comes from CustomObjectType */
export interface ICOTNoOpFromServer {
    /** NO_OP */
    n: boolean;
}

/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json object converter interface. Do not use. COT abbrev. comes from CustomObjectType */
export interface ICOTFullValueFromServer {

    /** VALUE */
    v: Record<string, any>;

    /** CONTENT_VERSION */
    vEr: number;

}

/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json object converter interface. Do not use. COT abbrev. comes from CustomObjectType */
export interface ICOTGranularUpdatesFromServer {

    /** GRANULAR_UPDATES */
    u: ICOTGranularOpFromServer[];

    /** CONTENT_VERSION */
    vEr: number;

}

/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json object converter interface. Do not use. COT abbrev. comes from CustomObjectType */
export interface ICOTGranularOpFromServer {

    /** KEY */
    k: string;

    /** VALUE **/
    v: any;

}

// ------------------- typed JSON that will be received from server - END -------------------

// ------------------- typed JSON that will be sent to server - START -------------------
type ICOTDataToServer = ICOTFullObjectToServer | ICOTGranularUpdatesToServer | ICOTNoOpToServer;

/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json array converter interface. Do not use. CAT abbrev. comes from CustomArrayType */
export interface ICOTFullObjectToServer {
    /** VALUE */
    v: Record<string, any>;

    /** CONTENT_VERSION server side <-> client side sync to make sure we don't end up granular updating something that has changed meanwhile */
    vEr: number;
}
/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json array converter interface. Do not use. CAT abbrev. comes from CustomArrayType */
export interface ICOTGranularUpdatesToServer {
    /** CONTENT_VERSION server side <-> client side sync to make sure we don't end up granular updating something that has changed meanwhile */
    vEr: number;

    /** UPDATES */
    u: ICOTGranularOpToServer[];
}
/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json array converter interface. Do not use. CAT abbrev. comes from CustomArrayType */
export interface ICOTNoOpToServer {
    /** NO_OP */
    n: boolean;
}
/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json object converter interface. Do not use. COT abbrev. comes from CustomObjectType */
export interface ICOTGranularOpToServer {

    /** KEY */
    k: string;

    /** VALUE */
    v: any;

}
// ------------------- typed JSON that will be sent to server - END -------------------
