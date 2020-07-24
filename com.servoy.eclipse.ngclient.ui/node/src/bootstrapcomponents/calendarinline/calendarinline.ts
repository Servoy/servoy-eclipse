import { Component, OnInit, Renderer2 } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';

@Component({
  selector: 'servoybootstrap-calendarinline',
  templateUrl: './calendarinline.html',
  styleUrls: ['./calendarinline.scss']
})
export class ServoyBootstrapCalendarinline extends ServoyBootstrapBasefield {

    constructor(renderer: Renderer2) { 
        super(renderer);
    }

}
