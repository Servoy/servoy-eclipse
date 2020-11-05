import { Component } from '@angular/core';
import { DateTimeAdapter, OwlDateTimeIntl } from '@danielmoncada/angular-datetime-picker';
import { LocaleService } from '../../../ngclient/servoy_public';
import { DatagridEditor } from './datagrideditor';
import * as moment from 'moment';
import { ICellEditorParams } from 'ag-grid-community';

@Component({
  selector: 'datagrid-datepicker',
  template: `
      <div class="input-group ag-cell-edit-input">
        <input class="form-control" [owlDateTime]="datetime" (dateTimeChange)="dateChanged($event)" [value]="initialValue" #element>
        <span class="input-group-text input-group-append" [owlDateTimeTrigger]="datetime"><span class="far fa-calendar-alt"></span></span>
        <owl-date-time #datetime [firstDayOfWeek]="firstDayOfWeek" [hour12Timer]="hour12Timer" [pickerType]="pickerType" [showSecondsTimer]="showSecondsTimer"></owl-date-time>
      </div>
    `,
  providers: [OwlDateTimeIntl]
})
export class DatePicker extends DatagridEditor {

  firstDayOfWeek = 1;
  hour12Timer = false;
  pickerType = 'both';
  showSecondsTimer = false;

  selectedValue;

  constructor(localeService: LocaleService, dateTimeAdapter: DateTimeAdapter<any>) {
    super();
    dateTimeAdapter.setLocale(localeService.getLocale());

    const ld = moment.localeData();
    this.firstDayOfWeek = ld.firstDayOfWeek();
    const lts = ld.longDateFormat('LTS');
    this.hour12Timer = lts.indexOf('a') >= 0 || lts.indexOf('A') >= 0;
  }

  agInit(params: ICellEditorParams): void {
    super.agInit(params);
    this.selectedValue = this.initialValue;

    let format = 'MM/dd/yyyy hh:mm a';
    const column = this.dataGrid.getColumn(params.column.getColId());
    if (column && column.format) {
      format = column.format.edit ? column.format.edit : column.format.display;
    }

    const showCalendar = format.indexOf('y') >= 0 || format.indexOf('M') >= 0;
    const showTime = format.indexOf('h') >= 0 || format.indexOf('H') >= 0 || format.indexOf('m') >= 0;
    if (showCalendar) {
      if (showTime) this.pickerType = 'both';
      else this.pickerType = 'calendar'
    } else this.pickerType = 'timer'
    this.showSecondsTimer = format.indexOf('s') >= 0;
    this.hour12Timer = format.indexOf('h') >= 0 || format.indexOf('a') >= 0 || format.indexOf('A') >= 0;
  }

  ngAfterViewInit(): void {
    this.elementRef.nativeElement.focus();
    this.elementRef.nativeElement.select();
  }

  // returns the new value after editing
  getValue(): any {
    this.selectedValue;
  }

  dateChanged(e): any {
    if (e && e.value) {
      this.selectedValue = e.value.toDate();
    } else this.selectedValue = null;
  }
}