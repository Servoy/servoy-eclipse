import { Component, Renderer2 , ChangeDetectorRef, ChangeDetectionStrategy} from '@angular/core';

import { BaseTabpanel, Tab } from './basetabpanel';

import { WindowRefService, ServoyPublicService } from '@servoy/public';
import { LoggerFactory } from '@servoy/public';

import { NgbNavChangeEvent } from '@ng-bootstrap/ng-bootstrap';


@Component( {
    selector: 'servoydefault-tabpanel',
    templateUrl: './tabpanel.html',
    changeDetection: ChangeDetectionStrategy.OnPush
} )
export class ServoyDefaultTabpanel extends BaseTabpanel {
    
    containerStyle: { [property: string]: any } = { position: 'relative', overflow: 'auto' };
    
    constructor( windowRefService: WindowRefService, log: LoggerFactory, renderer: Renderer2, cdRef: ChangeDetectorRef, servoyPublicService: ServoyPublicService ) {
        super( windowRefService, log, renderer, cdRef, servoyPublicService );
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
        const tabs = element.querySelector('ul');
        this.containerStyle['height'] = 'calc(100% - ' + tabs.clientHeight + 'px)';
        this.applyOverflowFromForm(this.containerStyle);
        return this.containerStyle;
    }
}
