import { Injectable } from '@angular/core';
import { WebsocketService } from '../sablo/websocket.service';
import { SabloService } from '../sablo/sablo.service';
import { LoggerService, LoggerFactory, Deferred } from '@servoy/public';
import { ConverterService, IParentAccessForSubpropertyChanges, IChangeAwareValue, instanceOfChangeAwareValue, ChangeListenerFunction, isChanged } from '../sablo/converter.service';
import { ServoyService } from './servoy.service';
import { get, set } from 'lodash-es';
import { ComponentCache, FormCache, FormComponentCache, FormComponentProperties, IFormComponent, instanceOfFormComponent, PartCache, StructureCache } from './types';
import { ClientFunctionService } from './services/clientfunction.service';
import { PushToServerEnum, IType, IWebObjectSpecification, TypesRegistry, RootPropertyContextCreator, PushToServerUtils } from '../sablo/types_registry';
import { FoundsetLinkedValue } from './converters/foundsetLinked_converter';
import { DateType } from '../sablo/converters/date_converter';

@Injectable({
  providedIn: 'root'
})
export class FormService {

    private formsCache: Map<string, FormCache>;
    private log: LoggerService;
    private formComponentCache: Map<string, IFormComponent | Deferred<any>>; // this refers to forms (angular components), not to servoy form components
    private ngUtilsFormStyleclasses: {property: string};
    //    private touchedForms:Map<String,boolean>;

    /**
     * Set this to true before updating form/component models with changes/values from server, and set it to false when you are done updating.
     * When this is set to true, Proxy triggers of any SHALLOW/DEEP (property pushToServer) properties will be ignored as they are not client side generated changes.
     */
    private ignoreProxyTriggeredChangesAsServerUpdatesAreBeingApplied = false;
    private isInDesigner = false;

    constructor(private sabloService: SabloService, private converterService: ConverterService, websocketService: WebsocketService, logFactory: LoggerFactory,
        private servoyService: ServoyService, private clientFunctionService: ClientFunctionService, private typesRegistry: TypesRegistry) {

        this.log = logFactory.getLogger('FormService');
        this.formsCache = new Map();
        this.formComponentCache = new Map();

        websocketService.getSession().then((session) => {
            session.onMessageObject((msg) => {
                if (msg.forms) {
                    for (const formname in msg.forms) {
                        // if form is loaded
                        if (this.formComponentCache.has(formname) && !(this.formComponentCache.get(formname) instanceof Deferred)) {
                            this.formMessageHandler(this.formsCache.get(formname), formname, msg, servoyService);
                        } else {
                            if (!this.formComponentCache.has(formname)) {
                                this.formComponentCache.set(formname, new Deferred<any>());
                            }
                            const deferred = this.formComponentCache.get(formname) as Deferred<any>;
                            deferred.promise.then(() =>
                                this.formMessageHandler(this.formsCache.get(formname), formname, msg, servoyService)
                            );
                        }
                    }
                }
                if (msg.call) {
                    // this is a component API call; execute it
                    // {"call":{"form":"product","element":"datatextfield1","api":"requestFocus","args":[arg1, arg2]}, // optionally "viewIndex":1 }
                    const componentCall = msg.call;

                    this.log.spam(this.log.buildMessage(() => ('sbl * Received API call from server: "' + componentCall.api + '" to form ' + componentCall.form
                        + ', component ' + (componentCall.propertyPath ? componentCall.propertyPath : componentCall.bean))));

                    const callItNow = ((doReturnTheRetVal: boolean): any => {
                        const formComponent = this.formComponentCache.get(componentCall.form) as IFormComponent;
                        const def = new Deferred();
                        this.clientFunctionService.waitForLoading().finally(() => {
                            const retValue = formComponent.callApi(componentCall.bean, componentCall.api, componentCall.args, componentCall.propertyPath);
                            formComponent.detectChanges();
                            def.resolve(retValue);
                        });
                        if (doReturnTheRetVal) return def.promise;
                    });

                    // if form is loaded just call the api
                    if (this.formComponentCache.has(componentCall.form) && !(this.formComponentCache.get(componentCall.form) instanceof Deferred)) {
                        return callItNow(true);
                    }
                    if (!componentCall.delayUntilFormLoads) {
                        // form is not loaded yet, api cannot wait; i think this should be an error
                        this.log.error(this.log.buildMessage(() => ('calling api ' + componentCall.api + ' in form ' + componentCall.form + ' on component '
                            + componentCall.bean + '. Form not loaded and api cannot be delayed. Api call was skipped.')));
                        return;
                    }
                    if (!this.formComponentCache.has(componentCall.form)) {
                        this.formComponentCache.set(componentCall.form, new Deferred<any>());
                    }
                    const deferred = this.formComponentCache.get(componentCall.form) as Deferred<any>;
                    deferred.promise.then(callItNow(false));
                }
            });
        });
    }

