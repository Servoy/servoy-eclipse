import { Component, Directive, ViewChild, QueryList, OnInit, Input, Output, EventEmitter, Renderer2, ElementRef, OnChanges, AfterViewInit, SimpleChanges } from '@angular/core';

import {ServoyDefaultBaseLabel} from  '../baselabel'

@Component( {
    selector: 'servoydefault-label',
    templateUrl: './label.html'
} )
export class ServoyDefaultLabel extends ServoyDefaultBaseLabel implements AfterViewInit {
  
    @Input() labelFor;

    private changes: SimpleChanges;

    constructor(renderer: Renderer2 ) {
        super(renderer);
    }
    
    ngAfterViewInit() {
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

