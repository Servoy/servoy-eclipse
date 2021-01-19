import { Component, Renderer2, SimpleChanges, ChangeDetectorRef, ViewChild, HostListener, QueryList, ElementRef, ViewChildren, ChangeDetectionStrategy } from '@angular/core';
import { NgbDropdownItem } from '@ng-bootstrap/ng-bootstrap';
import { FormattingService } from '../../ngclient/servoy_public';
import { ServoyDefaultBaseField } from '../basefield';

@Component({
    selector: 'servoydefault-combobox',
    templateUrl: './combobox.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyDefaultCombobox extends ServoyDefaultBaseField<HTMLInputElement> {

    @ViewChildren(NgbDropdownItem) menuItems: QueryList<NgbDropdownItem>;
    @ViewChild('input') input: ElementRef<HTMLButtonElement>;

    formattedValue: any;
    valueComparator: (value: { displayValue: any; realValue: any }) => boolean;
    openState = false;
    private skipFocus = false;

    constructor(renderer: Renderer2, protected cdRef: ChangeDetectorRef, private formatService: FormattingService) {
        super(renderer, cdRef, formatService);
    }

    @HostListener('keydown', ['$event'])
    handleKeyDown(event: KeyboardEvent) {
        if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
            // stop propagation when using list form component (to not break the selection)
            event.stopPropagation();
        }
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
            this.requestFocus(this.mustExecuteOnFocus);
        }
    }

    svyOnChanges(changes: SimpleChanges) {
        this.valueComparator = this.valuelistID.isRealValueDate()? this.dateValueCompare: this.valueCompare;
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
        delete changes['editable']; // ignore the editable property
        super.svyOnChanges(changes);
    }

    updateValue(realValue: any) {
        this.dataProviderID = realValue;
        this.dataProviderIDChange.emit(this.dataProviderID);
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

