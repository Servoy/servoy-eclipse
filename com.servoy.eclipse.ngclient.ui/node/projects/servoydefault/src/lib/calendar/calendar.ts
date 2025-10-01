import { Component, SimpleChanges, Renderer2, ElementRef, ViewChild, ChangeDetectorRef, ChangeDetectionStrategy, Inject, DOCUMENT } from '@angular/core';

import { FormattingService, ServoyPublicService, getFirstDayOfWeek } from '@servoy/public';

import { ServoyDefaultBaseField } from '../basefield';

import { DateTime as LuxonDateTime } from 'luxon';


import { FormatDirective, LoggerFactory, LoggerService } from '@servoy/public';
import { TempusDominus, DateTime, Namespace, Options} from '@eonasdan/tempus-dominus';

@Component({
    selector: 'servoydefault-calendar',
    templateUrl: './calendar.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ServoyDefaultCalendar extends ServoyDefaultBaseField<HTMLDivElement> {

    @ViewChild('inputElement') inputElementRef: ElementRef;
    
    @ViewChild(FormatDirective) svyFormat: FormatDirective;

    private log: LoggerService;
    private picker: TempusDominus;

    private hasFocus = false;
    private isBlur = false;

    private readonly config: Options = {
        allowInputToggle: false,
        useCurrent: false,
        display: {
            keyboardNavigation: true,
            components: {
				calendar: true,
                decades: true,
                year: true,
                month: true,
                date: true,
                clock: true,
                hours: true,
                minutes: true,
                seconds: true
            },
            calendarWeeks: true,
            buttons: {
                today: true,
                close: true,
                clear: true,
            },
            theme: 'light'
        },
        restrictions: {
        },
        localization: {
            startOfTheWeek: 1,
            locale: 'nl',
            hourCycle: 'h23'
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
        this.config.localization.startOfTheWeek = getFirstDayOfWeek(servoyService.getLocaleObject() ? servoyService.getLocaleObject().full : servoyService.getLocale());
        const lts = LuxonDateTime.now().setLocale(servoyService.getLocale()).toLocaleString(LuxonDateTime.DATETIME_FULL).toUpperCase();
        if (lts.indexOf('AM') >= 0 || lts.indexOf('PM') >= 0) {
            this.config.localization.hourCycle = 'h12';
        }
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
            const value = (this.dataProviderID instanceof Date) ? DateTime.convert(this.dataProviderID, null, this.config.localization) : null;
            this.picker.dates.setValue(value);
        }
        if (this.dataProviderID && !this.findmode) {
            const value = (this.dataProviderID instanceof Date) ? DateTime.convert(this.dataProviderID, null, this.config.localization) : null;
            this.config.viewDate = value;
       }
        if (changes.format)
            if (changes.format.currentValue) {
                if (changes.format.currentValue.type === 'DATETIME' && changes.format.currentValue.display) {
                    const format = changes.format.currentValue.display;
                    const showYear = format.indexOf('y') >= 0;
                    const showMonth = format.indexOf('M') >= 0;
                    const showDate = format.indexOf('d') >= 0;
                    const showHour = format.indexOf('h') >= 0 || format.indexOf('H') >= 0;
                    const showMinute = format.indexOf('m') >= 0;
                    const showSecond = format.indexOf('s') >= 0;
                    this.config.display.components.calendar = showYear || showMonth || showDate;
                    this.config.display.components.decades = showYear;
                    this.config.display.components.year = showYear;
                    this.config.display.components.month = showMonth;
                    this.config.display.components.date = showDate;
                    this.config.display.components.clock = showHour || showMinute || showSecond;
                    this.config.display.components.hours = showHour;
                    this.config.display.components.minutes = showMinute;
                    this.config.display.components.seconds = showSecond;
                    if (format.indexOf('a') >= 0 || format.indexOf('A') >= 0 || format.indexOf('am') >= 0 || format.indexOf('AM') >= 0) {
						this.config.localization.hourCycle = 'h12';
					} else if (format.indexOf('H') >= 0) {
						this.config.localization.hourCycle = 'h23';
					} else if (format.indexOf('h') >= 0) {
						this.config.localization.hourCycle = 'h12';
					}
                    if (this.picker !== null) this.picker.updateOptions(this.config);
                } else {
                    this.log.warn('wrong format or type given into the calendar field ' + JSON.stringify(changes.format.currentValue));
                }
            }
        if (changes.size)
            this.renderer.setStyle(this.inputElementRef.nativeElement, 'height', changes.size.currentValue['height'] + 'px');
    }

    public dateChanged(event: any) {
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
        else  if (event === undefined ||
            event.toString() === 'Invalid Date') {
                // revert to old value
                this.svyFormat.writeValue(this.dataProviderID);
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
            const currentValue = (this.inputElementRef.nativeElement as HTMLInputElement).value;
            (this.inputElementRef.nativeElement as HTMLInputElement).value='';
            this.picker = new TempusDominus(this.getNativeElement(), this.config);
            (this.inputElementRef.nativeElement as HTMLInputElement).value = currentValue;
            this.picker.dates.formatInput =  (date: DateTime) => date?this.formattingService.format(date, this.format, false):'';
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
        let language = locale.toLowerCase();
        if (index > 0 && language !== 'ar-sa' && language !== 'sr-latn') {
            language = locale.substring(0, index);
        }
        language = language.toLowerCase();
        
        const moduleLoader =  (module: { default: { localization: { [key: string]: string | number} }}) => {
            const copy = Object.assign({}, module.default.localization);
            copy.startOfTheWeek =   this.config.localization.startOfTheWeek;
            copy.hourCycle = this.config.localization.hourCycle;
            this.config.localization = copy;
            if (this.picker) this.picker.updateOptions(this.config);
        }
        const errorHandler = () => {
                this.log.info('Locale ' + locale + ' for calendar not found, default to english');
        }
        switch(language) {
            case 'ar-sa': import('@eonasdan/tempus-dominus/dist/locales/ar-SA.js').then(moduleLoader,errorHandler); break;
            case 'ar': import('@eonasdan/tempus-dominus/dist/locales/ar.js').then(moduleLoader,errorHandler); break;
            case 'ca': import('@eonasdan/tempus-dominus/dist/locales/ca.js').then(moduleLoader,errorHandler); break;
            case 'cs': import('@eonasdan/tempus-dominus/dist/locales/cs.js').then(moduleLoader,errorHandler); break;
            case 'de': import('@eonasdan/tempus-dominus/dist/locales/de.js').then(moduleLoader,errorHandler); break;
            case 'es': import('@eonasdan/tempus-dominus/dist/locales/es.js').then(moduleLoader,errorHandler); break;
            case 'fi': import('@eonasdan/tempus-dominus/dist/locales/fi.js').then(moduleLoader,errorHandler); break;
            case 'fr': import('@eonasdan/tempus-dominus/dist/locales/fr.js').then(moduleLoader,errorHandler); break;
            case 'hr': import('@eonasdan/tempus-dominus/dist/locales/hr.js').then(moduleLoader,errorHandler); break;
            case 'hy': import('@eonasdan/tempus-dominus/dist/locales/hy.js').then(moduleLoader,errorHandler); break;
            case 'it': import('@eonasdan/tempus-dominus/dist/locales/it.js').then(moduleLoader,errorHandler); break;
            case 'nl': import('@eonasdan/tempus-dominus/dist/locales/nl.js').then(moduleLoader,errorHandler); break;
            case 'pl': import('@eonasdan/tempus-dominus/dist/locales/pl.js').then(moduleLoader,errorHandler); break;
            case 'ro': import('@eonasdan/tempus-dominus/dist/locales/ro.js').then(moduleLoader,errorHandler); break;
            case 'ru': import('@eonasdan/tempus-dominus/dist/locales/ru.js').then(moduleLoader,errorHandler); break;
            case 'sl': import('@eonasdan/tempus-dominus/dist/locales/sl.js').then(moduleLoader,errorHandler); break;
            case 'sr': import('@eonasdan/tempus-dominus/dist/locales/sr.js').then(moduleLoader,errorHandler); break;
            case 'sr-latn': import('@eonasdan/tempus-dominus/dist/locales/sr-Latn.js').then(moduleLoader,errorHandler); break;
            case 'tr': import('@eonasdan/tempus-dominus/dist/locales/tr.js').then(moduleLoader,errorHandler); break;
        }
    }
}
