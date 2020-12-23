import { Component, ChangeDetectorRef, Renderer2, Input, SimpleChanges } from '@angular/core';
import { Format } from '../../ngclient/servoy_public';
import { ServoyBootstrapBasefield } from '../bts_basefield';

@Component({
    selector: 'bootstrapcomponents-textbox',
    templateUrl: './textbox.html',
    styleUrls: ['./textbox.scss']
})
export class ServoyBootstrapTextbox extends ServoyBootstrapBasefield {

    @Input() format: Format;
    @Input() inputType: string;
    @Input() autocomplete: string;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }
}
