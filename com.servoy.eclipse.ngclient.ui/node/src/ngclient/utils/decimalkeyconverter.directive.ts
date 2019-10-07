import { Directive , Input , ElementRef, HostListener} from '@angular/core';
import { Format } from '../format/format.directive';
import * as numeral from 'numeral';

@Directive({
  selector: '[svyDecimalKeyConverter]'
})
export class DecimalkeyconverterDirective {

  @Input('svyDecimalKeyConverter') svyFormat : Format;
  private element: HTMLInputElement;
  
  public constructor(private el: ElementRef) {
      this.element = el.nativeElement;
  }

  @HostListener('keydown',['$event']) onKeypress(e:KeyboardEvent) {   
      if(e.which == 110 && this.svyFormat && this.svyFormat.type == 'NUMBER') {
          var caretPos = this.element.selectionStart;
          var startString = this.element.value.slice(0, caretPos);
          var endString = this.element.value.slice(this.element.selectionEnd, this.element.value.length);
          this.element.value = (startString + numeral.localeData().delimiters.decimal + endString);
          this.element.focus();
          this.element.setSelectionRange(caretPos+1, caretPos+1);
          if (e.preventDefault) e.preventDefault();
      }
  }
}
