import { Component, OnInit, Input, ViewChild, ElementRef, Renderer2, SimpleChanges } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';
import { IValuelist } from '../../sablo/spectypes.service';
import { ShowDisplayValuePipe } from '../lib/showDisplayValue.pipe';

@Component({
  selector: 'servoybootstrap-select',
  templateUrl: './select.html',
  styleUrls: ['./select.scss']
})
export class ServoyBootstrapSelect extends ServoyBootstrapBasefield implements OnInit {

  @ViewChild('element') elementRef: ElementRef;
  
  @Input() valuelistID: IValuelist;
  @Input() multiselect: boolean;
  @Input() selectSize;
  selectedValues: any[];

  constructor(renderer: Renderer2, private showDisplayValuePipe: ShowDisplayValuePipe) {
    super(renderer);
  }

  ngOnInit(): void {
  }

  ngOnChanges( changes: SimpleChanges ) {
    if (changes) {
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
        super.ngOnChanges(changes);
    }
  }

  showPlaceholder() {
    if (!this.placeholderText || this.placeholderText.length === 0) { return false; }
    const displayValue = this.showDisplayValuePipe.transform(this.dataProviderID, this.valuelistID, true, true);
    return displayValue == null || (displayValue == "" && this.dataProviderID === null);
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
    this.renderer.removeAttribute(this.elementRef, 'placeholder');
    if (this.updateDataprovider() && this.onActionMethodID) {
      this.onActionMethodID(event);
    }
  }

  updateDataprovider() {
    if (this.valuelistID) {
      let value = null;
      for (let i = 0; i < this.valuelistID.length; i++) {
        if (this.selectedValues.indexOf(this.valuelistID[i].displayValue) != -1) {
          if (this.multiselect) {
            if (value == null) value = [];
            value.push(this.valuelistID[i].realValue);
          } else {
            value = this.valuelistID[i].realValue;
          }
        }
      }
      if (this.multiselect && value) {
        value = value.join('\n');
      }
      if ((this.dataProviderID + '') != (value + '')) {
        this.update(value);
        return true;
      }
    }
    return false;
  }
}
 