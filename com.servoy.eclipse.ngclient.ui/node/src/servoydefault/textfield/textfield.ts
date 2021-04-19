import { DOCUMENT } from '@angular/common';
import { Component, Renderer2, ChangeDetectorRef, ChangeDetectionStrategy, Inject} from '@angular/core';

import {FormattingService} from '@servoy/public';

import {ServoyDefaultBaseField} from '../basefield';

@Component( {
    selector: 'servoydefault-textfield',
    templateUrl: './textfield.html',
    changeDetection: ChangeDetectionStrategy.OnPush
} )
export class ServoyDefaultTextField extends ServoyDefaultBaseField<HTMLInputElement> {

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef , formattingService: FormattingService, @Inject(DOCUMENT) doc: Document) {
        super(renderer, cdRef, formattingService, doc);
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
