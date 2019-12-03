import { Injectable, TemplateRef } from '@angular/core';
import { Observable } from 'rxjs';

import { WebsocketService } from '../sablo/websocket.service';
import { SabloService } from '../sablo/sablo.service';
import { Deferred } from '../sablo/util/deferred';
import { FormComponent } from './form/form_component.component'

import { ConverterService } from '../sablo/converter.service'
import { LoggerService, LoggerFactory} from '../sablo/logger.service'

import { ServoyService, FormSettings } from './servoy.service'

@Injectable()
export class FormService {

    private formsCache: Map<String, FormCache>;
    private log: LoggerService;
    private formComponentCache: Map<String, any>;
//    private touchedForms:Map<String,boolean>;

    constructor( private sabloService: SabloService, private converterService: ConverterService, websocketService: WebsocketService, private logFactory: LoggerFactory,
        private servoyService: ServoyService ) {
        this.log = logFactory.getLogger("FormService");
        this.formsCache = new Map();
        this.formComponentCache = new Map();
        
        websocketService.getSession().then(( session ) => {
            session.onMessageObject(( msg, conversionInfo ) => {
                if ( msg.forms ) {
                    for ( var formname in msg.forms ) {
                        // if form is loaded
                        if (this.formComponentCache.has(formname) && ! (this.formComponentCache.get(formname) instanceof Deferred))
                        {   
                            this.formMessageHandler(this.formsCache.get( formname ), formname, msg, conversionInfo, servoyService);
                        }
                        else {
                            if (!this.formComponentCache.has(formname))
                            {
                                this.formComponentCache.set(formname,new Deferred<any>());
                            }
                            this.formComponentCache.get(formname).promise.then(() =>
                                this.formMessageHandler(this.formsCache.get( formname ), formname, msg, conversionInfo, servoyService)
                            );
                        }
                    }
                }
                if ( msg.call ) {
                    // if form is loaded just call the api
                    if (this.formComponentCache.has( msg.call.form) && ! (this.formComponentCache.get(msg.call.form) instanceof Deferred))
                    {    
                        return this.formComponentCache.get(msg.call.form).callApi(msg.call.bean,msg.call.api,msg.call.args);
                    }
                    if (!msg.call.delayUntilFormLoads)
                    {
                        // form is not loaded yet, api cannot wait; i think this should be an error
                        this.log.error(this.log.buildMessage(() => ("calling api " + msg.call.api + " in form "+ msg.call.form + " on component " +  msg.call.bean + ". Form not loaded and api cannot be delayed. Api call was skipped.")));
                        return;
                    }    
                    if (!this.formComponentCache.has( msg.call.form))
                    {
                        this.formComponentCache.set( msg.call.form,new Deferred<any>());
                    }
                    this.formComponentCache.get(msg.call.form).promise.then(function(){
                        this.formComponentCache.get(msg.call.form).callApi(msg.call.bean,msg.call.api,msg.call.args);
                    });
                }
            } );
        } );
    }
    
    private formMessageHandler(formCache: FormCache, formname: string, msg: any, conversionInfo: any, servoyService: ServoyService) {
        const formConversion = conversionInfo && conversionInfo.forms ? conversionInfo.forms[formname] : null;
        var formData = msg.forms[formname];
        for ( var beanname in formData ) {
            const comp = formCache.getComponent( beanname );
            if (!comp) {
                this.log.debug(this.log.buildMessage(() => ("got message for " + beanname + " of form " + formname + " but that component is not in the cache")));
                continue;
            }
            const beanConversion = formConversion ? formConversion[beanname] : null;
            for ( var property in formData[beanname] ) {
                let value = formData[beanname][property];
                if ( beanConversion && beanConversion[property] ) {
                    value = this.converterService.convertFromServerToClient( value, beanConversion[property], comp.model[property], (property: string) => { return comp.model ? comp.model[property] : comp.model });
                }
                comp.model[property] = value;
            }
            if(beanname === "") {
                servoyService.setFindMode(formname, formData[beanname]["findmode"]);
            }
        }
    }

    public getFormCacheByName( formName: string ): FormCache {
        return  this.formsCache.get( formName );
    }

    public getFormCache( form: FormComponent ): FormCache {
        if (this.formComponentCache.get(form.name) instanceof Deferred)
        {
            this.formComponentCache.get(form.name).resolve();
        } 
        this.formComponentCache.set(form.name,form);
        return  this.formsCache.get( form.name );
    }
    
    public destroy( form: FormComponent ) {
        this.formComponentCache.delete(form.name) ;
    }
    
