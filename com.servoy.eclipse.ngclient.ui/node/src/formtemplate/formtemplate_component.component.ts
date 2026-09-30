import {
  Component, Input, OnChanges, SimpleChanges, viewChild,
  TemplateRef, ElementRef, Renderer2, ChangeDetectionStrategy, ChangeDetectorRef, Inject, ViewEncapsulation,
  DOCUMENT
} from '@angular/core';

import { FormCache, StructureCache, FormComponentCache, ComponentCache, IFormComponent } from '../ngclient/types';

import { ServoyService } from '../ngclient/servoy.service';

import { LoggerService, LoggerFactory, ServoyBaseComponent } from '@servoy/public';

import { ServoyApi } from '../ngclient/servoy_api';
import { FormService } from '../ngclient/form.service';

import { AbstractFormComponent } from '../ngclient/form/form_component.component';

@Component({
    // eslint-disable-next-line
    selector: 'svy-formtemplate',
    changeDetection: ChangeDetectionStrategy.OnPush,
    encapsulation: ViewEncapsulation.None,
    /* eslint-disable max-len */
    template: `
      @if (formCache.absolute) {
        <div [ngStyle]="getAbsoluteFormStyle()" class="svy-form" [ngClass]="formClasses"> <!-- main div -->
          @for (part of formCache.parts; track part.rId) {
            <div [svyContainerStyle]="part" [svyContainerLayout]="part.layout" [svyContainerClasses]="part.classes"> <!-- part div -->
              @for (item of part.items; track item.rId) {
                <div [svyContainerStyle]="item" [svyContainerLayout]="item.layout" class="svy-wrapper" [ngStyle]="item.model.visible === false && {'display': 'none'}" style="position:absolute"> <!-- wrapper div -->
                  <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this }"></ng-template>  <!-- component or formcomponent -->
                </div>
              }
            </div>
          }
        </div>
      }
      @if (!formCache.absolute&&formCache.mainStructure) {
        <div class="svy-form svy-respform" [ngClass]="formClasses"> <!-- main container div -->
          @for (item of formCache.mainStructure.items; track item.rId) {
            <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this}"></ng-template>
            }  <!-- component or responsive div  -->
          </div>
        }

        <ng-template  #svyResponsiveDiv  let-state="state" >
          <div [svyContainerStyle]="state" [svyContainerClasses]="state.classes" [svyContainerAttributes]="state.attributes" class="svy-layoutcontainer">
            @for (item of state.items; track item.rId) {
              <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this}"></ng-template>
            }
          </div>
        </ng-template>

        <ng-template  #cssPositionContainer  let-state="state" >
          <div [svyContainerStyle]="state" [svyContainerClasses]="state.classes" [svyContainerAttributes]="state.attributes" class="svy-layoutcontainer">
            @for (item of state.items; track item.rId) {
              <div [svyContainerStyle]="item" [svyContainerLayout]="item.layout" class="svy-wrapper" [ngStyle]="item.model.visible === false && {'display': 'none'}" style="position:absolute"> <!-- wrapper div -->
                <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this}"></ng-template>
              </div>
            }
          </div>
        </ng-template>

        <!-- structure template generate start -->
        <!-- structure template generate end -->
        <ng-template  #formComponentAbsoluteDiv  let-state="state" >
          @if (state.model.visible) {
            <div [svyContainerStyle]="state.formComponentProperties" [svyContainerLayout]="state.formComponentProperties.layout" [svyContainerClasses]="state.formComponentProperties.classes" [svyContainerAttributes]="state.formComponentProperties.attributes" style="position:relative" class="svy-formcomponent">
              @for (item of state.items; track item.rId) {
                <div [svyContainerStyle]="item" [svyContainerLayout]="item.layout" class="svy-wrapper" [ngStyle]="item.model.visible === false && {'display': 'none'}" style="position:absolute"> <!-- wrapper div -->
                  <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this }"></ng-template>  <!-- component  -->
                </div>
              }
            </div>
          }
        </ng-template>
        <ng-template  #formComponentResponsiveDiv  let-state="state" >
          @if (state.model.visible) {
            <servoycore-formcomponent-responsive-container  [items]="state.items" [class]="state.model.styleClass" [formComponent]="this"></servoycore-formcomponent-responsive-container>
          }
        </ng-template>
        <!-- component template generate start -->
        <ng-template #servoycoreDefaultLoadingIndicator let-callback="callback" let-state="state"><servoycore-defaultLoadingIndicator  [servoyAttributes]="state.model.servoyAttributes" [cssPosition]="state.model.cssPosition" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-defaultLoadingIndicator></ng-template>
        <ng-template #servoycoreErrorbean let-callback="callback" let-state="state"><servoycore-errorbean  [servoyAttributes]="state.model.servoyAttributes" [cssPosition]="state.model.cssPosition" [error]="state.model.error" [toolTipText]="state.model.toolTipText" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-errorbean></ng-template>
        <ng-template #servoycoreFormcomponent let-callback="callback" let-state="state">@if (state.model.visible) {
          <servoycore-formcomponent  [servoyAttributes]="state.model.servoyAttributes" [containedForm]="state.model.containedForm" [cssPosition]="state.model.cssPosition" [height]="state.model.height" [styleClass]="state.model.styleClass" [width]="state.model.width" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-formcomponent>
        }</ng-template>
        <ng-template #servoycoreFormcontainer let-callback="callback" let-state="state">@if (state.model.visible) {
          <servoycore-formcontainer  [servoyAttributes]="state.model.servoyAttributes" [containedForm]="state.model.containedForm" [cssPosition]="state.model.cssPosition" [height]="state.model.height" [relationName]="state.model.relationName" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [waitForData]="state.model.waitForData" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp><ng-template let-name='name'>@if (isFormAvailable(name)) {
            <svy-formtemplate [name]="name"></svy-formtemplate>
          }</ng-template></servoycore-formcontainer>
        }</ng-template>
        <ng-template #servoycoreListformcomponent let-callback="callback" let-state="state">@if (state.model.visible) {
          <servoycore-listformcomponent  [servoyAttributes]="state.model.servoyAttributes" [containedForm]="state.model.containedForm" [cssPosition]="state.model.cssPosition" [foundset]="state.model.foundset" [pageLayout]="state.model.pageLayout" [paginationStyleClass]="state.model.paginationStyleClass" [readOnly]="state.model.readOnly" [responsivePageSize]="state.model.responsivePageSize" [rowStyleClass]="state.model.rowStyleClass" [rowStyleClassDataprovider]="state.model.rowStyleClassDataprovider" [selectionClass]="state.model.selectionClass" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [onSelectionChanged]="callback.getHandler(state,'onSelectionChanged')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-listformcomponent>
        }</ng-template>
        <ng-template #servoycoreNavigator let-callback="callback" let-state="state"><servoycore-navigator  [servoyAttributes]="state.model.servoyAttributes" [cssPosition]="state.model.cssPosition" [currentIndex]="state.model.currentIndex" [hasMore]="state.model.hasMore" [maxIndex]="state.model.maxIndex" [minIndex]="state.model.minIndex" [setSelectedIndex]="callback.getHandler(state,'setSelectedIndex')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-navigator></ng-template>
        <ng-template #servoycoreSlider let-callback="callback" let-state="state">@if (state.model.visible) {
          <servoycore-slider  [animate]="state.model.animate" [servoyAttributes]="state.model.servoyAttributes" [cssPosition]="state.model.cssPosition" [dataProviderID]="state.model.dataProviderID" (dataProviderIDChange)="callback.datachange(state,'dataProviderID',$event, true)" [enabled]="state.model.enabled" [max]="state.model.max" [min]="state.model.min" [orientation]="state.model.orientation" [range]="state.model.range" [step]="state.model.step" [onChangeMethodID]="callback.getHandler(state,'onChangeMethodID')" [onCreateMethodID]="callback.getHandler(state,'onCreateMethodID')" [onSlideMethodID]="callback.getHandler(state,'onSlideMethodID')" [onStartMethodID]="callback.getHandler(state,'onStartMethodID')" [onStopMethodID]="callback.getHandler(state,'onStopMethodID')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-slider>
        }</ng-template>
        <!-- component template generate end -->
      `
    /* eslint-enable max-len */
    ,
    standalone: false
})