    public static updateComponentModelPropertiesFromServer(newComponentProperties: any, comp: ComponentCache, componentSpec: IWebObjectSpecification,
                        converterService: ConverterService,
                        smartPropertyChangeListenerGenerator: (propertyName: string, newPropertyValue: any) => ChangeListenerFunction,
                        triggerNgOnChangesWithSameRefDueToNestedPropUpdate: (propertiesChangedButNotByRef: {propertyName: string; newPropertyValue: any}[]) => void) {

        const propertyContextCreator = new RootPropertyContextCreator(
                (propertyName: string) => (comp.model ? comp.model[propertyName] : undefined),
                componentSpec);

        // prepare to remember any dynamically-set-from-server client side types
        const componentDynamicTypesHolder = comp.dynamicClientSideTypes;
        const propertiesChangedButNotByRef: {propertyName: string; newPropertyValue: any}[] = [];

        for (const propertyName of Object.keys(newComponentProperties)) {
            let newPropertyValue = newComponentProperties[propertyName];
            const propertyType = componentSpec?.getPropertyType(propertyName); // get static client side type if any
            const oldValueOfProperty = comp.model[propertyName];

            newPropertyValue = converterService.convertFromServerToClient(newPropertyValue, propertyType, oldValueOfProperty,
                    componentDynamicTypesHolder, propertyName, propertyContextCreator.withPushToServerFor(propertyName));

            if (propertyName === 'cssPosition'){
                comp.layout = newPropertyValue;
            } else{
                comp.model[propertyName] = newPropertyValue;
            }

            if (instanceOfChangeAwareValue(newPropertyValue)) {
                newPropertyValue.getInternalState().setChangeListener(smartPropertyChangeListenerGenerator(propertyName, newPropertyValue));
            }

            if (oldValueOfProperty === newPropertyValue) {
                propertiesChangedButNotByRef.push({propertyName, newPropertyValue});
                // this value didn't realy change but it was changed internally
                // but we want to let the component know that this is still a (nested) change.
            }
        }

        // after all props of this component have been updated, fire ngOnChanges (which will fire svyOnChanges) for properties that are changed but not by reference
        // so for those properties that will not generate ngOnChanges automatically due to detectChanges() on parent
        if (propertiesChangedButNotByRef.length > 0) triggerNgOnChangesWithSameRefDueToNestedPropUpdate(propertiesChangedButNotByRef);
    }

    public static pushEditingStarted(componentModel: { [property: string]: any }, propertyName: string,
                                        sendStartEditFunc: (foundsetLinkedRowId: string, propertyNameToSend: string) => void) {

        // detect if this is a foundset linked dataprovider - in which case we need to provide a rowId for it to server and trim down the last array index which identifies the row on client
        // TODO this is a big hack - see comment in pushApplyDataprovider below
        const foundsetLinkedDPInfo = this.getFoundsetLinkedDPInfo(propertyName, componentModel);
        let foundsetLinkedRowId: string;
        let propertyNameToSend = propertyName;
        if (foundsetLinkedDPInfo) {
            if (foundsetLinkedDPInfo.rowId) foundsetLinkedRowId = foundsetLinkedDPInfo.rowId;
            propertyNameToSend = foundsetLinkedDPInfo.propertyNameForServer;
        }

        sendStartEditFunc(foundsetLinkedRowId, propertyNameToSend);
    }

