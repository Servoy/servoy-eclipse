import { Directive, input, ViewChild, ElementRef, inject} from '@angular/core';

import { BGSplitter } from './bg_splitter.component';

@Directive({
    selector: 'bg-pane',
    host: {
        '[class]': '"split-pane"+index',
        style: 'overflow:auto'
    },
    standalone: true
})
export class BGPane{

    readonly minSize = input<any>(undefined);

    index: any;

    public element = inject(ElementRef);
}
