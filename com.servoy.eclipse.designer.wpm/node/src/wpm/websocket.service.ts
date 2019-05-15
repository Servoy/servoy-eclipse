import { Injectable } from '@angular/core';
import * as Rx from 'rxjs'

interface WebSocketConnection {
  open: Rx.Observable<MessageEvent>;
  messages: Rx.Subject<MessageEvent>;
}

@Injectable()
export class WebsocketService {

  constructor() {}

  private connection: WebSocketConnection;

  public connect(url): WebSocketConnection {
    if (!this.connection) {
      this.connection = this.create(url);
    }
    return this.connection;
  }

  private create(url): WebSocketConnection {
    const ws = new WebSocket(url);

    const openObservable = Rx.Observable.create((obs: Rx.Observer<MessageEvent>) => {
      ws.onopen = obs.next.bind(obs);
      ws.onerror = obs.error.bind(obs);
      ws.onclose = obs.complete.bind(obs);
      return ws.close.bind(ws);
    });

    const messageObservable = Rx.Observable.create((obs: Rx.Observer<MessageEvent>) => {
      ws.onmessage = obs.next.bind(obs);
      ws.onerror = obs.error.bind(obs);
      ws.onclose = obs.complete.bind(obs);
      return ws.close.bind(ws);
    });

    const messageObserver = {
      next: (data: Object) => {
        if (ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify(data));
        }
      }
    };

    return {open: openObservable, messages: Rx.Subject.create(messageObserver, messageObservable)};
  }
}