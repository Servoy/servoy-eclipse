import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { LocalStorageService } from './localstorage.service';
import { LoggerFactory, LoggerService } from '../logger.service';

const createLoggerFactoryMock = (): LoggerFactory => {
    const logger = { error: vi.fn() } as unknown as LoggerService;
    return { getLogger: vi.fn().mockReturnValue(logger) } as unknown as LoggerFactory;
};

describe('LocalStorageService', () => {
    let service: LocalStorageService;
    let loggerFactory: LoggerFactory;

    beforeEach(() => {
        localStorage.clear();
        loggerFactory = createLoggerFactoryMock();
        service = new LocalStorageService(loggerFactory);
    });

    afterEach(() => {
        localStorage.clear();
        vi.restoreAllMocks();
    });

    it('should report localStorage as supported in jsdom', () => {
        expect(service.hasLocalStorage).toBe(true);
        expect(service.isSupported()).toBe(true);
    });

    it('should set and get a JSON value', () => {
        expect(service.set('foo', { a: 1 })).toBe(true);
        expect(service.get('foo')).toEqual({ a: 1 });
    });

    it('should store values as JSON strings under the prefix', () => {
        service.changePrefix('px_');
        service.set('bar', 'baz');
        expect(localStorage.getItem('px_bar')).toBe(JSON.stringify('baz'));
        expect(service.get('bar')).toBe('baz');
    });

    it('should return null from get when key is missing', () => {
        expect(service.get('missing')).toBeNull();
    });

    it('has should be true only when the value exists', () => {
        expect(service.has('k')).toBe(false);
        service.set('k', 10);
        expect(service.has('k')).toBe(true);
    });

    it('should remove a key', () => {
        service.set('k', 1);
        expect(service.remove('k')).toBe(true);
        expect(service.get('k')).toBeNull();
    });

    it('clear should empty the whole store when no prefix', () => {
        service.set('a', 1);
        service.set('b', 2);
        expect(service.clear()).toBe(true);
        expect(localStorage.length).toBe(0);
    });

    it('clear should only remove prefixed keys when a prefix is set', () => {
        service.changePrefix('mine_');
        service.set('a', 1);
        localStorage.setItem('other', 'x');
        expect(service.clear()).toBe(true);
        expect(localStorage.getItem('mine_a')).toBeNull();
        expect(localStorage.getItem('other')).toBe('x');
    });

    it('key should return the key name at an index', () => {
        service.set('onlyone', 42);
        expect(service.key(0)).toBe('onlyone');
    });

    it('length should reflect the number of stored items', () => {
        expect(service.length()).toBe(0);
        service.set('x', 1);
        expect(service.length()).toBe(1);
    });

    describe('error handling / unsupported store', () => {
        it('set should log and return false when localStorage.setItem throws', () => {
            vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
                throw new Error('quota');
            });
            expect(service.set('k', 1)).toBe(false);
            expect(loggerFactory.getLogger('LocalStorageService').error).toHaveBeenCalled();
        });

        it('get should log and return null when JSON.parse fails', () => {
            localStorage.setItem('bad', '{not-json');
            expect(service.get('bad')).toBeNull();
            expect(loggerFactory.getLogger('LocalStorageService').error).toHaveBeenCalled();
        });

        it('remove should log and return false when removeItem throws', () => {
            vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => {
                throw new Error('fail');
            });
            expect(service.remove('k')).toBe(false);
            expect(loggerFactory.getLogger('LocalStorageService').error).toHaveBeenCalled();
        });

        it('all mutating ops should return false / defaults when storage is unsupported', () => {
            service.hasLocalStorage = false;
            expect(service.set('k', 1)).toBe(false);
            expect(service.get('k')).toBeNull();
            expect(service.remove('k')).toBe(false);
            expect(service.clear()).toBe(false);
            expect(service.key(0)).toBeNull();
            expect(service.length()).toBe(0);
        });

        it('isSupported should return false when setItem throws', () => {
            vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
                throw new Error('disabled');
            });
            expect(service.isSupported()).toBe(false);
        });
    });
});
