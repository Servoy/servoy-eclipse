import { Component, Renderer2, ElementRef, ViewChild, Input  } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';
import { DateTimeAdapter, OwlDateTimeIntl } from '@danielmoncada/angular-datetime-picker';

@Component({
  selector: 'servoybootstrap-calendar',
  templateUrl: './calendar.html',
  styleUrls: ['./calendar.scss'],
  providers: [OwlDateTimeIntl]
})
export class ServoyBootstrapCalendar extends ServoyBootstrapBasefield {

    @ViewChild( 'inputElement' , {static: true} ) inputElementRef: ElementRef;
    @Input() format;
    
    constructor(renderer: Renderer2) { 
        super(renderer);
    }
    
    getFocusElement(): any {
        return this.inputElementRef.nativeElement;
    }

    getStyleClassElement(): any {
        return this.inputElementRef.nativeElement;
    }

}
