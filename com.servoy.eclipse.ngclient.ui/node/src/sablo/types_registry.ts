import { Injectable } from '@angular/core';
import { LoggerService, LoggerFactory, LogLevel } from '@servoy/public';

// types useful for when type information is received from server and processed; what is used from the rest
// of the client side code (that does not care about how types are received from server) should only use
// sablo.ITypesRegistry and others classes/interfaces that are not aware of server-to-client websocket format
// for client types

@Injectable({
    providedIn: 'root'
})
/** This class holds (registers and provides) information about all service specifications/client side types and all needed component specifications/client side types. See also interface doc. */
export class TypesRegistry implements ITypesRegistryForTypeFactories, ITypesRegistryForSabloConverters {

    private componentSpecifications: ObjectOfWebObjectSpecification = {};
    private serviceSpecifications: ObjectOfWebObjectSpecification = {};
    private typeFactoryRegistry: ITypeFactoryRegistry = new TypeFactoryRegistry();
    private types: ObjectOfIType = {}; // simple (don't need a factory to create more specific sub-types) global types that need client-side conversion
    private readonly logger: LoggerService;

    constructor(logFactory: LoggerFactory) {
        this.logger = logFactory.getLogger('TypesRegistry');
    }

    getTypeFactoryRegistry(): ITypeFactoryRegistry  {
        return this.typeFactoryRegistry;
    }

    /**
     * @param onlyIfNotAlreadyRegistered default: false; if true, it will not overwrite a previously added type.
     */
    registerGlobalType(typeName: string, theType: IType<any>, onlyIfNotAlreadyRegistered?: boolean) {
        if (!theType) throw new Error('You cannot register a null/undefined global type for \'' + typeName + '\'!');
        if (this.types[typeName]) {
            this.logger.debug('[TypesRegistry] registerGlobalType - a global type with the same name (' + typeName + ') was already previously registered. Old: '
                + this.types[typeName].constructor['name'] + ', New: ' + theType.constructor['name'] + '. The old one will be' + (onlyIfNotAlreadyRegistered ? 'kept' : ' discarded.'));
            if (!onlyIfNotAlreadyRegistered) this.types[typeName] = theType;
        } else this.types[typeName] = theType;
    }

    getAlreadyRegisteredType(typeFromServer: ITypeFromServer): IType<any> {
        let t: IType<any>;
        if (typeof typeFromServer == 'string') {
            t = this.types[typeFromServer];
            if (!t) this.logger.error('[TypeRegistry] getAlreadyRegisteredType: cannot find simple client side type \''
                        + typeFromServer + '\'; no such type was registered in client side code; ignoring...');
        }
        return t;
    }

    processTypeFromServer(typeFromServer: ITypeFromServer, webObjectSpecName: string): IType<any> {
        if (typeof typeFromServer == 'string') {
            const t = this.types[typeFromServer];
            if (!t) this.logger.error('[TypeRegistry] processTypeFromServer: cannot find simple client side type \''
                            + typeFromServer + '\'; no such type was registered in client side code; ignoring...');
            return t;
        } else {
            const factoryTypeFromServer = typeFromServer as [string, Record<string, unknown>] ;
            // it's a factory created type; get the actual specific type name from the factory
            const typeFactory = this.typeFactoryRegistry.getTypeFactory(factoryTypeFromServer[0]);
            if (!typeFactory) {
                this.logger.error('[TypeRegistry] trying to process factory type into actual specific type for a factory type with name \''
                        + factoryTypeFromServer[0] + '\' but no such factory is registered...');
                return null;
            } else {
                return typeFactory.getOrCreateSpecificType(factoryTypeFromServer[1], webObjectSpecName);
            }
        }
    }

    processPropertyDescriptionFromServer(propertyDescriptionFromServer: IPropertyDescriptionFromServer, webObjectSpecName: string): IPropertyDescription {
        if (propertyDescriptionFromServer instanceof Array || typeof propertyDescriptionFromServer == 'string') {
            // it's just a type, no pushToServer value
            const t = this.processTypeFromServer(propertyDescriptionFromServer as ITypeFromServer, webObjectSpecName);
            return t ? new PropertyDescription(t) : undefined;
        } else {
            // it's a PD, type && pushToServer
            const propertyDescriptionWithMultipleEntries = propertyDescriptionFromServer as IPropertyDescriptionFromServerWithMultipleEntries;
            return new PropertyDescription(
                    propertyDescriptionWithMultipleEntries.t ? this.processTypeFromServer(propertyDescriptionWithMultipleEntries.t, webObjectSpecName) : undefined,
                    PushToServerUtils.valueOf(propertyDescriptionWithMultipleEntries.s));
        }
    }

