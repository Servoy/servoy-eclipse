import { DOCUMENT } from '@angular/common';
import { Component, ChangeDetectorRef, Renderer2, Input, ChangeDetectionStrategy, Inject } from '@angular/core';
import { Format } from '../../ngclient/servoy_public';
import { ServoyBootstrapBasefield } from '../bts_basefield';

@Component({
    selector: 'bootstrapcomponents-textbox',
    templateUrl: './textbox.html',
    styleUrls: ['./textbox.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyBootstrapTextbox extends ServoyBootstrapBasefield<HTMLInputElement> {

    @Input() format: Format;
    @Input() inputType: string;
    @Input() autocomplete: string;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, @Inject(DOCUMENT) doc: Document) {
        super(renderer, cdRef, doc);
    }
}
