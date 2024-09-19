import { Component, Renderer2 , ChangeDetectorRef, ChangeDetectionStrategy} from '@angular/core';

import { BaseTabpanel, Tab } from './basetabpanel';

import { WindowRefService } from '@servoy/public';
import { LoggerFactory } from '@servoy/public';

import { NgbNavChangeEvent } from '@ng-bootstrap/ng-bootstrap';


@Component( {
    selector: 'servoydefault-tabpanel',
    templateUrl: './tabpanel.html',
    changeDetection: ChangeDetectionStrategy.OnPush
} )
export class ServoyDefaultTabpanel extends BaseTabpanel {
    
    containerStyle = { position: 'relative', overflow: 'auto' };
    height: any = '100%';
    
    constructor( windowRefService: WindowRefService, log: LoggerFactory, renderer: Renderer2, cdRef: ChangeDetectorRef ) {
        super( windowRefService, log, renderer, cdRef );
    }

    onTabChange( event: NgbNavChangeEvent ) {
        // do prevent it by default, so that hte server side can decide of the swich can happen.
        event.preventDefault();
            for (const tab of this.tabs) { 
            if ( tab._id === event.nextId ) {
                this.select( tab );
                return;
            }
        }
    }
    
    getContainerStyle(element: HTMLElement) : { [property: string]: any }{
        const navpane = element.querySelector('[ngbnavpane]');
        const fullsize = (this.height === '100%');
        if (navpane) {
            if (this.height > 0) this.renderer.setStyle(navpane, 'min-height', this.height + 'px');
            else this.renderer.setStyle(navpane, 'height', '100%');
            this.renderer.setStyle(navpane, 'position', 'relative');
            if (fullsize) {
                const tabs = element.querySelector('ul');
                let calcHeight = tabs.clientHeight;
                const clientRects = tabs.getClientRects();
                if (clientRects && clientRects.length > 0) {
                    calcHeight = tabs.getClientRects()[0].height;
                }
                this.renderer.setStyle(navpane.parentElement, 'height', 'calc(100% - ' + calcHeight + 'px)');
            }
        }
        if (this.servoyApi.isInAbsoluteLayout()) {
            const tabs = element.querySelector('ul');
            let calcHeight = tabs.clientHeight;
            const clientRects = tabs.getClientRects();
            if (clientRects && clientRects.length > 0) {
                calcHeight = tabs.getClientRects()[0].height;
            }
            this.containerStyle['height'] = 'calc(100% - ' + calcHeight + 'px)';
            // should we set this to absolute ? it cannot be relative
            delete this.containerStyle.position;
        } else {
            if (fullsize) {
                this.containerStyle['height'] = this.height;
                if (this.getNativeElement()) this.renderer.setStyle(this.getNativeElement(), 'height', '100%');
            } else {
                this.containerStyle['minHeight'] = this.height + 'px';
            }
        }
        this.containerStyle['marginTop'] = (element.offsetWidth < element.scrollWidth ? 8 : 0) + 'px';
        return this.containerStyle;
    }
}
