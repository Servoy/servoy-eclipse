import { Injectable, TemplateRef } from '@angular/core';
import { Observable } from 'rxjs/Observable';

import { WebsocketService } from '../sablo/websocket.service';
import { SabloService } from '../sablo/sablo.service';

import { FormComponent } from './svy-form/svy-form.component'

import { ConverterService } from '../sablo/converter.service'


@Injectable()
export class FormService {

    private formsCache: Map<String, FormCache>;

    constructor( private sabloService: SabloService, private converterService: ConverterService, websocketService: WebsocketService ) {

        this.formsCache = new Map();

        websocketService.getSession().then(( session ) => {
            session.onMessageObject(( msg, conversionInfo ) => {
                if ( msg.forms ) {
                    for ( var formname in msg.forms ) {
                        const formCache = this.formsCache.get( formname );
                        if ( formCache != null ) {
                            const formConversion = conversionInfo ? conversionInfo.forms[formname] : null;
                            var formData = msg.forms[formname];
                            for ( var beanname in formData ) {
                                const comp = formCache.getComponent( beanname );
                                const beanConversion = formConversion ? formConversion[beanname] : null;
                                for ( var property in formData[beanname] ) {
                                    let value = formData[beanname][property];
                                    if (beanConversion && beanConversion[property]) {
                                       value = this.converterService.convertFromServerToClient( value, beanConversion[property], comp.model[property], null, null );
                                    }
                                    comp.model[property] = value;
                                }
                            }
                        }
                        else {
                            // wait for the form cache to arrive??
                        }
                    }
                }
            } );
        } );

        //      // some sample data
        //      var absolute= true;
        //      var formCache = new FormCache();
        //      
        //      if (absolute) {
        //          formCache.add(new ComponentCache("text1","svyTextfield", {dataprovider:4},[],{left:"10px",top:"10px"}));
        //          formCache.add(new ComponentCache("text2","svyTextfield", {dataprovider:5},[],{left:"20px",top:"40px"}));
        //          formCache.add(new ComponentCache("button1","svyButton", {dataprovider:6},['click'],{left:"10px",top:"70px"}));
        //          formCache.add(new ComponentCache("button2","svyButton", {dataprovider:7},[],{left:"30px",top:"100px"}));
        //      }
        //      else {
        //          var main = new StructureCache(["container-fluid"]);
        //          var row = main.addChild(new StructureCache(["row"]) );
        //          row.addChild(new StructureCache(["col-md-4"],[new ComponentCache("text1","svyTextfield", {dataprovider:4},[],{})]));
        //          row.addChild(new StructureCache(["col-md-4"],[new ComponentCache("text2","svyTextfield", {dataprovider:5},[],{})])); 
        //          row.addChild(new StructureCache(["col-md-4"],[new ComponentCache("button1","svyButton", {dataprovider:6},['click'],{}),
        //                                                              new ComponentCache("button2","svyButton", {dataprovider:7},[],{})])); 
        //          formCache.mainStructure = main;
        //      }
        //      this.formsCache.set("test", formCache);
    }

    public getFormCache( form: FormComponent ): FormCache {
        var formCache = this.formsCache.get( form.name );
        if ( formCache != null ) return formCache;

    }

    public createFormCache( formName: String, jsonData ) {
        var formCache = new FormCache();
        if ( jsonData.responsive ) {

        }
        else {
            jsonData.children.forEach(( elem ) => {
                if ( elem.model[ConverterService.TYPES_KEY] != null ) {
                    this.converterService.convertFromServerToClient( elem.model, elem.model[ConverterService.TYPES_KEY], null, null, null );
                    formCache.addConversionInfo( elem.name, elem.model[ConverterService.TYPES_KEY] );
                }
                formCache.add( new ComponentCache( elem.name, elem.type, elem.model, elem.handlers, elem.position ) );
            } );
        }
        this.formsCache.set( formName, formCache )
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

    public executeEvent( formname: string, beanname: string, handler: string, args: IArguments ) {
        console.log( formname + "," + beanname + ", executing: " + handler + " with values: " + JSON.stringify( args ) );
        
        var newargs = this.converterService.getEventArgs(args, handler);
        var data = {};
//        if (property) {
//            data[property] = formState.model[beanName][property];
//        }
        var cmd = { formname: formname, beanname: beanname, event: handler, args: newargs, changes: data };
//        if (rowId)
//            cmd['rowId'] = rowId;
        return this.sabloService.callService('formService', 'executeEvent', cmd, false);
    }


    public formWillShow( name: string, server: boolean ) {

    }
}

export class FormCache {
    private componentCache: Map<String, ComponentCache>;
    private _mainStructure: StructureCache;

    private _items: Array<ComponentCache>;

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
        public readonly model: { [property: string]: Object },
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
