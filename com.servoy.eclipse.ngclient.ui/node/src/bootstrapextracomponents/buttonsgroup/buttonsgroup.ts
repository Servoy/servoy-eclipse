import { Component, Input, ChangeDetectorRef, Renderer2, Output, EventEmitter, SimpleChanges } from '@angular/core';
import { IValuelist, ServoyBaseComponent } from '@servoy/public';
import { Format } from '@servoy/public';

@Component({
    selector: 'bootstrapextracomponents-buttons-group',
    templateUrl: './buttonsgroup.html'
})
export class ServoyBootstrapExtraButtonsGroup extends ServoyBaseComponent<HTMLElement> {

    @Input() styleClass: string;
    @Input() valuelistID: IValuelist;
    @Input() showAs: string;
    @Input() enabled: boolean;
    @Input() tabSeq: number;
    @Input() inputType: string;
    @Input() toolTipText: string;
    @Input() format: Format;
    @Input() onDataChangeMethodID: (e: Event) => void;
    
    @Output() dataProviderIDChange = new EventEmitter();
    @Input() dataProviderID: any;

    selectedValues = {};
    oldValue: any;
    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

    svyOnChanges(changes: SimpleChanges) {
        for (const property of Object.keys(changes)) {
            const change = changes[property];
            switch (property) {
                case 'enabled':
                    if (change.currentValue)
                        this.renderer.removeAttribute(this.getNativeElement(), 'disabled');
                    else
                        this.renderer.setAttribute(this.getNativeElement(), 'disabled', 'disabled');
                    break;

                case 'dataProviderID':
                    this.updateSelectedValues(this.dataProviderID);
                    break;
                case 'valuelistID':
                    this.updateSelectedValues(this.dataProviderID);
                    break;

            }
        }
        super.svyOnChanges(changes);
    }

    updateSelectedValues(value: any) {
        var selectedValues = {};
        if (value || value == 0) {
            // store clientside selected values
            if (this.hasMultiSelection()) {
                let values = value.toString().split("\n");
                for (let i = 0; i < values.length; i++) {
                    selectedValues[values[i]] = true;
                }
            } else {    // store the selectedValue
                selectedValues[value] = true;
            }
        }
        this.selectedValues = selectedValues;
    }

    onClick(item) {
        // prevent click if is disabled
        if (this.enabled) {
            // keep the old value. Old value will be restored if onDataChange returns false.
            this.oldValue = this.dataProviderID;

            // allow deselection
            let newValue;
            let selectedValue = item.realValue;

            if (this.oldValue == selectedValue) {    // deselect last value
                if (this.allowEmptyValuelistItem(item) && this.oldValue == selectedValue) {   // deselect last option
                    newValue = null;
                    this.selectedValues = {};
                } else {    // cannot deselect last value
                    // Do nothing
                    newValue = this.oldValue;
                    return;
                }
            } else {    // select/deselect a value
                if (this.hasMultiSelection() && this.isTypeString()) {
                    if (this.selectedValues[selectedValue]) { // value is already selected;
                        // TODO remove it
                        delete this.selectedValues[selectedValue];
                        let values = this.dataProviderID.toString().split("\n");    // dataProviderID should be filled since there is a selectedValue
                        newValue = values.filter(function(value) {
                            return value != selectedValue
                        }).join("\n");
                    } else { // value was not selected;
                        this.selectedValues[selectedValue] = true;
                        if (this.oldValue !== null && this.oldValue !== undefined && this.oldValue !== "") {
                            newValue = this.oldValue + '\n' + selectedValue;
                        } else {
                            newValue = selectedValue;
                        }
                    }
                } else {
                    // update selection
                    newValue = selectedValue;
                    delete this.selectedValues[this.oldValue];
                    this.selectedValues[selectedValue] = true;
                }
            }

            this.dataProviderID = newValue;
            this.dataProviderIDChange.emit(this.dataProviderID);
        }
    }

    onDataChangeCallback(event, returnval) {

        if (returnval == false) { // restore the oldValue
            this.updateSelectedValues(this.oldValue);
            this.dataProviderID = this.oldValue;
            this.dataProviderIDChange.emit(this.dataProviderID);
        } else {
            this.oldValue = null;
        }
    }

    hasMultiSelection() {
        return this.inputType === 'checkbox';
    }

    allowEmptyValuelistItem(item) {
        if (this.valuelistID.length) {
            let item = this.valuelistID[0];
            return (item.realValue == null || item.realValue == '') && item.displayValue == '';
        }
        return false;
    }

    isTypeString() {
        return (!this.format && (this.dataProviderID === null || this.dataProviderID === undefined)) || (this.format && this.format.type === 'TEXT')
    }
}