/**
 * A stripped, stateless copy of FormComponent used to render the real runtime DOM of a form
 * without any data, handlers or server communication. See SVY-21460.
 */
export class FormTemplateComponent extends AbstractFormComponent implements OnChanges, IFormComponent {
    readonly svyResponsiveDiv = viewChild<TemplateRef<any>>('svyResponsiveDiv');
    readonly cssPositionContainer = viewChild<TemplateRef<any>>('cssPositionContainer');
    // structure viewchild template generate start
    // structure viewchild template generate end
    readonly formComponentAbsoluteDiv = viewChild<TemplateRef<any>>('formComponentAbsoluteDiv');
    readonly formComponentResponsiveDiv = viewChild<TemplateRef<any>>('formComponentResponsiveDiv');

    // component viewchild template generate start
    readonly servoycoreSlider = viewChild<TemplateRef<any>>('servoycoreSlider');
    readonly servoycoreErrorbean = viewChild<TemplateRef<any>>('servoycoreErrorbean');
    readonly servoycoreListformcomponent = viewChild<TemplateRef<any>>('servoycoreListformcomponent');
    readonly servoycoreFormcontainer = viewChild<TemplateRef<any>>('servoycoreFormcontainer');
    // component viewchild template generate end

    @Input() name: string;

    formClasses: string[];
    formCache: FormCache;

