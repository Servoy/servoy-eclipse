
import { Component, ChangeDetectionStrategy } from '@angular/core';

import {ServoyDefaultBaseField} from '../basefield';

@Component( {
    selector: 'servoydefault-textfield',
    templateUrl: './textfield.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
} )
export class ServoyDefaultTextField extends ServoyDefaultBaseField<HTMLInputElement> {

    onModelChange(newValue: any) {
        if(newValue && typeof newValue.getTime === 'function' && isNaN(newValue.getTime())) {
            // invalid date, force dataprovider display with invalid date text
            this.dataProviderID.set(null);
            this.cdRef.detectChanges();
            this.dataProviderID.set(newValue);
            this.cdRef.detectChanges();
        }
        else {
            this.dataProviderID.set(newValue);
        }
    }
    
    onClick(event: any){
        if (this.editable() == false && this.onActionMethodID()) {
            this.onActionMethodID()(event)
        }
    }
}
