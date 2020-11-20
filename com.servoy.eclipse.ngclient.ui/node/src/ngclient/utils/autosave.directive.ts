import { Directive , HostListener, ElementRef} from '@angular/core';
import { SabloService } from '../../sablo/sablo.service';

@Directive({
  selector: '[svyAutosave]'
})
export class AutosaveDirective {

  constructor(private sabloService: SabloService, private elementRef: ElementRef) {
  }

  @HostListener('click', ['$event.target'])
  onClick(target): void {
    if (target == this.elementRef.nativeElement || target.parentNode == this.elementRef.nativeElement) {
      this.sabloService.callService('applicationServerService', 'autosave', {}, true);
    }
  }
}
