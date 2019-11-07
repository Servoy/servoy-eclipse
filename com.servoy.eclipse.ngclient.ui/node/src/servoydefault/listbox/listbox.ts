import { Component, Renderer2, Input} from '@angular/core';

import {FormattingService, StartEditDirective} from '../../ngclient/servoy_public'

import {ServoyDefaultBaseField} from '../basefield'

@Component( {
    selector: 'servoydefault-listbox',
    templateUrl: './listbox.html'
} )
export class ServoyDefaultListBox extends ServoyDefaultBaseField {
    @Input() multiselectListbox;
    
    constructor(renderer: Renderer2, formattingService : FormattingService) { 
        super(renderer,formattingService);
    }
}
