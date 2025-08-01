
import { Component, Renderer2, ChangeDetectorRef, ChangeDetectionStrategy, Inject, DOCUMENT } from '@angular/core';

import {FormattingService} from '@servoy/public';

import {ServoyDefaultBaseField} from  '../basefield';

@Component( {
    selector: 'servoydefault-htmlview',
    templateUrl: './htmlview.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
} )
export class ServoyDefaultHTMLView extends ServoyDefaultBaseField<HTMLDivElement> {

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, formattingService: FormattingService, @Inject(DOCUMENT) doc: Document ) {
        super(renderer, cdRef, formattingService, doc);
    }
}

