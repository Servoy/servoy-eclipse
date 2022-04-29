import { Component, SimpleChanges, Renderer2, ElementRef, ViewChild, ChangeDetectorRef, ChangeDetectionStrategy, Inject } from '@angular/core';

import { FormattingService, ServoyPublicService, getFirstDayOfWeek } from '@servoy/public';

import { ServoyDefaultBaseField } from '../basefield';

import { DateTime as LuxonDateTime } from 'luxon';

import { DOCUMENT } from '@angular/common';
import { LoggerFactory, LoggerService } from '@servoy/public';
import { TempusDominus, DateTime, Namespace, Options} from '@servoy/tempus-dominus';
import { ChangeEvent } from '@servoy/tempus-dominus/types/utilities/event-types';
import Dates from '@servoy/tempus-dominus/types/dates';

@Component({
    selector: 'servoydefault-calendar',
    templateUrl: './calendar.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyDefaultCalendar extends ServoyDefaultBaseField<HTMLDivElement> {

    @ViewChild('inputElement') inputElementRef: ElementRef;

    private log: LoggerService;
    private picker: TempusDominus;

    private hasFocus = false;
    private isBlur = false;

    private readonly config: Options = {
        allowInputToggle: false,
        useCurrent: false,
        display: {
            components: {
                useTwentyfourHour: true,
                decades: true,
                year: true,
                month: true,
                date: true,
                hours: true,
                minutes: true,
                seconds: true
            },
            calendarWeeks: true,
            buttons: {
                today: true,
                close: true,
                clear: true,
            }
        },
        restrictions: {
        },
        localization: {
            startOfTheWeek: 1,
            locale: 'nl'
        }
    };

    constructor(renderer: Renderer2,
        cdRef: ChangeDetectorRef,
        formattingService: FormattingService,
        servoyService: ServoyPublicService,
        @Inject(DOCUMENT) doc: Document,
        logFactory: LoggerFactory) {
        super(renderer, cdRef, formattingService, doc);
        this.config.localization.locale = servoyService.getLocale();
        this.loadCalendarLocale(this.config.localization.locale);
        this.log = logFactory.getLogger('default-calendar');
        this.config.localization.startOfTheWeek = getFirstDayOfWeek(servoyService.getLocale());
        const lts = LuxonDateTime.now().setLocale(servoyService.getLocale()).toLocaleString(LuxonDateTime.DATETIME_FULL).toUpperCase();
        this.config.display.components.useTwentyfourHour = lts.indexOf('AM') >= 0 || lts.indexOf('PM') >= 0;
    }

    svyOnInit() {
        this.initializePicker();
        super.svyOnInit();
    }

    ngOnDestroy() {
        super.ngOnDestroy();
        if (this.picker !== null) this.picker.dispose();
    }

    attachFocusListeners(nativeElement: any) {
        if (this.onFocusGainedMethodID) {
            this.renderer.listen(nativeElement, 'focus', () => this.checkOnFocus());
            this.picker.subscribe(Namespace.events.show, () => this.checkOnFocus());
        }

        if (this.onFocusLostMethodID) {
            this.renderer.listen(nativeElement, 'blur', () => this.checkOnBlur());
            this.picker.subscribe(Namespace.events.hide, () => this.checkOnBlur());
        }
    }

    svyOnChanges(changes: SimpleChanges) {
        super.svyOnChanges(changes);
        if (changes.findmode)
            if (changes.findmode.currentValue) {
                this.picker.dispose();
                this.picker = null;
            } else {
                this.initializePicker();
            }

        if (changes.dataProviderID && this.picker) {
            const value = (this.dataProviderID instanceof Date) ? DateTime.convert(this.dataProviderID) : null;
            this.picker.dates.setValue(value);
        }
        if (changes.format)
            if (changes.format.currentValue) {
                if (changes.format.currentValue.type === 'DATETIME' && changes.format.currentValue.display) {
                    const format = changes.format.currentValue.display;
                    const showCalendar = format.indexOf('y') >= 0 || format.indexOf('M') >= 0;
                    const showTime = format.indexOf('h') >= 0 || format.indexOf('H') >= 0 || format.indexOf('m') >= 0;
                    const showSecondsTimer = format.indexOf('s') >= 0;
                    this.config.display.components.useTwentyfourHour = !(format.indexOf('h') >= 0 || format.indexOf('a') >= 0 || format.indexOf('A') >= 0);
                    this.config.display.components.decades = showCalendar;
                    this.config.display.components.year = showCalendar;
                    this.config.display.components.month = showCalendar;
                    this.config.display.components.date = showCalendar;
                    this.config.display.components.hours = showTime;
                    this.config.display.components.minutes = showTime;
                    this.config.display.components.seconds = showTime;
                    this.config.display.components.seconds = showSecondsTimer;
                    if (this.picker !== null) this.picker.updateOptions(this.config);
                } else {
                    this.log.warn('wrong format or type given into the calendar field ' + JSON.stringify(changes.format.currentValue));
                }
            }
        if (changes.size)
            this.renderer.setStyle(this.inputElementRef.nativeElement, 'height', changes.size.currentValue['height'] + 'px');
    }

    public dateChanged(event: ChangeEvent) {
        if (event.type === 'change.td') {
            if ((event.date && this.dataProviderID && event.date.getTime() === this.dataProviderID.getTime()) ||
                (!event.date && !this.dataProviderID)) return;
            this.dataProviderID = !event.date?null:event.date;
        } else this.dataProviderID = null;
        super.pushUpdate();
    }

    public modelChange(event) {
        if (this.findmode) {
            this.dataProviderID = event;
            super.pushUpdate();
        }
    }

    public getNativeChild(): any {
        return this.inputElementRef.nativeElement;
    }

    getFocusElement(): any {
        return this.inputElementRef.nativeElement;
    }

    private initializePicker() {
        if (!this.picker) {
            let formatted = '';
            if (this.dataProviderID) {
                formatted = this.formattingService.format(this.dataProviderID, this.format, false);
            }
            this.renderer.setProperty(this.inputElementRef.nativeElement, 'value', formatted);
            this.picker = new TempusDominus(this.getNativeElement(), this.config);
            this.picker.dates.formatInput =  (date: DateTime) => this.formattingService.format(date, this.format, false);
            this.picker.dates.parseInput =  (value: string) => {
                const parsed = this.formattingService.parse(value?value.trim():null, this.format, true, this.dataProviderID);
                if (parsed instanceof Date && !isNaN(parsed.getTime())) return  new DateTime(parsed);
                return null;
            };
            this.picker.subscribe(Namespace.events.change, (event) => this.dateChanged(event));
            if (this.onFocusGainedMethodID) {
                this.picker.subscribe(Namespace.events.show, () => this.checkOnFocus());
            }
            if (this.onFocusLostMethodID) {
                this.picker.subscribe(Namespace.events.hide, () => this.checkOnBlur());
            }
        }
    }

    private checkOnBlur() {
        this.isBlur = true;
        setTimeout(() => {
            if (this.hasFocus && this.isBlur && (this.doc.activeElement.parentElement !== this.getNativeElement())) {
                this.hasFocus = false;
                this.isBlur = false;
                this.onFocusLostMethodID(new CustomEvent('blur'));
            }
        });
    }

    private checkOnFocus() {
        this.isBlur = false;
        if (!this.hasFocus) {
            this.hasFocus = true;
            this.onFocusGainedMethodID(new CustomEvent('focus'));
        }
    }

    private loadCalendarLocale(locale: string) {
        const index = locale.indexOf('-');
        let language = locale;
        if (index > 0) {
            language = locale.substring(0, index);
        }
        language = language.toLowerCase();
        import(`@servoy/tempus-dominus/dist/locales/${language}.js`).then(
            (module: { localization: { [key: string]: string } }) => {
                this.config.localization = module.localization;
                if (this.picker !== null) this.picker.updateOptions(this.config);
            },
            () => {
                this.log.info('Locale ' + locale + ' for calendar not found, default to english');
            });
    }

}
