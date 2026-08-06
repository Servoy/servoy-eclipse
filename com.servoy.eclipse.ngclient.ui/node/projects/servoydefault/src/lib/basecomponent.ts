import { SimpleChanges, Renderer2, ChangeDetectorRef, Directive, input, model } from '@angular/core';

import { ServoyBaseComponent, PropertyUtils, Format } from '@servoy/public';


@Directive()
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export class ServoyDefaultBaseComponent<T extends HTMLElement> extends ServoyBaseComponent<T> {

    readonly onActionMethodID = input<(e: Event, data?: any) => void>(undefined as any);
    readonly onRightClickMethodID = input<(e: Event, data?: any) => void>(undefined as any);
    readonly onDoubleClickMethodID = input<(e: Event, data?: any) => void>(undefined as any);

    readonly background = input<string>(undefined as any);
    readonly borderType = input<string>(undefined as any);
    readonly dataProviderID = model<any>(undefined);
    readonly displaysTags = input<boolean>(undefined as any);
    readonly enabled = input<boolean>(undefined as any);
    readonly fontType = input<string>(undefined as any);
    readonly foreground = input<string>(undefined as any);
    readonly format = input<Format>(undefined as any);
    readonly horizontalAlignment = input<any>(undefined);
    readonly margin = input<any>(undefined);
    readonly styleClass = input<string>(undefined as any);
    readonly tabSeq = input<number>(undefined as any);
    readonly text = input<string>(undefined as any);
    readonly toolTipText = model<string>(undefined as any);
    readonly transparent = input<boolean>(undefined as any);
    readonly scrollbars = input<any>(undefined);

    timeoutID: number | null = null;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
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
                        this.renderer.setStyle(this.getNativeChild(), 'backgroundColor', this.transparent() ? 'transparent' : change.currentValue);
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
        if (this.onRightClickMethodID()) {
            this.renderer.listen(this.getNativeElement(), 'contextmenu', e => {
                this.onRightClickMethodID()(e); return false;
            });
        }
    }

}
