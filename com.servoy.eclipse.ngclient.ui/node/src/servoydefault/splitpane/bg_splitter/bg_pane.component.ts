import { Directive, Component, Input, ElementRef, Renderer2, OnInit, OnDestroy, NgZone } from '@angular/core';

import { BGSplitter } from './bg_splitter.component';

@Directive({
    selector: 'bg-pane',
    host: {
       '[class]':'"split-pane"+index'
     }
})
export class BGPane implements OnInit {
    
    @Input()minSize;
    
    index;
    
    constructor(private splitter:BGSplitter) {
	
    }
    
    ngOnInit() {
        this.index = this.splitter.addPane(this);
    }
    
}