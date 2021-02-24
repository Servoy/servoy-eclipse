import { DOCUMENT } from '@angular/common';
import { Component, Input, ChangeDetectorRef, Renderer2, ChangeDetectionStrategy, Inject } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';

@Component({
  selector: 'bootstrapcomponents-textarea',
  templateUrl: './textarea.html',
  styleUrls: ['./textarea.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyBootstrapTextarea extends ServoyBootstrapBasefield<HTMLTextAreaElement> {

    @Input() maxLength: number;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, @Inject(DOCUMENT) doc: Document) {
        super(renderer, cdRef, doc);
    }

    svyOnInit() {
        super.svyOnInit();
        if (!this.maxLength || this.maxLength === 0) {
            this.maxLength = 524288;
        }
    }

}
