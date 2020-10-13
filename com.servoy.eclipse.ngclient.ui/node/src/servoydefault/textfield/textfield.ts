import { Component, Renderer2, ChangeDetectorRef} from '@angular/core';

import {FormattingService} from '../../ngclient/servoy_public'

import {ServoyDefaultBaseField} from '../basefield'

@Component( {
    selector: 'servoydefault-textfield',
    templateUrl: './textfield.html'
} )
export class ServoyDefaultTextField extends ServoyDefaultBaseField {
    
    valueBeforeChange: any;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef ,formattingService : FormattingService) { 
        super(renderer, cdRef, formattingService);
    }
    
    attachFocusListeners(nativeElement : any){
        if(this.onFocusGainedMethodID)
            this.renderer.listen( nativeElement, 'focus', ( e ) => {
                this.onFocusGainedMethodID(e);
                this.valueBeforeChange = nativeElement.value;
            } );
        if(this.onFocusLostMethodID)
            this.renderer.listen( nativeElement, 'blur', ( e ) => {
                this.onFocusLostMethodID(e);
            } ); 
    }
    
    update( val: string ) {
        if(!this.findmode && this.format) {
            var newDataProviderID = this.formattingService.parse(val, this.format, this.dataProviderID);
            if(this.dataProviderID == newDataProviderID) {
                this.getNativeElement().value = this.valueBeforeChange;
            }
        }
        super.update(val);
    }
}
