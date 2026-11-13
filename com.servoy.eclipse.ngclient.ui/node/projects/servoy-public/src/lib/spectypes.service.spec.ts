import { describe, it, expect, beforeEach, vi } from 'vitest';
import {
    SpecTypesService, BaseCustomObject, BaseCustomObjectState, ArrayState,
    getDeprecatedCustomArrayState
} from './spectypes.service';
import { LoggerFactory, LoggerService } from './logger.service';

const createLoggerFactoryMock = (): LoggerFactory => {
    const logger = {
        info: vi.fn(),
        buildMessage: vi.fn((m: any) => {
            return typeof m === 'function' ? m() : m;
        })
    } as unknown as LoggerService;
    return { getLogger: vi.fn().mockReturnValue(logger) } as unknown as LoggerFactory;
};

describe('SpecTypesService', () => {
    let service: SpecTypesService;
    let loggerFactory: LoggerFactory;

    beforeEach(() => {
        loggerFactory = createLoggerFactoryMock();
        service = new SpecTypesService(loggerFactory);
    });

    it('should register and return a custom object type constructor', () => {
        class MyType {
            foo = 1;
        }
        service.registerCustomObjectType('mycomp.myType', MyType as any);
        expect(service.getRegisteredCustomObjectTypeConstructor('mycomp.myType')).toBe(MyType);
    });

    it('should return undefined for an unregistered type', () => {
        expect(service.getRegisteredCustomObjectTypeConstructor('unknown')).toBeUndefined();
    });

    it('registerType (deprecated) should log and delegate to registerCustomObjectType', () => {
        class Legacy extends BaseCustomObject {
        }
        service.registerType('legacy.type', Legacy as any);
        expect(loggerFactory.getLogger('SpecTypesService').info).toHaveBeenCalled();
        expect(service.getRegisteredCustomObjectTypeConstructor('legacy.type')).toBe(Legacy);
    });
});

describe('deprecated spectype helpers', () => {
    it('BaseCustomObject.getStateHolder returns a shared state object', () => {
        const obj = new BaseCustomObject();
        const state = obj.getStateHolder();
        expect(state).toBeInstanceOf(BaseCustomObjectState);
        // shared instance between invocations
        expect(obj.getStateHolder()).toBe(state);
        expect(obj.getWatchedProperties()).toBeNull();
    });

    it('BaseCustomObjectState.getChangedKeys returns an empty set and setProperty assigns value', () => {
        const state = new BaseCustomObjectState();
        expect(state.getChangedKeys().size).toBe(0);
        const target: any = {};
        state.setPropertyAndHandleChanges(target, '_x', 'x', 42);
        expect(target._x).toBe(42);
        // these are no-ops but should not throw
        expect(() => state.markAllChanged(true)).not.toThrow();
        expect(() => state.notifyChangeListener()).not.toThrow();
    });

    it('getDeprecatedCustomArrayState returns a shared ArrayState', () => {
        const a = getDeprecatedCustomArrayState();
        const b = getDeprecatedCustomArrayState();
        expect(a).toBeInstanceOf(ArrayState);
        expect(a).toBe(b);
        expect(a.getChangedKeys().size).toBe(0);
        expect(() => {
            a.initDiffer();
            a.clearChanges();
        }).not.toThrow();
    });
});
