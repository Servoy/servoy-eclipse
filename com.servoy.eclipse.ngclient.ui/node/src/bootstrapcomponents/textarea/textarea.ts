import { Component, OnInit, Renderer2,ViewEncapsulation } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';

@Component({
  selector: 'servoybootstrap-textarea',
  templateUrl: './textarea.html',
  styleUrls: ['./textarea.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ServoyBootstrapTextarea extends ServoyBootstrapBasefield {

  constructor(renderer: Renderer2) {
    super(renderer);
  }
}
