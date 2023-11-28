import { TestBed, inject, tick, fakeAsync, flushMicrotasks } from '@angular/core/testing';
import { ConverterService, IChangeAwareValue } from '../../sablo/converter.service';
import { SabloService } from '../../sablo/sablo.service';
import { WindowRefService, IValuelist, SessionStorageService, LoggerFactory } from '@servoy/public';
import { ServicesService } from '../../sablo/services.service';
import { ValuelistType, IValuelistTValueFromServer } from './valuelist_converter';
import { SabloDeferHelper} from '../../sablo/defer.service';
import { LoadingIndicatorService } from '../../sablo/util/loading-indicator/loading-indicator.service';
import { TestSabloService, TestWebsocketService } from '../../testing/servoytesting.module';
import { WebsocketService } from '../../sablo/websocket.service';
import { IPropertyContext, PushToServerEnum } from '../../sablo/types_registry';

describe('ValuelistConverter', () => {

//  const FILTER = 'filter';
//  const HANDLED = 'handledID';
//  const ID_KEY = 'id';
//  const VALUE_KEY = 'value';
//  const VALUES = 'values';

  let converterService: ConverterService<IValuelist>;
  let propertyContext: IPropertyContext;
  let valuelistType: ValuelistType;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ ConverterService,
        LoggerFactory, WindowRefService, SabloDeferHelper, ServicesService, SessionStorageService, LoadingIndicatorService,
        { provide: WebsocketService, useClass: TestWebsocketService }, { provide: SabloService, useClass: TestSabloService }]
    });

    const sabloService: SabloService = TestBed.inject( SabloService );
    sabloService.callService = <T>(serviceName: string, methodName: string, argsObject: Record<string, any>, _async?: boolean): Promise<T> => {
        if (serviceName === 'formService' && methodName === 'getValuelistDisplayValue') {
            const promise = new Promise<any>((resolve, reject) => {
                if (argsObject.realValue === 4) {
                    resolve('d');
                } else {
                    reject('No display value found for ' + argsObject.realValue);
                }
            });
            return promise;
        }
    };
    sabloService.connect({}, {}, '');

    const sabloDeferHelper = TestBed.inject(SabloDeferHelper);
    converterService = TestBed.inject(ConverterService) as ConverterService<IValuelist>;

    valuelistType = new ValuelistType(sabloDeferHelper);
    propertyContext = {
            getProperty: (_propertyName: string) => undefined,
            getPushToServerCalculatedValue: () => PushToServerEnum.REJECT
        } as IPropertyContext;

  });

  const createDefaultValuelist = () => ({ values: [{displayValue: 'abbba', realValue: 1}, {displayValue: 'bbbbaab', realValue: 2}, {displayValue: 'caaabbc', realValue: 3}],
               valuelistid: 1073741880 } as IValuelistTValueFromServer);

  it( 'should convert from server to client', () => {
      const val: IValuelist = converterService.convertFromServerToClient(createDefaultValuelist(),
               valuelistType , undefined, undefined, undefined, propertyContext);

      expect( val ).toBeDefined();
      expect( val.length ).toBe(3, 'valuelist length should be \'3\' ');
      expect( val[0].displayValue).toBe( 'abbba', 'display value should be \'abbba\'' );
      expect( val[0].realValue).toBe( 1, 'real value should be \'1\'' );
      expect( val[2].displayValue).toBe( 'caaabbc', 'display value should be \'caaabbc\'' );
      expect( val[2].realValue).toBe( 3, 'real value should be \'3\'' );
  });

  it( 'should get display value', fakeAsync(() => {
      const val: IValuelist = converterService.convertFromServerToClient(createDefaultValuelist(),
               valuelistType , undefined, undefined, undefined, propertyContext);
      expect( val ).toBeDefined();
      expect( val.getDisplayValue).toBeDefined('should have \'getDisplayValue\' function');

      let changeNotified = false;
      let triggeredSendToServer = false;
      ((val as any) as IChangeAwareValue).getInternalState().setChangeListener((doNotPushNow?: boolean) => {
          changeNotified = true;
          triggeredSendToServer = !doNotPushNow;
      });
      const checkNotifiedAndTriggeredAndClear = (changeNotifiedWanted: boolean, triggeredSendToServerWanted: boolean) => {
          expect(changeNotified).toBe(changeNotifiedWanted);
          expect(triggeredSendToServer).toBe(triggeredSendToServerWanted);
          changeNotified = false;
          triggeredSendToServer = false;
      };

      let displayValue: string;
      val.getDisplayValue(4).subscribe((response) => {
          displayValue = response;
      });
      checkNotifiedAndTriggeredAndClear(true, true);

      let clientChange = converterService.convertFromClientToServer(val, valuelistType, val, propertyContext)[0];

      expect(clientChange.getDisplayValue).toBe(4, 'value should be 4');
      expect(displayValue).not.toBeDefined( 'display value should not be defined yet.' );

      converterService.convertFromServerToClient({ handledID : { id: clientChange.id, value: true }, getDisplayValue : 'd' } as IValuelistTValueFromServer ,
            valuelistType , val, undefined, undefined, propertyContext);
      flushMicrotasks();

      expect(displayValue).toBe( 'd', 'display value should be \'d\'' );

      // should be resolved right away
      displayValue = null;

      val.getDisplayValue(4).subscribe((response) => {
          displayValue = response;
      });
      checkNotifiedAndTriggeredAndClear(false, false);
      expect(displayValue).toBe( 'd', 'display value should be \'d\'' );

      const realValue = 5;
      let errorMessage: string;
      let display: any;
      val.getDisplayValue(realValue).subscribe((response) => {
          display = response;
      }, (response) => {
          errorMessage = response;
      });
      checkNotifiedAndTriggeredAndClear(true, true);
      clientChange = converterService.convertFromClientToServer(val, valuelistType, val, propertyContext)[0];
      expect(clientChange.getDisplayValue).toBe(5, 'value should be 4');
      expect(display).not.toBeDefined( 'display value should not be defined yet.' );
      expect(errorMessage).not.toBeDefined( 'error message should not be defined yet.' );

      converterService.convertFromServerToClient({ handledID : { id: clientChange.id, value: true }, getDisplayValue: realValue } as IValuelistTValueFromServer,
            valuelistType , val, undefined, undefined, propertyContext);
      flushMicrotasks();

      expect(display).toBe(realValue, 'should just return the realvalue');
      expect(errorMessage).not.toBeDefined('display value should not be defined' );
  }) as any);

  it( 'should filter list', fakeAsync(() => {
      let val: IValuelist = converterService.convertFromServerToClient(createDefaultValuelist(),
               valuelistType , undefined, undefined, undefined, propertyContext);
      expect( val ).toBeDefined();
      expect( val.length ).toBe(3, 'valuelist length should be \'3\' ');
      expect( val.filterList).toBeDefined('should have \'filter\' function');

      let isFiltered: boolean;
      val.filterList('abb').subscribe((_val) => {
          isFiltered = true;
      });
      expect(isFiltered).not.toBeDefined('filter promise should not be resolved yet.');

      let convertedValueForServer = converterService.convertFromClientToServer(val, valuelistType, val, propertyContext)[0];
      expect(convertedValueForServer).toBeDefined();
      expect(convertedValueForServer.filter).toBe('abb', 'valuelist filter should be \'abb\'');
      expect(convertedValueForServer.id).toBe(1, 'valuelist filter defer id should be \'1\'');
      expect(isFiltered).not.toBeDefined('filter promise should still not be resolved.');

      // convert once more, should have no changes
      convertedValueForServer = converterService.convertFromClientToServer(val, valuelistType, val, propertyContext)[0];
      expect(convertedValueForServer).toBe(null, 'convertedValueForServer should be \'null\' because there is no change on the valuelist');

      // simulate answer from server
      val = converterService.convertFromServerToClient({ handledID: {id: 1, value: true},
                        values: [{displayValue: 'abbba', realValue: 1}, {displayValue: 'caaabbc', realValue: 3}] } as IValuelistTValueFromServer,
                    valuelistType , val, undefined, undefined, propertyContext);
      tick(1); // a bit weird, but it seems we need to wait for the then function to be called..
      expect(isFiltered).toBe(true, 'filter promise should be resolved.');
      expect( val.length ).toBe(2, 'list is filtered, valuelist length should be \'2\' ');
      expect( val[0].displayValue).toBe( 'abbba', 'display value should be \'abbba\'' );
      expect( val[0].realValue).toBe( 1, 'real value should be \'1\'' );

      isFiltered = undefined;
      val.filterList('xyz').subscribe((_val) => {
          isFiltered = true;
      });
      expect(isFiltered).not.toBeDefined('filter promise should not be resolved yet.');

      convertedValueForServer = converterService.convertFromClientToServer(val, valuelistType, val, propertyContext)[0];
      expect(convertedValueForServer.filter).toBe('xyz', 'valuelist filter should be \'xyz\'');
      expect(convertedValueForServer.id).toBe(2, 'valuelist filter defer id should be \'2\'');
      expect(isFiltered).not.toBeDefined('filter promise should still not be resolved.');

      val = converterService.convertFromServerToClient({ handledID: {id: 2, value: true}, values: [] } as IValuelistTValueFromServer,
                    valuelistType , val, undefined, undefined, propertyContext);
      tick(1);
      expect( isFiltered ).toBe(true, 'filter promise should be resolved.');
      expect( val.length ).toBe(0, 'list is filtered, valuelist length should be \'0\' ');

      isFiltered = undefined;
      val.filterList('x').subscribe((_val) => {
          isFiltered = true;
      }, (_val) => {
          isFiltered = false;
      });
      convertedValueForServer = converterService.convertFromClientToServer(val, valuelistType, val, propertyContext)[0];
      expect(convertedValueForServer.filter).toBe('x', 'valuelist filter should be \'x\'');
      expect(convertedValueForServer.id).toBe(3, 'valuelist filter defer id should be \'3\'');

      // assume the server did not filter the valuelist
      val = converterService.convertFromServerToClient({ handledID: {id: 3, value: false} } as IValuelistTValueFromServer,
                    valuelistType , val, undefined, undefined, propertyContext);
      tick(1);
      expect(isFiltered).toBe(false, 'filter promise should be rejected.');
  }) as any);

  it( 'should have real values', () => {
      const vl = createDefaultValuelist();
      vl.hasRealValues = true;
      const val: IValuelist = converterService.convertFromServerToClient(vl, valuelistType , undefined, undefined, undefined, propertyContext);;

      expect( val.hasRealValues() ).toBe(true, 'valuelist should have real values');
  });
});
