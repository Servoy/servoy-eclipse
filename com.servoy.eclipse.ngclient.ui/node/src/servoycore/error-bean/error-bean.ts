import { Component, ChangeDetectorRef, Renderer2, input } from '@angular/core';
import { ServoyBaseComponent } from '@servoy/public';

@Component({
    selector: 'servoycore-errorbean',
    templateUrl: './error-bean.html',
    styles: ['.svy-errorbean { color: #a94442; }'],
    standalone: false
})
export class ErrorBean extends ServoyBaseComponent<HTMLDivElement> {

    readonly error = input(undefined);
    readonly toolTipText = input<string>(undefined);

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }
}
