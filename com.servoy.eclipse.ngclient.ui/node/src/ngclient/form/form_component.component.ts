import {
  Component, Input, OnDestroy, OnChanges, SimpleChanges,
  TemplateRef, ElementRef, Renderer2, ChangeDetectionStrategy, ChangeDetectorRef, Inject, AfterViewInit, AfterViewChecked,
  DOCUMENT,
  input,
  viewChild,
  signal,
  forwardRef
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { FormCache, StructureCache, FormComponentCache, ComponentCache, instanceOfApiExecutor, IFormComponent } from '../types';

import { ServoyService } from '../servoy.service';

import { SabloService } from '../../sablo/sablo.service';
import { LoggerService, LoggerFactory, ServoyBaseComponent, WindowRefService } from '@servoy/public';

import { ServoyApi } from '../servoy_api';
import { FormService } from '../form.service';

import { ConverterService } from '../../sablo/converter.service';
import { IWebObjectSpecification } from '../../sablo/types_registry';
import { fromEvent, debounceTime } from 'rxjs';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AutosaveDirective } from '@servoy/public';
import { AllComponentsModule } from '../allcomponents.module';
import { AllServicesModules } from '../allservices.service';
import { SERVOYCORE_COMPONENTS } from '../../servoycore/servoycore.components';
import { ListFormComponent } from '../../servoycore/listformcomponent/listformcomponent';
import { AbstractFormComponent } from './abstract_form_component.component';

export { AbstractFormComponent } from './abstract_form_component.component';

@Component({
     
    selector: 'svy-form',
    changeDetection: ChangeDetectionStrategy.OnPush,
    /* eslint-disable max-len */
    template: `
      @if (formCache.absolute) {
        <div [ngStyle]="getAbsoluteFormStyle()" class="svy-form" [ngClass]="formClasses()" svyAutosave> <!-- main div -->
          @for (part of formCache.parts; track part.rId) {
            <div [svyContainerStyle]="part" [svyContainerLayout]="part.layout" [svyContainerClasses]="part.classes"> <!-- part div -->
              @for (item of part.items; track item.rId) {
                <div [svyContainerStyle]="item" [svyContainerLayout]="item.layout" class="svy-wrapper" [ngStyle]="item.model.visible === false ? {'display': 'none'} : null" style="position:absolute"> <!-- wrapper div -->
                  <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state:item, callback:this }"></ng-template>  <!-- component or formcomponent -->
                </div>
              }
            </div>
          }
        </div>
      }
      @if (!formCache.absolute&&formCache.mainStructure) {
        <div class="svy-form svy-respform" [ngClass]="formClasses()"> <!-- main container div -->
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
              <div [svyContainerStyle]="item" [svyContainerLayout]="item.layout" class="svy-wrapper" [ngStyle]="item.model.visible === false ? {'display': 'none'} : null" style="position:absolute"> <!-- wrapper div -->
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
                <div [svyContainerStyle]="item" [svyContainerLayout]="item.layout" class="svy-wrapper" [ngStyle]="item.model.visible === false ? {'display': 'none'} : null" style="position:absolute"> <!-- wrapper div -->
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
        <ng-template #servoycoreListformcomponent let-callback="callback" let-state="state">@if (state.model.visible) {
          <servoycore-listformcomponent  [containedForm]="state.model.containedForm" [foundset]="state.model.foundset" [pageLayout]="state.model.pageLayout" [readOnly]="state.model.readOnly" [responsivePageSize]="state.model.responsivePageSize" [rowStyleClass]="state.model.rowStyleClass" [rowStyleClassDataprovider]="state.model.rowStyleClassDataprovider" [selectionClass]="state.model.selectionClass" [styleClass]="state.model.styleClass" [tabSeq]="state.model.tabSeq" [onSelectionChanged]="callback.getHandler(state,'onSelectionChanged')" [servoyApi]="callback.getServoyApi(state)" [name]="state.name" #cmp></servoycore-listformcomponent>
        }</ng-template>
        <!-- component template generate end -->
      `
    /* eslint-enable max-len */
    ,
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        AutosaveDirective,
        AllComponentsModule,
        AllServicesModules,
        ...SERVOYCORE_COMPONENTS,
        ListFormComponent
    ],
    providers: [{ provide: AbstractFormComponent, useExisting: forwardRef(() => FormComponent) }]
})

/**
 * This is the definition of a angular component that represents servoy forms.
 */
export class FormComponent extends AbstractFormComponent implements OnDestroy, OnChanges, AfterViewInit, AfterViewChecked, IFormComponent {
    [key: string]: any;
    readonly svyResponsiveDiv = viewChild<TemplateRef<any>>('svyResponsiveDiv');
    readonly cssPositionContainer = viewChild<TemplateRef<any>>('cssPositionContainer');
    // structure viewchild template generate start
    // structure viewchild template generate end
    readonly formComponentAbsoluteDiv = viewChild<TemplateRef<any>>('formComponentAbsoluteDiv');
    readonly formComponentResponsiveDiv = viewChild<TemplateRef<any>>('formComponentResponsiveDiv');

