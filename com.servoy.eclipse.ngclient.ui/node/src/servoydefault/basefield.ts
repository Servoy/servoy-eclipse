import { Input, Output, EventEmitter, SimpleChanges, Renderer2, Directive, OnChanges, ChangeDetectorRef } from '@angular/core';

import { PropertyUtils, FormattingService } from '../ngclient/servoy_public';

import { ServoyDefaultBaseComponent } from './basecomponent';

import { IValuelist } from '../sablo/spectypes.service';

@Directive()
// eslint-disable-next-line
export class ServoyDefaultBaseField extends ServoyDefaultBaseComponent {

    @Input() onDataChangeMethodID: (e: Event, data?: any) => void;
    @Input() onFocusGainedMethodID: (e: Event, data?: any) => void;
    @Input() onFocusLostMethodID: (e: Event, data?: any) => void;

    @Output() dataProviderIDChange = new EventEmitter();
    @Input() editable: boolean;
    @Input() findmode: boolean;
    @Input() placeholderText: string;
    @Input() readOnly: boolean;
    @Input() selectOnEnter: boolean;
    @Input() valuelistID: IValuelist;

    storedTooltip: any;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, public formattingService: FormattingService) {
        super(renderer, cdRef);
    }

    svyOnInit() {
        super.svyOnInit();
        if (this.dataProviderID === undefined) {
            this.dataProviderID = null;
        }
    }

    attachFocusListeners(nativeElement: any) {
        if (this.onFocusGainedMethodID)
            this.renderer.listen(nativeElement, 'focus', (e) => {
                this.onFocusGainedMethodID(e);
            });
        if (this.onFocusLostMethodID)
            this.renderer.listen(nativeElement, 'blur', (e) => {
                this.onFocusLostMethodID(e);
            });
    }

    onDataChangeCallback(event, returnval) {
        const stringValue = (typeof returnval === 'string' || returnval instanceof String);
        if (returnval === false || stringValue) {
            this.renderer.removeClass(this.elementRef.nativeElement, 'ng-valid');
            this.renderer.addClass(this.elementRef.nativeElement, 'ng-invalid');
            if (stringValue) {
                if (this.storedTooltip === false) {
                    this.storedTooltip = this.toolTipText;
                }
                this.toolTipText = returnval;
            }
        } else {
            this.renderer.removeClass(this.elementRef.nativeElement, 'ng-invalid');
            this.renderer.addClass(this.elementRef.nativeElement, 'ng-valid');
            if (this.storedTooltip !== false) this.toolTipText = this.storedTooltip;
            this.storedTooltip = false;
        }
    }

    svyOnChanges(changes: SimpleChanges) {
        if (changes) {
            for (const property of Object.keys(changes)) {
                const change = changes[property];
                switch (property) {
                    case 'editable':
                        if (change.currentValue)
                            this.renderer.removeAttribute(this.getFocusElement(), 'readonly');
                        else
                            this.renderer.setAttribute(this.getFocusElement(), 'readonly', 'readonly');
                        break;
                    case 'placeholderText':
                        if (change.currentValue) this.renderer.setAttribute(this.getNativeElement(), 'placeholder', change.currentValue);
                        else this.renderer.removeAttribute(this.getNativeElement(), 'placeholder');
                        break;
                    case 'selectOnEnter':
                        if (change.currentValue) PropertyUtils.addSelectOnEnter(this.getFocusElement(), this.renderer);
                        break;
                }
            }
        }
        super.svyOnChanges(changes);
    }

    pushUpdate() {
        this.dataProviderIDChange.emit(this.dataProviderID);
    }

    public selectAll() {
        this.getNativeElement().select();
    }

    public getSelectedText(): string {
        return window.getSelection().toString();
    }

    public replaceSelectedText(text: string) {
        const elem = this.getNativeElement();
        const startPos = elem.selectionStart;
        const endPos = elem.selectionEnd;

        const beginning = elem.value.substring(0, startPos);
        const end = elem.value.substring(endPos);
        elem.value = beginning + text + end;
        elem.selectionStart = startPos;
        elem.selectionEnd = startPos + text.length;

        const evt = document.createEvent('HTMLEvents');
        evt.initEvent('change', false, true);
        elem.dispatchEvent(evt);
    }

    public getAsPlainText(): string {
        if (this.dataProviderID) {
            return this.dataProviderID.replace(/<[^>]*>/g, '');
        }
        return this.dataProviderID;
    }


    protected attachHandlers() {
        super.attachHandlers();
        this.attachFocusListeners(this.getFocusElement());
        if (this.onActionMethodID) {
            this.renderer.listen(this.getFocusElement(), 'keyup', (e) => {
                if (this.formattingService.testKeyPressed(e, 13)) {
                    this.onActionMethodID(e);
                }
            });
        }
    }

}
