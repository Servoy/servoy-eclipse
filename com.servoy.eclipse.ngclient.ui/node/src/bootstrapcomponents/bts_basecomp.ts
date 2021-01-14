import { ServoyBaseComponent } from '../ngclient/servoy_public';
import { Directive, Input, Renderer2, SimpleChanges, ChangeDetectorRef } from '@angular/core';

@Directive()
// eslint-disable-next-line
export class ServoyBootstrapBaseComponent<T extends HTMLElement> extends ServoyBaseComponent<T> {

    @Input() onActionMethodID: (e: Event, data?: any) => void;
    @Input() onRightClickMethodID: (e: Event, data?: any) => void;
    @Input() onDoubleClickMethodID: (e: Event, data?: any) => void;

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

    svyOnInit() {
        super.svyOnInit();
        this.attachHandlers();
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
                            const array = change.previousValue.split(' ');
                            array.forEach(element => this.renderer.removeClass(this.getStyleClassElement(), element));
                        }
                        if (change.currentValue) {
                            const array = change.currentValue.split(' ');
                            array.forEach(element => this.renderer.addClass(this.getStyleClassElement(), element));
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

    protected attachHandlers() {
        if (this.onActionMethodID) {
            if (this.onDoubleClickMethodID) {
                this.renderer.listen(this.getFocusElement(), 'click', e => {
                    if (this.timeoutID) {
                        window.clearTimeout(this.timeoutID);
                        this.timeoutID = null;
                        // double click, do nothing
                    } else {
                        this.timeoutID = window.setTimeout(() => {
                            this.timeoutID = null;
                            this.onActionMethodID(e);
                        }, 250);
                    }
                });
            } else {
                if (this.getFocusElement().tagName === 'TEXTAREA' /* || this.getNativeElement().type === 'text' */) {
                    this.renderer.listen(this.getFocusElement(), 'keydown', e => {
                        if (e.keyCode === 13) this.onActionMethodID(e);
                    });
                } else {
                    this.renderer.listen(this.getFocusElement(), 'click', e => this.onActionMethodID(e));
                }

            }
        }
        if (this.onRightClickMethodID) {
            this.renderer.listen(this.getFocusElement(), 'contextmenu', e => {
                this.onRightClickMethodID(e); return false;
            });
        }
    }

}
