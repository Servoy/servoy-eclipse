import { Injectable } from '@angular/core';
import { Observable, Observer } from 'rxjs';

interface WebSocketConnection {
  open: Observable<Event>;
  messageSender: Observer<Message>;
  messageObservable: Observable<MessageEvent>;
}

export interface Message {
  method: string;
  data?: unknown;
  package?: Package;
  url?: string;
  solution?: string;
  name?: string;
  values?: Repository
}

export interface Release {
  servoyVersion?: string;
  url: string;
  version: string;
}

export interface Package {
  markedAsRemoved: boolean;
  activeSolution: string;
  description: string;
  displayName: string;
  icon: string;
  installed: string;
  installedIsWPA: boolean;
  installing: boolean;
  name: string;
  packageType: string;
  releases: Release[];
  removing: boolean;
  selected: string;
  sourceUrl: string;
  top: boolean;
  wikiUrl: string;
  hasLatestVersion: boolean;
}

export interface PackagesInfo {
  packageType: string;
  packages: Package[];
}

export interface Repository {
  name: string;
  selected?: boolean;
  url?: string;
}

export interface PackagesAndRepositories {
  packages: Package[];
  repositories: Repository[];
}

@Injectable()
export class WebsocketService {

  private connection: WebSocketConnection;

  public connect(url: string): WebSocketConnection {
    if (!this.connection) {
      this.connection = this.create(url);
    }
    return this.connection;
  }

  private create(url: string): WebSocketConnection {
    const ws = new WebSocket(url);

    const openObservable = new Observable((obs: Observer<Event>) => {
      ws.onopen = (event: Event) => obs.next(event);
      ws.onerror = (event: Event) => obs.error(event);
      ws.onclose = () => obs.complete();
      return () => ws.close();
    });

    const messageObservable = new Observable((obs: Observer<MessageEvent>) => {
      ws.onmessage =  (event: MessageEvent) => obs.next(event);
      ws.onerror = (event: Event) => obs.error(event);
      ws.onclose = () => obs.complete();
      return  () => ws.close();
    });

    const messageObserver: Observer<Message> = {
      next: (data: Message) => {
        if (ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify(data));
        }
      }
    } as Observer<Message>;

    return {open: openObservable, messageSender: messageObserver, messageObservable};
  }
}