import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';
import * as moment from 'moment';
import numbro from 'numbro';

const MILLSIGN = '\u2030';

export class Format {
    display: string = null;
    uppercase = false;
    lowercase = false;
    type: string = null;
    isMask = false;
    isRaw = false;
    isNumberValidator = false;
    edit: string = null;
    placeHolder = '';
    percent = '';
    allowedCharacters = '';
    maxLength = 0;
}


@Injectable()
export class FormattingService {
    private doc: Document
    constructor(@Inject(DOCUMENT) _doc: any) {
        this.doc = _doc;
    }
    // formatting stufff
    public format(data: any, format: Format, useEditFormat: boolean): string {
        const formatString = useEditFormat ? format.edit : format.display;

        if ((!format) || (!format.type) || ((typeof data === 'number') && isNaN(data))) {
            if (!format && ((format.type === 'NUMBER') || (format.type === 'INTEGER')) && (typeof data === 'number') && !isNaN(data)) {
                // make sure is always returned with correct type, otherwise compare will not work well
                return data.toString();
            }
            return data;
        }
        if (data === undefined || data === null) return '';
        if ((format.type === 'NUMBER') || (format.type === 'INTEGER')) {
            return this.formatNumbers(data, formatString);
        } else if (format.type === 'TEXT') {
            let formattedValue = this.formatText(data, formatString);
            if (format.uppercase) formattedValue = formattedValue.toUpperCase();
            else if (format.lowercase) formattedValue = formattedValue.toLowerCase();
            return formattedValue;
        } else if (format.type === 'DATETIME') {
            return this.formatDate(data, formatString);
        }
        return data;
    }

    public testKeyPressed(event: KeyboardEvent, keyCode: number) {
        let code: number;

        if (!event) event = window.event as KeyboardEvent;
        if (!event) return false;
        if (event.keyCode) code = event.keyCode;
        else if (event.which) code = event.which;
        return code === keyCode;
    }

	// test numbers only
	public testForNumbersOnly(e, keyChar, vElement, vFindMode, vCheckNumbers, vSvyFormat, skipMaxLength) {
		if (!vFindMode && vCheckNumbers) {
			if (this.testKeyPressed(e, 13) && e.target.tagName.toUpperCase() === 'INPUT') {
				//do not looses focus, just apply the format and push value
				vElement.dispatchEvent(new CustomEvent('change', { bubbles: true, detail: { text: () => vElement.value } }));
			} else if (vSvyFormat.type === 'INTEGER') {
				const currentLanguageNumeralSymbols = numbro.languageData();

				if(keyChar === undefined) {
					return this.numbersonly(e, false, currentLanguageNumeralSymbols.delimiters.decimal, currentLanguageNumeralSymbols.delimiters.thousands, currentLanguageNumeralSymbols.currency
							.symbol,
							vSvyFormat.percent, vElement, skipMaxLength === true ? 0 : vSvyFormat.maxLength);
				} else {
					return this.numbersonlyForChar(keyChar, false, currentLanguageNumeralSymbols.delimiters.decimal, currentLanguageNumeralSymbols.delimiters.thousands,
                    currentLanguageNumeralSymbols.currency.symbol, vSvyFormat.percent, vElement, skipMaxLength === true ? 0 : vSvyFormat.maxLength);
				}
			} else if (vSvyFormat.type === 'NUMBER' || ((vSvyFormat.type === 'TEXT') && vSvyFormat.isNumberValidator)) {
				const currentLanguageNumeralSymbols = numbro.languageData();

				if(keyChar === undefined) {
					return this.numbersonly(e, true, currentLanguageNumeralSymbols.delimiters.decimal, currentLanguageNumeralSymbols.delimiters.thousands,
                        currentLanguageNumeralSymbols.currency.symbol, vSvyFormat.percent, vElement, skipMaxLength === true ? 0 : vSvyFormat.maxLength);
				} else {
					return this.numbersonlyForChar(keyChar, true, currentLanguageNumeralSymbols.delimiters.decimal, currentLanguageNumeralSymbols.delimiters.thousands,
                        currentLanguageNumeralSymbols.currency.symbol, vSvyFormat.percent, vElement, skipMaxLength === true ? 0 : vSvyFormat.maxLength);
				}
			}
		}
		return true;
	}

	// unformatting stuff

