import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { CUSTOM_ELEMENTS_SCHEMA, Component, Renderer2, ChangeDetectorRef, DOCUMENT, Injector, runInInjectionContext } from '@angular/core';

import { FormattingService, TooltipService, LoggerFactory, ServoyPublicModule,
            WindowRefService, SpecTypesService } from '@servoy/public';

import { FormTemplateComponent } from './formtemplate_component.component';

import { FormService } from '../ngclient/form.service';
import { ServoyService } from '../ngclient/servoy.service';
import { CustomArrayTypeFactory } from '../ngclient/converters/json_array_converter';
import { CustomObjectTypeFactory } from '../ngclient/converters/json_object_converter';
import { SabloService } from '../sablo/sablo.service';

import { ErrorBean } from '../servoycore/error-bean/error-bean';
import { ServoyCoreSlider } from '../servoycore/slider/slider';

import { ConverterService } from '../sablo/converter.service';
import { WebsocketService } from '../sablo/websocket.service';
import { TypesRegistry } from '../sablo/types_registry';
import { DateType } from '../sablo/converters/date_converter';

import { ServoyTestingModule } from '../testing/servoytesting.module';
import { PopupFormService } from '../ngclient/services/popupform.service';
import { AddAttributeDirective } from '../servoycore/addattribute.directive';

import { ClientFunctionService } from '../sablo/clientfunction.service';
import { ObjectType } from '../sablo/converters/object_converter';
import { LocaleService } from '../ngclient/locale.service';
import { I18NProvider } from '../ngclient/services/i18n_provider.service';
import { ServoyApi } from '../ngclient/servoy_api';
import { ComponentCache } from '../ngclient/types';
import { ListFormComponent } from '../servoycore/listformcomponent/listformcomponent';
import { AbstractFormComponent } from '../ngclient/form/form_component.component';

import { By } from '@angular/platform-browser';

/**
 * Tests for the stateless svy-formtemplate render component (SVY-21460).
 *
 * The suite renders the real <svy-formtemplate> through a host component (exactly the way
 * form_component.component.spec.ts renders <svy-form>), building the FormCache through the
 * production FormService.createFormCache/walkOverChildren + ConverterService + TypesRegistry
 * path. Rendering exercises AddAttributeDirective (activated by the [svyContainerStyle] wrapper
 * divs), which is precisely the NG0201-regression surface this feature had to fix.
 *
 * The assertions catch real regressions against spec §5:
 *  - the render component instance MUST be reachable inside the component tree (NG0201 guard);
 *  - getHandler / callApi return null (no events, no api calls wired);
 *  - datachange / updateFormStyleClasses are inert no-ops that never call the server;
 *  - building + rendering the form never calls SabloService.callService (no server communication);
 *  - the injected specs drive typed-property conversion (a Date);
 *  - the rendered DOM is the runtime .svy-form markup with NO designer-only artifacts;
 *  - getServoyApi returns a design-time api whose server-affecting methods are neutralised.
 *
 * NOTE: the component is created through TestBed (never `new FormTemplateComponent(...)`), because
 * its viewChild() field initializers require an Angular injection context (NG0203 otherwise).
 */
@Component({
    // eslint-disable-next-line @stylistic/ts/quotes
    template: `<svy-formtemplate [name]="'aForm'"></svy-formtemplate>`,
    standalone: false
})
class TestHostComponent { }