    // METHODS that are not in sablo.ITypesRegistry start here:

    /** Add a bunch of component specifications/client side types/push to server values that the server has sent to the registry; they will be needed client side. */
    addComponentClientSideSpecs(componentSpecificationsFromServer: IWebObjectTypesFromServer) {
        if (this.logger.logLevel >= LogLevel.SPAM)
            this.logger.spam('[typesRegistry] Adding component specifications for: ' + JSON.stringify(componentSpecificationsFromServer, undefined, 2));

        for (const componentSpecName of Object.keys(componentSpecificationsFromServer)) {
            this.componentSpecifications[componentSpecName] = this.processWebObjectSpecificationFromServer(componentSpecName, componentSpecificationsFromServer[componentSpecName]);
        }
    }

    /** The server sent all the service specifications/client side types/push to server values. Those are always sent initially as you never know when client side code might call a service... */
    setServiceClientSideSpecs(serviceSpecificationsFromServer: IWebObjectTypesFromServer) {
        if (this.logger.logLevel >= LogLevel.SPAM) this.logger.spam('[typesRegistry] Setting service specifications for: ' + JSON.stringify(serviceSpecificationsFromServer, undefined, 2));

        this.serviceSpecifications = {};
        for (const serviceSpecName of Object.keys(serviceSpecificationsFromServer)) {
            this.serviceSpecifications[serviceSpecName] = this.processWebObjectSpecificationFromServer(serviceSpecName, serviceSpecificationsFromServer[serviceSpecName]);
        }
    }

    getComponentSpecification(componentSpecName: string): IWebObjectSpecification {
        return this.componentSpecifications[componentSpecName];
    }

    getServiceSpecification(serviceSpecName: string): IWebObjectSpecification {
        return this.serviceSpecifications[serviceSpecName];
    }

    /**
     * @param webObjectSpecificationFromServer see ***ClientSideTypeCache.java method buildClientSideTypesFor*** javadoc that describes what we receive here.
     */
    private processWebObjectSpecificationFromServer(webObjectSpecName: string, webObjectSpecificationFromServer: IWebObjectSpecificationFromServer): WebObjectSpecification {

        // first create the custom object types defined in this spec ('ftd' stands for factory type details)
        if (webObjectSpecificationFromServer.ftd) this.processFactoryTypeDetails(webObjectSpecificationFromServer.ftd, webObjectSpecName);

        let properties: ObjectOfIPropertyDescription;
        let handlers: ObjectOfIEventHandlerFunctions;
        let apiFunctions: ObjectOfIWebObjectFunctions;

        // properties
        if (webObjectSpecificationFromServer.p) {
            properties = {};
            for (const propertyName of Object.keys(webObjectSpecificationFromServer.p)) {
                properties[propertyName] = this.processPropertyDescriptionFromServer(webObjectSpecificationFromServer.p[propertyName], webObjectSpecName);
            }
        }

        // handlers
        if (webObjectSpecificationFromServer.h) {
            handlers = {};
            for (const handlerName of Object.keys(webObjectSpecificationFromServer.h)) {
                handlers[handlerName] = this.processEventHandler(webObjectSpecificationFromServer.h[handlerName], webObjectSpecName);
            }
        }

        // api functions
        if (webObjectSpecificationFromServer.a) {
            apiFunctions = {};
            for (const apiFunctionName of Object.keys(webObjectSpecificationFromServer.a)) {
                apiFunctions[apiFunctionName] = this.processApiFunction(webObjectSpecificationFromServer.a[apiFunctionName], webObjectSpecName);
            }
        }

        return new WebObjectSpecification(webObjectSpecName, properties, handlers, apiFunctions);
    }

    private processFactoryTypeDetails(factoryTypeDetails: IFactoryTypeDetails, webObjectSpecName: string): void {
        // currently this is used only for custom object types
        for (const factoryName of Object.keys(factoryTypeDetails)) {
            const typeFactory = this.typeFactoryRegistry.getTypeFactory(factoryName);
            if (!typeFactory) this.logger.error('[TypeRegistry] trying to add details to a factory type with name \'' + factoryName + '\' but no such factory is registered client-side...');
            else {
                typeFactory.registerDetails(factoryTypeDetails[factoryName], webObjectSpecName);
            }
        }
    }