    absolutFormPosition = {};

    private servoyApiCache: { [property: string]: ServoyApi } = {};
    private log: LoggerService;

    constructor(private formservice: FormService,
        private servoyService: ServoyService, logFactory: LoggerFactory,
        private changeHandler: ChangeDetectorRef,
        private el: ElementRef<Element>, protected renderer: Renderer2,
        @Inject(DOCUMENT) private document: Document) {
        super(renderer);
        this.log = logFactory.getLogger('FormTemplateComponent');
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

    ngOnChanges(changes: SimpleChanges) {
        if (changes.name) {
            this.formCache = this.formservice.getFormCache(this);
            const styleClasses: string = this.formCache.getComponent('').model.styleClass as string;
            if (styleClasses)
                this.formClasses = styleClasses.split(' ');
            else
                this.formClasses = null;
            this._containers = this.formCache.getComponent('').model.containers;
            this._cssstyles = this.formCache.getComponent('').model.cssstyles;
            this.servoyApiCache = {};
            this.componentCache = {};

            this.renderer.setAttribute(this.el.nativeElement, 'name', this.name);
        }
    }

    getTemplate(item: StructureCache | ComponentCache | FormComponentCache): TemplateRef<any> {
        if (item instanceof StructureCache) {
            return item.tagname ? this[item.tagname]() : (item.cssPositionContainer ? this.cssPositionContainer() : this.svyResponsiveDiv());
        } else if (item instanceof FormComponentCache) {
            if (item.hasFoundset) return this.servoycoreListformcomponent();
            return item.responsive ? this.formComponentResponsiveDiv() : this.formComponentAbsoluteDiv();
        } else {
            const componentRef = this[item.type];
            if (componentRef === undefined && item.type !== undefined) {
                this.log.error(this.log.buildMessage(() => ('Template for ' + item.type + ' was not found, please check formtemplate_component template.')));
            }
            return typeof componentRef === 'function' ? componentRef() : componentRef;
        }
    }

    getTemplateForLFC(state: ComponentCache): TemplateRef<any> {
        if (state.type.includes('formcomponent')) {
            return state.model.containedForm.absoluteLayout ? this.formComponentAbsoluteDiv() : this.formComponentResponsiveDiv();
        } else {
            let compDirectiveName = state.type;
            const index = compDirectiveName.indexOf('-');
            compDirectiveName = compDirectiveName.replace('-', '');
            return this[compDirectiveName.substring(0, index) + compDirectiveName.charAt(index).toUpperCase() + compDirectiveName.substring(index + 1)]();
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
        return this.absolutFormPosition;
    }

    public isFormAvailable(name: string): boolean {
        return this.formservice.hasFormCacheEntry(name);
    }

    datachange(_component: ComponentCache, _property: string, _value: any, _dataprovider: boolean) {
        // stateless render: no data is pushed back
    }

    getHandler(_item: ComponentCache, _handler: string) {
        return null;
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
            api = new FormTemplateServoyApi(item, this.name, this.formCache.absolute, this.formservice, this.servoyService);
            this.servoyApiCache[item.name] = api;
        }
        return api;
    }

    public callApi(_componentName: string, _apiName: string, _args: any, _path?: string[]): any {
        return null;
    }

    getContainerByName(containername: string): Element {
        return this.document.querySelector('[name="' + this.name + '.' + containername + '"]');
    }

    public updateFormStyleClasses(_ngutilsstyleclasses: string): void {
        // stateless render: no ngutils style classes
    }
}

class FormTemplateServoyApi extends ServoyApi {
    constructor(item: ComponentCache,
        formname: string,
        absolute: boolean,
        formservice: FormService,
        servoyService: ServoyService) {
        super(item, formname, absolute, formservice, servoyService, true);
    }

    public formWillShow(_formname: string, _relationname?: string, _formIndex?: number): Promise<boolean> {
        return new Promise<any>(resolve => {
            resolve(true);
        });
    }

    public hideForm(_formname: string, _relationname?: string, _formIndex?: number,
        _formNameThatWillShow?: string, _relationnameThatWillBeShown?: string, _formIndexThatWillBeShown?: number): Promise<boolean> {
        return new Promise<any>(resolve => {
            resolve(true);
        });
    }

    public apply(_propertyName: string, _value: any) {
        // stateless render: no apply to server
    }
}
