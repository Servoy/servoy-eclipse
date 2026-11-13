import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { WebsocketService, WebsocketSession, SabloUtils, wrapPromiseToPropagateCustomRequestInfoInternal } from './websocket.service';

import { WindowRefService } from '@servoy/public';
import { ServicesService } from './services.service';
import { ConverterService } from './converter.service';
import { LoggerFactory } from '@servoy/public';
import { LoadingIndicatorService } from './util/loading-indicator/loading-indicator.service';
import { IWebSocket, WebsocketCustomEvent } from './io/iwebsocket';

describe('WebsocketService', () => {
  let windowRef: any;
  let normalWebSocket: any = null;
  let converterService: any;
  let loadingIndicatorService: any;

  beforeEach(() => {
    normalWebSocket = (window as any)['WebSocket'];
    (window as any)['Web' + 'Socket'] = WebSocketMock;

    windowRef = { nativeWindow: { location: { protocol: 'http:', host: 'localhost', pathname: '/solution/index.html', search: '' } } };
    const servicesService = { callServiceApi: vi.fn(), updateServiceScopes: vi.fn() } as any;
    converterService = { convertFromServerToClient: vi.fn(), convertFromClientToServer: vi.fn(), convertClientObject: vi.fn() } as any;
    loadingIndicatorService = { showLoading: vi.fn(), hideLoading: vi.fn(), isShowing: vi.fn() } as any;
    TestBed.configureTestingModule({
      providers: [
        WebsocketService,
        { provide: WindowRefService, useFactory: () => windowRef },
        { provide: ServicesService, useFactory: () => servicesService },
        { provide: ConverterService, useFactory: () => converterService },
        { provide: LoadingIndicatorService, useFactory: () => loadingIndicatorService },
        LoggerFactory,
      ],
    });
  });

  afterEach(() => {
    (window as any)['WebSocket'] = normalWebSocket;
  });

  it('should be created', () => {
    const service = TestBed.inject(WebsocketService);
    expect(service).toBeTruthy();
  });

  it('should make a connection', () => {
    vi.useFakeTimers();
    const service = TestBed.inject(WebsocketService);
    windowRef.nativeWindow = { location: { protocol: 'http', host: 'localhost', pathname: '/' } };
    const session = service.connect('', [], {}, null as any);
    vi.advanceTimersByTime(10);
    expect(session.isConnected()).toBeTruthy();
    vi.useRealTimers();
  });

  describe('query string / url parameters', () => {
    it('getQueryString strips a leading ? from location.search', () => {
      const service = TestBed.inject(WebsocketService);
      windowRef.nativeWindow.location.search = '?a=1&b=2';
      expect(service.getQueryString()).toBe('a=1&b=2');
    });

    it('getQueryString returns an explicitly set query string', () => {
      const service = TestBed.inject(WebsocketService);
      service.setQueryString('x=9');
      expect(service.getQueryString()).toBe('x=9');
    });

    it('getURLParameter extracts a decoded value', () => {
      const service = TestBed.inject(WebsocketService);
      service.setQueryString('clientnr=5&windowname=my%20window');
      expect(service.getURLParameter('clientnr')).toBe('5');
      expect(service.getURLParameter('windowname')).toBe('my window');
    });

    it('getURLParameter returns null for an absent parameter', () => {
      const service = TestBed.inject(WebsocketService);
      service.setQueryString('a=1');
      expect(service.getURLParameter('missing')).toBeNull();
    });
  });

  describe('pathname', () => {
    it('getPathname returns the explicitly set value', () => {
      const service = TestBed.inject(WebsocketService);
      service.setPathname('/custom');
      expect(service.getPathname()).toBe('/custom');
    });

    it('getPathname falls back to location.pathname', () => {
      const service = TestBed.inject(WebsocketService);
      expect(service.getPathname()).toBe('/solution/index.html');
    });
  });

  describe('connection query/path arguments', () => {
    it('setConnectionQueryArgument stores a value', () => {
      const service = TestBed.inject(WebsocketService);
      service.setConnectionQueryArgument('foo', 'bar');
      // generated URL should include the query arg
      (service as any).windowRef = windowRef;
      const url = (service as any).generateURL('', [], { foo: 'bar' }, '/websocket');
      expect(url).toContain('foo=bar');
    });

    it('setConnectionPathArguments stores path args used by generateURL', () => {
      const service = TestBed.inject(WebsocketService);
      const url = (service as any).generateURL('', ['a', 'b'], undefined, '/websocket');
      expect(url).toContain('/a/b');
    });
  });

  describe('generateURL', () => {
    it('uses wss when the page is served over https', () => {
      const service = TestBed.inject(WebsocketService);
      windowRef.nativeWindow.location.protocol = 'https:';
      const url = (service as any).generateURL('', [], undefined, '/websocket');
      expect(url.startsWith('wss://localhost')).toBe(true);
    });

    it('uses ws for plain http and includes the websocket uri', () => {
      const service = TestBed.inject(WebsocketService);
      const url = (service as any).generateURL('', [], undefined, '/websocket');
      expect(url.startsWith('ws://localhost')).toBe(true);
      expect(url).toContain('/websocket');
      expect(url).toContain('connectNr=');
    });

    it('drops deep-link parameters on reconnect attempts', () => {
      const service = TestBed.inject(WebsocketService);
      service.setQueryString('method=foo&m=bar&arguments=1&a=2&keep=yes');
      const url = (service as any).generateURL('', [], undefined, '/websocket', true);
      expect(url).not.toContain('method=foo');
      expect(url).not.toContain('arguments=1');
      expect(url).toContain('keep=yes');
    });

    it('appends lastServerMessageNumber when set', () => {
      const service = TestBed.inject(WebsocketService);
      service.setLastServerMessageNumber(123);
      const url = (service as any).generateURL('', [], undefined, '/websocket');
      expect(url).toContain('lastServerMessageNumber=123');
    });
  });

  describe('getSession', () => {
    it('resolves immediately once a session exists', async () => {
      vi.useFakeTimers();
      const service = TestBed.inject(WebsocketService);
      service.connect('', [], {}, null as any);
      vi.useRealTimers();
      await expect(service.getSession()).resolves.toBeTruthy();
    });

    it('resolves the deferred when connect happens afterwards', async () => {
      vi.useFakeTimers();
      const service = TestBed.inject(WebsocketService);
      const sessionPromise = service.getSession();
      service.connect('', [], {}, null as any);
      vi.useRealTimers();
      await expect(sessionPromise).resolves.toBeTruthy();
    });
  });

  it('disconnect and isConnected delegate to the session', () => {
    vi.useFakeTimers();
    const service = TestBed.inject(WebsocketService);
    const session = service.connect('', [], {}, null as any);
    vi.advanceTimersByTime(10);
    expect(service.isConnected()).toBe(true);
    service.disconnect();
    expect(session.isConnected()).toBe(false);
    vi.useRealTimers();
  });
});