    private processEventHandler(functionFromServer: IEventHandlerFromServer, webObjectSpecName: string): IEventHandler {
        return new WebObjectEventHandler(...this.processFunction(functionFromServer, webObjectSpecName), functionFromServer.iBDE);
    }

    private processApiFunction(functionFromServer: IWebObjectFunctionFromServer, webObjectSpecName: string): IWebObjectFunction {
        return new WebObjectFunction(...this.processFunction(functionFromServer, webObjectSpecName));
    }

    private processFunction(functionFromServer: IWebObjectFunctionFromServer, webObjectSpecName: string): [IType<any>, ObjectOfITypeWithNumberKeys] {
        let returnType: IType<any>;
        let argumentTypes: ObjectOfITypeWithNumberKeys;

        if (functionFromServer.r) returnType = this.processTypeFromServer(functionFromServer.r, webObjectSpecName);
        for (const argIdx in functionFromServer) {
            if (argIdx !== 'r' && argIdx !== 'iBDE') {
                if (!argumentTypes) argumentTypes = {};
                argumentTypes[argIdx] = this.processTypeFromServer(functionFromServer[argIdx], webObjectSpecName);
            }
        }
        return [returnType, argumentTypes];
    }

}

export interface ObjectOfWebObjectSpecification { [key: string]: IWebObjectSpecification }
export interface ObjectOfIType { [key: string]: IType<any> }

export class RootPropertyContextCreator implements IPropertyContextCreator {

    constructor(private readonly getProperty: IPropertyContextGetterMethod, private readonly webObjectSpec: IWebObjectSpecification) {}

    withPushToServerFor(rootPropertyName: string): PropertyContext {
        return new PropertyContext(this.getProperty, this.webObjectSpec ? this.webObjectSpec.getPropertyPushToServer(rootPropertyName)
                        : PushToServerEnum.REJECT, true); // getPropertyPushToServer not getPropertyDeclaredPushToServer
    }
}

export class ChildPropertyContextCreator implements IPropertyContextCreator {

    constructor(private readonly getProperty: IPropertyContextGetterMethod,
            private readonly propertyDescriptions: { [propName: string]: IPropertyDescription },
            private readonly computedParentPushToServer: PushToServerEnum,
            private readonly isInsideModel: boolean) {}

    withPushToServerFor(childPropertyName: string): PropertyContext {
        return new PropertyContext(this.getProperty, PushToServerUtils.combineWithChildStatic(this.computedParentPushToServer,
                    this.propertyDescriptions[childPropertyName]?.getPropertyDeclaredPushToServer()), this.isInsideModel); // getPropertyDeclaredPushToServer not getPropertyPushToServer
    }
}

export class PropertyContext implements IPropertyContext {

    constructor(public readonly getProperty: IPropertyContextGetterMethod,
        private readonly pushToServerComputedValue: PushToServerEnum,
        public readonly isInsideModel: boolean) {}

    getPushToServerCalculatedValue(): PushToServerEnum {
        return this.pushToServerComputedValue;
    }
}

export enum PushToServerEnum {

    /**
     * Default PushToServerEnum; it will throw an exception server-side when updates are pushed to server for this property.
     * Client should never attempt/allow a push to server (neither automatic nor manual) on properties that have REJECT (default) push-to-server.
     */
    REJECT = 0,

    /**
     * Allow changes to be sent to server for this property (manually triggered by component/service code/directives that use that prop.).
     * Manual triggering can be done using .emit(value) on the component @ Output (and in case of services via ServoyPublicService.sendServiceChanges()) of the root property that is/contains the changed value (even if it intends to send a subprop/element of the root property that only has ALLOW pushToServer).
     * Before using .emit(...)/.sendServiceChanges(...), in the rare cases where you use an 'object' type in the .spec file for elements of custom arrays or sub-properties of typed custom objects, and the content of that value is a nested JSON, in order for the custom objects type/custom array type to 'see' the changes nested inside the JSON of the plain 'object' value, you can use either ICustomObjectValue.markSubPropertyAsHavingDeepChanges(subPropertyName: string) or ICustomArrayValue.markElementAsHavingDeepChanges(index: number).
     */
    ALLOW = 1,

