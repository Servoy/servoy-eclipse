import { Injectable } from '@angular/core';

import { WebsocketService } from '../sablo/websocket.service';
import { SabloService } from '../sablo/sablo.service';
import { Deferred, IChangeAwareValue, IComponentCache, instanceOfChangeAwareValue } from '@servoy/public';
import { ConverterService } from '../sablo/converter.service';
import { LoggerService, LoggerFactory } from '@servoy/public';
import { ServoyService } from './servoy.service';
import { get } from 'lodash-es';
import { ComponentCache, FormCache, FormComponentCache, FormComponentProperties, IFormComponent, instanceOfFormComponent, PartCache, StructureCache } from './types';
import { ClientFunctionService } from './services/clientfunction.service';

@Injectable({
  providedIn: 'root'
})
export class FormService {

    private formsCache: Map<string, FormCache>;
    private log: LoggerService;
    private formComponentCache: Map<string, IFormComponent | Deferred<any>>;
    private ngUtilsFormStyleclasses: {property: string};
    //    private touchedForms:Map<String,boolean>;

    constructor(private sabloService: SabloService, private converterService: ConverterService, websocketService: WebsocketService, logFactory: LoggerFactory,
        servoyService: ServoyService, private clientFunctionService: ClientFunctionService) {
        this.log = logFactory.getLogger('FormService');
        this.formsCache = new Map();
        this.formComponentCache = new Map();

        websocketService.getSession().then((session) => {
            session.onMessageObject((msg, conversionInfo) => {
                if (msg.forms) {
                    for (const formname in msg.forms) {
                        // if form is loaded
                        if (this.formComponentCache.has(formname) && !(this.formComponentCache.get(formname) instanceof Deferred)) {
                            this.formMessageHandler(this.formsCache.get(formname), formname, msg, conversionInfo, servoyService);
                        } else {
                            if (!this.formComponentCache.has(formname)) {
                                this.formComponentCache.set(formname, new Deferred<any>());
                            }
                            const deferred = this.formComponentCache.get(formname) as Deferred<any>;
                            deferred.promise.then(() =>
                                this.formMessageHandler(this.formsCache.get(formname), formname, msg, conversionInfo, servoyService)
                            );
                        }
                    }
                }
                if (msg.call) {
                    // if form is loaded just call the api
                    if (this.formComponentCache.has(msg.call.form) && !(this.formComponentCache.get(msg.call.form) instanceof Deferred)) {
                        const formComponent = this.formComponentCache.get(msg.call.form) as IFormComponent;
                            const def = new Deferred();
                            this.clientFunctionService.waitForLoading().finally(() => {
                                const retValue = formComponent.callApi(msg.call.bean, msg.call.api, msg.call.args, msg.call.propertyPath);
                                formComponent.detectChanges();
                                def.resolve(retValue);
                            });
                            return def.promise;
                    }
                    if (!msg.call.delayUntilFormLoads) {
                        // form is not loaded yet, api cannot wait; i think this should be an error
                        this.log.error(this.log.buildMessage(() => ('calling api ' + msg.call.api + ' in form ' + msg.call.form + ' on component '
                            + msg.call.bean + '. Form not loaded and api cannot be delayed. Api call was skipped.')));
                        return;
                    }
                    if (!this.formComponentCache.has(msg.call.form)) {
                        this.formComponentCache.set(msg.call.form, new Deferred<any>());
                    }
                    const deferred = this.formComponentCache.get(msg.call.form) as Deferred<any>;
                    deferred.promise.then(() => {
                        const formComponent = this.formComponentCache.get(msg.call.form) as IFormComponent;
                        formComponent.callApi(msg.call.bean, msg.call.api, msg.call.args);
                        formComponent.detectChanges();
                    });
                }
            });
        });
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
        this.ngUtilsFormStyleclasses = styleclasses;
        if (styleclasses) {
            for (const formname of Object.keys(styleclasses)) {
                if (this.formComponentCache.has(formname) && !(this.formComponentCache.get(formname) instanceof Deferred)){
                    (this.formComponentCache.get(formname)as IFormComponent).updateFormStyleClasses(styleclasses[formname]);
                }
            }
        }

    }

    public destroy(formName: string) {
        this.formComponentCache.delete(formName);
    }

    public hasFormCacheEntry(name: string): boolean {
        return this.formsCache.has(name);
    }

