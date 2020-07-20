import { Component, OnInit, Renderer2, Input } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';
import { IValuelist } from '../../sablo/spectypes.service';

@Component({
  selector: 'servoybootstrap-combobox',
  templateUrl: './combobox.html',
  styleUrls: ['./combobox.scss']
})
export class ServoyBootstrapCombobox extends ServoyBootstrapBasefield implements OnInit {

  @Input() format;
  @Input() showAs;
  @Input() valuelist: IValuelist;

  constructor(renderer: Renderer2) {
    super(renderer);
}

  ngOnInit(): void {
  }

}
