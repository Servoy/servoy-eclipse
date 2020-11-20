import { Component, Renderer2, ElementRef, ViewChild, Input, ChangeDetectorRef, SimpleChanges } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';
import { DateTimeAdapter, OwlDateTimeComponent, OwlDateTimeIntl } from '@danielmoncada/angular-datetime-picker';
import * as moment from 'moment';
import { FormattingService, I18NProvider, LocaleService } from '../../ngclient/servoy_public';

@Component({
    selector: 'bootstrapcomponents-calendar',
    templateUrl: './calendar.html',
    styleUrls: ['./calendar.scss'],
    providers: [OwlDateTimeIntl]
})
export class ServoyBootstrapCalendar extends ServoyBootstrapBasefield {

    @ViewChild('inputElement') inputElementRef: ElementRef;
    @ViewChild(OwlDateTimeComponent) datetime: OwlDateTimeComponent<any>;
    @Input() format;

    public filter: any;
    min: Date;
    max: Date;

    public firstDayOfWeek = 1;
    public hour12Timer = false;
    public pickerType = 'both';
    public showSecondsTimer = false;


    constructor(renderer: Renderer2,
        cdRef: ChangeDetectorRef,
        i18nProvider: I18NProvider,
        localeService: LocaleService,
        dateTimeAdapter: DateTimeAdapter<any>,
        owlDateTimeIntl: OwlDateTimeIntl) {
        super(renderer, cdRef);
        dateTimeAdapter.setLocale(localeService.getLocale());
        i18nProvider.getI18NMessages('servoy.button.ok', 'servoy.button.cancel').then((val) => {
            if (val['servoy.button.ok']) owlDateTimeIntl.setBtnLabel = val['servoy.button.ok'];
            if (val['servoy.button.cancel']) owlDateTimeIntl.cancelBtnLabel = val['servoy.button.cancel'];
        });

        const ld = moment.localeData();
        this.firstDayOfWeek = ld.firstDayOfWeek();
        const lts = ld.longDateFormat('LTS');
        this.hour12Timer = lts.indexOf('a') >= 0 || lts.indexOf('A') >= 0;
    }


    attachFocusListeners(nativeElement: any) {
        super.attachFocusListeners(nativeElement);
        if (this.onFocusGainedMethodID) {
            this.datetime.afterPickerOpen.subscribe(() => {
                this.onFocusGainedMethodID(new CustomEvent('focus'));
            });
        }

        if (this.onFocusLostMethodID) {
            this.datetime.afterPickerClosed.subscribe(() => {
                this.onFocusLostMethodID(new CustomEvent('blur'));
            });
        }
    }

    svyOnChanges(changes: SimpleChanges) {
        for (const property of Object.keys(changes)) {
            const change = changes[property];
            switch (property) {
                case 'format':
                    const format = change.currentValue.display;
                    const showCalendar = format.indexOf('y') >= 0 || format.indexOf('M') >= 0;
                    const showTime = format.indexOf('h') >= 0 || format.indexOf('H') >= 0 || format.indexOf('m') >= 0;
                    if (showCalendar) {
                        if (showTime) this.pickerType = 'both';
                        else this.pickerType = 'calendar';
                    } else this.pickerType = 'timer';
                    this.showSecondsTimer = format.indexOf('s') >= 0;
                    this.hour12Timer = format.indexOf('h') >= 0 || format.indexOf('a') >= 0 || format.indexOf('A') >= 0;
                    break;
            }
        }
        super.svyOnChanges(changes);
    }

    public dateChanged(event) {
        if (event && event.value) {
            this.dataProviderID = event.value.toDate();
        } else this.dataProviderID = null;
        super.pushUpdate();
    }

    getFocusElement(): any {
        return this.inputElementRef.nativeElement;
    }

    getStyleClassElement(): any {
        return this.inputElementRef.nativeElement;
    }

    public disableDays(dateArray: Number[]) {
        this.filter = (d: moment.Moment): boolean => !dateArray.includes(d.day());
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