    /**
     * Client code will automatically react when components/services change by reference the values inside nested custom objects/arrays/... marked with SHALLOW push-to-server.
     * These new values will automatically be sent to server. But it does not work for root properties that change by ref.
     * Root property change-by-ref in Titanium always needs .emit(value) to be called on the @ Output (and in case of services via ServoyPublicService.sendServiceChanges()) of that property. (because only that emit will actually update that ref. in the sablo model of the component/service, otherwise it's just a variable change by ref. inside the component/service itself)
     * Changes nested inside untyped nested JSON values ('object' in .spec) need to be triggered manually as well, as they are not changes-by ref of that 'object' value; see description from 'allow'.
     *
     * Impl detail: This is done using Proxy client side objects that 'wrap' custom objects/arrays/... . But that won't work for root properties, as those are just @ Input vars inside the component/service, so a Proxy inside the sablo component/service value would not detect that.
     */
    SHALLOW = 2,

    /**
     * NG2/Titanium does not have deep watches (to detect nested changes in plain 'object' typed-in-spec JSON values); it can only handle automatically SHALLOW to a limited extent.
     * (see doc from SHALLOW) through proxies and it will do the same for DEEP.
     * 
     * Changes nested inside untyped nested JSON values ('object' in spec) need to be triggered by component/service client-side code manually using one of:
     * - emit(value) on the component @ Output (and in case of services via ServoyPublicService.sendServiceChanges()) of the root property that is/contains the changed values (even if it intends to send a subprop/element of the root property that only has ALLOW pushToServer)
     * - ICustomObjectValue.markSubPropertyAsHavingDeepChanges(subPropertyName: string) if the deep untyped JSON that has changes is a subproperty of a typed custom object from .spec
     * - ICustomArrayValue.markElementAsHavingDeepChanges(index: number) if the deep untyped JSON that has changes is and element of a custom array from .spec
     */
    DEEP = 3

}

export class PushToServerUtils {


    public static readonly PROPERTY_CONTEXT_FOR_INCOMMING_ARGS_AND_RETURN_VALUES: IPropertyContext = {
        isInsideModel: false,
        // arguments/return values received from server in case of api calls/handlers are not properties of a component or service so can't return sibling properties
        getProperty: (_propertyName: string): any  => undefined,
        getPushToServerCalculatedValue: () => PushToServerEnum.REJECT
    };

    public static readonly PROPERTY_CONTEXT_FOR_OUTGOING_ARGS_AND_RETURN_VALUES: IPropertyContext = {
        isInsideModel: false,
        // arguments/return values sent to server in case of api calls/handlers are not properties of a component or service so can't return sibling properties
        getProperty: (_propertyName: string): any  => undefined,
        getPushToServerCalculatedValue: () => PushToServerEnum.ALLOW
    };

    public static valueOf(pushToServerRawValue: PushToServerEnumServerValue): PushToServerEnum {
        if (pushToServerRawValue === undefined) throw new Error('pushToServerRawValue cannot be present but undefined!');
        else if (pushToServerRawValue < PushToServerEnum.REJECT || pushToServerRawValue > PushToServerEnum.DEEP) throw new Error('pushToServerRawValue out of bounds! ->' + pushToServerRawValue);

        return pushToServerRawValue;
    }

    public static combineWithChildStatic(parentComputedPushToServer: PushToServerEnum, childDeclaredPushToServer: PushToServerEnum) {
        let computed: PushToServerEnum;
        if (typeof parentComputedPushToServer == 'undefined') parentComputedPushToServer = PushToServerEnum.REJECT; // so parent can never be undefined; it would be reject then

        if (parentComputedPushToServer === PushToServerEnum.REJECT || childDeclaredPushToServer === PushToServerEnum.REJECT) computed = PushToServerEnum.REJECT;
        else {
            // parent is not reject; child is not reject; all other values are inherited if not present in child or replaced by child value if present
            if (childDeclaredPushToServer == null) computed = parentComputedPushToServer; // parent cannot be undefined
            else computed = childDeclaredPushToServer;
        }

        return computed;
    }

    public static newRootPropertyContextCreator(getProperty: IPropertyContextGetterMethod, webObjectSpec: IWebObjectSpecification): IPropertyContextCreator {
        return new RootPropertyContextCreator(getProperty, webObjectSpec);
    }

    public static newChildPropertyContextCreator(getProperty: IPropertyContextGetterMethod,
                propertyDescriptions: { [propName: string]: IPropertyDescription },
                computedParentPushToServer: PushToServerEnum, isInsideModel: boolean): IPropertyContextCreator {
        return new ChildPropertyContextCreator(getProperty, propertyDescriptions, computedParentPushToServer, isInsideModel);
    }

}