    public static pushApplyDataprovider(componentModel: { [property: string]: any }, propertyName: string, propertyType: IType<any>,
                                            newValue: any, componentSpec: IWebObjectSpecification, converterService: ConverterService, oldValue: any,
                                            sendApplyDataproviderFunc: (foundsetLinkedRowId: string, propertyNameToSend: string, valueToSend: any) => void,
                                            typesRegistry: TypesRegistry) {
        let valueToSendToServer: any;
        let propertyNameForServer = propertyName;

        let fslRowID = null;
        let converted: [any, any];
        if (propertyType && !componentModel?.findmode) {
            // I think this never happens currently except for simple date dataproviders; because other simple dataproviders don't have a client-side type (either static or dynamic)
            // and foundset linked dataproviders would be an array of values and applyDataprovider would be called with a propertyName like ...pathToFSLinkedDP[15]
            // for which no propertyType will be found directly in spec - due to the index or any dots
            const propertyContext = {
                getProperty: (otherPropertyName: string) => componentModel[otherPropertyName],
                getPushToServerCalculatedValue: () => (componentSpec ? componentSpec.getPropertyPushToServer(propertyName) : PushToServerEnum.REJECT)
            };
            converted = converterService.convertFromClientToServer(newValue, propertyType, oldValue, propertyContext);
            valueToSendToServer = converted[0];
            componentModel[propertyName] = converted[1];
        } else {
            // foundset linked stuff or a simple dataprovider with no dynamic client side type

            if (propertyName.indexOf('.') > 0 || propertyName.indexOf('[') > 0) {
                // TODO this is a big hack - it would be nicer in the future if we move
                // internal states out of the values of the properties and into a separate locations (so that we can have internal states even for primitive dataprovider types)
                // to have DP types register themselves to the apply() and startEdit() and do the apply/startEdit completely through the property itself (send property updates);
                // then we can get rid of all the custom apply code on server as well as all this custom code in applyDataprovider on client
                // Without this . and [ impl, it could go though custom array/custom object/fs linked... property types instead? that are aware of types and push-to-server for the prop. path

                // now detect if this is a foundset linked dataprovider - in which case we need to provide a rowId for it to server
                const foundsetLinkedDPInfo = this.getFoundsetLinkedDPInfo(propertyName, componentModel);
                if (foundsetLinkedDPInfo) {
                    fslRowID = foundsetLinkedDPInfo.rowId;
                    propertyNameForServer = foundsetLinkedDPInfo.propertyNameForServer;
                }
            }

            // the old value could be just null and the type system doesn't know this is a Date type (if it is a 'date' DataproviderType on server)
            // have special support for it by checking the instanceof so we always map on the DateType for javascript Dates;
            // Dataprovider type on server will know to expect date based on DP type
            if (newValue instanceof Date) propertyType = typesRegistry.getAlreadyRegisteredType(DateType.TYPE_NAME_SVY);
            
            // apply default or date conversion if needed as we don't search for/generate client side type, property context etc. for props nested with . and [
            // see TODO above if the lack of type/property context causes problems
            converted = converterService.convertFromClientToServer(newValue, propertyType, undefined, undefined);

            valueToSendToServer = converted[0];
            set(componentModel, propertyName, converted[1]);
        }

        sendApplyDataproviderFunc(fslRowID, propertyNameForServer, valueToSendToServer);
    }

    private static getFoundsetLinkedDPInfo(propertyName: string, componentModel: { [property: string]: any }): { propertyNameForServer: string; rowId?: string } {
        let propertyNameForServerAndRowID: { propertyNameForServer: string; rowId?: string };

        if ((propertyName.indexOf('.') > 0 || propertyName.indexOf('[') > 0) && propertyName.endsWith(']')) {
            // TODO this is a big hack - see comment in applyDataprovider

            // now detect somehow if this is a foundset linked dataprovider - in which case we need to provide a rowId for it to server
            const lastIndexOfOpenBracket = propertyName.lastIndexOf('[');
            const indexInLastArray = parseInt(propertyName.substring(lastIndexOfOpenBracket + 1, propertyName.length - 1), 10);
            const dpPathWithoutArray = propertyName.substring(0, lastIndexOfOpenBracket);
            const foundsetLinkedDPValueCandidate = get(componentModel, dpPathWithoutArray);

            if (foundsetLinkedDPValueCandidate instanceof FoundsetLinkedValue) {
                // get the corresponding rowID from the foundset property that it uses;
                // strip the last index as we now identify the record via rowId and server has no use for it anyway (on server side it's just a foundsetlinked property value,
                // not an array, so property name should not contain that last index)
                propertyNameForServerAndRowID = { propertyNameForServer: dpPathWithoutArray };

                const foundset = foundsetLinkedDPValueCandidate.getInternalState().forFoundset();
                if (foundset) {
                    propertyNameForServerAndRowID.rowId = foundset.viewPort.rows[indexInLastArray]._svyRowId;
                }
            }
        }

        return propertyNameForServerAndRowID;
    }

    public getFormCacheByName(formName: string): FormCache {
        return this.formsCache.get(formName);
    }

    public getFormCache(form: IFormComponent): FormCache {
        if (this.formComponentCache.get(form.name) instanceof Deferred) {
            (this.formComponentCache.get(form.name) as Deferred<any>).resolve(null);
        }
        this.formComponentCache.set(form.name, form);
        return this.formsCache.get(form.name);
    }

    public getFormStyleClasses(name: string){
        if (this.ngUtilsFormStyleclasses){
            return this.ngUtilsFormStyleclasses[name];
        }
        return null;
    }

