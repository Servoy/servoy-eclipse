import { Directive, input, ViewChild, ElementRef} from '@angular/core';

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

    readonly minSize = input<any>(undefined);

    index: any;

    constructor(public element: ElementRef) {

    }
}
