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

    globalDayArray: Number[];
    globalDateArray: Date[];

    constructor(renderer: Renderer2, protected cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

    public dateChanged(event: Moment) {
        if (event) {
            this.dataProviderID = event.toDate();
        } else this.dataProviderID = null;
        super.pushUpdate();
    }

    public disableDays(dateArray: Number[]) {
        this.globalDayArray = dateArray;
        this.filter = ( d: moment.Moment ): boolean => {
            if ( this.globalDateArray ) {
                let result = true;
                this.globalDateArray.forEach( el => {
                    let year = d.toDate().getUTCFullYear().toString();
                    let month = d.toDate().getUTCMonth().toString();
                    let day = d.toDate().getUTCDate() + 1;
                    if ( el.getUTCFullYear().toString() === year &&
                        el.getUTCMonth().toString() === month &&
                        el.getUTCDate() === day ) {
                        result = false;
                    }
                } );

                return result && !this.globalDayArray.includes( d.day() );
            }
            return !dateArray.includes( d.day() );
        };
    }

    public disableDates( dateArray: Date[] ) {
        this.globalDateArray = dateArray;
        this.filter = ( d: moment.Moment ): boolean => {
            let result = true;
            dateArray.forEach( el => {
                let year = d.toDate().getUTCFullYear().toString();
                let month = d.toDate().getUTCMonth().toString();
                let day = d.toDate().getUTCDate() + 1;
                if ( el.getUTCFullYear().toString() === year &&
                    el.getUTCMonth().toString() === month &&
                    el.getUTCDate() === day ) {
                    result = false;
                }
            } );
            if ( this.globalDayArray ) {
                return result && !this.globalDayArray.includes( d.day() );
            }
            return result;
        };
    }

    public setMinMaxDate(minDate: Date, maxDate: Date) {
        if (minDate) this.min = minDate;
        if (maxDate) this.max = maxDate;
    }

}
