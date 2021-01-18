import { Component, OnInit, Input, Renderer2, ElementRef, ViewChild, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { IValuelist } from '../../sablo/spectypes.service';
import { ServoyBootstrapBasefield } from '../bts_basefield';

@Component({
    selector: 'bootstrapcomponents-choicegroup',
    templateUrl: './choicegroup.html',
    styleUrls: ['./choicegroup.scss']
})
export class ServoyBootstrapChoicegroup extends ServoyBootstrapBasefield<HTMLDivElement> {

    @Input() inputType: string;
    @Input() findmode: boolean;
    @Input() valuelistID: IValuelist;
    @Input() showAs: string;

    @ViewChild('input') input: ElementRef<HTMLInputElement>;

    selection: any[] = [];
    allowNullinc = 0;

    constructor(renderer: Renderer2, protected cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

    svyOnInit() {
        super.svyOnInit();
        this.onValuelistChange();
    }

    svyOnChanges(changes: SimpleChanges) {
        for (const property of Object.keys(changes)) {
            switch (property) {
                case 'dataProviderID':
                    this.setSelectionFromDataprovider();
                    break;

            }
        }
        super.svyOnChanges(changes);
    }

    requestFocus( mustExecuteOnFocusGainedMethod: boolean ) {
        this.mustExecuteOnFocus = mustExecuteOnFocusGainedMethod;
        const choiceInput = this.input;
        choiceInput.nativeElement.focus();
    }

    setHandlersAndTabIndex() {
        for (let i = 0; i < this.getNativeElement().children.length; i++) {
            const elm = this.getNativeElement().children[i];
            this.attachEventHandlers(elm.children[0] as HTMLElement, i);
        }
    }

    onValuelistChange() {
        if (this.valuelistID)
            if (this.valuelistID.length > 0 && this.isValueListNull(this.valuelistID[0])) this.allowNullinc = 1;
        this.setHandlersAndTabIndex();
    }

    getDataproviderFromSelection() {
        let returnValue = '';
        this.selection.forEach((element, index) => {
            if (element === true)
                returnValue += this.valuelistID[index + this.allowNullinc].realValue + '\n';
        });
        returnValue = returnValue.replace(/\n$/, ''); // remove the last \n
        if (returnValue === '') returnValue = null;
        return returnValue;
    }

    setSelectionFromDataprovider() {
        this.selection = [];
        if (this.dataProviderID === null || this.dataProviderID === undefined) return;
        const arr = (typeof this.dataProviderID === 'string') ? this.dataProviderID.split('\n') : [this.dataProviderID];
        arr.forEach((element, index, array) => {
            for (let i = 0; i < this.valuelistID.length; i++) {
                const item = this.valuelistID[i];
                if (item.realValue + '' === element + '' && !this.isValueListNull(item)) {
                    if (this.inputType === 'radio') {
                        if (arr.length > 1) this.selection = []; else this.selection[i - this.allowNullinc] = item.realValue;
                    } else {
                        this.selection[i - this.allowNullinc] = true;
                    }
                }
            }
        });
    }

    isValueListNull = (item) => (item.realValue === null || item.realValue === '') && item.displayValue === '';

    itemClicked(event, index) {
        let changed = true;
        if (this.inputType === 'radio') {
            this.dataProviderID = this.valuelistID[index + this.allowNullinc].realValue;
        } else {
            this.selection[index] = event.target.checked;
            let checkedTotal = 0;
            for (const i of  this.selection) {
                if (this.selection[i] === true) checkedTotal++;
            }
            changed = !(checkedTotal == 0 && this.allowNullinc === 0 && !this.findmode);
            if (!changed) {
                this.selection[index] = true;
            }
            this.dataProviderID = this.getDataproviderFromSelection();
        }
        if (changed) this.pushUpdate();
        event.target.blur();
    }

    attachHandlers() {
        // just ignore this.
    }
    
    attachEventHandlers(element: HTMLElement, index: number) {
        if (element) {
            this.renderer.listen(element, 'click', (event) => {
                if (!this.readOnly && this.enabled) {
                    this.itemClicked(event, index);
                    if (this.onActionMethodID) this.onActionMethodID(event);
                }
            });
            this.attachFocusListeners(element);
        }
    }
}
