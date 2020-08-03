import { Component, Renderer2, Input, Output, EventEmitter, ViewChild, SimpleChanges, ElementRef,ContentChild, TemplateRef,ViewEncapsulation } from '@angular/core';
import { WindowRefService } from '../../sablo/util/windowref.service';

import { ServoyBootstrapBaseTabPanel,Tab } from '../bts_basetabpanel';
import { NgbPanelChangeEvent } from "@ng-bootstrap/ng-bootstrap";

@Component({
  selector: 'servoybootstrap-accordion',
  templateUrl: './accordion.html',
  styleUrls: ['./accordion.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ServoyBootstrapAccordion extends ServoyBootstrapBaseTabPanel {

    private panelHeight: number;

    constructor(renderer: Renderer2,windowRefService: WindowRefService) {
        super(renderer,windowRefService);
     }
    
    ngOnChanges( changes: SimpleChanges ) {
        if ( changes["height"]) {
            this.updateContentHeight();
        }
        super.ngOnChanges(changes);
    }
    
    ngAfterViewInit() {
       super.ngAfterViewInit();
       this.updateContentHeight();
    }
    
    private updateContentHeight()
    {
        let totalHeight = this.height;
        let wrapper = null;
        if (this.elementRef)
        {
            wrapper = this.elementRef.nativeElement.closest('svy-wrapper');
        }
        if (wrapper)
        {
            totalHeight = wrapper.offsetHeight;
        }
        if (this.tabs)
        {
            totalHeight = totalHeight - 40 * this.tabs.length;
        }    
        this.panelHeight = totalHeight;
    }
    
    onTabChange( event: NgbPanelChangeEvent ) {
        // do prevent it by default, so that hte server side can decide of the swich can happen.
        event.preventDefault();
    }
    

    tabClicked(tab: Tab,tabIndexClicked : number, event){
        this.select( this.tabs[tabIndexClicked] ); 
    }
}
