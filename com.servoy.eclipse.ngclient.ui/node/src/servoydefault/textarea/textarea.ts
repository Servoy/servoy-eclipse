import { Component, Renderer2} from '@angular/core';

import {FormattingService, StartEditDirective} from '../../ngclient/servoy_public'

import {ServoyDefaultBaseField} from '../basefield'

@Component( {
    selector: 'servoydefault-textarea',
    templateUrl: './textarea.html'
} )
export class ServoyDefaultTextArea extends ServoyDefaultBaseField {
    constructor(renderer: Renderer2, formattingService : FormattingService) { 
        super(renderer,formattingService);
    }
    
    public selectAll() {
        this.getNativeElement().select();
    }
}
