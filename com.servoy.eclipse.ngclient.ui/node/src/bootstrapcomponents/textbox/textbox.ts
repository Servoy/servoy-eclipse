import { DOCUMENT } from '@angular/common';
import { Component, ChangeDetectorRef, Renderer2, Input, ChangeDetectionStrategy, Inject, Output, EventEmitter } from '@angular/core';
import { Format } from '@servoy/public';
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

    @Output() inputTypeChange = new EventEmitter();

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, @Inject(DOCUMENT) doc: Document) {
        super(renderer, cdRef, doc);
    }

    setInputType(inputType: string) {
        const types = ["text", "tel", "date", "time", "datetime-local", "month", "week", "number", "color"];

        if (types.indexOf(inputType) > -1) {
            this.dataProviderID = null;
            this.inputType = inputType;
            this.pushUpdate();
            this.inputTypeChange.emit(this.inputType);
            return true;
        } else {
            return false;
        }
    }
}
