import { Directive, Input ,ViewChild,ElementRef} from '@angular/core';

import { BGSplitter } from './bg_splitter.component';

@Directive({
    selector: 'bg-pane',
    host: {
        '[class]': '"split-pane"+index',
        style: 'overflow:auto'
    },
    standalone: false
})
export class BGPane{

    @Input()minSize: any;

    index: any;

    constructor(public element: ElementRef) {

    }
}
