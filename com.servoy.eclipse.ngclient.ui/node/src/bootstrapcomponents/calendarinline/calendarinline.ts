import { Component, Renderer2, ChangeDetectorRef } from '@angular/core';
import { Moment } from 'moment';
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

    constructor(renderer: Renderer2, protected cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

    public dateChanged(event: Moment) {
        if (event) {
            this.dataProviderID = event.toDate();
        } else this.dataProviderID = null;
        super.pushUpdate();
    }

    public disableDays(dateArray: number[]) {
        this.filter = (d: Moment): boolean => dateArray.includes(d.day());
    }

    public disableDates(dateArray: Date[]) {
        this.filter = (d: Moment): boolean => {
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
