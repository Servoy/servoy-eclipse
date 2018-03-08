import { Input, ContentChild, TemplateRef, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';

import { PropertyUtils, ServoyApi } from '../../ngclient/servoy_public'

import { WindowRefService } from '../../sablo/util/windowref.service'

import { BaseCustomObject } from '../../sablo/spectypes.service'


export abstract class BaseTabpanel implements OnChanges {
    @Input() name: string;
    @Input() servoyApi: ServoyApi;

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
    @Input() tabOrientation;
    @Input() tabSeq;
    @Input() tabs: Array<Tab>
    @Output() tabsChange = new EventEmitter();
    @Input() transparent;
    @Input() visible;

    @Input() tabIndex;
    @Output() tabIndexChange = new EventEmitter();
    @Input() activeTabIndex;
    @Output() activeTabIndexChange = new EventEmitter();

    @ContentChild( TemplateRef )
    private templateRef: TemplateRef<any>;

    private waitingForServerVisibility = {};

    protected selectedTab: Tab

    constructor( private windowRefService: WindowRefService ) {
    }

    ngOnChanges( changes: SimpleChanges ) {
        if ( changes["tabs"] ) {
            // quickly generate the id's for a the tab html id (and selecting it)
            for ( let i = 0; i < this.tabs.length; i++ ) {
                this.tabs[i]._id = this.servoyApi.getMarkupId() + "_tab_" + i;
            }
        }
        if ( changes["tabIndex"] ) {
            let realIndex = this.tabIndex - 1;
            console.log( realIndex );
            this.select( this.tabs[realIndex] );
        }
    }

    getForm( tab: Tab ) {
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
        if ( tab ) {
            if ( this.selectedTab && ( tab.containsFormId == this.selectedTab.containsFormId ) && ( tab.relationName == this.selectedTab.relationName ) ) {
                return tab.containsFormId;
            }
        } else if ( this.selectedTab ) {
            return this.selectedTab.containsFormId;
        }
        return null;
    }

    select( tab: Tab ) {
        if ( !this.visible ) return;
        if ( this.isValidTab( tab ) ) {
            if ( !tab.active ) {
                if ( this.selectedTab ) {
                    this.selectedTab.active = false;
                }
                else {
                    // there is no selected tab yet, make sure all tabs are set to none active 
                    // then the tab that needs to be selected is the only one. 
                    this.tabs.forEach(( tab ) => tab.active = false );
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
                    promise.then(( ok ) => {
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

    private isValidTab( tab: Tab ) {
        if ( this.tabs ) {
            for ( var i = 0; i < this.tabs.length; i++ ) {
                if ( this.tabs[i] === tab ) {
                    return true;
                }
            }
        }
        return false;
    }

    private getTabIndex( tab: Tab ) {
        if ( tab ) {
            for ( var i = 0; i < this.tabs.length; i++ ) {
                if ( ( this.tabs[i].containsFormId == tab.containsFormId ) && ( this.tabs[i].relationName == tab.relationName ) ) {
                    return i + 1;
                }
            }
        }
        return -1;
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
        this.tabsChange.emit( this.tabs );
    }

    protected setFormVisible( tab: Tab, event ) {
        console.log( "setting form visible: " + tab.containsFormId )
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
}

export class Tab extends BaseCustomObject {
    _id: string;
    name: string;
    containsFormId: string;
    text: string
    relationName: string
    active: boolean
    foreground: string
    disabled: boolean
    imageMediaID: string
    mnemonic: string

    private _isActive: boolean;

    // only push to server properties are generated with get/setters
    get isActive(): boolean {
        return this._isActive
    }
    set isActive( value: boolean ) {
        if ( this.getStateHolder().markIfChanged( "isActive", value, this._isActive ) ) {
            this._isActive = value;
        }
    }
}