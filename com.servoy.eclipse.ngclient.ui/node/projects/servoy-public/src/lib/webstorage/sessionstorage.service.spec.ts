import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { SessionStorageService } from './sessionstorage.service';
import { LoggerFactory, LoggerService } from '../logger.service';

const createLoggerFactoryMock = (): LoggerFactory => {
    const logger = { error: vi.fn() } as unknown as LoggerService;
    return { getLogger: vi.fn().mockReturnValue(logger) } as unknown as LoggerFactory;
};

describe('SessionStorageService', () => {
    let service: SessionStorageService;
    let loggerFactory: LoggerFactory;

    beforeEach(() => {
        sessionStorage.clear();
        loggerFactory = createLoggerFactoryMock();
        service = new SessionStorageService(loggerFactory);
    });

    afterEach(() => {
        sessionStorage.clear();
        vi.restoreAllMocks();
    });

    it('should report sessionStorage as supported in jsdom', () => {
        expect(service.hasSessionStorage).toBe(true);
        expect(service.isSupported()).toBe(true);
    });

    it('should set and get a JSON value', () => {
        expect(service.set('foo', [1, 2, 3])).toBe(true);
        expect(service.get('foo')).toEqual([1, 2, 3]);
    });

    it('should use the prefix for keys', () => {
        service.changePrefix('s_');
        service.set('bar', 'baz');
        expect(sessionStorage.getItem('s_bar')).toBe(JSON.stringify('baz'));
    });

    it('has should reflect existence', () => {
        expect(service.has('k')).toBe(false);
        service.set('k', true);
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
        expect(sessionStorage.length).toBe(0);
    });

    it('clear should only remove prefixed keys when a prefix is set', () => {
        service.changePrefix('mine_');
        service.set('a', 1);
        sessionStorage.setItem('other', 'x');
        expect(service.clear()).toBe(true);
        expect(sessionStorage.getItem('mine_a')).toBeNull();
        expect(sessionStorage.getItem('other')).toBe('x');
    });

    it('length should reflect the number of stored items', () => {
        expect(service.length()).toBe(0);
        service.set('x', 1);
        expect(service.length()).toBe(1);
    });

    describe('error handling / unsupported store', () => {
        it('set should log and return false when setItem throws', () => {
            vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
                throw new Error('quota');
            });
            expect(service.set('k', 1)).toBe(false);
            expect(loggerFactory.getLogger('SessionStorageService').error).toHaveBeenCalled();
        });

        it('get should log and return null when JSON.parse fails', () => {
            sessionStorage.setItem('bad', '{not-json');
            expect(service.get('bad')).toBeNull();
            expect(loggerFactory.getLogger('SessionStorageService').error).toHaveBeenCalled();
        });

        it('remove should log and return false when removeItem throws', () => {
            vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => {
                throw new Error('fail');
            });
            expect(service.remove('k')).toBe(false);
        });

        it('all mutating ops should return false / defaults when storage is unsupported', () => {
            service.hasSessionStorage = false;
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
