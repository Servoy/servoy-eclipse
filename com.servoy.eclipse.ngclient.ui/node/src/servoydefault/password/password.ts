import {Component, OnInit, Renderer2} from '@angular/core';
import {FormattingService} from "../../ngclient/servoy_public";
import {ServoyDefaultBaseField} from "../basefield";
@Component({
  selector: 'servoydefault-password',
  templateUrl: './password.html'
})
export class ServoyDefaultPassword extends ServoyDefaultBaseField implements OnInit{
  constructor(renderer: Renderer2, formattingService: FormattingService) {
    super(renderer, formattingService);
  }
}
