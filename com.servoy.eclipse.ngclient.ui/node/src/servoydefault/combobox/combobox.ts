import { Component, Renderer2, ViewChild , ElementRef, OnChanges, SimpleChanges} from '@angular/core';
import { FormattingService } from '../../ngclient/servoy_public';
import { ServoyDefaultTypeahead } from '../typeahead/typeahead';

@Component({
  selector: 'servoydefault-combo',
  templateUrl: './combobox.html'
})
export class ServoyDefaultCombobox extends ServoyDefaultTypeahead {

  // this is a hack so that this can be none static access because this references in this component to a conditional template
  @ViewChild('input', {static: true}) inputElement: ElementRef;

  constructor(renderer: Renderer2,
              formattingService: FormattingService) {
    super(renderer, formattingService);
  }

  getFocusElement(): any {
      return this.inputElement.nativeElement;
  }

  ngOnChanges( changes: SimpleChanges ) {
    // this change should be ignored for the combobox.
    delete changes['editable'];
    super.ngOnChanges(changes);
  }
}
