import { ViewChild } from '@angular/core';
import { HostListener } from '@angular/core';
import { Input } from '@angular/core';
import { Component } from '@angular/core';
import { NgbTypeahead, NgbTypeaheadConfig } from '@ng-bootstrap/ng-bootstrap';
import { merge, Observable, of, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, switchMap } from 'rxjs/operators';
import { FormattingService } from '../../../ngclient/servoy_public';
import { DatagridEditor } from './datagrideditor';

@Component({
    selector: 'datagrid-typeahededitor',
    template: `
      <input class="ag-table-typeahed-editor-input"
        [value]="initialDisplayValue"
        [maxLength]="maxLength"
        [style.width.px]="width"
        [ngbTypeahead]="filterValues"
        [resultFormatter]="resultFormatter"
		    [inputFormatter]="inputFormatter"
        #instance="ngbTypeahead" #element>
    `
})
export class TypeaheadEditor extends DatagridEditor {

  @ViewChild('instance') instance: NgbTypeahead;
  focus$ = new Subject<string>();
  click$ = new Subject<string>();

  @Input() initialDisplayValue;
  @Input() maxLength = 524288;

  width: number;
  valuelist;
  hasRealValues: boolean;
  format;

  constructor(private formatService: FormattingService, config: NgbTypeaheadConfig) {
    super();
    config.container = "body";
  }

  agInit(params: any): void {
    super.agInit(params);

    if(params.column.actualWidth) {
      this.width = params.column.actualWidth;
    }

    this.valuelist = this.dataGrid.getValuelist(params);
    if (this.valuelist) {
      const _this = this;
      this.valuelist.filterList('').subscribe(valuelistValues => {
        let hasRealValues = false;
        for (let i = 0; i < valuelistValues.length; i++) {
          const item = valuelistValues[i];
          if (item.realValue != item.displayValue) {
            hasRealValues = true;
            break;
          }
        }
        _this.hasRealValues = hasRealValues;
        // make sure initial value has the "realValue" set, so when oncolumndatachange is called
        // the previous value has the "realValue"
        if(hasRealValues && params.value && (params.value['realValue'] == undefined)) {
          params.node['data'][params.column.colDef['field']] = {realValue: params.value, displayValue: params.value};
        }
      });
    }

    if(this.initialValue && this.initialValue.displayValue != undefined) {
      this.initialValue = this.initialValue.displayValue;
    }
    let v = this.initialValue;
    const column = this.dataGrid.getColumn(params.column.getColId());
    if(column && column.format) {
        this.format = column.format;
        if (this.format.maxLength) {
            this.maxLength = this.format.maxLength;
        }
        if(this.format.edit) {
            v = this.dataGrid.formattingService.format(v, this.format, true);
        }
        else if(this.format.display) {
            v = this.dataGrid.formattingService.format(v, this.format, false);
        }
    }
    this.initialDisplayValue = v;
  }

  @HostListener('keydown',['$event']) onKeyDown(e: KeyboardEvent) {
    if(this.dataGrid.arrowsUpDownMoveWhenEditing && this.dataGrid.arrowsUpDownMoveWhenEditing != 'NONE') {
        const isNavigationLeftRightKey = e.keyCode === 37 || e.keyCode === 39;
        const isNavigationUpDownEntertKey = e.keyCode === 38 || e.keyCode === 40 || e.keyCode === 13;

        if (isNavigationLeftRightKey || isNavigationUpDownEntertKey) {
            e.stopPropagation();
        }
    }
  }

  @HostListener('keypress',['$event']) onKeyPress(e: KeyboardEvent) {
      const isNavigationLeftRightKey = e.keyCode === 37 || e.keyCode === 39;
      const isNavigationUpDownEntertKey = e.keyCode === 38 || e.keyCode === 40 || e.keyCode === 13;

      if(!(isNavigationLeftRightKey || isNavigationUpDownEntertKey) && this.format) {
        return this.dataGrid.formattingService.testForNumbersOnly(e, null, this.elementRef.nativeElement, false, true, this.format, false);
      }
      else return true;
  }

