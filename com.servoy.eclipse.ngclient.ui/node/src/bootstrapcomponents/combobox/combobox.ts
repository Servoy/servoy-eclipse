import { Component, Renderer2, Input, SimpleChanges, ChangeDetectorRef, ViewChild } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';
import { IValuelist } from '../../sablo/spectypes.service';
import { Format, FormattingService } from '../../ngclient/servoy_public';
import { Select2, Select2Option, Select2UpdateEvent } from 'ng-select2-component';

@Component({
    selector: 'bootstrapcomponents-combobox',
    templateUrl: './combobox.html',
    styleUrls: ['./combobox.scss']
})
export class ServoyBootstrapCombobox extends ServoyBootstrapBasefield {

    private static readonly DATEFORMAT = 'ddMMyyyHHmmss';

    @ViewChild(Select2) select2: Select2;

    @Input() format: Format;
    @Input() showAs: string;
    @Input() valuelistID: IValuelist;
    @Input() appendToBody: boolean;

    data: Select2OptionWithReal[] = [];
    filteredDataProviderId: any;

    constructor(renderer: Renderer2, protected cdRef: ChangeDetectorRef, private formatService: FormattingService) {
        super(renderer, cdRef);
    }

    requestFocus() {
        this.select2.toggleOpenAndClose();
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

    svyOnInit(): void {
        super.svyOnInit();
        this.setData();
    }

    svyOnChanges(changes: SimpleChanges) {
        if (changes['valuelistID']) {
            this.setData();
        }
        if (changes['dataProviderID']) {
            // if the real value is a date and the
            const dateFormat = this.valuelistID.isRealValueDate() && this.format.type === 'DATETIME' ? this.format.display : ServoyBootstrapCombobox.DATEFORMAT;
            const format = new Format();
            format.display = dateFormat;
            format.type = 'DATETIME';
            this.filteredDataProviderId = this.valuelistID.isRealValueDate() ?
                this.formatService.format(this.dataProviderID, format, false) :
                this.dataProviderID;
        }
        super.svyOnChanges(changes);
    }

    setData() {
        if (this.valuelistID) {
            const options: Select2OptionWithReal[] = [];
            let formatter = (value) => value;
            if (this.valuelistID.isRealValueDate()) {
                const dateFormat = this.valuelistID.isRealValueDate() && this.format.type === 'DATETIME' ? this.format.display : ServoyBootstrapCombobox.DATEFORMAT;
                const format = new Format();
                format.display = dateFormat;
                format.type = 'DATETIME';
                formatter = (value) => this.formatService.format(value, format, false);
            }
            for (let i = 0; i < this.valuelistID.length; i++) {
                options.push({
                    value: formatter(this.valuelistID[i].realValue),
                    realValue: this.valuelistID[i].realValue,
                    label: this.formatService.format(this.valuelistID[i].displayValue, this.format, false)
                });
            }
            this.data = options;
        }
    }

    updateValue(event: Select2UpdateEvent<any>) {
        this.filteredDataProviderId = event.value;
        if (this.valuelistID.isRealValueDate() && event.value) {
            const value = this.data.find(el => el.value === event.value);
            this.dataProviderID = value.realValue;
        } else this.dataProviderID = event.value;
        this.dataProviderIDChange.emit(this.dataProviderID);
    }
}

interface Select2OptionWithReal extends Select2Option {
    realValue: any;
}
