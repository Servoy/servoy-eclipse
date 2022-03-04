import {
    Component, Input, OnInit, OnDestroy, OnChanges, SimpleChanges, ViewChild, ViewChildren,
    TemplateRef, Directive, ElementRef, Renderer2, ChangeDetectionStrategy, ChangeDetectorRef, SimpleChange, Inject
} from '@angular/core';

import { FormCache, StructureCache, FormComponentCache, ComponentCache, instanceOfApiExecutor, PartCache, FormComponentProperties } from '../types';

import { ServoyService } from '../servoy.service';

import { SabloService } from '../../sablo/sablo.service';
import { LoggerService, LoggerFactory } from '@servoy/public';

import { ServoyApi } from '../servoy_api';
import { FormService } from '../form.service';
import { DOCUMENT } from '@angular/common';
import { ServoyBaseComponent } from '@servoy/public';

@Component({
    template: ''
})
export abstract class AbstractFormComponent {

    abstract getFormCache(): FormCache;

    abstract getTemplateForLFC(state: ComponentCache): TemplateRef<any>;

    abstract getContainerByName(containername: string): Element;

    _containers: { added: any; removed: any };
    _cssstyles: { [x: string]: any };

    constructor(protected renderer: Renderer2) {
    }

    @Input()
    set containers(containers: { added: any; removed: any }) {
        if (!containers) return;
        this._containers = containers;
        for (const containername in containers.added) {
            const container = this.getContainerByName(containername);
            if (container) {
                containers.added[containername].forEach((cls: string) => this.renderer.addClass(container, cls));
            }
        }
        for (const containername in containers.removed) {
            const container = this.getContainerByName(containername);
            if (container) {
                containers.removed[containername].forEach((cls: string) => this.renderer.removeClass(container, cls));
            }
        }
    }

    get containers() {
        return this._containers;
    }

    @Input('cssStyles')
    set cssstyles(cssStyles: { [x: string]: any }) {
        if (!cssStyles) return;
        this._cssstyles = cssStyles;
        for (const containername in cssStyles) {
            const container = this.getContainerByName(containername);
            if (container) {
                const stylesMap = cssStyles[containername];
                for (const key in stylesMap) {
                    this.renderer.setStyle(container, key, stylesMap[key]);
                }
            }
        }
    }

    get cssstyles() {
        return this._cssstyles;
    }
}

