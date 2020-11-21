import { ServoyBootstrapBaseComponent } from './bts_basecomp';
import { Input, Renderer2, Directive, ChangeDetectorRef } from '@angular/core';

@Directive()
// eslint-disable-next-line
export class ServoyBootstrapBaseLabel extends ServoyBootstrapBaseComponent {

    @Input() imageStyleClass: string;
    @Input() showAs: string;

    constructor(renderer: Renderer2, protected cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

    isTrustedHTML(): boolean {
        if(this.servoyApi.trustAsHtml() || this.showAs === 'trusted_html') {
            return true;
        }
        return false;
    }
}
