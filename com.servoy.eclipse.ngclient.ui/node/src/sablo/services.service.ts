import { Injectable } from '@angular/core';

import { ConverterService, instanceOfChangeAwareValue, ChangeListenerFunction } from './converter.service';
import { LoggerService, LoggerFactory, RequestInfoPromise, Deferred } from '@servoy/public';
import { TypesRegistry, IType, IWebObjectSpecification, IPropertyContextCreator, PushToServerUtils, PushToServerEnum } from '../sablo/types_registry';
import { WebsocketService, wrapPromiseToPropagateCustomRequestInfoInternal } from '../sablo/websocket.service';
import { SabloService } from '../sablo/sablo.service';
import { ClientFunctionService } from './clientfunction.service';

@Injectable({
    providedIn: 'root'
})
export class ServicesService {

    private serviceProvider: ServiceProvider = new VoidServiceProvider();
    private log: LoggerService;
    private serviceDynamicClientSideTypes = {}; // it stores property types that are dynamic (can change at runtime)

    constructor(private converterService: ConverterService<unknown>,
        private readonly typesRegistry: TypesRegistry,
        websocketService: WebsocketService,
        private sabloService: SabloService,
        logFactory: LoggerFactory,
        private clientFunctionService: ClientFunctionService) {
        this.log = logFactory.getLogger('ServicesService');

        websocketService.getSession().then((session) => session.setServicesHandler({
            handleServiceApisWithApplyFirst: (serviceApisJSON: any, previousResponseValue: any): any => {
                // services calls; first process the once with the flag 'apply_first'
                let responseValue: any = previousResponseValue;
                for (const serviceCall of serviceApisJSON as Array<ApiCallFromServer>) {
                    if (serviceCall['pre_data_service_call']) {
                        // responseValue keeps last services call return value
                        responseValue = this.callServiceApi(serviceCall); // this handles arg type conversions and return value type conversion as well
                    }
                }
                return responseValue;
            },

            handlerServiceUpdatesFromServer: (servicesUpdatesFromServerJSON: any): void => {
                this.clientFunctionService.waitForLoading().finally(() => {
                    this.updateServiceScopes(servicesUpdatesFromServerJSON);
                });
            },

            handleNormalServiceApis: (serviceApisJSON: any, previousResponseValue: any): any => {
                // normal api calls
                const def = new Deferred();
                this.clientFunctionService.waitForLoading().finally(() => {
                    let responseValue: any = previousResponseValue;
                    for (const serviceCall of serviceApisJSON as Array<ApiCallFromServer>) {
                        if (!serviceCall['pre_data_service_call']) {
                            // responseValue keeps last services call return value
                            responseValue = this.callServiceApi(serviceCall); // this handles arg type conversions and return value type conversion as well
                        }
                    }
                    def.resolve(responseValue);
                });
                return def.promise;
            }
        }));
    }

    public setServiceProvider(serviceProvider: ServiceProvider) {
        if (serviceProvider == null) this.serviceProvider = new VoidServiceProvider();
        else this.serviceProvider = serviceProvider;
    }

    public getServiceProvider(): ServiceProvider {
        return this.serviceProvider;
    }

