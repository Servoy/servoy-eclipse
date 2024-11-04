import { Component, ChangeDetectorRef, Renderer2, ViewChild, SimpleChanges, HostListener, ChangeDetectionStrategy, Inject } from '@angular/core';
import { Observable, merge, Subject, of } from 'rxjs';
import { ServoyDefaultBaseField } from '../basefield';
import { NgbTypeahead } from '@ng-bootstrap/ng-bootstrap';
import { FormattingService, ServoyPublicService } from '@servoy/public';
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

	currentValue: any;
	showPopupOnFocusGain: boolean;

	constructor(renderer: Renderer2, cdRef: ChangeDetectorRef,
		formattingService: FormattingService, @Inject(DOCUMENT) doc: Document, protected servoyService: ServoyPublicService) {
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
		this.renderer.listen(this.getFocusElement(), 'focus', () => {
			setTimeout(this.onFocus);
		});
		// add custom class to the popup, needed by ng-grids (ag-grid) so it can be used in form editors (popups)
		this.instance.popupClass = 'ag-custom-component-popup';
		this.showPopupOnFocusGain = this.servoyApi.getClientProperty('TypeAhead.showPopupOnFocusGain');
		if (this.showPopupOnFocusGain === null || this.showPopupOnFocusGain === undefined) {
			this.showPopupOnFocusGain = this.servoyService.getUIProperty('TypeAhead.showPopupOnFocusGain');
		}
	}

	onFocus = () => {
		const popup = this.doc.getElementById(this.instance.popupId);
		if (popup) {
			popup.style.width = this.getFocusElement().clientWidth + 'px';
		}
	};

	focusGained() {
		if (((this.showPopupOnFocusGain || this.showPopupOnFocusGain === null || this.showPopupOnFocusGain === undefined) && this.editable && !this.readOnly) || this.findmode) {
			this.focus$.next('');
		}
	}
	onClick() {
		if (((this.showPopupOnFocusGain || this.showPopupOnFocusGain === null || this.showPopupOnFocusGain === undefined) && this.editable && !this.readOnly) || this.findmode) {
			this.click$.next('');
		}
	}

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
		if (changes.enabled || changes.findmode) {
			this.instance.setDisabledState(!this.enabled && !this.findmode);
		}
		if (changes.format || changes.findmode) {
			if (this.format && this.format.maxLength) {
				if (!this.findmode) {
					this.renderer.setAttribute(this.elementRef.nativeElement, 'maxlength', this.format.maxLength + '');
				} else {
					this.renderer.removeAttribute(this.elementRef.nativeElement, 'maxlength');
				}
			}
			if (this.valuelistID) this.instance.writeValue(this.dataProviderID);
		}
		if (changes.dataProviderID) {
			this.currentValue = changes.dataProviderID.currentValue;
		}
	}

	values = (text$: Observable<string>) => {
		const debouncedText$ = text$.pipe(debounceTime(200), distinctUntilChanged());
		const clicksWithClosedPopup$ = this.click$.pipe(filter(() => !this.instance.isPopupOpen()));
		const inputFocus$ = this.focus$;

		return merge(debouncedText$, inputFocus$, clicksWithClosedPopup$).pipe(switchMap(term => (this.valuelistID.filterList(term))));
	};

	pushUpdate() {
		if (!this.dataProviderID && (!this.isEditable() || this.findmode)) {
			if (this.findmode || !this.valuelistID) {
				this.dataProviderID = this.elementRef.nativeElement.value;
			} else {
				const allowEmptyValue = this.valuelistID[0]?.displayValue === '' && this.valuelistID[0]?.realValue === null;
				if (!allowEmptyValue) {
					if (this.valuelistID[0]?.displayValue && this.valuelistID[0]?.realValue && this.elementRef.nativeElement.value === this.valuelistID[0]?.displayValue) {
						this.dataProviderID = this.valuelistID[0]?.realValue;
						this.currentValue = this.dataProviderID;
					} else {
						this.dataProviderID = this.currentValue;
					}
					return;
				}
			}
		}
		this.currentValue = this.dataProviderID;
		super.pushUpdate();
	}

	isEditable() {
		return this.valuelistID && !this.valuelistID.hasRealValues();
	}

	resultFormatter = (result: { displayValue: string; realValue: any }) => {
		// eslint-disable-next-line eqeqeq
		if (result.displayValue === null || result.displayValue == '') return '\u00A0';
		return this.formattingService.format(result.displayValue, this.format, false);
	};

	private realToDisplay: Map<any, string> = new Map();
	inputFormatter = (result: any) => {
		if (result === null) return '';
		if (result.displayValue !== undefined) result = result.displayValue;
		else if (this.valuelistID?.hasRealValues()) {
			// on purpose test with == so that "2" equals to 2
			// eslint-disable-next-line eqeqeq
			const value = this.valuelistID.find((item) => {
				if (item.realValue == result) {
					return true;
				}
				if (item.realValue instanceof Date && result instanceof Date) {
					return item.realValue.getTime() === result.getTime()
				}
				return false;
			});
			if (value) {
				result = value.displayValue;
			} else {
				let display = this.realToDisplay.get(result);
				if (display === null || display === undefined) {
					this.valuelistID.getDisplayValue(result).subscribe(val => {
						if (val) {
							this.realToDisplay.set(result, val);
							this.instance.writeValue(result);
						}
					});
					display = this.realToDisplay.get(result); // in case the getDisplayValue above runs sync, before this return happen (uses of() not from())
					if (display === null || display === undefined) return '';
					else result = display;
				} else {
					result = display;
				}
			}
		}
		return this.formattingService.format(result, this.format, false);
	};

	valueChanged(value: { displayValue: string; realValue: any }) {
		if (value && value.realValue !== undefined) this.dataProviderID = value.realValue;
		else if (value) this.dataProviderID = value;
		else this.dataProviderID = null;
		this.dataProviderIDChange.emit(this.dataProviderID);
		this.currentValue = this.dataProviderID;
	}
}
