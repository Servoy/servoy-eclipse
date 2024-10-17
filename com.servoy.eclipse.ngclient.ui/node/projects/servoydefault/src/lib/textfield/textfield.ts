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

    private initialValue: any;
    private changeEmitted = false;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef , formattingService: FormattingService, @Inject(DOCUMENT) doc: Document) {
        super(renderer, cdRef, formattingService, doc);
    }

    attachFocusListeners(nativeElement: any) {
        super.attachFocusListeners(nativeElement);

        this.renderer.listen(nativeElement, 'focus', (e) => {
            this.initialValue = nativeElement.value;
            this.changeEmitted = false;
        });

        this.renderer.listen(nativeElement, 'blur', (e) => {
            if (this.changeEmitted) return;

            const currentValue = nativeElement.value;
            
            if (currentValue !== this.initialValue) {
                this.onModelChange(currentValue);
                this.handleChangeEvent(e); 
                this.changeEmitted = true;
            }
        });
    }

    handleChangeEvent(event: Event) {
        if (!this.changeEmitted) {
            this.pushUpdate();
            this.changeEmitted = true;
        }
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
