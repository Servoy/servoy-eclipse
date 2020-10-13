import { Component, Renderer2, ElementRef, ViewChild, Input, ChangeDetectorRef} from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';
import { OwlDateTimeIntl } from '@danielmoncada/angular-datetime-picker';
import * as moment from 'moment';

@Component({
  selector: 'servoybootstrap-calendar',
  templateUrl: './calendar.html',
  styleUrls: ['./calendar.scss'],
  providers: [OwlDateTimeIntl]
})
export class ServoyBootstrapCalendar extends ServoyBootstrapBasefield {

    @ViewChild( 'inputElement') inputElementRef: ElementRef;
    @Input() format;

    public filter: any;
    min: Date;
    max: Date;
    
    constructor(renderer: Renderer2, protected cdRef: ChangeDetectorRef) { 
        super(renderer, cdRef);
    }

    getFocusElement(): any {
        return this.inputElementRef.nativeElement;
    }

    getStyleClassElement(): any {
        return this.inputElementRef.nativeElement;
    }

    public disableDays(dateArray: Number[]) {
        this.filter = (d: moment.Moment): boolean => {
            return !dateArray.includes(d.day());
        };
    }

    public disableDates(dateArray: Date[]) {
        this.filter = (d: moment.Moment): boolean => {
          let result = true;
          dateArray.forEach(el => {
            if (el.toString() === d.toDate().toString()) {
                result = false;
            }
          });
          return result;
        }
    }

    public setMinMaxDate(minDate: Date, maxDate: Date) {
        if (minDate) this.min = minDate;
        if (maxDate) this.max = maxDate;
    }
}