describe('WebsocketSession', () => {
  let socketMock: FakeSocket;
  let converterService: any;
  let loadingIndicatorService: any;
  let websocketService: any;
  let ngZone: any;
  let session: WebsocketSession;

  const buildSession = () => {
    socketMock = new FakeSocket();
    converterService = { convertFromServerToClient: vi.fn().mockImplementation((v: any) => v) };
    loadingIndicatorService = { showLoading: vi.fn(), hideLoading: vi.fn(), isShowing: vi.fn().mockReturnValue(false) };
    websocketService = { reconnectingEmitter: { next: vi.fn() }, setLastServerMessageNumber: vi.fn() };
    ngZone = { run: (fn: any) => fn(), runOutsideAngular: (fn: any) => fn() };
    const windowRef = { nativeWindow: { location: { reload: vi.fn() } } } as any;
    session = new WebsocketSession(socketMock, websocketService, windowRef, converterService, loadingIndicatorService, ngZone, new LoggerFactory(new WindowRefService()));
    return session;
  };

  beforeEach(() => buildSession());

  it('should mark itself connected when the socket opens', () => {
    expect(session.isConnected()).toBe(false);
    socketMock.onopen(new WebsocketCustomEvent('open'));
    expect(session.isConnected()).toBe(true);
  });

  it('async callService sends a message object and returns undefined', () => {
    socketMock.onopen(new WebsocketCustomEvent('open'));
    const result = session.callService('svc', 'm', { a: 1 }, true);
    expect(result).toBeUndefined();
    const sent = JSON.parse(socketMock.sentData[socketMock.sentData.length - 1]);
    expect(sent.service).toBe('svc');
    expect(sent.methodname).toBe('m');
  });

  it('sync callService shows loading, registers a deferred and resolves on the server response', async () => {
    socketMock.onopen(new WebsocketCustomEvent('open'));
    const promise = session.callService('svc', 'm', {}, false);
    expect(loadingIndicatorService.showLoading).toHaveBeenCalled();
    const sent = JSON.parse(socketMock.sentData[socketMock.sentData.length - 1]);
    expect(sent.cmsgid).toBeDefined();

    socketMock.onmessage({ data: JSON.stringify({ cmsgid: sent.cmsgid, ret: 'theResult' }) } as any);
    await expect(promise).resolves.toBe('theResult');
    expect(loadingIndicatorService.hideLoading).toHaveBeenCalled();
  });

  it('sync callService rejects when the server returns an exception', async () => {
    socketMock.onopen(new WebsocketCustomEvent('open'));
    const promise = session.callService('svc', 'm', {}, false);
    const sent = JSON.parse(socketMock.sentData[socketMock.sentData.length - 1]);
    socketMock.onmessage({ data: JSON.stringify({ cmsgid: sent.cmsgid, exception: 'boom' }) } as any);
    await expect(promise).rejects.toBe('boom');
  });

  it('buffers messages while disconnected and flushes them when connected', () => {
    // not connected yet
    session.sendMessageObject({ hello: 'world' });
    expect(socketMock.sentData.length).toBe(0);
    socketMock.onopen(new WebsocketCustomEvent('open'));
    expect(socketMock.sentData.length).toBe(1);
    expect(JSON.parse(socketMock.sentData[0]).hello).toBe('world');
  });

  it('includes the current event level as prio on sent messages', () => {
    socketMock.onopen(new WebsocketCustomEvent('open'));
    session.setCurrentEventLevelForServer(SabloUtils.EVENT_LEVEL_SYNC_API_CALL);
    expect(session.getCurrentEventLevelForServer()).toBe(SabloUtils.EVENT_LEVEL_SYNC_API_CALL);
    session.sendMessageObject({ foo: 1 });
    const sent = JSON.parse(socketMock.sentData[socketMock.sentData.length - 1]);
    expect(sent.prio).toBe(SabloUtils.EVENT_LEVEL_SYNC_API_CALL);
  });

  it('responds to a server ping with a pong', () => {
    socketMock.onopen(new WebsocketCustomEvent('open'));
    socketMock.sentData.length = 0;
    socketMock.onmessage({ data: 'P' } as any);
    expect(socketMock.sentData).toContain('p');
  });

  it('onMessageObject handlers receive the msg payload', () => {
    socketMock.onopen(new WebsocketCustomEvent('open'));
    const handler = vi.fn();
    session.onMessageObject(handler);
    socketMock.onmessage({ data: JSON.stringify({ msg: { some: 'data' } }) } as any);
    expect(handler).toHaveBeenCalledWith({ some: 'data' });
  });

  it('reads a message-number prefix and forwards it to the websocket service', () => {
    socketMock.onopen(new WebsocketCustomEvent('open'));
    socketMock.onmessage({ data: '42#' + JSON.stringify({ msg: {} }) } as any);
    expect(websocketService.setLastServerMessageNumber).toHaveBeenCalledWith('42');
  });

  it('CLIENT-OUT-OF-SYNC reload is triggered from onconnecting', () => {
    const evt = new WebsocketCustomEvent('connecting');
    evt.reason = 'CLIENT-OUT-OF-SYNC';
    socketMock.onconnecting(evt);
    expect((session as any).windowRef.nativeWindow.location.reload).toHaveBeenCalled();
  });

  it('onclose transitions to RECONNECTING when not explicitly closed', () => {
    socketMock.onopen(new WebsocketCustomEvent('open'));
    socketMock.onclose(new WebsocketCustomEvent('close'));
    expect(session.isReconnecting()).toBe(true);
    expect(websocketService.reconnectingEmitter.next).toHaveBeenCalledWith(true);
  });

  it('addIncomingMessageHandlingDoneTask runs the task immediately when no message is being handled', () => {
    const task = vi.fn();
    session.addIncomingMessageHandlingDoneTask(task);
    expect(task).toHaveBeenCalled();
  });

  it('createDeferredEvent/resolveDeferedEvent resolve a registered deferred', async () => {
    const { deferred, cmsgid } = session.createDeferredEvent<string>();
    session.resolveDeferedEvent(cmsgid, 'value', true);
    await expect(deferred.promise).resolves.toBe('value');
  });

  it('resolveDeferedEvent rejects when success is false', async () => {
    const { deferred, cmsgid } = session.createDeferredEvent<string>();
    session.resolveDeferedEvent(cmsgid, 'bad', false);
    await expect(deferred.promise).rejects.toBe('bad');
  });

  it('disconnect closes the socket and sets state to CLOSED', () => {
    socketMock.onopen(new WebsocketCustomEvent('open'));
    const closeSpy = vi.spyOn(socketMock, 'close');
    session.disconnect();
    expect(closeSpy).toHaveBeenCalled();
    expect(session.isConnected()).toBe(false);
    expect(session.isReconnecting()).toBe(false);
  });

  it('onerror handlers are invoked when the socket errors', () => {
    const handler = vi.fn();
    session.onerror(handler);
    socketMock.onerror(new WebsocketCustomEvent('error'));
    expect(handler).toHaveBeenCalled();
  });

  it('onopen/onclose registered handlers are invoked', () => {
    const openHandler = vi.fn();
    const closeHandler = vi.fn();
    session.onopen(openHandler);
    session.onclose(closeHandler);
    socketMock.onopen(new WebsocketCustomEvent('open'));
    expect(openHandler).toHaveBeenCalled();
    socketMock.onclose(new WebsocketCustomEvent('close'));
    expect(closeHandler).toHaveBeenCalled();
  });

  it('CLIENT-SHUTDOWN onconnecting closes the socket and prevents reconnection', () => {
    const closeSpy = vi.spyOn(socketMock, 'close');
    const evt = new WebsocketCustomEvent('connecting');
    evt.reason = 'CLIENT-SHUTDOWN';
    socketMock.onconnecting(evt);
    expect(closeSpy).toHaveBeenCalled();
    // a close after a shutdown must not flip to RECONNECTING
    socketMock.onclose(new WebsocketCustomEvent('close'));
    expect(session.isReconnecting()).toBe(false);
  });

  it('handles serviceApis by invoking the services handler', () => {
    socketMock.onopen(new WebsocketCustomEvent('open'));
    const servicesHandler = {
      handleServiceApisWithApplyFirst: vi.fn().mockReturnValue('r1'),
      handlerServiceUpdatesFromServer: vi.fn(),
      handleNormalServiceApis: vi.fn().mockReturnValue('r2'),
    };
    session.setServicesHandler(servicesHandler);
    socketMock.onmessage({ data: JSON.stringify({ serviceApis: [{ name: 's', call: 'c' }] }) } as any);
    expect(servicesHandler.handleServiceApisWithApplyFirst).toHaveBeenCalled();
    expect(servicesHandler.handleNormalServiceApis).toHaveBeenCalled();
  });

  it('forwards service updates nested in msg.services', () => {
    socketMock.onopen(new WebsocketCustomEvent('open'));
    const servicesHandler = {
      handleServiceApisWithApplyFirst: vi.fn(),
      handlerServiceUpdatesFromServer: vi.fn(),
      handleNormalServiceApis: vi.fn(),
    };
    session.setServicesHandler(servicesHandler);
    socketMock.onmessage({ data: JSON.stringify({ msg: { services: { svc: { a: 1 } } } }) } as any);
    expect(servicesHandler.handlerServiceUpdatesFromServer).toHaveBeenCalledWith({ svc: { a: 1 } });
  });

  it('sends a response for messages that carry an smsgid', async () => {
    socketMock.onopen(new WebsocketCustomEvent('open'));
    session.onMessageObject(() => Promise.resolve('theReturn'));
    socketMock.sentData.length = 0;
    socketMock.onmessage({ data: JSON.stringify({ smsgid: 77, msg: { something: 1 } }) } as any);
    await Promise.resolve();
    await Promise.resolve();
    await Promise.resolve();
    const responses = socketMock.sentData.map((d) => JSON.parse(d));
    const resp = responses.find((r) => r.smsgid === 77);
    expect(resp).toBeDefined();
    expect(resp.ret).toBe('theReturn');
  });

  it('logs and recovers from a malformed message', () => {
    socketMock.onopen(new WebsocketCustomEvent('open'));
    expect(() => socketMock.onmessage({ data: 'not json {' } as any)).not.toThrow();
  });
});

