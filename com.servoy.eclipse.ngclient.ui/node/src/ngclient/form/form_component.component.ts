import { Component, Input, OnInit, OnDestroy, ViewChild, ViewChildren, TemplateRef, QueryList, Directive, ElementRef, Renderer2, NgModule } from '@angular/core';

import { FormService, FormCache, StructureCache, ComponentCache } from '../form.service';

import { ServoyService } from '../servoy.service'

import { SabloService } from '../../sablo/sablo.service'

import { ServoyApi } from '../servoy_api'

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
      <ng-template #servoydefaultTextfield let-state="state"><servoydefault-textfield  [borderType]="state.model.borderType" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [valuelistID]="state.model.valuelistID" [findmode]="state.model.findmode" [placeholderText]="state.model.placeholderText" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [fontType]="state.model.fontType" [margin]="state.model.margin" [visible]="state.model.visible" [editable]="state.model.editable" [format]="state.model.format" [readOnly]="state.model.readOnly" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="datachange(state.name,'dataProviderID',$event)" [horizontalAlignment]="state.model.horizontalAlignment" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [displaysTags]="state.model.displaysTags" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [selectOnEnter]="state.model.selectOnEnter" [tabSeq]="state.model.tabSeq" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [onFocusLostMethodID]="getHandler(state,'onFocusLostMethodID')" [onDataChangeMethodID]="getHandler(state,'onDataChangeMethodID')" [onFocusGainedMethodID]="getHandler(state,'onFocusGainedMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [name]="state.name" #cmp></servoydefault-textfield></ng-template>
      <ng-template #servoydefaultButton let-state="state"><servoydefault-button  [borderType]="state.model.borderType" [foreground]="state.model.foreground" [hideText]="state.model.hideText" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [textRotation]="state.model.textRotation" [mnemonic]="state.model.mnemonic" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [imageMediaID]="state.model.imageMediaID" [fontType]="state.model.fontType" [margin]="state.model.margin" [visible]="state.model.visible" [format]="state.model.format" [mediaOptions]="state.model.mediaOptions" [dataProviderID]="state.model.dataProviderID" [showFocus]="state.model.showFocus" [horizontalAlignment]="state.model.horizontalAlignment" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [displaysTags]="state.model.displaysTags" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [rolloverCursor]="state.model.rolloverCursor" [rolloverImageMediaID]="state.model.rolloverImageMediaID" [tabSeq]="state.model.tabSeq" [verticalAlignment]="state.model.verticalAlignment" [onDoubleClickMethodID]="getHandler(state,'onDoubleClickMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [name]="state.name" #cmp></servoydefault-button></ng-template>
      <ng-template #servoydefaultLabel let-state="state"><servoydefault-label  [borderType]="state.model.borderType" [labelFor]="state.model.labelFor" [foreground]="state.model.foreground" [hideText]="state.model.hideText" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [textRotation]="state.model.textRotation" [mnemonic]="state.model.mnemonic" [text]="state.model.text" [toolTipText]="state.model.toolTipText" [imageMediaID]="state.model.imageMediaID" [fontType]="state.model.fontType" [margin]="state.model.margin" [visible]="state.model.visible" [format]="state.model.format" [mediaOptions]="state.model.mediaOptions" [dataProviderID]="state.model.dataProviderID" [showFocus]="state.model.showFocus" [horizontalAlignment]="state.model.horizontalAlignment" [size]="state.model.size" (sizeChange)="datachange(state.name,'size',$event)" [background]="state.model.background" [displaysTags]="state.model.displaysTags" [location]="state.model.location" (locationChange)="datachange(state.name,'location',$event)" [rolloverCursor]="state.model.rolloverCursor" [rolloverImageMediaID]="state.model.rolloverImageMediaID" [tabSeq]="state.model.tabSeq" [verticalAlignment]="state.model.verticalAlignment" [onDoubleClickMethodID]="getHandler(state,'onDoubleClickMethodID')" [onRightClickMethodID]="getHandler(state,'onRightClickMethodID')" [onActionMethodID]="getHandler(state,'onActionMethodID')" [servoyApi]="getServoyApi(state)" [name]="state.name" #cmp></servoydefault-label></ng-template>
       <ng-template #servoydefaultTabpanel let-state="state"><servoydefault-tabpanel  [borderType]="state.model.borderType" [fontType]="state.model.fontType" [visible]="state.model.visible" [selectedTabColor]="state.model.selectedTabColor" [tabs]="state.model.tabs" (tabsChange)="datachange(state.name,'tabs',$event)" [readOnly]="state.model.readOnly" [tabIndex]="state.model.tabIndex" (tabIndexChange)="datachange(state.name,'tabIndex',$event)" [foreground]="state.model.foreground" [styleClass]="state.model.styleClass" [enabled]="state.model.enabled" [transparent]="state.model.transparent" [activeTabIndex]="state.model.activeTabIndex" (activeTabIndexChange)="datachange(state.name,'activeTabIndex',$event)" [horizontalAlignment]="state.model.horizontalAlignment" [size]="state.model.size" [background]="state.model.background" [tabOrientation]="state.model.tabOrientation" [location]="state.model.location" [tabSeq]="state.model.tabSeq" [onChangeMethodID]="getHandler(state,'onChangeMethodID')" [servoyApi]="getServoyApi(state)" [name]="state.name" #cmp><ng-template let-name='name'><svy-form *ngIf="getForm(name)"  [name]="name"></svy-form></ng-template></servoydefault-tabpanel></ng-template>
   `
} )
export class FormComponent implements OnInit, OnDestroy {
    @ViewChild( 'svyResponsiveDiv' ) readonly svyResponsiveDiv: TemplateRef<any>;
    @ViewChild( 'servoydefaultTextfield' ) readonly servoydefaultTextfield: TemplateRef<any>;
    @ViewChild( 'servoydefaultButton' ) readonly servoydefaultButton: TemplateRef<any>;
    @ViewChild( 'servoydefaultLabel' ) readonly servoydefaultLabel: TemplateRef<any>;
    @ViewChild( 'servoydefaultTabpanel' ) readonly servoydefaultTabpanel: TemplateRef<any>;

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
        console.log("fc: " + this.name)
        this.formCache = this.formservice.getFormCache( this );
        this.sabloService.callService( 'formService', 'formLoaded', { formname: this.name }, true )
    }

    ngOnDestroy() {
    }
    
    public getForm(name):boolean {
        console.log("getform: " + name)
        return this.formservice.getFormCache( this ) == null;
    }

    private datachange( component: string, property: string, value ) {
        const model = this.formCache.getComponent( component ).model;
        const oldValue = model[property];
        this.formCache.getComponent( component ).model[property] = value;
        this.formservice.sendChanges( this.name, component, property, value, oldValue );
    }

    private getHandler( item: ComponentCache, handler: string ) {
        let itemCache = this.handlerCache[item.name];
        if ( itemCache == null ) {
            itemCache = {};
            this.handlerCache[item.name] = itemCache;
        }
        let func = itemCache[handler];
        if ( func == null ) {
            if ( item.handlers && item.handlers.indexOf( handler ) >= 0 ) {
                const me = this;
                func = function( e ) {
                    me.formservice.executeEvent( me.name, item.name, handler, arguments );
                }
                itemCache[handler] = func;
            }
        }
        return func;
    }

    private getServoyApi( item: ComponentCache ) {
        let api = this.servoyApiCache[item.name];
        if ( api == null ) {
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



