import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { SabloService } from './sablo.service';
import { WebsocketService } from './websocket.service';
import { ConverterService } from './converter.service';
import { WindowRefService, SessionStorageService, LoggerFactory, Deferred } from '@servoy/public';

describe('SabloService', () => {
  let windowRef: any;
  let sessionStorage: any;
  let websocketService: any;
  let fakeWindow: any;
  let sessionData: Record<string, any>;

  const buildWindow = (search = '?wraplogging=false') => {
    const consoleMock = { log: vi.fn(), info: vi.fn(), warn: vi.fn(), error: vi.fn(), debug: vi.fn() };
    const listeners: Record<string, (evt: any) => any> = {};
    return {
      listeners,
      window: {
        addEventListener: vi.fn().mockImplementation((name: string, fn: any) => (listeners[name] = fn)),
        console: consoleMock,
        location: { search, reload: vi.fn() },
      },
      navigator: { languages: ['en-US'], language: 'en-US' },
      console: consoleMock,
    };
  };

  beforeEach(() => {
    sessionData = {};
    fakeWindow = buildWindow();
    windowRef = { nativeWindow: fakeWindow };
    sessionStorage = {
      get: vi.fn().mockImplementation((k: string) => sessionData[k]),
      set: vi.fn().mockImplementation((k: string, v: any) => (sessionData[k] = v)),
      has: vi.fn().mockImplementation((k: string) => sessionData[k] !== undefined),
      remove: vi.fn().mockImplementation((k: string) => delete sessionData[k]),
    };
    websocketService = {
      connect: vi.fn(),
      setConnectionPathArguments: vi.fn(),
      getURLParameter: vi.fn(),
      getSession: vi.fn(),
      getCurrentRequestInfo: vi.fn().mockReturnValue('reqInfo'),
    };

    TestBed.configureTestingModule({
      providers: [
        SabloService,
        { provide: WindowRefService, useFactory: () => windowRef },
        { provide: SessionStorageService, useFactory: () => sessionStorage },
        { provide: WebsocketService, useFactory: () => websocketService },
        { provide: ConverterService, useValue: {} },
        LoggerFactory,
      ],
    });
  });

  const createService = () => TestBed.inject(SabloService);

  it('should be created and register unload listeners and a session lock', () => {
    const service = createService();
    expect(service).toBeTruthy();
    expect(fakeWindow.window.addEventListener).toHaveBeenCalledWith('beforeunload', expect.any(Function));
    expect(fakeWindow.window.addEventListener).toHaveBeenCalledWith('pagehide', expect.any(Function));
    expect(sessionStorage.set).toHaveBeenCalledWith('svy_session_lock', '1');
  });

  it('should clear sablo info if a stale session lock is found', () => {
    sessionData['svy_session_lock'] = '1';
    createService();
    expect(sessionStorage.remove).toHaveBeenCalledWith('windownr');
    expect(sessionStorage.remove).toHaveBeenCalledWith('clientnr');
  });

  describe('locale handling', () => {
    it('getLocale should build a locale from the browser language', () => {
      const service = createService();
      const locale = service.getLocale();
      expect(locale).toEqual({ language: 'en', country: 'US', full: 'en-US' });
    });

    it('getLocale should cache the locale', () => {
      const service = createService();
      const first = service.getLocale();
      expect(service.getLocale()).toBe(first);
    });

    it('setLocale should override the locale', () => {
      const service = createService();
      service.setLocale({ language: 'nl', country: 'NL', full: 'nl-NL' });
      expect(service.getLocale()).toEqual({ language: 'nl', country: 'NL', full: 'nl-NL' });
    });

    it('getLanguageAndCountryFromBrowser prefers a country-specific entry following a bare language', () => {
      fakeWindow.navigator.languages = ['en', 'en-GB'];
      const service = createService();
      expect(service.getLanguageAndCountryFromBrowser()).toBe('en-GB');
    });

    it('getLanguageAndCountryFromBrowser falls back to navigator.language when languages is empty', () => {
      fakeWindow.navigator.languages = [];
      fakeWindow.navigator.language = 'de-DE';
      const service = createService();
      expect(service.getLanguageAndCountryFromBrowser()).toBe('de-DE');
    });

    it('getLanguageAndCountryFromBrowser defaults to en when nothing is set', () => {
      fakeWindow.navigator.languages = undefined;
      fakeWindow.navigator.language = undefined;
      const service = createService();
      expect(service.getLanguageAndCountryFromBrowser()).toBe('en');
    });
  });

  describe('client/window identifiers', () => {
    it('getClientnr returns the session storage value when present', () => {
      sessionData['clientnr'] = '99';
      const service = createService();
      expect(service.getClientnr()).toBe('99');
      expect(websocketService.getURLParameter).not.toHaveBeenCalled();
    });

    it('getClientnr falls back to the URL parameter', () => {
      websocketService.getURLParameter.mockReturnValue('urlclient');
      const service = createService();
      expect(service.getClientnr()).toBe('urlclient');
      expect(websocketService.getURLParameter).toHaveBeenCalledWith('clientnr');
    });

    it('getWindowName delegates to the websocket URL parameter', () => {
      websocketService.getURLParameter.mockReturnValue('win1');
      const service = createService();
      expect(service.getWindowName()).toBe('win1');
      expect(websocketService.getURLParameter).toHaveBeenCalledWith('windowname');
    });

    it('getWindownr reads from session storage', () => {
      sessionData['windownr'] = '7';
      const service = createService();
      expect(service.getWindownr()).toBe('7');
    });

    it('getWindowUrl builds an encoded index url', () => {
      sessionData['clientnr'] = '5';
      const service = createService();
      expect(service.getWindowUrl('my window')).toBe('index.html?windowname=my%20window&clientnr=5');
    });
  });

  describe('expectFormToShowOnClient', () => {
    it('isExpectingAFormToShowSoon reflects a pending form show', async () => {
      const service = createService();
      expect(service.isExpectingAFormToShowSoon()).toBe(false);
      service.expectFormToShowOnClient(true);
      expect(service.isExpectingAFormToShowSoon()).toBe(true);
      const promise = service.waitForPendingFormShowFromServer();
      service.expectFormToShowOnClient(false);
      expect(service.isExpectingAFormToShowSoon()).toBe(false);
      await expect(promise).resolves.toBeUndefined();
    });

    it('should only create a single deferred for nested form shows', () => {
      const service = createService();
      service.expectFormToShowOnClient(true);
      const promise = service.waitForPendingFormShowFromServer();
      service.expectFormToShowOnClient(true);
      expect(service.waitForPendingFormShowFromServer()).toBe(promise);
      service.expectFormToShowOnClient(false);
      expect(service.isExpectingAFormToShowSoon()).toBe(true);
      service.expectFormToShowOnClient(false);
      expect(service.isExpectingAFormToShowSoon()).toBe(false);
    });
  });

  describe('connect', () => {
    it('should connect through the websocket service and store client/window numbers from messages', () => {
      const messageHandlers: ((msg: any) => void)[] = [];
      const wsSession = {
        onMessageObject: (h: any) => messageHandlers.push(h),
        createDeferredEvent: vi.fn(),
        resolveDeferedEvent: vi.fn(),
      };
      websocketService.connect.mockReturnValue(wsSession);
      const service = createService();

      const result = service.connect('ctx', { a: 1 }, 'wsUri');
      expect(result).toBe(wsSession);
      expect(websocketService.connect).toHaveBeenCalled();

      messageHandlers[0]({ clientnr: '11', windownr: '22' });
      expect(sessionStorage.set).toHaveBeenCalledWith('clientnr', '11');
      expect(sessionStorage.set).toHaveBeenCalledWith('windownr', '22');
      expect(websocketService.setConnectionPathArguments).toHaveBeenCalled();
    });
  });

  describe('deferred events delegate to the ws session', () => {
    let wsSession: any;
    let service: SabloService;

    beforeEach(() => {
      wsSession = {
        onMessageObject: vi.fn(),
        createDeferredEvent: vi.fn().mockReturnValue({ deferred: new Deferred(), cmsgid: 3 }),
        resolveDeferedEvent: vi.fn(),
        callService: vi.fn(),
        sendMessageObject: vi.fn(),
        addIncomingMessageHandlingDoneTask: vi.fn(),
      };
      websocketService.connect.mockReturnValue(wsSession);
      service = createService();
      service.connect('', {}, '');
    });

    it('createDeferredWSEvent delegates to the session', () => {
      const result = service.createDeferredWSEvent();
      expect(result.cmsgid).toBe(3);
      expect(wsSession.createDeferredEvent).toHaveBeenCalled();
    });

    it('resolveDeferedEvent delegates to the session', () => {
      service.resolveDeferedEvent(3, 'arg', true);
      expect(wsSession.resolveDeferedEvent).toHaveBeenCalledWith(3, 'arg', true);
    });

    it('sendServiceChangesJSON wraps the servicedatapush message', () => {
      service.sendServiceChangesJSON('svc', { a: 1 });
      expect(wsSession.sendMessageObject).toHaveBeenCalledWith({ servicedatapush: 'svc', changes: { a: 1 } });
    });

    it('callService returns the async promise directly when async', () => {
      wsSession.callService.mockReturnValue('asyncPromise');
      const result = service.callService('svc', 'm', {}, true);
      expect(result).toBe('asyncPromise');
    });

    it('addIncomingMessageHandlingDoneTask delegates to the session when one exists', () => {
      const func = vi.fn();
      service.addIncomingMessageHandlingDoneTask(func);
      expect(wsSession.addIncomingMessageHandlingDoneTask).toHaveBeenCalledWith(func);
    });
  });

  it('callService returns undefined when there is no session', () => {
    const service = createService();
    expect(service.callService('svc', 'm', {}, true)).toBeUndefined();
  });

  it('getCurrentRequestInfo delegates to the websocket service', () => {
    const service = createService();
    expect(service.getCurrentRequestInfo()).toBe('reqInfo');
  });

  it('addIncomingMessageHandlingDoneTask falls back to the websocket session promise when there is no session', async () => {
    const task = vi.fn();
    const session = { addIncomingMessageHandlingDoneTask: vi.fn() };
    websocketService.getSession.mockReturnValue(Promise.resolve(session));
    const service = createService();
    service.addIncomingMessageHandlingDoneTask(task);
    await Promise.resolve();
    await Promise.resolve();
    expect(session.addIncomingMessageHandlingDoneTask).toHaveBeenCalledWith(task);
  });

  describe('addToCurrentServiceCall', () => {
    it('runs the function on a timeout when no service call is in progress', () => {
      vi.useFakeTimers();
      const service = createService();
      const func = vi.fn();
      service.addToCurrentServiceCall(func);
      expect(func).not.toHaveBeenCalled();
      vi.advanceTimersByTime(0);
      expect(func).toHaveBeenCalledTimes(1);
      vi.useRealTimers();
    });
  });

  describe('console log wrapping', () => {
    it('wraps console methods when wraplogging is not disabled and forwards to consoleLogger service', () => {
      fakeWindow = buildWindow('');
      windowRef.nativeWindow = fakeWindow;
      const wsSession: any = { onMessageObject: vi.fn(), callService: vi.fn() };
      websocketService.connect.mockReturnValue(wsSession);
      const service = createService();
      service.connect('', {}, '');

      // the wrapped console.warn should forward a consoleLogger call to the server
      fakeWindow.window.console.warn('hello');
      expect(wsSession.callService).toHaveBeenCalledWith('consoleLogger', 'warn', { message: 'hello' }, true);
    });

    it('toggleSabloLogWrapping disables and re-enables wrapping', () => {
      fakeWindow = buildWindow('');
      windowRef.nativeWindow = fakeWindow;
      createService();
      const toggle = (fakeWindow as any)['toggleSabloLogWrapping'];
      expect(toggle).toBeInstanceOf(Function);
      expect(() => toggle(false)).not.toThrow();
      expect(() => toggle(true)).not.toThrow();
    });

    it('a window error event forwards the message to the consoleLogger service', () => {
      fakeWindow = buildWindow('');
      windowRef.nativeWindow = fakeWindow;
      const wsSession: any = { onMessageObject: vi.fn(), callService: vi.fn() };
      websocketService.connect.mockReturnValue(wsSession);
      const service = createService();
      service.connect('', {}, '');

      fakeWindow.listeners['error']({ message: 'boom', filename: 'f.js', lineno: 1, colno: 2, error: new Error('boom') });
      expect(wsSession.callService).toHaveBeenCalledWith('consoleLogger', 'error', expect.objectContaining({ message: expect.stringContaining('boom') }), true);
    });
  });

  describe('callService (sync)', () => {
    beforeEach(() => vi.useFakeTimers());
    afterEach(() => vi.useRealTimers());

    it('wraps the session promise so that it resolves with the server return value', async () => {
      const deferred = new Deferred<string>();
      const wsSession: any = {
        onMessageObject: vi.fn(),
        callService: vi.fn().mockReturnValue(deferred.promise),
      };
      websocketService.connect.mockReturnValue(wsSession);
      const service = createService();
      service.connect('', {}, '');

      const result = service.callService<string>('svc', 'm', { x: 1 }, false);
      expect(wsSession.callService).toHaveBeenCalledWith('svc', 'm', { x: 1 }, false);

      deferred.resolve('done');
      await expect(result).resolves.toBe('done');
    });

    it('queues a callback while a sync service call is waiting', () => {
      const deferred = new Deferred<string>();
      const wsSession: any = {
        onMessageObject: vi.fn(),
        callService: vi.fn().mockReturnValue(deferred.promise),
      };
      websocketService.connect.mockReturnValue(wsSession);
      const service = createService();
      service.connect('', {}, '');

      service.callService<string>('svc', 'm', {}, false);
      const queued = vi.fn();
      service.addToCurrentServiceCall(queued);
      // while the call is in progress the callback is deferred, not run immediately
      expect(queued).not.toHaveBeenCalled();
    });
  });
});
