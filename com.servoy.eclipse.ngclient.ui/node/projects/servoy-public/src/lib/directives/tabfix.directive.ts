import { Directive , Input , HostListener} from '@angular/core';
import {IPopupSupportComponent} from '../spectypes.service';

@Directive({
    selector: '[svyTabFix]',
    standalone: false
})
export class TabFixDirective {
    // this directive needs to be in a module because of listener priority, so i put it here
    @Input('svyTabFix') typeahead: IPopupSupportComponent;
    startedTyping = false;

    constructor() { }

    @HostListener('focus', ['$event'])
    onFocus(target) {
       this.startedTyping = false;
    }

    @HostListener('keydown', ['$event'])
    handleKeyDown(event: KeyboardEvent) {
        if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
            // stop propagation when using list form component (to not break the selection)
            event.stopPropagation();
        }
        if (event.key !== 'Tab') {
            this.startedTyping = true;
        } else if (!this.startedTyping) {
            this.typeahead.closePopup();
        }
    }
}
