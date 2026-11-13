import { describe, it, expect, beforeEach, vi } from 'vitest';
import { LoggerFactory, LoggerService, LogConfiguration, LogLevel, ServoyWindow } from './logger.service';
import { WindowRefService } from './services/windowref.service';

const createConsoleMock = (): Console => ({
    debug: vi.fn(),
    info: vi.fn(),
    warn: vi.fn(),
    error: vi.fn(),
    log: vi.fn()
} as unknown as Console);

const createWindowRef = (win: Partial<ServoyWindow>): WindowRefService =>
    ({ nativeWindow: win as any } as WindowRefService);

describe('LoggerService', () => {
    let consoleMock: Console;
    let windowRef: WindowRefService;

    beforeEach(() => {
        consoleMock = createConsoleMock();
        windowRef = createWindowRef({ console: consoleMock });
    });

    it('error should log when level allows it', () => {
        const logger = new LoggerService(windowRef, new LogConfiguration(false, LogLevel.ERROR), 'Test');
        logger.error('boom');
        expect(consoleMock.error).toHaveBeenCalled();
    });

    it('warn should be a noop when the level is below WARN', () => {
        const logger = new LoggerService(windowRef, new LogConfiguration(false, LogLevel.ERROR), 'Test');
        logger.warn('msg');
        expect(consoleMock.warn).not.toHaveBeenCalled();
    });

    it('info/debug/spam should respect the configured level', () => {
        const logger = new LoggerService(windowRef, new LogConfiguration(false, LogLevel.SPAM), 'Test');
        logger.info('i');
        logger.debug('d');
        logger.spam('s');
        expect(consoleMock.info).toHaveBeenCalled();
        expect(consoleMock.debug).toHaveBeenCalledTimes(2); // debug + spam both go to console.debug
    });

    it('debug mode forces debug/info/warn/error to be enabled regardless of level', () => {
        const logger = new LoggerService(windowRef, new LogConfiguration(true, LogLevel.ERROR), 'Test');
        logger.debug('d');
        logger.info('i');
        logger.warn('w');
        expect(consoleMock.debug).toHaveBeenCalled();
        expect(consoleMock.info).toHaveBeenCalled();
        expect(consoleMock.warn).toHaveBeenCalled();
    });

    it('spam should be a noop when level is below SPAM', () => {
        const logger = new LoggerService(windowRef, new LogConfiguration(false, LogLevel.DEBUG), 'Test');
        logger.spam('s');
        // only the single debug call below should hit the console
        logger.debug('d');
        expect(consoleMock.debug).toHaveBeenCalledTimes(1);
    });

    describe('buildMessage', () => {
        it('should return undefined when logging is disabled', () => {
            const logger = new LoggerService(windowRef, new LogConfiguration(false, LogLevel.ERROR), 'Test');
            // warn is disabled at ERROR level -> enabled flag stays false (reading the getter has the side effect of setting it)
            void logger.warn;
            expect(logger.buildMessage('hello')).toBeUndefined();
        });

        it('should resolve a function message when enabled', () => {
            const logger = new LoggerService(windowRef, new LogConfiguration(false, LogLevel.ERROR), 'Test');
            void logger.error; // reading the getter enables logging
            expect(logger.buildMessage(() => 'computed')).toBe('computed');
        });

        it('should return a plain string message when enabled', () => {
            const logger = new LoggerService(windowRef, new LogConfiguration(false, LogLevel.ERROR), 'Test');
            void logger.error; // reading the getter enables logging
            expect(logger.buildMessage('plain')).toBe('plain');
        });
    });

    describe('log level accessors', () => {
        it('toggleDebugMode should flip the debug flag', () => {
            const config = new LogConfiguration(false, LogLevel.WARN);
            const logger = new LoggerService(windowRef, config, 'Test');
            expect(logger.toggleDebugMode()).toBe(true);
            expect(config.isDebugMode).toBe(true);
            expect(logger.toggleDebugMode()).toBe(false);
        });

        it('should get and set logLevel', () => {
            const logger = new LoggerService(windowRef, new LogConfiguration(false, LogLevel.WARN), 'Test');
            expect(logger.logLevel).toBe(LogLevel.WARN);
            logger.logLevel = LogLevel.DEBUG;
            expect(logger.logLevel).toBe(LogLevel.DEBUG);
        });
    });
});

describe('LoggerFactory', () => {
    let consoleMock: Console;

    beforeEach(() => {
        consoleMock = createConsoleMock();
    });

    it('should initialize a default configuration on the window when none exists', () => {
        const win: Partial<ServoyWindow> = { console: consoleMock };
        const factory = new LoggerFactory(createWindowRef(win));
        expect(win.logFactory).toBe(factory);
        expect(win.svyLogConfiguration).toBeInstanceOf(LogConfiguration);
        expect(win.logLevels!['error']).toBe(LogLevel.ERROR);
    });

    it('should reuse an existing window configuration', () => {
        const existing = new LogConfiguration(true, LogLevel.DEBUG);
        const win: Partial<ServoyWindow> = { console: consoleMock, svyLogConfiguration: existing };
        new LoggerFactory(createWindowRef(win));
        // existing config kept (isDebugMode true), logLevels not re-created
        expect(win.svyLogConfiguration).toBe(existing);
    });

    it('getLogger should cache logger instances per class name', () => {
        const win: Partial<ServoyWindow> = { console: consoleMock };
        const factory = new LoggerFactory(createWindowRef(win));
        const a = factory.getLogger('Foo');
        const b = factory.getLogger('Foo');
        const c = factory.getLogger('Bar');
        expect(a).toBe(b);
        expect(a).not.toBe(c);
    });
});
