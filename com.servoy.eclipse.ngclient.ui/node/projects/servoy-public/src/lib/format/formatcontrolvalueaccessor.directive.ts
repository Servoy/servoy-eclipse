
import { Directive, Renderer2, ElementRef, Input, HostListener, forwardRef, AfterViewInit, OnChanges, Inject } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MaskFormat } from './maskformat';
import { Format, FormattingService } from './formatting.service';
import { DOCUMENT } from '@angular/common';
import { LoggerFactory, LoggerService } from '../logger.service';
import { DateTime } from 'luxon';


class NumberParser {
  private _group: RegExp;
    private _decimal: RegExp;
    private _numeral: RegExp;
    private _index: (d: any) => string;
  constructor() {
    const locale = new Intl.NumberFormat().resolvedOptions().locale;
    const parts = (new Intl.NumberFormat(locale) as any).formatToParts(12345.6);
    const numerals = [...new Intl.NumberFormat(locale, {useGrouping: false}).format(9876543210)].reverse();
    const index = new Map(numerals.map((d, i) => [d, i]));
    this._group = new RegExp(`[${parts.find(d => d.type === 'group').value}]`, 'g');
    this._decimal = new RegExp(`[${parts.find(d => d.type === 'decimal').value}]`);
    this._numeral = new RegExp(`[${numerals.join('')}]`, 'g');
    this._index = d => index.get(d).toString();
  }
  parse(str: string) {
    return (str.trim().replace(this._group, '').replace(this._decimal, '.').replace(this._numeral, this._index)) ? +str : NaN;
  }
}

/**
  * This is the format directive [svyFormat]="format" that can format the dataprovider value in a input field and parse the value back when the user changes the value.
  * this is an Angular  ControlValueAccessor  that sits between the angular component ( {@link ServoyBaseComponent } and the dom element, it writes the formatted value to the dom 
  * and parses and pushes the parsed value back into the component.
  *
  * it uses the {@link FormattingService} for this. 
  */
@Directive({

    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: '[svyFormat]',
    providers: [{
        provide: NG_VALUE_ACCESSOR,
        useExisting: forwardRef(() => FormatDirective),
        multi: true
    }]
})
export class FormatDirective implements ControlValueAccessor, AfterViewInit, OnChanges {
    private static DATETIMEFORMAT: Format = {display:'yyyy-MM-dd\'T\'HH:mm:ss', type:'DATETIME'} as Format;
    private static DATEFORMAT: Format = {display:'yyyy-MM-dd', type:'DATETIME'} as Format;
    private static MONTHFORMAT: Format = {display:'yyyy-MM', type:'DATETIME'} as Format;
    private static WEEKFORMAT: Format = {display:'YYYY-[W]WW', type:'DATETIME'} as Format;
    private static TIMEFORMAT: Format = {display:'HH:mm', type:'DATETIME'} as Format;
    private static BROWSERNUMBERFORMAT = new NumberParser();

    @Input('svyFormat') format: Format;
    @Input() findmode: boolean;

    private hasFocus = false;
    private realValue = null;

    private isKeyPressEventFired = false;
    private oldInputValue = null;
    private listeners = [];
    private maskFormat : MaskFormat;
    private readonly log: LoggerService;
    private focusText: string;

    constructor(private _renderer: Renderer2, private _elementRef: ElementRef, private formatService: FormattingService,
        @Inject(DOCUMENT) private doc: Document, logFactory: LoggerFactory) {
        this.log = logFactory.getLogger('formatdirective');
    }

    @HostListener('blur', ['$event.target.value']) touched(value: any) {
        this.onTouchedCallback();
        this.hasFocus = false;
        this.dateInputChange(value);
        const inputType = this.getType();
        if (this.format.display && !this.format.isMask && !(inputType === 'datetime-local' || inputType === 'date' || inputType === 'time' || inputType === 'month' || inputType === 'week')) {
            this.writeValue(this.realValue);
        }
        if (!this.format.isMask && (this.format.uppercase || this.format.lowercase) && this.focusText != this._elementRef.nativeElement.value && inputType === 'text'){
            this._elementRef.nativeElement.dispatchEvent(new CustomEvent('change', { bubbles: true }));
        }
    }

    @HostListener('focus', []) focussed() {
        this.hasFocus = true;
        this.focusText = this._elementRef.nativeElement.value;
        if (this.format.display && this.format.edit && this.format.edit !== this.format.display) {
            this.writeValue(this.realValue);
        }
    }
    
    @HostListener('keydown', ['$event']) keyDown(event: KeyboardEvent) {
		if (event.key === 'Enter') {
			this.dateInputChange((event.target as HTMLInputElement).value);
		}
	}

