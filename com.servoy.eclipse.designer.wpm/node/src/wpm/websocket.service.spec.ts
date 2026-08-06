import { describe, it, expect, beforeEach, vi } from 'vitest';

import { WebsocketService } from './websocket.service';

describe('WebsocketService', () => {
  let service: WebsocketService;
  let mockWebSocket: any;

  beforeEach(() => {
    mockWebSocket = {
      onopen: null as any,
      onclose: null as any,
      onerror: null as any,
      onmessage: null as any,
      readyState: 1,
      send: vi.fn(),
      close: vi.fn(),
    };

    vi.stubGlobal('WebSocket', class {
      static OPEN = 1;
      onopen: any;
      onclose: any;
      onerror: any;
      onmessage: any;
      readyState = 1;
      send = mockWebSocket.send;
      close = mockWebSocket.close;
      constructor(_url: string) {
        Object.assign(mockWebSocket, this);
        const that = this; // eslint-disable-line @typescript-eslint/no-this-alias
        Object.defineProperty(mockWebSocket, 'onopen', {
          set: (v) => {
 that.onopen = v; 
},
          get: () => that.onopen,
          configurable: true
        });
        Object.defineProperty(mockWebSocket, 'onmessage', {
          set: (v) => {
 that.onmessage = v; 
},
          get: () => that.onmessage,
          configurable: true
        });
        Object.defineProperty(mockWebSocket, 'onerror', {
          set: (v) => {
 that.onerror = v; 
},
          get: () => that.onerror,
          configurable: true
        });
        Object.defineProperty(mockWebSocket, 'onclose', {
          set: (v) => {
 that.onclose = v; 
},
          get: () => that.onclose,
          configurable: true
        });
      }
    });

    service = Object.create(WebsocketService.prototype);
    (service as any).connection = undefined;
  });

  describe('connect', () => {
    it('should create a WebSocket connection', () => {
      const connection = service.connect('ws://localhost/test');
      expect(connection).toBeDefined();
      expect(connection.open).toBeDefined();
      expect(connection.messageSender).toBeDefined();
      expect(connection.messageObservable).toBeDefined();
    });

    it('should reuse existing connection on second call', () => {
      const conn1 = service.connect('ws://localhost/test');
      const conn2 = service.connect('ws://localhost/other');
      expect(conn1).toBe(conn2);
    });
  });

  describe('open observable', () => {
    it('should emit when WebSocket opens', () => {
      const connection = service.connect('ws://localhost/test');
      const openHandler = vi.fn();
      connection.open.subscribe(openHandler);

      mockWebSocket.onopen(new Event('open'));
      expect(openHandler).toHaveBeenCalled();
    });
  });

  describe('messageObservable', () => {
    it('should emit messages from WebSocket', () => {
      const connection = service.connect('ws://localhost/test');
      const msgHandler = vi.fn();
      connection.messageObservable.subscribe(msgHandler);

      const event = new MessageEvent('message', { data: '{"method":"test"}' });
      mockWebSocket.onmessage(event);
      expect(msgHandler).toHaveBeenCalledWith(event);
    });
  });

  describe('messageSender', () => {
    it('should send JSON data through WebSocket when open', () => {
      const connection = service.connect('ws://localhost/test');

      connection.messageSender.next({ method: 'test' });
      expect(mockWebSocket.send).toHaveBeenCalledWith('{"method":"test"}');
    });

    it('should not send when WebSocket is not open', () => {
      vi.stubGlobal('WebSocket', class {
        static OPEN = 1;
        onopen: any;
        onclose: any;
        onerror: any;
        onmessage: any;
        readyState = 0;
        send = vi.fn();
        close = vi.fn();
      });
      (service as any).connection = undefined;
      const connection = service.connect('ws://localhost/test');

      connection.messageSender.next({ method: 'test' });
      expect(mockWebSocket.send).not.toHaveBeenCalled();
    });
  });
});
