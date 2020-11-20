import { Pipe, PipeTransform } from '@angular/core';
import { IValuelist } from '../../sablo/spectypes.service';

@Pipe({name: 'showDisplayValue'})
export class ShowDisplayValuePipe implements PipeTransform {

    transform(input: any, ...args: any[]) {
        let realValue = input;
        const valuelist: IValuelist = args[0];
        const noEscape = args[1];
        const noParsedDisplayValue = args[2];

        if (valuelist) {
            if (input && input.hasOwnProperty('realValue')) {
                realValue = input.realValue;
            }
            for (let i = 0; i < valuelist.length; i++) {
                if ((realValue + '') === (valuelist[i].realValue + '')) {
                    return noParsedDisplayValue ? valuelist[i].displayValue : this.getParsedDisplayValue(valuelist[i].displayValue, noEscape);
                }
            }
            let hasRealValues = false;
            for (let i = 0; i < valuelist.length; i++) {
                const item = valuelist[i];
                if (item.realValue != item.displayValue) {
                    hasRealValues = true;
                    break;
                }
            }
            if (hasRealValues) {
                let displayValue = null;
                valuelist.getDisplayValue(realValue).subscribe((val) => {
                    displayValue = val;
                });
                return noParsedDisplayValue ? displayValue : this.getParsedDisplayValue(displayValue, noEscape);
            }
            if (valuelist.length == 0) return null;

            return input;
        }
    }

    getParsedDisplayValue(value: string, noEscape: any) {
        if (!value || value === '') {
            return noEscape ? ' ' : '&nbsp';
        } else {
            return value;
        }
    }
}
