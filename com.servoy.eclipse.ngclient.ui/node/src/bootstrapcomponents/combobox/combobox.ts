import { Component, Renderer2, Input, SimpleChanges, ChangeDetectorRef, ViewChild, ViewChildren, QueryList, ElementRef, HostListener, ChangeDetectionStrategy, Inject } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';
import { IValuelist } from '../../sablo/spectypes.service';
import { Format, FormattingService } from '../../ngclient/servoy_public';
import { NgbDropdownItem, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { DOCUMENT } from '@angular/common';

@Component({
    selector: 'bootstrapcomponents-combobox',
    templateUrl: './combobox.html',
    styleUrls: ['./combobox.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyBootstrapCombobox extends ServoyBootstrapBasefield<HTMLDivElement> {

    @Input() format: Format;
    @Input() showAs: string;
    @Input() valuelistID: IValuelist;
    @Input() appendToBody: boolean;

    @ViewChildren(NgbDropdownItem) menuItems: QueryList<NgbDropdownItem>;
    @ViewChild('input') input: ElementRef<HTMLButtonElement>;
    @ViewChild('tooltip') tooltip: NgbTooltip;

    formattedValue: any;
    valueComparator: (value: { displayValue: any; realValue: any }) => boolean;
    openState = false;
    keyboardSelectValue: string = null;
    lastSelectValue: string = null;
    firstItemFound = false;
    private kbSelection = null;
    private skipFocus = false;

    constructor(renderer: Renderer2, protected cdRef: ChangeDetectorRef, private formatService: FormattingService, @Inject(DOCUMENT) doc: Document) {
        super(renderer, cdRef, doc);
    }

    @HostListener('keydown', ['$event'])
    handleKeyDown(event: KeyboardEvent) {
        if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
            // stop propagation when using list form component (to not break the selection)
            event.stopPropagation();
        }
        this.lastSelectValue = null;
        this.firstItemFound = false;
        if (this.isPrintableChar(event.key)) {
            clearTimeout(this.kbSelection);
            this.keyboardSelectValue = (this.keyboardSelectValue ? this.keyboardSelectValue : '') + event.key;
            this.lastSelectValue = this.keyboardSelectValue.slice();
            this.refreshTooltip();
            this.cdRef.detectChanges();
            this.kbSelection = setTimeout(() => {
                this.scrollToFirstMatchingItem();
                this.keyboardSelectValue = null;
                this.lastSelectValue = null;
                this.tooltip.close();
            }, 300);
        }
    }

    svyOnInit() {
        super.svyOnInit();
        this.tooltip.autoClose = false;
    }

    refreshTooltip() {
        if (!this.tooltip.isOpen()) {
            this.tooltip.open();
        }
    }

    handleTooltip(event: KeyboardEvent) {
        this.tooltip.autoClose = false;
        this.tooltip.ngbTooltip = 'This is the CHANGED text';
        if (this.tooltip.isOpen()) {
            this.tooltip.close();
        } else {
            this.tooltip.open();
        }
    }

    isPrintableChar(key: string): boolean {
        const nonPrintableValue = [
            'Alt', 'AltGraph', 'CapsLock', 'Fn', 'Meta', 'NumLock', 'ScrollLock', 'Shift',
            'Enter', 'Tab', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'ArrowUp', 'End', 'Home',
            'PageUp', 'PageDown', 'Backspace', 'Delete', 'Insert', 'Del', 'Escape'
        ];
        if (nonPrintableValue.includes(key))
            return false;
        return true;
    }

    getDropDownWidth() {
        return this.input?.nativeElement?.clientWidth;
    }

    getFocusElement() {
        return this.input.nativeElement;
    }

    attachFocusListeners(nativeElement: HTMLElement) {
        if (this.onFocusGainedMethodID)
            this.renderer.listen(nativeElement, 'focus', (e) => {
                if (!this.skipFocus && this.mustExecuteOnFocus) this.onFocusGainedMethodID(e);
                this.skipFocus = false;
                this.mustExecuteOnFocus = true;
            });
        if (this.onFocusLostMethodID)
            this.renderer.listen(nativeElement, 'blur', (e) => {
                if (!this.openState) this.onFocusLostMethodID(e);
            });
    }

    openChange(state: boolean) {
        this.openState = state;
        if (state) {
            this.skipFocus = true;
            setTimeout(() => {
                const item = this.menuItems.find((element) => element.elementRef.nativeElement.classList.contains('active'));
                if (item) {
                    item.elementRef.nativeElement.focus();
                }
            });
        } else {
            this.lastSelectValue = null;
            this.requestFocus(this.mustExecuteOnFocus);
        }
    }

    svyOnChanges(changes: SimpleChanges) {
        this.valueComparator =  this.valuelistID && this.valuelistID.isRealValueDate()? this.dateValueCompare: this.valueCompare;
        if (changes['dataProviderID']) {
            // eslint-disable-next-line eqeqeq
            const valueListElem = this.valuelistID.find(this.valueComparator);
            if (valueListElem) this.formattedValue = this.formatService.format(valueListElem.displayValue, this.format, false);
            else {
                if (!this.valuelistID.hasRealValues())
                    this.formattedValue = this.formatService.format(this.dataProviderID, this.format, false);
                else
                    this.formattedValue = this.dataProviderID;
            }
        }
        super.svyOnChanges(changes);
    }

    updateValue(realValue: any) {
        this.dataProviderID = realValue;
        this.dataProviderIDChange.emit(this.dataProviderID);
    }

    getStrongValue(value: any): any {
        let retValue = '';
        if (this.openState && this.lastSelectValue && value && value.startsWith(this.lastSelectValue)) {
            retValue = this.lastSelectValue;
        }
        return retValue;
    }

    getRemainingValue(value: any): any {
        let retValue = value;
        if (this.openState && this.lastSelectValue && value && value.startsWith(this.lastSelectValue)) {
            retValue = value.substring(this.lastSelectValue.length);
        }
        return retValue;
    }

    scrollToFirstMatchingItem() {
        if (this.openState && this.lastSelectValue) {
            for (const item of this.menuItems) {
                if (item.elementRef.nativeElement.innerText.startsWith(this.lastSelectValue) && !this.firstItemFound) {
                    this.firstItemFound = true;
                    item.elementRef.nativeElement.focus();
                }
            }
       }
    }

    // eslint-disable-next-line eqeqeq
    private valueCompare = (valueListValue: { displayValue: any; realValue: any }): boolean  => valueListValue.realValue == this.dataProviderID;

    private dateValueCompare = (valueListValue: { displayValue: any; realValue: Date }): boolean => {
        if (this.dataProviderID){
            return valueListValue.realValue.getTime() === this.dataProviderID.getTime();
        }
        return false;
    };
}
