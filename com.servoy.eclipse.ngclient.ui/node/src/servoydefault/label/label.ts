import { Component, Directive, ViewChild, QueryList, OnInit, Input, Output, EventEmitter, Renderer2, ElementRef, OnChanges, AfterViewInit, SimpleChanges } from '@angular/core';

import {ServoyDefaultBaseLabel} from  '../baselabel'

@Component( {
    selector: 'servoydefault-label',
    templateUrl: './label.html'
} )
export class ServoyDefaultLabel extends ServoyDefaultBaseLabel implements AfterViewInit {
  
    @Input() labelFor;
    
    // this is a hack for test, so that this has a none static child ref because the child is in a nested template
    @ViewChild('child') child:ElementRef;
    @ViewChild('element') elementRef:ElementRef;

    private changes: SimpleChanges;

    constructor(renderer: Renderer2 ) {
        super(renderer);
    }
    
    ngAfterViewInit() {
        super.ngAfterViewInit();
        this.ngOnChanges( this.changes );
    }

    ngOnChanges( changes: SimpleChanges ) {
        if ( !this.elementRef ) {
            if ( this.changes == null ) {
                this.changes = changes;
            }
            else {
                for ( let property in changes ) {
                    this.changes[property] = changes[property];
                }
            }
        }
        else super.ngOnChanges(changes);
    }
}