@Component({
    // eslint-disable-next-line
    selector: 'svy-form',
    changeDetection: ChangeDetectionStrategy.OnPush,
    /* eslint-disable max-len */
    template: `
      <div *ngIf="formCache.absolute" [ngStyle]="getAbsoluteFormStyle()" class="svy-form" [ngClass]="formClasses" svyAutosave> <!-- main div -->
           <div *ngFor="let part of formCache.parts" [svyContainerStyle]="part" [svyContainerLayout]="part.layout" [svyContainerClasses]="part.classes"> <!-- part div -->
               <div *ngFor="let item of part.items" [svyContainerStyle]="item" [svyContainerLayout]="item.layout" class="svy-wrapper" [ngStyle]="item.model.visible === false && {'display': 'none'}" style="position:absolute"> <!-- wrapper div -->
                   <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this }"></ng-template>  <!-- component or formcomponent -->
                </div>
          </div>
      </div>
      <div *ngIf="!formCache.absolute" class="svy-form svy-respform svy-overflow-auto" [ngClass]="formClasses"> <!-- main container div -->
            <ng-template *ngFor="let item of formCache.mainStructure.items" [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this}"></ng-template>  <!-- component or responsive div  -->
      </div>

      <ng-template  #svyResponsiveDiv  let-state="state" >
          <div [svyContainerStyle]="state" [svyContainerClasses]="state.classes" [svyContainerAttributes]="state.attributes" class="svy-layoutcontainer">
               <ng-template *ngFor="let item of state.items" [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this}"></ng-template>
          </div>
      </ng-template>
      <!-- structure template generate start -->
      <!-- structure template generate end -->
      <ng-template  #formComponentAbsoluteDiv  let-state="state" >
          <div [svyContainerStyle]="state.formComponentProperties" [svyContainerLayout]="state.formComponentProperties.layout" [svyContainerClasses]="state.formComponentProperties.classes" [svyContainerAttributes]="state.formComponentProperties.attributes" style="position:relative" class="svy-formcomponent">
               <div *ngFor="let item of state.items" [svyContainerStyle]="item" [svyContainerLayout]="item.layout" class="svy-wrapper" [ngStyle]="item.model.visible === false && {'display': 'none'}" style="position:absolute"> <!-- wrapper div -->
                   <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this }"></ng-template>  <!-- component  -->
               </div>
          </div>
      </ng-template>
      <ng-template  #formComponentResponsiveDiv  let-state="state" >
          <ng-template *ngFor="let item of state.items" [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this }"></ng-template>  <!-- component  -->
      </ng-template>
      <!-- component template generate start -->
<ng-template #servoycoreDefaultLoadingIndicator let-callback="callback" let-state="state"><servoycore-defaultLoadingIndicator  [servoyAttributes]="state.model.servoyAttributes" [cssPosition]="state.model.cssPosition" [location]="state.model.location" [size]="state.model.size" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-defaultLoadingIndicator></ng-template>
<ng-template #servoycoreErrorbean let-callback="callback" let-state="state"><servoycore-errorbean  [servoyAttributes]="state.model.servoyAttributes" [cssPosition]="state.model.cssPosition" [error]="state.model.error" [location]="state.model.location" [size]="state.model.size" [toolTipText]="state.model.toolTipText" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-errorbean></ng-template>
<ng-template #servoycoreFormcomponent let-callback="callback" let-state="state"><servoycore-formcomponent  [servoyAttributes]="state.model.servoyAttributes" [containedForm]="state.model.containedForm" [cssPosition]="state.model.cssPosition" [height]="state.model.height" [location]="state.model.location" [size]="state.model.size" [styleClass]="state.model.styleClass" *ngIf="state.model.visible" [width]="state.model.width" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-formcomponent></ng-template>
<ng-template #servoycoreFormcontainer let-callback="callback" let-state="state"><servoycore-formcontainer  [servoyAttributes]="state.model.servoyAttributes" [containedForm]="state.model.containedForm" [cssPosition]="state.model.cssPosition" [height]="state.model.height" [location]="state.model.location" [relationName]="state.model.relationName" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" *ngIf="state.model.visible" [waitForData]="state.model.waitForData" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp><ng-template let-name='name'><svy-form *ngIf="isFormAvailable(name)" [name]="name"></svy-form></ng-template></servoycore-formcontainer></ng-template>
<ng-template #servoycoreListformcomponent let-callback="callback" let-state="state"><servoycore-listformcomponent  [servoyAttributes]="state.model.servoyAttributes" [containedForm]="state.model.containedForm" [cssPosition]="state.model.cssPosition" [foundset]="state.model.foundset" [location]="state.model.location" [pageLayout]="state.model.pageLayout" [paginationStyleClass]="state.model.paginationStyleClass" [readOnly]="state.model.readOnly" [responsivePageSize]="state.model.responsivePageSize" [rowStyleClass]="state.model.rowStyleClass" [rowStyleClassDataprovider]="state.model.rowStyleClassDataprovider" [selectionClass]="state.model.selectionClass" [size]="state.model.size" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" *ngIf="state.model.visible" [onSelectionChanged]="callback.getHandler(state,'onSelectionChanged')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-listformcomponent></ng-template>
<ng-template #servoycoreNavigator let-callback="callback" let-state="state"><servoycore-navigator  [servoyAttributes]="state.model.servoyAttributes" [cssPosition]="state.model.cssPosition" [currentIndex]="state.model.currentIndex" [hasMore]="state.model.hasMore" [location]="state.model.location" [maxIndex]="state.model.maxIndex" [minIndex]="state.model.minIndex" [size]="state.model.size" [setSelectedIndex]="callback.getHandler(state,'setSelectedIndex')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-navigator></ng-template>
<ng-template #servoycoreSlider let-callback="callback" let-state="state"><servoycore-slider  [animate]="state.model.animate" [servoyAttributes]="state.model.servoyAttributes" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="callback.datachange(state,'dataProviderID',$event, true)" [enabled]="state.model.enabled" [location]="state.model.location" [max]="state.model.max" [min]="state.model.min" [orientation]="state.model.orientation" [range]="state.model.range" [size]="state.model.size" [step]="state.model.step" *ngIf="state.model.visible" [onChangeMethodID]="callback.getHandler(state,'onChangeMethodID')" [onCreateMethodID]="callback.getHandler(state,'onCreateMethodID')" [onSlideMethodID]="callback.getHandler(state,'onSlideMethodID')" [onStartMethodID]="callback.getHandler(state,'onStartMethodID')" [onStopMethodID]="callback.getHandler(state,'onStopMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-slider></ng-template>
     <!-- component template generate end -->
   `
    /* eslint-enable max-len */
})
export class FormComponent extends AbstractFormComponent implements OnDestroy, OnChanges {
    @ViewChild('svyResponsiveDiv', { static: true }) readonly svyResponsiveDiv: TemplateRef<any>;
    // structure viewchild template generate start
    // structure viewchild template generate end
    @ViewChild('formComponentAbsoluteDiv', { static: true }) readonly formComponentAbsoluteDiv: TemplateRef<any>;
    @ViewChild('formComponentResponsiveDiv', { static: true }) readonly formComponentResponsiveDiv: TemplateRef<any>;

