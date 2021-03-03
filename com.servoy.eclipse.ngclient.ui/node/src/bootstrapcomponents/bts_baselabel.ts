import { ServoyBootstrapBaseComponent } from './bts_basecomp';
import { Input, Renderer2, Directive, ChangeDetectorRef } from '@angular/core';

@Directive()
// eslint-disable-next-line
export class ServoyBootstrapBaseLabel<T extends HTMLElement> extends ServoyBootstrapBaseComponent<T> {

    @Input() onActionMethodID: (e: Event, data?: any) => void;
    @Input() onRightClickMethodID: (e: Event, data?: any) => void;
    @Input() onDoubleClickMethodID: (e: Event, data?: any) => void;

    @Input() imageStyleClass: string;
    @Input() showAs: string;

    constructor(renderer: Renderer2, protected cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

    svyOnInit() {
        super.svyOnInit();
        if (this.onActionMethodID) {
            if (this.onDoubleClickMethodID) {
                this.renderer.listen(this.getFocusElement(), 'click', e => {
                    if (this.timeoutID) {
                        window.clearTimeout(this.timeoutID);
                        this.timeoutID = null;
                        // double click, do nothing will be done in sub classes
                    } else {
                        this.timeoutID = window.setTimeout(() => {
                            this.timeoutID = null;
                            this.onActionMethodID(e);
                        }, 250);
                    }
                });
            } else {
                    this.renderer.listen(this.getFocusElement(), 'click', e => this.onActionMethodID(e));
            }
        }
        if (this.onRightClickMethodID) {
            this.renderer.listen(this.getFocusElement(), 'contextmenu', e => {
                this.onRightClickMethodID(e); return false;
            });
        }
    }

    isTrustedHTML(): boolean {
        if (this.servoyApi.trustAsHtml() || this.showAs === 'trusted_html') {
            return true;
        }
        return false;
    }
}
