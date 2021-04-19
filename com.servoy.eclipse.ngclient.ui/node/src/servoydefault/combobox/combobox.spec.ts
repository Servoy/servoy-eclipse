import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ServoyDefaultCombobox } from './combobox';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { Format, FormattingService, ServoyApi, TooltipService } from '../../ngclient/servoy_public';
import { SabloModule } from '../../sablo/sablo.module';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { ConverterService } from '../../sablo/converter.service';
import { SabloService } from '../../sablo/sablo.service';
import { SabloDeferHelper } from '../../sablo/defer.service';
import { ValuelistConverter } from '../../ngclient/converters/valuelist_converter';
import { SpecTypesService } from '../../sablo/spectypes.service';
import { LoadingIndicatorService } from '../../sablo/util/loading-indicator/loading-indicator.service';
import { SessionStorageService } from '../../sablo/webstorage/sessionstorage.service';
import { ServicesService } from '../../sablo/services.service';
import { WebsocketService } from '../../sablo/websocket.service';
import { WindowRefService } from '../../sablo/util/windowref.service';
import { LoggerFactory } from '../../sablo/logger.service';
import { By } from '@angular/platform-browser';
import { DebugElement, SimpleChange } from '@angular/core';
import { FormsModule } from '@angular/forms';

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
  },
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
  let servoyApi;
  let combobox: DebugElement;
  let converterService: ConverterService;

  beforeEach(waitForAsync(() => {
    servoyApi = jasmine.createSpyObj( 'ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent','registerComponent','unRegisterComponent']);


    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultCombobox],
      providers: [ FormattingService, TooltipService, ValuelistConverter, ConverterService, SabloService, SabloDeferHelper, SpecTypesService,
        LoggerFactory, WindowRefService, WebsocketService, ServicesService, SessionStorageService, LoadingIndicatorService],
      imports: [ServoyPublicModule, SabloModule, NgbModule, FormsModule]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    const sabloService: SabloService = TestBed.inject( SabloService );
    const sabloDeferHelper = TestBed.inject( SabloDeferHelper );
    converterService = TestBed.inject( ConverterService );
    converterService.registerCustomPropertyHandler( 'valuelist', new ValuelistConverter( sabloService, sabloDeferHelper) );

    fixture = TestBed.createComponent(ServoyDefaultCombobox);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;

    component = fixture.componentInstance;
    component.valuelistID = converterService.convertFromServerToClient(createDefaultValuelist(), 'valuelist');
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
