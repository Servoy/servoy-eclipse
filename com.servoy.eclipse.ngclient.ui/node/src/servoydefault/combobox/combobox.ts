import { Component, Renderer2, SimpleChanges, ChangeDetectorRef, ViewChild} from '@angular/core';
import { Format, FormattingService } from '../../ngclient/servoy_public';
import { Select2Option, Select2UpdateEvent, Select2 } from 'ng-select2-component';
import { ServoyDefaultBaseField } from '../basefield';

@Component({
  selector: 'servoydefault-combobox',
  templateUrl: './combobox.html'
})
export class ServoyDefaultCombobox extends ServoyDefaultBaseField {

  private static readonly DATEFORMAT = 'ddMMyyyHHmmss';

  @ViewChild(Select2) select2: Select2;

  data: Select2OptionWithReal[] = [];
  filteredDataProviderId: any;

  constructor(renderer: Renderer2, cdRef: ChangeDetectorRef,
              formattingService: FormattingService) {
    super(renderer, cdRef, formattingService);
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

  svyOnInit() {
      super.svyOnInit();
      this.setData();
  }
  setData() {
    if (this.valuelistID) {
      const options: Select2OptionWithReal[] = [];
      let formatter = ( value ) => value;
      if (this.valuelistID.isRealValueDate() ) {
          const dateFormat = this.valuelistID.isRealValueDate() && this.format.type === 'DATETIME' ? this.format.display : ServoyDefaultCombobox.DATEFORMAT;
          const format = new Format();
          format.display = dateFormat;
          format.type = 'DATETIME';
          formatter = ( value ) => this.formattingService.format(value, format, false);
      }
      for (let i = 0; i < this.valuelistID.length; i++) {
          options.push({
              value: formatter(this.valuelistID[i].realValue),
              realValue: this.valuelistID[i].realValue,
              label: this.formattingService.format(this.valuelistID[i].displayValue, this.format, false)
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
        const format = new Format();
        format.display = dateFormat;
        format.type = 'DATETIME';
        this.filteredDataProviderId = this.valuelistID.isRealValueDate() ?
            this.formattingService.format(this.dataProviderID, format, false) :
            this.dataProviderID;
    }
    super.svyOnChanges(changes);
  }
}

interface Select2OptionWithReal extends Select2Option {
  realValue: any;
}
