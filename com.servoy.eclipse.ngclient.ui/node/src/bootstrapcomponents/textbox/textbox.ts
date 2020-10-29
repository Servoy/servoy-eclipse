import { Component, ChangeDetectorRef, Renderer2, Input, SimpleChanges } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';
import { FormattingService } from '../../ngclient/servoy_public';

@Component({
  // tslint:disable-next-line: component-selector
  selector: 'servoybootstrap-textbox',
  templateUrl: './textbox.html',
  styleUrls: ['./textbox.scss']
})
export class ServoyBootstrapTextbox extends ServoyBootstrapBasefield {

  @Input() format;
  @Input() inputType;
  @Input() selectOnEnter;
  @Input() autocomplete;

  constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, private formattingService: FormattingService) {
    super(renderer, cdRef);
  }
}
