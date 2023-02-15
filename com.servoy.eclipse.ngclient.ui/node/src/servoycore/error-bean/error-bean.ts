import { Component, ViewChild, Input, Renderer2, ElementRef } from '@angular/core';
import { ServoyBaseComponent } from '@servoy/public';

@Component({
  selector: 'servoycore-errorbean',
  templateUrl: './error-bean.html',
  styles: ['.svy-errorbean { color: #a94442; }']
})
export class ErrorBean extends ServoyBaseComponent<HTMLDivElement> {

    @Input() error;
    @Input() servoyApi;
    @Input() toolTipText: string;

    @ViewChild('element') elementRef: ElementRef;

    constructor(renderer: Renderer2 ) {
        super(renderer, null);
    }
}