    public callServiceApi(serviceCall: ApiCallFromServer): any {

        const serviceInstance = this.getServiceProvider().getService(serviceCall.name);

        const serviceSpec = this.typesRegistry.getServiceSpecification(serviceCall.name);
        if (serviceInstance
            && serviceInstance[serviceCall.call]) {
            const serviceCallSpec = serviceSpec?.getApiFunction(serviceCall.call);

            if (serviceCall.args) for (let argNo = 0; argNo < serviceCall.args.length; argNo++) {
                serviceCall.args[argNo] = this.converterService.convertFromServerToClient(serviceCall.args[argNo], serviceCallSpec?.getArgumentType(argNo),
                    undefined, undefined, undefined, PushToServerUtils.PROPERTY_CONTEXT_FOR_INCOMMING_ARGS_AND_RETURN_VALUES);
            }

            // wrap return value in a Promise.resolve to make sure we convert-to-server the return value as well when the api returns a promise
            return Promise.resolve(serviceInstance[serviceCall.call].apply(serviceInstance, serviceCall.args)).then(
                (ret) => this.converterService.convertFromClientToServer(ret, serviceCallSpec?.returnType,
                    undefined, PushToServerUtils.PROPERTY_CONTEXT_FOR_OUTGOING_ARGS_AND_RETURN_VALUES)[0],
                (reason) => {
                    // error
                    this.log.error('sbl * Error (follows below) in in executing service Api call "' + serviceCall.call + '" to service ' + serviceCall.name);
                    this.log.error(reason);
                });
        } else {
            if (serviceInstance) {
                this.log.error('trying to call a service api ' + serviceCall.call + ' for service ' + serviceCall.name + ' but the api function was not found!');
            }
            else {
                this.log.error('trying to call a service api ' + serviceCall.call + ' for service ' + serviceCall.name + ' but the service (' + serviceInstance + ') was not found!');
            }
        }
    }

    public updateServiceScopes(services: any) {
        for (const servicename of Object.keys(services)) {
            // current model
            const service = this.serviceProvider.getService(servicename);
            if (service) {
                const serviceData = services[servicename];

                const serviceSpec: IWebObjectSpecification = this.typesRegistry.getServiceSpecification(servicename); // get static client side types for this service - if it has any
                const propertyContextCreator: IPropertyContextCreator = PushToServerUtils.newRootPropertyContextCreator(
                    (propertyName: string) => service[propertyName],
                    serviceSpec
                );
                try {
                    // convert all properties, remember any dynamic type(s) for when a client-server conversion will be needed
                    const dynamicPropertyTypesForThisService = this.getServiceDynamicClientSideTypes(servicename);
                    for (const propertyName of Object.keys(serviceData)) {
                        const oldValue = service[propertyName];
                        const staticPropertyType = serviceSpec?.getPropertyType(propertyName); // get static client side type if any
                        const oldPropertyType = staticPropertyType ? staticPropertyType : dynamicPropertyTypesForThisService[propertyName];

                        service[propertyName] = this.converterService.convertFromServerToClient(serviceData[propertyName],
                            staticPropertyType /*update from server; old dynamic type does not matter*/, service[propertyName],
                            dynamicPropertyTypesForThisService, propertyName, propertyContextCreator.withPushToServerFor(propertyName));

                        const newPropertyType = staticPropertyType ? staticPropertyType : dynamicPropertyTypesForThisService[propertyName];

                        if (newPropertyType && (service[propertyName] !== oldValue || oldPropertyType !== newPropertyType))
                            this.setChangeListenerIfSmartProperty(service[propertyName], servicename, propertyName);

                        // we currently do not have a mechanism similar to ngOnChange (present on components, based on detectChanges()) for services; so
                        // currently, the services do not need to manually trigger that if old property value and new property value are the same by ref (which detectChanges() would not see);
                        // so currently services that need to detect incomming changes from server for a property define that property as getter+setter in their class
                    }
                } catch (ex) {
                    this.log.error(ex);
                }
            }
        }
    }

    /* Send granular changes - the property itself has not changed by reference */
    public sendServiceChanges(serviceName: string, propertyName: string) {
        const service = this.serviceProvider.getService(serviceName);
        this.sendServiceChangesWithValue(serviceName, propertyName, service[propertyName], service[propertyName]);
    }