export interface ObjectOfITypeFactory { [key: string]: ITypeFactory<any> }

class TypeFactoryRegistry implements ITypeFactoryRegistry {

    private readonly typeFactories: ObjectOfITypeFactory = {};

    getTypeFactory(typeFactoryName: string): ITypeFactory<any> {
        return this.typeFactories[typeFactoryName];
    }

    contributeTypeFactory(typeFactoryName: string, typeFactory: ITypeFactory<any>) {
        this.typeFactories[typeFactoryName] = typeFactory;
    }

}

export interface ObjectOfIPropertyDescription { [key: string]: IPropertyDescription }
export interface ObjectOfIWebObjectFunctions { [key: string]: IWebObjectFunction }
export interface ObjectOfIEventHandlerFunctions { [key: string]: IEventHandler }

class PropertyDescription implements IPropertyDescription {

    constructor(
            private readonly propertyType?: IType<any>,
            private readonly pushToServer?: PushToServerEnum,
        ) {}

    getPropertyType(): IType<any> {
        return this.propertyType;
    }

    getPropertyDeclaredPushToServer(): PushToServerEnum {
        return this.pushToServer;
    }

    getPropertyPushToServer(): PushToServerEnum {
        return this.pushToServer ? this.pushToServer : PushToServerEnum.REJECT;
    }

}

class WebObjectSpecification implements IWebObjectSpecification {

    constructor(
        public readonly webObjectType: string,
        private readonly propertyDescriptions?: ObjectOfIPropertyDescription,
        private readonly handlers?: ObjectOfIWebObjectFunctions,
        private readonly apiFunctions?: ObjectOfIWebObjectFunctions
    ) {}

    getPropertyDescription(propertyName: string): IPropertyDescription {
        return this.propertyDescriptions ? this.propertyDescriptions[propertyName] : undefined;
    }

    getPropertyType(propertyName: string): IType<any> {
        return this.propertyDescriptions ? this.propertyDescriptions[propertyName]?.getPropertyType() : undefined;
    }

    getPropertyDeclaredPushToServer(propertyName: string): PushToServerEnum {
        return this.propertyDescriptions ? this.propertyDescriptions[propertyName]?.getPropertyDeclaredPushToServer() : undefined;
    }

    getPropertyPushToServer(propertyName: string): PushToServerEnum {
        if (!this.propertyDescriptions || !this.propertyDescriptions[propertyName]) return PushToServerEnum.REJECT;
        return this.propertyDescriptions[propertyName].getPropertyPushToServer();
    }

        /** this can return null if no property descriptions needed to be sent to client (no special client side type nor pushToServer) */
    getPropertyDescriptions(): ObjectOfIPropertyDescription {
        return this.propertyDescriptions;
    }

    getHandler(handlerName: string): IWebObjectFunction {
        return this.handlers ? this.handlers[handlerName] : undefined;
    }

    getApiFunction(apiFunctionName: string): IWebObjectFunction {
        return this.apiFunctions ? this.apiFunctions[apiFunctionName] : undefined;
    }

}

export interface ObjectOfITypeWithNumberKeys { [key: number]: IType<any> }

class WebObjectFunction implements IWebObjectFunction {

    constructor(
            readonly returnType?: IType<any>,
            private readonly argumentTypes?: ObjectOfITypeWithNumberKeys,
        ) {}

    getArgumentType(argumentIdx: number): IType<any> {
        return this.argumentTypes ? this.argumentTypes[argumentIdx] : undefined;
    }

}

class WebObjectEventHandler extends WebObjectFunction implements IEventHandler {

    constructor(
            returnType?: IType<any>,
            argumentTypes?: ObjectOfITypeWithNumberKeys,
            readonly ignoreNGBlockDuplicateEvents?: boolean
    ) {
        super(returnType, argumentTypes);
    }

}

export interface IWebObjectTypesFromServer {
    [specName: string]: IWebObjectSpecificationFromServer;
}

/** This type definition must match what the server sends; see org.sablo.specification.ClientSideTypeCache.buildClientSideTypesFor(WebObjectSpecification) javadoc and impl. */
export interface IWebObjectSpecificationFromServer {

         p?: IPropertiesFromServer;
         ftd?: IFactoryTypeDetails; // this will be the custom type details from spec something like { "JSON_obj": ICustomTypesFromServer}}

         /** any handlers */
         h?: IEventHandlersFromServer;

