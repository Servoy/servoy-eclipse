import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { CUSTOM_ELEMENTS_SCHEMA, Component, ViewChild, TemplateRef,
            Input, Output, ChangeDetectionStrategy, EventEmitter,
            Renderer2, ChangeDetectorRef, DebugElement } from '@angular/core';

import { FormattingService, TooltipService, LoggerFactory, ServoyBaseComponent,
            ICustomObjectValue, ICustomArrayValue, ServoyPublicModule, WindowRefService, SpecTypesService } from '@servoy/public';
import { LocaleService } from '../../ngclient/locale.service';
import { I18NProvider } from '../../ngclient/services/i18n_provider.service';

import { FormComponent } from '../../ngclient/form/form_component.component';

import { FormService } from '../../ngclient/form.service';
import { ServoyService } from '../../ngclient/servoy.service';
import { CustomArrayTypeFactory } from '../../ngclient/converters/json_array_converter';
import { CustomObjectTypeFactory } from '../../ngclient/converters/json_object_converter';
import { SabloService } from '../../sablo/sablo.service';

import { ErrorBean } from '../../servoycore/error-bean/error-bean';
import { ServoyCoreSlider } from '../../servoycore/slider/slider';

import { ConverterService } from '../../sablo/converter.service';
import { WebsocketService } from '../../sablo/websocket.service';
import { TypesRegistry } from '../../sablo/types_registry';

import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { PopupFormService } from '../services/popupform.service';
import { AddAttributeDirective } from '../../servoycore/addattribute.directive';

import { ClientFunctionService } from '../../sablo/clientfunction.service';
import { ObjectType } from '../../sablo/converters/object_converter';

import { By } from '@angular/platform-browser';

// this test will create a Form (component) (inside a TestHostComponent) that contains a test Servoy component

@Component({
    template: `
    <svy-form [name]="'aForm'" [injectedComponentRefs]="getCustomTestComponentTemplates()"></svy-form>
    <ng-template #customTestComponent let-callback="callback" let-state="state">
      @if (state.model.visible) {
        <testcomponents-custom-component
          [divLocation]="state.model.divLocation"
          (divLocationChange)="callback.datachange(state,'divLocation',$event)"
          [dataprovider]="state.model.dataprovider"
          (dataprovider)="callback.datachange(state,'dataprovider',$event,true)"
          [arrayOfCustomObjects]="state.model.arrayOfCustomObjects" (arrayOfCustomObjectsChange)="callback.datachange(state,'arrayOfCustomObjects',$event)"
          [myActionHandler]="callback.getHandler(state,'myActionHandler')"
          [servoyApi]="callback.getServoyApi(state)"
          [servoyAttributes]="state.model.servoyAttributes"
          [cssPosition]="state.model.cssPosition"
          [name]="state.name" #cmp>
        </testcomponents-custom-component>
      }
    </ng-template>`,
    standalone: false
})
class TestHostComponent {
    @ViewChild('customTestComponent', { static: true }) readonly customTestComponentTemplate: TemplateRef<any>;

    getCustomTestComponentTemplates() {
        return { customTestComponent: this.customTestComponentTemplate };
    }
}

