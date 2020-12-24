import { Component, ChangeDetectorRef, Renderer2, ViewChild, SimpleChanges } from '@angular/core';
import { Observable, merge, Subject, of, Subscriber } from 'rxjs';
import { ServoyDefaultBaseField } from '../basefield';
import { NgbTypeahead } from '@ng-bootstrap/ng-bootstrap';
import { FormattingService } from '../../ngclient/servoy_public';
import { map, debounceTime, distinctUntilChanged, filter, take, switchMap } from 'rxjs/operators';

@Component({
  selector: 'servoydefault-typeahead',
  templateUrl: './typeahead.html'
})
export class ServoyDefaultTypeahead extends ServoyDefaultBaseField<HTMLInputElement> {
  // this is a hack so that this can be none static access because this references in this component to a conditional template
  @ViewChild('instance', { static: true }) instance: NgbTypeahead;
  focus$ = new Subject<string>();
  click$ = new Subject<string>();

  constructor(renderer: Renderer2, cdRef: ChangeDetectorRef,
    formattingService: FormattingService) {
    super(renderer, cdRef, formattingService);
  }

  scroll($event: any) {
    if (!this.instance.isPopupOpen()) {
      return;
    }

    setTimeout(() => {
      const popup = document.getElementById(this.instance.popupId);
        const activeElements = popup.getElementsByClassName('active');
      if (activeElements.length === 1) {
        const elem = <HTMLElement>activeElements[0];
        elem.scrollIntoView({
          behavior: 'smooth',
          block: 'center'
        });
      }
    });
  }

  svyOnChanges(changes: SimpleChanges) {
    super.svyOnChanges(changes);
    if (changes.readOnly || changes.enabled) {
        this.instance.setDisabledState(this.readOnly || !this.enabled);
    }
  }

  values = (text$: Observable<string>) => {
    const debouncedText$ = text$.pipe(debounceTime(200), distinctUntilChanged());
    const clicksWithClosedPopup$ = this.click$.pipe(filter(() => !this.instance.isPopupOpen()));
    const inputFocus$ = this.focus$;

    return merge(debouncedText$, inputFocus$, clicksWithClosedPopup$).pipe(switchMap(term => (term === '' ? of(this.valuelistID)
      : this.valuelistID.filterList(term))));
  };

  isEditable() {
    return this.valuelistID && !this.valuelistID.hasRealValues();
  }

  resultFormatter = (result: { displayValue: string; realValue: object }) => {
    if (result.displayValue === null) return '';
    return this.formattingService.format(result.displayValue, this.format, false);
  };

  inputFormatter = (result: any) => {
    if (result === null) return '';
    if (result.displayValue !== undefined) result = result.displayValue;
    else if (this.valuelistID.hasRealValues()) {
      // on purpose test with == so that "2" equals to 2
      // eslint-disable-next-line eqeqeq
      const value = this.valuelistID.find((item) => item.realValue == result);
      if (value) {
        result = value.displayValue;
      }
    }
    return this.formattingService.format(result, this.format, false);
  };

  valueChanged(value: { displayValue: string; realValue: object }) {
    if (value && value.realValue) this.dataProviderID = value.realValue;
    else if (value) this.dataProviderID = value;
    else this.dataProviderID = null;
    this.dataProviderIDChange.emit(this.dataProviderID);
  }
}
