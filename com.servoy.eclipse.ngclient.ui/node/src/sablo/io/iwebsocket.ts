import { CustomEvent } from '../util/eventemitter';

export interface IWebSocket {
    onerror: (evt: WebsocketCustomEvent) => void;
    onopen: (evt: WebsocketCustomEvent) => void;
    onclose: (evt: WebsocketCustomEvent) => void;
    onconnecting: (evt: WebsocketCustomEvent) => void;
    onmessage: (message: WebsocketCustomEvent) => true | void;
    
    send(data:string): void;
    
    close(code?: number, reason?: string):void;
}



export class WebsocketCustomEvent extends CustomEvent {
    public isReconnect: boolean;
    public code: number;
    public reason: string;
    public wasClean: boolean;
    public data: any;
}