import { Component, Renderer2, AfterViewInit, ViewChild, ElementRef, SimpleChanges } from '@angular/core';

import { ServoyBootstrapBaseLabel } from '../bts_baselabel';
 
@Component( {
    selector: 'servoybootstrap-button',
    templateUrl: './button.html',
    styleUrls: ['./button.scss']
} )
export class ServoyBootstrapButton extends ServoyBootstrapBaseLabel implements AfterViewInit {

    @ViewChild('element') elementRef: ElementRef;
    private changes: SimpleChanges;
    
    constructor(renderer: Renderer2) {
        super(renderer);
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
        else {
            super.ngOnChanges( changes );
        }
    }
    
    ngAfterViewInit() {
        super.ngAfterViewInit();
        this.ngOnChanges( this.changes );
        if ( this.onDoubleClickMethodID ) {
            this.renderer.listen( this.elementRef.nativeElement, 'dblclick', ( e ) => {
                this.onDoubleClickMethodID( e );
            } );
        }
    }
}

