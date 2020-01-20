import { Component, Renderer2, ViewChild, Input, ElementRef, ChangeDetectorRef, AfterViewInit, SimpleChanges } from '@angular/core';

import { FormattingService } from '../../ngclient/servoy_public'

import { ServoyDefaultBaseField } from '../basefield'

@Component( {
    selector: 'servoydefault-listbox',
    templateUrl: './listbox.html'
} )
export class ServoyDefaultListBox extends ServoyDefaultBaseField implements AfterViewInit {
    @Input() multiselectListbox;

    selectedValues: any[];

    @ViewChild( 'element', { static: false } ) elementRef: ElementRef;

    private changes: SimpleChanges;

    constructor( private changeDetectorRef: ChangeDetectorRef, renderer: Renderer2, formattingService: FormattingService ) {
        super( renderer, formattingService );
    }

    ngOnInit() {
        //this method should do nothing
    }

    ngAfterViewInit() {
        this.ngOnChanges( this.changes );
        this.changeDetectorRef.detectChanges();
        this.addAttributes();
        this.attachFocusListeners( this.getFocusElement() );
        this.attachHandlers();
    }

    ngOnChanges( changes: SimpleChanges ) {
        if ( !this.elementRef ) {
            if ( this.changes == null ) {
                this.changes = changes;
            }
            else {
                for ( let property in changes ) {
                    this.changes[property] = changes[property];
                }
            }
        }
        else {
            for ( let property in changes ) {
                let change = changes[property];
                switch ( property ) {
                    case "dataProviderID":
                        this.selectedValues = [];
                        if (this.multiselectListbox && this.dataProviderID) {
                            this.selectedValues = (''+this.dataProviderID).split( '\n' );
                        }
                        break;

                }
            }
            super.ngOnChanges( changes );
        }
    }

    multiUpdate() {
        for ( var i = 0; i < this.selectedValues.length; i += 1 ) {
            this.selectedValues[i] = '' + this.selectedValues[i];
        }
        this.update( this.selectedValues.join( '\n' ) );
    }
}
