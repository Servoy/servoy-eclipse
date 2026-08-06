import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
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
        return undefined as any;
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
               valuelistType , undefined as any, undefined as any, undefined as any, propertyContext);

      expect( val ).toBeDefined();
      expect( val.length ).toBe(3);
      expect( val[0].displayValue).toBe( 'abbba');
      expect( val[0].realValue).toBe( 1);
      expect( val[2].displayValue).toBe( 'caaabbc');
      expect( val[2].realValue).toBe( 3);
  });

  it( 'should get display value', async () => {
      const val: IValuelist = converterService.convertFromServerToClient(createDefaultValuelist(),
               valuelistType , undefined as any, undefined as any, undefined as any, propertyContext);
      expect( val ).toBeDefined();
      expect( val.getDisplayValue).toBeDefined();

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

      let displayValue: any;
      val.getDisplayValue(4).subscribe((response) => {
          displayValue = response;
      });
      checkNotifiedAndTriggeredAndClear(true, true);

      let clientChange = converterService.convertFromClientToServer(val, valuelistType, val, propertyContext)[0];

      expect(clientChange.getDisplayValue).toBe(4);
      expect(displayValue).not.toBeDefined();

      converterService.convertFromServerToClient({ handledID : { id: clientChange.id, value: true }, getDisplayValue : 'd' } as IValuelistTValueFromServer ,
            valuelistType , val, undefined as any, undefined as any, propertyContext);
      await new Promise(resolve => setTimeout(resolve, 0));

      expect(displayValue).toBe( 'd');

      // should be resolved right away
      displayValue = null as any;

      val.getDisplayValue(4).subscribe((response) => {
          displayValue = response;
      });
      checkNotifiedAndTriggeredAndClear(false, false);
      expect(displayValue).toBe( 'd');

      const realValue = 5;
      let errorMessage: any;
      let display: any;
      val.getDisplayValue(realValue).subscribe((response) => {
          display = response;
      }, (response) => {
          errorMessage = response;
      });
      checkNotifiedAndTriggeredAndClear(true, true);
      clientChange = converterService.convertFromClientToServer(val, valuelistType, val, propertyContext)[0];
      expect(clientChange.getDisplayValue).toBe(5);
      expect(display).not.toBeDefined();
      expect(errorMessage).not.toBeDefined();

      converterService.convertFromServerToClient({ handledID : { id: clientChange.id, value: true }, getDisplayValue: realValue } as IValuelistTValueFromServer,
            valuelistType , val, undefined as any, undefined as any, propertyContext);
      await new Promise(resolve => setTimeout(resolve, 0));

      expect(display).toBe(realValue);
      expect(errorMessage).not.toBeDefined();
  });

  it( 'should filter list', async () => {
      let val: IValuelist = converterService.convertFromServerToClient(createDefaultValuelist(),
               valuelistType , undefined as any, undefined as any, undefined as any, propertyContext);
      expect( val ).toBeDefined();
      expect( val.length ).toBe(3);
      expect( val.filterList).toBeDefined();

      let isFiltered: any;
      val.filterList('abb').subscribe((_val) => {
          isFiltered = true;
      });
      expect(isFiltered).not.toBeDefined();

      let convertedValueForServer = converterService.convertFromClientToServer(val, valuelistType, val, propertyContext)[0];
      expect(convertedValueForServer).toBeDefined();
      expect(convertedValueForServer.filter).toBe('abb');
      expect(convertedValueForServer.id).toBe(1);
      expect(isFiltered).not.toBeDefined();

      // convert once more, should have no changes
      convertedValueForServer = converterService.convertFromClientToServer(val, valuelistType, val, propertyContext)[0];
      expect(convertedValueForServer).toBe(null);

      // simulate answer from server
      val = converterService.convertFromServerToClient({ handledID: {id: 1, value: true},
                        values: [{displayValue: 'abbba', realValue: 1}, {displayValue: 'caaabbc', realValue: 3}] } as IValuelistTValueFromServer,
                    valuelistType , val, undefined as any, undefined as any, propertyContext);
      await new Promise(resolve => setTimeout(resolve, 1));
      expect(isFiltered).toBe(true);
      expect( val.length ).toBe(2);
      expect( val[0].displayValue).toBe( 'abbba');
      expect( val[0].realValue).toBe( 1);

      isFiltered = undefined as any;
      val.filterList('xyz').subscribe((_val) => {
          isFiltered = true;
      });
      expect(isFiltered).not.toBeDefined();

      convertedValueForServer = converterService.convertFromClientToServer(val, valuelistType, val, propertyContext)[0];
      expect(convertedValueForServer.filter).toBe('xyz');
      expect(convertedValueForServer.id).toBe(2);
      expect(isFiltered).not.toBeDefined();

      val = converterService.convertFromServerToClient({ handledID: {id: 2, value: true}, values: [] } as IValuelistTValueFromServer,
                    valuelistType , val, undefined as any, undefined as any, propertyContext);
      await new Promise(resolve => setTimeout(resolve, 1));
      expect( isFiltered ).toBe(true);
      expect( val.length ).toBe(0);

      isFiltered = undefined as any;
      val.filterList('x').subscribe((_val) => {
          isFiltered = true;
      }, (_val) => {
          isFiltered = false;
      });
      convertedValueForServer = converterService.convertFromClientToServer(val, valuelistType, val, propertyContext)[0];
      expect(convertedValueForServer.filter).toBe('x');
      expect(convertedValueForServer.id).toBe(3);

      // assume the server did not filter the valuelist
      val = converterService.convertFromServerToClient({ handledID: {id: 3, value: false} } as IValuelistTValueFromServer,
                    valuelistType , val, undefined as any, undefined as any, propertyContext);
      await new Promise(resolve => setTimeout(resolve, 1));
      expect(isFiltered).toBe(false);
  });

  it( 'should have real values', () => {
      const vl = createDefaultValuelist();
      vl.hasRealValues = true;
      const val: IValuelist = converterService.convertFromServerToClient(vl, valuelistType , undefined as any, undefined as any, undefined as any, propertyContext);;

      expect( val.hasRealValues() ).toBe(true);
  });
});