    public unformat(data: any, servoyFormat: string, type: string, currentValue: any) {
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
				if (i % 2 === 1) {
					data = data.replace(new RegExp(parts[i], 'g'), '');
				}
			}
		}
		let ret = numbro(data).value();
		ret *= multFactor;
		return ret;
	}

	private numbersonly(e, decimal, decimalChar, groupingChar, currencyChar, percentChar, vElement, mlength) {
		let key;

		if (window.event) {
			key = window.event['keyCode'];
		} else if (e) {
			key = e.which;
		} else {
			return true;
		}

		if ((key == null) || (key === 0) || (key === 8) || (key === 9) || (key === 13) || (key === 27) || (e.ctrlKey && key === 97) || (e.ctrlKey && key === 99) ||
            (e.ctrlKey && key === 118) || (e.ctrlKey && key === 120)) { //added CTRL-A, X, C and V
			return true;
		}

		const keychar = String.fromCharCode(key);
		return this.numbersonlyForChar(keychar, decimal, decimalChar, groupingChar, currencyChar, percentChar, vElement, mlength);

	}

	private numbersonlyForChar(keychar, decimal, decimalChar, groupingChar, currencyChar, percentChar, vElement, mlength) {
		const value = vElement.value;
		if (mlength > 0 && value) {
			let counter = 0;
			if (('0123456789').indexOf(keychar) !== -1) counter++;
			const stringLength = value.length;
			for (let i = 0; i < stringLength; i++) {
				if (('0123456789').indexOf(value.charAt(i)) !== -1) counter++;
			}
			const selectedTxt = this.getSelectedText(vElement);
			if (selectedTxt) {
				// selection will get deleted/replaced by typed key
				for (let i = 0; i < selectedTxt.length; i++) {
					if (('0123456789').indexOf(selectedTxt.charAt(i)) !== -1) counter--;
				}
			}
			if (counter > mlength) return false;
		}

		if ((('-0123456789').indexOf(keychar) > -1)) {
			return true;
		} else if (decimal && (keychar === decimalChar)) {
			return true;
		} else if (keychar === groupingChar) {
			return true;
		} else if (keychar === currencyChar) {
			return true;
		} else if (keychar === percentChar) {
			return true;
		}
		return false;
	}

    private getSelectedText(textarea) {
		let sel = null;
		if(textarea) {
			// code for IE
			if (this.doc['selection']) {
				textarea.focus();
				sel = this.doc['selection'].createRange().text;
			} else {
				// code for Mozilla
				const start = textarea['selectionStart'];
				const end = textarea['selectionEnd'];
				sel = textarea['value'].substring(start, end);
			}
		}
		return sel;
	}

    private formatNumbers(data, servoyFormat: string): string {
        if (!servoyFormat)
            return data;
        if (data === '')
            return data;

        data = Number(data); // just to make sure that if it was a string representing a number we turn it into a number
        if (typeof data === 'number' && isNaN(data)) return ''; // cannot format something that is not a number

        const initialData = data;
        let patchedFormat = servoyFormat; // patched format for numeraljs format
        let i; let j;
        let prefix = '';
        let sufix = '';


        if (patchedFormat.indexOf(';') > 0) {
            if (data < 0) {
                patchedFormat = patchedFormat.split(';')[1];
            } else patchedFormat = patchedFormat.split(';')[0];
        }

        if (data < 0) data *= -1;
        if (patchedFormat.indexOf('\u00A4') >= 0) {
            patchedFormat = patchedFormat.replace(new RegExp('\u00A4', 'g'), numbro.languageData().currency.symbol);
        }
        if (servoyFormat.indexOf('-') >= 0 && initialData >= 0 && servoyFormat.indexOf(';') < 0) {
            patchedFormat = patchedFormat.replace(new RegExp('-', 'g'), '');
        }

        if (patchedFormat.indexOf('%') > -1 && patchedFormat.indexOf('\'%\'') === -1) {
            data *= 100;
        } else if (patchedFormat.indexOf(MILLSIGN) > -1 && patchedFormat.indexOf('\'' + MILLSIGN + '\'') === -1) {
            data *= 1000;
        }
        let numberStart = patchedFormat.length;
        let index = patchedFormat.indexOf('0');
        if (index >= 0) {
            numberStart = index;
        }
        index = patchedFormat.indexOf('#');
        if (index >= 0 && index < numberStart) {
            numberStart = index;
        }
        let numberEnd = 0;
        index = patchedFormat.lastIndexOf('0');
        if (index >= 0) {
            numberEnd = index;
        }
        index = patchedFormat.lastIndexOf('#');
        if (index >= 0 && index > numberEnd) {
            numberEnd = index;
        }

        if (numberStart > 0) {
            prefix = patchedFormat.substring(0, numberStart);
        }

        if (numberEnd < patchedFormat.length - 1) {
            sufix = patchedFormat.substring(numberEnd + 1);
        }

        patchedFormat = patchedFormat.substring(numberStart, numberEnd + 1);
        let ret;


        prefix = prefix.replace(new RegExp('\'', 'g'), '');
        sufix = sufix.replace(new RegExp('\'', 'g'), '');

        if (servoyFormat.indexOf('-') === -1 && initialData < 0 && servoyFormat.indexOf(';') < 0) {
            prefix = prefix + '-';
        }
        // scientific notation case
        if (servoyFormat.indexOf('E') > -1) {
            const frmt = /([0#.,]+)+E0+.*/.exec(patchedFormat)[1];
            let integerDigits = 0;
            let fractionalDigits = 0;
            let countIntegerState = true;
            for (i = 0; i < frmt.length; i++) {
                const chr = frmt[i];
                if (chr === '.') {
                    countIntegerState = false;
                    continue;
                }
                if (chr === ',') continue;
                if (countIntegerState) {
                    integerDigits++;
                } else {
                    fractionalDigits++;
                }
            }
            ret = Number(data).toExponential(integerDigits + fractionalDigits);
        } else {
            // get min digits
            let minLen = 0;
            let optionalDigits = 0;
            for (i = 0; i < patchedFormat.length; i++) {
                if (patchedFormat[i] === '0') {
                    minLen++;
                } else if (patchedFormat[i] === '#' && minLen === 0) {
                    optionalDigits++;
                } else if (patchedFormat[i] === '.') {
                    break;
                }
            }

            patchedFormat = patchedFormat.replace(new RegExp('(#+)', 'g'), '[$1]');
            patchedFormat = patchedFormat.replace(new RegExp('#', 'g'), '0');

            ret = numbro(data).format(patchedFormat);

            // set min digits
            if (minLen > 0) {
                const retSplit = ret.split(numbro.languageData().delimiters.decimal);
                for (i = 0; i < retSplit[0].length; i++) {
                    if (retSplit[0][i] < '0' || retSplit[0][i] > '9') continue;
                    for (j = i; j < retSplit[0].length; j++) {
                        if (retSplit[0][j] >= '0' && retSplit[0][j] <= '9') continue;
                        break;
                    }
                    const nrMissing0 = minLen - (j - i);
                    if (nrMissing0 > 0) {
                        ret = retSplit[0].substring(0, i);
                        for (j = 0; j < nrMissing0; j++) ret += '0';
                        ret += retSplit[0].substring(i);
                        if (retSplit.length > 1) ret += (numbro.languageData().delimiters.decimal + retSplit[1]);
                    }
                    break;
                }
            }
            // fix the optional digits
            if (patchedFormat.indexOf(',') === -1 && optionalDigits > 0) {
                let toEliminate = 0;
                for (i = 0; i < ret.length; i++) {
                    if (ret.charAt(i) === '0') {
                        toEliminate++;
                    } else {
                        break;
                    }
                }
                if (toEliminate > 0) {
                    if (toEliminate > optionalDigits) {
                        toEliminate = optionalDigits;
                    }
                    ret = ret.substring(toEliminate);
                    if (ret.indexOf(numbro.languageData().delimiters.decimal) === 0) {
                        // we eliminated too much
                        ret = '0' + ret;
                    }
                }
            }
        }
        return prefix + ret + sufix;
    }

    private formatText(data, servoyFormat: string): string {
        if (!servoyFormat) return data;
        const error = 'input string not corresponding to format : ' + data + ' , ' + servoyFormat;
        let ret = '';
        let isEscaping = false;
        let offset = 0;
        if (data && typeof data === 'number') {
            data = data.toString();
        }
        for (let i = 0; i < servoyFormat.length; i++) {
            const formatChar = servoyFormat[i];
            const dataChar = data[i - offset];
            if (dataChar === undefined) break;
            if (isEscaping && formatChar !== '\'') {
                if (dataChar !== formatChar) throw error;
                ret += dataChar;
                continue;
            }
            switch (formatChar) {
                case '\'':
                    isEscaping = !isEscaping;
                    offset++;
                    break;
                case 'U':
                    if (dataChar.match(/[a-zA-Z\u00C0-\u00ff]/) == null) throw error;
                    ret += dataChar.toUpperCase();
                    break;
                case 'L':
                    if (dataChar.match(/[a-zA-Z\u00C0-\u00ff]/) == null) throw error;
                    ret += dataChar.toLowerCase();
                    break;
                case 'A':
                    if (dataChar.match(/[0-9a-zA-Z\u00C0-\u00ff]/) == null) throw error;
                    ret += dataChar;
                    break;
                case '?':
                    if (dataChar.match(/[a-zA-Z\u00C0-\u00ff]/) == null) throw error;
                    ret += dataChar;
                    break;
                case '*':
                    ret += dataChar;
                    break;
                case 'H':
                    if (dataChar.match(/[0-9a-fA-F]/) == null) throw error;
                    ret += dataChar.toUpperCase();
                    break;
                case '#':
                    if (dataChar.match(/[\d]/) == null) throw error;
                    ret += dataChar;
                    break;
                default:
                    ret += formatChar;
                    if (formatChar !== dataChar) offset++;
                    break;
            }
        }
        return ret;
    }

    private formatDate(data, dateFormat: string): string {
        if ( !(data instanceof Date)) return data;
        if (!dateFormat) dateFormat = 'L'; // long date format of moment
        // adjust to moment js formatting (from java simple date format)
        dateFormat = dateFormat.replace(new RegExp('d', 'g'), 'D');
        dateFormat = dateFormat.replace(new RegExp('y', 'g'), 'Y');
        dateFormat = dateFormat.replace(new RegExp('aa', 'g'), 'a');
        dateFormat = dateFormat.replace(new RegExp('AA', 'g'), 'A');
        const formatted = moment(data).format(dateFormat);
        return formatted.trim ? formatted.trim() : formatted;
    }
}
