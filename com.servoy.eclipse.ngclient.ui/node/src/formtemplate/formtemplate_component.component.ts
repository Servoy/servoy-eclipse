import {
  Component,
  Input,
  OnChanges,
  SimpleChanges,
  viewChild,
  TemplateRef,
  ElementRef,
  Renderer2,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  ViewEncapsulation,
  DOCUMENT,
  forwardRef,
  inject,
  input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { FormCache, StructureCache, FormComponentCache, ComponentCache, IFormComponent } from '../ngclient/types';

import { ServoyService } from '../ngclient/servoy.service';

import { LoggerService, LoggerFactory, ServoyBaseComponent } from '@servoy/public';

import { ServoyApi } from '../ngclient/servoy_api';
import { FormService } from '../ngclient/form.service';

import { AbstractFormComponent } from '../ngclient/form/abstract_form_component.component';
import { AddAttributeDirective } from '../servoycore/addattribute.directive';
import { AllComponentsModule } from '../ngclient/allcomponents.module';
import { AllServicesModules } from '../ngclient/allservices.service';
import { SERVOYCORE_COMPONENTS } from '../servoycore/servoycore.components';
import { ServoyCoreFormcomponentResponsiveCotainer } from '../servoycore/formcomponent-responsive-container/formcomponent-responsive-container';
import { ListFormComponent } from '../servoycore/listformcomponent/listformcomponent';

@Component({
  selector: 'svy-formtemplate',
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  template: `
    @if (formCache.absolute) {
      <div [ngStyle]="getAbsoluteFormStyle()" class="svy-form" [ngClass]="formClasses">
        <!-- main div -->
        @for (part of formCache.parts; track part.rId) {
          <div [svyContainerStyle]="part" [svyContainerLayout]="part.layout" [svyContainerClasses]="part.classes">
            <!-- part div -->
            @for (item of part.items; track item.rId) {
              <div [svyContainerStyle]="item" [svyContainerLayout]="item.layout" class="svy-wrapper" [ngStyle]="item.model.visible === false ? { display: 'none' } : null" style="position:absolute">
                <!-- wrapper div -->
                <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state: item, callback: this }"></ng-template>
                <!-- component or formcomponent -->
              </div>
            }
          </div>
        }
      </div>
    }
    @if (!formCache.absolute && formCache.mainStructure) {
      <div class="svy-form svy-respform" [ngClass]="formClasses">
        <!-- main container div -->
        @for (item of formCache.mainStructure.items; track item.rId) {
          <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state: item, callback: this }"></ng-template>
        }
        <!-- component or responsive div  -->
      </div>
    }

    <ng-template #svyResponsiveDiv let-state="state">
      <div [svyContainerStyle]="state" [svyContainerClasses]="state.classes" [svyContainerAttributes]="state.attributes" class="svy-layoutcontainer">
        @for (item of state.items; track item.rId) {
          <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state: item, callback: this }"></ng-template>
        }
      </div>
    </ng-template>

    <ng-template #cssPositionContainer let-state="state">
      <div [svyContainerStyle]="state" [svyContainerClasses]="state.classes" [svyContainerAttributes]="state.attributes" class="svy-layoutcontainer">
        @for (item of state.items; track item.rId) {
          <div [svyContainerStyle]="item" [svyContainerLayout]="item.layout" class="svy-wrapper" [ngStyle]="item.model.visible === false ? { display: 'none' } : null" style="position:absolute">
            <!-- wrapper div -->
            <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state: item, callback: this }"></ng-template>
          </div>
        }
      </div>
    </ng-template>

    <!-- structure template generate start -->
    <!-- structure template generate end -->
    <ng-template #formComponentAbsoluteDiv let-state="state">
      @if (state.model.visible) {
        <div
          [svyContainerStyle]="state.formComponentProperties"
          [svyContainerLayout]="state.formComponentProperties.layout"
          [svyContainerClasses]="state.formComponentProperties.classes"
          [svyContainerAttributes]="state.formComponentProperties.attributes"
          style="position:relative"
          class="svy-formcomponent"
        >
          @for (item of state.items; track item.rId) {
            <div [svyContainerStyle]="item" [svyContainerLayout]="item.layout" class="svy-wrapper" [ngStyle]="item.model.visible === false ? { display: 'none' } : null" style="position:absolute">
              <!-- wrapper div -->
              <ng-template [ngTemplateOutlet]="getTemplate(item)" [ngTemplateOutletContext]="{ state: item, callback: this }"></ng-template>
              <!-- component  -->
            </div>
          }
        </div>
      }
    </ng-template>
    <ng-template #formComponentResponsiveDiv let-state="state">
      @if (state.model.visible) {
        <servoycore-formcomponent-responsive-container [items]="state.items" [class]="state.model.styleClass" [formComponent]="this"></servoycore-formcomponent-responsive-container>
      }
    </ng-template>
    <!-- component template generate start -->
    <!-- component template generate end -->
  `,
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    AddAttributeDirective,
    AllComponentsModule,
    AllServicesModules,
    ...SERVOYCORE_COMPONENTS,
    ServoyCoreFormcomponentResponsiveCotainer,
    ListFormComponent,
  ],
  providers: [{ provide: AbstractFormComponent, useExisting: forwardRef(() => FormTemplateComponent) }],
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
  // component viewchild template generate end

  @Input() name!: string;

  //** "injectedComponentRefs" is used only for being able to inject some test component templates inside unit tests */
  readonly injectedComponentRefs = input<Record<string, TemplateRef<any>> | undefined>(undefined);

  formClasses: string[] | null = null;
  formCache!: FormCache;

  absolutFormPosition: Record<string, any> = {};

  private servoyApiCache: Record<string, ServoyApi> = {};
  private log: LoggerService;

  private formservice = inject(FormService);
  private servoyService = inject(ServoyService);
  private changeHandler = inject(ChangeDetectorRef);
  private el = inject(ElementRef<Element>);
  private document = inject(DOCUMENT) as Document;

  constructor() {
    super(inject(Renderer2));
    this.log = inject(LoggerFactory).getLogger('FormTemplateComponent');
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
      const styleClasses: string = this.formCache.getComponent('')!.model.styleClass as string;
      if (styleClasses) this.formClasses = styleClasses.split(' ');
      else this.formClasses = null;
      this._containers = this.formCache.getComponent('')!.model.containers!;
      this._cssstyles = this.formCache.getComponent('')!.model.cssstyles!;
      this.servoyApiCache = {};
      this.componentCache = {};

      this.renderer.setAttribute(this.el.nativeElement, 'name', this.name);
    }
  }

  getTemplate(item: StructureCache | ComponentCache | FormComponentCache): TemplateRef<any> {
    if (item instanceof StructureCache) {
      return item.tagname ? (this as any)[item.tagname]() : item.cssPositionContainer ? this.cssPositionContainer()! : this.svyResponsiveDiv()!;
    } else if (item instanceof FormComponentCache) {
      if (item.hasFoundset) return (this as any).servoycoreListformcomponent();
      return item.responsive ? this.formComponentResponsiveDiv()! : this.formComponentAbsoluteDiv()!;
    } else {
      let componentRef = (this as any)[item.type];

      // "injectedComponentRefs" is used only for being able to inject some TEST component templates inside unit tests
      const injectedComponentRefs = this.injectedComponentRefs();
      if (!componentRef && injectedComponentRefs) componentRef = injectedComponentRefs[item.type];

      if (componentRef === undefined && item.type !== undefined) {
        this.log.error(this.log.buildMessage(() => 'Template for ' + item.type + ' was not found, please check formtemplate_component template.'));
      }
      return typeof componentRef === 'function' ? componentRef() : componentRef;
    }
  }

  getTemplateForLFC(state: ComponentCache): TemplateRef<any> {
    if (state.type.includes('formcomponent')) {
      return state.model.containedForm!.absoluteLayout ? this.formComponentAbsoluteDiv()! : this.formComponentResponsiveDiv()!;
    } else {
      let compDirectiveName = state.type;
      const index = compDirectiveName.indexOf('-');
      compDirectiveName = compDirectiveName.replace('-', '');
      return (this as any)[compDirectiveName.substring(0, index) + compDirectiveName.charAt(index).toUpperCase() + compDirectiveName.substring(index + 1)]();
    }
  }

  public getAbsoluteFormStyle() {
    const formData = this.formCache.getComponent('')!;

    for (const key in this.absolutFormPosition) {
      if (Object.prototype.hasOwnProperty.call(this.absolutFormPosition, key)) {
        delete this.absolutFormPosition[key];
      }
    }
    this.absolutFormPosition['left'] = '0px';
    this.absolutFormPosition['top'] = '0px';
    this.absolutFormPosition['right'] = '0px';
    this.absolutFormPosition['bottom'] = '0px';
    this.absolutFormPosition['position'] = 'absolute';

    if (formData.model.borderType) {
      const borderStyle: Record<string, any> = formData.model.borderType as Record<string, any>;
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

  getHandler(_item: ComponentCache, _handler: string): any {
    return null;
  }

  registerComponent(component: ServoyBaseComponent<any>): void {
    this.componentCache[component.name()] = component;
  }

  unRegisterComponent(component: ServoyBaseComponent<any>): void {
    delete this.componentCache[component.name()];
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
    return this.document.querySelector('[name="' + this.name + '.' + containername + '"]')!;
  }

  public updateFormStyleClasses(_ngutilsstyleclasses: string): void {
    // stateless render: no ngutils style classes
  }
}

class FormTemplateServoyApi extends ServoyApi {
  constructor(item: ComponentCache, formname: string, absolute: boolean, formservice: FormService, servoyService: ServoyService) {
    super(item, formname, absolute, formservice, servoyService, true);
  }

  public override formWillShow(_formname: string, _relationname?: string, _formIndex?: number): Promise<boolean> {
    return new Promise<any>((resolve) => {
      resolve(true);
    });
  }

  public override hideForm(
    _formname: string,
    _relationname?: string,
    _formIndex?: number,
    _formNameThatWillShow?: string,
    _relationnameThatWillBeShown?: string,
    _formIndexThatWillBeShown?: number,
  ): Promise<boolean> {
    return new Promise<any>((resolve) => {
      resolve(true);
    });
  }

  public override apply(_propertyName: string, _value: any) {
    // stateless render: no apply to server
  }
}
