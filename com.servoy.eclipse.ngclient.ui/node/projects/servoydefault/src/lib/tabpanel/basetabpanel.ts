import { Input, ContentChild, TemplateRef, Output, EventEmitter, SimpleChanges, Renderer2, Directive, ChangeDetectorRef } from '@angular/core';

import { BaseCustomObject, ServoyBaseComponent, WindowRefService } from '@servoy/public';

import { LoggerService, LoggerFactory } from '@servoy/public';

@Directive()
export abstract class BaseTabpanel extends ServoyBaseComponent<HTMLDivElement> {

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
    @Input() tabs: Array<Tab>;
    @Input() transparent;

    @Input() tabIndex;
    @Output() tabIndexChange = new EventEmitter();

    @ContentChild(TemplateRef, { static: true })
    templateRef: TemplateRef<any>;

    public selectedTabID: string;

    protected selectedTab: Tab;

    private waitingForServerVisibility = {};
    private lastSelectedTab: Tab;

    protected log: LoggerService;

    constructor(private windowRefService: WindowRefService, logFactory: LoggerFactory, renderer: Renderer2, cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
        this.log = logFactory.getLogger('BaseTabpanel');
    }

    ngOnInit() {
        super.ngOnInit();
        this.initTabID();
    }

    svyOnChanges(changes: SimpleChanges) {
        if (changes['tabs']) {
            // quickly generate the id's for a the tab html id (and selecting it)
            this.initTabID();
            if (!changes['tabs'].firstChange && this.selectedTab) {
                let index = this.getRealTabIndex();
                if (index >= 0) {
                    this.select(this.tabs[index]);
                }
            }
        }
        if (changes['tabIndex']) {
            Promise.resolve(null).then(() => {
                this.select(this.tabs[this.getRealTabIndex()]);
            });
        }
        super.svyOnChanges(changes);
    }

    getForm(tab?: Tab) {
        if (!this.selectedTab) {
            const tabIndex = this.getRealTabIndex();
            if (tabIndex >= 0) this.select(this.tabs[tabIndex]);

            if (!this.selectedTab && this.tabs.length) {
                this.select(this.tabs[0]);
            }
        }
        if (tab) {
            if (this.selectedTab && (tab.containsFormId === this.selectedTab.containsFormId) && (tab.relationName === this.selectedTab.relationName)) {
                return tab.containsFormId;
            }
        } else if (this.selectedTab) {
            return this.selectedTab.containsFormId;
        }
        return null;
    }

    select(tab: Tab) {
        if (this.isValidTab(tab)) {
            this.log.debug(this.log.buildMessage(() => ('svy * Will select tab \'' + (tab ? tab.containsFormId : undefined) + '\'. Previously selected: \'' +
                (this.selectedTab ? this.selectedTab.containsFormId : undefined) + '\'. Same: ' + (tab === this.selectedTab))));
           if (this.isValidTab(this.selectedTab) && this.selectedTab === tab) return;
            const selectEvent = this.windowRefService.nativeWindow.event ? this.windowRefService.nativeWindow.event : null;
            if (this.selectedTab) {
                if (this.selectedTab.containsFormId && !this.waitingForServerVisibility[this.selectedTab.containsFormId]) {
                    const formInWait = this.selectedTab.containsFormId;
                    this.waitingForServerVisibility[formInWait] = true;
                    const currentSelectedTab = this.selectedTab;
                    this.lastSelectedTab = tab;
                    const promise = this.servoyApi.hideForm(this.selectedTab.containsFormId, null, null, tab.containsFormId, tab.relationName, this.getTabIndex(tab) - 1);
                    this.log.debug(this.log.buildMessage(() => ('svy * Will hide previously selected form (tab): ' + this.selectedTab.containsFormId)));
                    promise.then((ok) => {
                        this.log.debug(this.log.buildMessage(() => ('svy * Previously selected form (tab) hide completed with \'' + ok + '\': ' + this.selectedTab.containsFormId)));
                        delete this.waitingForServerVisibility[formInWait];
                        if (this.lastSelectedTab !== tab) {
                            // visibility changed again, just ignore this
                            this.log.debug(this.log.buildMessage(() => ('svy * Tab \'' + tab.containsFormId + '\': no longer active, ignore making it visible')));
                            // it could be that the server was sending the correct state in the mean time already at the same time
                            // we try to hide it. just call show again to be sure.
                            if (currentSelectedTab === this.selectedTab) this.servoyApi.formWillShow(this.selectedTab.containsFormId, this.selectedTab.relationName,
                                    this.getTabIndex(this.selectedTab) - 1);
                            return;
                        }
                        if (ok) {
                            this.setFormVisible(tab, selectEvent, false);
                        }
                    });
                }
            } else {
                this.setFormVisible(tab, selectEvent, true);
            }
        }
    }

    getSelectedTab(): Tab {
        return this.selectedTab;
    }

    getSelectedTabId() {
        if ( this.selectedTab ) return this.selectedTab._id;
        const tabIndex = this.getRealTabIndex();
        if (tabIndex > 0) {
            return this.tabs[tabIndex]._id;
        } else if (this.tabs && this.tabs.length > 0) return this.tabs[0]._id;
        return null;
    }
    
    protected setFormVisible(tab: Tab, event, callShow: boolean) {
        if (callShow && tab.containsFormId)
            this.servoyApi.formWillShow(tab.containsFormId, tab.relationName, this.getTabIndex(tab) - 1).finally(() => this.cdRef.markForCheck());
        this.log.debug(this.log.buildMessage(() => ('svy * selectedTab = \'' + tab.containsFormId + '\' -- ' + new Date().getTime())));
        const oldSelected = this.selectedTab;
        this.selectedTab = tab;
        this.selectedTabID = tab._id;
        this.tabIndex = this.getTabIndex(this.selectedTab);
        this.tabIndexChange.emit(this.tabIndex);
        if (oldSelected && oldSelected !== tab && this.onChangeMethodID) {
            setTimeout(() => {
                this.onChangeMethodID(this.getTabIndex(oldSelected), event != null ? event : null /* TODO $.Event("change") */);
            }, 0, false);
        }
    }

    protected getRealTabIndex(): number {
        if (this.tabIndex) {
            if (isNaN(this.tabIndex)) {
                if (!this.tabs) return -1;
                for (let i = 0; i < this.tabs.length; i++) {
                    if (this.tabs[i].name === this.tabIndex) {
                        this.tabIndex = i + 1;
                        this.tabIndexChange.emit(i);
                        return i;
                    }
                }
                return -1;
            }
            return this.tabIndex - 1;
        }
        if (this.tabs && this.tabs.length > 0) return 0;
        return -1;
    }

    private isValidTab(tab: Tab) {
        if (this.tabs) {
            for (const t of this.tabs) {
                if (t === tab) {
                    return true;
                }
            }
        }
        return false;
    }

    getTabIndex(tab: Tab) {
        if (tab) {
            for (let i = 0; i < this.tabs.length; i++) {
                if (this.tabs[i] === tab) {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    private initTabID() {
        for (let i = 0; i < this.tabs.length; i++) {
            this.tabs[i]._id = this.servoyApi.getMarkupId() + '_tab_' + i;
        }
    }
}

export class Tab extends BaseCustomObject {
    _id: string;
    name: string;
    containsFormId: string;
    text: string;
    relationName: string;
    foreground: string;
    disabled: boolean;
    imageMediaID: string;
    mnemonic: string;
    toolTipText: string;
}
