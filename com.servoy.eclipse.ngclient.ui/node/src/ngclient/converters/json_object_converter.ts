import { ConverterService, ChangeAwareState, instanceOfChangeAwareValue,
            SubpropertyChangeByReferenceHandler, IParentAccessForSubpropertyChanges, IChangeAwareValue, ChangeListenerFunction, SoftProxyRevoker, CASBackup } from '../../sablo/converter.service';
import { IType, IPropertyContext, ITypeFactory, PushToServerEnum, IPropertyDescription,
            ICustomTypesFromServer, ITypesRegistryForTypeFactories,
            PushToServerUtils, PropertyContext, ChildPropertyContextCreator, IPropertyContextGetterMethod } from '../../sablo/types_registry';
import { BaseCustomObject, LoggerFactory, LoggerService, SpecTypesService  } from '@servoy/public';
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
            private readonly converterService: ConverterService<unknown>, private readonly specTypesService: SpecTypesService,
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
            customTypesForThisSpec[customTypeName] = new CustomObjectType(this.converterService, this.specTypesService, this.logger, customTypeName);
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

/** implementers of this interface are generated via initCustomObjectValue */
class CustomObjectValue implements IChangeAwareValue, ICustomObjectValue {

    // NOTE: constructor and field initializers pf this class will never be called as this class is never instantiated;
    // instead, it is set on exiting objects as a prototype (to avoid server JSON creating an object and then creating another new instance and copying over the subProps...)
    // so to avoid this double object creation + copy, this class is used via Object.setPrototypeOf(objectToInitialize, CustomObjectValue.prototype);
    // that means unfortunately that fields don't work properly, especially new ECMA private class fields so we can't use #internalState to
    // avoid iteration/enumeration/public access to/on it
    __internalState: CustomObjectState; // ChangeAwareState.INTERNAL_STATE_MEMBER_NAME === "__internalState"

    initialize(contentVersion: number, calculatedPushToServerOfWholeProp: PushToServerEnum, propertyDescriptions: { [propName: string]: IPropertyDescription }) {
        Object.defineProperty(this, ChangeAwareState.INTERNAL_STATE_MEMBER_NAME, {
            configurable: true,
            enumerable: false,
            writable: false,
            value: new CustomObjectState(this, propertyDescriptions)
        }); // we use Object.defineProperty to make the internal state not enumerable (so that it does not appear when iterating using 'in' or 'of')
        this.__internalState.contentVersion = contentVersion; // being full content updates, we don't care about the version, we just accept it
        this.__internalState.calculatedPushToServerOfWholeProp = calculatedPushToServerOfWholeProp;
        this.__internalState.dynamicPropertyTypesHolder = {};
    }

    /** do not call this method from component/service impls.; this state is meant to be used only by the property type impl. */
    getInternalState(): CustomObjectState {
        return this.__internalState;
    }

    markSubPropertyAsHavingDeepChanges(subPropertyName: string): void {
        // this can be called by user for >= ALLOW pushToServer combined with 'object' type,
        // so simple JSON subproperties, that change by content, not reference
        const pushToServerOnSubProp = PushToServerUtils.combineWithChildStatic(this.__internalState.calculatedPushToServerOfWholeProp,
                                                    this.__internalState.propertyDescriptions[subPropertyName]?.getPropertyDeclaredPushToServer());

        if (pushToServerOnSubProp >= PushToServerEnum.ALLOW) {
            this.__internalState.changedKeys.set(subPropertyName, this[subPropertyName]);

            // notify parent that changes are present, but trigger an actual push-to-server oonly if pushToServer is DEEP
            // SHALLOW will work/trigger push-to-server automatically through proxy obj. impl., and ALLOW doesn't need to trigger push right away
            this.__internalState.notifyChangeListener(pushToServerOnSubProp <= PushToServerEnum.SHALLOW);
        }
    }

}

/** This is exported just in order to be useful in unit tests. Otherwise it's an internal json array converter interface. Do not use externally otherwise. */
export class CustomObjectType implements IType<CustomObjectValue> {

    private propertyDescriptions: { [propName: string]: IPropertyDescription };
    
