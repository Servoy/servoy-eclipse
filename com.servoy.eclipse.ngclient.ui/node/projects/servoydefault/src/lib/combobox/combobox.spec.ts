import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ServoyDefaultCombobox } from './combobox';
import { ServoyPublicModule, ServoyBaseComponent, Format, ServoyApi } from '@servoy/public';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { ConverterService } from '../../sablo/converter.service';
import { SabloDeferHelper } from '../../sablo/defer.service';
import { ValuelistType } from '../../ngclient/converters/valuelist_converter';
import { ServicesService } from '../../sablo/services.service';
import { By } from '@angular/platform-browser';
import { DebugElement, SimpleChange } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TypesRegistry, IPropertyContext, PushToServerEnum } from '../../sablo/types_registry';

const mockData = [
  {
    realValue: 1,
    displayValue: 'Bucuresti'
  },
  {
    realValue: 2,
    displayValue: 'Timisoara'
  },
  {
    realValue: 3,
    displayValue: 'Cluj'
  }
];

function createDefaultValuelist() {
  const json = {};
  json['values'] = mockData;
  json['valuelistid'] = 1073741880;
  return json;
}

describe('ComboboxComponent', () => {
  let component: ServoyDefaultCombobox;
  let fixture: ComponentFixture<ServoyDefaultCombobox>;
  let servoyApi: ServoyApi;
  let combobox: DebugElement;
  let converterService: ConverterService;

  beforeEach(waitForAsync(() => {

    // the following construct does nothing but make sure that an error is generated at compile-time if ServoyApi changes
    // so that when that happens we can update the jasmine.createSpyObj below which is based on strings only
    class _X extends ServoyApi {
        formWillShow(_fn: string, _rn?: string, _fi?: number): Promise<boolean> { return undefined; }
        hideForm(_fn: string, _rn?: string, _fi?: number, _fntws?: string, _rntwbs?: string, _fitwbs?: number): Promise<boolean> { return undefined; }
        startEdit(_p: string) {}
        apply(_propertyName: string, _value: any) {}
        callServerSideApi(_methodName: string, _args: Array<any>) {}
        getFormComponentElements(_propertyName: string, _formComponentValue: any) {}
        isInDesigner(): boolean { return false }
        trustAsHtml(): boolean { return false }
        isInAbsoluteLayout(): boolean { return false }
        getMarkupId(): string  { return undefined; }
        getFormName(): string  { return undefined; }
        registerComponent(_component: ServoyBaseComponent<any>) {}
        unRegisterComponent(_component: ServoyBaseComponent<any>) {}
        getClientProperty(_key:string): any { return undefined; }
    };
    new _X(); // just to remove unused class warning

    servoyApi = jasmine.createSpyObj( 'ServoyApi', [
        'formWillShow',
        'hideForm',
        'startEdit',
        'apply',
        'callServerSideApi',
        'getFormComponentElements',
        'isInDesigner',
        'trustAsHtml',
        'isInAbsoluteLayout',
        'getMarkupId',
        'getFormName',
        'registerComponent',
        'unRegisterComponent',
        'getClientProperty'
    ]);


    TestBed.configureTestingModule({
        declarations: [ ServoyDefaultCombobox ],
        providers: [ ServicesService ],
        imports: [ ServoyPublicModule, ServoyTestingModule, NgbModule, FormsModule ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    const sabloDeferHelper = TestBed.inject( SabloDeferHelper );
    const typesRegistry = TestBed.inject(TypesRegistry);
    converterService = TestBed.inject( ConverterService );

    typesRegistry.registerGlobalType(ValuelistType.TYPE_NAME, new ValuelistType(sabloDeferHelper), true);

    fixture = TestBed.createComponent(ServoyDefaultCombobox);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;

    component = fixture.componentInstance;
    const propertyContext = {
            getProperty: (_propertyName: string) => undefined,
            getPushToServerCalculatedValue: () => PushToServerEnum.REJECT
        } as IPropertyContext;
    component.valuelistID = converterService.convertFromServerToClient(createDefaultValuelist(),
                     new ValuelistType(sabloDeferHelper) , undefined, undefined, undefined, propertyContext);
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId', 'trustAsHtml', 'startEdit','registerComponent','unRegisterComponent']);
    component.dataProviderID = 3;
    component.format = new Format();
    component.format.type = 'TEXT';
    component.ngOnInit();
    component.ngOnChanges({
      dataProviderID: new SimpleChange(null, 3, true)
    });
    combobox = fixture.debugElement.query(By.css('.svy-combobox'));
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial length = 3', () => {
    expect(component.valuelistID.length).toBe(3);
  });

  it('should have called servoyApi.getMarkupId', () => {
    expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
  });

  it('should use start edit directive', () => {
    combobox.triggerEventHandler('focus', null);
    fixture.detectChanges();
    expect(component.servoyApi.startEdit).toHaveBeenCalled();
  });

  xit('should call update method', () => {
     // test should be to click on the combo to open the tree and then click on an item.
    spyOn(component, 'updateValue');
    combobox.nativeElement.dispatchEvent(new Event('update'));
    fixture.detectChanges();
    expect(component.updateValue).toHaveBeenCalled();
  });

});
