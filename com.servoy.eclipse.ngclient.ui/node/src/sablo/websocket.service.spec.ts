import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { WebsocketService } from './websocket.service';

import { WindowRefService } from '@servoy/public';
import { ServicesService } from './services.service';
import { ConverterService } from './converter.service';
import { LoggerFactory } from '@servoy/public';
import { LoadingIndicatorService } from './util/loading-indicator/loading-indicator.service';

describe('WebsocketService', () => {
  let windowRef: any;
  let normalWebSocket: any = null;
  beforeEach(() => {
    normalWebSocket = (window as any)['WebSocket'];
    (window as any)['Web' + 'Socket'] = WebSocketMock;

    windowRef = { nativeWindow: {} };
    const servicesService = { callServiceApi: vi.fn(), updateServiceScopes: vi.fn() } as any;
    const converterService = { convertFromServerToClient: vi.fn(), convertFromClientToServer: vi.fn(), convertClientObject: vi.fn() } as any;
    const loadingIndicatorService = { showLoading: vi.fn(), hideLoading: vi.fn(), isShowing: vi.fn() } as any;
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
  it('should be make a connection', () => {
    vi.useFakeTimers();
    const service = TestBed.inject(WebsocketService);
    windowRef.nativeWindow = { location: { protocol: 'http', host: 'localhost', pathname: '/' } };
    const session = service.connect('', [], {}, null as any);
    vi.advanceTimersByTime(10);
    expect(session.isConnected()).toBeTruthy();
    vi.useRealTimers();
  });
});

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