    public setFormStyleClasses(styleclasses: {property: string}){
		if (this.ngUtilsFormStyleclasses) {
			for (const formname of Object.keys(this.ngUtilsFormStyleclasses)) {
        		if (this.formComponentCache.has(formname) && !(this.formComponentCache.get(formname) instanceof Deferred)){
            		(this.formComponentCache.get(formname)as IFormComponent).updateFormStyleClasses('');
            	}
        	}
		}

        if (styleclasses) {
			for (const formname of Object.keys(styleclasses)) {
                if (this.formComponentCache.has(formname) && !(this.formComponentCache.get(formname) instanceof Deferred)){
                    (this.formComponentCache.get(formname)as IFormComponent).updateFormStyleClasses(styleclasses[formname]);
                }
            }
        }
		this.ngUtilsFormStyleclasses = styleclasses;
    }

    public destroy(formName: string) {
        this.formComponentCache.delete(formName);
    }

    public hasFormCacheEntry(name: string): boolean {
        return this.formsCache.has(name);
    }

    public createFormCache(formName: string, jsonData: any,  url: string) {
        const formCache = new FormCache(formName, jsonData.size, jsonData.responsive, url, this.typesRegistry);
        try {
            this.ignoreProxyTriggeredChangesAsServerUpdatesAreBeingApplied = true;
            this.walkOverChildren(jsonData.children, formCache);
        } finally {
            this.ignoreProxyTriggeredChangesAsServerUpdatesAreBeingApplied = false;
        }

        this.clientFunctionService.waitForLoading().finally(() => {
            this.formsCache.set(formName, formCache);
            const formComponent = this.formComponentCache.get(formName);
            if (instanceOfFormComponent(formComponent)) {
                formComponent.formCacheChanged(formCache);
            } else {
                this.formComponentCache.forEach((value) => {
                    if (instanceOfFormComponent(value)) {
                        value.detectChanges();
                    }
                });
            }
        });

        //        this.touchedForms[formName] = true;
    }

    public destroyFormCache(formName: string) {
        this.formsCache.delete(formName);
        this.formComponentCache.delete(formName);
        //        delete this.touchedForms[formName];
    }

    public getLoadedFormState() {
        const loadedState: { [s: string]: {url: string; attached: boolean} } = {};
        for ( const formName of Object.keys(this.formsCache) ) {
            loadedState[formName] = {url: this.formsCache.get(formName).url, attached: instanceOfFormComponent(this.formComponentCache.get(formName))};
        }
        return loadedState;
    }

    public executeEvent(formName: string, componentName: string, handlerName: string, args: IArguments | Array<any>, async?: boolean): Promise<any> {
        this.log.debug(this.log.buildMessage(() => (formName + ',' + componentName + ', executing: ' + handlerName + ' with values: ' + JSON.stringify(args))));

        const handlerSpec = this.formsCache.get(formName)?.getComponentSpecification(componentName)?.getHandler(handlerName);

        const newargs = this.converterService.getEventArgs(args, handlerName, handlerSpec); // this will do the needed handler arg conversions from client to server
        const cmd = { formname: formName, beanname: componentName, event: handlerName, args: newargs, changes: {} };

        const form = this.formsCache.get(formName);
        if (form) {
            const component = form.getComponent(componentName);
            if (component && component.model) {
                const componentId = formName + '_' + componentName;
                if ((!handlerSpec || !handlerSpec.ignoreNGBlockDuplicateEvents) && this.servoyService.getUIBlockerService().shouldBlockDuplicateEvents(componentId, component.model, handlerName)) {
                    // reject execution
                    this.log.debug('Prevented duplicate  execution of: ' + handlerName + ' on ' + componentName);
                    return Promise.resolve(null);
                }

                // call handler
                const promise = this.sabloService.callService('formService', 'executeEvent', cmd, async !== undefined ? async : false).then(
                        // convert return value from server to client
                        (retVal) => this.converterService.convertFromServerToClient(retVal, handlerSpec?.returnType,
                                        undefined, undefined, undefined, PushToServerUtils.PROPERTY_CONTEXT_FOR_INCOMMING_ARGS_AND_RETURN_VALUES)
                );

                promise.finally(() => {
                    this.servoyService.getUIBlockerService().eventExecuted(componentId, component.model, handlerName);
                });
                return promise;
            }
        }

        // call handler
        return this.sabloService.callService('formService', 'executeEvent', cmd, async !== undefined ? async : false).then(
                // convert return value from server to client
                (retVal) => this.converterService.convertFromServerToClient(retVal, handlerSpec?.returnType,
                                undefined, undefined, undefined, PushToServerUtils.PROPERTY_CONTEXT_FOR_INCOMMING_ARGS_AND_RETURN_VALUES)
        );
    }

