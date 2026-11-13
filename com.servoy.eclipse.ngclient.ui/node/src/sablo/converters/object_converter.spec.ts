import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { ObjectType } from './object_converter';
import { DateType } from './date_converter';
import { ConverterService } from '../converter.service';
import { TypesRegistry, PushToServerUtils, IPropertyContext } from '../types_registry';
import { LoggerFactory, WindowRefService } from '@servoy/public';

describe('ObjectType', () => {
  let objectType: ObjectType;
  let typesRegistry: TypesRegistry;
  const ctx: IPropertyContext = PushToServerUtils.PROPERTY_CONTEXT_FOR_OUTGOING_ARGS_AND_RETURN_VALUES;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [ConverterService, TypesRegistry, LoggerFactory, WindowRefService] });
    TestBed.inject(ConverterService);
    typesRegistry = TestBed.inject(TypesRegistry);
    // ConverterService construction already registers date + object; grab the registered object type
    objectType = typesRegistry.getAlreadyRegisteredType(ObjectType.TYPE_NAME) as ObjectType;
  });

  describe('fromServerToClient', () => {
    it('returns primitives unchanged', () => {
      expect(objectType.fromServerToClient('hello', undefined, ctx)).toBe('hello');
      expect(objectType.fromServerToClient(42, undefined, ctx)).toBe(42);
    });

    it('converts a value that itself carries a conversion marker', () => {
      const value = { [ConverterService.CONVERSION_CL_SIDE_TYPE_KEY]: DateType.TYPE_NAME_SABLO, [ConverterService.VALUE_KEY]: '2020-05-05T00:00:00' };
      const result = objectType.fromServerToClient(value, undefined, ctx) as Date;
      expect(result).toBeInstanceOf(Date);
      expect(result.getFullYear()).toBe(2020);
    });

    it('converts nested sub-properties that carry a conversion marker', () => {
      const value = {
        plain: 'x',
        when: { [ConverterService.CONVERSION_CL_SIDE_TYPE_KEY]: DateType.TYPE_NAME_SABLO, [ConverterService.VALUE_KEY]: '2021-06-06T00:00:00' },
      };
      const result = objectType.fromServerToClient(value, undefined, ctx) as any;
      expect(result.plain).toBe('x');
      expect(result.when).toBeInstanceOf(Date);
      expect(result.when.getFullYear()).toBe(2021);
    });

    it('converts nested array elements that carry a conversion marker', () => {
      const value = [{ [ConverterService.CONVERSION_CL_SIDE_TYPE_KEY]: DateType.TYPE_NAME_SABLO, [ConverterService.VALUE_KEY]: '2019-03-03T00:00:00' }];
      const result = objectType.fromServerToClient(value, undefined, ctx) as any[];
      expect(result[0]).toBeInstanceOf(Date);
    });
  });

  describe('fromClientToServer', () => {
    it('returns primitives unchanged', () => {
      expect(objectType.fromClientToServer('abc', undefined, ctx)).toEqual(['abc', 'abc']);
      expect(objectType.fromClientToServer(7, undefined, ctx)).toEqual([7, 7]);
    });

    it('wraps a Date with its server-side type marker', () => {
      const date = new Date(2020, 0, 1, 0, 0, 0, 0);
      const [json, clientVal] = objectType.fromClientToServer(date, undefined, ctx);
      expect(json[ConverterService.CONVERSION_CL_SIDE_TYPE_KEY]).toBe('date');
      expect(typeof json[ConverterService.VALUE_KEY]).toBe('string');
      expect(clientVal).toBe(date);
    });

    it('recurses into nested objects, converting nested dates', () => {
      const obj = { a: 1, d: new Date(2020, 0, 1) };
      const [json] = objectType.fromClientToServer(obj, undefined, ctx);
      expect(json.a).toBe(1);
      expect(json.d[ConverterService.CONVERSION_CL_SIDE_TYPE_KEY]).toBe('date');
    });

    it('recurses into arrays', () => {
      const arr = [new Date(2020, 0, 1), 'plain'];
      const [json] = objectType.fromClientToServer(arr, undefined, ctx);
      expect(json[0][ConverterService.CONVERSION_CL_SIDE_TYPE_KEY]).toBe('date');
      expect(json[1]).toBe('plain');
    });

    it('detects circular references, nulls the cycle and logs an error', () => {
      const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
      (objectType as any).log.logLevel = 1;
      const a: any = { name: 'a' };
      a.self = a;
      const [json] = objectType.fromClientToServer(a, undefined, ctx);
      // the recursive self reference becomes null
      expect(json.self).toBeNull();
      expect(errorSpy).toHaveBeenCalled();
      errorSpy.mockRestore();
    });

    it('leaves DOM Event and Element values untouched', () => {
      const evt = new Event('x');
      expect(objectType.fromClientToServer(evt, undefined, ctx)).toEqual([evt, evt]);
    });
  });
});
