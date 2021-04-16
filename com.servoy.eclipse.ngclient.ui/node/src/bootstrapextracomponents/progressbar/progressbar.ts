import { Component, Output, EventEmitter, Input, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBaseComponent } from '@servoy/public';

@Component({
    selector: 'bootstrapextracomponents-progressbar',
    templateUrl: './progressbar.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyBootstrapExtraProgressBar extends ServoyBaseComponent<HTMLDivElement> {
    @Input() styleClass: string;
    @Input() value: number;
    @Input() type: string;
    @Input() animate: boolean;
    @Input() showValue: boolean;
    @Input() showValueAsPercentage: boolean;
    @Input() valueText: string;
    @Input() max: number;
    @Input() tabSeq: number;

    svyOnInit() {
        super.svyOnInit();
    }

    updateProgressBar = function(value, text) {
        this.value = value;
        if (text != undefined) {
            this.valueText = text;
        }
    }

}

