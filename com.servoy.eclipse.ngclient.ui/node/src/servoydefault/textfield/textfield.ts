import { Component, Renderer2} from '@angular/core';

import {FormattingService, DecimalkeyconverterDirective, StartEditDirective} from '../../ngclient/servoy_public'

import {ServoyDefaultBaseField} from '../basefield'

@Component( {
    selector: 'servoydefault-textfield',
    templateUrl: './textfield.html'
} )
export class ServoyDefaultTextField extends ServoyDefaultBaseField {
    constructor(renderer: Renderer2, formattingService : FormattingService) { 
        super(renderer,formattingService);
    }
}
