import { DOCUMENT } from '@angular/common';
import { Component, Renderer2, Output, ChangeDetectorRef, ChangeDetectionStrategy, Inject } from '@angular/core';
import { FormattingService } from 'servoy-public';
import { ServoyDefaultBaseChoice } from '../basechoice';

@Component({
    selector: 'servoydefault-checkgroup',
    templateUrl: './checkgroup.html',
    styleUrls: ['./checkgroup.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyDefaultCheckGroup extends ServoyDefaultBaseChoice {

    @Output() mainTabIndex;

    constructor(renderer: Renderer2, formattingService: FormattingService, cdRef: ChangeDetectorRef, @Inject(DOCUMENT) doc: Document) {
        super(renderer, cdRef, formattingService, doc);
    }

    getDataproviderFromSelection() {
        const allowMultiselect = !this.format || this.format.type === 'TEXT';
        let ret = allowMultiselect ? '' : null;
        this.selection.forEach((element, index) => {
            if (element === true)
                ret = allowMultiselect ? ret + this.valuelistID[index + this.allowNullinc].realValue + '\n' :
                    this.valuelistID[index + this.allowNullinc].realValue + '';
        });
        if (allowMultiselect) ret = ret.replace(/\n$/, ''); // remove the last \n
        if (ret === '') ret = null;
        return ret;
    }

    setSelectionFromDataprovider() {
        this.selection = [];
        if (this.dataProviderID === null || this.dataProviderID === undefined) return;
        const arr = (typeof this.dataProviderID === 'string') ? this.dataProviderID.split('\n') : [this.dataProviderID];
        arr.forEach((element, index, array) => {
            for (let i = 0; i < this.valuelistID.length; i++) {
                const item = this.valuelistID[i];
                if (item.realValue + '' === element + '' && !this.isValueListNull(item)) this.selection[i - this.allowNullinc] = true;
            }
        });
    }

    itemClicked(event, index) {
        const checkedTotal = this.selection.filter(a => a === true).length;
        let changed = true;
        if (event.target.checked) {
            if (!(!this.format || this.format.type === 'TEXT') && checkedTotal > 1) {
                this.selection.map(() => false);
            }
            this.selection[index] = true;
        } else {
            event.target.checked = this.selection[index] = this.allowNullinc === 0 && checkedTotal <= 1 && !this.findmode;
            changed = !event.target.checked;
        }
        super.baseItemClicked(event, changed, this.getDataproviderFromSelection());
    }

    attachEventHandlers(element, index) {
        this.renderer.listen(element, 'click', (event) => {
            if (!this.readOnly && this.enabled) {
                this.itemClicked(event, index);
                if (this.onActionMethodID) this.onActionMethodID(event);
            }
        });
        super.attachEventHandlers(element, index);
    }

    ngAfterViewChecked() {
        this.mainTabIndex = this.getNativeElement().getAttribute('tabindex');
        this.cdRef.detectChanges();
    }
}

