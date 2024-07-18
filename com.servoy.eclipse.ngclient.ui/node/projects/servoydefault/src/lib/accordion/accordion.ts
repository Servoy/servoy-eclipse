import { Component, Renderer2, Input, Output, EventEmitter, ViewChild, SimpleChanges, ElementRef,ContentChild, TemplateRef, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { WindowRefService } from '@servoy/public';

import { BaseTabpanel,Tab } from '../tabpanel/basetabpanel';
import { LoggerFactory } from '@servoy/public';

@Component({
  selector: 'servoydefault-accordion',
  templateUrl: './accordion.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyDefaultAccordion extends BaseTabpanel {

    panelHeight: number;

    constructor(windowRefService: WindowRefService , logFactory: LoggerFactory, renderer: Renderer2,protected cdRef: ChangeDetectorRef) {
        super( windowRefService, logFactory, renderer, cdRef);
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
        
        let totalHeight : number;
        let wrapper = null;
        if (this.elementRef) {
            wrapper = this.elementRef.nativeElement.closest('.svy-wrapper');
        }
        if (wrapper) {
            totalHeight = wrapper.offsetHeight;
        }
        if (this.tabs) {
            totalHeight = totalHeight - 40 * this.tabs.length;
        }
        this.panelHeight = totalHeight;
    }

    selectTabAt( selectionIndex: number ) {
        if ( selectionIndex >= 0 && selectionIndex < this.tabs.length ) {
            let tabToSelect = this.tabs[selectionIndex];
            if ( tabToSelect.disabled == true ) {
                return;
            }
            this.select( tabToSelect );
        }
    }

    getSelectedTabId() : any{
        const id = super.getSelectedTabId();
        if (id == null) return [];
        return id;
    }

    tabClicked(tab: Tab,tabIndexClicked: number, event){
        this.select( this.tabs[tabIndexClicked] );
    }
}
