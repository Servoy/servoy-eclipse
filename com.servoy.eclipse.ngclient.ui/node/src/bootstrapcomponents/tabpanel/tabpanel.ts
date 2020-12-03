import { Component, Renderer2, Input, Output, EventEmitter, ChangeDetectorRef, ViewChild } from '@angular/core';
import { WindowRefService } from '../../sablo/util/windowref.service';

import { ServoyBootstrapBaseTabPanel, Tab } from '../bts_basetabpanel';
import { NgbNavChangeEvent } from '@ng-bootstrap/ng-bootstrap';

@Component({
	selector: 'bootstrapcomponents-tabpanel',
	templateUrl: './tabpanel.html',
	styleUrls: ['./tabpanel.scss']
})
export class ServoyBootstrapTabpanel extends ServoyBootstrapBaseTabPanel {

	@Input() onTabClickedMethodID;
	@Input() onTabCloseMethodID;

	@Input() showTabCloseIcon;
	@Input() closeIconStyleClass;

	@Input() tabIndex;
	@Output() tabIndexChange = new EventEmitter();
	@ViewChild('nav') nav;

	constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, windowRefService: WindowRefService) {
		super(renderer, cdRef, windowRefService);
	}

	onTabChange(event: NgbNavChangeEvent) {
		// do prevent it by default, so that the server side can decide of the swich can happen.
		event.preventDefault();
	}

	tabClicked(tab: Tab, tabIndexClicked: number, event) {
		if (event.target.classList.contains('bts-tabpanel-close-icon')) {
			if (this.onTabCloseMethodID) {
				const promise = this.onTabCloseMethodID(this.windowRefService.nativeWindow.event != null ? this.windowRefService.nativeWindow.event : null /* TODO $.Event("tabclicked") */, tabIndexClicked + 1);
				promise.then((ok) => {
					if (ok) {
						this.removeTabAt(tabIndexClicked + 1);
					}
				});
			} else {
				this.removeTabAt(tabIndexClicked + 1);
			}
		} else {
			if (tab.disabled === true) {
				return;
			}

			if (this.onTabClickedMethodID) {
				/*var dataTargetAttr = $(event.target).closest('[data-target]');
				var dataTarget = dataTargetAttr ? dataTargetAttr.attr('data-target') : null;*/
				const promise = this.onTabClickedMethodID(this.windowRefService.nativeWindow.event != null ? this.windowRefService.nativeWindow.event : null /*$.Event("tabclicked")*/, tabIndexClicked + 1, null);
				promise.then((ok) => {
					if (ok) {
						this.select(this.tabs[tabIndexClicked]);
					}
				});
			} else {
				this.select(this.tabs[tabIndexClicked]);
			}
		}
	}

	removeTabAt(removeIndex: number) {
		// copied from the serverside code
		if (removeIndex > 0 && removeIndex <= this.tabs.length) {
			const oldTabIndex = this.tabIndex;
			let formToHide;
			let formToShow;
			if (this.tabIndex === removeIndex) {
				formToHide = this.tabs[removeIndex - 1];

				const nextIndex = this.getFirstEnabledTabIndexNotAtIndex(this.tabIndex);
				// if the tabIndex after removal will remain the same after removal, shall force showForm
				if ((nextIndex > -1 && nextIndex === this.tabIndex + 1) && this.tabs.length > 1) {
					// get the tab at second position
					formToShow = this.tabs[nextIndex - 1];
				}
			}

			// remove the tab
			// create a new tabObject, so angular-ui is properly refreshed.
			const newTabs = [];
			for (let i = 0; i < this.tabs.length; i++) {
				if (i == removeIndex - 1) continue;
				newTabs.push(this.tabs[i]);
			}
			this.tabs = newTabs;


			// update the tabIndex
			if (this.tabIndex >= removeIndex) {
				if (this.tabIndex === removeIndex) {
					let newTabIndex = this.getFirstEnabledTabIndex();
					if (newTabIndex > - 1) {
						this.tabIndex = newTabIndex;
					} else {
						// deselect all tabs setting tabIndex to 0
						this.tabIndex = 0;
						newTabIndex = 0;
					}
				} else {
					this.tabIndex--;
				}
			}

			// hide the form
			if (formToHide) {
				// hide the current form
				if (formToHide.containedForm && !formToShow) {
					// TODO what if doesn't hide ?
					this.servoyApi.hideForm(formToHide.containedForm);
				}

				// show the next form if the tabIndex was 1 and has not changed
				if (formToShow && formToShow.containedForm) {
					// This will happen only when the first tab is the visible tab and i am closing the first tab.
					// The previous tab already call the onHide.. here i force the onShow of the "next" tab.. since the $scope.model.tabIndex doesn't change
					// Using ng-repeat="tab in model.tabs track by $index" to make angularui aware of the change.

					this.servoyApi.formWillShow(formToShow.containedForm, formToShow.relationName);
					if (this.onChangeMethodID) {
						setTimeout(() => {
							this.onChangeMethodID(1, this.windowRefService.nativeWindow.event != null ? this.windowRefService.nativeWindow.event : null /* TODO $.Event("change") */);
						}, 0, false);
					}
				}
			}
		}
	}

	getFirstEnabledTabIndexNotAtIndex(skipIndex: number) {
		for (let i = 0; this.tabs && i < this.tabs.length; i++) {
			const tab = this.tabs[i];
			if (tab.disabled !== true && (skipIndex !== i + 1)) {
				return i + 1;
			}
		}
		return -1;
	}

	getFirstEnabledTabIndex() {
		for (let i = 0; this.tabs && i < this.tabs.length; i++) {
			const tab = this.tabs[i];
			if (tab.disabled !== true) {
				return i + 1;
			}
		}
		return -1;
	}
	
	setFormVisible(tab: Tab){
		super.setFormVisible(tab);
		this.nav.select(tab._id);
	}
}
