import { Component, ChangeDetectorRef, Renderer2, ViewChild, SimpleChanges, HostListener, ChangeDetectionStrategy, Inject } from '@angular/core';
import { Observable, merge, Subject, of } from 'rxjs';
import { ServoyDefaultBaseField } from '../basefield';
import { NgbTypeahead } from '@ng-bootstrap/ng-bootstrap';
import { FormattingService } from 'servoy-public';
import { debounceTime, distinctUntilChanged, filter, switchMap } from 'rxjs/operators';
import { DOCUMENT } from '@angular/common';

@Component({
    selector: 'servoydefault-typeahead',
    templateUrl: './typeahead.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyDefaultTypeahead extends ServoyDefaultBaseField<HTMLInputElement> {
    // this is a hack so that this can be none static access because this references in this component to a conditional template
    @ViewChild('instance', { static: true }) instance: NgbTypeahead;
    focus$ = new Subject<string>();
    click$ = new Subject<string>();

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef,
        formattingService: FormattingService, @Inject(DOCUMENT) doc: Document) {
        super(renderer, cdRef, formattingService, doc);
    }

    @HostListener('keydown', ['$event'])
    handleKeyDown(event: KeyboardEvent) {
        if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
            // stop propagation when using list form component (to not break the selection)
            event.stopPropagation();
        }
    }

    svyOnInit() {
        super.svyOnInit();
         this.renderer.listen( this.getFocusElement(), 'focus', () => {
            setTimeout(this.onFocus);
        });
    }

    onFocus = () => {
        const popup = this.doc.getElementById(this.instance.popupId);
        if (popup) {
            popup.style.width = this.getFocusElement().clientWidth + 'px';
        }
    };

    scroll() {
        if (!this.instance.isPopupOpen()) {
            return;
        }

        setTimeout(() => {
            const popup = this.doc.getElementById(this.instance.popupId);
            const activeElements = popup.getElementsByClassName('active');
            if (activeElements.length === 1) {
                const elem = activeElements[0] as HTMLElement;
                elem.scrollIntoView({
                    behavior: 'smooth',
                    block: 'center'
                });
            }
        });
    }

    svyOnChanges(changes: SimpleChanges) {
        super.svyOnChanges(changes);
        if (changes.readOnly || changes.enabled) {
            this.instance.setDisabledState(this.readOnly || !this.enabled);
        }
        if ((changes.format || changes.findmode) && this.valuelistID) {
            this.instance.writeValue(this.dataProviderID);
        }
    }

    values = (text$: Observable<string>) => {
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
        // eslint-disable-next-line eqeqeq
        if (result.displayValue === null || result.displayValue == '') return '\u00A0';
        return this.formattingService.format(result.displayValue, this.format, false);
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
        return this.formattingService.format(result, this.format, false);
    };

    valueChanged(value: { displayValue: string; realValue: any }) {
        if (value && value.realValue !== undefined) this.dataProviderID = value.realValue;
        else if (value) this.dataProviderID = value;
        else this.dataProviderID = null;
        this.dataProviderIDChange.emit(this.dataProviderID);
    }
}
