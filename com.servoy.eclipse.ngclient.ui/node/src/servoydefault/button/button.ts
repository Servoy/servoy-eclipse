import { Component,ChangeDetectorRef,Renderer2, ChangeDetectionStrategy } from '@angular/core';

import {ServoyDefaultBaseLabel} from  '../baselabel';

@Component( {
    selector: 'servoydefault-button',
    templateUrl: './button.html',
    styleUrls: ['./style.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
} )
export class ServoyDefaultButton extends ServoyDefaultBaseLabel<HTMLButtonElement> {

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }
}

