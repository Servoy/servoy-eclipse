import { ChangeDetectorRef, Component, Input, Renderer2, SimpleChanges, ViewChild } from '@angular/core';
import { NgbTypeahead } from '@ng-bootstrap/ng-bootstrap';
import { merge, Observable, of, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, switchMap } from 'rxjs/operators';
import { Format, FormattingService } from '../../ngclient/servoy_public';
import { IValuelist } from '../../sablo/spectypes.service';
import { ServoyBootstrapBasefield } from '../bts_basefield';

@Component({
    selector: 'bootstrapcomponents-typeahead',
    templateUrl: './typeahead.html',
    styleUrls: ['./typeahead.scss']
})
export class ServoyBootstrapTypeahead extends ServoyBootstrapBasefield {

    @Input() format: Format;
    @Input() valuelistID: IValuelist;

    @ViewChild('instance') instance: NgbTypeahead;
    focus$ = new Subject<string>();
    click$ = new Subject<string>();

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, private formatService: FormattingService) {
        super(renderer, cdRef);
    }

    svyOnChanges(changes: SimpleChanges) {
        super.svyOnChanges(changes);
        if (changes.readOnly || changes.enabled) {
            this.instance.setDisabledState(this.readOnly || !this.enabled);
        }
    }

    filterValues = (text$: Observable<string>) => {
        const debouncedText$ = text$.pipe(debounceTime(200), distinctUntilChanged());
        const clicksWithClosedPopup$ = this.click$.pipe(filter(() => !this.instance.isPopupOpen()));
        const inputFocus$ = this.focus$;

        return merge(debouncedText$, inputFocus$, clicksWithClosedPopup$).pipe(switchMap(term => (term === '' ? of(this.valuelistID)
            : this.valuelistID.filterList(term))));
    };

    isEditable() {
        return this.valuelistID && !this.valuelistID.hasRealValues();
    }

    resultFormatter = (result: { displayValue: string; realValue: any }) => {
        if (result.displayValue === null) return '';
        return this.formatService.format(result.displayValue, this.format, false);
    };

    inputFormatter = (result: any) => {
        if (result === null) return '';
        if (result.displayValue !== undefined) result = result.displayValue;
        else if (this.valuelistID.hasRealValues()) {
            // on purpose test with == so that "2" equals to 2
            // eslint-disable-next-line eqeqeq
            const value = this.valuelistID.find((item) => item.realValue == result);
            if (value) {
                result = value.displayValue;
            }
        }
        return this.formatService.format(result, this.format, false);
    };

    valueChanged(value: { displayValue: string; realValue: any }) {
        if (value && value.realValue) this.dataProviderID = value.realValue;
        else if (value) this.dataProviderID = value;
        else this.dataProviderID = null;
        this.dataProviderIDChange.emit(this.dataProviderID);
    }
}