@Component({
    selector: 'testcomponents-custom-component',
    template: `
        <div  [id]="servoyApi.getMarkupId()" #element>
            <button type="button" (click)="click1()" class="button1">
                {{divLocation}}
            </button>
            <button type="button" (click)="click2()" class="button2">
                {{divLocation}}
            </button>
            <button type="button" (click)="click3()" class="button3">
                {{divLocation}}
            </button>
        </div>`,
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class TestComponentsCustomComponent extends ServoyBaseComponent<HTMLButtonElement> {

    @Input() arrayOfCustomObjects: Array<SomeCustomObject>;

    @Input() divLocation: number;
    @Output() divLocationChange = new EventEmitter();

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

//    svyOnInit() {
//        super.svyOnInit();
//    }
//
//    svyOnChanges(changes: SimpleChanges) {
//        if(changes['arrayOfCustomObjects']) {
//        }
//        super.svyOnChanges(changes);
//    }

    click1() {
        this.divLocation++;
        this.divLocationChange.emit(this.divLocation);
    }

    click2() {
        this.arrayOfCustomObjects.push({ active: true });
    }

    click3() {
        this.arrayOfCustomObjects[0].active = false;
    }

}

interface SomeCustomObject extends ICustomObjectValue {
    active: boolean;
}

interface ComponentModelContents { divLocation: number; arrayOfCustomObjects: ICustomArrayValue<SomeCustomObject>,[property: string]: unknown,
                                containedForm?: {
                                    absoluteLayout?: boolean,
                                },
                                styleClass?: string,
                                size?: {width: number, height: number},
                                containers?: {  added: { [container: string]: string[] }; removed: { [container: string]: string[] } }, 
                                cssstyles?: { [container: string]: { [classname: string]: string } }  };

/** we make use here of a full FormService and FormComponent as well in order to test a bit push-to-server settings and how it integrates with form/formService impl */
describe('FormComponentComponentTest', () => {
    let testHostComponent: TestHostComponent; // that contains a FormComponent that contains a TestComponentsCustomComponent
    let button1InsideTestComponentDebug: DebugElement;
    let button2InsideTestComponentDebug: DebugElement;
    let button3InsideTestComponentDebug: DebugElement;

    let fixture: ComponentFixture<TestHostComponent>;
    let sabloService: jasmine.SpyObj<SabloService>;
    let servoyService: jasmine.SpyObj<ServoyService>;
    let converterService: ConverterService<unknown>;
    let websocketService: jasmine.SpyObj<WebsocketService>;
    let logFactory: LoggerFactory;
    let typesRegistry: TypesRegistry;
    let specTypesService: SpecTypesService;

    let formService: FormService;
    let testComponentModel: ComponentModelContents;

    beforeEach(waitForAsync(() => {
        sabloService = jasmine.createSpyObj('SabloService', ['callService']);
        servoyService = jasmine.createSpyObj('ServoyService', ['connect']);
        websocketService = jasmine.createSpyObj('WebsocketService', {
                getSession: { then: () => Promise.resolve() }
        });

        //        formService = jasmine.createSpyObj('FormService', {
        //            getFormCache: {
        //                absolute: true,
        //                getComponent: () => ({ model: {} })
        //            },
        //            destroy: () => null
        //        });
        TestBed.configureTestingModule({
            declarations: [ TestHostComponent, FormComponent, TestComponentsCustomComponent, AddAttributeDirective, ServoyCoreSlider, ErrorBean],
            imports: [
                ServoyTestingModule, ServoyPublicModule
            ],
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
            schemas: [
                CUSTOM_ELEMENTS_SCHEMA
            ]
        }).compileComponents();

        formService = TestBed.inject(FormService);
        typesRegistry = TestBed.inject(TypesRegistry);
        logFactory = TestBed.inject(LoggerFactory);
        converterService = TestBed.inject(ConverterService);
        specTypesService = TestBed.inject(SpecTypesService);

        typesRegistry.registerGlobalType(ObjectType.TYPE_NAME, new ObjectType(typesRegistry, converterService, logFactory));
        typesRegistry.getTypeFactoryRegistry().contributeTypeFactory('JSON_arr', new CustomArrayTypeFactory(typesRegistry, converterService, logFactory));
        typesRegistry.getTypeFactoryRegistry().contributeTypeFactory('JSON_obj', new CustomObjectTypeFactory(typesRegistry, converterService, specTypesService, logFactory));

        // here we feed in the client side equivalent some (imaginary test) servoy .spec file
        // so we can play a bit with push to server settings in tests
        typesRegistry.addComponentClientSideSpecs({
            customTestComponent: {
                p: {
                    divLocation: { s: 2 }, // shallow push to server
                    arrayOfCustomObjects: { t: ['JSON_arr', { t: [ 'JSON_obj', 'customO' ], s: 2 }], s: 1 } // element push to server shallow, array push to server is allow
                },
                ftd: {
                    JSON_obj: {
                        customO: {
                            active: { s: 2 } // shallow push to server
                        }
                    }
                }
            }
        });

        formService.createFormCache('aForm', {
            responsive: false,
            size: {
                width: 543,
                height: 368
            },
            children: [
                {
                    name: '',
                    model: {
                        visible: true,
                        findmode: false,
                        enabled: true,
                        designSize: {
                            width: 543,
                            height: 368
                        },
                        size: {
                            width: 543,
                            height: 368
                        },
                        addMinSize: true,
                        useCssPosition: {
                            tabpanel_1: true
                        },
                        absoluteLayout: {
                            '': true
                        },
                        hasExtraParts: false
                    }
                },
                {
                    part: true,
                    classes: [
                        'svy-body'
                    ],
                    layout: {
                        position: 'absolute',
                        left: '0px',
                        right: '0px',
                        top: '0px',
                        bottom: '0px',
                        'overflow-x': 'auto',
                        'overflow-y': 'auto'
                    },
                    children: [
                        {
                            name: 'myCustomTestComponent',
                            specName: 'customTestComponent',
                            model: {
                                divLocation: 15,
                                cssPosition: {
                                    position: 'absolute',
                                    top: '27px',
                                    left: '38px',
                                    height: '305px',
                                    width: '467px'
                                },
                                visible: true,
                                svyMarkupId: 'saf6c089908a013ca21e631b041f8a64a',
                                arrayOfCustomObjects: {
                                    vEr: 1,
                                    v: []
                                },
                                servoyAttributes: {}
                            },
                            position: {
                                left: '38px',
                                top: '27px',
                                width: '467px',
                                height: '305px'
                            }
                        }
                    ]
                }
            ]
        }, null);
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(TestHostComponent);
        fixture.detectChanges();

        testHostComponent = fixture.componentInstance;
        button1InsideTestComponentDebug = fixture.debugElement.query(By.css('.button1'));
        button2InsideTestComponentDebug = fixture.debugElement.query(By.css('.button2'));
        button3InsideTestComponentDebug = fixture.debugElement.query(By.css('.button3'));

        testComponentModel = formService.getFormCacheByName('aForm').getComponent('myCustomTestComponent').model as ComponentModelContents;
        expect(sabloService.callService).toHaveBeenCalledWith(
            'formService',
            'formLoaded',
            { formname: 'aForm' },
            true);
         expect(sabloService.callService).toHaveBeenCalledWith(
            'formService',
            'dataPush',
            { formname: 'aForm',
             beanname: '',
             changes: { size : jasmine.anything() } },
            true);    
        expect(sabloService.callService).toHaveBeenCalledTimes(2);    
        sabloService.callService.calls.reset();
    });

    it('stuff should be available and value should be correct', () => {
        expect(testHostComponent).toBeTruthy();
        expect(converterService).toBeTruthy();
        expect(formService).toBeTruthy();
        expect(button1InsideTestComponentDebug).toBeTruthy();
        expect(button2InsideTestComponentDebug).toBeTruthy();
        expect(button3InsideTestComponentDebug).toBeTruthy();

        expect(testComponentModel.divLocation).toBe(15);
        expect(testComponentModel.arrayOfCustomObjects.length).toBe(0);
    });

    it('changing div location via a click in the component should get sent to server', () => {
        button1InsideTestComponentDebug.triggerEventHandler('click', new Event('click'));
        expect(testComponentModel.divLocation).toBe(16);

        expect(sabloService.callService).toHaveBeenCalledOnceWith(
            'formService',
            'dataPush', {
               formname: 'aForm',
               beanname: 'myCustomTestComponent',
               changes: { divLocation: 16 },
               oldvalues: { divLocation: 15 } }, true);
    });

    it('adding an element to the shallow-element array should send stuff to server right away; then altering that should send it again as well', () => {
        button2InsideTestComponentDebug.triggerEventHandler('click', new Event('click'));
        expect(testComponentModel.arrayOfCustomObjects[0].active).toBe(true);

        // as the shallow watch in array elements of 'arrayOfCustomObjects' (see .spec definition above in this test file) triggered a send-to-server
        // the custom array and custom object values should now be 'smart'& proxied in the model
        expect(testComponentModel.arrayOfCustomObjects[0].markSubPropertyAsHavingDeepChanges).toBeInstanceOf(Function);
        expect(testComponentModel.arrayOfCustomObjects.markElementAsHavingDeepChanges).toBeInstanceOf(Function);

        expect(sabloService.callService).toHaveBeenCalledOnceWith(
            'formService',
            'dataPush', {
                formname: 'aForm',
                beanname: 'myCustomTestComponent',
                changes: {
                    arrayOfCustomObjects: { vEr: 0,
                        v: [ { vEr: 0,
                                v: { active: true }
                             }]}
                    }
                }, true);
        sabloService.callService.calls.reset();

        // ok now change the value of a prop inside the custom object that is in the array
        button3InsideTestComponentDebug.triggerEventHandler('click', new Event('click'));
        expect(testComponentModel.arrayOfCustomObjects[0].active).toBe(false);

        expect(sabloService.callService).toHaveBeenCalledOnceWith(
            'formService',
            'dataPush', {
                formname: 'aForm',
                beanname: 'myCustomTestComponent',
                changes: {
                    arrayOfCustomObjects: {
                        vEr: 1,
                        u: [ {
                                i: 0,
                                v: {
                                    vEr: 1,
                                    u: [ { k: 'active', v: false } ]
                                }
                             }
                        ]
                    }
                }
            }, true);
    });

});

