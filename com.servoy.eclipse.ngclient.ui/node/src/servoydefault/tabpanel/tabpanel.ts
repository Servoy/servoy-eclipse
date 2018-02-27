import { Component, Directive, ContentChild, ViewChild, QueryList, OnInit, Input, Output, EventEmitter, Renderer2, TemplateRef, ElementRef, OnChanges, SimpleChanges } from '@angular/core';

import { PropertyUtils, ServoyApi} from '../../ngclient/servoy_public'

import {WindowRefService} from '../../sablo/util/windowref.service'

@Component( {
    selector: 'servoydefault-tabpanel',
    templateUrl: './tabpanel.html'
} )
export class ServoyDefaultTabpanel implements OnInit, OnChanges {

    @Input() name:string;
    @Input() servoyApi:ServoyApi;

    @Input() onChangeMethodID;

    @Input() background;
    @Input() borderType;
    @Input() enabled;
    @Input() fontType;
    @Input() foreground;
    @Input() horizontalAlignment;
    @Input() location;
    @Input() readOnly;
    @Input() selectedTabColor;
    @Input() size;
    @Input() styleClass;
    @Input() tabIndex;
    @Output() tabIndexChange = new EventEmitter();
    @Input() tabOrientation;
    @Input() tabSeq;
    @Input() tabs;
    @Output() tabsChange = new EventEmitter();
    @Input() transparent;
    @Input() visible;
    @Input() activeTabIndex;
    @Output() activeTabIndexChange = new EventEmitter();

    private selectedTab;
    private waitingForServerVisibility;
    
    @ContentChild(TemplateRef) templateRef:TemplateRef<any>;

    constructor(private windowRefService:WindowRefService) {

    }

    ngOnInit() {

    }

    ngOnChanges( changes ) {
        console.log( changes )
    }

    getForm( tab ) {
        if ( !this.selectedTab ) {
            for ( var i = 0; i < this.tabs.length; i++ ) {
                if ( this.tabs[i].active ) {
                    this.select( this.tabs[i] );
                    break;
                }
            }

            if ( !this.selectedTab && this.tabs.length ) {
                this.select( this.tabs[0] );
            }
        }
        if ( this.selectedTab && ( tab.containsFormId == this.selectedTab.containsFormId ) && ( tab.relationName == this.selectedTab.relationName ) ) {
            if ( this.servoyApi.touchForm(tab.containsFormId )) {
                return tab.containsFormId;
            }
        }
        return null;
    }

    select( tab ) {
        if ( !this.visible ) return;
        if ( this.isValidTab( tab ) ) {
            if ( !tab.active ) {
                if ( this.selectedTab ) {
                    this.selectedTab.active = false;
                }
                tab.active = true;
                this.updateActiveTabIndex();
            }
            //                if ($log.debugEnabled) $log.debug("svy * Will select tab '" + (tab ? tab.containsFormId : undefined) + "'. Previously selected: '" + (this.selectedTab ? this.selectedTab.containsFormId : undefined) + "'. Same: " + (tab == this.selectedTab));
            if ( ( tab != undefined && this.selectedTab != undefined && tab.containsFormId == this.selectedTab.containsFormId && tab.relationName == this.selectedTab.relationName ) || ( tab == this.selectedTab ) ) return;
            var selectEvent = this.windowRefService.nativeWindow.event ? this.windowRefService.nativeWindow.event : null;
            if ( this.selectedTab ) {
                if ( this.selectedTab.containsFormId && !this.waitingForServerVisibility[this.selectedTab.containsFormId] ) {
                    var formInWait = this.selectedTab.containsFormId;
                    this.waitingForServerVisibility[formInWait] = true;
                    var currentSelectedTab = this.selectedTab;
                    var promise = this.servoyApi.hideForm( this.selectedTab.containsFormId, null, null, tab.containsFormId, tab.relationName );
                    //                        if ($log.debugEnabled) $log.debug("svy * Will hide previously selected form (tab): " + this.selectedTab.containsFormId);
                    promise.then( function( ok ) {
                        //                            if ($log.debugEnabled) $log.debug("svy * Previously selected form (tab) hide completed with '" + ok + "': " + this.selectedTab.containsFormId);
                        delete this.waitingForServerVisibility[formInWait];
                        if ( !tab.active ) {
                            // visibility changed again, just ignore this
                            //                                if ($log.debugEnabled) $log.debug("svy * Tab '" + tab.containsFormId + "': no longer active, ignore making it visible");
                            // it could be that the server was sending the correct state in the mean time already at the same time 
                            // we try to hide it. just call show again to be sure.
                            if ( currentSelectedTab == this.selectedTab && this.selectedTab.active ) this.servoyApi.formWillShow( this.selectedTab.containsFormId, this.selectedTab.relationName );
                            return;
                        }
                        if ( ok ) {
                            this.setFormVisible( tab, selectEvent );
                        }
                        else {
                            tab.active = false;
                            this.selectedTab.active = true;
                            this.updateActiveTabIndex();
                        }
                    } )
                }
            }
            else {
                this.setFormVisible( tab, selectEvent );
            }
        }
    }


    getTabIndex( tab ) {
        if ( tab ) {
            for ( var i = 0; i < this.tabs.length; i++ ) {
                if ( ( this.tabs[i].containsFormId == tab.containsFormId ) && ( this.tabs[i].relationName == tab.relationName ) ) {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    private setFormVisible( tab, event ) {
        if ( tab.containsFormId ) this.servoyApi.formWillShow( tab.containsFormId, tab.relationName );
        //            if ($log.debugEnabled) $log.debug("svy * selectedTab = '" + tab.containsFormId + "' -- " + new Date().getTime());
        var oldSelected = this.selectedTab;
        this.selectedTab = tab;
        this.tabIndex = this.getTabIndex( this.selectedTab );
        if ( oldSelected && oldSelected != tab && this.onChangeMethodID ) {
            setTimeout(() => {
                this.onChangeMethodID( this.getTabIndex( oldSelected ), event != null ? event : null /* TODO $.Event("change") */ );
            }, 0, false );
        }
    }

    private isValidTab( tab ) {
        if ( this.tabs ) {
            for ( var i = 0; i < this.tabs.length; i++ ) {
                if ( this.tabs[i] === tab ) {
                    return true;
                }
            }
        }
        return false;
    }

    private updateActiveTabIndex() {
        if ( this.tabs ) {
            this.activeTabIndex = 0;

            var activeSet = false;
            for ( var i = 0; i < this.tabs.length; i++ ) {
                this.tabs[i].isActive = activeSet ? false : this.tabs[i].active;
                if ( !activeSet && this.tabs[i].active ) {
                    this.activeTabIndex = i;
                    activeSet = true;
                }
            }
        }
    }
}