    // component viewchild template generate start
    readonly servoycoreListformcomponent = viewChild<TemplateRef<any>>('servoycoreListformcomponent');
    // component viewchild template generate end

    @Input() name!: string;

    //** "injectedComponentRefs" is used for being able to inject some test component templates inside Karma/Jasmine unit tests */
    readonly injectedComponentRefs = input<Record<string, TemplateRef<any>> | undefined>(undefined);

    formClasses = signal<string[] | null>(undefined!);

    formCache!: FormCache;

    absolutFormPosition: Record<string, any> = {};
    detectingChanges = false;

    private handlerCache: Record<string, Record<string, (event: Event) => void>> = {};
    private servoyApiCache: Record<string, ServoyApi> = {};
    private log: LoggerService;

    constructor(private formservice: FormService, private sabloService: SabloService,
        private servoyService: ServoyService, logFactory: LoggerFactory,
        private changeHandler: ChangeDetectorRef,
        private el: ElementRef<Element>, protected renderer: Renderer2,
        private converterService: ConverterService<unknown>,
        @Inject(DOCUMENT) private document: Document,
        private windowRefService: WindowRefService) {
        super(renderer);
        this.log = logFactory.getLogger('FormComponent');
        const resizeObservable$ = fromEvent(this.windowRefService.nativeWindow, 'resize')
        resizeObservable$.pipe(debounceTime(500), takeUntilDestroyed()).subscribe(evt => {
            this.onResize()
        });
    }

    public static doCallApiOnComponent(comp: ServoyBaseComponent<any>, componentSpec: IWebObjectSpecification, apiName: string, args: any[],
        converterService: ConverterService<unknown>, log: LoggerService, compName: string): Promise<any> {
        return AbstractFormComponent.doCallApiOnComponent(comp, componentSpec, apiName, args, converterService, log, compName);
    }

    public detectChanges() {
        const oldDetect = this.detectingChanges;
        try {
            if (this.detectingChanges) {
                this.log.warn('Nested detectChanges call in form: ' + this.name);
            }
            this.detectingChanges = true;
            this.changeHandler.detectChanges();
        } finally {
            this.detectingChanges = oldDetect;
        }
    }

    public formCacheChanged(cache: FormCache): void {
        this.formCache = cache;
        this.detectChanges();
    }

    public getFormCache(): FormCache {
        return this.formCache;
    }

    ngOnChanges(changes: SimpleChanges) {
        const name = this.name;
        if (changes.name) {
            // Form Instances can be reused for tabpanels that have a template reference to this (depending on what the component does)
            // they are reused in main form switches
            // make sure to clean up the old reference/name to this instance and mark the previous form as destroyed
            if (changes.name.previousValue) this.formservice.destroy(changes.name.previousValue, false); // TODO should we do something so that ngOnDestroy of child components still gets called before the uiDestroyed() or model properties? see what formservice.destroy does
            // really make sure all form state is reverted to default for this new name
            this.formCache = this.formservice.getFormCache(this);
            const styleClasses: string = this.formCache.getComponent('')!.model.styleClass as string;
            if (styleClasses)
                this.formClasses.set(styleClasses.split(' '));
            else
                this.formClasses.set(null);
            this._containers = this.formCache.getComponent('')!.model.containers!;
            this._cssstyles = this.formCache.getComponent('')!.model.cssstyles!;
            this.handlerCache = {};
            this.servoyApiCache = {};
            this.componentCache = {};

            this.renderer.setAttribute(this.el.nativeElement, 'name', name);
        }
        this.updateFormStyleClasses(this.formservice.getFormStyleClasses(name));
    }

    ngAfterViewInit() {
        this.formservice.resolveComponentCache(this);
        this.onResize();
    }
    
    ngAfterViewChecked() {
        this.formservice.resolveComponentCache(this);
    }

    ngOnDestroy() {
        this.formservice.destroy(this.name, true);
    }

    getTemplate(item: StructureCache | ComponentCache | FormComponentCache): TemplateRef<any> {
        if (item instanceof StructureCache) {
            return item.tagname ? this[item.tagname]() : (item.cssPositionContainer ? this.cssPositionContainer()! : this.svyResponsiveDiv()!);
        } else if (item instanceof FormComponentCache) {
            if (item.hasFoundset) return this.servoycoreListformcomponent()!;
            return item.responsive ? this.formComponentResponsiveDiv()! : this.formComponentAbsoluteDiv()!;
        } else {
            let componentRef = this[item.type];

            // "injectedComponentRefs" is used only for being able to inject some TEST component templates inside Karma/Jasmine unit tests
            const injectedComponentRefs = this.injectedComponentRefs();
            if (!componentRef && injectedComponentRefs) componentRef = injectedComponentRefs[item.type];

            if (componentRef === undefined && item.type !== undefined) {
                this.log.error(this.log.buildMessage(() => ('Template for ' + item.type + ' was not found, please check form_component template.')));
            }
            return typeof componentRef === 'function' ? componentRef() : componentRef;
        }
    }

