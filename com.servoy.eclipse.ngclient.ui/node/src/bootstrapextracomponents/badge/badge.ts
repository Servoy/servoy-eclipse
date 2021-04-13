import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBaseComponent } from '@servoy/public';

@Component({
    selector: 'bootstrapextracomponents-badge',
    templateUrl: './badge.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyBootstrapExtraBadge extends ServoyBaseComponent<HTMLDivElement> {
    @Input() onAction: (e: Event, data?: any) => void;
    @Input() onRightClick: (e: Event, data?: any) => void;
    @Input() onDoubleClick: (e: Event, data?: any) => void;

    @Input() enabled: boolean;
    @Input() displayType: string;
    @Input() text: string;
    @Input() badgeText: string;
    @Input() size: { width: number; height: number };
    @Input() location: any;
    @Input() imageStyleClass: string;
    @Input() styleClass: string;
    @Input() visible: boolean;
    @Input() toolTipText: string;

    buttonClass = 'btn-outline-secondary';
    timeoutID: number;

    isTrustedHTML(): boolean {
        if (this.servoyApi.trustAsHtml()) {
            return true;
        }
        return false;
    }

    public getFocusElement(): HTMLElement {
        return this.getNativeElement();
    }

    svyOnInit() {
        super.svyOnInit();
        if (this.onAction) {
            if (this.onDoubleClick) {
                this.renderer.listen(this.getFocusElement(), 'click', e => {
                    if (this.timeoutID) {
                        window.clearTimeout(this.timeoutID);
                        this.timeoutID = null;
                        // double click, do nothing will be done in sub classes
                    } else {
                        this.timeoutID = window.setTimeout(() => {
                            this.timeoutID = null;
                            this.onAction(e);
                        }, 250);
                    }
                });
            } else {
                this.renderer.listen(this.getFocusElement(), 'click', e => this.onAction(e));
            }
        }
        if (this.onRightClick) {
            this.renderer.listen(this.getFocusElement(), 'contextmenu', e => {
                this.onRightClick(e); return false;
            });
        }
        if (this.onDoubleClick) {
            this.renderer.listen(this.elementRef.nativeElement, 'dblclick', (e) => {
                this.onDoubleClick(e);
            });
        }
    }

}