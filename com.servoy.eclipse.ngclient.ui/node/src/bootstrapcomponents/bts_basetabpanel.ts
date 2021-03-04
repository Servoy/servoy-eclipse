import { Renderer2, Input, Output, EventEmitter, SimpleChanges, ContentChild, TemplateRef, Directive, ChangeDetectorRef } from '@angular/core';
import { WindowRefService } from '../sablo/util/windowref.service';
import { ServoyBootstrapBaseComponent } from './bts_basecomp';
import { BaseCustomObject } from '../sablo/spectypes.service';

@Directive()
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export class ServoyBootstrapBaseTabPanel<T extends HTMLElement> extends ServoyBootstrapBaseComponent<T> {
	@Input() onChangeMethodID: (previousIndex: number, event: Event) => void;

	@Input() height: number;
	@Input() tabs: Array<Tab>;

	@Input() tabIndex: number;
	@Output() tabIndexChange = new EventEmitter();

    @Input() activeTabIndex: number;
    @Output() activeTabIndexChange= new EventEmitter();
    
	@ContentChild(TemplateRef, { static: true })
	templateRef: TemplateRef<any>;

	public selectedTabID: string;

	private selectedTab: Tab;
	private waitingForServerVisibility = {};
	private lastSelectedTab: Tab;

	constructor(renderer: Renderer2, protected cdRef: ChangeDetectorRef, protected windowRefService: WindowRefService) {
		super(renderer, cdRef);
	}

	ngOnInit() {
		super.ngOnInit();
		this.generateIDs();
	}

	svyOnChanges(changes: SimpleChanges) {
		if (changes['tabs']) {
			// quickly generate the id's for a the tab html id (and selecting it)
			this.generateIDs();
		}
		if (changes['tabIndex']) {
			Promise.resolve(null).then(() => {
				this.select(this.tabs[this.getRealTabIndex()]);
			});
		}
		super.svyOnChanges(changes);
	}

	getForm(tab: Tab) {
		if (!this.selectedTab) {
			const tabIndex = this.getRealTabIndex();
			if (tabIndex >= 0) this.select(this.tabs[tabIndex]);

			if (!this.selectedTab && this.tabs.length) {
				this.select(this.tabs[0]);
			}
		}
		if (this.selectedTab && (tab.containedForm === this.selectedTab.containedForm) && (tab.relationName === this.selectedTab.relationName)) {
			return tab.containedForm;
		}
		return null;
	}

	select(tab: Tab) {
		if (this.tabs && this.tabs.length > 0 && !this.tabs[0]._id) {
			this.generateIDs();
		}
		if (this.isValidTab(tab)) {
			if ((tab !== undefined && this.selectedTab !== undefined && tab.containedForm === this.selectedTab.containedForm &&
			              tab.relationName === this.selectedTab.relationName) || (tab === this.selectedTab)) return;
			if (this.selectedTab) {
				if (this.selectedTab.containedForm && !this.waitingForServerVisibility[this.selectedTab.containedForm]) {
					const formInWait = this.selectedTab.containedForm;
					this.waitingForServerVisibility[formInWait] = true;
					const currentSelectedTab = this.selectedTab;
					this.lastSelectedTab = tab;
					const promise = this.servoyApi.hideForm(this.selectedTab.containedForm, null, null, tab.containedForm, tab.relationName);
					promise.then((ok) => {
						delete this.waitingForServerVisibility[formInWait];
						if (this.lastSelectedTab !== tab) {
							// visibility changed again, just ignore this
							// it could be that the server was sending the correct state in the mean time already at the same time
							// we try to hide it. just call show again to be sure.
							if (currentSelectedTab === this.selectedTab) this.servoyApi.formWillShow(this.selectedTab.containedForm, this.selectedTab.relationName);
							return;
						}
						if (ok) {
							this.setFormVisible(tab);
						}
					});
				}
			} else {
				this.setFormVisible(tab);
			}
		}
	}

	getSelectedTabId() {
		if (this.tabs && this.tabs.length > 0 && !this.tabs[0]._id) {
			this.generateIDs();
		}
		if (this.selectedTab) return this.selectedTab._id;
		const tabIndex = this.getRealTabIndex();
		if (tabIndex > 0) {
			return this.tabs[tabIndex]._id;
		} else if (this.tabs && this.tabs.length > 0) return this.tabs[0]._id;
	}

	getRealTabIndex(): number {
		if (this.tabIndex) {
			return this.tabIndex - 1;
		}
		if (this.tabs && this.tabs.length > 0) return 0;
		return -1;
	}

	setFormVisible(tab: Tab) {
		if (tab.containedForm) this.servoyApi.formWillShow(tab.containedForm, tab.relationName).finally(() => this.cdRef.markForCheck());
		const oldSelected = this.selectedTab;
		this.selectedTab = tab;
		this.selectedTabID = tab._id;
		this.tabIndex = this.getTabIndex(this.selectedTab);
		this.tabIndexChange.emit(this.tabIndex);
		if (oldSelected && oldSelected !== tab && this.onChangeMethodID) {
			setTimeout(() => {
				this.onChangeMethodID(this.getTabIndex(oldSelected), this.windowRefService.nativeWindow.event != null ? this.windowRefService.nativeWindow.event : null /* TODO $.Event("change") */);
			}, 0, false);
		}
	}

    isValidTab(tab: Tab) {
        if (this.tabs) {
            for (const i of Object.keys(this.tabs)) {
                if (this.tabs[i] === tab) {
                    return true;
                }
            }
        }
        return false;
    }

	private getTabIndex(tab: Tab) {
		if (tab) {
			for (let i = 0; i < this.tabs.length; i++) {
				if (this.tabs[i] === tab) {
					return i + 1;
				}
			}
		}
		return -1;
	}

    private generateIDs() {
        if (this.tabs) {
            for (let i = 0; i < this.tabs.length; i++) {
                this.tabs[i]._id = this.servoyApi.getMarkupId() + '_tab_' + i;
            }
        }
    }
}

export class Tab extends BaseCustomObject {
	_id: string;
	name: string;
	containedForm: string;
	text: string;
	relationName: string;
	disabled: boolean;
	imageMediaID: string;
	hideCloseIcon: boolean;
	iconStyleClass: string;
}
