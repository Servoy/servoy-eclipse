import { Component, Input,  Renderer2, ChangeDetectorRef, ChangeDetectionStrategy  } from '@angular/core';

import {ServoyDefaultBaseLabel} from  '../baselabel';

@Component( {
    selector: 'servoydefault-label',
    templateUrl: './label.html',
    changeDetection: ChangeDetectionStrategy.OnPush
} )
export class ServoyDefaultLabel extends ServoyDefaultBaseLabel<HTMLDivElement> {

    @Input() labelFor;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef ) {
        super(renderer, cdRef);
    }
}

