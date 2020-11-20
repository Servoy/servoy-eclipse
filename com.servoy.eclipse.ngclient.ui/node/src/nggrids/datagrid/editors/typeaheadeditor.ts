import { ViewChild } from '@angular/core';
import { HostListener } from '@angular/core';
import { Input } from '@angular/core';
import { Component } from '@angular/core';
import { NgbTypeahead } from '@ng-bootstrap/ng-bootstrap';
import { ICellEditorParams } from 'ag-grid-community';
import { merge, Observable, of, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, switchMap } from 'rxjs/operators';
import { DatagridEditor } from './datagrideditor';

@Component({
    selector: 'datagrid-typeahededitor',
    template: `
      <input class="ag-table-typeahed-editor-input" [value]="initialDisplayValue" [maxLength]="maxLength" [style.width.px]="width" [ngbTypeahead]="filterValues" #instance="ngbTypeahead" #element>
    `
})
export class TypeaheadEditor extends DatagridEditor {

  @ViewChild('instance') instance: NgbTypeahead;
  focus$ = new Subject<string>();
  click$ = new Subject<string>();

  @Input() initialDisplayValue;
  @Input() format;
  @Input() maxLength = 524288;

  width: number;
  valuelist;
  hasRealValues: boolean;

  agInit(params: any): void {
    super.agInit(params);

    if(params.column.actualWidth) {
      this.width = params.column.actualWidth;
    }

    let vl = this.dataGrid.getValuelist(params);
    if (vl) {
      const _this = this;
      vl.filterList('').subscribe(valuelistValues => {
        _this.valuelist = valuelistValues;
        let hasRealValues = false;
        for (let i = 0; i < _this.valuelist.length; i++) {
          const item = _this.valuelist[i];
          if (item.realValue != item.displayValue) {
            hasRealValues = true;
            break;
          }
        }
        _this.hasRealValues = hasRealValues;
        // make sure initial value has the "realValue" set, so when oncolumndatachange is called
        // the previous value has the "realValue"
        if(hasRealValues && params.value && (params.value['realValue'] == undefined)) {
          let rv = params.value;
          let rvFound = false;
          for (let i = 0; i < _this.valuelist.length; i++) {
            const item = _this.valuelist[i];
            if (item.displayValue == params.value) {
              rv = item.realValue;
              rvFound = true;
              break;
            }
          }
          // it could be the valuelist does not have all the entries on the client
          // try to get the entry using a filter call to the server
          if(!rvFound) {
            vl = _this.dataGrid.getValuelist(params);

            vl.filterList('').subscribe(valuelistWithInitialValue => {
              for (let i = 0; i < valuelistWithInitialValue.length; i++) {
                if (valuelistWithInitialValue[i].displayValue == params.value) {
                  rv = valuelistWithInitialValue[i].realValue;
                  break;
                }
              }
              params.node['data'][params.column.colDef['field']] = {realValue: rv, displayValue: params.value};
            });
          } else {
            params.node['data'][params.column.colDef['field']] = {realValue: rv, displayValue: params.value};
          }
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
        const editFormat = this.format.edit ? this.format.edit : this.format.display;
        if(editFormat) {
            //TODO: formatterUtils
            //v = $formatterUtils.format(v, editFormat, this.format.type);
        }

        if (v && this.format.type == 'TEXT') {
            if (this.format.uppercase) v = v.toUpperCase();
            else if (this.format.lowercase) v = v.toLowerCase();
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

      //TODO: formatterUtils
      // if(!(isNavigationLeftRightKey || isNavigationUpDownEntertKey) && $formatterUtils.testForNumbersOnly && this.format) {
      //     return $formatterUtils.testForNumbersOnly(e, null, this.elementRef.nativeElement, false, true, this.format);
      // }
      // else return true;

      return true;
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
    this.elementRef.nativeElement.focus();

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
            //TODO: formatterUtils
            //displayValue = $formatterUtils.unformat(displayValue, editFormat, this.format.type, this.initialValue);
        }
        if (this.format.type == 'TEXT' && (this.format.uppercase || this.format.lowercase)) {
            if (this.format.uppercase) displayValue = displayValue.toUpperCase();
            else if (this.format.lowercase) displayValue = displayValue.toLowerCase();
        }
    }
    let realValue = displayValue;

    let vl = this.dataGrid.getValuelist(this.params);
    if (vl) {
      let hasMatchingDisplayValue = false;
      let fDisplayValue = this.findDisplayValue(vl, displayValue);
      if(fDisplayValue == null) {
        // try to find it also on this.valuelist, that is filtered with "" to get all entries
        vl = this.valuelist;
        fDisplayValue = this.findDisplayValue(vl, displayValue);
      }
      if(fDisplayValue != null) {
        hasMatchingDisplayValue = fDisplayValue['hasMatchingDisplayValue'];
        realValue = fDisplayValue['realValue'];
      }

      if (!hasMatchingDisplayValue) {
        if (this.hasRealValues) {
          // if we still have old value do not set it to null or try to  get it from the list.
          if (this.initialValue != null && this.initialValue !== displayValue) {
            // so invalid thing is typed in the list and we are in real/display values, try to search the real value again to set the display value back.
            for (let i = 0; i < vl.length; i++) {
              //TODO: compare trimmed values, typeahead will trim the selected value
              if (this.initialValue === vl[i].displayValue) {
                realValue = vl[i].realValue;
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
