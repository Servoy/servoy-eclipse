import { Directive , Input , ElementRef, HostListener} from '@angular/core';
import { getLocaleNumberSymbol, NumberSymbol } from '@angular/common';
import { LocaleService } from '../locale.service';
import { Format } from '../format/formatting.service';

@Directive({
  selector: '[svyDecimalKeyConverter]'
})
export class DecimalkeyconverterDirective {

  @Input('svyDecimalKeyConverter') svyFormat: Format;
  private element: HTMLInputElement;

  public constructor(private el: ElementRef, private localeService: LocaleService) {
      this.element = el.nativeElement;
  }

  @HostListener('keydown', ['$event']) onKeypress(e: KeyboardEvent) {
      if (e.which === 110 && this.svyFormat && this.svyFormat.type === 'NUMBER') {
          const caretPos = this.element.selectionStart;
          const startString = this.element.value.slice(0, caretPos);
          const endString = this.element.value.slice(this.element.selectionEnd, this.element.value.length);
          this.element.value = (startString + getLocaleNumberSymbol(this.localeService.getLocale(), NumberSymbol.Decimal) + endString);
          this.element.focus();
          this.element.setSelectionRange(caretPos + 1, caretPos + 1);
          if (e.preventDefault) e.preventDefault();
      }
  }
}
