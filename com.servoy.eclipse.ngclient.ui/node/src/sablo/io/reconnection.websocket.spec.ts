import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { ReconnectingWebSocket } from './reconnecting.websocket';

import { CustomEvent } from '../util/eventemitter';
import { LoggerFactory } from '@servoy/public';
import { WindowRefService } from '@servoy/public';

describe('ReconnectionWebsocket', () => {
  let normalWebSocket: any = null;
  beforeEach(() => {
    normalWebSocket = (window as any)['WebSocket'];
    (window as any)['Web' + 'Socket'] = WebSocketMock;
  });

  afterEach(() => {
    (window as any)['WebSocket'] = normalWebSocket;
  });

  it('should be connecting and reconnecting', () => {
    vi.useFakeTimers();
    const socket = new TestReconnectingWebSocket('ws://localhost/', new LoggerFactory(new WindowRefService()));
    expect(socket.__latestEvent.name).toBe('connecting');
    vi.advanceTimersByTime(10);
    expect(socket.__latestEvent.name).toBe('open');
    expect((socket.__latestEvent as any)['isReconnect']).toBe(false);
    (WebSocketMock.instance as any)['onclose'](new CustomEvent('close')); // internal websocket closed, reconnect should happen
    expect(socket.__latestEvent.name).toBe('close');
    vi.advanceTimersByTime(1500);
    expect(socket.__latestEvent.name).toBe('open');
    socket.close(); // now a real close of the reconnection socket, now it should really stay closed.
    expect(socket.__latestEvent.name).toBe('close');
    vi.advanceTimersByTime(1500);
    expect(socket.__latestEvent.name).toBe('close');
    vi.useRealTimers();
  });

  it('should send and receive data', () => {
    const socket = new TestReconnectingWebSocket('ws://localhost/', new LoggerFactory(new WindowRefService()));

    socket.send('some data');
    expect((WebSocketMock.instance as any).data).toBe('some data');

    const event = new CustomEvent('message');
    (event as any)['data'] = 'mymessage';
    (WebSocketMock.instance as any)['onmessage'](event);
    expect(socket.__latestEvent.name).toBe('message');
    expect((socket.__latestEvent as any)['data']).toBe('mymessage');
  });

  it('test url as function param', () => {
    const socket = new TestReconnectingWebSocket(() => 'ws://localhost/', new LoggerFactory(new WindowRefService()));
    expect(socket.__latestEvent.name).toBe('connecting');
    expect(WebSocketMock.instance.url).toBe('ws://localhost/');
  });
});

class WebSocketMock {
  data: any;
  public static instance: WebSocketMock;
  public url: string;
  public closed = false;
  constructor(url: string, protocols?: string | string[]) {
    this.url = url;
    WebSocketMock.instance = this;
    setTimeout(() => {
      (WebSocketMock.instance as any)['onopen'](new CustomEvent('open'));
    }, 1);
  }

  public close() {
    this.closed = true;
    (WebSocketMock.instance as any)['onclose'](new CustomEvent('close'));
  }

  public send(data: any) {
    this.data = data;
  }
}

class TestReconnectingWebSocket extends ReconnectingWebSocket {
  public __latestEvent!: CustomEvent;

  override onopen(event: any) {
    this.__latestEvent = event;
  }
  override onclose(event: any) {
    this.__latestEvent = event;
  }
  override onconnecting(event: any) {
    this.__latestEvent = event;
  }
  override onmessage(event: any) {
    this.__latestEvent = event;
  }
  override onerror(event: any) {
    this.__latestEvent = event;
  }
}
