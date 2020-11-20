import { Component, OnInit, Input, ViewChild, ElementRef, Renderer2, SimpleChanges,ChangeDetectorRef } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';
import { IValuelist } from '../../sablo/spectypes.service';
import { ShowDisplayValuePipe } from '../lib/showDisplayValue.pipe';

@Component({
  selector: 'bootstrapcomponents-select',
  templateUrl: './select.html',
  styleUrls: ['./select.scss']
})
export class ServoyBootstrapSelect extends ServoyBootstrapBasefield {

  @Input() valuelistID: IValuelist;
  @Input() multiselect;
  @Input() selectSize;
  selectedValues: any[];

  constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, private showDisplayValuePipe: ShowDisplayValuePipe) {
    super(renderer, cdRef);
  }

  svyOnChanges( changes: SimpleChanges ) {
    if (changes && this.elementRef) {
      for ( const property of Object.keys(changes) ) {
          const change = changes[property];
          switch ( property ) {
              case 'dataProviderID':
                  if (this.multiselect && this.dataProviderID) {
                    this.selectedValues = ('' + this.dataProviderID).split('\n');
                  }
                  if (change.currentValue) this.renderer.setAttribute(this.getNativeElement(), 'placeholder', change.currentValue );
                  else  this.renderer.removeAttribute(this.getNativeElement(), 'placeholder' );
                  break;
            }
        }
        super.svyOnChanges(changes);
    }
  }

  showPlaceholder() {
    if (!this.placeholderText || this.placeholderText.length === 0) {
 return false;
}
    const displayValue = this.showDisplayValuePipe.transform(this.dataProviderID, this.valuelistID, true, true);
    return displayValue == null || (displayValue == '' && this.dataProviderID === null);
  }

  isDPinValuelist() {
    let isDPinValuelist = false;
    if (this.valuelistID) {
      for (let i = 0; i < this.valuelistID.length; i++) {
        if (this.dataProviderID == this.valuelistID[i].realValue) {
          isDPinValuelist = true;
          break;
        }
      }
    }
    return isDPinValuelist;
  }

  onChange(event) {
    this.renderer.removeAttribute(this.getNativeElement(), 'placeholder');
    if (this.updateDataprovider() && this.onActionMethodID) {
      this.onActionMethodID(event);
    }
  }

  updateDataprovider() {
    if (this.valuelistID) {
      let value = null;
      for (let i = 0; i < this.valuelistID.length; i++) {
        if (this.multiselect) {
          if (this.selectedValues.indexOf(this.valuelistID[i].displayValue) != -1) {
              if (value == null) value = [];
              value.push(this.valuelistID[i].realValue);
            }
        } else {
          if (this.dataProviderID == this.valuelistID[i].displayValue) {
            value = this.valuelistID[i].realValue;
          }
        }
      }
      if (this.multiselect && value) {
        value = value.join('\n');
      }
      if ((this.dataProviderID + '') != (value + '')) {
        this.updateValue(value);
        return true;
      }
    }
    return false;
  }

  updateValue(val: string) {
    this.dataProviderID = val;
    super.pushUpdate();
  }
}
