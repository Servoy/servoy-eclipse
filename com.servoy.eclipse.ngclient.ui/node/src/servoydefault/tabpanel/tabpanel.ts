import { Component, ViewChild, Input, Output, EventEmitter, Renderer2 } from '@angular/core';

import { BaseTabpanel, Tab } from "./basetabpanel"

import { PropertyUtils, ServoyApi } from '../../ngclient/servoy_public'

import { WindowRefService } from '../../sablo/util/windowref.service'
import { LoggerFactory } from '../../sablo/logger.service'

import { NgbTabset, NgbTabChangeEvent } from "@ng-bootstrap/ng-bootstrap";


@Component( {
    selector: 'servoydefault-tabpanel',
    templateUrl: './tabpanel.html'
} )
export class ServoyDefaultTabpanel extends BaseTabpanel {
    @ViewChild( 'tabset' )
    private tabset: NgbTabset;

    constructor( windowRefService: WindowRefService, log : LoggerFactory, renderer: Renderer2) {
        super( windowRefService, log, renderer );
    }

    onTabChange( event: NgbTabChangeEvent ) {
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
        }
        else if (this.tabs && this.tabs.length > 0) return this.tabs[0]._id;
    }
}
