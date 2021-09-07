import { Component, SimpleChanges, Renderer2, ElementRef, ViewChild, ChangeDetectorRef, ChangeDetectionStrategy, Inject } from '@angular/core';

import { FormattingService, ServoyPublicService, getFirstDayOfWeek } from '@servoy/public';

import { DateTimeAdapter, OwlDateTimeIntl, OwlDateTimeComponent } from '@danielmoncada/angular-datetime-picker';

import { ServoyDefaultBaseField } from '../basefield';

import { DateTime } from 'luxon';

import { DOCUMENT } from '@angular/common';
import { LoggerFactory, LoggerService } from '@servoy/public';
import { PickerType } from '@danielmoncada/angular-datetime-picker/lib/date-time/date-time.class';

@Component({
    selector: 'servoydefault-calendar',
    templateUrl: './calendar.html',
    providers: [OwlDateTimeIntl],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyDefaultCalendar extends ServoyDefaultBaseField<HTMLDivElement> {

    @ViewChild('inputElement') inputElementRef: ElementRef;
    @ViewChild(OwlDateTimeComponent) datetime: OwlDateTimeComponent<any>;

    public firstDayOfWeek = 1;
    public hour12Timer = false;
    public pickerType: PickerType = 'both';
    public showSecondsTimer = false;

    private log: LoggerService;

    constructor(renderer: Renderer2,
        cdRef: ChangeDetectorRef,
        formattingService: FormattingService,
        servoyService: ServoyPublicService,
        dateTimeAdapter: DateTimeAdapter<any>,
        owlDateTimeIntl: OwlDateTimeIntl,
        @Inject(DOCUMENT) doc: Document,
        logFactory: LoggerFactory) {
        super(renderer, cdRef, formattingService, doc);
        dateTimeAdapter.setLocale(servoyService.getLocale());
        servoyService.getI18NMessages('servoy.button.ok', 'servoy.button.cancel').then((val) => {
            if (val['servoy.button.ok']) owlDateTimeIntl.setBtnLabel = val['servoy.button.ok'];
            if (val['servoy.button.cancel']) owlDateTimeIntl.cancelBtnLabel = val['servoy.button.cancel'];
        });
        this.log = logFactory.getLogger('default-calendar');
        this.firstDayOfWeek = getFirstDayOfWeek(servoyService.getLocale());
        const lts = DateTime.now().setLocale(servoyService.getLocale()).toLocaleString(DateTime.DATETIME_FULL).toUpperCase();
        this.hour12Timer = lts.indexOf('AM') >= 0 || lts.indexOf('PM') >= 0;
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
                    if (change.currentValue) {
                        if (change.currentValue.type === 'DATETIME' && change.currentValue.display) {
                            const format = change.currentValue.display;
                            const showCalendar = format.indexOf('y') >= 0 || format.indexOf('M') >= 0;
                            const showTime = format.indexOf('h') >= 0 || format.indexOf('H') >= 0 || format.indexOf('m') >= 0;
                            if (showCalendar) {
                                if (showTime) this.pickerType = 'both';
                                else this.pickerType = 'calendar';
                            } else this.pickerType = 'timer';
                            this.showSecondsTimer = format.indexOf('s') >= 0;
                            this.hour12Timer = format.indexOf('h') >= 0 || format.indexOf('a') >= 0 || format.indexOf('A') >= 0;
                        } else {
                            this.log.warn('wrong format or type given into the calendar field ' + JSON.stringify(change.currentValue));
                        }
                    }
                    break;
                case 'size':
                    this.renderer.setStyle(this.inputElementRef.nativeElement, 'height', change.currentValue['height'] + 'px');
                    break;
            }
        }
        super.svyOnChanges(changes);
    }

    onModelChange(newValue) {
        const previousValue = this.dataProviderID;
        this.dataProviderID = newValue;
        if(this.dataProviderID && isNaN(this.dataProviderID.getTime())) {
            // invalid date, restore previous value
            this.cdRef.detectChanges();
            this.dataProviderID = previousValue;
            this.cdRef.detectChanges();
        }
    }

    public dateChanged(event) {
        if (event && event.value) {
            this.dataProviderID = event.value;
        } else this.dataProviderID = null;
        super.pushUpdate();
    }

    getFocusElement(): any {
        return this.inputElementRef.nativeElement;
    }
}