    @HostListener('change', ['$event.target.value']) input(value: any) {
        let data = value;
        const inputType = this.getType();
        if (inputType === 'datetime-local' || inputType === 'date' || inputType === 'time' || inputType === 'month' || inputType === 'week') {
			return;
		}
        else if (inputType === 'number') {
             data = FormatDirective.BROWSERNUMBERFORMAT.parse(value);
        } else if (inputType === 'email') {
             data = value;
        } else if (!this.findmode && this.format) {
            const type = this.format.type;
            let format = this.format.display ? this.format.display : this.format.edit;
            if (this.hasFocus && this.format.edit && !this.format.isMask) format = this.format.edit;
            try {
                data = this.formatService.unformat(data, format, type, this.realValue, true);
            } catch (e) {
                this.log.error(e);
                //TODO set error state
            }
            if (this.format.type === 'TEXT' && this.format.isRaw && this.format.isMask) {
                if (data && format && data.length === format.length) {
                    let ret = '';
                    for (let i = 0; i < format.length; i++) {
                        switch (format[i]) {
                            case 'U':
                            case 'L':
                            case 'A':
                            case '?':
                            case '*':
                            case 'H':
                            case '#':
                                ret += data[i];
                                break;
                            default:
                                // ignore literal characters
                                break;
                        }
                    }
                    data = ret;
                }
            }
        }
        this.setRealValue(data);
    }
    
    dateInputChange(value: any) {
		let data = value;
        const inputType = this.getType();
		if (inputType === 'datetime-local' || inputType === 'date' || inputType === 'time' || inputType === 'month' || inputType === 'week') {
			const luxonDate = DateTime.fromISO(data);
			if (luxonDate.isValid)
				data = luxonDate.toJSDate();
			else data = null;
			
			this.setRealValue(data);
        }
	}
	
	setRealValue(value: any) {
		this.realValue = value;
        this.onChangeCallback(value);
	}

    onChangeCallback = (_: any) => { };
    onTouchedCallback = () => { };

    ngAfterViewInit(): void {
        this.setFormat();
    }

    ngOnChanges(): void {
        this.setFormat();
    }

    registerOnChange(fn: any) {
        this.onChangeCallback = fn;
    }

    registerOnTouched(fn: any) {
        this.onTouchedCallback = fn;
    }


    writeValue(value: any): void {
        this.realValue = value;
        const inputType = this.getType();
        if (inputType === 'datetime-local') {
             const data = this.formatService.format(value, FormatDirective.DATETIMEFORMAT, false);
             this._renderer.setProperty(this._elementRef.nativeElement, 'value', data);
        } else if (inputType === 'date') {
            const data = this.formatService.format(value, FormatDirective.DATEFORMAT, false);
            this._renderer.setProperty(this._elementRef.nativeElement, 'value', data);
        } else if (inputType === 'time') {
            const data = this.formatService.format(value, FormatDirective.TIMEFORMAT, false);
            this._renderer.setProperty(this._elementRef.nativeElement, 'value', data);
        } else if (inputType === 'month') {
            const data = this.formatService.format(value, FormatDirective.MONTHFORMAT, false);
            this._renderer.setProperty(this._elementRef.nativeElement, 'value', data);
        } else if (inputType === 'week') {
            const data = this.formatService.format(value, FormatDirective.WEEKFORMAT, false);
            this._renderer.setProperty(this._elementRef.nativeElement, 'value', data);
        } else if (inputType === 'number') {
            this._renderer.setProperty(this._elementRef.nativeElement, 'value', value);
        } else if (inputType === 'email') {
            this._renderer.setProperty(this._elementRef.nativeElement, 'value', value);
        } else if ((value !== null && value !== undefined) && this.format) {
            let data = value;
            if (!this.findmode) {
                data = inputType  === 'number' && data.toString().length >= this.format.maxLength ? data.toString().substring(0, this.format.maxLength) : data;
                let useEdit = !this.format.display;
                if (this.format.edit && !this.format.isMask && this.hasFocus) useEdit = true;
                try {
                    data = this.formatService.format(data, this.format, useEdit);
                } catch (e) {
                    this.log.error(e);
                }
                if (data && this.format.type === 'TEXT') {
                    if (this.format.uppercase) data = data.toUpperCase();
                    else if (this.format.lowercase) data = data.toLowerCase();
                }
            }
            this._renderer.setProperty(this._elementRef.nativeElement, 'value', data);
        } else {
            this._renderer.setProperty(this._elementRef.nativeElement, 'value', (value !== null && value !== undefined) ? value : '');
        }
    }

    private getType(): string {
        const type = this._elementRef.nativeElement.type as string;
        return type?type.toLocaleLowerCase():'text';
    }

