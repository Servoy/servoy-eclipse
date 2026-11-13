import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed, inject } from '@angular/core/testing';

import { ConverterService, isChanged, instanceOfChangeAwareValue, instanceOfUIDestroyAwareValue, SoftProxyRevoker } from './converter.service';
import { DateType } from './converters/date_converter';
import { ObjectType } from './converters/object_converter';
import { PushToServerUtils, IType } from './types_registry';

import { SpecTypesService, LoggerFactory } from '@servoy/public';
import { WindowRefService } from '@servoy/public';

describe('ConverterService', () => {
  let service: ConverterService<unknown>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ConverterService, SpecTypesService, LoggerFactory, WindowRefService],
    });
    service = TestBed.inject(ConverterService);
  });

  it('should be created', inject([ConverterService], (s: ConverterService<unknown>) => {
    expect(s).toBeTruthy();
  }));

  it('registers the global date and object types on construction', () => {
    expect((service as any).typesRegistry.getAlreadyRegisteredType(DateType.TYPE_NAME_SABLO)).toBeInstanceOf(DateType);
    expect((service as any).typesRegistry.getAlreadyRegisteredType(DateType.TYPE_NAME_SVY)).toBeInstanceOf(DateType);
    expect((service as any).typesRegistry.getAlreadyRegisteredType(ObjectType.TYPE_NAME)).toBeInstanceOf(ObjectType);
  });

  describe('convertFromServerToClient', () => {
    const noContext = PushToServerUtils.PROPERTY_CONTEXT_FOR_INCOMMING_ARGS_AND_RETURN_VALUES;

    it('delegates to the given type and clears any previous dynamic type', () => {
      const type: IType<any> = {
        fromServerToClient: vi.fn().mockReturnValue('converted'),
        fromClientToServer: vi.fn(),
      };
      const dynamicHolder: Record<string, IType<any>> = { key: {} as any };
      const result = service.convertFromServerToClient('raw', type, undefined, dynamicHolder, 'key', noContext);
      expect(result).toBe('converted');
      expect(type.fromServerToClient).toHaveBeenCalled();
      expect(dynamicHolder['key']).toBeUndefined();
    });

    it('returns the raw value unchanged when no type and no conversion marker are present', () => {
      expect(service.convertFromServerToClient('plain', undefined, undefined, undefined!, undefined!, noContext)).toBe('plain');
    });

    it('uses a dynamic type looked up from the conversion marker and stores it', () => {
      const dateMarkerValue = { _T: DateType.TYPE_NAME_SABLO, _V: '2018-01-01T00:00:00' };
      const dynamicHolder: Record<string, IType<any>> = {};
      const result = service.convertFromServerToClient(dateMarkerValue, undefined, undefined, dynamicHolder, 'prop', noContext) as Date;
      expect(result).toBeInstanceOf(Date);
      expect(dynamicHolder['prop']).toBeInstanceOf(DateType);
    });

    it('logs an error when the marker references an unknown type', () => {
      const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
      (service as any).log.logLevel = 1;
      service.convertFromServerToClient({ _T: 'noSuchType', _V: 1 }, undefined, undefined, undefined!, undefined!, noContext);
      expect(errorSpy).toHaveBeenCalled();
      errorSpy.mockRestore();
    });
  });

  describe('convertFromClientToServer', () => {
    const outgoing = PushToServerUtils.PROPERTY_CONTEXT_FOR_OUTGOING_ARGS_AND_RETURN_VALUES;

    it('delegates to the given type', () => {
      const type: IType<any> = {
        fromServerToClient: vi.fn(),
        fromClientToServer: vi.fn().mockReturnValue(['json', 'client']),
      };
      expect(service.convertFromClientToServer('v', type, undefined, outgoing)).toEqual(['json', 'client']);
    });

    it('falls back to the registered object type when no type is given', () => {
      const [json] = service.convertFromClientToServer({ a: 1 }, undefined, undefined, outgoing);
      expect(json).toEqual({ a: 1 });
    });

    it('returns [null, newClientData] when a type returns null', () => {
      const type: IType<any> = { fromServerToClient: vi.fn(), fromClientToServer: vi.fn().mockReturnValue(null) };
      expect(service.convertFromClientToServer('x', type, undefined, outgoing)).toEqual([null, 'x']);
    });

    it('replaces an undefined JSON value with null', () => {
      const type: IType<any> = { fromServerToClient: vi.fn(), fromClientToServer: vi.fn().mockReturnValue([undefined, 'client']) };
      expect(service.convertFromClientToServer('x', type, undefined, outgoing)).toEqual([null, 'client']);
    });
  });

  describe('getOrCreateInDepthProperty', () => {
    it('returns undefined for no args', () => {
      expect(service.getOrCreateInDepthProperty()).toBeUndefined();
    });

    it('returns the object itself with a single arg', () => {
      const o = { a: 1 };
      expect(service.getOrCreateInDepthProperty(o)).toBe(o);
    });

    it('creates missing intermediate objects', () => {
      const root: any = {};
      const leaf = service.getOrCreateInDepthProperty(root, 'a', 'b', 'c');
      expect(leaf).toEqual({});
      expect(root.a.b.c).toBe(leaf);
    });

    it('returns null/undefined roots unchanged', () => {
      expect(service.getOrCreateInDepthProperty(null, 'a')).toBeNull();
    });
  });

  describe('getInDepthProperty', () => {
    it('returns undefined for no args', () => {
      expect(service.getInDepthProperty()).toBeUndefined();
    });

    it('returns a nested value when present', () => {
      expect(service.getInDepthProperty({ a: { b: { c: 7 } } }, 'a', 'b', 'c')).toBe(7);
    });

    it('returns undefined when an intermediate is missing', () => {
      expect(service.getInDepthProperty({ a: {} }, 'a', 'b', 'c')).toBeUndefined();
    });

    it('returns the missing value directly if it is the last key', () => {
      expect(service.getInDepthProperty({ a: {} }, 'a', 'missing')).toBeUndefined();
    });
  });

  describe('getEventArgs', () => {
    it('converts a plain argument through the type system', () => {
      const handlerSpec: any = { getArgumentType: () => undefined };
      const result = service.getEventArgs([{ plain: true }], 'onClick', handlerSpec);
      expect(result[0]).toEqual({ plain: true });
    });

    it('maps a MouseEvent into a Servoy event object', () => {
      const mouseEvent = new MouseEvent('click', { shiftKey: true, clientX: 10, clientY: 20 });
      Object.defineProperty(mouseEvent, 'pageX', { value: 10, configurable: true });
      Object.defineProperty(mouseEvent, 'pageY', { value: 20, configurable: true });
      const result = service.getEventArgs([mouseEvent], 'onClick', { getArgumentType: () => undefined } as any);
      expect(result[0].type).toBe('event');
      expect(result[0].eventName).toBe('onClick');
      expect(result[0].x).toBe(10);
      expect(result[0].y).toBe(20);
    });

    it('maps a generic Event into a Servoy event object', () => {
      const evt = new Event('change');
      const result = service.getEventArgs([evt], 'onChange', { getArgumentType: () => undefined } as any);
      expect(result[0].type).toBe('event');
      expect(result[0].eventName).toBe('onChange');
    });

    it('unwraps an originalEvent before processing', () => {
      const inner = new MouseEvent('click');
      const wrapper = { originalEvent: inner };
      const result = service.getEventArgs([wrapper], 'onClick', { getArgumentType: () => undefined } as any);
      expect(result[0].type).toBe('event');
    });
  });

  describe('static helpers', () => {
    it('getCombinedPropertyNames unions keys of both objects', () => {
      const combined = ConverterService.getCombinedPropertyNames({ a: 1, b: 2 }, { b: 3, c: 4 });
      expect(Object.keys(combined).sort()).toEqual(['a', 'b', 'c']);
    });

    it('getCombinedPropertyNames handles null prev/now', () => {
      expect(ConverterService.getCombinedPropertyNames({ a: 1 }, null)).toEqual({ a: true });
    });
  });
});