    public formWillShow(formname: string, notifyFormVisibility?: boolean, parentForm?: string, beanName?: string, relationname?: string, formIndex?: number): Promise<boolean> {
        this.log.debug(this.log.buildMessage(() => ('svy * Form ' + formname + ' is preparing to show. Notify server needed: ' + notifyFormVisibility)));
        //        if ($rootScope.updatingFormName === formname) {
        //            this.log.debug(this.log.buildMessage(() => ("svy * Form " + formname + " was set in hidden div. Clearing out hidden div.")));
        //            $rootScope.updatingFormUrl = ''; // it's going to be shown; remove it from hidden DOM
        //            $rootScope.updatingFormName = null;
        //        }

        if (!formname) {
            throw new Error('formname is undefined');
        }
        // TODO do we need request initial data?
        //        $sabloApplication.getFormState(formname).then(function (formState) {
        //            // if first show of this form in browser window then request initial data (dataproviders and such);
        //            if (formState.initializing && !formState.initialDataRequested) $servoyInternal.requestInitialData(formname, formState);
        //        });
        if (notifyFormVisibility) {
            return this.sabloService.callService('formService', 'formvisibility', {
                formname, visible: true, parentForm,
                bean: beanName, relation: relationname, formIndex
            }, false);
        }
        // dummy promise
        return Promise.resolve(true);
    }

    public hideForm(formname: string, parentForm?: string, beanName?: string, relationname?: string, formIndex?: number,
        formnameThatWillBeShown?: string, relationnameThatWillBeShown?: string, formIndexThatWillBeShown?: number): Promise<boolean> {
        if (!formname) {
            throw new Error('formname is undefined');
        }
        let formShow = {};
        if (formnameThatWillBeShown) {
            formShow = { formname: formnameThatWillBeShown, relation: relationnameThatWillBeShown, formIndex: formIndexThatWillBeShown };
        }
        const formDetails = { formname, visible: false, parentForm, bean: beanName, relation: relationname, formIndex, show: formShow };
        return this.sabloService.callService('formService', 'formvisibility', formDetails);
    }

    public pushEditingStarted(formName: string, componentName: string, propertyName: string) {
        FormService.pushEditingStarted(this.formsCache.get(formName).getComponent(componentName).model, propertyName,
            (foundsetLinkedRowId: string, propertyNameToSend: string) => {
                const messageForServer = { formname: formName, beanname: componentName, property: propertyNameToSend };
                if (foundsetLinkedRowId) messageForServer['fslRowID'] = foundsetLinkedRowId; // if it was a foundset linked DP we give the row identifier as well

                this.sabloService.callService('formService', 'startEdit', messageForServer, true);
            });
    }

    public goToForm(formname: string) {
        this.sabloService.callService('formService', 'gotoform', { formname });
    }

    public callComponentServerSideApi(formName: string, componentName: string, methodName: string, args: Array<any>): Promise<any> {
        // see if args and/or return value need client side conversions
        const apiSpec = this.formsCache.get(formName)?.getComponentSpecification(componentName)?.getApiFunction(methodName);

        if (args && args.length) for (let i = 0; i < args.length; i++) {
            args[i] = this.converterService.convertFromClientToServer(args[i], apiSpec?.getArgumentType(i), undefined, PushToServerUtils.PROPERTY_CONTEXT_FOR_OUTGOING_ARGS_AND_RETURN_VALUES)[0];
        }

        return this.sabloService.callService('formService', 'callServerSideApi', { formname: formName, beanname: componentName, methodName, args })
                    .then((serviceCallResult) => this.converterService.convertFromServerToClient(serviceCallResult, apiSpec?.returnType,
                                     undefined, undefined, undefined, PushToServerUtils.PROPERTY_CONTEXT_FOR_INCOMMING_ARGS_AND_RETURN_VALUES));
    }

    /**
     * @deprecated use ServicesService.callServiceServerSideApi instead
     */
    public callServiceServerSideApi<T>(serviceName: string, methodName: string, args: Array<any>): Promise<T> {
        const apiSpec = this.typesRegistry.getServiceSpecification(serviceName)?.getApiFunction(methodName);

        // convert args as needed
        if (args && args.length) for (let i = 0; i < args.length; i++) {
            args[i] = this.converterService.convertFromClientToServer(args[i], apiSpec?.getArgumentType(i), undefined, PushToServerUtils.PROPERTY_CONTEXT_FOR_OUTGOING_ARGS_AND_RETURN_VALUES)[0];
        }

        // convert return value as needed
        return this.sabloService.callService('applicationServerService', 'callServerSideApi', { service: serviceName, methodName, args })
                    .then((serviceCallResult) => this.converterService.convertFromServerToClient(serviceCallResult, apiSpec?.returnType,
                                undefined, undefined, undefined, PushToServerUtils.PROPERTY_CONTEXT_FOR_INCOMMING_ARGS_AND_RETURN_VALUES));
                    // in case of a reject/errorCallback we just let it propagete to caller;
    }

