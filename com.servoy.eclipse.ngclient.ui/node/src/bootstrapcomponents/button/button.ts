import { Component, Renderer2, AfterViewInit } from '@angular/core';

import { ServoyBootstrapBaseLabel } from '../bts_baselabel';
 
@Component( {
    selector: 'servoybootstrap-button',
    templateUrl: './button.html',
    styleUrls: ['./button.scss']
} )
export class ServoyBootstrapButton extends ServoyBootstrapBaseLabel implements AfterViewInit {

    constructor(renderer: Renderer2) {
        super(renderer);
    }

    ngAfterViewInit() {
        super.ngAfterViewInit();
        if ( this.onDoubleClickMethodID ) {
            this.renderer.listen( this.elementRef.nativeElement, 'dblclick', ( e ) => {
                this.onDoubleClickMethodID( e );
            } );
        }
    }
}

