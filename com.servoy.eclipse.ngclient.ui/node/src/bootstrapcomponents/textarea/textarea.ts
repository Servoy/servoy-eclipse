import { Component, OnInit, Renderer2 } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';

@Component({
  selector: 'servoybootstrap-textarea',
  templateUrl: './textarea.html',
  styleUrls: ['./textarea.scss']
})
export class ServoyBootstrapTextarea extends ServoyBootstrapBasefield {

  constructor(renderer: Renderer2) {
    super(renderer);
   }
}