    public sendChanges(formname: string, beanname: string, property: string, value: any, oldvalue: any, dataprovider?: boolean) {
        if (dataprovider) {
            this.applyDataprovider(formname, beanname, property, value, oldvalue);
        } else {
            this.dataPush(formname, beanname, property, value, oldvalue);
        }
    }

    public setDesignerMode() {
        this.isInDesigner = true;
    }

    private setChangeListenerIfSmartProperty(propertyValue: any, formName: string, componentName: string, propertyName: string) {
        if (instanceOfChangeAwareValue(propertyValue)) {
            // setChangeListener can be called now after the new client side type was applied (which already happened before this method was called)
            const changeListenerFunction = this.createChangeListenerForSmartValue(formName, componentName, propertyName, propertyValue);

            // TODO should we decouple this scenario? if we are still processing server to client changes when change notifier is called we could trigger
            // the change notifier later/async for sending changes back to server...
            propertyValue.getInternalState().setChangeListener(changeListenerFunction);

            // as setChangeListener on smart property types might end up calling the change notifier right away to announce it already has changes (because for example
            // the convertFromServerToClient on that property type above might have triggered some listener to the component that uses it which then requested
            // another thing from the property type and it then already has changes...)

            // we check for changes anyway in case a property type doesn't do it itself (to notify) in the situation described in the comment above
            if (propertyValue.getInternalState().hasChanges()) changeListenerFunction();
        }
    }

    private dataPush(formName: string, componentName: string, propertyName: string, newValue: any, oldValue: any) {
        if (this.isInDesigner) return; // form designer doesn't send stuff back to server (doesn't even have access to wsSession in SabloService to do that)

        if (!isChanged(newValue, oldValue)) {
            this.log.spam(this.log.buildMessage(() => ('form service * dataPush (sendChanges) denied; no changes detected between new and old value!')));
            return;
        }

        const formState = this.formsCache.get(formName);
        const propertyType = formState.getClientSideType(componentName, propertyName);
        const componentModel = formState.getComponent(componentName).model;
        const componentSpec = formState.getComponentSpecification(componentName);
        const propertyContext = {
            getProperty: (otherPropertyName: string) => componentModel[otherPropertyName],
            getPushToServerCalculatedValue: () => (componentSpec ? componentSpec.getPropertyPushToServer(propertyName) : PushToServerEnum.REJECT)
        };

        const changes = {};
        if (!propertyName) throw new Error('propertyName should not be null here!');

        const converted = this.converterService.convertFromClientToServer(newValue, propertyType, oldValue, propertyContext);
        changes[propertyName] = converted[0];

        // set/update change notifier just in case a new full value was set into a smart property type that needs a changeListener for that specific property
        this.setChangeListenerIfSmartProperty(converted[1], formName, componentName, propertyName);
        componentModel[propertyName] = converted[1];

        if (propertyType || (typeof oldValue === 'object')) {
            this.sabloService.callService('formService', 'dataPush', { formname: formName, beanname: componentName, changes }, true);
        } else {
            // we only send oldValue to server for primives... see typeof oldValue === 'object' check in the if above (as well as in isChanged(...) impl)
            // because in case of plain nested arrays/objects with subproperty/element changes, the old value is not
            // correct, the same object reference being used (we are not on angular1 here with deep watches that create deep copies of JSON for comparison...)

            // if this is a simple property change without any special client side type - then push the old value as well
            const oldValues = {};
            oldValues[propertyName] = this.converterService.convertFromClientToServer(oldValue, undefined, undefined, propertyContext)[0]; // just for any default conversions
            this.sabloService.callService('formService', 'dataPush', { formname: formName, beanname: componentName, changes, oldvalues: oldValues }, true);
        }
    }

    private applyDataprovider(formName: string, componentName: string, propertyName: string, newValue: any, oldValue: any) {
        if (this.isInDesigner) return; // form designer doesn't send stuff back to server (doesn't even have access to wsSession in SabloService to do that)

        const formState = this.formsCache.get(formName);
        let typeOfData  = formState.getClientSideType(componentName, propertyName);

        FormService.pushApplyDataprovider(formState.getComponent(componentName).model, propertyName, typeOfData,
            newValue, formState.getComponentSpecification(componentName), this.converterService, oldValue,
            (foundsetLinkedRowId: string, propertyNameToSend: string, valueToSend: any) => {
                const changes = {};
                changes[propertyNameToSend] = valueToSend;
                const dpChange = { formname: formName, beanname: componentName, property: propertyNameToSend, changes };
                if (foundsetLinkedRowId) {
                    dpChange['fslRowID'] = foundsetLinkedRowId;
                }

                this.sabloService.callService('formService', 'svyPush', dpChange, true);
            }, this.typesRegistry);
    }

