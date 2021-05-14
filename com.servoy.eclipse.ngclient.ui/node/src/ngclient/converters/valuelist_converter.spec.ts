import { TestBed, inject, tick, fakeAsync, flushMicrotasks } from '@angular/core/testing';
import { ConverterService } from '../../sablo/converter.service';
import { SabloService } from '../../sablo/sablo.service';
import { LoggerFactory } from '@servoy/public';
import { WindowRefService, IValuelist, SpecTypesService, instanceOfChangeAwareValue } from '@servoy/public';
import { ServicesService } from '../../sablo/services.service';
import { ValuelistConverter } from './valuelist_converter';
import { SabloDeferHelper} from '../../sablo/defer.service';
import { SessionStorageService } from '../../sablo/webstorage/sessionstorage.service';
import { LoadingIndicatorService } from '../../sablo/util/loading-indicator/loading-indicator.service';
import { TestSabloService, TestWebsocketService } from '../../testing/servoytesting.module';
import { WebsocketService } from '../../sablo/websocket.service';
import { TestabilityService } from '../../sablo/testability.service';

describe('ValuelistConverter', () => {

  const FILTER = 'filter';
  const HANDLED = 'handledID';
  const ID_KEY = 'id';
  const VALUE_KEY = 'value';
  const VALUES = 'values';

  let converterService: ConverterService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ValuelistConverter, ConverterService, SpecTypesService,
        LoggerFactory, WindowRefService, SabloDeferHelper, TestabilityService, ServicesService, SessionStorageService, LoadingIndicatorService,
        { provide: WebsocketService, useClass: TestWebsocketService }, { provide: SabloService, useClass: TestSabloService }]
    });

    const sabloService: SabloService = TestBed.get( SabloService );
    sabloService.callService = <T>(serviceName: string, methodName: string, argsObject, async?: boolean): Promise<T> => {
        if (serviceName === 'formService' && methodName === 'getValuelistDisplayValue') {
            const promise = new Promise<any>((resolve, reject) => {
                if (argsObject.realValue === 4) {
                    resolve('d');
                } else {
                    reject('No display value found for ' + argsObject.realValue);
                }
            });
            return promise
        }
    };
    sabloService.connect({}, {}, '');
    const sabloDeferHelper = TestBed.inject( SabloDeferHelper );
    converterService = TestBed.inject( ConverterService );
    converterService.registerCustomPropertyHandler( 'valuelist', new ValuelistConverter( sabloService, sabloDeferHelper) );
  });

  function createDefaultValuelist() {
      const data = [{displayValue: 'abbba', realValue: 1}, {displayValue: 'bbbbaab', realValue: 2}, {displayValue: 'caaabbc', realValue: 3}];
      const json = {};
      json[VALUES] = data;
      json['valuelistid'] = 1073741880;
      return json;
  }

  it('should be created', inject([ValuelistConverter], (service: ValuelistConverter) => {
    expect(service).toBeTruthy();
  }));

  it( 'should convert from server to client', () => {
      const val: IValuelist = converterService.convertFromServerToClient(createDefaultValuelist() , 'valuelist' );
      expect( val ).toBeDefined();
      expect( val.length ).toBe(3, 'valuelist length should be \'3\' ');
      expect( val[0].displayValue).toBe( 'abbba', 'display value should be \'abbba\'' );
      expect( val[0].realValue).toBe( 1, 'real value should be \'1\'' );
      expect( val[2].displayValue).toBe( 'caaabbc', 'display value should be \'caaabbc\'' );
      expect( val[2].realValue).toBe( 3, 'real value should be \'3\'' );
  });

  it( 'should get display value', <any>fakeAsync(() => {
      const val: IValuelist = converterService.convertFromServerToClient(createDefaultValuelist() , 'valuelist' );
      expect( val ).toBeDefined();
      expect( val.getDisplayValue).toBeDefined('should have \'getDisplayValue\' function');

      let changeListenerCalled = false;

      if (instanceOfChangeAwareValue(val)) {
        val.getStateHolder().setChangeListener(() => {
          changeListenerCalled = true;
        });
      } else {
        fail('should be a change aware value');
      }

      let displayValue;
      val.getDisplayValue(4).subscribe((response) => {
          displayValue = response;
      });
      expect(changeListenerCalled).toBe(true, 'change listener should be called');

      let clientChange = converterService.convertFromClientToServer(val, 'valuelist');
      expect(clientChange.getDisplayValue).toBe(4, 'value should be 4');

      expect(displayValue).not.toBeDefined( 'display value should not be defined yet.' );

      const fromServer = {handledID : {id: clientChange.id, value: true}, getDisplayValue : 'd'};
      converterService.convertFromServerToClient(fromServer , 'valuelist', val );
      flushMicrotasks();

      expect(displayValue).toBe( 'd', 'display value should be \'d\'' );

      // should be resolved right away
      displayValue = null;
      changeListenerCalled = false;
      val.getDisplayValue(4).subscribe((response) => {
          displayValue = response;
      });
      expect(changeListenerCalled).toBe(false, 'change listener should not be called');
      expect(displayValue).toBe( 'd', 'display value should be \'d\'' );

      const realValue = 5;
      let errorMessage;
      let display;
      changeListenerCalled = false;
      val.getDisplayValue(realValue).subscribe((response) => {
          display = response;
      }, (response) => {
          errorMessage = response;
      });
      expect(changeListenerCalled).toBe(true, 'change listener should be called');
      clientChange = converterService.convertFromClientToServer(val, 'valuelist');
      expect(clientChange.getDisplayValue).toBe(5, 'value should be 4');
      expect(display).not.toBeDefined( 'display value should not be defined yet.' );
      expect(errorMessage).not.toBeDefined( 'error message should not be defined yet.' );

      const fromServer2 = {handledID : {id: clientChange.id, value: true}, getDisplayValue : realValue};
      converterService.convertFromServerToClient(fromServer2 , 'valuelist', val );
      flushMicrotasks();
      expect(display).toBe(realValue, 'should just return the realvalue');
      expect(errorMessage).not.toBeDefined('display value should not be defined' );
  }));

  it( 'should filter list', <any>fakeAsync(() => {
      let serverJSONValue = createDefaultValuelist();
      let val: IValuelist = converterService.convertFromServerToClient(serverJSONValue , 'valuelist' );
      expect( val ).toBeDefined();
      expect( val.length ).toBe(3, 'valuelist length should be \'3\' ');
      expect( val.filterList).toBeDefined('should have \'filter\' function');

      let isFiltered: boolean;
      let promise = val.filterList('abb').subscribe((val) => {
          isFiltered = true;
      });
      expect(isFiltered).not.toBeDefined('filter promise should not be resolved yet.');

      let convertedValueForServer = converterService.convertFromClientToServer( val, 'valuelist', val );
      expect(convertedValueForServer).toBeDefined();
      expect(convertedValueForServer.filter).toBe('abb', 'valuelist filter should be \'abb\'');
      expect(convertedValueForServer.id).toBe(1, 'valuelist filter defer id should be \'1\'');
      expect(isFiltered).not.toBeDefined('filter promise should still not be resolved.');

      // convert once more, should have no changes
      convertedValueForServer = converterService.convertFromClientToServer( val, 'valuelist', val );
      expect(convertedValueForServer).toBe(null, 'convertedValueForServer should be \'null\' because there is no change on the valuelist');

      // simulate answer from server
      serverJSONValue = {};
      serverJSONValue[HANDLED] = {id: 1, value: true};
      serverJSONValue[VALUES] = [{displayValue: 'abbba', realValue: 1}, {displayValue: 'caaabbc', realValue: 3}];

      val = converterService.convertFromServerToClient(serverJSONValue , 'valuelist', val );
      tick(1); // a bit weird, but it seems we need to wait for the then function to be called..
      expect(isFiltered).toBe(true, 'filter promise should be resolved.');
      expect( val.length ).toBe(2, 'list is filtered, valuelist length should be \'2\' ');
      expect( val[0].displayValue).toBe( 'abbba', 'display value should be \'abbba\'' );
      expect( val[0].realValue).toBe( 1, 'real value should be \'1\'' );

      isFiltered = undefined;
      promise = val.filterList('xyz').subscribe((val) => {
          isFiltered = true;
      });
      expect(isFiltered).not.toBeDefined('filter promise should not be resolved yet.');

      convertedValueForServer = converterService.convertFromClientToServer( val, 'valuelist', val );
      expect(convertedValueForServer.filter).toBe('xyz', 'valuelist filter should be \'xyz\'');
      expect(convertedValueForServer.id).toBe(2, 'valuelist filter defer id should be \'2\'');
      expect(isFiltered).not.toBeDefined('filter promise should still not be resolved.');

      serverJSONValue = {};
      serverJSONValue[HANDLED] = {id: 2, value: true};
      serverJSONValue[VALUES] = [];

      val = converterService.convertFromServerToClient(serverJSONValue , 'valuelist', val );
      tick(1);
      expect( isFiltered ).toBe(true, 'filter promise should be resolved.');
      expect( val.length ).toBe(0, 'list is filtered, valuelist length should be \'0\' ');

      isFiltered = undefined;
      promise = val.filterList('x').subscribe((val) => {
          isFiltered = true;
      }, (val) => {
          isFiltered = false;
      });
      convertedValueForServer = converterService.convertFromClientToServer( val, 'valuelist', val );
      expect(convertedValueForServer.filter).toBe('x', 'valuelist filter should be \'x\'');
      expect(convertedValueForServer.id).toBe(3, 'valuelist filter defer id should be \'3\'');

      // assume the server did not filter the valuelist
      serverJSONValue = {};
      serverJSONValue[HANDLED] = {id: 3, value: false};
      val = converterService.convertFromServerToClient(serverJSONValue , 'valuelist', val );
      tick(1);
      expect(isFiltered).toBe(false, 'filter promise should be rejected.');
  }));

  it( 'should have real values', () => {
      const vl = createDefaultValuelist();
      vl['hasRealValues'] = true;
      const val: IValuelist = converterService.convertFromServerToClient(vl , 'valuelist' );
      expect( val.hasRealValues() ).toBe(true, 'valuelist should have real values');
  });
});
