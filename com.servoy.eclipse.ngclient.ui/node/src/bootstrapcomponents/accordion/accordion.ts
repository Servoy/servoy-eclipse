import { Component, Renderer2, Input, Output, EventEmitter, ViewChild, SimpleChanges, ElementRef,ContentChild, TemplateRef, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { WindowRefService } from '../../sablo/util/windowref.service';

import { ServoyBootstrapBaseTabPanel,Tab } from '../bts_basetabpanel';
import { NgbPanelChangeEvent } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'bootstrapcomponents-accordion',
  templateUrl: './accordion.html',
  styleUrls: ['./accordion.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyBootstrapAccordion extends ServoyBootstrapBaseTabPanel<HTMLDivElement> {

    panelHeight: number;

    constructor(renderer: Renderer2,protected cdRef: ChangeDetectorRef, windowRefService: WindowRefService) {
        super(renderer,cdRef, windowRefService);
     }

    svyOnChanges( changes: SimpleChanges ) {
        if ( changes['height']) {
            this.updateContentHeight();
        }
        super.svyOnChanges(changes);
    }

    svyOnInit() {
       super.svyOnInit();
       this.updateContentHeight();
    }

    private updateContentHeight() {
        let totalHeight = this.height;
        let wrapper = null;
        if (this.elementRef) {
            wrapper = this.elementRef.nativeElement.closest('svy-wrapper');
        }
        if (wrapper) {
            totalHeight = wrapper.offsetHeight;
        }
        if (this.tabs) {
            totalHeight = totalHeight - 40 * this.tabs.length;
        }
        this.panelHeight = totalHeight;
    }

    onTabChange( event: NgbPanelChangeEvent ) {
        // do prevent it by default, so that hte server side can decide of the swich can happen.
        event.preventDefault();
    }

    selectTabAt( selectionIndex: number ) {
        if ( selectionIndex >= 0 && selectionIndex <= this.tabs.length ) {
            let tabToSelect = this.tabs[selectionIndex];
            if ( tabToSelect.disabled == true ) {
                return;
            }
            this.select( tabToSelect );
        }
    }

    tabClicked(tab: Tab,tabIndexClicked: number, event){
        this.select( this.tabs[tabIndexClicked] );
    }
}
