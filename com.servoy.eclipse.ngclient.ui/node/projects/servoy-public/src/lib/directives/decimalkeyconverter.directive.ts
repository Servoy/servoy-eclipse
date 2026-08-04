import { Directive , Input , ElementRef, HostListener, inject} from '@angular/core';
import { ServoyPublicService } from '../services/servoy_public.service';
import { Format } from '../format/formatting.service';
import { NumberSymbol } from '@angular/common';

@Directive({
    selector: '[svyDecimalKeyConverter]',
    standalone: false
})
export class DecimalkeyconverterDirective {

  @Input('svyDecimalKeyConverter') svyFormat!: Format;
  private element: HTMLInputElement;
  private servoyService: ServoyPublicService;

  public constructor(el?: ElementRef, servoyService?: ServoyPublicService) {
      const elRef = el ?? inject(ElementRef);
      this.element = elRef.nativeElement;
      this.servoyService = servoyService ?? inject(ServoyPublicService);
  }

  @HostListener('keydown', ['$event']) onKeypress(e: KeyboardEvent) {
      if (e.which === 110 && this.svyFormat && this.svyFormat.type === 'NUMBER') {
          const caretPos = this.element.selectionStart!;
          const startString = this.element.value.slice(0, caretPos);
          const endString = this.element.value.slice(this.element.selectionEnd!, this.element.value.length);
          this.element.value = (startString + this.servoyService.getLocaleNumberSymbol(NumberSymbol.Decimal) + endString);
          this.element.focus();
          if(this.element.type === 'text') this.element.setSelectionRange(caretPos + 1, caretPos + 1);
          if (e.preventDefault) e.preventDefault();
      }
  }

}
