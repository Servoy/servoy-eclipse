import { Component, Renderer2, SimpleChanges, ChangeDetectorRef, ViewChild, Input, Output, EventEmitter, HostListener, ChangeDetectionStrategy, Inject } from '@angular/core';
import { Select2Option, Select2UpdateEvent, Select2 } from 'ng-select2-component';
import { ServoyBaseComponent } from '@servoy/public';
import { IValuelist } from '../../sablo/spectypes.service';
import { DOCUMENT } from '@angular/common';

@Component( {
    selector: 'servoyextra-select2tokenizer',
    templateUrl: './select2tokenizer.html',
    changeDetection: ChangeDetectionStrategy.OnPush
} )
export class ServoyExtraSelect2Tokenizer extends ServoyBaseComponent<HTMLDivElement> {

    @Input() onDataChangeMethodID: ( e: Event, data?: any ) => void;
    @Input() onFocusGainedMethodID: ( e: Event, data?: any ) => void;
    @Input() onFocusLostMethodID: ( e: Event, data?: any ) => void;

    @Output() dataProviderIDChange = new EventEmitter();
    @Input() placeholderText: string;
    @Input() readOnly: boolean;
    @Input() valuelistID: IValuelist;
    @Input() styleClass: string;
    @Input() tabSeq: number;
    @Input() toolTipText: string;
    @Input() dataProviderID: any;
    @Input() enabled: boolean;
    @Input() editable: boolean;

    @ViewChild( Select2 ) select2: Select2;

    data: Select2Option[] = [];
    filteredDataProviderId: any;
    listPosition: 'above' | 'below' = "below";

    constructor( renderer: Renderer2, cdRef: ChangeDetectorRef, @Inject( DOCUMENT ) private doc: Document ) {
        super( renderer, cdRef );
    }

    @HostListener( 'keydown', ['$event'] )
    handleKeyDown( event: KeyboardEvent ) {
        if ( event.key === 'ArrowUp' || event.key === 'ArrowDown' ) {
            // stop propagation when using list form component (to not break the selection)
            event.stopPropagation();
        }
    }

    svyOnInit() {
        super.svyOnInit();
        this.setData();
        this.attachFocusListeners();
        const position = this.getNativeElement().getBoundingClientRect();
        let availableHeight = this.doc.defaultView.innerHeight - position.top - position.height;
        let dropDownheight = this.valuelistID.length * 30;
        if ( dropDownheight > availableHeight ) {
            this.listPosition = 'above';
        }
    }

    attachFocusListeners() {
        if ( this.onFocusGainedMethodID ) {
            this.select2.focus.subscribe(() => {
                this.onFocusGainedMethodID( new CustomEvent( 'focus' ) );
            } );
        }

        if ( this.onFocusLostMethodID ) {
            this.select2.blur.subscribe(() => {
                this.onFocusLostMethodID( new CustomEvent( 'blur' ) );
            } );
        }
    }

    requestFocus() {
        this.select2.toggleOpenAndClose();
    }

    isEditable() {
        return this.readOnly === false && this.editable === true;
    }

    setData() {
        if ( this.valuelistID ) {
            const options: Select2Option[] = [];
            for ( const value of this.valuelistID ) {
                if ( value.realValue === null || value.realValue === '' ) {
                    continue;
                }
                options.push( {
                    value: value.realValue,
                    label: value.displayValue
                } );
            }
            this.data = options;
        }
    }

    onDataChangeCallback( event, returnval ) {
        const stringValue = ( typeof returnval === 'string' || returnval instanceof String );
        if ( returnval === false || stringValue ) {
            //this.renderer.removeClass( this.select2, 'ng-valid' );
            this.renderer.addClass( this.elementRef.nativeElement, 'ng-invalid' );
        } else {
            this.renderer.removeClass( this.elementRef.nativeElement, 'ng-invalid' );
            //this.renderer.addClass( this.select2, 'ng-valid' );
        }
    }    
    
    updateValue( event: Select2UpdateEvent<any> ) {
        if ( this.filteredDataProviderId !== event.value ) {
            this.filteredDataProviderId = event.value;
            this.dataProviderID = event.value.join( '\n' );
            this.dataProviderIDChange.emit( this.dataProviderID );
        }
    }

    svyOnChanges( changes: SimpleChanges ) {
        if ( changes['valuelistID'] ) {
            this.setData();
        }
        if ( changes['dataProviderID'] ) {
            this.filteredDataProviderId = ( typeof this.dataProviderID === 'string' ) ? this.dataProviderID.split( '\n' ) : [this.dataProviderID];
        }
        super.svyOnChanges( changes );
    }
}
