import { Component, Renderer2, SimpleChanges} from '@angular/core';
import { FormattingService } from '../../ngclient/servoy_public';
import { Select2Data, Select2UpdateEvent, Select2Value } from 'ng-select2-component';
import { ServoyDefaultBaseField } from '../basefield';
import { Observable, Subscriber, of } from 'rxjs';
import { take } from 'rxjs/operators';

@Component({
  selector: 'servoydefault-combo',
  templateUrl: './combobox.html'
})
export class ServoyDefaultCombobox extends ServoyDefaultBaseField {

  data: Select2Data;
  observableValue: Observable<any>; 
  private observer: Subscriber<any>;

  constructor(renderer: Renderer2,
              formattingService: FormattingService) {
    super(renderer, formattingService);
    this.observableValue = new Observable(observer => {
      this.observer = observer;
      this.getObservableDataprovider().pipe(take(1)).subscribe(
        displayValue => this.observer.next(displayValue));
    });
  } 

  ngOnInit() {
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
    // this change should be ignored for the combobox.
    delete changes['editable'];
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

