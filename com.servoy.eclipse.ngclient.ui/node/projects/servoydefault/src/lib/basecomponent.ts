import { Input, SimpleChanges, Renderer2, ChangeDetectorRef, Directive } from '@angular/core';

import { ServoyBaseComponent, PropertyUtils, Format } from '@servoy/public';


@Directive()
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export class ServoyDefaultBaseComponent<T extends HTMLElement> extends ServoyBaseComponent<T> {

    @Input() onActionMethodID: (e: Event, data?: any) => void;
    @Input() onRightClickMethodID: (e: Event, data?: any) => void;
    @Input() onDoubleClickMethodID: (e: Event, data?: any) => void;

    @Input() background: string;
    @Input() borderType: string;
    @Input() dataProviderID: any;
    @Input() displaysTags: boolean;
    @Input() enabled: boolean;
    @Input() fontType: string;
    @Input() foreground: string;
    @Input() format: Format;
    @Input() horizontalAlignment;
    @Input() margin;
    @Input() styleClass: string;
    @Input() tabSeq: number;
    @Input() text: string;
    @Input() toolTipText: string;
    @Input() transparent: boolean;
    @Input() scrollbars;

    timeoutID: number;

    constructor(protected readonly renderer: Renderer2, protected cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

    svyOnInit() {
        super.svyOnInit();
        this.attachHandlers();
    }

    getFocusElement(): HTMLElement {
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

    needsScrollbarInformation(): boolean {
        return true;
    }

    svyOnChanges(changes: SimpleChanges) {
        if (changes) {
            for (const property of Object.keys(changes)) {
                const change = changes[property];
                switch (property) {
                    case 'borderType':
                        PropertyUtils.setBorder(this.getNativeChild(), this.renderer, change.currentValue);
                        break;
                    case 'background':
                    case 'transparent':
                        this.renderer.setStyle(this.getNativeChild(), 'backgroundColor', this.transparent ? 'transparent' : change.currentValue);
                        break;
                    case 'foreground':
                        this.renderer.setStyle(this.getNativeChild(), 'color', change.currentValue);
                        break;
                    case 'fontType':
                        PropertyUtils.setFont(this.getNativeChild(), this.renderer, change.currentValue);
                        break;
                    case 'horizontalAlignment':
                        PropertyUtils.setHorizontalAlignment(this.getNativeChild(), this.renderer, change.currentValue);
                        break;
                    case 'scrollbars':
                        if (this.needsScrollbarInformation()) {
                            PropertyUtils.setScrollbars(this.getNativeChild(), this.renderer, change.currentValue);
                        }
                        break;
                    case 'enabled':
                        if (change.currentValue)
                            this.renderer.removeAttribute(this.getFocusElement(), 'disabled');
                        else
                            this.renderer.setAttribute(this.getFocusElement(), 'disabled', 'disabled');
                        break;
                    case 'margin':
                        if (change.currentValue) {
                            for (const style of Object.keys(change.currentValue)) {
                                this.renderer.setStyle(this.getNativeElement(), style, change.currentValue[style]);
                            }
                        }
                        break;
                    case 'styleClass':
                        if (change.previousValue) {
                            const array = change.previousValue.trim().split(' ');
                            array.filter((element: string) => element !== '').forEach((element: string) => this.renderer.removeClass(this.getNativeChild(), element));
                        }
                        if (change.currentValue) {
                            const array = change.currentValue.trim().split(' ');
                            array.filter((element: string) => element !== '').forEach((element: string) => this.renderer.addClass(this.getNativeChild(), element));
                        }
                        break;
                }
            }
        }
        super.svyOnChanges(changes);
    }

    protected attachHandlers() {
        if (this.onRightClickMethodID) {
            this.renderer.listen(this.getNativeElement(), 'contextmenu', e => {
                this.onRightClickMethodID(e); return false;
            });
        }
    }

}
