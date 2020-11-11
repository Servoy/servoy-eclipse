import { Component, Renderer2, SimpleChanges, ChangeDetectorRef} from '@angular/core';
import { FormattingService } from '../../ngclient/servoy_public';
import { Select2Option, Select2UpdateEvent } from 'ng-select2-component';
import { ServoyDefaultBaseField } from '../basefield';

@Component({
  selector: 'servoydefault-combobox',
  templateUrl: './combobox.html'
})
export class ServoyDefaultCombobox extends ServoyDefaultBaseField {

  private static readonly DATEFORMAT = 'ddMMyyyHHmmss';

  data: Select2OptionWithReal[] = [];
  filteredDataProviderId: any;

  constructor(renderer: Renderer2, cdRef: ChangeDetectorRef,
              formattingService: FormattingService) {
    super(renderer, cdRef, formattingService);
  }

  svyOnInit() {
      super.svyOnInit();
      this.setData();
  }
  setData() {
    if (this.valuelistID) {
      const options: Select2OptionWithReal[] = [];
      let formatter = ( value ) => {
          return value;
      };
      if (this.valuelistID.isRealValueDate() ) {
          const dateFormat = this.valuelistID.isRealValueDate() && this.format.type === 'DATETIME' ? this.format.display : ServoyDefaultCombobox.DATEFORMAT;
          formatter = ( value ) => {
              return this.formattingService.format(value, dateFormat , 'DATETIME');
          };
      }
      for (let i = 0; i < this.valuelistID.length; i++) {
          options.push({
              value: formatter(this.valuelistID[i].realValue),
              realValue: this.valuelistID[i].realValue,
              label: this.formattingService.format(this.valuelistID[i].displayValue, this.format.display, this.format.type)
          });
      }
      this.data = options;
}
  }

  updateValue(event: Select2UpdateEvent<any>) {
    if (this.filteredDataProviderId !== event.value) {
      this.filteredDataProviderId = event.value;
      if (this.valuelistID.isRealValueDate() && event.value) {
          const value = this.data.find(el => el.value === event.value);
          this.dataProviderID = value.realValue;
      } else this.dataProviderID = event.value;
      this.dataProviderIDChange.emit(this.dataProviderID);
      if (this.onActionMethodID) this.onActionMethodID( new CustomEvent('click') );
    }
  }

  svyOnChanges( changes: SimpleChanges ) {
    // this change should be ignored for the combobox.
    delete changes['editable'];
    if (changes['valuelistID']) {
      this.setData();
    }
    if (changes['dataProviderID']) {
        // if the real value is a date and the
        const dateFormat = this.valuelistID.isRealValueDate() && this.format.type === 'DATETIME' ? this.format.display : ServoyDefaultCombobox.DATEFORMAT;
        this.filteredDataProviderId = this.valuelistID.isRealValueDate() ?
            this.formattingService.format(this.dataProviderID, dateFormat , 'DATETIME') :
            this.dataProviderID;
    }
    super.svyOnChanges(changes);
  }
}

interface Select2OptionWithReal extends Select2Option {
  realValue: any;
}
