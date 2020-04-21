import { Directive, OnInit, HostBinding, Input, Output, ViewChild, ElementRef, AfterViewInit, Renderer2 } from "@angular/core";
import { WebsocketService } from "../websocket.service";
import { I18NProvider } from '../../ngclient/services/i18n_provider.service';
import { ServoyService } from "../../ngclient/servoy.service";

@Directive({
    selector: '[sabloReconnectingFeedback]'
})
export class SabloReconnectingFeedback implements OnInit {

    i18n_reconnecting_feedback: string = "Disconnected from server, Reconnecting....";

    constructor(private websocketService: WebsocketService,
         private i18nProvider: I18NProvider, 
         private renderer: Renderer2,
         private elRef: ElementRef,
         private servoyService: ServoyService) {
    }
    
    @HostBinding('style.visibility') visibility: string; 

    ngOnInit() {
        if (!this.websocketService.isReconnecting()) {
            this.visibility = 'hidden';
        } else {
            this.i18nProvider.getI18NMessages(
                "servoy.ngclient.reconnecting").then((val)=> {
                  this.i18n_reconnecting_feedback = val["servoy.ngclient.reconnecting"];
                });
            this.renderer.appendChild(this.elRef.nativeElement, this.renderer.createText(this.i18n_reconnecting_feedback));
            this.servoyService.reconnectingEmitter.next(true);
        }
    }
}