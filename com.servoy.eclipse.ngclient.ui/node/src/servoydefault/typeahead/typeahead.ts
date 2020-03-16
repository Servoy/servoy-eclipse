import { Component, OnInit, Renderer2, ViewChild } from '@angular/core';
import { Observable, merge, Subject, of } from 'rxjs';
import { ServoyDefaultBaseField } from '../basefield';
import {NgbTypeahead} from '@ng-bootstrap/ng-bootstrap';
import { FormattingService } from '../../ngclient/servoy_public';
import { map, debounceTime, distinctUntilChanged, filter} from 'rxjs/operators';

@Component({
  selector: 'servoydefault-typeahead',
  templateUrl: './typeahead.html'
})
export class ServoyDefaultTypeahead extends ServoyDefaultBaseField {
  // this is a hack so that this can be none static access because this references in this component to a conditional template
  @ViewChild('instance', {static: true}) instance: NgbTypeahead;
  focus$ = new Subject<string>();
  click$ = new Subject<string>();

  constructor(renderer: Renderer2,
              formattingService: FormattingService) {
    super(renderer, formattingService);
  }

  isEditable() {
    return this.valuelistID && !this.valuelistID.hasRealValues();
  }

  formatter = (result: {displayValue: string, realValue: object}) => {
    console.log(result);
    if (result.displayValue === null) return '';
    if (result.displayValue !== undefined) return result.displayValue;
    return result;
  }

  value = () => {
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

    return merge(debouncedText$, inputFocus$, clicksWithClosedPopup$).pipe(
      map(term => (term === '' ? this.valuelistID
        : this.valuelistID.filter(v => v.displayValue.toLowerCase().indexOf(term.toLowerCase()) > -1)))
    );
  }

  valueChanged(value: {displayValue: string, realValue: object}) {
    console.log(value);
    if (value && value.realValue) this.dataProviderID = value.realValue;
    else if (value) this.dataProviderID = value;
    else this.dataProviderID = null;
    this.dataProviderIDChange.emit( this.dataProviderID );
  }
}
