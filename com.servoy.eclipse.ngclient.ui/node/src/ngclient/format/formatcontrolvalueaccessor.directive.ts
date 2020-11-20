
import {Directive, Renderer2, ElementRef, Input, HostListener, forwardRef, AfterViewInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MaskFormat } from './maskformat';
import numbro from 'numbro';
import * as moment from 'moment';
import { Format, FormattingService } from './formatting.service';


@Directive({

    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: '[svyFormat]',
    providers: [{
        provide: NG_VALUE_ACCESSOR,
        useExisting: forwardRef(() => FormatDirective),
        multi: true
    }]
})
export class FormatDirective implements ControlValueAccessor, AfterViewInit {
    @Input('svyFormat') format: Format;
    @Input() inputType: string;
    @Input() findMode: boolean;

    onChangeCallback = (_: any) => {};
    onTouchedCallback = () => {};

    private hasFocus = false;
	private realValue = null;

	private isKeyPressEventFired = false;
	private oldInputValue = null;


    constructor(private _renderer: Renderer2, private _elementRef: ElementRef, private formatService: FormattingService) {}

    ngAfterViewInit(): void {
		if (this.format) {
			if (this.format.uppercase || this.format.lowercase) {
				this._renderer.listen(this._elementRef.nativeElement,'input',(event) => this.upperOrLowerCase(event));
			}
			if (this.format.isNumberValidator || this.format.type == 'NUMBER' || this.format.type == 'INTEGER') {
				this._renderer.listen(this._elementRef.nativeElement,'keypress',(event) => {
					this.isKeyPressEventFired=true;
					return this.testForNumbersOnly(event, null,true,false);
				});
				this._renderer.listen(this._elementRef.nativeElement,'input',(event) => this.inputFiredForNumbersCheck(event));
			}
			if (this.format.maxLength) {
				this._renderer.setAttribute(this._elementRef.nativeElement, 'maxlength', this.format.maxLength + '');
			}
			if (this.format.isMask) {
				new MaskFormat(this.format, this._renderer, this._elementRef.nativeElement, this.formatService);
			}
		}
	}

	registerOnChange(fn: any) {
        this.onChangeCallback = fn;
    }

    registerOnTouched(fn: any) {
        this.onTouchedCallback = fn;
    }

    @HostListener('blur', []) touched() {
        this.onTouchedCallback();
        this.hasFocus = false;
		if (this.format.display && this.format.edit && this.format.edit !== this.format.display) {
			this.writeValue(this.realValue);
		}
    }

    @HostListener('focus', []) focussed() {
		this.hasFocus = true;
		if (this.format.display && this.format.edit && this.format.edit !== this.format.display) {
			this.writeValue(this.realValue);
		}
    }

