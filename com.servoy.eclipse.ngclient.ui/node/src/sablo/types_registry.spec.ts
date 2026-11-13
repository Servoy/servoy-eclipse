import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';

import {
  TypesRegistry,
  PushToServerEnum,
  PushToServerUtils,
  RootPropertyContextCreator,
  ChildPropertyContextCreator,
  PropertyContext,
  IType,
  ITypeFactory,
  IWebObjectSpecification,
} from './types_registry';
import { LoggerFactory, WindowRefService, LogLevel } from '@servoy/public';

const dummyType: IType<any> = {
  fromServerToClient: (v: any) => v,
  fromClientToServer: (v: any) => [v, v],
};

describe('TypesRegistry', () => {
  let registry: TypesRegistry;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [TypesRegistry, LoggerFactory, WindowRefService] });
    registry = TestBed.inject(TypesRegistry);
  });

  describe('global types', () => {
    it('registers and retrieves a global type', () => {
      registry.registerGlobalType('myType', dummyType);
      expect(registry.getAlreadyRegisteredType('myType')).toBe(dummyType);
    });

    it('throws when registering a null type', () => {
      expect(() => registry.registerGlobalType('x', null as any)).toThrow();
    });

    it('overwrites an existing type by default', () => {
      registry.registerGlobalType('t', dummyType);
      const other: IType<any> = { fromServerToClient: (v) => v, fromClientToServer: (v) => [v, v] };
      registry.registerGlobalType('t', other);
      expect(registry.getAlreadyRegisteredType('t')).toBe(other);
    });

    it('keeps the existing type when onlyIfNotAlreadyRegistered is true', () => {
      registry.registerGlobalType('t', dummyType);
      const other: IType<any> = { fromServerToClient: (v) => v, fromClientToServer: (v) => [v, v] };
      registry.registerGlobalType('t', other, true);
      expect(registry.getAlreadyRegisteredType('t')).toBe(dummyType);
    });

    it('logs an error and returns undefined for an unknown simple type', () => {
      const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
      (registry as any).logger.logLevel = LogLevel.ERROR;
      expect(registry.getAlreadyRegisteredType('missing')).toBeUndefined();
      expect(errorSpy).toHaveBeenCalled();
      errorSpy.mockRestore();
    });
  });

  describe('processTypeFromServer', () => {
    it('resolves a simple registered type', () => {
      registry.registerGlobalType('simple', dummyType);
      expect(registry.processTypeFromServer('simple', 'myComp')).toBe(dummyType);
    });

    it('logs an error for an unknown simple type', () => {
      const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
      (registry as any).logger.logLevel = LogLevel.ERROR;
      expect(registry.processTypeFromServer('nope', 'c')).toBeUndefined();
      expect(errorSpy).toHaveBeenCalled();
      errorSpy.mockRestore();
    });

    it('uses a registered factory for factory types', () => {
      const specificType = dummyType;
      const factory: ITypeFactory<any> = {
        getOrCreateSpecificType: vi.fn().mockReturnValue(specificType),
        registerDetails: vi.fn(),
      };
      registry.getTypeFactoryRegistry().contributeTypeFactory('myFactory', factory);
      const result = registry.processTypeFromServer(['myFactory', { info: 1 }], 'comp');
      expect(result).toBe(specificType);
      expect(factory.getOrCreateSpecificType).toHaveBeenCalledWith({ info: 1 }, 'comp');
    });

    it('logs an error for a missing factory', () => {
      const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
      (registry as any).logger.logLevel = LogLevel.ERROR;
      expect(registry.processTypeFromServer(['noFactory', {}], 'comp')).toBeUndefined();
      expect(errorSpy).toHaveBeenCalled();
      errorSpy.mockRestore();
    });
  });

  describe('processPropertyDescriptionFromServer', () => {
    beforeEach(() => registry.registerGlobalType('simple', dummyType));

    it('handles a bare type string', () => {
      const pd = registry.processPropertyDescriptionFromServer('simple', 'c')!;
      expect(pd.getPropertyType()).toBe(dummyType);
      expect(pd.getPropertyDeclaredPushToServer()).toBeUndefined();
      expect(pd.getPropertyPushToServer()).toBe(PushToServerEnum.REJECT);
    });

    it('handles a multi-entry description with a pushToServer value', () => {
      const pd = registry.processPropertyDescriptionFromServer({ t: 'simple', s: 2 }, 'c')!;
      expect(pd.getPropertyType()).toBe(dummyType);
      expect(pd.getPropertyDeclaredPushToServer()).toBe(PushToServerEnum.SHALLOW);
    });

    it('handles a multi-entry description without a type', () => {
      const pd = registry.processPropertyDescriptionFromServer({ s: 1 } as any, 'c')!;
      expect(pd.getPropertyType()).toBeUndefined();
      expect(pd.getPropertyDeclaredPushToServer()).toBe(PushToServerEnum.ALLOW);
    });
  });

  describe('component and service specifications', () => {
    beforeEach(() => registry.registerGlobalType('simple', dummyType));

    it('addComponentClientSideSpecs builds a retrievable spec with properties, handlers and apis', () => {
      registry.addComponentClientSideSpecs({
        myComp: {
          p: { myProp: { t: 'simple', s: 2 } },
          h: { onFoo: { 0: 'simple', iBDE: true } as any },
          a: { doApi: { r: 'simple', srv: true, 0: 'simple' } as any },
        },
      });
      const spec = registry.getComponentSpecification('myComp');
      expect(spec.getPropertyType('myProp')).toBe(dummyType);
      expect(spec.getPropertyPushToServer('myProp')).toBe(PushToServerEnum.SHALLOW);
      expect(spec.getPropertyDeclaredPushToServer('myProp')).toBe(PushToServerEnum.SHALLOW);
      expect(spec.getHandler('onFoo')!.ignoreNGBlockDuplicateEvents).toBe(true);
      const api = spec.getApiFunction('doApi')!;
      expect(api.shouldReturnValue).toBe(true);
      expect(api.returnType).toBe(dummyType);
      expect(api.getArgumentType(0)).toBe(dummyType);
    });

    it('setServiceClientSideSpecs replaces all service specs', () => {
      registry.setServiceClientSideSpecs({ svc1: { p: { a: 'simple' } } });
      expect(registry.getServiceSpecification('svc1')).toBeDefined();
      registry.setServiceClientSideSpecs({ svc2: {} });
      expect(registry.getServiceSpecification('svc1')).toBeUndefined();
      expect(registry.getServiceSpecification('svc2')).toBeDefined();
    });

    it('spec getters default sensibly for unknown properties', () => {
      registry.addComponentClientSideSpecs({ c: {} });
      const spec = registry.getComponentSpecification('c');
      expect(spec.getPropertyType('x')).toBeUndefined();
      expect(spec.getPropertyPushToServer('x')).toBe(PushToServerEnum.REJECT);
      expect(spec.getPropertyDeclaredPushToServer('x')).toBeUndefined();
      expect(spec.getPropertyDescriptions()).toBeUndefined();
      expect(spec.getHandler('x')).toBeUndefined();
      expect(spec.getApiFunction('x')).toBeUndefined();
    });

    it('processes factory type details', () => {
      const factory: ITypeFactory<any> = { getOrCreateSpecificType: vi.fn(), registerDetails: vi.fn() };
      registry.getTypeFactoryRegistry().contributeTypeFactory('JSON_obj', factory);
      registry.addComponentClientSideSpecs({ c: { ftd: { JSON_obj: { someType: {} } } } });
      expect(factory.registerDetails).toHaveBeenCalledWith({ someType: {} }, 'c');
    });
  });
});

