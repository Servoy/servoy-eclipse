import { Component,ChangeDetectorRef,Renderer2, ChangeDetectionStrategy } from '@angular/core';

import {ServoyDefaultBaseLabel} from  '../baselabel';

@Component( {
    selector: 'servoydefault-button',
    templateUrl: './button.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
} )
export class ServoyDefaultButton extends ServoyDefaultBaseLabel<HTMLButtonElement> {

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }
}

