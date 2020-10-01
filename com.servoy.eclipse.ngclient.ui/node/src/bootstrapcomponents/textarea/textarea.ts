import { Component, Input, OnInit, Renderer2 } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';

@Component({
  selector: 'servoybootstrap-textarea',
  templateUrl: './textarea.html',
  styleUrls: ['./textarea.scss']
})
export class ServoyBootstrapTextarea extends ServoyBootstrapBasefield implements OnInit {

  @Input() maxLength: number;

  ngOnInit() {
    super.ngOnInit();
    if (!this.maxLength || this.maxLength == 0) {
      this.maxLength = 524288;
    }
  }

  constructor(renderer: Renderer2) {
    super(renderer);
  }
}
