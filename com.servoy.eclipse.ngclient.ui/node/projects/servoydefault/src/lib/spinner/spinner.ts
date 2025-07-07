import { Component, Renderer2, SimpleChanges, ChangeDetectorRef, ElementRef, ViewChild, ChangeDetectionStrategy, Inject } from '@angular/core';
import { ServoyDefaultBaseField } from '../basefield';
import { FormattingService } from '@servoy/public';
import { DOCUMENT } from '@angular/common';

@Component({
    selector: 'servoydefault-spinner',
    templateUrl: './spinner.html',
    styleUrls: ['./spinner.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ServoyDefaultSpinner extends ServoyDefaultBaseField<HTMLDivElement> {

    @ViewChild('child', { static: false }) child: ElementRef<HTMLInputElement>;

    selection: any;
    private counter = 0;
    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, formattingService: FormattingService, @Inject(DOCUMENT) doc: Document) {
        super(renderer, cdRef, formattingService, doc);
    }

    svyOnInit() {
        this.selection = this.getSelectionFromDataprovider();
        this.addHandlersToInputAndSpinnerButtons();
        super.svyOnInit();
    }

    requestFocus( mustExecuteOnFocusGainedMethod: boolean ) {
        this.mustExecuteOnFocus = mustExecuteOnFocusGainedMethod;
        ( this.getFocusElement() as HTMLElement ).focus();
    }

    getFocusElement(): HTMLElement {
        return this.child.nativeElement;
    }

    svyOnChanges(changes: SimpleChanges) {
        for (const property of Object.keys(changes)) {
            switch (property) {
                case 'dataProviderID':
                    this.selection = this.getSelectionFromDataprovider();
                    break;
            }
        }
        super.svyOnChanges(changes);
    }

    addHandlersToInputAndSpinnerButtons() {
        const spinnerButtons = this.getNativeElement().getElementsByTagName('button');

        this.renderer.listen(this.getNativeChild(), 'scroll', e => this.scrollCallback(e));
        this.renderer.listen(this.getNativeChild(), 'keydown keypress', e => this.keydownKeypressCallback(e));

        this.renderer.listen(spinnerButtons[0], 'click', e => this.increment());
        this.renderer.listen(spinnerButtons[1], 'click', e => this.decrement());

        for (const i of Object.keys(spinnerButtons)) {
            if (this.onActionMethodID)
                this.renderer.listen(spinnerButtons[i], 'click', e => this.onActionMethodID(e));

            if (this.onFocusLostMethodID)
                this.renderer.listen(spinnerButtons[i], 'blur', e => this.onFocusLostMethodID(e));

            if (this.onFocusGainedMethodID)
                this.renderer.listen(spinnerButtons[i], 'focus', ( e ) => {
                    if(this.mustExecuteOnFocus !== false) {
                        this.onFocusGainedMethodID(e);
                    }
                    this.mustExecuteOnFocus = true;
                } );
        }
    }

    // copied from angularui timepicker
    isScrollingUp(e): boolean {
        if (e.originalEvent) {
            e = e.originalEvent;
        }
        // pick correct delta variable depending on event
        const delta = (e.wheelDelta) ? e.wheelDelta : -e.deltaY;
        return (e.detail || delta > 0);
    }

    scrollCallback(e) {
        if (!this.isDisabled()) {
            if (this.isScrollingUp(e))this.increment();
            else this.decrement();
        }
        e.preventDefault();
    }

    keydownKeypressCallback(e) {
        if (!this.isDisabled()) {
            if (e.which === 40)
                this.decrement();
            if (e.which === 38)
                this.increment();
        }
    }

    isDisabled() {
        return this.enabled === false || this.editable === false;
    }

    increment() {
        if (this.valuelistID) {
            this.counter = this.counter < this.valuelistID.length - 1 ? this.counter + 1 : 0;
            this.dataProviderID = this.valuelistID[this.counter].realValue;
        }
        this.pushUpdate();
    }

    decrement() {
        if (this.valuelistID) {
            this.counter = this.counter > 0 ? this.counter - 1 : this.valuelistID.length - 1;
            this.dataProviderID = this.valuelistID[this.counter].realValue;
        }
        this.pushUpdate();
    }

    getSelectionFromDataprovider() {
        if (!this.dataProviderID) {
            this.counter = 0;
            return undefined;
        }

        for (let i = 0; i < this.valuelistID.length; i++) {
            const item = this.valuelistID[i];
            if (item && item.realValue && this.dataProviderID === item.realValue) {
                let displayFormat;
                let type;
                if (this.format && this.format.display)
                    displayFormat = this.format.display;
                if (this.format && this.format.type)
                    type = this.format.type;
                this.counter = i;
                return item.displayValue;
            }
        }
    }

}