    public hasFormCacheEntry(name:string):boolean {
        return this.formsCache.has(name);
    }

    public createFormCache( formName: string, jsonData ) {
        let formCache = new FormCache();
        this.walkOverChildren(jsonData.children, formCache);
        this.formsCache.set( formName, formCache )
//        this.touchedForms[formName] = true;
    }
    
    public destroyFormCache(formName:string){
        this.formsCache.delete(formName);
        this.formComponentCache.delete(formName);
//        delete this.touchedForms[formName];
    }

    private walkOverChildren( children, formCache: FormCache, parent?: StructureCache ) {
        children.forEach(( elem ) => {
            if ( elem.layout == true ) {
                const structure = new StructureCache( elem.styleclass );
                this.walkOverChildren(elem.children, formCache, structure);
                if (parent == null) {
                    formCache.mainStructure = structure;
                }
                else {
                    parent.addChild(structure);
                }
            }
            else {
                if ( elem.model[ConverterService.TYPES_KEY] != null ) {
                    this.converterService.convertFromServerToClient( elem.model, elem.model[ConverterService.TYPES_KEY], null, (property: string) => { return elem.model ? elem.model[property] : elem.model } );
                    formCache.addConversionInfo( elem.name, elem.model[ConverterService.TYPES_KEY] );
                }
                const comp = new ComponentCache( elem.name, elem.type, elem.model, elem.handlers, elem.position );
                if ( parent != null ) {
                    parent.addChild( comp )
                }
                else {
                    formCache.add( comp );
                }
            }
        } );
    }

    public sendChanges( formname: string, beanname: string, property: string, value: object, oldvalue: object ) {
        const formState = this.formsCache.get( formname );
        const changes = {};

        var conversionInfo = formState.getConversionInfo( beanname );
        var fslRowID = null;
        if ( conversionInfo && conversionInfo[property] ) {
            // I think this never happens currently
            changes[property] = this.converterService.convertFromClientToServer( value, conversionInfo[property], oldvalue );
        } else {
            // TODO foundset linked stuff.
            //          var dpValue = null;
            //
            //          if (property.indexOf('.') > 0 || property.indexOf('[') > 0) {
            //              // TODO this is a big hack - it would be nicer in the future if we have type info for all properties on the client and move
            //              // internal states out of the values of the properties and into a separate locations (so that we can have internal states even for primitive dataprovider types)
            //              // to have DP types register themselves to the apply() and startEdit() and do the apply/startEdit completely through the property itself (send property updates);
            //              // then we can get rid of all the custom apply code on server as well as all this pushDPChange on client
            //              
            //              // nested property; get the value correctly
            //              dpValue = eval('formState.model[beanname].' + property)
            //              
            //              // now detect if this is a foundset linked dataprovider - in which case we need to provide a rowId for it to server
            //              var foundsetLinkedDPInfo = getFoundsetLinkedDPInfo(property, formState.model[beanname]);
            //              if (foundsetLinkedDPInfo)   {
            //                  fslRowID = foundsetLinkedDPInfo.rowId;
            //                  property = foundsetLinkedDPInfo.propertyNameForServer;
            //              }   
            //          } else {
            //              dpValue = formState.model[beanname][property];
            //          }

            changes[property] = this.converterService.convertClientObject( value );
        }

        var dpChange = { formname: formname, beanname: beanname, property: property, changes: changes };
        if ( fslRowID ) {
            dpChange['fslRowID'] = fslRowID;
        }

        this.sabloService.callService( 'formService', 'svyPush', dpChange, true );
    }

    public executeEvent( formname: string, beanname: string, handler: string, args: IArguments|Array<any> ) {
        this.log.debug(this.log.buildMessage(() => (formname + "," + beanname + ", executing: " + handler + " with values: " + JSON.stringify( args ) )));

        var newargs = this.converterService.getEventArgs( args, handler );
        var data = {};
        //        if (property) {
        //            data[property] = formState.model[beanName][property];
        //        }
        var cmd = { formname: formname, beanname: beanname, event: handler, args: newargs, changes: data };
        //        if (rowId)
        //            cmd['rowId'] = rowId;
        return this.sabloService.callService( 'formService', 'executeEvent', cmd, false );
    }