    @HostListener('change', ['$event.target.value']) input(value: any) {
        let data = value;
        if (!this.findMode && this.format) {
            const type = this.format.type;
            let format = this.format.display ? this.format.display : this.format.edit;
            if (this.hasFocus&& this.format.edit && !this.format.isMask) format = this.format.edit;
            try {
                data = this.unformat(data, format, type, this.realValue);
            } catch (e) {
                console.log(e);
                    //TODO set error state
            }
            if (this.format.type == 'TEXT' && this.format.isRaw && this.format.isMask) {
                if (data && format && data.length === format.length){
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

        this.onChangeCallback(data);
	}

    writeValue(value: any): void {
        this.realValue = value;
         if (value && this.format) {
             let data = value;
             if (!this.findMode) {
                data = this.inputType === 'number' && data.toString().length >= this.format.maxLength ? data.toString().substring(0, this.format.maxLength):data;
                 let format = this.format.display ? this.format.display : this.format.edit;
                 if (this.format.edit && !this.format.isMask && this.hasFocus) format = this.format.edit;
                 try {
                     data = this.formatService.format(data, format, this.format.type);
                 } catch (e) {
                     console.log(e);
                 }
                 if (data && this.format.type == 'TEXT') {
                     if (this.format.uppercase) data = data.toUpperCase();
                     else if (this.format.lowercase) data = data.toLowerCase();
                 }
             }
             this._renderer.setProperty(this._elementRef.nativeElement, 'value', data);
         } else {
             this._renderer.setProperty(this._elementRef.nativeElement, 'value', value?value:'');
         }
	 }

	 // lower and upper case handling

	 private upperOrLowerCase(event: Event) {
        const element = this._elementRef.nativeElement;
        const caretPos = element.selectionStart;
        element.value = this.format.uppercase?element.value.toUpperCase():element.value.toLowerCase();
        element.setSelectionRange(caretPos, caretPos);
	}

	// unformatting stuff

    private unformat(data: any, servoyFormat: string, type: string, currentValue: any) {
        if ((!servoyFormat) || (!type) || (!data && data !== 0)) return data;
        if ((type == 'NUMBER') || (type == 'INTEGER')) {
            return this.unformatNumbers(data, servoyFormat);
        } else if (type == 'TEXT') {
            return data;
        } else if (type == 'DATETIME') {
            if ('' === data ) return null;
            // some compatibility issues, see http://momentjs.com/docs/ and http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
            servoyFormat = servoyFormat.replace(new RegExp('d', 'g'), 'D');
            servoyFormat = servoyFormat.replace(new RegExp('y', 'g'), 'Y');
            // use moment.js from calendar component
            const d = moment(data, servoyFormat,true).toDate();
            // if format has not year/month/day use the one from the current model value
            // because moment will just use current date
            if(currentValue && !isNaN(currentValue.getTime())) {
                if(servoyFormat.indexOf('Y') == -1) {
                    d.setFullYear(currentValue.getFullYear());
                }
                if(servoyFormat.indexOf('M') == -1) {
                    d.setMonth(currentValue.getMonth());
                }
                if(servoyFormat.indexOf('D') == -1) {
                    d.setDate(currentValue.getDate());
                }
            }
            return d;
        }
        return data;
    }

    private unformatNumbers(data: any, format: string) { // todo throw error when not coresponding to format (reimplement with state machine)
		if (data === '') return data;
		//treat scientiffic numbers
		if (data.toString().toLowerCase().indexOf('e') > -1 && !isNaN(data)) {
			return new Number(data).valueOf();
		}

		let multFactor = 1;
		const MILLSIGN = '\u2030';
		if (format.indexOf(MILLSIGN) > -1 && format.indexOf('\''+MILLSIGN+'\'') == -1) {
			multFactor *= 0.001;
		}
		if (format.indexOf('\'') > -1) {
			// replace the literals
			const parts = format.split('\'');
			for (let i=0;i<parts.length;i++) {
				if (i % 2 == 1) {
					data = data.replace(new RegExp(parts[i], 'g'), '');
				}
			}
		}
		let ret = numbro(data).value();
		ret *= multFactor;
		return ret;
	}


	// test numbers only

	private testForNumbersOnly(e, keyChar, vCheckNumbers, skipMaxLength) {
		if (!this.findMode && vCheckNumbers) {
			if (this.formatService.testKeyPressed(e, 13) && e.target.tagName.toUpperCase() == 'INPUT') {
				//do not looses focus, just apply the format and push value
				this._elementRef.nativeElement.dispatchEvent(new CustomEvent('change', { bubbles: true, detail: { text: () => this._elementRef.nativeElement.value } }));
			} else if (this.format.type == 'INTEGER') {
				var currentLanguageNumeralSymbols = numbro.languageData();

				if(keyChar == undefined) {
					return this.numbersonly(e, false, currentLanguageNumeralSymbols.delimiters.decimal, currentLanguageNumeralSymbols.delimiters.thousands, currentLanguageNumeralSymbols.currency
							.symbol,
							this.format.percent, skipMaxLength === true ? 0 : this.format.maxLength,);
				} else {
					return this.numbersonlyForChar(keyChar, false, currentLanguageNumeralSymbols.delimiters.decimal, currentLanguageNumeralSymbols.delimiters.thousands, currentLanguageNumeralSymbols.currency
							.symbol,
							this.format.percent, skipMaxLength === true ? 0 : this.format.maxLength);
				}
			} else if (this.format.type == 'NUMBER' || ((this.format.type == 'TEXT') && this.format.isNumberValidator)) {
				var currentLanguageNumeralSymbols = numbro.languageData();

				if(keyChar == undefined) {
					return this.numbersonly(e, true, currentLanguageNumeralSymbols.delimiters.decimal, currentLanguageNumeralSymbols.delimiters.thousands, currentLanguageNumeralSymbols.currency.symbol,
						this.format.percent, skipMaxLength === true ? 0 : this.format.maxLength);
				} else {
					return this.numbersonlyForChar(keyChar, true, currentLanguageNumeralSymbols.delimiters.decimal, currentLanguageNumeralSymbols.delimiters.thousands, currentLanguageNumeralSymbols.currency.symbol,
						this.format.percent, skipMaxLength === true ? 0 : this.format.maxLength);
				}
			}
		}
		return true;
	}

	private numbersonlyForChar(keychar, decimal, decimalChar, groupingChar, currencyChar, percentChar, mlength) {
		const value = this._elementRef.nativeElement.value;
		if (mlength > 0 && value) {
			let counter = 0;
			if (('0123456789').indexOf(keychar) != -1) counter++;
			const stringLength = value.length;
			for (var i = 0; i < stringLength; i++) {
				if (('0123456789').indexOf(value.charAt(i)) != -1) counter++;
			}
			const selectedTxt = this.getSelectedText(this._elementRef.nativeElement);
			if (selectedTxt) {
				// selection will get deleted/replaced by typed key
				for (var i = 0; i < selectedTxt.length; i++) {
					if (('0123456789').indexOf(selectedTxt.charAt(i)) != -1) counter--;
				}
			}
			if (counter > mlength) return false;
		}

		if ((('-0123456789').indexOf(keychar) > -1)) {
			return true;
		} else if (decimal && (keychar == decimalChar)) {
			return true;
		} else if (keychar == groupingChar) {
			return true;
		} else if (keychar == currencyChar) {
			return true;
		} else if (keychar == percentChar) {
			return true;
		}
		return false;
	}

	private numbersonly(e, decimal, decimalChar, groupingChar, currencyChar, percentChar, mlength) {
		let key;
		let keychar;

		if (window.event) {
			key = window.event['keyCode'];
		} else if (e) {
			key = e.which;
		} else {
			return true;
		}

		if ((key == null) || (key == 0) || (key == 8) || (key == 9) || (key == 13) || (key == 27) || (e.ctrlKey && key == 97) || (e.ctrlKey && key == 99) || (e.ctrlKey && key ==
				118) || (e.ctrlKey && key == 120)) { //added CTRL-A, X, C and V
			return true;
		}

		keychar = String.fromCharCode(key);
		return this.numbersonlyForChar(keychar, decimal, decimalChar, groupingChar, currencyChar, percentChar, mlength);

	}

	private getSelectedText(textarea) {
		let sel = null;
		if(textarea) {
			// code for IE
			if (document['selection']) {
				textarea.focus();
				sel = document['selection'].createRange().text;
			} else {
				// code for Mozilla
				const start = textarea['selectionStart'];
				const end = textarea['selectionEnd'];
				sel = textarea['value'].substring(start, end);
			}
		}
		return sel;
	}

	private inputFiredForNumbersCheck(event) {
		let currentValue = this._elementRef.nativeElement.value;

		if(!this.isKeyPressEventFired && event.target.tagName.toUpperCase() == 'INPUT') {
			// get inserted chars
			const inserted = this.findDelta(currentValue, this.oldInputValue);
			// get removed chars
			const removed = this.findDelta(this.oldInputValue, currentValue);
			// determine if user pasted content
			const pasted = inserted.length > 1 || (!inserted && !removed);

			if(!pasted && !removed) {
				if(!this.testForNumbersOnly(event, inserted, true, true)) {
					currentValue = this.oldInputValue;
				}
			}

			//If number validator, check all chars in string and extract only the valid chars.
			if(event.target.type.toUpperCase() == 'NUMBER' || this.format.type =='NUMBER' || this.format.type == 'INTEGER' || this.format.isNumberValidator){
				currentValue = this.getNumbersFromString(event,currentValue, this.oldInputValue);
			}

			if (currentValue != this._elementRef.nativeElement.value) {
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

		if(this.isKeyPressEventFired){
			this.oldInputValue = currentValue;
			this.isKeyPressEventFired = false;
		}
	}

	private findDelta(value: string, prevValue: string): string {
		let delta = '';
		if(typeof value === 'string' && typeof prevValue === 'string' && value.length >= prevValue.length) {
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

	private getNumbersFromString(e, currentValue, oldInputValue){
		if(oldInputValue === currentValue){
			return currentValue;
		}
		let stripped = '';
		for (let i = 0; i < currentValue.length; i++) {
			if(this.testForNumbersOnly(e, currentValue.charAt(i), true, true)){
				stripped = stripped + currentValue.charAt(i);
				if(stripped.length === this.format.maxLength) break;
			}
		}
		return stripped;
	}
}