    /**
     * Send a changed property's value to server.
     * 
     * @param propertyValue the new value that the service has (and should send to server) for the given propertyName; if you didn't assign it yet to the service's property,
     *                      this method will do it for you.
     * @param oldPropertValue the value that this property used to have (or has if you did not change the reference - in this case it should be the same as „propertyValue”);
     *                        this value is used in case of smart types (custom array/custom objects) in order to detect if it's a full change by reference for example
     */
    public sendServiceChangesWithValue(serviceName: string, propertyName: string, propertyValue: any, oldPropertValue: any) {
        const service = this.serviceProvider.getService(serviceName);

        const changes = {};
        let propertyType: IType<any>;
        const serviceSpec = this.typesRegistry.getServiceSpecification(serviceName);
        propertyType = serviceSpec?.getPropertyType(propertyName); // first check if it has a static type

        if (!propertyType) { // try to see if this prop. had a dynamic type then
            propertyType = this.getServiceDynamicClientSideTypes(serviceName)?.[propertyName];
        }

        const converted = this.converterService.convertFromClientToServer(propertyValue, propertyType, oldPropertValue, {
            getProperty: (propertyN: string) => service[propertyN],
            getPushToServerCalculatedValue: () => serviceSpec ? serviceSpec.getPropertyPushToServer(propertyName) : PushToServerEnum.REJECT,
            isInsideModel: true
        });
        changes[propertyName] = converted[0];
        service[propertyName] = converted[1];

        // set/update change notifier just in case a new full value was set into a smart property type that needs a changeListener for that specific property
        this.setChangeListenerIfSmartProperty(service[propertyName], serviceName, propertyName);

        this.sabloService.sendServiceChangesJSON(serviceName, changes);
    }

    /**
     * If a service defines a server side scripting file in it's .spec ("serverscript" key), the client side of the service can call server-side apis defined
     * on that scope.
     */
    public callServiceServerSideApi<T>(serviceName: string, methodName: string, args: Array<any>): RequestInfoPromise<T> {
        const apiSpec = this.typesRegistry.getServiceSpecification(serviceName)?.getApiFunction(methodName);

        // convert args as needed
        if (args && args.length) for (let i = 0; i < args.length; i++) {
            args[i] = this.converterService.convertFromClientToServer(args[i], apiSpec?.getArgumentType(i), undefined, PushToServerUtils.PROPERTY_CONTEXT_FOR_OUTGOING_ARGS_AND_RETURN_VALUES)[0];
        }

        // convert return value as needed
        const promise = this.sabloService.callService('applicationServerService', 'callServerSideApi', { service: serviceName, methodName, args });

        return wrapPromiseToPropagateCustomRequestInfoInternal(promise, promise.then((serviceCallResult) => this.converterService.convertFromServerToClient(serviceCallResult, apiSpec?.returnType,
            undefined, undefined, undefined, PushToServerUtils.PROPERTY_CONTEXT_FOR_INCOMMING_ARGS_AND_RETURN_VALUES)));
        // in case of a reject/errorCallback we just let it propagate to caller;
    }

    private setChangeListenerIfSmartProperty(propertyValue: any, serviceName: string, propertyName: string): void {
        if (instanceOfChangeAwareValue(propertyValue)) {
            const changeListenerFunction = this.getChangeListener(serviceName, propertyName);
            propertyValue.getInternalState().setChangeListener(changeListenerFunction);
            // we check for changes anyway - in case a property type doesn't do that itself when changeListener is assigned to it (setChangeListener)
            if (propertyValue.getInternalState().hasChanges()) changeListenerFunction();
        }
    }

    private getChangeListener(serviceName: string, propertyName: string): ChangeListenerFunction {
        return (doNotPushNow?: boolean) => {
            if (!doNotPushNow) {
                this.sendServiceChanges(serviceName, propertyName);
            }  // else this was triggered by an custom array or object change with push to server ALLOW - which should not send it automatically but just mark changes in the
            // nested values towards this root prop; so nothing to do here then
        };
    }

    private getServiceDynamicClientSideTypes(serviceName: string) {
        let dynamicTypesForService = this.serviceDynamicClientSideTypes[serviceName];
        if (!dynamicTypesForService)
            this.serviceDynamicClientSideTypes[serviceName] = dynamicTypesForService = {};

        return dynamicTypesForService;
    }

}

interface ApiCallFromServer { name: string; call: string; args: any[] }

export interface ServiceProvider {
    getService(name: string): any;
}

class VoidServiceProvider implements ServiceProvider {
    getService(_name: string) {
        return null;
    }
}
