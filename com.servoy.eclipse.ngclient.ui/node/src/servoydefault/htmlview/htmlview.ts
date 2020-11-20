import { Component, Renderer2, ChangeDetectorRef} from '@angular/core';

import {FormattingService} from '../../ngclient/servoy_public';

import {ServoyDefaultBaseField} from  '../basefield';

@Component( {
    selector: 'servoydefault-htmlview',
    templateUrl: './htmlview.html'
} )
export class ServoyDefaultHTMLView extends ServoyDefaultBaseField {

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, formattingService: FormattingService ) {
        super(renderer, cdRef, formattingService);
    }
}

