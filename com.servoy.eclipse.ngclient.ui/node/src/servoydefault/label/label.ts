import { Component, Input,  Renderer2, ChangeDetectorRef  } from '@angular/core';

import {ServoyDefaultBaseLabel} from  '../baselabel'

@Component( {
    selector: 'servoydefault-label',
    templateUrl: './label.html'
} )
export class ServoyDefaultLabel extends ServoyDefaultBaseLabel {
  
    @Input() labelFor;
    
    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef ) {
        super(renderer, cdRef);
    }
}

