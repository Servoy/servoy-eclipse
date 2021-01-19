import { Component, Renderer2, ChangeDetectorRef, ChangeDetectionStrategy} from '@angular/core';

import {FormattingService} from '../../ngclient/servoy_public';

import {ServoyDefaultBaseField} from  '../basefield';

@Component( {
    selector: 'servoydefault-htmlview',
    templateUrl: './htmlview.html',
    changeDetection: ChangeDetectionStrategy.OnPush
} )
export class ServoyDefaultHTMLView extends ServoyDefaultBaseField<HTMLDivElement> {

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, formattingService: FormattingService ) {
        super(renderer, cdRef, formattingService);
    }
}