    getTemplateForLFC(state: ComponentCache): TemplateRef<any> {
        if (state.type.includes('formcomponent')) {
            return state.model.containedForm!.absoluteLayout ? this.formComponentAbsoluteDiv()! : this.formComponentResponsiveDiv()!;
        } else {
            // TODO: this has to be replaced with a type property on the state object
            // TODO - hmm type is already camel case here with dashes removed normally - so I don't think we need the indexOf, replace etc anymore
            let compDirectiveName = state.type;
            const index = compDirectiveName.indexOf('-');
            compDirectiveName = compDirectiveName.replace('-', '');
            return this[compDirectiveName.substring(0, index) + compDirectiveName.charAt(index).toUpperCase() + compDirectiveName.substring(index + 1)]();
        }
    }

    public getAbsoluteFormStyle() {
        const formData = this.formCache.getComponent('')!;

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
            const borderStyle: Record<string, any> = formData.model.borderType;
            for (const key of Object.keys(borderStyle)) {
                this.absolutFormPosition[key] = borderStyle[key];
            }
        }
        if (formData.model.transparent) {
            this.absolutFormPosition['backgroundColor'] = 'transparent';
        }

        // add a min size if needed and if this is not the main form to avoid scrollbars there.
        if (this.el.nativeElement.parentElement?.closest('svy-form') != null) {
            // see svyFormstyle from ng1
            if (formData.model.useMinWidth) this.absolutFormPosition['minWidth'] = this.formCache.size.width + 'px';
            if (formData.model.useMinHeight) this.absolutFormPosition['minHeight'] = this.formCache.size.height + 'px';
        }
        return this.absolutFormPosition;
    }

    public isFormAvailable(name: string): boolean {
        // console.log("isFormAvailable: " + name + " " +  this.formservice.hasFormCacheEntry( name));
        return this.formservice.hasFormCacheEntry(name);
    }

    datachange(component: ComponentCache, property: string, value: any, dataprovider: boolean) {
        const model = this.formCache.getComponent(component.name)!.model;
        const oldValue = model[property];
        model[property] = value;
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
            func = function(event: any) {
                if (event && event.preventDefault instanceof Function) event.preventDefault();
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

    public callApi(componentName: string, apiName: string, args: any[], path?: string[]): any {
        if (path && path.length > 0) {
            // an api call to a component nested inside a list form component like component (so with nested fs linked 'component' typed properties)?
            const comp = this.componentCache[path[0]]; // first thing in path is always component name I think
            if (instanceOfApiExecutor(comp)) {
                return comp.callApi(path[1], apiName, args, path.slice(2));
            } else {
                this.log.error('trying to call api: ' + apiName + ' on component: ' + componentName + ' with path: ' + path +
                    ', but comp: ' + (comp == null ? ' is not found' : comp.name + ' doesnt implement IApiExecutor'));
            }
            return null;
        } else {
            return FormComponent.doCallApiOnComponent(this.componentCache[componentName], this.formCache.getComponentSpecification(componentName)!,
                apiName, args, this.converterService, this.log, componentName);
        }
    }

    getContainerByName(containername: string): Element {
        return this.document.querySelector('[name="' + this.name + '.' + containername + '"]')!;
    }

    public updateFormStyleClasses(ngutilsstyleclasses: string): void {
        const styleClasses: string | undefined = this.formCache.getComponent('')!.model.styleClass;
        if (styleClasses)
            this.formClasses.set(styleClasses.split(' '));
        else
            this.formClasses.set([]);
        if (ngutilsstyleclasses) {
            this.formClasses.set(this.formClasses()!.concat(ngutilsstyleclasses.split(' ')));
        }
    }

    private onResize(): void {
        const formElement = this.el.nativeElement.querySelector('.svy-form');
        if (formElement) {
            const formSize = formElement.getBoundingClientRect();
            const value = { width: formSize.width, height: formSize.height }
            const oldValue = this.formCache.getComponent('')!.model['size'] as any;
            if (oldValue?.width != value.width || oldValue?.height != value.height) {
				if (this.formservice.getFormCache(this)){
					this.formservice.sendChanges(this.name, '', 'size', value, oldValue, false);
				}else{
					this.log.warn('onResize called on form visible in browser but missing from cache: ' + this.name);
				}
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
