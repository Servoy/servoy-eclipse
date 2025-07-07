import { WindowRefService } from "@servoy/public";
import { IWebSocket, WebsocketCustomEvent } from "./iwebsocket";

export class MobileBridge implements IWebSocket {

    constructor(private windowRef: WindowRefService) {
        windowRef.nativeWindow.addEventListener('message', (event) => {
            if (event.data?.from === 'gwt') {
                this.onGWTEvent(event.data.data);
            }
        });
        setTimeout(() => {
            this.onopen(new WebsocketCustomEvent('open'));    
        }, 10);
    }

    private onGWTEvent(message: string) {
		const e = new WebsocketCustomEvent('message');
		e.data = message;
        this.onmessage(e);
    }

    close(code?: number, reason?: string): void {
        console.log('close');
    }
    send(data: string): void {
        if (data === 'P') {
			const e = new WebsocketCustomEvent('message');
			e.data = 'p';
			this.onmessage(e);
		}
        else {
            const iframe = this.windowRef.nativeWindow.document.getElementById('mobileclient');
            if (iframe) {
                (iframe as any).contentWindow.postMessage(data);
            }
        }        
    }
    onmessage: (message: WebsocketCustomEvent) => true | void;
    onconnecting: (evt: WebsocketCustomEvent) => void;
    onclose: (evt: WebsocketCustomEvent) => void;
    onopen: (evt: WebsocketCustomEvent) => void;
    onerror: (evt: WebsocketCustomEvent) => void;

}