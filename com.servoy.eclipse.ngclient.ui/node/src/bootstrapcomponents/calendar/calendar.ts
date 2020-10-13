import { Component, Renderer2, ElementRef, ViewChild, Input, ChangeDetectorRef, SimpleChanges} from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';
import { OwlDateTimeIntl } from '@danielmoncada/angular-datetime-picker';
import * as moment from 'moment';

@Component({
  selector: 'servoybootstrap-calendar',
  templateUrl: './calendar.html',
  styleUrls: ['./calendar.scss'],
  providers: [OwlDateTimeIntl]
})
export class ServoyBootstrapCalendar extends ServoyBootstrapBasefield {

    @ViewChild( 'inputElement') inputElementRef: ElementRef;
    @Input() format;

    public filter: any;
    min: Date;
    max: Date;
    
    public firstDayOfWeek = 1;
    public hour12Timer = false;
    public pickerType = 'both';
    public showSecondsTimer  = false;
    
    constructor(renderer: Renderer2, protected cdRef: ChangeDetectorRef) { 
        super(renderer, cdRef);
        
        const ld = moment.localeData();
        this.firstDayOfWeek = ld.firstDayOfWeek();
        const  lts = ld.longDateFormat('LTS');
        this.hour12Timer = lts.indexOf('a') >= 0 || lts.indexOf('A') >= 0;
    }

    svyOnChanges( changes: SimpleChanges ) {
        for ( let property in changes ) {
            let change = changes[property];
            switch ( property ) {
                case 'format':
                    const format = change.currentValue.display;
                    const showCalendar = format.indexOf('y') >= 0 || format.indexOf('M') >= 0;
                    const showTime = format.indexOf('h') >= 0 || format.indexOf('H') >= 0 || format.indexOf('m') >= 0;
                    if (showCalendar) {
                        if (showTime) this.pickerType = 'both';
                        else this.pickerType = 'calendar'
                    } else this.pickerType = 'timer'
                    this.showSecondsTimer = format.indexOf('s') >= 0;
                    this.hour12Timer = format.indexOf('h') >= 0 || format.indexOf('a') >= 0 || format.indexOf('A') >= 0;
                    break;
            }
        }
        super.svyOnChanges(changes);
    }
    
    getFocusElement(): any {
        return this.inputElementRef.nativeElement;
    }

    getStyleClassElement(): any {
        return this.inputElementRef.nativeElement;
    }

    public disableDays(dateArray: Number[]) {
        this.filter = (d: moment.Moment): boolean => {
            return !dateArray.includes(d.day());
        };
    }

    public disableDates(dateArray: Date[]) {
        this.filter = (d: moment.Moment): boolean => {
          let result = true;
          dateArray.forEach(el => {
            if (el.toString() === d.toDate().toString()) {
                result = false;
            }
          });
          return result;
        }
    }

    public setMinMaxDate(minDate: Date, maxDate: Date) {
        if (minDate) this.min = minDate;
        if (maxDate) this.max = maxDate;
    }
}
