import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { SabloTabseq } from '@servoy/public';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Component, Input, Output, EventEmitter, ViewChild, SimpleChanges, ElementRef,ContentChild, TemplateRef, ChangeDetectionStrategy } from '@angular/core';

import { BaseTabpanel,Tab } from '../tabpanel/basetabpanel';

@Component({
    selector: 'servoydefault-accordion',
    templateUrl: './accordion.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [CommonModule, FormsModule, SabloTabseq, NgbModule]
})
export class ServoyDefaultAccordion extends BaseTabpanel {

    panelHeight!: number;

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
        
        let totalHeight = 0;
        let wrapper = null;
        if (this.elementRef) {
            wrapper = this.elementRef.nativeElement.closest('.svy-wrapper');
        }
        if (wrapper) {
            totalHeight = (wrapper as HTMLElement).offsetHeight;
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

    tabClicked(tab: Tab,tabIndexClicked: number, event: any){
        this.select( this.tabs[tabIndexClicked] );
    }
}
