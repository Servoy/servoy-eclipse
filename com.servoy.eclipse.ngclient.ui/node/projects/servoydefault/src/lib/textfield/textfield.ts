
import { Component, Renderer2, ChangeDetectorRef, ChangeDetectionStrategy, Inject, DOCUMENT } from '@angular/core';

import {FormattingService} from '@servoy/public';

import {ServoyDefaultBaseField} from '../basefield';

@Component( {
    selector: 'servoydefault-textfield',
    templateUrl: './textfield.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
} )
export class ServoyDefaultTextField extends ServoyDefaultBaseField<HTMLInputElement> {

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef , formattingService: FormattingService, @Inject(DOCUMENT) doc: Document) {
        super(renderer, cdRef, formattingService, doc);
    }

    onModelChange(newValue) {
        if(newValue && typeof newValue.getTime === 'function' && isNaN(newValue.getTime())) {
            // invalid date, force dataprovider display with invalid date text
            this.dataProviderID = null;
            this.cdRef.detectChanges();
            this.dataProviderID = newValue;
            this.cdRef.detectChanges();
        }
        else {
            this.dataProviderID = newValue;
        }
    }
    
    onClick(event){
        if (this.editable == false && this.onActionMethodID) {
            this.onActionMethodID(event)
        }
    }
}
