import { Component, Renderer2, ChangeDetectorRef} from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';

@Component({
  selector: 'bootstrapcomponents-calendarinline',
  templateUrl: './calendarinline.html',
  styleUrls: ['./calendarinline.scss']
})
export class ServoyBootstrapCalendarinline extends ServoyBootstrapBasefield {

    public filter: any;
    min: Date;
    max: Date;

    constructor(renderer: Renderer2,protected cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

    public disableDays(dateArray: Number[]) {
      this.filter = (d: moment.Moment): boolean => dateArray.includes(d.day());
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
      };
    }

    public setMinMaxDate(minDate: Date, maxDate: Date) {
        if (minDate) this.min = minDate;
        if (maxDate) this.max = maxDate;
    }

}
