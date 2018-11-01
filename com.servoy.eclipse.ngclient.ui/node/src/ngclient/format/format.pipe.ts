import { Injector, Pipe, PipeTransform} from '@angular/core';
import {UpperCasePipe, LowerCasePipe, DatePipe, DecimalPipe } from '@angular/common';
import {SabloService} from '../../sablo/sablo.service';

@Pipe( { name: 'formatFilter'} )
export class FormatFilterPipe implements PipeTransform {
    
    public constructor(private injector: Injector, private sabloService: SabloService) {
    }

    transform( input:any, servoyFormat:string, type:string, fullFormat:{uppercase:boolean,lowercase:boolean, type:string}): any{
        var ret = input;
        try {
            // TODO apply servoy default formatting from properties file here
            if (type == 'DATETIME' && !servoyFormat) {
                servoyFormat = 'MM/dd/yyyy hh:mm aa';
                type = 'DATETIME';
            }

            // commented out because in case one uses a INTEGER dp + format + DB-col(INTEGER)-valuelist we might receive strings
            // instead of ints (display values of DB valuelist do .toString() on server); don't know why that is; $formatterUtils.format can handle that correctly though and it should work the same here (portal vs. extra table)
            // if (((typeof input === 'string') || (input instanceof String)) && type !== "TEXT") return input;
// TODO formatter utils
//            ret = $formatterUtils.format(input, servoyFormat, type);
            if (ret && fullFormat) {
                var locale = this.sabloService.getLocale().full;//can be the one from browser or set on solution open
                if (type == 'TEXT' && fullFormat.uppercase)
                {
                    ret = this.injector.get(UpperCasePipe).transform(ret);
                }
                else if (type == 'TEXT' && fullFormat.lowercase)
                {
                    ret = this.injector.get(LowerCasePipe).transform(ret);
                }
                else if (type == 'DATETIME')
                {
                    ret = this.injector.get(DatePipe).transform(ret, servoyFormat, null, locale); // TODO timezone?
                }
                else if (type == 'NUMBER' || type == 'INTEGER')
                {
                    if (!servoyFormat.match(/[1-9]+(\.\d+(-\d+)?)?/g))
                    {
                        servoyFormat = this.convertFormat(servoyFormat);
                    }
                    ret = this.injector.get(DecimalPipe).transform(ret, servoyFormat, locale);
                }
            }            
                
        } catch (e) {
            console.log(e);
            return input;
        }
        return ret;
    }
    
    private convertFormat(servoyFormat : string) {
        var digits = servoyFormat.split(".");
        var minIntegerDigits = Math.max((digits[0].match(/0/g)||[]).length, 1);
        var format = ""+minIntegerDigits;
        if (digits.length == 2)
        {
            var minFractionDigits = (digits[1].match(/0/g)||[]).length;
            format += "."+minFractionDigits;
            if (minFractionDigits != digits[1].length) format += "-" + digits[1].length;
        }
        return format;
    }
}