import { DOCUMENT } from '@angular/common';
import { Component, Renderer2, ChangeDetectorRef, ChangeDetectionStrategy, Inject } from '@angular/core';
import { DateTimeAdapter } from '@danielmoncada/angular-datetime-picker';
import { Moment } from 'moment';
import { LocaleService } from '../../ngclient/servoy_public';
import { ServoyBootstrapBaseCalendar } from '../calendar/basecalendar';

@Component({
    selector: 'bootstrapcomponents-calendarinline',
    templateUrl: './calendarinline.html',
    styleUrls: ['./calendarinline.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyBootstrapCalendarinline extends ServoyBootstrapBaseCalendar {

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef,
        localeService: LocaleService, dateTimeAdapter: DateTimeAdapter<any>, @Inject(DOCUMENT) doc: Document) {
        super(renderer, cdRef, localeService, dateTimeAdapter, doc);
    }

    public dateChanged(event: Moment) {
        if (event) {
            this.dataProviderID = event.toDate();
        } else this.dataProviderID = null;
        super.pushUpdate();
    }
}
