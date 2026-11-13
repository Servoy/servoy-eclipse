import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { ConverterService } from '../../sablo/converter.service';
import { SabloService } from '../../sablo/sablo.service';
import { LoggerFactory, SessionStorageService, WindowRefService } from '@servoy/public';
import { ServicesService } from '../../sablo/services.service';
import { FoundsetType } from './foundset_converter';
import { SabloDeferHelper } from '../../sablo/defer.service';
import { ViewportService } from '../services/viewport.service';
import { DateType } from '../../sablo/converters/date_converter';
import { LoadingIndicatorService } from '../../sablo/util/loading-indicator/loading-indicator.service';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { IPropertyContext, PushToServerEnum, TypesRegistry } from '../../sablo/types_registry';
import { ObjectType } from '../../sablo/converters/object_converter';
import { ComponentType } from './component_converter';
import { UIBlockerService } from '../services/ui_blocker.service';
import { ServoyService } from '../servoy.service';
import { FormcomponentType, FormComponentValue } from './formcomponent_converter';

describe('FormcomponentConverter', () => {
  let converterService: ConverterService<any>;
  let typesRegistry: TypesRegistry;
  let loggerFactory: LoggerFactory;
  let sabloService: SabloService;
  let viewportService: ViewportService;
  let sabloDeferHelper: SabloDeferHelper;
  let formComponentType: FormcomponentType;
  let propertyContext: IPropertyContext;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ServoyTestingModule],
      providers: [ConverterService, LoggerFactory, WindowRefService, ServicesService, SessionStorageService, ViewportService, LoadingIndicatorService],
    });

    sabloService = TestBed.inject(SabloService);
    sabloService.connect({}, {}, '');
    sabloDeferHelper = TestBed.inject(SabloDeferHelper);
    viewportService = TestBed.inject(ViewportService);
    loggerFactory = TestBed.inject(LoggerFactory);
    converterService = TestBed.inject(ConverterService);
    const uiBlockerService = new UIBlockerService(TestBed.inject(ServoyService));
    typesRegistry = TestBed.inject(TypesRegistry);
    typesRegistry.registerGlobalType(FoundsetType.TYPE_NAME, new FoundsetType(sabloService, sabloDeferHelper, viewportService, loggerFactory));
    typesRegistry.registerGlobalType(ComponentType.TYPE_NAME, new ComponentType(converterService, typesRegistry, loggerFactory, viewportService, sabloService, uiBlockerService));
    typesRegistry.registerGlobalType(DateType.TYPE_NAME_SABLO, new DateType());
    typesRegistry.registerGlobalType(ObjectType.TYPE_NAME, new ObjectType(typesRegistry, converterService, loggerFactory), true);

    typesRegistry.addComponentClientSideSpecs({
      'test-datalabel': {
        p: {
          dp: { s: 2 },
        },
      },
    });

    formComponentType = new FormcomponentType(converterService, typesRegistry);
    typesRegistry.registerGlobalType(FormcomponentType.TYPE_NAME, formComponentType);

    propertyContext = {
      getProperty: () => undefined,
      getPushToServerCalculatedValue: () => PushToServerEnum.ALLOW,
      isInsideModel: true,
    } as IPropertyContext;
  });

  const childElement = () => ({
    componentDirectiveName: 'test-datalabel',
    name: 'fc$datalabel_1',
    position: { left: '0px', top: '0px', width: '80px', height: '30px' },
    model: { dp: 'Hello' },
    handlers: {},
  });

  const fullServerValue = () => ({
    absoluteLayout: true,
    childElements: [childElement()],
    formHeight: 300,
    formWidth: 400,
    useCssPosition: false,
    uuid: 'the-uuid',
  });

  describe('fromServerToClient', () => {
    it('should return null when server value is null', () => {
      expect(formComponentType.fromServerToClient(null, null as any, propertyContext)).toBeNull();
    });

    it('should build a FormComponentValue from the full server value', () => {
      const value = formComponentType.fromServerToClient(fullServerValue(), null as any, propertyContext);
      expect(value).toBeInstanceOf(FormComponentValue);
      expect(value.absoluteLayout).toBe(true);
      expect(value.formHeight).toBe(300);
      expect(value.formWidth).toBe(400);
      expect(value.useCssPosition).toBe(false);
      expect(value.uuid).toBe('the-uuid');
      expect(value.childElements.length).toBe(1);
      expect(value.childElements[0].model.dp).toBe('Hello');
    });

    it('should reuse an existing client value and refresh its child elements', () => {
      const first = formComponentType.fromServerToClient(fullServerValue(), null as any, propertyContext);
      const second = formComponentType.fromServerToClient(fullServerValue(), first, propertyContext);
      expect(second).toBe(first);
      expect(second.childElements.length).toBe(1);
    });

    it('should propagate child element changes to markAllChanged on the form component value', () => {
      const value = formComponentType.fromServerToClient(fullServerValue(), null as any, propertyContext);
      let notified = false;
      value.getInternalState().setChangeListener(() => {
        notified = true;
      });

      // the converter wires a change listener on each child that calls markAllChanged on the form component value;
      // simulate the child notifying a change (which is what the child's own internal state would do)
      value.childElements[0].getInternalState().notifyChangeListener();
      expect(notified).toBe(true);
      expect(value.getInternalState().hasChanges()).toBe(true);
    });
  });

  describe('fromClientToServer', () => {
    it('should return null when no client data is given', () => {
      expect(formComponentType.fromClientToServer(null as any, null as any, propertyContext)).toBeNull();
    });

    it('should convert each child element to server and clear changes afterwards', () => {
      const value = formComponentType.fromServerToClient(fullServerValue(), null as any, propertyContext);
      value.getInternalState().setChangeListener(() => undefined);
      value.markAllChanged(true);

      const [changes, returned] = formComponentType.fromClientToServer(value, value, propertyContext)!;
      expect(Array.isArray(changes)).toBe(true);
      expect(changes.length).toBe(1);
      expect(returned).toBe(value);
      expect(value.getInternalState().hasChanges()).toBe(false);
    });

    it('should return a null changes array when there are no child elements', () => {
      const value = new FormComponentValue(true, undefined as any, 10, 20, false, 'u');
      const result = formComponentType.fromClientToServer(value, value, propertyContext)!;
      expect(result[0]).toBeNull();
      expect(result[1]).toBe(value);
    });
  });

  describe('FormComponentValue.uiDestroyed', () => {
    it('should not throw and should call uiDestroyed on child elements', () => {
      const value = formComponentType.fromServerToClient(fullServerValue(), null as any, propertyContext);
      expect(() => value.uiDestroyed()).not.toThrow();
    });

    it('should not throw when there are no child elements', () => {
      const value = new FormComponentValue(true, undefined as any, 10, 20, false, 'u');
      expect(() => value.uiDestroyed()).not.toThrow();
    });
  });
});