    private formMessageHandler(formCache: FormCache, formName: string, msg: any, servoyService: ServoyService) {
        const formComponent = this.formComponentCache.get(formName) as IFormComponent;
        try {
            this.ignoreProxyTriggeredChangesAsServerUpdatesAreBeingApplied = true;

            const newFormData = msg.forms[formName];
            const newFormProperties = newFormData['']; // properties of the form itself

            if (newFormProperties) {
                // properties of the form itself were received
                // currently what server side sends for the form itself doesn't need client side conversions
                for (const p of Object.keys(newFormProperties)) {
                    formComponent[p] = newFormProperties[p];
                }
            }

            for (const componentName of Object.keys(newFormData)) {
                if (componentName === '') {
                    servoyService.setFindMode(formName, !!newFormData['']['findmode']);
                    continue; // skip other form properties; they were already handled/updated above
                }
                let comp: ComponentCache = formCache.getComponent(componentName);
                if (!comp) { // is it a form component?
                    comp = formCache.getFormComponent(componentName);
                }
                if (!comp) {
                    this.log.debug(this.log.buildMessage(() => ('got message for ' + componentName + ' of form ' + formName + ' but that component is not in the cache')));
                    continue;
                }
                // get static client side types for this component - if it has any
                const componentSpec: IWebObjectSpecification = formCache.getComponentSpecification(componentName);

                // apply any client side type conversions and update the properties received from server
                const newComponentProperties = newFormData[componentName];
                FormService.updateComponentModelPropertiesFromServer(newComponentProperties, comp, componentSpec, this.converterService,
                                (propertyName: string, newPropertyValue: any) => this.createChangeListenerForSmartValue(formCache.formname, componentName, propertyName, newPropertyValue),
                                (propertiesChangedButNotByRef: {propertyName: string; newPropertyValue: any}[]) =>
                                    formComponent.triggerNgOnChangeWithSameRefDueToSmartPropUpdate(componentName, propertiesChangedButNotByRef));
            }
        } finally {
            this.ignoreProxyTriggeredChangesAsServerUpdatesAreBeingApplied = false;
        }
        formComponent.detectChanges(); // this will also fire ngOnChanges which will fire svyOnChanges for all root props that changed by ref
    }

    private walkOverChildren(children: any[], formCache: FormCache, parent?: StructureCache | FormComponentCache | PartCache) {
        children.forEach((elem) => {
            if (elem.layout === true) {
                const structure = new StructureCache(elem.tagname, elem.styleclass, elem.attributes, [], elem.attributes ? elem.attributes['svy-id'] : null, elem.cssPositionContainer, elem.position);
                this.walkOverChildren(elem.children, formCache, structure);
                if (parent == null) {
                    parent = new StructureCache(null, null);
                    formCache.mainStructure = parent;
                }
                if (parent instanceof StructureCache || parent instanceof FormComponentCache) {
                    parent.addChild(structure);
                }

                if (parent instanceof PartCache) {
                    // this is a absolute form where a layout container is added to a part, make sure the form knows about this main "component"
                     formCache.add(structure, parent);
                }
                formCache.addLayoutContainer(structure);
            } else if (elem.part === true) {
                const part = new PartCache(elem.classes, elem.layout);
                this.walkOverChildren(elem.children, formCache, part);
                formCache.addPart(part);
            } else {
                // either a simple component or a component that has servoy-form-component properties in it
                // prepare to remember any dynamically-set-from-server client side types
                const componentSpec: IWebObjectSpecification = this.typesRegistry.getComponentSpecification(elem.specName);

                if (elem.formComponent) {
                    // component that also has servoy-form-component properties
                    const classes: Array<string> = elem.model.styleClass ? elem.model.styleClass.trim().split(' ') : new Array();
                    const layout: { [property: string]: string } = {};
                    if (!elem.responsive) {
                        // form component content is anchored layout

                        const continingFormIsResponsive = !formCache.absolute;
                        let minHeight = elem.model.minHeight !== undefined ? elem.model.minHeight : elem.model.height; // height is deprecated in favor of minHeight but they do the same thing;
                        let minWidth = elem.model.minWidth !== undefined ? elem.model.minWidth : elem.model.width; // width is deprecated in favor of minWidth but they do the same thing;;
                        let widthExplicitlySet: boolean;

                        if (!minHeight && elem.model.containedForm) minHeight = elem.model.containedForm.formHeight;
                        if (!minWidth && elem.model.containedForm) {
                            widthExplicitlySet = false;
                            minWidth = elem.model.containedForm.formWidth;
                        } else widthExplicitlySet = true;

                        if (minHeight) {
                            layout['min-height'] = minHeight + 'px';
                            if (!continingFormIsResponsive) layout['height'] = '100%'; // allow anchoring to bottom in anchored form + anchored form component
                        }
                        if (minWidth) {
                            layout['min-width'] = minWidth + 'px'; // if the form that includes this form component is responsive and
                                                                   // this form component is anchored, allow it to grow in width to fill responsive space

                            if (continingFormIsResponsive && widthExplicitlySet) {
                                // if container is in a responsive form, content is anchored and width model property is explicitly set
                                // then we assume that developer wants to really set width of the form component so it can put multiple of them inside
                                // for example a 12grid column; that means they should not simply be div / block elements; we change float as well
                                layout['float'] = 'left';
                            }
                        }
                    }

                    const formComponentProperties: FormComponentProperties = new FormComponentProperties(classes, layout, elem.model.servoyAttributes);
                    const fcc = new FormComponentCache(elem.name, elem.specName, elem.elType, elem.handlers, elem.responsive, elem.position, formComponentProperties,
                                        !!elem.model.foundset, this.typesRegistry, this.createParentAccessForSubpropertyChanges(formCache.formname, elem.name));

                    this.handleComponentModelConversionsAndChangeListeners(elem, fcc, componentSpec, formCache);

                    elem.formComponent.forEach((child: string) => {
                        this.walkOverChildren(elem[child], formCache, fcc);
                    });
                    formCache.addFormComponent(fcc);
                    if (parent != null) {
                        parent.addChild(fcc);
                    }
                } else {
                    // simple component
                    const comp = new ComponentCache(elem.name, elem.specName, elem.elType, elem.handlers, elem.position, this.typesRegistry,
                                                        this.createParentAccessForSubpropertyChanges(formCache.formname, elem.name));
                    this.handleComponentModelConversionsAndChangeListeners(elem, comp, componentSpec, formCache);

                    formCache.add(comp, parent);
                }
            }
        });
    }