    private setFormat() {
        this.listeners.forEach(lFn => lFn());
        this.listeners = [];
        if (this.maskFormat){
           this.maskFormat.destroy();
           this.maskFormat = null; 
        } 
        if (this.format) {
            if (!this.findmode && (this.format.uppercase || this.format.lowercase)) {
                this.listeners.push(this._renderer.listen(this._elementRef.nativeElement, 'input', () => this.upperOrLowerCase()));
            }
            if (this.format.isNumberValidator || this.format.type === 'NUMBER' || this.format.type === 'INTEGER') {
                this.listeners.push(this._renderer.listen(this._elementRef.nativeElement, 'keypress', (event) => {
                    this.isKeyPressEventFired = true;
                    return this.formatService.testForNumbersOnly(event, null, this._elementRef.nativeElement, this.findmode, true, this.format, false);
                }));
                this.listeners.push(this._renderer.listen(this._elementRef.nativeElement, 'input', (event) => this.inputFiredForNumbersCheck(event)));
            }
            if (this.format.maxLength) {
                if (!this.findmode) {
                    this._renderer.setAttribute(this._elementRef.nativeElement, 'maxlength', this.format.maxLength + '');
                } else {
                    this._renderer.removeAttribute(this._elementRef.nativeElement, 'maxlength');
                }
            }
            if (!this.findmode && this.format.isMask) {
                this.maskFormat = new MaskFormat(this.format, this._renderer, this._elementRef.nativeElement, this.formatService, this.doc);
            }
            this.writeValue(this.realValue);
        }
    }

    // lower and upper case handling

    private upperOrLowerCase() {
        const element = this._elementRef.nativeElement;
        const caretPos = element.selectionStart;
        element.value = this.format.uppercase ? element.value.toUpperCase() : element.value.toLowerCase();
        element.setSelectionRange(caretPos, caretPos);
    }

    private inputFiredForNumbersCheck(event: Event) {
        let currentValue = this._elementRef.nativeElement.value;

        if (!this.isKeyPressEventFired && (event.target as HTMLElement).tagName.toUpperCase() === 'INPUT') {
            // get inserted chars
            const inserted = this.findDelta(currentValue, this.oldInputValue);
            // get removed chars
            const removed = this.findDelta(this.oldInputValue, currentValue);
            // determine if user pasted content
            const pasted = inserted.length > 1 || (!inserted && !removed);

            if (!pasted && !removed) {
                if (!this.formatService.testForNumbersOnly(event, inserted, this._elementRef.nativeElement, this.findmode, true, this.format, true)) {
                    currentValue = this.oldInputValue;
                }
            }

            //If number validator, check all chars in string and extract only the valid chars.
            if ((event.target as HTMLInputElement).type.toUpperCase() === 'NUMBER' || this.format.type === 'NUMBER' || this.format.type === 'INTEGER' || this.format.isNumberValidator) {
                currentValue = this.getNumbersFromString(event, currentValue, this.oldInputValue);
            }

            if (currentValue !== this._elementRef.nativeElement.value) {
                this._elementRef.nativeElement.value = currentValue;

                // // detect IE8 and above, and Edge; call on change manually because of https://bugs.jquery.com/ticket/10818
                // if (/MSIE/.test(navigator.userAgent) || /rv:11.0/i.test(navigator.userAgent) || /Edge/.test(navigator.userAgent)) {
                // 	var changeOnBlurForIE = function() {
                // 		 element.change();
                // 		 element.off("blur", callChangeOnBlur);
                // 	 }
                // 	 element.on("blur", changeOnBlurForIE);
                // }
            }

            this.oldInputValue = currentValue;
            this.isKeyPressEventFired = false;
        }

        if (this.isKeyPressEventFired) {
            this.oldInputValue = currentValue;
            this.isKeyPressEventFired = false;
        }
    }

    private findDelta(value: string, prevValue: string): string {
        let delta = '';
        if (typeof value === 'string' && typeof prevValue === 'string' && value.length >= prevValue.length) {
            for (let i = 0; i < value.length; i++) {
                const str = value.substr(0, i) + value.substr(i + value.length - prevValue.length);
                if (str === prevValue) {
                    delta = value.substr(i, value.length - prevValue.length);
                    break;
                }
            }
        }
        return delta;
    }

    private getNumbersFromString(e: Event, currentValue: string, oldInputValue: string) {
        if (oldInputValue === currentValue) {
            return currentValue;
        }
        let stripped = '';
        for (let i = 0; i < currentValue.length; i++) {
            if (this.formatService.testForNumbersOnly(e, currentValue.charAt(i), this._elementRef.nativeElement, this.findmode, true, this.format, true)) {
                stripped = stripped + currentValue.charAt(i);
                if (stripped.length === this.format.maxLength) break;
            }
        }
        return stripped;
    }
}


