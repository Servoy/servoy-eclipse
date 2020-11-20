import { Injectable } from '@angular/core';
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

    // formatting stufff
    public format(data: any, format: string, type: string): string {
        if ((!format) || (!type) || ((typeof data === 'number') && isNaN(data))) {
            if (!format && ((type === 'NUMBER') || (type === 'INTEGER')) && (typeof data === 'number') && !isNaN(data)) {
                // make sure is always returned with correct type, otherwise compare will not work well
                return data.toString();
            }
            return data;
        }
        if (data === undefined || data === null) return '';
        if ((type === 'NUMBER') || (type === 'INTEGER')) {
            return this.formatNumbers(data, format);
        } else if (type === 'TEXT') {
            return this.formatText(data, format);
        } else if (type === 'DATETIME') {
            return this.formatDate(data, format);
        }
        return data;
    }

    public testKeyPressed(event: KeyboardEvent, keyCode: number) {
        let code;

        if (!event) event = window.event as KeyboardEvent;
        if (!event) return false;
        if (event.keyCode) code = event.keyCode;
        else if (event.which) code = event.which;
        return code === keyCode;
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