    // component viewchild template generate start
    @ViewChild('servoycoreSlider', { static: true }) readonly servoycoreSlider: TemplateRef<any>;
    @ViewChild('servoycoreErrorbean', { static: true }) readonly servoycoreErrorbean: TemplateRef<any>;
    @ViewChild('servoycoreListformcomponent', { static: true }) readonly servoycoreListformcomponent: TemplateRef<any>;
    @ViewChild('servoycoreFormcontainer', { static: true }) readonly servoycoreFormcontainer: TemplateRef<any>;

    // component viewchild template generate end



    @Input() name: string;

    formClasses: string[];

    formCache: FormCache;

    absolutFormPosition = {};

    private handlerCache: { [property: string]: { [property: string]: () => void } } = {};
    private servoyApiCache: { [property: string]: ServoyApi } = {};
    private componentCache: { [property: string]: ServoyBaseComponent<any> } = {};
    private log: LoggerService;

    constructor(private formservice: FormService, private sabloService: SabloService,
        private servoyService: ServoyService, logFactory: LoggerFactory,
        private changeHandler: ChangeDetectorRef,
        private el: ElementRef, protected renderer: Renderer2,
        @Inject(DOCUMENT) private document: Document) {
        super(renderer);
        this.log = logFactory.getLogger('FormComponent');
    }

    public detectChanges() {
        this.changeHandler.detectChanges();
    }

    public formCacheChanged(cache: FormCache): void {
        this.formCache = cache;
        this.detectChanges();
    }

    public getFormCache(): FormCache {
        return this.formCache;
    }

