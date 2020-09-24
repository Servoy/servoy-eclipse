import { Component, Renderer2,Input,ViewChild,SimpleChanges } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';
import {IValuelist} from '../../sablo/spectypes.service';
import {NgbTypeahead} from '@ng-bootstrap/ng-bootstrap';
import { Observable, merge, Subject, of, Subscriber } from 'rxjs';
import { map, debounceTime, distinctUntilChanged, filter, take, switchMap} from 'rxjs/operators';

@Component({
  selector: 'servoybootstrap-typeahead',
  templateUrl: './typeahead.html',
  styleUrls: ['./typeahead.scss']
})
export class ServoyBootstrapTypeahead extends ServoyBootstrapBasefield  {

  @Input() format;
  @Input() valuelistID: IValuelist;
  
  @ViewChild('instance', {static: true}) instance: NgbTypeahead;
  focus$ = new Subject<string>();
  click$ = new Subject<string>();
  
  observableValue: Observable<object>;
  private observer: Subscriber<object>;
  
  constructor(renderer: Renderer2) { 
      super(renderer);
      this.observableValue = new Observable(observer => {
          this.observer = observer;
          this.getObservableDataprovider().pipe(take(1)).subscribe(
            displayValue => this.observer.next(displayValue));
        });
  }

  ngOnChanges( changes: SimpleChanges ) {
      super.ngOnChanges(changes);
      if (changes['dataProviderID']) {
        if (this.observer) {
          this.getObservableDataprovider().pipe(take(1)).subscribe(
            displayValue => this.observer.next(displayValue));
        }
      }
    }
  
  getObservableDataprovider(): Observable<any> {

      if (this.valuelistID && this.valuelistID.hasRealValues()) {
        const found = this.valuelistID.find(entry => entry.realValue === this.dataProviderID);
        if (found) return of(found.displayValue);
        return this.valuelistID.getDisplayValue(this.dataProviderID);
      }
      return of(this.dataProviderID);
    }
  
  filterValues = (text$: Observable<string>) => {
      const debouncedText$ = text$.pipe(debounceTime(200), distinctUntilChanged());
      const clicksWithClosedPopup$ = this.click$.pipe(filter(() => !this.instance.isPopupOpen()));
      const inputFocus$ = this.focus$;

      return merge(debouncedText$, inputFocus$, clicksWithClosedPopup$).pipe( switchMap(term => (term === '' ? of(this.valuelistID)
      : this.valuelistID.filterList(term))));
    }
  
  formatter = (result: {displayValue: string, realValue: object}) => {
      if (result.displayValue === null) return '';
      if (result.displayValue !== undefined) return result.displayValue;
      return result;
    }
  
  valueChanged(value: {displayValue: string, realValue: object}) {
      if (value && value.realValue) this.dataProviderID = value.realValue;
      else if (value) this.dataProviderID = value;
      else this.dataProviderID = null;
      this.dataProviderIDChange.emit( this.dataProviderID );
    }
}