         /** any api functions */
         a?: IWebObjectFunctionsFromServer;

}

export interface IFactoryTypeDetails {
    [factoryTypeName: string]: any; // generic, for any factory type; that any will be ICustomTypesFromServer in case of JSON_obj factory
}

/** Any custom object types defined in the component/service .spec (by name, each containing the sub-properties defined in spec. for it) */
export interface ICustomTypesFromServer {
    [customTypeName: string]: IPropertiesFromServer;
}

/**
 * So any properties that have client side conversions (by name or in case of factory types via a tuple / array of 2: factory name and factory param);
 * these tuples are used only when getting them from server, afterwards when the IProperties obj. is genearated from this, the specific type from that
 * factory is created and the value of the property type is changed from the tuple to the string that represents the created type.
 */
export interface IPropertiesFromServer {
    [propertyName: string]: IPropertyDescriptionFromServer;
}

interface IEventHandlersFromServer {
    [name: string]: IEventHandlerFromServer;
}

export interface IWebObjectFunctionsFromServer {
    [name: string]: IWebObjectFunctionFromServer;
}

interface IEventHandlerFromServer extends IWebObjectFunctionFromServer {
    /** "ignoreNGBlockDuplicateEvents" flag from spec. - if the handler is supposed to ignore the blocking of duplicates - when that is enabled via client or ui properties of component */
    iBDE?: boolean;
}

export interface IWebObjectFunctionFromServer {
    /** any api/handler call arguments with client side conversion types (by arg no.)  */
    [argumentIdx: number]: ITypeFromServer;
    /** return value of api/handler call if it's a converting client side type */
    r?: ITypeFromServer;
}

/**
 * The types registry holds information about all service client side types (for properties, api params/return value) and about all needed
 * component client side types (for properties, apis, handlers).
 * Client side types are those types that require client side conversions to/from server.
 */
export interface ITypesRegistry {

    getTypeFactoryRegistry(): ITypeFactoryRegistry;
    registerGlobalType(typeName: string, theType: IType<any>, onlyIfNotAlreadyRegistered: boolean): void;

    getComponentSpecification(componentSpecName: string): IWebObjectSpecification;
    getServiceSpecification(serviceSpecName: string): IWebObjectSpecification;

}

export interface ITypesRegistryForTypeFactories extends ITypesRegistry {

    /**
     * This method is to be used outside of the type registry only by ITypeFactory instances that need to get IType instances from ITypeFromServer
     * when the ITypeFactory.registerDetails(...) method is called.
     * All other code already has IType instances available (not ITypeFromServer) and does not need this.
     *
     * @param typeFromServer the type as it was received from server.
     * @param webObjectSpecName the name of the component/service that it was received for.
     */
    processTypeFromServer(typeFromServer: ITypeFromServer, webObjectSpecName: string): IType<any>;

    /**
     * Similar to #processTypeFromServer(...) but it processes a property description not just a type; PDs can have both type and pushToServer values for a property.
     *
     * @param propertyDescriptionFromServer what we received from server for a property description
     * @param webObjectSpecName the name of the component/service that it was received for.
     */
    processPropertyDescriptionFromServer(propertyDescriptionFromServer: IPropertyDescriptionFromServer, webObjectSpecName: string): IPropertyDescription;

}

export interface ITypesRegistryForSabloConverters extends ITypesRegistry {

    /**
     * This method returns only simple (non-factory) types that are already registered with the type registry. It should only be used from $sabloConverters.convertFromServerToClient(...) or
     * types that inside their impl. can send variable nested types (for instance 'object' type can have random 'date' values nested in it).
     * All other code already has IType instances available (not ITypeFromServer) via IWebObjectSpecification - and does not need this.
     *
     * @param typeFromServer the type as it was received from server.
     */
    getAlreadyRegisteredType(typeFromServer: ITypeFromServer): IType<any>;

}

/**
 * An IType is a type of data (properties/arguments/return values) that requires client side client-to-server and server-to-client conversions.
 * VT is the client side type of value for that property.
 */
export interface IType<VT> {

