import { Component, ViewChild,OnInit, Input, Output, EventEmitter, Renderer2, ElementRef, HostListener } from '@angular/core';

import { FormattingService } from '../../ngclient/servoy_public'

import {ServoyDefaultBaseField} from '../basefield'

export interface Item {
    displayValue: any,
    realValue: any
};

@Component( {
    selector: 'servoydefault-combo',
    templateUrl: './combobox.html',
    styleUrls: ['./combobox.css']
} )
export class ServoyDefaultCombobox extends ServoyDefaultBaseField implements OnInit {
    @Input() state: any;
    @Output() onRightClickMethodID: EventEmitter<any> = new EventEmitter();
    @ViewChild('comboboxInput') comboboxInput: ElementRef;
    valueList: Array<Item>;
    filteredValueList: Array<Item>;
    selectedComboboxItem: Item;
    selectedComboboxItemIndex: number = 0;
    isTouched: boolean = false;
    showDropdown: boolean = false;
    isFocused: boolean = false;
    allowClear: boolean = false;
    spinnerEnabled: boolean = false;
    isGrouped: boolean = false;
    submitted: boolean = false;

    constructor(renderer: Renderer2,    
                formattingService : FormattingService,
                private el: ElementRef) { 
        super(renderer,formattingService);
    }

    ngOnInit() {
        this.valueList = this.servoyApi.getApiData();
        this.selectedComboboxItem = this.valueList[0];
        this.setInitialFilteredValueList();  
    }

    @HostListener('keyup', ['$event']) onInsideClick(event: KeyboardEvent) {
        const keyCode = event.keyCode;
        
        if (keyCode === 13 && !this.submitted && this.showDropdown) {
            this.showDropdown = false;
            this.submitted = true;
            
        }
        if (keyCode === 13 && this.submitted && !this.showDropdown) {
            this.showDropdown = false;
            this.isFocused = false;
            this.submitted = false;
        }
        
        if (keyCode !== 9 && this.isFocused && !this.showDropdown) {
            this.showDropdown = true;
        } 
        
        
        if ((keyCode !== 9 && keyCode !== 13 && keyCode !== 40 && keyCode !== 38) && this.showDropdown) {
            this.toggleInput();
            if (this.comboboxInput.nativeElement.value === '' && (keyCode >= 48 && keyCode <= 90)) {
                this.comboboxInput.nativeElement.value = event.key;
            }
        }

        this.onInputValue(event);

        // Down key
        if (keyCode === 40) {
            this.selectedComboboxItemIndex = (this.selectedComboboxItemIndex < (this.filteredValueList.length - 1)
             ? this.selectedComboboxItemIndex + 1 : 0);
            
             this.selectedComboboxItem = this.filteredValueList[this.selectedComboboxItemIndex];
             this.isTouched = true;
             // Up Key
            } else if (keyCode === 38) {
                this.selectedComboboxItemIndex = (this.selectedComboboxItemIndex === 0
                    ? (this.filteredValueList.length - 1) : this.selectedComboboxItemIndex - 1);
                    
            this.selectedComboboxItem = this.filteredValueList[this.selectedComboboxItemIndex];
            this.isTouched = true;
        }
    }

    @HostListener('document:click', ['$event']) onOutsideClick() {        
        if(this.el.nativeElement.contains(event.target)) {
            this.toggleInput();
        } else {            
            this.showDropdown = false;
            this.isFocused = false;
            this.clearSearchInput();
        }
    }

    onFocus() {
        this.isFocused = true;
    }

    onBlur() {
        this.isFocused = false;
        this.showDropdown = false;
    }

    setInitialFilteredValueList() {
        this.filteredValueList = this.valueList;
    }

    toggleInput() {
        this.comboboxInput.nativeElement.focus();   
        this.isFocused = true;    
        this.showDropdown = true;
    }

    selectValue(valueToBeSelected) {
        this.selectedComboboxItem = valueToBeSelected;
        this.isTouched = true;
        this.showDropdown = false;
    }

    isComboboxItemSelected(comboboxItem) {
        return this.selectedComboboxItem === comboboxItem;
    }

    filteredValueListHasValues() {
        return this.filteredValueList.length !== 0;
    }

    clearSearchInput(): void {
        this.comboboxInput.nativeElement.value = "";
        this.setInitialFilteredValueList();
    }

    isInputEmpty(): boolean {
        return this.comboboxInput.nativeElement.value === 0 && this.allowClear;
    }

    isSpinnerEnabled() {
        return this.spinnerEnabled;
    }

    onInputValue(event?) {
        if (event && event.keyCode === 13) {
            this.clearSearchInput();
        }
        
        this.filteredValueList = this.valueList.filter(d => {
            this.comboboxInput.nativeElement.focus();
            this.isFocused = true;
            
            if (d.displayValue) {
                if (event.target['value']) {
                    return d.displayValue.toLowerCase().indexOf(event.target['value'].toLowerCase()) != -1
                }
            }
            return {};
        });
    }
}

