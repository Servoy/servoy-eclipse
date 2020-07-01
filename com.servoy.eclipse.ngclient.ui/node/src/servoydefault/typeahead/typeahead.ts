import { Component, OnInit, Renderer2, ViewChild, SimpleChanges } from '@angular/core';
import { Observable, merge, Subject, of, Subscriber } from 'rxjs';
import { ServoyDefaultBaseField } from '../basefield';
import {NgbTypeahead} from '@ng-bootstrap/ng-bootstrap';
import { FormattingService } from '../../ngclient/servoy_public';
import { map, debounceTime, distinctUntilChanged, filter, take, switchMap} from 'rxjs/operators';

@Component({
  selector: 'servoydefault-typeahead',
  templateUrl: './typeahead.html'
})
export class ServoyDefaultTypeahead extends ServoyDefaultBaseField {
  // this is a hack so that this can be none static access because this references in this component to a conditional template
  @ViewChild('instance', {static: true}) instance: NgbTypeahead;
  focus$ = new Subject<string>();
  click$ = new Subject<string>();
  observableValue: Observable<object>;

  private observer: Subscriber<object>;

  constructor(renderer: Renderer2,
              formattingService: FormattingService) {
    super(renderer, formattingService);
    this.observableValue = new Observable(observer => {
      this.observer = observer;
      this.getObservableDataprovider().pipe(take(1)).subscribe(
        displayValue => this.observer.next(displayValue));
    });
  }

  scroll($event: any) {
    if (!this.instance.isPopupOpen()) {
      return;
    }

    setTimeout(() => {
      const popup = document.getElementById(this.instance.popupId),
        activeElements = popup.getElementsByClassName('active');
      if (activeElements.length === 1) {
        const elem = <HTMLElement>activeElements[0];
        elem.scrollIntoView({
          behavior: 'smooth',
          block: 'center'
        });
      }
    });
  }

  isEditable() {
    return this.valuelistID && !this.valuelistID.hasRealValues();
  }

  formatter = (result: {displayValue: string, realValue: object}) => {
    if (result.displayValue === null) return '';
    if (result.displayValue !== undefined) return result.displayValue;
    return result;
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

  values = (text$: Observable<string>) => {
    const debouncedText$ = text$.pipe(debounceTime(200), distinctUntilChanged());
    const clicksWithClosedPopup$ = this.click$.pipe(filter(() => !this.instance.isPopupOpen()));
    const inputFocus$ = this.focus$;

    return merge(debouncedText$, inputFocus$, clicksWithClosedPopup$).pipe( switchMap(term => (term === '' ? of(this.valuelistID)
    : this.valuelistID.filterList(term))));
  }

  valueChanged(value: {displayValue: string, realValue: object}) {
    if (value && value.realValue) this.dataProviderID = value.realValue;
    else if (value) this.dataProviderID = value;
    else this.dataProviderID = null;
    this.dataProviderIDChange.emit( this.dataProviderID );
  }
}
