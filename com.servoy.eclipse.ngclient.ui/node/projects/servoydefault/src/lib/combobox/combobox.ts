import { DOCUMENT } from '@angular/common';
import { Component, Renderer2, SimpleChanges, ChangeDetectorRef, ViewChild, HostListener, QueryList, ElementRef, ViewChildren, ChangeDetectionStrategy, Inject } from '@angular/core';
import { NgbDropdownItem, NgbTooltip, NgbDropdown } from '@ng-bootstrap/ng-bootstrap';
import { FormattingService, ServoyPublicService } from '@servoy/public';
import { ServoyDefaultBaseField } from '../basefield';

@Component({
    selector: 'servoydefault-combobox',
    templateUrl: './combobox.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ServoyDefaultCombobox extends ServoyDefaultBaseField<HTMLInputElement> {

    @ViewChildren(NgbDropdownItem) menuItems: QueryList<NgbDropdownItem>;
    @ViewChild('input') input: ElementRef<HTMLButtonElement>;
    @ViewChild(NgbDropdown) comboboxDropdown: NgbDropdown;
    @ViewChild('tooltip') tooltip: NgbTooltip;

    formattedValue: any;
    valueComparator: (value: { displayValue: any; realValue: any }) => boolean;
    openState = false;
    keyboardSelectValue: string = null;
    lastSelectValue: string = null;
    firstItemFound = false;
    private skipFocus = false;
    private showPopupOnFocusGain = false;

    constructor(renderer: Renderer2, protected cdRef: ChangeDetectorRef, private formatService: FormattingService, @Inject(DOCUMENT) doc: Document, protected servoyService: ServoyPublicService) {
        super(renderer, cdRef, formatService, doc);
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
            if (document.activeElement === this.getFocusElement() && !this.comboboxDropdown.isOpen()) {
                this.comboboxDropdown.open();
            }
            if (event.key !== 'Backspace') this.keyboardSelectValue = (this.keyboardSelectValue ? this.keyboardSelectValue : '') + event.key;
            else this.keyboardSelectValue = this.keyboardSelectValue ? this.keyboardSelectValue.slice(0, -1) : '';
            this.lastSelectValue = this.keyboardSelectValue.slice();
            if (!this.lastSelectValue) this.closeTooltip();
            else this.refreshTooltip();

            this.cdRef.detectChanges();
            this.scrollToFirstMatchingItem();
        } else {
            if (this.keyboardSelectValue) this.lastSelectValue = this.keyboardSelectValue.slice();
            if (!this.lastSelectValue) this.closeTooltip();
            else this.refreshTooltip();

            this.cdRef.detectChanges();
            this.scrollToFirstMatchingItem();
        }
    }

    svyOnInit() {
        let showPopup = this.servoyApi.getClientProperty('Combobox.showPopupOnFocusGain');
        if (showPopup === null || showPopup === undefined) {
            showPopup = this.servoyService.getUIProperty('Combobox.showPopupOnFocusGain');
        }
        if (showPopup !== null && showPopup !== undefined) {
            this.showPopupOnFocusGain = showPopup;
        }
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
            'Alt', 'AltGraph', 'CapsLock', 'Fn', 'Meta', 'NumLock', 'ScrollLock', 'Command', 'Shift',
            'Enter', 'Tab', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'ArrowUp', 'End', 'Home',
            'PageUp', 'PageDown', 'Delete', 'Control', 'Insert', 'Del', 'Escape',
            'F1', 'F2', 'F3', 'F4', 'F5', 'F6', 'F7', 'F8', 'F9', 'F10', 'F11', 'F12', 'Dead',
            'AudioVolumeMute', 'AudioVolumeDown', 'AudioVolumeUp', 'LaunchApplication2', 'Unidentified', 'ContextMenu'
        ];
        if (nonPrintableValue.includes(key)) {
            const keysThatCloseTooltip = [
                'ArrowDown', 'ArrowLeft', 'ArrowRight', 'ArrowUp', 'End', 'Home',
                'PageUp', 'PageDown', 'Delete', 'Del', 'Tab'
            ];
            if (keysThatCloseTooltip.includes(key)) this.closeTooltip();
            return false;
        }
        return true;
    }

    getDropDownWidth() {
        return this.input?.nativeElement?.clientWidth;
    }

    getFocusElement() {
        return this.input.nativeElement;
    }

    attachFocusListeners(nativeElement: HTMLElement) {
        if (this.onFocusGainedMethodID || this.showPopupOnFocusGain)
            this.renderer.listen(nativeElement, 'focus', (e) => {
                if (this.onFocusGainedMethodID && !this.skipFocus && this.mustExecuteOnFocus) this.onFocusGainedMethodID(e);
                if (!this.skipFocus && this.showPopupOnFocusGain && !this.comboboxDropdown.isOpen()) {
                    this.comboboxDropdown.open();
                }
                this.skipFocus = false;
                this.mustExecuteOnFocus = true;
            });
        if (this.onFocusLostMethodID)
            this.renderer.listen(nativeElement, 'blur', (e) => {
                if (!this.openState) this.onFocusLostMethodID(e);
            });
    }

    requestFocus(mustExecuteOnFocusGainedMethod: boolean) {
        super.requestFocus(mustExecuteOnFocusGainedMethod);
        if (this.showPopupOnFocusGain) {
            this.comboboxDropdown.open();
        }
    }

    openChange(state: boolean) {
        this.openState = state;
        this.skipFocus = true;
        if (state) {
            setTimeout(() => {
                const item = this.menuItems.find((element) => element.nativeElement.classList.contains('active'));
                if (item) {
                    item.nativeElement.focus();
                }
            });
        } else {
            this.closeTooltip();
            const nativeElementBtn = this.elementRef.nativeElement.firstElementChild;
            if (this.doc.activeElement !== nativeElementBtn) {
                const event = new Event('blur');
                nativeElementBtn.dispatchEvent(event);
            }
            this.skipFocus = false;
        }
    }

    svyOnChanges(changes: SimpleChanges) {
        this.valueComparator = this.valuelistID && this.valuelistID.isRealValueDate() ? this.dateValueCompare : this.valueCompare;
        if (changes['dataProviderID'] && this.findmode) {
            this.formattedValue = this.dataProviderID;
        } else if ( (changes['dataProviderID'] || changes['valuelistID']) && this.valuelistID) {
            // eslint-disable-next-line eqeqeq
            const valueListElem = this.valuelistID.find(this.valueComparator);
            if (valueListElem) this.formattedValue = this.formatService.format(valueListElem.displayValue, this.format, false);
            else {
				if (!this.valuelistID.hasRealValues())
                	this.formattedValue = this.formatService.format(this.dataProviderID, this.format, false);
                else {
					this.formattedValue = null;
                	this.valuelistID.getDisplayValue(this.dataProviderID).subscribe(val => {
                    	this.formattedValue = val
                    	this.cdRef.detectChanges();
                	});
				}  
            }
        }
        else if (changes['dataProviderID'] && !this.valuelistID) {
            this.formattedValue = this.dataProviderID;
        }
        delete changes['editable']; // ignore the editable property
        if (this.formattedValue === "" || this.formattedValue === null || this.formattedValue === undefined) {
            if (changes['placeholderText']) {
                this.formattedValue = this.placeholderText;
            }
        }
        super.svyOnChanges(changes);
    }

    updateValue(realValue: any) {
        this.dataProviderID = realValue;
        this.dataProviderIDChange.emit(this.dataProviderID);
    }
    
    getRemainingValueBefore(value: any): any {
        let retValue = '';
        const valIndex = this.lastSelectValue ? value.toLowerCase().indexOf(this.lastSelectValue.toLowerCase()) : -1;
        if (this.openState && value && valIndex >= 0) {
            retValue = value.substring(0, valIndex);
        }
        return retValue;
    }
    
    getStrongValue(value: any): any {
        let retValue = '';
        const valIndex = this.lastSelectValue ? value.toLowerCase().indexOf(this.lastSelectValue.toLowerCase()) : -1;
        if (this.openState && value && valIndex >= 0) {
            retValue = value.substring(valIndex, (valIndex + this.lastSelectValue.length));
        }
        return retValue;
    }
    
    getRemainingValueAfter(value: any): any {
        let retValue = value;
        const valIndex = this.lastSelectValue ? value.toLowerCase().indexOf(this.lastSelectValue.toLowerCase()) : -1;
        if (this.openState && value && valIndex >= 0) {
            retValue = value.substring(valIndex + this.lastSelectValue.length);
        }
        return retValue;
    }

    scrollToFirstMatchingItem() {
        if (this.openState && this.lastSelectValue) {
            for (const item of this.menuItems) {
                if (item.nativeElement.innerText.toLowerCase().indexOf(this.lastSelectValue.toLowerCase()) >= 0 && !this.firstItemFound) {
                    this.firstItemFound = true;
                    item.nativeElement.focus();
                }
            }
        }
    }

    private closeTooltip() {
        this.keyboardSelectValue = null;
        this.lastSelectValue = null;
        this.tooltip.close();
    }

    // eslint-disable-next-line eqeqeq
    private valueCompare = (valueListValue: { displayValue: any; realValue: any }): boolean => valueListValue.realValue == this.dataProviderID;

    private dateValueCompare = (valueListValue: { displayValue: any; realValue: Date }): boolean => {
        if (this.dataProviderID && valueListValue.realValue) {
            return valueListValue.realValue.getTime() === this.dataProviderID.getTime();
        }
        return false;
    };
}

