import { Component,ChangeDetectorRef,Renderer2 } from '@angular/core';

import {ServoyDefaultBaseLabel} from  '../baselabel';

@Component( {
    selector: 'servoydefault-button',
    templateUrl: './button.html',
    styleUrls: ['./style.scss']
} )
export class ServoyDefaultButton extends ServoyDefaultBaseLabel {

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }
}

