import { Component, ChangeDetectorRef, Input, Renderer2 } from '@angular/core';
import { ServoyBaseComponent } from '@servoy/public';

@Component({
    selector: 'servoycore-errorbean',
    templateUrl: './error-bean.html',
    styles: ['.svy-errorbean { color: #a94442; }'],
    standalone: false
})
export class ErrorBean extends ServoyBaseComponent<HTMLDivElement> {

    @Input() error;
    @Input() toolTipText: string;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }
}
