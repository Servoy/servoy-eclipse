import { Component, Input, ChangeDetectorRef, Renderer2, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';

@Component({
  selector: 'bootstrapcomponents-textarea',
  templateUrl: './textarea.html',
  styleUrls: ['./textarea.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyBootstrapTextarea extends ServoyBootstrapBasefield<HTMLTextAreaElement> {

    @Input() maxLength: number;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

    svyOnInit() {
        super.svyOnInit();
        if (!this.maxLength || this.maxLength === 0) {
            this.maxLength = 524288;
        }
    }

}
