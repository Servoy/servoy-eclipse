
import { ElementRef, SimpleChanges, Directive, input, OnInit, viewChild } from '@angular/core';
import { ServoyDefaultBaseField } from './basefield';

@Directive()
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export abstract class ServoyDefaultBaseChoice extends ServoyDefaultBaseField<HTMLDivElement> {

    readonly input = viewChild<ElementRef<HTMLInputElement>>('input');

    selection: any[] = [];
    allowNullinc = 0;

    svyOnInit() {
        super.svyOnInit();
    }

    requestFocus( mustExecuteOnFocusGainedMethod: boolean ) {
        this.mustExecuteOnFocus = mustExecuteOnFocusGainedMethod;
        ( this.getFocusElement() as HTMLElement ).focus();
    }

    getFocusElement(): HTMLElement {
        return this.input() != null ? this.input()!.nativeElement : null!;
    }

    svyOnChanges(changes: SimpleChanges) {
        for (const property of Object.keys(changes)) {
            switch (property) {
                case 'dataProviderID':
                    this.setSelectionFromDataprovider();
                    break;
                case 'valuelistID':
                    if (this.valuelistID() && this.valuelistID().length > 0 && this.isValueListNull(this.valuelistID()[0]))
                        this.allowNullinc = 1;
                    else this.allowNullinc = 0;
                    this.setSelectionFromDataprovider();
                    break;
            }
        }
        super.svyOnChanges(changes);
    }

    baseItemClicked(event: any, changed: any, dp: any) {
        if (event.target.localName === 'label' || event.target.localName === 'span') {
            event.preventDefault();
        }
        if (changed) {
            this.dataProviderID.set(dp);
            this.pushUpdate();
        }
        event.target.blur();
    }
    
    attachHandlers() {
        // just ignore this.
    }

    attachEventHandlers(element: any, index: any) {
        if (!element)
            element = this.getNativeElement();
        if (this.onFocusGainedMethodID()) {
            this.renderer.listen(element, 'focus', (event) => {
                if ( this.mustExecuteOnFocus !== false ) {
                    this.onFocusGainedMethodID()( event );
                }
                this.mustExecuteOnFocus = true;
            });
        }

        if (this.onFocusLostMethodID()) {
            this.renderer.listen(element, 'blur', (event) => {
                this.onFocusLostMethodID()(event);
            });
        }

        if (this.onRightClickMethodID()) {
            this.renderer.listen(element, 'contextmenu', (e) => {
                this.onRightClickMethodID()(e);
            });
        }
    }

    isValueListNull = (item: any) => (item.realValue == null || item.realValue === '') && item.displayValue === '';

    /**
     * Gets the selected values (real values from valuelist) as array. The form element should have a dataProviderID assigned in order for this to work.
     *
     * @example var values = %%prefix%%%%elementName%%.getSelectedElements();
     * @return array with selected values
     */
    getSelectedElements() {
        return this.selection
            .map((item, index) => {
                if (item === true) return this.valuelistID()[index + this.allowNullinc].realValue;
                return null;
            })
            .filter(item => item !== null);
    }

    abstract setSelectionFromDataprovider(): void;
}

@Directive({
    selector: '[svyBaseChoiceElement]',
    standalone: true
})
export class ChoiceElementDirective implements OnInit {

    readonly svyBaseChoiceElement = input<ServoyDefaultBaseChoice>(undefined as any);
    readonly index = input<number>(undefined as any);

    constructor(private el: ElementRef) {
    }
    
    ngOnInit(): void {
        this.svyBaseChoiceElement().attachEventHandlers(this.el.nativeElement, this.index());
    }
}