    public formWillShow(formname,notifyFormVisibility?,parentForm?,beanName?,relationname?,formIndex?):Promise<boolean> {
        this.log.debug(this.log.buildMessage(() => ("svy * Form " + formname + " is preparing to show. Notify server needed: " + notifyFormVisibility)));
//        if ($rootScope.updatingFormName === formname) {
//            this.log.debug(this.log.buildMessage(() => ("svy * Form " + formname + " was set in hidden div. Clearing out hidden div.")));
//            $rootScope.updatingFormUrl = ''; // it's going to be shown; remove it from hidden DOM
//            $rootScope.updatingFormName = null;
//        }

        if (!formname) {
            throw new Error("formname is undefined");
        }
        // TODO do we need request initial data?
//        $sabloApplication.getFormState(formname).then(function (formState) {
//            // if first show of this form in browser window then request initial data (dataproviders and such);
//            if (formState.initializing && !formState.initialDataRequested) $servoyInternal.requestInitialData(formname, formState);
//        });
        if (notifyFormVisibility) {
            return  this.sabloService.callService('formService', 'formvisibility', {formname:formname,visible:true,parentForm:parentForm,bean:beanName,relation:relationname,formIndex:formIndex}, false);
        }
        // dummy promise
        return Promise.resolve(null);
    }
    public hideForm(formname,parentForm,beanName,relationname,formIndex,formnameThatWillBeShown,relationnameThatWillBeShown,formIndexThatWillBeShown) {
        if (!formname) {
            throw new Error("formname is undefined");
        }
        return  this.sabloService.callService('formService', 'formvisibility', {formname:formname,visible:false,parentForm:parentForm,bean:beanName,relation:relationname,formIndex:formIndex,show:{formname:formnameThatWillBeShown,relation:relationnameThatWillBeShown,formIndex:formIndexThatWillBeShown}});
    }

    public pushEditingStarted(formname, beanname, propertyname ) {
        var messageForServer = { formname : formname, beanname : beanname, property : propertyname };
			
        // // detect if this is a foundset linked dataprovider - in which case we need to provide a rowId for it to server and trim down the last array index which identifies the row on client
        // // TODO this is a big hack - see comment in pushDPChange below
        // var formState = $sabloApplication.getFormStateEvenIfNotYetResolved(formName);
        // var foundsetLinkedDPInfo = getFoundsetLinkedDPInfo(propertyName, formState.model[beanName]);
        // if (foundsetLinkedDPInfo)	{
        //     if (foundsetLinkedDPInfo.rowId) messageForServer['fslRowID'] = foundsetLinkedDPInfo.rowId;
        //     messageForServer.property = foundsetLinkedDPInfo.propertyNameForServer;
        // }	

        this.sabloService.callService("formService", "startEdit", messageForServer, true);
    }
}

export class FormCache {
    private componentCache: Map<String, ComponentCache>;
    private _mainStructure: StructureCache;

    private _items: Array<ComponentCache>;
    public navigatorForm: FormSettings;
    private conversionInfo = {};

    constructor() {
        this.componentCache = new Map();
        this._items = [];
    }
    public add( comp: ComponentCache ) {
        this.componentCache.set( comp.name, comp );
        this._items.push( comp );
    }
    set mainStructure( structure: StructureCache ) {
        this._mainStructure = structure;
        this.findComponents( structure );
    }

    get mainStructure(): StructureCache {
        return this._mainStructure;
    }

    get absolute(): boolean {
        return this._mainStructure == null;
    }
    get items(): Array<ComponentCache> {
        return this._items;
    }

    public getComponent( name: string ): ComponentCache {
        return this.componentCache.get( name );
    }

    public getConversionInfo( beanname: string ) {
        return this.conversionInfo[beanname];
    }
    public addConversionInfo( beanname: string, conversionInfo ) {
        const beanConversion = this.conversionInfo[beanname];
        if ( beanConversion == null ) {
            this.conversionInfo[beanname] = conversionInfo;
        }
        else for ( var key in conversionInfo ) {
            beanConversion[key] = conversionInfo[key];
        }
    }

    private findComponents( structure: StructureCache ) {
        structure.items.forEach( item => {
            if ( item instanceof StructureCache ) {
                this.findComponents( item );
            }
            else {
                this.add( item );
            }
        } )
    }
}

export class ComponentCache {
    constructor( public readonly name: string,
        public readonly type,
        public readonly model: { [property: string]: any },
        public readonly handlers: Array<String>,
        public readonly layout: { [property: string]: string } ) {
    }
}

export class StructureCache {
    constructor( public readonly classes: Array<string>,
        public readonly items?: Array<StructureCache | ComponentCache> ) {
        if ( !this.items ) this.items = [];
    }

    addChild( child: StructureCache | ComponentCache ): StructureCache {
        this.items.push( child );
        if ( child instanceof StructureCache )
            return child as StructureCache;
        return null;
    }
}
