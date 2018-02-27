import { Pipe, PipeTransform } from '@angular/core';


@Pipe( { name: 'mnemonicletterFilter' } )
export class MnemonicletterFilterPipe implements PipeTransform {
    transform( input: string, letter: string): string{
        if(letter && input) return input.replace(letter, '<u>'+letter+'</u>');
        return input
    } 
}

@Pipe( { name: 'formatFilter' } )
export class FormatFilterPipe implements PipeTransform {
    transform( input:any, servoyFormat:string, type:string, fullFormat:{uppercase:boolean,lowercase:boolean}): string{
        var ret = input;
        try {
            // TODO apply servoy default formatting from properties file here
            if (input instanceof Date && !servoyFormat) {
                servoyFormat = 'MM/dd/yyyy hh:mm aa';
                type = 'DATETIME';
            }

            // commented out because in case one uses a INTEGER dp + format + DB-col(INTEGER)-valuelist we might receive strings
            // instead of ints (display values of DB valuelist do .toString() on server); don't know why that is; $formatterUtils.format can handle that correctly though and it should work the same here (portal vs. extra table)
            // if (((typeof input === 'string') || (input instanceof String)) && type !== "TEXT") return input;
// TODO formatter utils
//            ret = $formatterUtils.format(input, servoyFormat, type);
            if (ret && fullFormat && type == 'TEXT' && fullFormat.uppercase)
            {
                ret = ret.toUpperCase();
            }
            if (ret && fullFormat && type == 'TEXT' && fullFormat.lowercase)
            {
                ret = ret.toLowerCase();
            }
                
        } catch (e) {
            console.log(e);
            //TODO decide what to do when string doesn't correspod to format
        }
        return ret;
    }
}
