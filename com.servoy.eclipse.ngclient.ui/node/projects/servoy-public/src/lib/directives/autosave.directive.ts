import { Directive , HostListener, ElementRef, inject} from '@angular/core';
import { ServoyPublicService } from '../services/servoy_public.service';

@Directive({
    selector: '[svyAutosave]',
    standalone: true
})
export class AutosaveDirective {

  private servoyService: ServoyPublicService;
  private elementRef: ElementRef;

  constructor(servoyService?: ServoyPublicService, elementRef?: ElementRef) {
      this.servoyService = servoyService ?? inject(ServoyPublicService);
      this.elementRef = elementRef ?? inject(ElementRef);
  }

  @HostListener('click', ['$event.target'])
  onClick(target: any): void {
    if (target == this.elementRef.nativeElement || target.parentNode == this.elementRef.nativeElement) {
      this.servoyService.callService('applicationServerService', 'autosave', {}, true);
    }
  }
}
