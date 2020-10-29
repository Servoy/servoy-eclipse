import { Component, Renderer2, Input, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';
import { IValuelist } from '../../sablo/spectypes.service';
import { Format, FormattingService } from '../../ngclient/servoy_public';
import { Select2Option, Select2UpdateEvent } from 'ng-select2-component';

@Component({
    selector: 'servoybootstrap-combobox',
    templateUrl: './combobox.html',
    styleUrls: ['./combobox.scss']
})
export class ServoyBootstrapCombobox extends ServoyBootstrapBasefield {

    private static readonly DATEFORMAT = 'ddMMyyyHHmmss';

    @Input() format: Format;
    @Input() showAs;
    @Input() valuelistID: IValuelist;

    data: Select2OptionWithReal[] = [];
    filteredDataProviderId: any;

    constructor(renderer: Renderer2, protected cdRef: ChangeDetectorRef, private formatService: FormattingService) {
        super(renderer, cdRef);
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
            this.filteredDataProviderId = this.valuelistID.isRealValueDate() ?
                this.formatService.format(this.dataProviderID, dateFormat , 'DATETIME') :
                this.dataProviderID;
        }
        super.svyOnChanges(changes);
    }

    setData() {
        if (this.valuelistID) {
            const options: Select2OptionWithReal[] = [];
            let formatter = ( value ) => {
                return value;
            };
            if (this.valuelistID.isRealValueDate() ) {
                const dateFormat = this.valuelistID.isRealValueDate() && this.format.type === 'DATETIME' ? this.format.display : ServoyBootstrapCombobox.DATEFORMAT;
                formatter = ( value ) => {
                    return this.formatService.format(value, dateFormat , 'DATETIME');
                };
            }
            for (let i = 0; i < this.valuelistID.length; i++) {
                options.push({
                    value: formatter(this.valuelistID[i].realValue),
                    realValue: this.valuelistID[i].realValue,
                    label: this.formatService.format(this.valuelistID[i].displayValue, this.format.display, this.format.type)
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