describe('FormTemplateComponent (svy-formtemplate render copy)', () => {
    let sabloService: jasmine.SpyObj<SabloService>;
    let servoyService: jasmine.SpyObj<ServoyService>;
    let websocketService: jasmine.SpyObj<WebsocketService>;
    let converterService: ConverterService<unknown>;
    let logFactory: LoggerFactory;
    let typesRegistry: TypesRegistry;
    let specTypesService: SpecTypesService;
    let formService: FormService;

    const buildFormState = (extraModel: Record<string, unknown> = {}) => ({
        responsive: false,
        size: { width: 543, height: 368 },
        children: [
            {
                name: '',
                model: {
                    visible: true,
                    enabled: true,
                    size: { width: 543, height: 368 },
                    designSize: { width: 543, height: 368 },
                    absoluteLayout: { '': true }
                }
            },
            {
                part: true,
                classes: ['svy-body'],
                layout: { position: 'absolute', left: '0px', right: '0px', top: '0px', bottom: '0px' },
                children: [
                    {
                        name: 'myErrorBean',
                        specName: 'servoycore-errorbean',
                        elType: 'servoycore-errorbean',
                        model: {
                            visible: true,
                            error: 'hello',
                            svyMarkupId: 'markup_error_1',
                            cssPosition: { position: 'absolute', top: '10px', left: '10px', height: '30px', width: '200px' },
                            aTypedDate: '2020-05-01T00:00:00',
                            servoyAttributes: {},
                            ...extraModel
                        },
                        position: { left: '10px', top: '10px', width: '200px', height: '30px' }
                    }
                ]
            }
        ]
    });

    // register the errorbean spec so createFormCache can convert its typed 'aTypedDate' property
    const registerErrorbeanSpecs = () => {
        typesRegistry.addComponentClientSideSpecs({
            'servoycore-errorbean': {
                p: {
                    aTypedDate: DateType.TYPE_NAME_SABLO
                }
            }
        } as any);
    };

    // render <svy-formtemplate> through the host and return both the fixture and the render component instance
    const renderForm = (): { fixture: ComponentFixture<TestHostComponent>; comp: FormTemplateComponent } => {
        const fixture = TestBed.createComponent(TestHostComponent);
        fixture.detectChanges();
        const debugComp = fixture.debugElement.query(By.directive(FormTemplateComponent));
        return { fixture, comp: debugComp.componentInstance as FormTemplateComponent };
    };

    beforeEach(waitForAsync(() => {
        sabloService = jasmine.createSpyObj('SabloService', ['callService']);
        servoyService = jasmine.createSpyObj('ServoyService', ['connect', 'getUIProperties']);
        servoyService.getUIProperties.and.returnValue({ getUIProperty: () => null } as any);
        websocketService = jasmine.createSpyObj('WebsocketService', {
            getSession: { then: () => Promise.resolve() }
        });

        TestBed.configureTestingModule({
            declarations: [TestHostComponent, FormTemplateComponent, AddAttributeDirective, ServoyCoreSlider, ErrorBean],
            imports: [ServoyTestingModule, ServoyPublicModule],
            providers: [
                { provide: SabloService, useValue: sabloService },
                { provide: ServoyService, useValue: servoyService },
                { provide: WebsocketService, useValue: websocketService },
                ConverterService,
                TypesRegistry,
                I18NProvider,
                FormattingService,
                TooltipService,
                LocaleService,
                WindowRefService,
                LoggerFactory,
                ClientFunctionService,
                FormService,
                PopupFormService,
                SpecTypesService
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA]
        }).compileComponents();

        formService = TestBed.inject(FormService);
        typesRegistry = TestBed.inject(TypesRegistry);
        logFactory = TestBed.inject(LoggerFactory);
        converterService = TestBed.inject(ConverterService);
        specTypesService = TestBed.inject(SpecTypesService);

        // register the global type factories that ServoyService normally registers locally (no server call)
        typesRegistry.registerGlobalType(ObjectType.TYPE_NAME, new ObjectType(typesRegistry, converterService, logFactory));
        typesRegistry.registerGlobalType(DateType.TYPE_NAME_SABLO, new DateType());
        typesRegistry.getTypeFactoryRegistry().contributeTypeFactory('JSON_arr', new CustomArrayTypeFactory(typesRegistry, converterService, logFactory));
        typesRegistry.getTypeFactoryRegistry().contributeTypeFactory('JSON_obj', new CustomObjectTypeFactory(typesRegistry, converterService, specTypesService, logFactory));
    }));

    describe('typed-property conversion (specs registered first)', () => {

        it('converts a typed Date property into a real Date instance when the spec was registered', () => {
            registerErrorbeanSpecs();
            formService.createFormCache('aForm', buildFormState(), null);

            const model = formService.getFormCacheByName('aForm').getComponent('myErrorBean').model;
            expect(model.aTypedDate instanceof Date).toBe(true);
            expect((model.aTypedDate as Date).getFullYear()).toBe(2020);
        });

        it('leaves the typed value unconverted (not a Date) when its spec was NOT registered - proving the specs drive conversion (gap 2.3)', () => {
            // deliberately skip registerErrorbeanSpecs(): mirrors the empty-registry gap the route root must avoid
            formService.createFormCache('aForm', buildFormState(), null);

            const model = formService.getFormCacheByName('aForm').getComponent('myErrorBean').model;
            expect(model.aTypedDate instanceof Date).toBe(false);
        });
    });

    describe('NG0201 regression guard (AddAttributeDirective parent resolution)', () => {

        beforeEach(() => {
            registerErrorbeanSpecs();
            formService.createFormCache('aForm', buildFormState(), null);
        });

        it('renders svy-formtemplate (with [svyContainerStyle] wrappers) WITHOUT throwing NG0201', () => {
            // AddAttributeDirective must resolve its parent as FormTemplateComponent; if the fix is reverted this throws
            // NG0201 "No provider for FormComponent". A meaningful guard: detectChanges must not throw.
            expect(() => renderForm()).not.toThrow();
        });

        it('resolves AddAttributeDirective.parent to the FormTemplateComponent instance', () => {
            const { fixture, comp } = renderForm();
            const directives = fixture.debugElement.queryAll(By.directive(AddAttributeDirective));
            expect(directives.length).toBeGreaterThan(0);
            directives.forEach(de => {
                const dir = de.injector.get(AddAttributeDirective);
                expect(dir.parent).toBe(comp);
            });
        });
    });

    describe('NG0201 regression guard (ListFormComponent parent resolution, SVY-21460)', () => {

        // A servoycore-listformcomponent rendered through the svy-formtemplate host resolves its
        // parent AbstractFormComponent via the SAME third-host injector branch as AddAttributeDirective:
        // FormComponent -> DesignFormComponent -> FormTemplateComponent (see listformcomponent.ts
        // constructor lines ~192-208). If the FormTemplateComponent branch is dropped, the LFC
        // constructor throws NG0201 "No provider for FormComponent" the moment it is created inside
        // svy-formtemplate. This guard constructs a real ListFormComponent using the rendered
        // FormTemplateComponent's element injector (the exact injector Angular would hand the LFC when
        // it is nested under svy-formtemplate) so the real _injector.get(FormTemplateComponent) branch
        // runs - the parent is NOT mocked.

        beforeEach(() => {
            registerErrorbeanSpecs();
            formService.createFormCache('aForm', buildFormState(), null);
        });

        it('resolves ListFormComponent.parent to the FormTemplateComponent instance via the real injector (does NOT throw NG0201)', () => {
            const fixture = TestBed.createComponent(TestHostComponent);
            fixture.detectChanges();
            const debugComp = fixture.debugElement.query(By.directive(FormTemplateComponent));
            const formTemplate = debugComp.componentInstance as FormTemplateComponent;

            // the element injector of the rendered <svy-formtemplate> host - this is exactly the parent
            // injector a servoycore-listformcomponent nested inside svy-formtemplate is created with, and
            // it provides FormTemplateComponent (self). Passing it as the LFC's _injector runs the real
            // _injector.get(FormTemplateComponent) third-host branch (FormComponent + DesignFormComponent
            // are absent here, so those two get() calls fail and are swallowed first).
            const formTemplateElementInjector = debugComp.injector;

            const renderer = jasmine.createSpyObj<Renderer2>('Renderer2', ['setStyle', 'setAttribute', 'removeAttribute']);
            const cdRef = jasmine.createSpyObj<ChangeDetectorRef>('ChangeDetectorRef', ['detectChanges', 'markForCheck', 'detach', 'reattach', 'checkNoChanges']);

            let lfc: ListFormComponent;
            expect(() => {
                // input()/viewChild() field initializers in the LFC constructor need an injection context.
                lfc = runInInjectionContext(TestBed.inject(Injector), () => new ListFormComponent(
                    renderer,
                    {} as any,
                    { getUIProperties: () => ({ getUIProperty: () => null }) } as any,
                    {} as any,
                    {} as any,
                    cdRef,
                    TestBed.inject(LoggerFactory),
                    formTemplateElementInjector,
                    TestBed.inject(DOCUMENT)
                ));
            }).not.toThrow();

            expect(lfc.parent).toBe(formTemplate);
            expect(lfc.parent instanceof AbstractFormComponent).toBe(true);
        });
    });

    describe('rendered DOM contract (spec §5)', () => {

        beforeEach(() => {
            registerErrorbeanSpecs();
            formService.createFormCache('aForm', buildFormState(), null);
        });

        it('renders a .svy-form root with an absolute .svy-wrapper and the runtime component tag', () => {
            const { fixture } = renderForm();
            const host = fixture.nativeElement as HTMLElement;

            const svyForm = host.querySelector('.svy-form');
            expect(svyForm).withContext('.svy-form root present').not.toBeNull();

            const wrapper = host.querySelector('.svy-wrapper') as HTMLElement;
            expect(wrapper).withContext('.svy-wrapper present').not.toBeNull();
            expect(wrapper.style.position).toBe('absolute');

            expect(host.querySelector('servoycore-errorbean')).withContext('runtime component tag present').not.toBeNull();
        });

        it('renders NO designer-only artifacts (svy-designform / designclass / svy-id / ghost / wireframe)', () => {
            const { fixture } = renderForm();
            const host = fixture.nativeElement as HTMLElement;

            expect(host.querySelector('svy-designform')).toBeNull();
            expect(host.querySelector('[designclass]')).toBeNull();
            expect(host.querySelector('[svy-id]')).toBeNull();
            expect(host.querySelector('.invisible_element')).toBeNull();
            expect(host.querySelector('.inherited_element')).toBeNull();
            expect(host.querySelector('.ghost')).toBeNull();
        });
    });

    describe('stateless contract: no handlers, no api, no server communication', () => {

        let comp: FormTemplateComponent;
        let errorBeanCache: ComponentCache;

        beforeEach(() => {
            registerErrorbeanSpecs();
            formService.createFormCache('aForm', buildFormState(), null);
            sabloService.callService.calls.reset();
            comp = renderForm().comp;
            errorBeanCache = formService.getFormCacheByName('aForm').getComponent('myErrorBean');
        });

        it('binds its formCache from the FormService', () => {
            expect(comp.getFormCache()).toBe(formService.getFormCacheByName('aForm'));
            expect(comp.getFormCache().absolute).toBe(true);
        });

        it('getHandler returns null so NO event functions are wired (clicks do nothing)', () => {
            expect(comp.getHandler(errorBeanCache, 'onActionMethodID')).toBeNull();
        });

        it('callApi returns null', () => {
            expect(comp.callApi('myErrorBean', 'requestFocus', [])).toBeNull();
        });

        it('datachange is an inert no-op that neither throws nor calls the server', () => {
            expect(() => comp.datachange(errorBeanCache, 'error', 'changed', false)).not.toThrow();
            expect(sabloService.callService).not.toHaveBeenCalled();
        });

        it('updateFormStyleClasses is an inert no-op', () => {
            expect(() => comp.updateFormStyleClasses('someClass')).not.toThrow();
            expect(sabloService.callService).not.toHaveBeenCalled();
        });

        it('building the cache and rendering the component never calls SabloService.callService (no formLoaded/dataPush/executeEvent/formUnloaded)', () => {
            expect(sabloService.callService).not.toHaveBeenCalled();
        });
    });

    describe('design-time ServoyApi', () => {

        let comp: FormTemplateComponent;
        let errorBeanCache: ComponentCache;

        beforeEach(() => {
            registerErrorbeanSpecs();
            formService.createFormCache('aForm', buildFormState(), null);
            comp = renderForm().comp;
            errorBeanCache = formService.getFormCacheByName('aForm').getComponent('myErrorBean');
        });

        it('getServoyApi returns a ServoyApi flagged as in-designer and caches it per component', () => {
            const api = comp.getServoyApi(errorBeanCache) as ServoyApi;
            expect(api).toBeTruthy();
            expect(api.isInDesigner()).toBe(true);
            // second call returns the SAME cached instance
            expect(comp.getServoyApi(errorBeanCache)).toBe(api);
        });

        it('exposes the component markup id through the api (needed for rendering)', () => {
            const api = comp.getServoyApi(errorBeanCache) as ServoyApi;
            expect(api.getMarkupId()).toBe('markup_error_1');
        });

        it('api.apply does not push to the server (stateless)', () => {
            sabloService.callService.calls.reset();
            const api = comp.getServoyApi(errorBeanCache) as ServoyApi;
            api.apply('error', 'x');
            expect(sabloService.callService).not.toHaveBeenCalled();
        });
    });
});