describe('isChanged', () => {
  it('returns true when a change-aware value reports changes', () => {
    const val = { getInternalState: () => ({ hasChanges: () => true }) };
    expect(isChanged(val, val)).toBe(true);
  });

  it('returns true for same-reference objects (nested changes possible)', () => {
    const o = { a: 1 };
    expect(isChanged(o, o)).toBe(true);
  });

  it('returns false for equal primitives', () => {
    expect(isChanged(5, 5)).toBe(false);
  });

  it('detects differing array lengths', () => {
    expect(isChanged([1, 2], [1])).toBe(true);
  });

  it('compares dates by time', () => {
    expect(isChanged(new Date(1000), new Date(1000))).toBe(false);
    expect(isChanged(new Date(1000), new Date(2000))).toBe(true);
  });

  it('deep-compares plain objects', () => {
    expect(isChanged({ a: 1 }, { a: 1 })).toBe(false);
    expect(isChanged({ a: 1 }, { a: 2 })).toBe(true);
  });

  it('returns true when only one side is set', () => {
    expect(isChanged(1, null)).toBe(true);
  });
});

describe('type guards', () => {
  it('instanceOfChangeAwareValue', () => {
    expect(instanceOfChangeAwareValue(null)).toBe(false);
    expect(instanceOfChangeAwareValue({})).toBe(false);
    expect(instanceOfChangeAwareValue({ getInternalState: () => ({}) })).toBe(true);
  });

  it('instanceOfUIDestroyAwareValue', () => {
    expect(instanceOfUIDestroyAwareValue(null)).toBe(false);
    expect(instanceOfUIDestroyAwareValue({ uiDestroyed: () => undefined })).toBe(true);
  });
});

describe('SoftProxyRevoker', () => {
  it('starts enabled and becomes disabled after the revoke function is called', () => {
    const log = { info: vi.fn() } as any;
    const revoker = new SoftProxyRevoker(log);
    expect(revoker.isProxyDisabled()).toBe(false);
    revoker.getRevokeFunction()();
    expect(revoker.isProxyDisabled()).toBe(true);
    expect(log.info).toHaveBeenCalled();
  });
});
