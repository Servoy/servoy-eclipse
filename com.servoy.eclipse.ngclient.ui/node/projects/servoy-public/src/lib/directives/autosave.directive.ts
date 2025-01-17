import { Directive , HostListener, ElementRef} from '@angular/core';
import { ServoyPublicService } from '../services/servoy_public.service';

@Directive({
    selector: '[svyAutosave]',
    standalone: false
})
export class AutosaveDirective {

  constructor(private servoyService: ServoyPublicService, private elementRef: ElementRef) {
  }

  @HostListener('click', ['$event.target'])
  onClick(target): void {
    if (target == this.elementRef.nativeElement || target.parentNode == this.elementRef.nativeElement) {
      this.servoyService.callService('applicationServerService', 'autosave', {}, true);
    }
  }
}
