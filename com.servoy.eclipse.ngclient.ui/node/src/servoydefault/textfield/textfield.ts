import { Component, Renderer2, ChangeDetectorRef, ChangeDetectionStrategy} from '@angular/core';

import {FormattingService} from '../../ngclient/servoy_public';

import {ServoyDefaultBaseField} from '../basefield';

@Component( {
    selector: 'servoydefault-textfield',
    templateUrl: './textfield.html',
    changeDetection: ChangeDetectionStrategy.OnPush
} )
export class ServoyDefaultTextField extends ServoyDefaultBaseField<HTMLInputElement> {

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef , formattingService: FormattingService) {
        super(renderer, cdRef, formattingService);
    }

    attachFocusListeners(nativeElement: any) {
        if (this.onFocusGainedMethodID)
            this.renderer.listen( nativeElement, 'focus', ( e ) => {
                if ( this.mustExecuteOnFocus === true ) {
                    this.onFocusGainedMethodID( e );
                }
                this.mustExecuteOnFocus = true;
            } );
        if (this.onFocusLostMethodID)
            this.renderer.listen( nativeElement, 'blur', ( e ) => {
                this.onFocusLostMethodID(e);
            } );
    }
}
