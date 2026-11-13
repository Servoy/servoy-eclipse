import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { ConverterService } from '../../sablo/converter.service';
import { SabloService } from '../../sablo/sablo.service';
import { LoggerFactory, SessionStorageService, WindowRefService } from '@servoy/public';
import { ServicesService } from '../../sablo/services.service';
import { SabloDeferHelper } from '../../sablo/defer.service';
import { LoadingIndicatorService } from '../../sablo/util/loading-indicator/loading-indicator.service';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { TypesRegistry, IPropertyContext } from '../../sablo/types_registry';
import { DateType } from '../../sablo/converters/date_converter';
import { ObjectType } from '../../sablo/converters/object_converter';
import { JSMenuType, JSMenu } from './jsmenu_converter';

describe('JSMenuConverter', () => {
  let converterService: ConverterService<any>;
  let typesRegistry: TypesRegistry;
  let loggerFactory: LoggerFactory;
  let jsMenuType: JSMenuType;
  const propertyContext = undefined as unknown as IPropertyContext;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ServoyTestingModule],
      providers: [ConverterService, LoggerFactory, WindowRefService, ServicesService, SessionStorageService, LoadingIndicatorService],
    });

    const sabloService = TestBed.inject(SabloService);
    sabloService.connect({}, {}, '');
    TestBed.inject(SabloDeferHelper);
    loggerFactory = TestBed.inject(LoggerFactory);
    converterService = TestBed.inject(ConverterService);
    typesRegistry = TestBed.inject(TypesRegistry);
    typesRegistry.registerGlobalType(DateType.TYPE_NAME_SVY, new DateType(), true);
    typesRegistry.registerGlobalType(ObjectType.TYPE_NAME, new ObjectType(typesRegistry, converterService, loggerFactory), true);

    jsMenuType = new JSMenuType(converterService);
    typesRegistry.registerGlobalType(JSMenuType.TYPE_NAME, jsMenuType);
  });

  const convertToClient = (serverValue: any, current?: JSMenu): JSMenu =>
    jsMenuType.fromServerToClient(serverValue, current, propertyContext);

  const convertToServer = (value: JSMenu): any => {
    const result = jsMenuType.fromClientToServer(value, null as any, propertyContext);
    return result === null ? null : result[0];
  };

  describe('fromServerToClient', () => {
    it('should build a JSMenu copying name, styleClass and items', () => {
      const menu = convertToClient({ name: 'menu1', styleClass: 'cls', items: [{ id: 'i1' }] });
      expect(menu).toBeInstanceOf(JSMenu);
      expect(menu.name).toBe('menu1');
      expect(menu.styleClass).toBe('cls');
      expect(menu.items.length).toBe(1);
    });

    it('should build a JSMenu even when there are no items', () => {
      const menu = convertToClient({ name: 'empty', styleClass: '', items: undefined });
      expect(menu).toBeInstanceOf(JSMenu);
      expect(menu.items).toBeUndefined();
    });

    it('should convert typed extraProperties values via the converter service', () => {
      const dateMillis = Date.UTC(2021, 5, 7, 8, 9, 10);
      const serverValue = {
        name: 'menu2',
        styleClass: '',
        items: [
          {
            id: 'i1',
            extraProperties: {
              myCategory: {
                aDate: { _T: 'svy_date', _V: dateMillis },
                plain: 'not-converted',
              },
            },
          },
        ],
      };
      const menu = convertToClient(serverValue);
      const converted = (menu.items[0] as any).extraProperties.myCategory;
      expect(converted.aDate instanceof Date).toBe(true);
      expect((converted.aDate as Date).getTime()).toBe(dateMillis);
      expect(converted.plain).toBe('not-converted');
    });

    it('should not fail when extraProperties category is empty/falsy', () => {
      const menu = convertToClient({
        name: 'menu3',
        styleClass: '',
        items: [{ id: 'i1', extraProperties: { emptyCategory: null } }],
      });
      expect(menu).toBeInstanceOf(JSMenu);
    });
  });

  describe('fromClientToServer', () => {
    it('should return null when client data is null', () => {
      expect(jsMenuType.fromClientToServer(null as any, null as any, propertyContext)).toBeNull();
    });

    it('should return null when there are no pending requests', () => {
      const menu = convertToClient({ name: 'm', styleClass: '', items: [] });
      expect(jsMenuType.fromClientToServer(menu, null as any, propertyContext)).toBeNull();
    });

    it('should send a pushDataProviderValue request and clear it', () => {
      const menu = convertToClient({ name: 'm', styleClass: '', items: [] });
      let notified = false;
      menu.getInternalState().setChangeListener(() => {
        notified = true;
      });

      menu.pushDataProviderValue('cat', 'prop', 2, 'the-value');
      expect(notified).toBe(true);
      expect(menu.getInternalState().hasChanges()).toBe(true);

      const req = convertToServer(menu);
      expect(req).toEqual({ category: 'cat', propertyName: 'prop', itemIndex: 2, dataproviderValue: 'the-value' });
      expect(menu.getInternalState().hasChanges()).toBe(false);
      expect(convertToServer(menu)).toBeNull();
    });

    it('should send a setSelectedItem request and clear it', () => {
      const menu = convertToClient({ name: 'm', styleClass: '', items: [] });
      let notified = false;
      menu.getInternalState().setChangeListener(() => {
        notified = true;
      });

      menu.setSelectedItem('item-5');
      expect(notified).toBe(true);
      expect(menu.getInternalState().hasChanges()).toBe(true);

      const req = convertToServer(menu);
      expect(req).toEqual({ selectedItemID: 'item-5' });
      expect(convertToServer(menu)).toBeNull();
    });
  });

  describe('uiDestroyed', () => {
    it('should not throw when there are no items', () => {
      const menu = convertToClient({ name: 'm', styleClass: '', items: undefined });
      expect(() => menu.uiDestroyed()).not.toThrow();
    });

    it('should not throw when items have no extraProperties', () => {
      const menu = convertToClient({ name: 'm', styleClass: '', items: [{ id: 'i1' }] });
      expect(() => menu.uiDestroyed()).not.toThrow();
    });
  });
});
