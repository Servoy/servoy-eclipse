import { Component, Renderer2, ViewChild, Input, ElementRef, ChangeDetectorRef, AfterViewInit, SimpleChanges } from '@angular/core';

import { FormattingService } from '../../ngclient/servoy_public'

import { ServoyDefaultBaseField } from '../basefield'

@Component( {
    selector: 'servoydefault-listbox',
    templateUrl: './listbox.html'
} )
export class ServoyDefaultListBox extends ServoyDefaultBaseField{
    @Input() multiselectListbox;

    selectedValues: any[];

    constructor( changeDetectorRef: ChangeDetectorRef, renderer: Renderer2, formattingService: FormattingService ) {
        super( renderer, changeDetectorRef, formattingService );
    }

    svyOnChanges( changes: SimpleChanges ) {
        for ( let property in changes ) {
            switch ( property ) {
            case "dataProviderID":
                this.selectedValues = [];
                if (this.multiselectListbox && this.dataProviderID) {
                    this.selectedValues = (''+this.dataProviderID).split( '\n' );
                }
                break;
                
            }
        }
        super.svyOnChanges( changes );
    }

    multiUpdate() {
        for ( var i = 0; i < this.selectedValues.length; i += 1 ) {
            this.selectedValues[i] = '' + this.selectedValues[i];
        }
        this.update( this.selectedValues.join( '\n' ) );
    }
}
