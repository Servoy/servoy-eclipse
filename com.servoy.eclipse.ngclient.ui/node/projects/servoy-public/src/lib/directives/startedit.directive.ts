import { Directive, input, HostListener, inject} from '@angular/core';
import {LoggerService, LoggerFactory} from '../logger.service';
import { ServoyBaseComponent } from '../basecomponent';

@Directive({
    selector: '[svyStartEdit]',
    standalone: true
})
export class StartEditDirective {

  readonly dataProviderID = input<string>(undefined as any, { alias: 'svyStartEdit' });
  readonly hostComponent = input<ServoyBaseComponent<HTMLElement>>(undefined as any);

  private log: LoggerService;

  public constructor(logFactory?: LoggerFactory) {
    this.log = (logFactory ?? inject(LoggerFactory)).getLogger('StartEditDirective');
  }

  @HostListener('focus', ['$event']) onFocus(e: FocusEvent) {
    if (!this.hostComponent()) {
      this.log.error('host component not found for the start edit directive use [hostComponent]="self" besides this in the template (component must be extending ServoyBaseComponent)');
    } else if (this.hostComponent().servoyApi && this.dataProviderID() !== undefined) {
      this.hostComponent().servoyApi.startEdit(this.dataProviderID());
    } else {
      this.log.error('Can\'t call startEdit, missing servoyApi and dataProviderID for field ' + this.hostComponent());
    }
  }
}