    public createFormCache(formName: string, jsonData,  url: string) {
        const formCache = new FormCache(formName, jsonData.size, jsonData.responsive, url);
        this.walkOverChildren(jsonData.children, formCache);
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

    public executeEvent(formname: string, beanname: string, handler: string, args: IArguments | Array<any>, async?: boolean) {
        this.log.debug(this.log.buildMessage(() => (formname + ',' + beanname + ', executing: ' + handler + ' with values: ' + JSON.stringify(args))));

        const newargs = this.converterService.getEventArgs(args, handler);
        const data = {};
        //        if (property) {
        //            data[property] = formState.model[beanName][property];
        //        }
        const cmd = { formname, beanname, event: handler, args: newargs, changes: data };
        //        if (rowId)
        //            cmd['rowId'] = rowId;
        return this.sabloService.callService('formService', 'executeEvent', cmd, async !== undefined ? async : false);
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
        const formDetails = { formname, visible: false, parentForm, bean: beanName, relation: relationname, formIndex, show: formShow }
        return this.sabloService.callService('formService', 'formvisibility', formDetails);
    }

    public pushEditingStarted(formname: string, beanname: string, propertyname: string) {
        const messageForServer = { formname, beanname, property: propertyname };

        // // detect if this is a foundset linked dataprovider - in which case we need to provide a rowId for it to server and trim down the last array index which identifies the row on client
        // // TODO this is a big hack - see comment in pushDPChange below
        const formState = this.formsCache.get(formname);
        const foundsetLinkedDPInfo = this.getFoundsetLinkedDPInfo(propertyname, formState.getComponent(beanname));
        if (foundsetLinkedDPInfo) {
            if (foundsetLinkedDPInfo.rowId) messageForServer['fslRowID'] = foundsetLinkedDPInfo.rowId;
            messageForServer.property = foundsetLinkedDPInfo.propertyNameForServer;
        }

        this.sabloService.callService('formService', 'startEdit', messageForServer, true);
    }

    public goToForm(formname: string) {
        this.sabloService.callService('formService', 'gotoform', { formname });
    }

    public callComponentServerSideApi(formname: string, beanname: string, methodName: string, args: Array<any>) {
        return this.sabloService.callService('formService', 'callServerSideApi', { formname, beanname, methodName, args });
    }

    public callServiceServerSideApi<T>(servicename: string, methodName: string, args: Array<any>): Promise<T> {
        return this.sabloService.callService('applicationServerService', 'callServerSideApi', { service: servicename, methodName, args });
    }

    public sendChanges(formname: string, beanname: string, property: string, value: any, oldvalue: any, dataprovider?: boolean) {
        if (dataprovider) {
            this.applyDataprovider(formname, beanname, property, value, oldvalue);
        } else {
            this.dataPush(formname, beanname, property, value, oldvalue);
        }
    }

    private dataPush(formname: string, beanname: string, property: string, value: any, oldvalue: any) {
        const formState = this.formsCache.get(formname);
        const conversionInfo = formState.getConversionInfo(beanname);
        const changes = {};

        if (conversionInfo && conversionInfo[property]) {
            changes[property] = this.converterService.convertFromClientToServer(value, conversionInfo[property], oldvalue);
            this.sabloService.callService('formService', 'dataPush', { formname, beanname, changes }, true);
        } else {
            changes[property] = this.converterService.convertClientObject(value);
            const oldvalues = {};
            oldvalues[property] = this.converterService.convertClientObject(oldvalue);
            this.sabloService.callService('formService', 'dataPush', { formname, beanname, changes, oldvalues }, true);
        }
    }

    private applyDataprovider(formname: string, beanname: string, property: string, value: any, oldvalue: any) {
        const formState = this.formsCache.get(formname);
        const changes = {};

        const conversionInfo = formState.getConversionInfo(beanname);
        let fslRowID = null;
        if (conversionInfo && conversionInfo[property] && !formState.getComponent(beanname)?.model?.findmode) {
            changes[property] = this.converterService.convertFromClientToServer(value, conversionInfo[property], oldvalue);
        } else {
            // foundset linked stuff.
            let dpValue = null;

            if (property.indexOf('.') > 0 || property.indexOf('[') > 0) {
                // TODO this is a big hack - it would be nicer in the future if we have type info for all properties on the client and move
                // internal states out of the values of the properties and into a separate locations (so that we can have internal states even for primitive dataprovider types)
                // to have DP types register themselves to the apply() and startEdit() and do the apply/startEdit completely through the property itself (send property updates);
                // then we can get rid of all the custom apply code on server as well as all this pushDPChange on client

                // nested property; get the value correctly
                dpValue = get(formState.getComponent(beanname).model, property);

                // now detect if this is a foundset linked dataprovider - in which case we need to provide a rowId for it to server
                const foundsetLinkedDPInfo = this.getFoundsetLinkedDPInfo(property, formState.getComponent(beanname).model);
                if (foundsetLinkedDPInfo) {
                    fslRowID = foundsetLinkedDPInfo.rowId;
                    property = foundsetLinkedDPInfo.propertyNameForServer;
                }
            } else {
                dpValue = formState.getComponent(beanname).model[property];
            }

            changes[property] = this.converterService.convertClientObject(value);
        }

        const dpChange = { formname, beanname, property, changes };
        if (fslRowID) {
            dpChange['fslRowID'] = fslRowID;
        }

        this.sabloService.callService('formService', 'svyPush', dpChange, true);
    }

    private formMessageHandler(formCache: FormCache, formname: string, msg: any, conversionInfo: any, servoyService: ServoyService) {
        const formComponent = this.formComponentCache.get(formname) as IFormComponent;
        const formConversion = conversionInfo && conversionInfo.forms ? conversionInfo.forms[formname] : null;
        const formData = msg.forms[formname];
        let newFormProperties = formData['']; // form properties
        if (newFormProperties) {
            if (formConversion && formConversion['']) {
                newFormProperties = this.converterService.convertFromServerToClient(newFormProperties, formConversion[''], formComponent,
                  (propertyName: string) => formComponent ? formComponent[propertyName] : formComponent);
            }
            for (const p in newFormProperties) {
                formComponent[p] = newFormProperties[p];
            }
        }
        for (const beanname of Object.keys(formData)) {
             if (beanname === '') {
                servoyService.setFindMode(formname, formData[beanname]['findmode']);
                continue;
            }
            let comp: IComponentCache = formCache.getComponent(beanname);
            if (!comp) { // is it a form component?
                comp = formCache.getFormComponent(beanname);
            }
            if (!comp) {
                this.log.debug(this.log.buildMessage(() => ('got message for ' + beanname + ' of form ' + formname + ' but that component is not in the cache')));
                continue;
            }
            const beanConversion = formConversion ? formConversion[beanname] : null;
            for (const property of Object.keys(formData[beanname])) {
                let value = formData[beanname][property];
                if (beanConversion && beanConversion[property]) {
                    value = this.converterService.convertFromServerToClient(value, beanConversion[property], comp.model[property],
                        (prop: string) => comp.model ? comp.model[prop] : comp.model);
                    if (instanceOfChangeAwareValue(value)) {
                        value.getStateHolder().setChangeListener(this.createChangeListener(formCache.formname, beanname, property, value));
                    }
                    if (comp.model[property] === value) {
                        // this value didn't realy change but it was changed internally
                        // but we want to let the component know that this is still a (nested) change.
                        formComponent.propertyChanged(beanname, property, value);
                    }
                }
                if (property === 'cssPosition'){
                    comp.layout = value;
                } else{
                    comp.model[property] = value;
                }
            }
        }
        this.clientFunctionService.waitForLoading().finally(() => {
            formComponent.detectChanges();
        });
    }

    private walkOverChildren(children, formCache: FormCache, parent?: StructureCache | FormComponentCache | PartCache) {
        children.forEach((elem) => {
            if (elem.layout === true) {
                const structure = new StructureCache(elem.tagname ,elem.styleclass, elem.attributes, [], elem.attributes ? elem.attributes['svy-id'] : null, elem.cssPositionContainer);
                this.walkOverChildren(elem.children, formCache, structure);
                if (parent == null) {
                    parent = new StructureCache(null, null);
                    formCache.mainStructure = parent;
                }
                if (parent instanceof StructureCache || parent instanceof FormComponentCache) {
                    parent.addChild(structure);
                }
               formCache.addLayoutContainer(structure);
            } else
                if (elem.formComponent) {
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
                            if (!continingFormIsResponsive) layout['height'] = "100%"; // allow anchoring to bottom in anchored form + anchored form component
                        }
                        if (minWidth) {
                            layout['min-width'] = minWidth + 'px'; // if the form that includes this form component is responsive and this form component is anchored, allow it to grow in width to fill responsive space

                            if (continingFormIsResponsive && widthExplicitlySet) {
                                // if container is in a responsive form, content is anchored and width model property is explicitly set
                                // then we assume that developer wants to really set width of the form component so it can put multiple of them inside
                                // for example a 12grid column; that means they should not simply be div / block elements; we change float as well
                                layout['float'] = 'left';
                            } 
                        }
                    }
                    if (elem.model[ConverterService.TYPES_KEY] != null) {
                        this.converterService.convertFromServerToClient(elem.model, elem.model[ConverterService.TYPES_KEY], null, (property: string) => elem.model ? elem.model[property] : elem.model);
                        for (const prop of Object.keys(elem.model)) {
                            const value = elem.model[prop];
                            if (instanceOfChangeAwareValue(value)) {
                                value.getStateHolder().setChangeListener(this.createChangeListener(formCache.formname, elem.name, prop, value));
                            }
                        }
                        formCache.addConversionInfo(elem.name, elem.model[ConverterService.TYPES_KEY]);
                    }

                    const formComponentProperties: FormComponentProperties = new FormComponentProperties(classes, layout, elem.model.servoyAttributes);
                    const fcc = new FormComponentCache(elem.name, elem.model, elem.handlers, elem.responsive, elem.position, formComponentProperties, elem.model.foundset);
                    elem.formComponent.forEach((child: string) => {
                        this.walkOverChildren(elem[child], formCache, fcc);
                    });
                    formCache.addFormComponent(fcc);
                    if (parent != null) {
                        parent.addChild(fcc);
                    }
                } else
                    if (elem.part === true) {
                        const part = new PartCache(elem.classes, elem.layout);
                        this.walkOverChildren(elem.children, formCache, part);
                        formCache.addPart(part);
                    } else {
                        if (elem.model[ConverterService.TYPES_KEY] != null) {
                            this.converterService.convertFromServerToClient(elem.model, elem.model[ConverterService.TYPES_KEY], null,
                                (property: string) => elem.model ? elem.model[property] : elem.model);
                            // eslint-disable-next-line guard-for-in
                            for (const prop in elem.model) {
                                const value = elem.model[prop];
                                if (instanceOfChangeAwareValue(value)) {
                                    value.getStateHolder().setChangeListener(this.createChangeListener(formCache.formname, elem.name, prop, value));
                                }
                            }
                            formCache.addConversionInfo(elem.name, elem.model[ConverterService.TYPES_KEY]);
                        }
                        const comp = new ComponentCache(elem.name, elem.type, elem.model, elem.handlers, elem.position);
                        formCache.add(comp, parent);
                    }
        });
    }

    private createChangeListener(formname: string, beanname: string, property: string, value: IChangeAwareValue) {
        return () => {
            this.sendChanges(formname, beanname, property, value, value);
        };
    }

    private getFoundsetLinkedDPInfo(propertyName: string, beanModel): { propertyNameForServer: string; rowId?: string } {
        let propertyNameForServerAndRowID: { propertyNameForServer: string; rowId?: string };

        if ((propertyName.indexOf('.') > 0 || propertyName.indexOf('[') > 0) && propertyName.endsWith(']')) {
            // TODO this is a big hack - see comment in pushDPChange below

            // now detect somehow if this is a foundset linked dataprovider - in which case we need to provide a rowId for it to server
            const lastIndexOfOpenBracket = propertyName.lastIndexOf('[');
            const indexInLastArray = parseInt(propertyName.substring(lastIndexOfOpenBracket + 1, propertyName.length - 1), 10);
            const dpPathWithoutArray = propertyName.substring(0, lastIndexOfOpenBracket);
            const foundsetLinkedDPValueCandidate = get(beanModel, dpPathWithoutArray);
            if (foundsetLinkedDPValueCandidate && foundsetLinkedDPValueCandidate.state
                && foundsetLinkedDPValueCandidate.state.forFoundset) {

                // it's very likely a foundsetLinked DP: it has the internals that that property type uses; get the corresponding rowID from the foundset property that it uses
                // strip the last index as we now identify the record via rowId and server has no use for it anyway (on server side it's just a foundsetlinked property value,
                // not an array, so property name should not contain that last index)
                propertyNameForServerAndRowID = { propertyNameForServer: dpPathWithoutArray };

                const foundset = foundsetLinkedDPValueCandidate.state.forFoundset();
                if (foundset) {
                    propertyNameForServerAndRowID.rowId = foundset.viewPort.rows[indexInLastArray]._svyRowId;
                }
            }
        }

        return propertyNameForServerAndRowID;
    }

}

