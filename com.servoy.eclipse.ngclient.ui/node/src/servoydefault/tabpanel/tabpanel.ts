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

    constructor( windowRefService: WindowRefService, log: LoggerFactory, renderer: Renderer2, cdRef: ChangeDetectorRef ) {
        super( windowRefService, log, renderer, cdRef );
    }

    onTabChange( event: NgbNavChangeEvent ) {
        // do prevent it by default, so that hte server side can decide of the swich can happen.
        event.preventDefault();
        for ( let i = 0; i < this.tabs.length; i++ ) {
            if ( this.tabs[i]._id == event.nextId ) {
                this.select( this.tabs[i] );
                return;
            }
        }
    }

    getSelectedTabId() {
        if ( this.selectedTab ) return this.selectedTab._id;
        const tabIndex = this.getRealTabIndex();
        if (tabIndex > 0) {
            return this.tabs[tabIndex]._id;
        } else if (this.tabs && this.tabs.length > 0) return this.tabs[0]._id;
    }
}