describe('PushToServerUtils', () => {
  it('valueOf validates bounds', () => {
    expect(PushToServerUtils.valueOf(0)).toBe(PushToServerEnum.REJECT);
    expect(PushToServerUtils.valueOf(3)).toBe(PushToServerEnum.DEEP);
    expect(() => PushToServerUtils.valueOf(undefined as any)).toThrow();
    expect(() => PushToServerUtils.valueOf(5 as any)).toThrow();
  });

  describe('combineWithChildStatic', () => {
    it('REJECT parent yields REJECT', () => {
      expect(PushToServerUtils.combineWithChildStatic(PushToServerEnum.REJECT, PushToServerEnum.ALLOW)).toBe(PushToServerEnum.REJECT);
    });

    it('REJECT child yields REJECT', () => {
      expect(PushToServerUtils.combineWithChildStatic(PushToServerEnum.ALLOW, PushToServerEnum.REJECT)).toBe(PushToServerEnum.REJECT);
    });

    it('undefined parent is treated as REJECT', () => {
      expect(PushToServerUtils.combineWithChildStatic(undefined as any, PushToServerEnum.ALLOW)).toBe(PushToServerEnum.REJECT);
    });

    it('inherits the parent when the child is null', () => {
      expect(PushToServerUtils.combineWithChildStatic(PushToServerEnum.DEEP, null)).toBe(PushToServerEnum.DEEP);
    });

    it('child overrides the parent when present', () => {
      expect(PushToServerUtils.combineWithChildStatic(PushToServerEnum.SHALLOW, PushToServerEnum.DEEP)).toBe(PushToServerEnum.DEEP);
    });
  });

  it('creates root and child context creators', () => {
    expect(PushToServerUtils.newRootPropertyContextCreator(() => undefined, undefined!)).toBeInstanceOf(RootPropertyContextCreator);
    expect(PushToServerUtils.newChildPropertyContextCreator(() => undefined, {}, PushToServerEnum.ALLOW, true)).toBeInstanceOf(ChildPropertyContextCreator);
  });
});

describe('RootPropertyContextCreator', () => {
  it('uses the spec computed push-to-server value', () => {
    const spec = { getPropertyPushToServer: () => PushToServerEnum.SHALLOW } as unknown as IWebObjectSpecification;
    const creator = new RootPropertyContextCreator((n) => n, spec);
    const ctx = creator.withPushToServerFor('p');
    expect(ctx.getPushToServerCalculatedValue()).toBe(PushToServerEnum.SHALLOW);
    expect(ctx.isInsideModel).toBe(true);
  });

  it('defaults to REJECT without a spec', () => {
    const creator = new RootPropertyContextCreator((n) => n, undefined!);
    expect(creator.withPushToServerFor('p').getPushToServerCalculatedValue()).toBe(PushToServerEnum.REJECT);
  });
});

describe('ChildPropertyContextCreator', () => {
  it('combines parent and child declared push-to-server', () => {
    const pds = { child: { getPropertyDeclaredPushToServer: () => PushToServerEnum.DEEP } } as any;
    const creator = new ChildPropertyContextCreator((n) => n, pds, PushToServerEnum.ALLOW, false);
    const ctx = creator.withPushToServerFor('child');
    expect(ctx.getPushToServerCalculatedValue()).toBe(PushToServerEnum.DEEP);
    expect(ctx.isInsideModel).toBe(false);
  });
});

describe('PropertyContext', () => {
  it('exposes its computed value and getter', () => {
    const getter = (n: string) => 'val:' + n;
    const ctx = new PropertyContext(getter, PushToServerEnum.ALLOW, true);
    expect(ctx.getPushToServerCalculatedValue()).toBe(PushToServerEnum.ALLOW);
    expect(ctx.getProperty('x')).toBe('val:x');
    expect(ctx.isInsideModel).toBe(true);
  });
});