    propertyChanged(componentName: string, property: string, value: any): void {
        const comp = this.componentCache[componentName];
        if (comp) {
            const change = {};
            change[property] = new SimpleChange(value, value, false);
            comp.ngOnChanges(change);
            // this is kind of like a push so we should trigger this.
            comp.detectChanges();
        }
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.name) {
            //
            // Form Instances are reused for tabpanels that have a template reference to this make sure to clean up the old reference/name to this instance
            if (changes.name.previousValue) this.formservice.destroy(changes.name.previousValue);
            // really make sure all form state is reverted to default for this new name
            this.formCache = this.formservice.getFormCache(this);
            const styleClasses: string = this.formCache.getComponent('').model.styleClass;
            if (styleClasses)
                this.formClasses = styleClasses.split(' ');
            else
                this.formClasses = null;

            this._containers = this.formCache.getComponent('').model.containers;
            this._cssstyles = this.formCache.getComponent('').model.cssstyles;
            this.handlerCache = {};
            this.servoyApiCache = {};
            this.componentCache = {};

            this.sabloService.callService('formService', 'formLoaded', { formname: this.name }, true);
            this.renderer.setAttribute(this.el.nativeElement, 'name', this.name);

        }
       this.updateFormStyleClasses(this.formservice.getFormStyleClasses(this.name));
    }

    ngOnDestroy() {
        this.formservice.destroy(this.name);
    }

    getTemplate(item: StructureCache | ComponentCache | FormComponentCache): TemplateRef<any> {
        if (item instanceof StructureCache) {
            return item.tagname ? this[item.tagname] : this.svyResponsiveDiv;
        } else if (item instanceof FormComponentCache) {
            if (item.hasFoundset) return this.servoycoreListformcomponent;
            return item.responsive ? this.formComponentResponsiveDiv : this.formComponentAbsoluteDiv;
        } else {
            if (this[item.type] === undefined && item.type !== undefined) {
                this.log.error(this.log.buildMessage(() => ('Template for ' + item.type + ' was not found, please check form_component template.')));
            }
            return this[item.type];
        }
    }

    getTemplateForLFC(state: ComponentCache): TemplateRef<any> {
        if (state.type.includes('formcomponent')) {
            return state.model.containedForm.absoluteLayout ? this.formComponentAbsoluteDiv : this.formComponentResponsiveDiv;
        } else {
            // TODO: this has to be replaced with a type property on the state object
            let compDirectiveName = state.type;
            const index = compDirectiveName.indexOf('-');
            compDirectiveName = compDirectiveName.replace('-', '');
            return this[compDirectiveName.substring(0, index) + compDirectiveName.charAt(index).toUpperCase() + compDirectiveName.substring(index + 1)];
        }
    }

    public getAbsoluteFormStyle() {
        const formData = this.formCache.getComponent('');

        for (const key in this.absolutFormPosition) {
            if (this.absolutFormPosition.hasOwnProperty(key)) {
                delete this.absolutFormPosition[key];
            }
        }
        this.absolutFormPosition['left'] = '0px';
        this.absolutFormPosition['top'] = '0px';
        this.absolutFormPosition['right'] = '0px';
        this.absolutFormPosition['bottom'] = '0px';
        this.absolutFormPosition['position'] = 'absolute';

        if (formData.model.borderType) {
            const borderStyle = formData.model.borderType;
            for (const key of Object.keys(borderStyle)) {
                this.absolutFormPosition[key] = borderStyle[key];
            }
        }
        if (formData.model.transparent) {
            this.absolutFormPosition['backgroundColor'] = 'transparent';
        }

        if (formData.model.addMinSize) {
            if (formData.model.hasExtraParts || this.el.nativeElement.parentNode.closest('.svy-form') == null) {
                // see svyFormstyle from ng1
                this.absolutFormPosition['minWidth'] = this.formCache.size.width + 'px';
                this.absolutFormPosition['minHeight'] = this.formCache.size.height + 'px';
            }
        }
        return this.absolutFormPosition;
    }

    public isFormAvailable(name: string): boolean {
        // console.log("isFormAvailable: " + name + " " +  this.formservice.hasFormCacheEntry( name));
        return this.formservice.hasFormCacheEntry(name);
    }

    datachange(component: ComponentCache, property: string, value, dataprovider: boolean) {
        const model = this.formCache.getComponent(component.name).model;
        const oldValue = model[property];
        this.formCache.getComponent(component.name).model[property] = value;
        this.formservice.sendChanges(this.name, component.name, property, value, oldValue, dataprovider);
    }

    getHandler(item: ComponentCache, handler: string) {
        let itemCache = this.handlerCache[item.name];
        if (itemCache == null) {
            itemCache = {};
            this.handlerCache[item.name] = itemCache;
        }
        let func = itemCache[handler];
        if (func == null && item.handlers && item.handlers.indexOf(handler) >= 0) {
            const me = this;
            // eslint-disable-next-line
            func = function() {
                return me.formservice.executeEvent(me.name, item.name, handler, arguments);
            };
            itemCache[handler] = func;
        }
        return func;
    }

    registerComponent(component: ServoyBaseComponent<any>): void {
        this.componentCache[component.name] = component;
    }

    unRegisterComponent(component: ServoyBaseComponent<any>): void {
        delete this.componentCache[component.name];
    }

    getServoyApi(item: ComponentCache) {
        let api = this.servoyApiCache[item.name];
        if (api == null) {
            api = new FormComponentServoyApi(item, this.name, this.formCache.absolute, this.formservice, this.servoyService, this);
            this.servoyApiCache[item.name] = api;
        }
        return api;
    }

    public callApi(componentName: string, apiName: string, args: any, path?: string[]): any {
        if (path && path.length > 0) {
            const comp = this.componentCache[path[0]];
            if (instanceOfApiExecutor(comp)) {
                comp.callApi(path[1], apiName, args, path.slice(2));
            } else {
                this.log.error('trying to call api: ' + apiName + ' on component: ' + componentName + ' with path: ' + path +
                    ', but comp: ' + (comp == null ? ' is not found' : comp.name + ' doesnt implement IApiExecutor'));
            }

        } else {
            const comp = this.componentCache[componentName];
            const proto = Object.getPrototypeOf(comp);
            if (proto[apiName]) {
                return proto[apiName].apply(comp, args);
            } else {
                this.log.error(this.log.buildMessage(() => ('Api ' + apiName + ' for component ' + componentName + ' was not found, please check component implementation.')));
                return null;
            }
        }
    }

    getContainerByName(containername: string): Element {
        return this.document.querySelector('[name="' + this.name + '.' + containername + '"]');
    }

    public updateFormStyleClasses(ngutilsstyleclasses: string): void{
        if (ngutilsstyleclasses) {
            if (!this.formClasses) {
                this.formClasses = ngutilsstyleclasses.split(' ');
            } else {
                this.formClasses = this.formClasses.concat(ngutilsstyleclasses.split(' '));
            }
        }
    }
}

class FormComponentServoyApi extends ServoyApi {
    constructor(item: ComponentCache,
        formname: string,
        absolute: boolean,
        formservice: FormService,
        servoyService: ServoyService,
        private fc: FormComponent) {
        super(item, formname, absolute, formservice, servoyService, false);
    }

    registerComponent(comp: ServoyBaseComponent<any>) {
        this.fc.registerComponent(comp);
    }

    unRegisterComponent(comp: ServoyBaseComponent<any>) {
        this.fc.unRegisterComponent(comp);
    }
}