    private static customObjectValuePrototypeWithDeprecated: any;
    static {
        let lastClonedProto: any;
        [CustomObjectType.customObjectValuePrototypeWithDeprecated, lastClonedProto] = CustomObjectType.clonePrototypeDeep(CustomObjectValue.prototype);
        Object.setPrototypeOf(lastClonedProto, BaseCustomObject.prototype); // for backwards compatibilility;
                // otherwise CustomObjectType.customObjectValuePrototypeWithDeprecated could just be CustomObjectValue.prototype
    }

    constructor(private converterService: ConverterService<unknown>, private readonly specTypesService: SpecTypesService,
        private readonly logger: LoggerService, private readonly fullyQulifiedTypeName: string) {}

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
                        this.propertyDescriptions, propertyContext?.getPushToServerCalculatedValue(), propertyContext?.isInsideModel);

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
                            this.propertyDescriptions, propertyContext?.getPushToServerCalculatedValue(), propertyContext?.isInsideModel);

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
        // note: oldClientData could be uninitialized (so not yet instance of CustomObjectValue) if a parent array decides to send itself fully when an
        // element/subproperty was added - in which case it will be the same as newClientData... at least until SVY-17854 gets implemented and then old would be null
        // as expected in that scenario; there was one more scenario for arrays at least (see comment from json_array_converter.ts) where getInternalState() could be
        // present on the oldValue but return undefined - so we check for that as well here

        let internalState: CustomObjectState;
        let newClientDataInited: CustomObjectValue;

        try {
            if (newClientData) {
                if (!instanceOfCustomObject(newClientData)) {
                    // revoke proxy on old value if present; new value is an old dumb/non-initialized one
                    if (oldClientData?.getInternalState && oldClientData.getInternalState()) oldClientData.getInternalState().destroyAndGetNonProxiedValueOfProp();

                    // this can happen when a new obj. value was set completely in browser
                    // any 'smart' child elements will initialize in their fromClientToServer conversion;
                    // set it up, make it 'smart' and mark it as all changed to be sent to server...
                    newClientDataInited = newClientData = this.initCustomObjectValue(newClientData, 1, propertyContext?.getPushToServerCalculatedValue());
                    internalState = newClientDataInited.getInternalState();

                    if (propertyContext?.isInsideModel) internalState.markAllChanged(false); // otherwise it will always be sent fully anyway
                    internalState.ignoreChanges = true;
                } else if (propertyContext?.isInsideModel && (newClientData !== oldClientData || !newClientData.getInternalState().hasChangeListener())) { // the hasChangeListener is meant to detect arguments that come as a api call to client and then are assigned to a property - and because of the now deprecated ServoyPublicService.sendServiceChanges that did not have an oldPropertyValue argument, here old was === new value and we couldn't detect it correctly as a change by ref
                    // the conversion happens for a value from the model (not handler/api arg or return value); and we see it as a change by ref

                    // revoke proxy on old value if present; new value is already initialized;
                    // this code means to make sure that values that are inside the model of components/services do change notifications to the correct parent even if relocated
                    // and that old values (that were before in the model but are no more) no longer send the change notifications
                    if (oldClientData?.getInternalState && oldClientData.getInternalState()) oldClientData.getInternalState().destroyAndGetNonProxiedValueOfProp();

                    // if a different smart value from the browser is assigned to replace old value it is a full value change; also adjust the version to it's new location

                    // clear old internal state and get non-proxied value in order to re-initialize/start fresh in the new location (old proxy would send change notif to wrong place)
                    // some things that need to be restored afterwards will be stored in "savedInternalState"
                    const savedInternalState = newClientData.getInternalState().saveInternalState();
                    newClientData = newClientData.getInternalState().destroyAndGetNonProxiedValueOfProp();
                    delete newClientData[ChangeAwareState.INTERNAL_STATE_MEMBER_NAME];

                    newClientDataInited = newClientData = this.initCustomObjectValue(newClientData, 1, propertyContext?.getPushToServerCalculatedValue(), true);
                    internalState = newClientDataInited.getInternalState();
                    internalState.restoreSavedInternalState(savedInternalState);

                    internalState.markAllChanged(false);
                    internalState.ignoreChanges = true;
                } else { // an already initialized value that is either the same value as before or it is used here as an argument or return value to api calls/handlers
                    newClientDataInited = newClientData; // it was already initialized in the past (it's not a new client side created value)
                    internalState = newClientDataInited.getInternalState();
                    internalState.ignoreChanges = true;
                }
            } else newClientDataInited = newClientData; // null/undefined

            if (newClientDataInited) {
                let calculatedPushToServerOfWholeProp: PushToServerEnum; 
                if (propertyContext.isInsideModel) {
                    internalState.calculatedPushToServerOfWholeProp = (typeof propertyContext?.getPushToServerCalculatedValue() != 'undefined' ? propertyContext?.getPushToServerCalculatedValue() : PushToServerEnum.REJECT);
                    calculatedPushToServerOfWholeProp = internalState.calculatedPushToServerOfWholeProp;
                } else calculatedPushToServerOfWholeProp = PushToServerEnum.ALLOW; // args/return values are always "allow"

                const propertyContextCreator = new ChildPropertyContextCreator(
                        this.getCustomObjectPropertyContextGetter(newClientDataInited, propertyContext),
                        this.propertyDescriptions, propertyContext?.getPushToServerCalculatedValue(), propertyContext?.isInsideModel);

                if (!propertyContext?.isInsideModel || internalState.hasChanges()) { // so either it has changes or it's used as an arg/return value to a handler/api call
                    const changes = {} as ICOTFullObjectToServer | ICOTGranularUpdatesToServer;
                    if (!propertyContext?.isInsideModel || internalState.hasFullyChanged()) { // fully changed or arg/return value of handler/api call
                        const fullChange = changes as ICOTFullObjectToServer;
                        // we can't rely/use the current contentVersion here because, in case of a change-by-reference in a service followed
                        // by a now deprecated ServoyPublicService.sendServiceChanges that did not have an oldPropertyValue argument, we sometimes do not have
                        // access to the old contentVersion to be able to use it... so full change from client will ignore old contentVersion on client and on server
                        // but that should not be a problem as those are meant more to ensure that granular updates don't happen on an wrong/obsolete value
                        changes.vEr = 0; // server treats this as a "don't check server content version as it's a full new value from client"

                        // we only reset client side contentVersion when sending full changes for model properties (so things that might have an equivalent on server);
                        // (args and return values to api/handlers should not reset client side state version when being sent to server (there they will be
                        // full new values anyway with no previous value - and not in sync with any client side value), because it is possible to send as argument or
                        // return value a value that is also present in the model at the same time, in which case this full send as an arg/return value should not
                        // alter client side version in the model - that version must remain unaltered, in sync with the server side version in the model)
                        if (propertyContext?.isInsideModel) internalState.contentVersion = 1; // start fresh;
                        
                        // send all
                        const toBeSentObj = fullChange.v = {};
                        for (const key of Object.keys(newClientDataInited)) {
                            const val = newClientDataInited[key];

                            // even if child value has only partial changes or no changes, do send the full subprop. value as we are sending full object value here
                            // that is, if this conversion is sending model values; otherwise (handler/api call arg/return values) it will always be sent fully anyway
                            if (instanceOfChangeAwareValue(val) && propertyContext?.isInsideModel) val.getInternalState().markAllChanged(false);

                            const converted = this.converterService.convertFromClientToServer(val, this.getPropertyType(internalState, key),
                                                oldClientData ? oldClientData[key] : undefined, propertyContextCreator.withPushToServerFor(key));
                            // TODO although this is a full change, we give oldClientData[key] (oldvalue) because server side does the same for some reason,
                            // but normally both should use undefined/null for old value of subprops as this is a full change; SVY-17854 is created for looking into this

                            toBeSentObj[key] = converted[0];

                            if (val !== converted[1]) newClientDataInited[key] = converted[1];
                            // if it's a nested obj/array or other smart prop that just got smart in convertFromClientToServer, attach the change notifier
                            if (instanceOfChangeAwareValue(converted[1]))
                                converted[1].getInternalState().setChangeListener(this.getChangeListener(newClientDataInited, key));

                            if (PushToServerUtils.combineWithChildStatic(calculatedPushToServerOfWholeProp, this.propertyDescriptions[key]?.getPropertyDeclaredPushToServer())
                                 === PushToServerEnum.REJECT) delete toBeSentObj[key]; // don't send to server pushToServer reject keys
                        }

                        if (propertyContext?.isInsideModel) internalState.clearChanges();

                        if (calculatedPushToServerOfWholeProp === PushToServerEnum.REJECT) {
                            // if whole value is reject, don't sent anything
                            return [{ n: true }, newClientDataInited];
                        } else return [changes, newClientDataInited];
                    } else {
                        changes.vEr = internalState.contentVersion;

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

                                const converted = this.converterService.convertFromClientToServer(newVal, this.getPropertyType(internalState, key), oldVal,
                                                        propertyContextCreator.withPushToServerFor(key));

                                ch.v = converted[0];
                                if (newVal !== converted[1]) newClientDataInited[key] = converted[1];

                                if (instanceOfChangeAwareValue(converted[1]))
                                    // if it was a new object/array set in this key, which was initialized by convertFromClientToServer call above, do add the change notifier to it (the same is true for values that move by reference in the model, their change listener must always be up-to-date)
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

    /**
     * @return [clonedProto, lastClonedProto] clonedProto that is returned it the clone of the given prototype; lastClonedProto is the inner most prototype
     *              that was cloned before Object.prototype (as the cloning is recursive).
     */
    private static clonePrototypeDeep(p: any): [any, any] {
        // used so that we can augment the property chain of server or client code created custom objects
        // so that CustomObjectValue.class is the first thing in their prototype but then the following prototypes on the value can remain unchanged
        // for each type of custom object value
        let protToUseAfterCloning = Object.getPrototypeOf(p);

        let lastClonedP: any;
        if (protToUseAfterCloning && protToUseAfterCloning !== Object.prototype)
            [protToUseAfterCloning, lastClonedP] = CustomObjectType.clonePrototypeDeep(protToUseAfterCloning);
        
        // shallow copy of "p"
        const clonedP = Object.create(
          protToUseAfterCloning,
          Object.getOwnPropertyDescriptors(p),
        );

        if (!lastClonedP) lastClonedP = clonedP;
        
        return [clonedP, lastClonedP];
    }

    private initCustomObjectValue(objectToInitialize: any, contentVersion: number,
                                pushToServerCalculatedValue: PushToServerEnum, force?: boolean): CustomObjectValue {

        let proxiedCustomObject: CustomObjectValue;
        if (!instanceOfChangeAwareValue(objectToInitialize) || force) {
            // this setPrototypeOf seems to be faster then creating a new CustomObjectValue and copying all elements over to it
            // and it is better then having just an interface for CustomObjectValue and adding via Object.defineProperties(...) all (deprecated or valid)
            // methods of that interface, because a lot more stuff is typed/checked at compile-time this way then the latter (with which it is comparable in performance)
            let protoOfObj = Object.getPrototypeOf(objectToInitialize);
            
            // if it doesn't yet extend our custom object behavior do integrate that into the prototype chain 
            if (protoOfObj !== CustomObjectValue.prototype && protoOfObj.constructor !== CustomObjectValue.prototype.constructor) { // so check if CustomObjectValue or a clone  is already there / either directly or as a clone (that has the same constructor)
                // add our functionality to it via prototype chain;
                let protoOfOurInternalImpl: any; 
                if (protoOfObj === Object.prototype) {
                    // if the previous proto was just Object.prototype (so it was just a plain object)
                    const registeredCustomObjectTypeConstructor = this.specTypesService.getRegisteredCustomObjectTypeConstructor(this.fullyQulifiedTypeName);
                    if (registeredCustomObjectTypeConstructor) {
                        // if it's a value that comes from the server and the component/service did register a class for it via SpecTypesService.registerCustomObjectType(...)
                        // do use that so that the component/service had nicely customized custom obj. values

                        // we can't use here CustomObjectValue.prototype because setting the prototype of that to what the obj. currently has would affect all custom
                        // objects in the system then, that is why clone is used here
                        let lastClonedProto: any;
                        [protoOfOurInternalImpl, lastClonedProto] = CustomObjectType.clonePrototypeDeep(CustomObjectValue.prototype);
                        Object.setPrototypeOf(lastClonedProto, registeredCustomObjectTypeConstructor.prototype); // we insert our proto on the obj itself and keep previous obj proto as proto of our impl - so keep what behavior it used to have...
                    } else {
                        // so this is likely a value that arrived from server (plain object), and the component has not registered a special class for this custom object type via specTypesService.register...
                        // this means that, for backwards compatibility we have to add BaseCustomObject as well in the prototype chain (for example collapsible.ts used custom object before without registering the type with specTypesService; but it expected .getStateHolder() to be there and it added changed keys etc.)
                        // we just use CustomObjectValue.prototype + the deprecated BaseCustomObject.prototype (which has as prototype Object.prototype as well)
                        protoOfOurInternalImpl = CustomObjectType.customObjectValuePrototypeWithDeprecated; 
                    }
                } else {
                    // if it had a different prototype before (so it has a class, it was not a plain JS object), we need a new CustomObjectValue() as prototype
                    // and we can safely set the prototype of that then to what the object now has - so it keeps both our behavior and the behavior it previously had

                    // we can't use here CustomObjectValue.prototype because setting the prototype of that to what the obj. currently has would affect all custom
                    // objects in the system then, that is why clone is used here
                    let lastClonedProto: any;
                    [protoOfOurInternalImpl, lastClonedProto] = CustomObjectType.clonePrototypeDeep(CustomObjectValue.prototype)
                    Object.setPrototypeOf(lastClonedProto, protoOfObj); // we insert our proto on the obj itself and keep previous obj proto as proto of our impl - so keep what behavior it used to have...
                } 
                Object.setPrototypeOf(objectToInitialize, protoOfOurInternalImpl);
            }

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
                proxiedCustomObject = new Proxy(object as CustomObjectValue, this.getProxyHandler(internalState));
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
        const softProxyRevoker = new SoftProxyRevoker(this.logger);
        internalState.proxyRevokerFunc = softProxyRevoker.getRevokeFunction();

        return {
            set: (underlyingCustomObject: CustomObjectValue, prop: any, v: any, receiver: any) => {
                if (softProxyRevoker.isProxyDisabled() || internalState.shouldIgnoreChangesBecauseFromOrToServerIsInProgress()) return Reflect.set(underlyingCustomObject, prop, v);

                const subPropCalculatedPushToServer = PushToServerUtils.combineWithChildStatic(internalState.calculatedPushToServerOfWholeProp,
                                                                    this.propertyDescriptions[prop]?.getPropertyDeclaredPushToServer());
                if (subPropCalculatedPushToServer > PushToServerEnum.REJECT) {
                    const dontPushNow = subPropCalculatedPushToServer === PushToServerEnum.ALLOW;
                    internalState.setPropertyAndHandleChanges(underlyingCustomObject, prop, v, dontPushNow); // 1 element has changed by ref
                    return true;
                } else return Reflect.set(underlyingCustomObject, prop, v, receiver);
            },

            deleteProperty: (underlyingCustomObject: CustomObjectValue, prop: any) => {
                if (softProxyRevoker.isProxyDisabled() || internalState.shouldIgnoreChangesBecauseFromOrToServerIsInProgress()) return Reflect.deleteProperty(underlyingCustomObject, prop);

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

export interface BCOSBackup extends CASBackup {
    dynamicPropertyTypesHolder: Record<string, any>;
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
    
    hasFullyChanged() {
        return super.hasChanges();
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

    public destroyAndGetNonProxiedValueOfProp(): VT {
        // this basically makes sure that the original thing will no longer be updated via the old proxy (which would notify changes to a wrong location...)
        if (this.proxyRevokerFunc) this.proxyRevokerFunc();
        return this.originalNonProxiedInstanceOfCustomObject;
    }
    
    public saveInternalState(): BCOSBackup {
        const superSBackup = super.saveInternalState() as BCOSBackup;
        superSBackup.dynamicPropertyTypesHolder = this.dynamicPropertyTypesHolder;
        // saves it before it's destroyed and recreated by caller; some things need to stay the same
        return superSBackup;
    }
    
    public restoreSavedInternalState(saved: BCOSBackup) {
        this.dynamicPropertyTypesHolder = saved.dynamicPropertyTypesHolder;
        super.restoreSavedInternalState(saved);
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

    constructor(originalNonProxiedInstanceOfCustomObject: any, public readonly propertyDescriptions: { [propName: string]: IPropertyDescription }) {
        super(originalNonProxiedInstanceOfCustomObject);
    }

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
