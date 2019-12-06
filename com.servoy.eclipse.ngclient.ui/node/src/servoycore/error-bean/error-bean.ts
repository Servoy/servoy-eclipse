import { Component, ViewChild, Input, Renderer2, ElementRef } from '@angular/core';

@Component({
  selector: 'servoycore-errorbean',
  templateUrl: './error-bean.html',
  styles: ['.svy-errorbean { color: #a94442; }']
})
export class ErrorBean {
    
    @Input() error;
    @Input() servoyApi;
    @Input() toolTipText: string;
    
    @ViewChild('element', {static: false}) elementRef:ElementRef;
    
    constructor(renderer: Renderer2 ) {
    }    
}