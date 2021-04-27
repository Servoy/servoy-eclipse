import { DOCUMENT } from '@angular/common';
import { Component, ChangeDetectorRef, Renderer2, ViewChild, Input, ElementRef, ChangeDetectionStrategy, Inject, Output, EventEmitter, SimpleChanges } from '@angular/core';
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

    svyOnChanges(changes: SimpleChanges) {
        super.svyOnChanges(changes);
        if (changes.inputType) {
            this.renderer.setAttribute(this.elementRef.nativeElement, 'type', this.inputType);
        }
    }

    setInputType(inputType: string) {
        const types = ['text', 'tel', 'date', 'time', 'datetime-local', 'month', 'week', 'number', 'color'];

        if (types.indexOf(inputType) > -1) {
            this.inputType = inputType;
            this.renderer.setAttribute(this.elementRef.nativeElement, 'type', this.inputType);
            this.inputTypeChange.emit(this.inputType);
            const dp = this.dataProviderID;
            if (dp) {
                this.dataProviderID = null;
                this.cdRef.detectChanges();
                this.dataProviderID = dp;
                this.cdRef.detectChanges();
            }
            return true;
        } else {
            return false;
        }
    }
}
