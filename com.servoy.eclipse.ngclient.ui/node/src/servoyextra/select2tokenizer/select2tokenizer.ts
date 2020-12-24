import { Component, Renderer2, SimpleChanges, ChangeDetectorRef, ViewChild, Input, Output, EventEmitter } from '@angular/core';
import { Select2Option, Select2UpdateEvent, Select2 } from 'ng-select2-component';
import { ServoyBaseComponent } from '../../ngclient/servoy_public';
import { IValuelist } from '../../sablo/spectypes.service';

@Component({
    selector: 'servoyextra-select2tokenizer',
    templateUrl: './select2tokenizer.html'
})
export class ServoyExtraSelect2Tokenizer extends ServoyBaseComponent<HTMLDivElement> {

 	@Input() onDataChangeMethodID: (e: Event, data?: any) => void;
    @Input() onFocusGainedMethodID: (e: Event, data?: any) => void;
    @Input() onFocusLostMethodID: (e: Event, data?: any) => void;

    @Output() dataProviderIDChange = new EventEmitter();
    @Input() placeholderText: string;
    @Input() readOnly: boolean;
    @Input() valuelistID: IValuelist;
	@Input() styleClass: string;
    @Input() tabSeq: number;
    @Input() toolTipText: string;
	@Input() dataProviderID: any;
	@Input() enabled: boolean;
	
    @ViewChild(Select2) select2: Select2;

    data: Select2Option[] = [];
    filteredDataProviderId: any;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

	svyOnInit() {
        super.svyOnInit();
        this.setData();
		this.attachFocusListeners();
    }

    attachFocusListeners() {
        if (this.onFocusGainedMethodID) {
            this.select2.focus.subscribe(() => {
                this.onFocusGainedMethodID(new CustomEvent('focus'));
            });
        }

        if (this.onFocusLostMethodID) {
            this.select2.blur.subscribe(() => {
                this.onFocusLostMethodID(new CustomEvent('blur'));
            });
        }
    }

    requestFocus() {
        this.select2.toggleOpenAndClose();
    }

   
    setData() {
        if (this.valuelistID) {
            const options: Select2Option[] = [];
            for (let i = 0; i < this.valuelistID.length; i++) {
                options.push({
                    value: this.valuelistID[i].realValue,
                    label: this.valuelistID[i].displayValue
                });
            }
            this.data = options;
        }
    }

    updateValue(event: Select2UpdateEvent<any>) {
        if (this.filteredDataProviderId !== event.value) {
            this.filteredDataProviderId = event.value;
            this.dataProviderID = event.value.join('\n');
            this.dataProviderIDChange.emit(this.dataProviderID);
        }
    }

    svyOnChanges(changes: SimpleChanges) {
        // this change should be ignored for the combobox.
        delete changes['editable'];
        if (changes['valuelistID']) {
            this.setData();
        }
        if (changes['dataProviderID']) {
            this.filteredDataProviderId =  (typeof this.dataProviderID === 'string') ? this.dataProviderID.split('\n') : [this.dataProviderID];
        }
        super.svyOnChanges(changes);
    }
}
