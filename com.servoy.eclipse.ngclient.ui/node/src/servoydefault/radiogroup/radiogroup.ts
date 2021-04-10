import { Component, OnInit, Renderer2, ChangeDetectorRef, ChangeDetectionStrategy, Inject } from '@angular/core';
import { ServoyDefaultBaseChoice } from '../basechoice';
import { FormattingService, PropertyUtils } from 'servoy-public';
import { DOCUMENT } from '@angular/common';

@Component({
    selector: 'servoydefault-radiogroup',
    templateUrl: './radiogroup.html',
    styleUrls: ['./radiogroup.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyDefaultRadiogroup extends ServoyDefaultBaseChoice {

    value: any;
    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, formattingService: FormattingService, @Inject(DOCUMENT) doc: Document) {
        super(renderer, cdRef, formattingService, doc);
    }

    setSelectionFromDataprovider() {
        this.value = this.dataProviderID;
        if (this.valuelistID) {
            for (let i = 0; i < this.valuelistID.length; i++) {
                const item = this.valuelistID[i];
                if ((item.realValue + '') === (this.dataProviderID + '')) {
                    this.value = item.realValue;
                    break;
                }
            }
        }
    }

    itemClicked(event, index) {
        const newValue = event.target.value ? event.target.value : event.target.innerText;
        const changed = !(newValue === this.value);
        this.value = newValue;

        super.baseItemClicked(event, changed, newValue);
    }

    attachEventHandlers(element, index) {
        this.renderer.listen(element, 'click', (e) => {
            if (!this.readOnly && this.enabled) {
                this.itemClicked(e, index);
                if (this.onActionMethodID) this.onActionMethodID(e);
            }
        });
        super.attachEventHandlers(element, index);
    }

    getSelectedElements() {
        return [this.value];
    }
}
