import { Component, Renderer2} from '@angular/core';

import {FormattingService} from '../../ngclient/servoy_public'

import {ServoyDefaultBaseField} from  '../basefield'

@Component( {
    selector: 'servoydefault-imagemedia',
    templateUrl: './imagemedia.html'
} )
export class ServoyDefaultImageMedia extends ServoyDefaultBaseField {
  
    constructor(renderer: Renderer2, formattingService : FormattingService ) {
        super(renderer,formattingService);
    }
    
    deleteMedia(): void {
        this.update(null);
    }
    
    downloadMedia(): void {
        if (this.dataProviderID) {
            let x = window.screenTop + 100;
            let y = window.screenLeft + 100;
            window.open(this.dataProviderID.url ? this.dataProviderID.url : this.dataProviderID, 'download', 'top=' + x + ',left=' + y + ',screenX=' + x
                    + ',screenY=' + y + ',location=no,toolbar=no,menubar=no,width=310,height=140,resizable=yes');
        }
    }
}