    private createParentAccessForSubpropertyChanges(formName: string, componentName: string): IParentAccessForSubpropertyChanges<number | string> {
        // this is needed to handle subproperty change-by-reference for pushToServer SHALLOW or DEEP subproperties
        return  {
            shouldIgnoreChangesBecauseFromOrToServerIsInProgress: () => this.ignoreProxyTriggeredChangesAsServerUpdatesAreBeingApplied,

            changeNeedsToBePushedToServer: (key: number | string, oldValue: any, doNotPushNow?: boolean) => {
                // this will only be used currently by the smart properties to push changes, not
                // for root property change-by reference, because a component doesn't have direct
                // access to it's model, just to individual properties via @Input and @Output, and when
                // it does change an @Input by reference it has to call .emit(...) on the corresponding output
                // anyway in order to update the property in the model - so we can't use a Proxy () on the model to
                // automatically detect and send these changes; we rely in emits for that that updates the model and calls that calls FormComponent.datachange() anyway
                
                if (! doNotPushNow) {
                    const formState = this.formsCache.get(formName);
                    const componentModel = formState?.getComponent(componentName)?.model;

                    this.dataPush(formName, componentName, key as string, componentModel?.[key], oldValue);
                } // else this was triggered by an custom array or object change with push to server ALLOW - which should not send it automatically but just mark changes in the
                  // nested values towards this root prop; so nothing to do here then
            }
        };
    }

    private handleComponentModelConversionsAndChangeListeners(jsonFromServerForComponent: any, componentCache: ComponentCache,
                componentSpec: IWebObjectSpecification, formCache: FormCache) {
        // handle model conversions of this component (that has some servoy-form-component properties as well)
        const componentDynamicTypesHolder = componentCache.dynamicClientSideTypes;
        const propertyContextCreator = new RootPropertyContextCreator((propertyName: string) => componentCache.model?.[propertyName], componentSpec);

        for (const propName of Object.keys(jsonFromServerForComponent.model)) {
            const propValue = componentCache.model[propName] = this.converterService.convertFromServerToClient(jsonFromServerForComponent.model[propName],
                                    componentSpec?.getPropertyType(propName), null,
                                    componentDynamicTypesHolder, propName, propertyContextCreator.withPushToServerFor(propName));

            if (instanceOfChangeAwareValue(propValue)) {
                propValue.getInternalState().setChangeListener(
                    this.createChangeListenerForSmartValue(formCache.formname, jsonFromServerForComponent.name, propName, componentCache.model[propName]));
            }
        }
    }

    private createChangeListenerForSmartValue(formName: string, componentName: string, propertyName: string, value: IChangeAwareValue): ChangeListenerFunction {
        return (doNotPushNow?: boolean) => {
            if (!doNotPushNow) {
                this.dataPush(formName, componentName, propertyName, value, value);
            }  // else this was triggered by an custom array or object change with push to server ALLOW - which should not send it automatically but just mark changes in the
               // nested values towards this root prop; so nothing to do here then
        };
    }

}
