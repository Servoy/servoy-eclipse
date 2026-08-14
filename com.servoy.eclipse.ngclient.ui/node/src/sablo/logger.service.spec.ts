import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed, inject } from '@angular/core/testing';

import { LoggerService, LogLevel, LoggerFactory } from '@servoy/public';
import { WindowRefService } from '@servoy/public';

describe('LoggerService', () => {
  let windowRef: any;

  beforeEach(() => {
    windowRef = { nativeWindow: { console: {} } };

    windowRef.nativeWindow.console = { debug: vi.fn(), info: vi.fn(), warn: vi.fn(), error: vi.fn() } as any;

    TestBed.configureTestingModule({
      providers: [LoggerFactory, { provide: WindowRefService, useFactory: () => windowRef }],
    });
  });

  it('should be created', inject([LoggerFactory], (logFactory: LoggerFactory) => {
    const log = logFactory.getLogger('LoggerService');
    expect(log).toBeTruthy();
  }));

  it('should not log anything but warn and errors', inject([LoggerFactory], (logFactory: LoggerFactory) => {
    //by default isDebugMode is false, so it should only log errors
    const log = logFactory.getLogger('LoggerService'); //by default isDebugMode is false

    log.debug('test');
    expect(windowRef.nativeWindow.console.debug).not.toHaveBeenCalled();

    log.info('test');
    expect(windowRef.nativeWindow.console.info).not.toHaveBeenCalled();

    log.warn('test');
    expect(windowRef.nativeWindow.console.warn).toHaveBeenCalled();

    log.error('ERROR!');
    expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith(expect.stringMatching(' ERROR LoggerService - '), 'ERROR!');

    log.warn(log.buildMessage(() => 'test'));
    expect(windowRef.nativeWindow.console.warn).toHaveBeenCalledWith(expect.stringMatching(' WARN LoggerService - '), 'test');
  }));

  it('should log error', inject([LoggerFactory], (logFactory: LoggerFactory) => {
    const log = logFactory.getLogger('LoggerService');
    log.logLevel = LogLevel.ERROR;

    log.warn('test');
    expect(windowRef.nativeWindow.console.warn).not.toHaveBeenCalled();

    log.error('ERROR!');
    expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith(expect.stringMatching(' ERROR LoggerService - '), 'ERROR!');
  }));

  it('should log info, warning, error', inject([LoggerFactory], (logFactory: LoggerFactory) => {
    const log = logFactory.getLogger('LoggerService');
    log.logLevel = LogLevel.INFO;

    log.spam('some spam');
    expect(windowRef.nativeWindow.console.debug).not.toHaveBeenCalled();

    log.debug('test');
    expect(windowRef.nativeWindow.console.debug).not.toHaveBeenCalled();

    log.info('some info');
    expect(windowRef.nativeWindow.console.info).toHaveBeenCalledWith(expect.stringMatching(' INFO LoggerService - '), 'some info');

    log.warn('warning...');
    expect(windowRef.nativeWindow.console.warn).toHaveBeenCalledWith(expect.stringMatching(' WARN LoggerService - '), 'warning...');

    log.error('ERROR!');
    expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith(expect.stringMatching(' ERROR LoggerService - '), 'ERROR!');
  }));

  it('should log debug ... error', inject([LoggerFactory], (logFactory: LoggerFactory) => {
    const log = logFactory.getLogger('LoggerService');
    log.logLevel = LogLevel.DEBUG;

    log.spam('some spam');
    expect(windowRef.nativeWindow.console.debug).not.toHaveBeenCalled();
    let stringResolved = false;
    log.spam(
      log.buildMessage(() => {
        stringResolved = true;
        return 'some spam' + 2;
      }),
    );
    expect(windowRef.nativeWindow.console.debug).not.toHaveBeenCalled();
    expect(stringResolved).toBeFalsy();

    log.debug('test');
    expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith(expect.stringMatching(' DEBUG LoggerService - '), 'test');

    log.debug(
      log.buildMessage(() => {
        stringResolved = true;
        return 'test' + 2;
      }),
    );
    expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith(expect.stringMatching(' DEBUG LoggerService - '), 'test2');
    expect(stringResolved).toBeTruthy();

    log.error('ERROR!');
    expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith(expect.stringMatching(' ERROR LoggerService - '), 'ERROR!');

    log.error(log.buildMessage(() => 'ERROR!' + 2));
    expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith(expect.stringMatching(' ERROR LoggerService - '), 'ERROR!2');
  }));

  it('should spam', inject([LoggerFactory], (logFactory: LoggerFactory) => {
    const log = logFactory.getLogger('LoggerService');
    log.logLevel = LogLevel.SPAM;

    log.spam('some spam');
    expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith(expect.stringMatching(' SPAM LoggerService - '), 'some spam');

    log.spam(log.buildMessage(() => 'some spam' + 2));
    expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith(expect.stringMatching(' SPAM LoggerService - '), 'some spam2');

    log.debug('test');
    expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith(expect.stringMatching(' DEBUG LoggerService - '), 'test');

    log.error('ERROR!');
    expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith(expect.stringMatching(' ERROR LoggerService - '), 'ERROR!');
  }));
});