    /**
     * Converts the JSON value received from server for this type of property into a client-side value specific for this type and returns that.
     *
     * @param serverJSONValue can be any JSON valid value (primitive, object, ...)
     * @param currentClientValue the current value that this property had (if any) on client before the new value arrived; this is useful sometimes in case of component/service properties.
     *                           In all other cases (args, return values) it's null/undefined.
     * @param propertyContext (useful for properties of components/services) a way for this property to access another property in the current property context (if in the root of the web
     *                        object then other root properties, if in a nested custom object - other properties in the same custom object with fallback to parent level property context).
     *                        It can be null/undefined if conversion happens for service/component API call parameters for example.
     *
     * @return the new or updated client side property value; if this returned value is interested in being able to triggering/sending updates to server when something changes client side then
     *         it's "state" must extend ChangeAwareState.
     */
    fromServerToClient(serverJSONValue: any, currentClientValue: VT, propertyContext: IPropertyContext): VT;

    /**
     * Converts a client side value to a corresponding JSON value that is to be sent to server.
     *
     * @param newClientData the client data to be converted for sending to server. It's not typed VT in case of values that can be completely created clientside (for example arrays/objects and
                            that can be set without yet being the correct instance).
     * @param oldClientData (only for properties, not args/ret vals) in case the value of this property has changed by reference - the old value of this property; it can be null/undefined if
     *                      conversion happens for service API call parameters for example...
     * @param propertyContext (only for properties, not args/ret vals) a way for this property to access another property in the current property context (if in the root of the web
     *                        object then other root properties, if in a nested custom object - other properties in the same custom object with fallback to parent level property context).
     *                        It can be null/undefined if conversion happens for service/component API call parameters for example.
     *
     * @return a tuple with first element being the JSON value to send to the server and the second element being the new value of the property on client (some prop. types like array and
     * custom object can change the new value references on client into Proxy instances of those values - to intercept shallow pushToServer subvalues inside them); so the caller needs
     * to update the property values in it's parent. It can return null if this type cannot send itself to server (in which case also the client side value remains unchanged in parent).
     * Most types will just return [someJSONToBeSentToServer, newClientData].
     */
    fromClientToServer(newClientData: any, oldClientData: VT, propertyContext: IPropertyContext): [any, VT] | null;

}

export type IPropertyContextGetterMethod = (propertyName: string) => any;

/**
 * websocket.ts implements this in it's sablo.propertyTypes.PropertyContext class.
 */
export interface IPropertyContext {
    /**
     * "true" if the conversion that uses this context is for/part of (directly or in a nested fashion) a service or component model.
     * So if a property of a component/service is being converted.
     * 
     * "false" if this context is for component/service api call/handler arguments or return value conversions.
     * 
     * IMPORTANT: if this property is <<false>>, and it's a client-to-server conversion, all smart properties should send themselves fully, as if they were
     * fully changed, but without clearing their existing change flags afterwards (in case the value is also stored in a model and it was marked as having
     * changes that need to be send to server from the model, and being used as an argument to a handler before that emit will happen on the model for example
     * should not reset those flags...)
     * 
     * Some change-aware property types need to keep track of relocations within the model - if they are used as part of the model. But they need
     * to differentiate between situations when they are relocated inside the model or they are just being sent as arguments to handlers for example, remaining
     * in the same location in the model where they were before as well.
     */
    readonly isInsideModel: boolean;

    /**
     * Can be used to get other sibling properties of the property that this context is used for. If the property is in the root of the component/service model,
     * then this getter will provide access to other properties in the root of the model. If the context is that of a custom object, this getter will first
     * return sibling properties from the same custom object, and, if such a sibling is not found, forward to a parent property context (either another custom
     * object or the root model property context).
     */
    getProperty: IPropertyContextGetterMethod;

    getPushToServerCalculatedValue(): PushToServerEnum;
}

export interface IPropertyContextCreator {
    withPushToServerFor(propertyName: string): IPropertyContext;
}

/** The type definition with client side conversion types for a component or service.  */
export interface IWebObjectSpecification {

    getPropertyDescription(propertyName: string): IPropertyDescription;
    getPropertyType(propertyName: string): IType<any>;
    /** This is the value of pushToServer as declared in the spec file... it can be undefined;
     * this value should be calculated from the parent properties using PushToServerUtil#combineWithChild, and root properties that do not
     * have it defined must be considered by default as PushToServerEnum.REJECT or call #getPropertyPushToServer(...) instead. */
    getPropertyDeclaredPushToServer(propertyName: string): PushToServerEnum;
    /**
     * Same as #getPropertyDeclaredPushToServer(...) but if spec file does not declare a push to server value it will default to PushToServerEnum.REJECT
     * instead of returning undefined. Use this for root component/service properties.
     */
    getPropertyPushToServer(propertyName: string): PushToServerEnum;

