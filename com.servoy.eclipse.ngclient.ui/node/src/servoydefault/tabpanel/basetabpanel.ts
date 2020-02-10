import { Input, ContentChild, TemplateRef, Output, EventEmitter, OnChanges, SimpleChanges, Renderer2, Directive } from '@angular/core';

import { PropertyUtils, ServoyApi, ServoyBaseComponent } from '../../ngclient/servoy_public'

import { WindowRefService } from '../../sablo/util/windowref.service'

import { BaseCustomObject } from '../../sablo/spectypes.service'

import { LoggerService, LoggerFactory } from '../../sablo/logger.service'

import { ServoyDefaultBaseComponent } from '../basecomponent';



@Directive()
export abstract class BaseTabpanel extends ServoyBaseComponent implements OnChanges {

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
    @Input() transparent;
    @Input() visible;

    @Input() tabIndex;
    @Output() tabIndexChange = new EventEmitter();

    @ContentChild( TemplateRef  , {static: true})
    templateRef: TemplateRef<any>;

    private waitingForServerVisibility = {};
    private lastSelectedTab: Tab;

    protected selectedTab: Tab;
    private log: LoggerService;

    constructor( private windowRefService: WindowRefService, private logFactory : LoggerFactory, renderer:Renderer2 ) {
        super(renderer);
        this.log = logFactory.getLogger("BaseTabpanel");
    }

    ngOnChanges( changes: SimpleChanges ) {
        if ( changes["tabs"] ) {
            // quickly generate the id's for a the tab html id (and selecting it)
            for ( let i = 0; i < this.tabs.length; i++ ) {
                this.tabs[i]._id = this.servoyApi.getMarkupId() + "_tab_" + i;
            }
        }
        if ( changes["tabIndex"] ) {
            Promise.resolve( null ).then(() => { this.select( this.tabs[this.getRealTabIndex()] ) } );
        }
    }

    getForm( tab?: Tab ) {
        if ( !this.selectedTab ) {
            const tabIndex = this.getRealTabIndex();
            if ( tabIndex >= 0 ) this.select( this.tabs[tabIndex] );

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
            this.log.debug(this.log.buildMessage(() => ("svy * Will select tab '" + (tab ? tab.containsFormId : undefined) + "'. Previously selected: '" + (this.selectedTab ? this.selectedTab.containsFormId : undefined) + "'. Same: " + (tab == this.selectedTab))));
            if ( ( tab != undefined && this.selectedTab != undefined && tab.containsFormId == this.selectedTab.containsFormId && tab.relationName == this.selectedTab.relationName ) || ( tab == this.selectedTab ) ) return;
            var selectEvent = this.windowRefService.nativeWindow.event ? this.windowRefService.nativeWindow.event : null;
            if ( this.selectedTab ) {
                if ( this.selectedTab.containsFormId && !this.waitingForServerVisibility[this.selectedTab.containsFormId] ) {
                    const formInWait = this.selectedTab.containsFormId;
                    this.waitingForServerVisibility[formInWait] = true;
                    const currentSelectedTab = this.selectedTab;
                    this.lastSelectedTab = tab;
                    const promise = this.servoyApi.hideForm( this.selectedTab.containsFormId, null, null, tab.containsFormId, tab.relationName );
                    this.log.debug(this.log.buildMessage(() => ("svy * Will hide previously selected form (tab): " + this.selectedTab.containsFormId)));
                    promise.then(( ok ) => {
                        this.log.debug(this.log.buildMessage(() => ("svy * Previously selected form (tab) hide completed with '" + ok + "': " + this.selectedTab.containsFormId)));
                        delete this.waitingForServerVisibility[formInWait];
                        if ( this.lastSelectedTab != tab ) {
                            // visibility changed again, just ignore this
                            this.log.debug(this.log.buildMessage(() => ("svy * Tab '" + tab.containsFormId + "': no longer active, ignore making it visible")));
                            // it could be that the server was sending the correct state in the mean time already at the same time 
                            // we try to hide it. just call show again to be sure.
                            if ( currentSelectedTab == this.selectedTab ) this.servoyApi.formWillShow( this.selectedTab.containsFormId, this.selectedTab.relationName );
                            return;
                        }
                        if ( ok ) {
                            this.setFormVisible( tab, selectEvent );
                        }
                    } )
                }
            }
            else {
                this.setFormVisible( tab, selectEvent );
            }
        }
    }
    
    getSelectedTab():Tab {
        return this.selectedTab;
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
                if ( this.tabs[i] == tab ) {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    protected setFormVisible( tab: Tab, event ) {
        if ( tab.containsFormId ) this.servoyApi.formWillShow( tab.containsFormId, tab.relationName );
        this.log.debug(this.log.buildMessage(() => ("svy * selectedTab = '" + tab.containsFormId + "' -- " + new Date().getTime())));
        var oldSelected = this.selectedTab;
        this.selectedTab = tab;
        this.tabIndex = this.getTabIndex( this.selectedTab );
        this.tabIndexChange.emit( this.tabIndex );
        if ( oldSelected && oldSelected != tab && this.onChangeMethodID ) {
            setTimeout(() => {
                this.onChangeMethodID( this.getTabIndex( oldSelected ), event != null ? event : null /* TODO $.Event("change") */ );
            }, 0, false );
        }
    }

    protected getRealTabIndex(): number {
        if ( this.tabIndex ) {
            if ( isNaN( this.tabIndex ) ) {
                if (!this.tabs) return -1;
                for ( let i = 0; i < this.tabs.length; i++ ) {
                    if (this.tabs[i].name == this.tabIndex) {
                        this.tabIndex = i +1
                        this.tabIndexChange.emit(i);
                        return i;
                    }
                }
                return -1;
            }
            return this.tabIndex - 1;
        }
        if ( this.tabs && this.tabs.length > 0 ) return 0;
        return -1;
    }
}

export class Tab extends BaseCustomObject {
    _id: string;
    name: string;
    containsFormId: string;
    text: string
    relationName: string
    foreground: string
    disabled: boolean
    imageMediaID: string
    mnemonic: string
}