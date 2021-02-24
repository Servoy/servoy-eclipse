import { Renderer2, ChangeDetectorRef, Inject } from '@angular/core';
import { Moment } from 'moment';
import { ServoyBootstrapBasefield } from '../bts_basefield';
import * as moment from 'moment';
import { LocaleService } from '../../ngclient/servoy_public';
import { DateTimeAdapter } from '@danielmoncada/angular-datetime-picker';
import { DOCUMENT } from '@angular/common';

export class ServoyBootstrapBaseCalendar extends ServoyBootstrapBasefield<HTMLDivElement> {

    public filter: any;
    public min: Date;
    public max: Date;

    public globalDayArray: number[];
    public globalDateArray: Date[];

    public firstDayOfWeek = 1;
    public hour12Timer = false;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef,
            localeService: LocaleService,
            dateTimeAdapter: DateTimeAdapter<any>, @Inject(DOCUMENT) doc: Document) {
        super(renderer, cdRef, doc);
        dateTimeAdapter.setLocale(localeService.getLocale());
        const ld = moment.localeData();
        this.firstDayOfWeek = ld.firstDayOfWeek();
        const lts = ld.longDateFormat('LTS');
        this.hour12Timer = lts.indexOf('a') >= 0 || lts.indexOf('A') >= 0;

    }

    public disableDays(dateArray: number[]) {
        this.globalDayArray = dateArray;
        this.filter = this.globalDateArray || this.globalDayArray ? this.filterImpl : null;
        this.cdRef.detectChanges();
    }

    public disableDates(dateArray: Date[]) {
        this.globalDateArray = dateArray;
        this.filter = this.globalDateArray || this.globalDayArray ? this.filterImpl : null;
        this.cdRef.detectChanges();
    }

    public setMinMaxDate(minDate: Date, maxDate: Date) {
        if (minDate) this.min = minDate;
        if (maxDate) this.max = maxDate;
    }

    private filterImpl = (d: Moment): boolean => {
        let result = true;
        if (this.globalDateArray) {
            this.globalDateArray.forEach(el => {
                const year = d.toDate().getUTCFullYear().toString();
                const month = d.toDate().getUTCMonth().toString();
                const day = d.toDate().getUTCDate() + 1;
                if (el.getUTCFullYear().toString() === year &&
                    el.getUTCMonth().toString() === month &&
                    el.getUTCDate() === day) {
                    result = false;
                }
            });
        }
        if (result && this.globalDayArray) {
            result = !this.globalDayArray.includes(d.day());
        }
        return result;
    }
}
