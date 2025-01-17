import { DOCUMENT } from '@angular/common';
import { Component, Renderer2, Output, ChangeDetectorRef, ChangeDetectionStrategy, Inject } from '@angular/core';
import { FormattingService } from '@servoy/public';
import { ServoyDefaultBaseChoice } from '../basechoice';

@Component({
    selector: 'servoydefault-checkgroup',
    templateUrl: './checkgroup.html',
    styleUrls: ['./checkgroup.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ServoyDefaultCheckGroup extends ServoyDefaultBaseChoice {

    @Output() mainTabIndex;

    constructor(renderer: Renderer2, formattingService: FormattingService, cdRef: ChangeDetectorRef, @Inject(DOCUMENT) doc: Document) {
        super(renderer, cdRef, formattingService, doc);
    }

    getFocusElement(): HTMLElement {
        let focusElement = super.getFocusElement();
        if (focusElement === null) focusElement = this.getNativeElement();
        return focusElement;
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
        for (let i = 0; i < this.valuelistID.length; i++) {
            const item = this.valuelistID[i];
            if (!this.isValueListNull(item)) {
                this.selection[i - this.allowNullinc] = arr.find(value => item.realValue + '' === value + '') !== undefined;
            }
        }
    }

    itemClicked(event: Event, index: number) {
        const allowMultiselect = !this.format || this.format.type === 'TEXT';
        const prevValue = this.selection[index];
        const element = event.target as HTMLInputElement;
        if (allowMultiselect || this.findmode) {
            this.selection[index] = element.checked;
            if(!this.findmode && this.allowNullinc === 0 &&  this.selection.filter(a => a === true).length === 0) {
                this.selection[index] = true;
                element.checked = true;
            }
        } else {
            this.selection.fill(false);
            this.selection[index] = element.checked;
            if (!this.selection[index] && this.allowNullinc === 0) {
                 this.selection[index] = true;
                 element.checked = true;
            }
        }
        const changed = prevValue !== this.selection[index];
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

