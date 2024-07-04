import { Injectable } from '@angular/core';
import numbro from 'numbro';
import { DateTime } from 'luxon';
import { ServoyPublicService } from '../services/servoy_public.service';

const MILLSIGN = '\u2030';
const SVY_FORMAT_DECIMAL_CHAR = '.';

/**
  * Class reflecting a Format object coming from the server (format spec property)
  */
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

/**
 * This service is able to format/parse/unformat data of different types (dates,numbers) according to the format string of the format spec property.
 *
 * Components can use the {@link FormatDirective} that uses this service in the component template: [svyFormat]="format" 
 */
@Injectable()
export class FormattingService {

    constructor(private servoyService: ServoyPublicService) {
    }

    /**
     * format the data give with the {@link Format} object give, optionally using the display or edit format.
     */
    public format(data: any, format: Format, useEditFormat: boolean): string {
        if ((!format) || (!format.type) || ((typeof data === 'number') && isNaN(data))) {
            if (!format && (typeof data === 'number') && !isNaN(data)) {
                // make sure is always returned with correct type, otherwise compare will not work well
                return data.toString();
            }
            return data;
        }
        const formatString = useEditFormat ? format.edit : format.display;
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

    /**
     * utility function to test if a certain key is pressed
     */
    public testKeyPressed(event: KeyboardEvent, keyCode: number) {
        let code: number;

        if (!event) event = window.event as KeyboardEvent;
        if (!event) return false;
        if (event.keyCode) code = event.keyCode;
        else if (event.which) code = event.which;
        return code === keyCode;
    }

    /**
     * utility function to test if only numbers ar pressed.
     */
    public testForNumbersOnly(e, keyChar, vElement, vFindMode, vCheckNumbers, vSvyFormat, skipMaxLength) {
        if (!vFindMode && vCheckNumbers) {
            if (this.testKeyPressed(e, 13) && e.target.tagName.toUpperCase() === 'INPUT') {
                // enter key is pressed
            } else if (vSvyFormat.type === 'INTEGER') {
                const currentLanguageNumeralSymbols = numbro.languageData();

                if (keyChar === undefined || keyChar === null) {
                    return this.numbersonly(e, false, currentLanguageNumeralSymbols.delimiters.decimal, currentLanguageNumeralSymbols.delimiters.thousands, currentLanguageNumeralSymbols.currency
                        .symbol,
                        vSvyFormat.percent, vElement, skipMaxLength === true ? 0 : vSvyFormat.maxLength, null);
                } else {
                    return this.numbersonlyForChar(keyChar, false, currentLanguageNumeralSymbols.delimiters.decimal, currentLanguageNumeralSymbols.delimiters.thousands,
                        currentLanguageNumeralSymbols.currency.symbol, vSvyFormat.percent, vElement, skipMaxLength === true ? 0 : vSvyFormat.maxLength);
                }
            } else if (vSvyFormat.type === 'NUMBER' || ((vSvyFormat.type === 'TEXT') && vSvyFormat.isNumberValidator)) {
                const currentLanguageNumeralSymbols = numbro.languageData();

                if (keyChar === undefined || keyChar === null) {
                    return this.numbersonly(e, true, currentLanguageNumeralSymbols.delimiters.decimal, currentLanguageNumeralSymbols.delimiters.thousands,
                        currentLanguageNumeralSymbols.currency.symbol, vSvyFormat.percent, vElement, skipMaxLength === true ? 0 : vSvyFormat.maxLength, vSvyFormat);
                } else {
                    return this.numbersonlyForChar(keyChar, true, currentLanguageNumeralSymbols.delimiters.decimal, currentLanguageNumeralSymbols.delimiters.thousands,
                        currentLanguageNumeralSymbols.currency.symbol, vSvyFormat.percent, vElement, skipMaxLength === true ? 0 : vSvyFormat.maxLength);
                }
            }
        }
        return true;
    }

    /**
     * calls the { @link #unformat} function for unformatting/parsing the data given
     */
    public parse(data: any, format: Format, useEditFormat: boolean, currentValue?: any, useHeuristics?: boolean): any {
        return this.unformat(data, (useEditFormat && format.edit && !format.isMask) ? format.edit : format.display, format.type, currentValue, useHeuristics);
    }

    /**
     * unformats/parse the give data according to the given format and type 
     */
    public unformat(data: any, servoyFormat: string, type: string, currentValue?: any, useHeuristics?: boolean) {
        if ((!servoyFormat) || (!type) || (!data && data !== 0)) return data;
        if ((type === 'NUMBER') || (type === 'INTEGER')) {
            return this.unformatNumbers(data, servoyFormat);
        } else if (type === 'TEXT') {
            return data;
        } else if (type === 'DATETIME') {
            if ('' === data) return null;
            servoyFormat = this.convertFormat(servoyFormat);
            let d = DateTime.fromFormat(data, servoyFormat, { locale: this.servoyService.getLocale() }).toJSDate();
            if (isNaN(d.getTime()) && useHeuristics) {
				const possibleDates: Array<Date> = [];
                for (const newFormat of this.getHeuristicFormats(servoyFormat, data)) {
                    d = DateTime.fromFormat(data, newFormat, { locale: this.servoyService.getLocale() }).toJSDate();
                    if (!isNaN(d.getTime())) {
                        possibleDates.push(d);
                    }
                }
				d = this.findClosestDate(possibleDates, new Date());
            }
            // if format has not year/month/day use the one from the current model value
            // because luxon will just use current date
            if (currentValue && !isNaN(currentValue.getTime())) {
                if (servoyFormat.indexOf('y') === -1) {
                    d.setFullYear(currentValue.getFullYear());
                }
                if (servoyFormat.indexOf('W') === -1 && servoyFormat.indexOf('o') === -1) {
                    if (servoyFormat.indexOf('M') === -1) {
                        d.setMonth(currentValue.getMonth());
                    }
                    if (servoyFormat.indexOf('d') === -1) {
                        d.setDate(currentValue.getDate());
                    }
                }
            }
            return d;
        }
        return data;
    }
	
	private findClosestDate(dateArray: Array<Date>, date: Date): Date {
		if (dateArray.length === 1) return dateArray[0];
		const currentDateTime = date.getTime();
		const dateArrayConverted = dateArray.map(date => date.getTime()).map(time => Math.abs(currentDateTime - time));
		const index = dateArrayConverted.indexOf(Math.min(...dateArrayConverted));
		return dateArray[index];
	}

    private addToFormats(formats: Array<string>,formatLetters: Array<string>, newChar: string, isSeparator: boolean) {
        const size = formats.length;
        if (size == 0) {
            formats.push(newChar);
        } else {
            const formatsCopy = Array.from(formats);
            if (newChar.match(/[a-zA-Z]/) && formatLetters.indexOf(newChar) == -1) {
                formatLetters.push(newChar);
            }
            for (let i = 0; i < formatsCopy.length; i++) {
                const newFormat = formatsCopy[i] + newChar;
                if (isSeparator && formatsCopy[i].lastIndexOf(newChar) == formatsCopy[i].length - 1)
                    continue;
                if (formats.indexOf(newFormat) == -1) {
                    if (!this.containsAllLetters(newFormat,formatLetters )) 
                        continue;
                    let insertIndex = -1;
                    if (newChar.match(/[a-zA-Z]/) && newFormat.indexOf(newChar) < newFormat.length - 1){
                        insertIndex = formats.indexOf(formatsCopy[i]);
                    }
                    if (insertIndex>= 0){
                        formats.splice(insertIndex, 0 , newFormat);
                    }else{
                        formats.push(newFormat);
                    }
                }
            }
        }
    }

    private containsAllLetters(newFormat: string, formatLetters: Array<string>): boolean{
        for (const formatLetter of  formatLetters){
            if (newFormat.indexOf(formatLetter) < 0){
                return false;
            }
        }
        return true;
    }
    
    private getHeuristicFormats(servoyFormat: string, input?: string): Array<string> {
        const formats = new Array<string>();
        const formatLetters = new Array<string>();
        if (servoyFormat) {
            for (let index = 0; index < servoyFormat.length; index++) {
                const currentChar = servoyFormat.charAt(index);
                if (currentChar.match(/[a-zA-Z]/)) {
                    this.addToFormats(formats,formatLetters, currentChar, false);
                } else {
                    this.addToFormats(formats,formatLetters, currentChar, true);
                }
            }
        }
		if (input) {
			return formats.filter(format => format.length === input.length && this.compareInputWithFormat(input, format));
		} 
        return formats;
    }
	
	private compareInputWithFormat(input: string, format: string): boolean {
		for (let i = 0; i < input.length; i ++) {
			const inputChar = input[i];
			const formatChar = format[i];
			if ((!inputChar.match(/[0-9]/) || !formatChar.match(/[a-zA-Z]/)) && inputChar !== formatChar) {
				return false;
			}
		}
		return true;
	}

    private unformatNumbers(data: any, format: string) { // todo throw error when not coresponding to format (reimplement with state machine)
        if (data === '') return data;
        //treat scientiffic numbers
        if (data.toString().toLowerCase().indexOf('e') > -1 && !isNaN(data)) {
            return new Number(data).valueOf();
        }

        let multFactor = 1;
        const MILLSIGN = '\u2030';
        if (format.indexOf(MILLSIGN) > -1 && format.indexOf('\'' + MILLSIGN + '\'') == -1) {
            multFactor *= 0.001;
        }
        if (format.indexOf('\'') > -1) {
            // replace the literals
            const parts = format.split('\'');
            for (let i = 0; i < parts.length; i++) {
                if (i % 2 === 1) {
                    data = data.replace(new RegExp(parts[i], 'g'), '');
                }
            }
        }
        const formatDecimalSeparatorPos = format.indexOf('\.');
        if (formatDecimalSeparatorPos > -1) {
            const currentLanguageNumeralSymbols = numbro.languageData();
            const dataDecimalSeparatorPos = data.indexOf(currentLanguageNumeralSymbols.delimiters.decimal);
            if (dataDecimalSeparatorPos > -1) {
                const decimalLen = format.length - formatDecimalSeparatorPos - 1;
                let adjustedData = data.toString().substring(0, dataDecimalSeparatorPos + 1);
                const decimal = data.toString().substring(dataDecimalSeparatorPos + 1);
                if (decimal.length > decimalLen) {
                    adjustedData += decimal.substring(0, decimalLen);
                    data = adjustedData;
                }
            }
        }
        let ret = numbro(data).value();
        ret *= multFactor;
        return ret;
    }

    private numbersonly(e, decimal, decimalChar, groupingChar, currencyChar, percentChar, vElement, mlength, vSvyFormat) {
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
        if (this.numbersonlyForChar(keychar, decimal, decimalChar, groupingChar, currencyChar, percentChar, vElement, mlength) && vSvyFormat !== null) {
            const value = vElement.value;
            if (value.includes(decimalChar) && window.getSelection().toString() !== value) {
                const allowToConcat = value.indexOf(decimalChar);
                if (e.target.selectionStart <= allowToConcat) {
                    return true;
                }
                if (vSvyFormat.edit) {
                    const maxDecimals = vSvyFormat.edit.split(SVY_FORMAT_DECIMAL_CHAR)[1].length;
                    if (value.split(decimalChar)[1].length >= maxDecimals) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return this.numbersonlyForChar(keychar, decimal, decimalChar, groupingChar, currencyChar, percentChar, vElement, mlength);
        }
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
        if (textarea) {
            const start = textarea['selectionStart'];
            const end = textarea['selectionEnd'];
            sel = textarea['value'].substring(start, end);
        }
        return sel;
    }

    private formatNumbers(data: any, servoyFormat: string): string {
        if (!servoyFormat)
            return data;
        if (data === '')
            return data;

        data = Number(data); // just to make sure that if it was a string representing a number we turn it into a number
        if (typeof data === 'number' && isNaN(data)) return ''; // cannot format something that is not a number

        const initialData = data;
        let patchedFormat = servoyFormat; // patched format for numbro format
        let i: number;
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
        let ret: string;

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

            let leftFormat = '';
            let rightFormat = '';

            if (patchedFormat.indexOf('.') >= 0) {
                leftFormat = patchedFormat.split('.')[0];
                rightFormat = patchedFormat.split('.')[1];
            } else {
                leftFormat = patchedFormat;
            }

            let minLenCharacteristic = 0;
            let minLenCharacteristicAfterZeroFound = 0;
            let optionalDigitsCharacteristic = 0;
            let zeroFound = false;
            for (i = 0; i < leftFormat.length; i++) {
                if (leftFormat[i] === '0') {
                    zeroFound = true;
                    minLenCharacteristic++;
                } else if (leftFormat[i] === '#') {
                    optionalDigitsCharacteristic++;
                }
                if (leftFormat[i] === '#' && zeroFound) {
                    minLenCharacteristicAfterZeroFound++;
                }
            }

            let minLenMantissa = 0;
            let optionalDigitsMantissa = 0;

            for (i = rightFormat.length - 1; i >= 0; i--) {
                if (rightFormat[i] === '0') {
                    minLenMantissa++;
                } else if (rightFormat[i] === '#') {
                    optionalDigitsMantissa++;
                } else if (rightFormat[i] === '.') {
                    break;
                }
            }

            let dataAsString = data + '';

            let rightData = '';

            if (dataAsString.indexOf('.') >= 0) {
                rightData = dataAsString.split('.')[1];
            }

            let rightDataMantissaLength = rightData.length;
            let mantissaLength = 0;
            if (rightDataMantissaLength <= minLenMantissa) {
                mantissaLength = minLenMantissa
            } else if (rightDataMantissaLength < minLenMantissa + optionalDigitsMantissa) {
                mantissaLength = rightDataMantissaLength;
            } else {
                mantissaLength = minLenMantissa + optionalDigitsMantissa;
            }
            ret = numbro(data).format({
                thousandSeparated: data > 999 && patchedFormat.includes(',') ? true : false,
                mantissa: mantissaLength,
                optionalMantissa: minLenMantissa === 0,
                trimMantissa: minLenMantissa === 0 && optionalDigitsMantissa >= 0 ? true : false,
                characteristic: minLenCharacteristic + minLenCharacteristicAfterZeroFound,
                optionalCharacteristic: rightDataMantissaLength === 0 && minLenMantissa === 0 && minLenCharacteristic === 0 && optionalDigitsCharacteristic > 0
            });
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
        if (!(data instanceof Date)) return data;
        // single quote escape workaround until https://github.com/moment/luxon/issues/649 is fixed
        dateFormat = this.convertFormat(dateFormat).replace("''", "'svy_quote'");
        const formatted = DateTime.fromJSDate(data).setLocale(this.servoyService.getLocale()).toFormat(dateFormat).replace('svy_quote', "'");
        return formatted.trim ? formatted.trim() : formatted;
    }

    private convertFormat(dateFormat: string): string {
        let result = '';
        let inSingleQuotes = false;
        let containsEE = dateFormat.includes('EE');
    
        for (let i = 0; i < dateFormat.length; i++) {
            if (dateFormat[i] === "'") {
                inSingleQuotes = !inSingleQuotes;
                result += dateFormat[i];
                continue;
            }
    
            if (inSingleQuotes) {
                result += dateFormat[i];
                continue;
            }
    
            if (!containsEE && dateFormat[i] === 'E') {
                result += 'EEE';
                continue;
            }
    
            if (dateFormat[i] === 'a' && i < dateFormat.length - 1 && dateFormat[i + 1] === 'a') {
                result += 'a'; 
                i++; 
                continue;
            }
    
            switch (dateFormat[i]) {
                case 'Y': result += 'y'; break;
                case 'u': result += 'E'; break;
                case 'w': result += 'W'; break;
                case 'K': result += 'h'; break;
                case 'k': result += 'H'; break;
                case 'D': result += 'o'; break;

                default: result += dateFormat[i]; 
            }
        }
    
        if (result.includes('W')) {
            let tempResult = '';
            inSingleQuotes = false;
    
            for (let i = 0; i < result.length; i++) {
                if (result[i] === "'") {
                    inSingleQuotes = !inSingleQuotes;
                    tempResult += result[i];
                    continue;
                }
    
                if (!inSingleQuotes && result[i] === 'y') {
                    tempResult += 'k';
                } else {
                    tempResult += result[i];
                }
            }
    
            result = tempResult;
        }
    
        return result;
    }    
}
