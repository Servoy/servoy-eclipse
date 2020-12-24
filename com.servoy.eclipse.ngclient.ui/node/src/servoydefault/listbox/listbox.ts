import { Component, Renderer2, ViewChild, Input, ElementRef, ChangeDetectorRef, AfterViewInit, SimpleChanges } from '@angular/core';

import { FormattingService } from '../../ngclient/servoy_public';

import { ServoyDefaultBaseField } from '../basefield';

@Component( {
    selector: 'servoydefault-listbox',
    templateUrl: './listbox.html'
} )
export class ServoyDefaultListBox extends ServoyDefaultBaseField<HTMLSelectElement> {
    @Input() multiselectListbox;

    selectedValues: any[];

    constructor( changeDetectorRef: ChangeDetectorRef, renderer: Renderer2, formattingService: FormattingService ) {
        super( renderer, changeDetectorRef, formattingService );
    }

    svyOnChanges( changes: SimpleChanges ) {
        for ( const property of Object.keys(changes) ) {
            switch ( property ) {
            case 'dataProviderID':
                this.selectedValues = [];
                if (this.multiselectListbox && this.dataProviderID) {
                    this.selectedValues = ('' + this.dataProviderID).split( '\n' );
                }
                break;

            }
        }
        super.svyOnChanges( changes );
    }

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
}
