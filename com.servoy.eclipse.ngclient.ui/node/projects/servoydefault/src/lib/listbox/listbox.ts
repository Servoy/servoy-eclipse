
import { Component, Renderer2, Input, ChangeDetectorRef, SimpleChanges, ChangeDetectionStrategy, Inject, DOCUMENT } from '@angular/core';

import { FormattingService } from '@servoy/public';

import { ServoyDefaultBaseField } from '../basefield';

@Component( {
    selector: 'servoydefault-listbox',
    templateUrl: './listbox.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
} )
export class ServoyDefaultListBox extends ServoyDefaultBaseField<HTMLSelectElement> {
    @Input() multiselectListbox;

    selectedValues: any[];

    selection: any[] = [];
    allowNullinc = 0;

    constructor( changeDetectorRef: ChangeDetectorRef, renderer: Renderer2, formattingService: FormattingService, @Inject(DOCUMENT) doc: Document ) {
        super( renderer, changeDetectorRef, formattingService, doc );
    }

    svyOnChanges( changes: SimpleChanges ) {
        for ( const property of Object.keys(changes) ) {
            switch ( property ) {
            case 'dataProviderID':
                this.selectedValues = [];
                if (this.multiselectListbox && this.dataProviderID) {
                    this.selectedValues = ('' + this.dataProviderID).split( '\n' );
                } else if (!this.multiselectListbox && this.dataProviderID) {
                    this.selectedValues = [this.dataProviderID];
                }
                this.setSelectionFromDataprovider();
                break;

            }
        }
        super.svyOnChanges( changes );
    }
    
    svyOnInit() {
        super.svyOnInit();
        this.onValuelistChange();
    }
    
    onValuelistChange() {
        if (this.valuelistID)
            if (this.valuelistID.length > 0 && this.isValueListNull(this.valuelistID[0])) this.allowNullinc = 1;
    }
    
    isValueListNull = (item) => (item.realValue == null || item.realValue === '') && item.displayValue === '';

    
    attachHandlers() {
        if (this.onActionMethodID) this.renderer.listen( this.getNativeElement(), 'click', e => this.onActionMethodID( e ));
        super.attachHandlers();
      }

    multiUpdate() {
        for ( let i = 0; i < this.selectedValues.length; i += 1 ) {
            this.selectedValues[i] = '' + this.selectedValues[i];
        }
        this.dataProviderID = this.selectedValues.join( '\n' );
        this.pushUpdate();
    }
    
    setSelectionFromDataprovider() {
        this.selection = [];
        if (this.dataProviderID === null || this.dataProviderID === undefined) return;
        const arr = (typeof this.dataProviderID === 'string') ? this.dataProviderID.split('\n') : [this.dataProviderID];
        arr.forEach((element, index, array) => {
            for (let i = 0; i < this.valuelistID.length; i++) {
                const item = this.valuelistID[i];
                if (item.realValue + '' === element + '' && !this.isValueListNull(item)) this.selection[i - this.allowNullinc] = true;
            }
        });
    }
    
    getSelectedElements() {
        return this.selection
            .map((item, index) => {
                if (item == true) return this.valuelistID[index + this.allowNullinc].realValue;
            })
            .filter(item => item !== null);
    }

}