    /** this can return null if no property descriptions needed to be sent to client (no special client side type nor pushToServer) */
    getPropertyDescriptions(): { [propertyName: string]: IPropertyDescription };
    getHandler(handlerName: string): IEventHandler;
    getApiFunction(apiFunctionName: string): IWebObjectFunction;

}

export interface IPropertyDescription {

    getPropertyType(): IType<any>;

    /** This is the value of pushToServer as declared in the spec file... it can be undefined;
     * this value should be calculated from the parent properties using PushToServerUtil#combineWithChild, and root properties that do not
     * have it defined must be considered by default as PushToServerEnum.REJECT or call #getPropertyPushToServer() instead. */
    getPropertyDeclaredPushToServer(): PushToServerEnum;

    /**
     * Same as #getPropertyDeclaredPushToServer() but if spec file does not declare a push to server value it will default to PushToServerEnum.REJECT
     * instead of returning undefined. Use this for root component/service properties.
     */
    getPropertyPushToServer(): PushToServerEnum;

}

/** The type definition with client side conversion types for a handler.  */
interface IEventHandler extends IWebObjectFunction {

    /** if the handler is supposed to ignore the blocking of duplicates - when that is enabled via client or ui properties of component */
    ignoreNGBlockDuplicateEvents?: boolean;

}

/** The type definition with client side conversion types for a handler or api function.  */
export interface IWebObjectFunction {

    readonly returnType?: IType<any>;
    getArgumentType(argumentIdx: number): IType<any>;

}

/**
 * This is what server sends for a type (either a simple global type or a tuple for factory types, the factory name and arg). This type is only
 * to be used in type registry code or code that processes server-side sent types such as ITypeFactory.registerDetails to get the client-side IType
 * instances from that.
 */
export type ITypeFromServer = string | [string, any];


export type PushToServerEnumServerValue = 0 | 1 | 2 | 3;

/**
 * This type is only to be used in type registry code or code that processes server-side sent types such as ITypeFactory.registerDetails
 * to get the client-side IType instances from that.
 */
export interface IPropertyDescriptionFromServerWithMultipleEntries { t?: ITypeFromServer; s: PushToServerEnumServerValue };

/** Type and pushToServer for a property. This type is only to be used in type registry code or code that processes server-side sent types such as ITypeFactory.registerDetails
 * to get the client-side IType instances from that. */
export type IPropertyDescriptionFromServer = ITypeFromServer | IPropertyDescriptionFromServerWithMultipleEntries;

/**
 * Factory types (custom objects for instance) are registered and used through this registry. For example a custom object type is not just a type,
 * it has a specific declaration for sub-property types based on each individual custom object type from spec. So a factory would create all these specific custom object types.
 */
export interface ITypeFactoryRegistry {

    getTypeFactory(typeFactoryName: string): ITypeFactory<any>;
    contributeTypeFactory(typeFactoryName: string, typeFactory: ITypeFactory<any>): void;

}

/**
 * See ITypeFactoryRegistry description. Some types like custom objects need to create more specific types for actual usage. For example a
 * custom object is different based on how it is defined in it's .spec file.
 *
 * VT is the client side type of value that specific types created by this factory will use.
 */
export interface ITypeFactory<VT> {

    /**
     * Asks the factory to get (if it has already created this specific (sub)type) or create a specific type with the given specificTypeInfo (could be a custom object type name from spec or
     * in case of arrays it could be the type name of elements). Some type factories will have to rely on previously registered details for that specificTypeInfo that can be received from
     * server via registerDetails(...).
     *
     * IMPORTANT: It is the responsibility of this factory to cache any newly created specific types as needed.
     *
     * @param specificTypeInfo the information that can make a specific type from this factory of types (could be forexample a custom object type
                               name from spec or in case of arrays it could be the type name of elements)
     * @param webObjectSpecName as types are/can be scoped inside a web object (component or service) .spec we also give the webObjectSpecName here.
     *
     * @returns the specific type for the given arguments.
     */
    getOrCreateSpecificType(specificTypeInfo: any, webObjectSpecName: string): IType<VT>;

    /**
     * Gives the factory details that are needed for it to be able to create needed specific (sub)types.
     *
     * @param details for example is case of a JSON_obj factory the details would be the types of it's child properties. (ICustomTypesFromServer)
     * @param webObjectSpecName the web object for which this details were sent from the server.
     */
    registerDetails(details: any, webObjectSpecName: string): void;

}
