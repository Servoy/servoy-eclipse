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
        this.updateNavpane(element);
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
            if (this.height === '100%') {
                this.containerStyle['height'] = this.height;
                if (this.getNativeElement()) this.renderer.setStyle(this.getNativeElement(), 'height', '100%');
            } else {
                this.containerStyle['minHeight'] = this.height + 'px';
            }
        }
        this.containerStyle['marginTop'] = (element.offsetWidth < element.scrollWidth ? 8 : 0) + 'px';
        return this.containerStyle;
    }

    updateNavpaneTimeout: any;
    updateNavpaneTimeoutCounter: number = 0;
    private updateNavpane(element: HTMLElement) {
        if(this.updateNavpaneTimeout) {
            clearTimeout(this.updateNavpaneTimeout);
            this.updateNavpaneTimeout = null;
        }
        const navpane = element.querySelector('[ngbnavpane].show');
        if (navpane) {
            this.updateNavpaneTimeoutCounter = 0;
            if (this.height > 0) this.renderer.setStyle(navpane, 'min-height', this.height + 'px');
            else this.renderer.setStyle(navpane, 'height', '100%');
            this.renderer.setStyle(navpane, 'position', 'relative');
            if (this.height === '100%') {
                const tabs = element.querySelector('ul');
                let calcHeight = tabs.clientHeight;
                const clientRects = tabs.getClientRects();
                if (clientRects && clientRects.length > 0) {
                    calcHeight = tabs.getClientRects()[0].height;
                }
                this.renderer.setStyle(navpane.parentElement, 'height', 'calc(100% - ' + calcHeight + 'px)');
            }
        } else {
            if(this.updateNavpaneTimeoutCounter < 10) {
                this.updateNavpaneTimeoutCounter++;
                this.updateNavpaneTimeout = setTimeout(() => {
                    this.updateNavpane(element);
                }, 200);
            } else {
                this.log.warn('Could not find navpane in tabpanel');
            }
        }
    }

    ngOnDestroy(): void {
        super.ngOnDestroy();
        if(this.updateNavpaneTimeout) {
            clearTimeout(this.updateNavpaneTimeout);
        }
    }
}