  filterValues = (text$: Observable<string>) => {
    const debouncedText$ = text$.pipe(debounceTime(200), distinctUntilChanged());
    const clicksWithClosedPopup$ = this.click$.pipe(filter(() => !this.instance.isPopupOpen()));
    const inputFocus$ = this.focus$;

    return merge(debouncedText$, inputFocus$, clicksWithClosedPopup$).pipe( switchMap(term => (term === '' ? of(this.valuelist)
    : this.valuelist.filterList(term))));
  };

  // focus and select can be done after the gui is attached
  ngAfterViewInit(): void {
    const editFormat = this.format.edit ? this.format.edit : this.format.display;
    if(this.format && editFormat && this.format.isMask) {
        const settings = {};
        settings['placeholder'] = this.format.placeHolder ? this.format.placeHolder : ' ';
        if (this.format.allowedCharacters)
            settings['allowedCharacters'] = this.format.allowedCharacters;

        //TODO: jquery mask
        //$(this.eInput).mask(editFormat, settings);
    }
  }

  // returns the new value after editing
  getValue(): any {
    let displayValue = this.elementRef.nativeElement.value;

    if(this.format) {
        const editFormat = this.format.edit ? this.format.edit : this.format.display;
        if(editFormat) {
            displayValue = this.dataGrid.formattingService.unformat(displayValue, editFormat, this.format.type, this.initialValue);
        }
        if (this.format.type == 'TEXT' && (this.format.uppercase || this.format.lowercase)) {
            if (this.format.uppercase) displayValue = displayValue.toUpperCase();
            else if (this.format.lowercase) displayValue = displayValue.toLowerCase();
        }
    }
    let realValue = displayValue;

    if (this.valuelist) {
      let hasMatchingDisplayValue = false;
      const fDisplayValue = this.findDisplayValue(this.valuelist, displayValue);
      if(fDisplayValue != null) {
        hasMatchingDisplayValue = fDisplayValue['hasMatchingDisplayValue'];
        realValue = fDisplayValue['realValue'];
      }

      if (!hasMatchingDisplayValue) {
        if (this.hasRealValues) {
          // if we still have old value do not set it to null or try to  get it from the list.
          if (this.initialValue != null && this.initialValue !== displayValue) {
            // so invalid thing is typed in the list and we are in real/display values, try to search the real value again to set the display value back.
            for (let i = 0; i < this.valuelist.length; i++) {
              //TODO: compare trimmed values, typeahead will trim the selected value
              if (this.initialValue === this.valuelist[i].displayValue) {
                realValue = this.valuelist[i].realValue;
                break;
              }
            }
          }
          // if the dataproviderid was null and we are in real|display then reset the value to ""
          else if(this.initialValue == null) {
            displayValue = realValue = '';
          }
        }
      }
    }

    return {displayValue, realValue};
  }

  resultFormatter = (result: {displayValue: string; realValue: object}) => {
    if (result.displayValue === null) return '';
    return this.formatService.format(result.displayValue, this.format, false);
  }

  inputFormatter = (result: any) => {
    if (result === null) return '';
    if (result.displayValue !== undefined) result = result.displayValue;
    else if (this.valuelist.hasRealValues()) {
      // on purpose test with == so that "2" equals to 2
      // eslint-disable-next-line eqeqeq
      const value = this.valuelist.find((item) => item.realValue == result);
      if (value) {
        result = value.displayValue;
      }
    }
    return this.formatService.format(result, this.format, false);
  }

  private findDisplayValue(vl, displayValue) {
    if(vl) {
      for (let i = 0; i < vl.length; i++) {
        //TODO: compare trimmed values, typeahead will trim the selected value
        if (displayValue === vl[i].displayValue) {
          return { hasMatchingDisplayValue: true, realValue: vl[i].realValue };
        }
      }
    }
    return null;
  }
}
