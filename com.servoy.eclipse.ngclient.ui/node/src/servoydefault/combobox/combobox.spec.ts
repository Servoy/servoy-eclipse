import { async, ComponentFixture, TestBed, tick, fakeAsync, flushMicrotasks } from '@angular/core/testing';
import { ServoyDefaultCombobox } from './combobox';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { FormattingService, ServoyApi, TooltipService } from '../../ngclient/servoy_public';
import { SabloModule } from '../../sablo/sablo.module';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { of } from 'rxjs';

import { ConverterService } from '../../sablo/converter.service';
import { SabloService } from '../../sablo/sablo.service';
import { SabloDeferHelper } from '../../sablo/defer.service';
import { ValuelistConverter } from '../../ngclient/converters/valuelist_converter';
import { SpecTypesService } from '../../sablo/spectypes.service';
import { LoadingIndicatorService } from '../../sablo/util/loading-indicator/loading-indicator.service';
import { SessionStorageService } from 'angular-web-storage';
import { ServicesService } from '../../sablo/services.service';
import { WebsocketService } from '../../sablo/websocket.service';
import { WindowRefService } from '../../sablo/util/windowref.service';
import { LoggerFactory } from '../../sablo/logger.service';
import { Select2Data } from 'ng-select2-component';

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
]

const formattedData : Select2Data = [
    { value: '1', label: 'Bucuresti' },
    { value: '2', label: 'Timisoara'},
    { value: '3', label: 'Cluj' }
]

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
  let converterService: ConverterService;

  beforeEach(async(() => {
    servoyApi = jasmine.createSpyObj( 'ServoyApi', ['getMarkupId', 'trustAsHtml']);


    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultCombobox],
      providers: [ FormattingService, TooltipService, ValuelistConverter, ConverterService, SabloService, SabloDeferHelper, SpecTypesService,
        LoggerFactory, WindowRefService, WebsocketService, ServicesService, SessionStorageService, LoadingIndicatorService],
      imports: [ServoyPublicModule, SabloModule, NgbModule]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    const sabloService: SabloService = TestBed.get( SabloService );
    const sabloDeferHelper = TestBed.get( SabloDeferHelper );
    converterService = TestBed.get( ConverterService );
    converterService.registerCustomPropertyHandler( 'valuelist', new ValuelistConverter( sabloService, sabloDeferHelper) );

    fixture = TestBed.createComponent(ServoyDefaultCombobox);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;

    component = fixture.componentInstance;
    component.valuelistID = converterService.convertFromServerToClient(createDefaultValuelist(),'valuelist');
    component.dataProviderID = 3;
    component.ngOnInit();

    fixture.detectChanges();
  }); 

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should set initial list of values', () => {
    expect(component.valuelistID.length).toBe(3);
  });

  it('should set initial list of values', () => {
    expect(component.data.values).toEqual(formattedData.values, 'the valuelist doesn\'t have the values of the formattedData')
  });

  it('should have the default value 3', done => {
    component.observableValue.subscribe(value => {
      expect(value).toBe(3); // data provider's value
      done();
    });
  });
});
