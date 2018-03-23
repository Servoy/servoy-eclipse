import { Injectable } from '@angular/core';
import * as moment from 'moment';
import * as numeral from 'numeral';

const MILLSIGN : string = '\u2030';

@Injectable()
export class FormattingService {   

   parse(value:string, format:{display:string, type:string}, currentValue : Object):Object {
       var result = value;
       if (format.type == "DATETIME")
       {
           return this.parseDate(value,format.display, currentValue as Date);
       }
       if (format.type == "NUMBER" || format.type == "INTEGER")
       {
           return (value === "") ? value : this.parseNumber(value, format.display);
       }
       return result;       
   } 
   
   private parseDate(value:string, servoyFormat:string, currentValue : Date) : Date {
       // some compatibility issues, see http://momentjs.com/docs/ and http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html 
       var format = servoyFormat.replace(/d/g,'D');
       format = format.replace(/y/g,'Y');
       var m = moment(value, format);
       var date = m.isValid() ? m.toDate() : new Date(value);
       if (date)
       {
           // if format has not year/month/day use the one from the current model value
           // because moment will just use current date
          if(currentValue) {
               if(format.indexOf('Y') == -1) {
                   date.setFullYear(currentValue.getFullYear());
               }
               if(format.indexOf('M') == -1) {
                   date.setMonth(currentValue.getMonth());
               }
               if(format.indexOf('D') == -1) {
                   date.setDate(currentValue.getDate());
               }
           }
       }
       return date;
   }
   
   private parseNumber(value:string, format:string) : Number {
       //treat scientific numbers
       if (value.toString().toLowerCase().indexOf('e') > -1 && !isNaN(Number(value))) {
           return new Number(value).valueOf()
       }

       var multFactor = 1;
       if (format.indexOf(MILLSIGN) > -1 && format.indexOf("'"+MILLSIGN+"'") == -1) {
           multFactor *= 0.001
       }
       if (format.indexOf("'%'") > -1) {
           multFactor = 100
       }

       var ret = numeral(value).value();
       ret *= multFactor;
       return ret
   }
} 