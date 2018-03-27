import { Component, Input, Output, EventEmitter ,ViewEncapsulation} from '@angular/core';

import { BGPane } from './bg_pane.component'
@Component( {
    selector: 'bg-splitter',
    template: '<div class="split-panes {{orientation}}"><ng-content></ng-content></div>',
    styleUrls: ['./bg_splitter.css'],
    encapsulation: ViewEncapsulation.None
} )
export class BGSplitter {
    
    @Input()orientation = "vertical"
        
    @Output() onDividerChange = new EventEmitter();

    private panes:Array<BGPane> = [];
    
    constructor() {

    }

    public addPane( pane: BGPane ) {
        if ( this.panes.length > 1 )
            throw 'splitters can only have two panes';
        this.panes.push( pane );
        return this.panes.length;
    }
}