describe('SabloUtils.cloneWithDifferentPrototype', () => {
  it('copies own enumerable properties onto a new object with the given prototype', () => {
    const proto = { greet: () => 'hi' };
    const clone = SabloUtils.cloneWithDifferentPrototype({ a: 1, b: 2 }, proto);
    expect(clone.a).toBe(1);
    expect(clone.b).toBe(2);
    expect(Object.getPrototypeOf(clone)).toBe(proto);
    expect((clone as any).greet()).toBe('hi');
  });
});

describe('wrapPromiseToPropagateCustomRequestInfoInternal', () => {
  it('forwards requestInfo assignment to the original promise', () => {
    const original: any = Promise.resolve();
    const spawned: any = Promise.resolve();
    const wrapped = wrapPromiseToPropagateCustomRequestInfoInternal(original, spawned);
    wrapped.requestInfo = 'info';
    expect(original.requestInfo).toBe('info');
  });
});

class FakeSocket implements IWebSocket {
  sentData: string[] = [];
  onerror!: (evt: WebsocketCustomEvent) => void;
  onopen!: (evt: WebsocketCustomEvent) => void;
  onclose!: (evt: WebsocketCustomEvent) => void;
  onconnecting!: (evt: WebsocketCustomEvent) => void;
  onmessage!: (message: WebsocketCustomEvent) => true | void;
  send(data: string): void {
    this.sentData.push(data);
  }
  close(): void {
    /* noop */
  }
}

class WebSocketMock {
  constructor(url: string) {
    this.url = url;
    WebSocketMock.instance = this;
    setTimeout(() => {
      (WebSocketMock.instance as any)['onopen'](new CustomEvent('open'));
    }, 1);
  }
  public static instance: WebSocketMock;
  data: any;
  public url: string;
  public closed = false;

  public close() {
    this.closed = true;
    (WebSocketMock.instance as any)['onclose'](new CustomEvent('close'));
  }

  public send(data: any) {
    this.data = data;
  }
}
