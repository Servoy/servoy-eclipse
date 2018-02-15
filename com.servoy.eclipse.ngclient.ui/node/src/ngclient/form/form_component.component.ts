import { Component, Input, OnInit, OnDestroy, ViewChild, ViewChildren, TemplateRef, QueryList, Directive, ElementRef, Renderer2 } from '@angular/core';

import { FormService, FormCache, StructureCache, ComponentCache } from '../form.service';

import { ServoyService } from '../servoy.service'

import { SabloService } from '../../sablo/sablo.service'

import {ServoyApi} from '../../servoyapi/servoy_api'

@Component( {
    selector: 'svy-form',
    template: `
          <div *ngIf="formCache.absolute"> <!-- main div -->
              <div *ngFor="let item of formCache.items" [config]="item" class="svy_wrapper" style="position:absolute"> <!-- wrapper div -->
                   <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item}"></ng-template>  <!-- component  -->
              </div>
          </div>
          <div *ngIf="!formCache.absolute" [config]="formCache.mainStructure"> <!-- main container div -->
                <ng-template *ngFor="let item of formCache.mainStructure.items" [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item}"></ng-template>  <!-- component or responsive div  -->
          </div>
      <ng-template  #svyResponsiveDiv  let-state="state" >
          <div [config]="state">
                     <ng-template *ngFor="let item of state.items" [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item}"></ng-template>
          </div>
      </ng-template>
      <ng-template  #servoydefaultTextfield  let-state="state" >
        <servoydefault-textfield [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [name]="state.name" #cmp></servoydefault-textfield>
      </ng-template>
      <ng-template #servoydefaultButton let-state="state"><servoydefault-button  [borderType]="state.model.borderType" [foreground]="state.model.foreground" [hideText]="state.model.hideText" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [textRotation]="state.model.textRotation" [mnemonic]="state.model.mnemonic" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [imageMediaID]="state.model.imageMediaID" [fontType]="state.model.fontType" [margin]="state.model.margin" [visible]="state.model.visible" [format]="state.model.format" [mediaOptions]="state.model.mediaOptions" [dataProviderID]="state.model.dataProviderID" [showFocus]="state.model.showFocus" [horizontalAlignment]="state.model.horizontalAlignment" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [displaysTags]="state.model.displaysTags" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [rolloverCursor]="state.model.rolloverCursor" [rolloverImageMediaID]="state.model.rolloverImageMediaID" [tabSeq]="state.model.tabSeq" [verticalAlignment]="state.model.verticalAlignment" [onDoubleClickMethodID]="getHandler(state,'onDoubleClickMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [name]="state.name" #cmp></servoydefault-button></ng-template>
   `
} )
export class FormComponent implements OnInit, OnDestroy {
    @ViewChild( 'svyResponsiveDiv' ) readonly svyResponsiveDiv: TemplateRef<any>;
    @ViewChild( 'servoydefaultTextfield' ) readonly servoydefaultTextfield: TemplateRef<any>;
    @ViewChild( 'servoydefaultButton' ) readonly servoydefaultButton: TemplateRef<any>;

    @ViewChildren( 'cmp' ) readonly components: QueryList<Component>;

    @Input() readonly name;

    formCache: FormCache;

    private handlerCache: { [property: string]: { [property: string]: ( e ) => void } } = {};
    private servoyApiCache: { [property: string]: ServoyApi } = {};

    constructor( private formservice: FormService, private sabloService: SabloService, private servoyService: ServoyService ) {
    }

    getTemplate( item: StructureCache | ComponentCache ): TemplateRef<any> {
        if ( item instanceof StructureCache ) {
            return this.svyResponsiveDiv;
        }
        else {
            return this[item.type]
        }
    }

    ngOnInit() {
        this.formCache = this.formservice.getFormCache( this );
        this.sabloService.callService( 'formService', 'formLoaded', { formname: this.name }, true )
    }

    ngOnDestroy() {
    }

    private datachange( component: string, property: string, value ) {
        const model = this.formCache.getComponent( component ).model;
        const oldValue = model[property];
        this.formCache.getComponent( component ).model[property] = value;
        this.formservice.sendChanges( this.name, component, property, value, oldValue );
    }

    private getHandler( item: ComponentCache, handler: string ) {
        let  itemCache = this.handlerCache[item.name];
        if ( itemCache == null ) {
            itemCache = {};
            this.handlerCache[item.name] = itemCache;
        }
        let  func = itemCache[handler];
        if ( func == null ) {
            if ( item.handlers && item.handlers.indexOf( handler ) >= 0 ) {
                const me = this;
                func =  function( e ) {
                    me.formservice.executeEvent( me.name, item.name, handler, arguments );
                }
                itemCache[handler] = func;
            }
        }
        return func;
    }

    private getServoyApi( item: ComponentCache ) {
        let api = this.servoyApiCache[item.name];
        if (api == null) {
            api = new ServoyApi( item, this.name, this.formCache.absolute, this.formservice, this.servoyService );
            this.servoyApiCache[item.name] = api;
        }
        return api;
    }

    public callApi( componentName: string, apiName: string, args: object ) {
        let comp = this.components.find( item => item['name'] == componentName );
        let proto = Object.getPrototypeOf( comp )
        proto[apiName].apply( comp, args );
    }
}

@Directive( { selector: '[config]' } )
export class AddAttributeDirective implements OnInit {
    @Input() config;

    constructor( private el: ElementRef, private renderer: Renderer2 ) { }

    ngOnInit() {
        if ( this.config.classes ) {
            this.config.classes.forEach( cls => this.renderer.addClass( this.el.nativeElement, cls ) );
        }
        if ( this.config.styles ) {
            for ( let key in this.config.styles ) {
                this.renderer.setStyle( this.el.nativeElement, key, this.config.styles[key] );
            }
        }
        if ( this.config.layout ) {
            for ( let key in this.config.layout ) {
                this.renderer.setStyle( this.el.nativeElement, key, this.config.layout[key] );
            }
        }
    }
}



