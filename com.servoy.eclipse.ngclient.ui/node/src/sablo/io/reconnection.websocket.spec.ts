import { TestBed,fakeAsync,tick } from '@angular/core/testing';

import {ReconnectingWebSocket} from './reconnecting.websocket';

import {CustomEvent} from '../util/eventemitter';
import { LoggerFactory } from '@servoy/public';
import { WindowRefService } from '@servoy/public';

describe('ReconnectionWebsocket', () => {
  let normalWebSocket = null;
  beforeEach(() => {
      normalWebSocket =  window['WebSocket'];
     window['Web' + 'Socket'] = WebSocketMock;
  });

  afterEach(() => {
      window['WebSocket'] = normalWebSocket;
  });

  it('should be connecting and reconnecting', fakeAsync(() => {
      const socket = new TestReconnectingWebSocket('ws://localhost/', new LoggerFactory(new WindowRefService()));
      expect(socket.__latestEvent.name).toBe('connecting');
      tick(10);
      expect(socket.__latestEvent.name).toBe('open');
      expect(socket.__latestEvent['isReconnect']).toBe(false, 'reconnect should be false');
      WebSocketMock.instance['onclose'](new CustomEvent('close')); // internal websocket closed, reconnect should happen
      expect(socket.__latestEvent.name).toBe('close');
      tick(1500);
      expect(socket.__latestEvent.name).toBe('open');
      socket.close(); // now a real close of the reconnection socket, now it should really stay closed.
      expect(socket.__latestEvent.name).toBe('close');
      tick(1500);
      expect(socket.__latestEvent.name).toBe('close');

    }));

  it('should send and receive data', () => {
      const socket = new TestReconnectingWebSocket('ws://localhost/', new LoggerFactory(new WindowRefService()));

      socket.send('some data');
      expect(WebSocketMock.instance.data).toBe('some data');

      const event = new CustomEvent('message');
      event['data'] = 'mymessage';
      WebSocketMock.instance['onmessage'](event);
      expect(socket.__latestEvent.name).toBe('message');
      expect(socket.__latestEvent['data']).toBe('mymessage');
    });

it('test url as function param', () => {
    const socket = new TestReconnectingWebSocket(()=>'ws://localhost/', new LoggerFactory(new WindowRefService()));
    expect(socket.__latestEvent.name).toBe('connecting');
    expect(WebSocketMock.instance.url).toBe('ws://localhost/');
  });
});

class WebSocketMock {
    data: any;
    public static instance: WebSocketMock;
    public url: string;
    public closed = false;
    constructor( url: string, protocols?: string | string[] ) {
        this.url = url;
        WebSocketMock.instance = this;
        setTimeout(() => {
            WebSocketMock.instance['onopen'](new CustomEvent('open'));
        },1);
    }

    public close() {
        this.closed = true;
        WebSocketMock.instance['onclose'](new CustomEvent('close'));
    }

    public send(data) {
        this.data = data;
    }
}

class TestReconnectingWebSocket extends ReconnectingWebSocket {
    public __latestEvent: CustomEvent;

    public onopen(event) {
        this.__latestEvent = event;
    }
    public onclose(event) {
        this.__latestEvent = event;
    }
    public onconnecting(event) {
        this.__latestEvent = event;
    }
    public onmessage(event) {
        this.__latestEvent = event;
    }
    public onerror(event) {
        this.__latestEvent = event;
    }
}
