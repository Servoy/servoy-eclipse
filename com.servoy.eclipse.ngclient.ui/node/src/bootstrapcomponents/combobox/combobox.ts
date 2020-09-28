import { Component, Renderer2, Input, SimpleChanges } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';
import { IValuelist } from '../../sablo/spectypes.service';
import { FormattingService } from '../../ngclient/servoy_public';
import { Select2Data, Select2UpdateEvent, Select2Value } from 'ng-select2-component';
import { Observable, Subscriber, of } from 'rxjs';
import { take } from 'rxjs/operators';

@Component({
  selector: 'servoybootstrap-combobox',
  templateUrl: './combobox.html',
  styleUrls: ['./combobox.scss']
})
export class ServoyBootstrapCombobox extends ServoyBootstrapBasefield {

  @Input() format;
  @Input() showAs;
  @Input() valuelistID: IValuelist;

  data: Select2Data;
  observableValue: Observable<any>; 
  private observer: Subscriber<any>;
  
  constructor(renderer: Renderer2) {
    super(renderer);
    this.observableValue = new Observable(observer => {
        this.observer = observer;
        this.getObservableDataprovider().pipe(take(1)).subscribe(
          displayValue => this.observer.next(displayValue));
      });
}

  ngOnInit(): void {
      super.ngOnInit();
      this.data = [];
      this.setData();
  }
  
  setData() {
      if (this.valuelistID) {
          let options: Select2Data = [];
          for(let i = 0; i < this.valuelistID.length; i++) {
              options.push({ 
                value: this.valuelistID.hasRealValues() ? this.valuelistID[i].realValue : this.valuelistID[i].displayValue, 
                label: this.valuelistID[i].displayValue });
          }
          this.data = options;
      }
    }
  
  updateValue(event: Select2UpdateEvent<any>) {
      this.dataProviderID = event.value;
      this.dataProviderIDChange.emit( this.dataProviderID );
    }

    ngOnChanges( changes: SimpleChanges ) {
      if (changes['dataProviderID']) {
        if (this.observer) {
          this.getObservableDataprovider().pipe(take(1)).subscribe(
            displayValue => this.observer.next(displayValue));
        }
      }
      super.ngOnChanges(changes);
    }

    getObservableDataprovider(): Observable<any> {
      if (this.valuelistID && this.valuelistID.hasRealValues()) {
        const found = this.valuelistID.find(entry => entry.realValue === this.dataProviderID);
        if (found) return of(found.displayValue);
        return this.valuelistID.getDisplayValue(this.dataProviderID);
      }
      return of(this.dataProviderID);
    }
}
