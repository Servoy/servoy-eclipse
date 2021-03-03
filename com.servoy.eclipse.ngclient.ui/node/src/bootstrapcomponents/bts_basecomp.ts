import { ServoyBaseComponent } from '../ngclient/servoy_public';
import { Directive, Input, Renderer2, SimpleChanges, ChangeDetectorRef } from '@angular/core';

@Directive()
// eslint-disable-next-line
export class ServoyBootstrapBaseComponent<T extends HTMLElement> extends ServoyBaseComponent<T> {

    @Input() enabled: boolean;
    @Input() size: { width: number; height: number };
    @Input() styleClass: string;
    @Input() tabSeq: number;
    @Input() text: string;
    @Input() toolTipText: string;

    timeoutID: number;

    constructor(protected readonly renderer: Renderer2, protected cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

    svyOnChanges(changes: SimpleChanges) {
        if (changes) {
            for (const property of Object.keys(changes)) {
                const change = changes[property];
                switch (property) {
                    case 'enabled':
                        if (change.currentValue)
                            this.renderer.removeAttribute(this.getFocusElement(), 'disabled');
                        else
                            this.renderer.setAttribute(this.getFocusElement(), 'disabled', 'disabled');
                        break;
                    case 'styleClass':
                        if (change.previousValue) {
                            const array = change.previousValue.trim().split(' ');
                            array.filter((element: string) => element !== '').forEach((element: string) => this.renderer.removeClass(this.getStyleClassElement(), element));
                        }
                        if (change.currentValue) {
                            const array = change.currentValue.trim().split(' ');
                            array.filter((element: string) => element !== '').forEach((element: string) => this.renderer.addClass(this.getStyleClassElement(), element));
                        }
                        break;
                }
            }
        }
        super.svyOnChanges(changes);
    }

    public getFocusElement(): HTMLElement {
        return this.getNativeElement();
    }

    public getStyleClassElement(): any {
        return this.getNativeElement();
    }

    public requestFocus(mustExecuteOnFocusGainedMethod: boolean) {
        this.getFocusElement().focus();
    }

    public getScrollX(): number {
        return this.getNativeElement().scrollLeft;
    }

    public getScrollY(): number {
        return this.getNativeElement().scrollTop;
    }

    public setScroll(x: number, y: number) {
        this.getNativeElement().scrollLeft = x;
        this.getNativeElement().scrollTop = y;
    }

    public needsScrollbarInformation(): boolean {
        return true;
    }
}
