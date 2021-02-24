import { DOCUMENT } from '@angular/common';
import { Renderer2, Component, ViewChild, ElementRef, ChangeDetectorRef, SimpleChanges, Input, ChangeDetectionStrategy, Inject } from '@angular/core';
import { FormattingService } from '../../ngclient/servoy_public';
import { ServoyDefaultBaseField } from '../basefield';

@Component( {
    selector: 'servoydefault-radio',
    templateUrl: './radio.html',
    styleUrls: ['./radio.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
} )
export class ServoyDefaultRadio extends ServoyDefaultBaseField<HTMLInputElement> {
    @Input() horizontalAlignment: any;

    @ViewChild('input', { static: false }) input: ElementRef<HTMLInputElement>;

    selected = false;
    constructor( renderer: Renderer2, cdRef: ChangeDetectorRef, formattingService: FormattingService, @Inject(DOCUMENT) doc: Document ) {
        super( renderer, cdRef, formattingService, doc );
    }

    svyOnChanges( changes: SimpleChanges ) {
        this.setHorizontalAlignmentFlexbox( this.getNativeElement(), this.renderer, this.horizontalAlignment );
        super.svyOnChanges( changes );
    }

    getFocusElement() {
        return this.input.nativeElement;
    }

    public setHorizontalAlignmentFlexbox( element: any, renderer: Renderer2, halign: any ) {
        if ( halign != -1 ) {
            if ( halign == 0 ) {
                renderer.setStyle( element, 'justify-content', 'center' );
            } else if ( halign == 4 ) {
                renderer.setStyle( element, 'justify-content', 'flex-end' );
            } else {
                renderer.setStyle( element, 'justify-content', 'flex-start' );
            }
        }
    }

    attachHandlers() {
        console.log("attaching");
        console.log(this.getFocusElement());
        this.renderer.listen( this.getFocusElement(), 'click', (e) => {
            console.log(e);
            if (!this.readOnly && this.enabled) {
                this.itemClicked(e);
                if (this.onActionMethodID) this.onActionMethodID(e);
            }
        });
        super.attachHandlers();
    }

    itemClicked( event: any ) {
        if ( event.target.localName === 'span' || event.target.localName === 'label' )
            this.selected = !this.selected;

        if ( this.valuelistID && this.valuelistID[0] )
            this.dataProviderID = this.dataProviderID == this.valuelistID[0].realValue ? null : this.valuelistID[0].realValue;
        else if ( typeof this.dataProviderID === 'string' )
            this.dataProviderID = this.dataProviderID == '1' ? '0' : '1';
        else
            this.dataProviderID = this.dataProviderID > 0 ? 0 : 1;
        this.pushUpdate();
    }

    getSelectionFromDataprovider() {
        if ( !this.dataProviderID )
            return false;
        if ( this.valuelistID && this.valuelistID[0] ) {
            return this.dataProviderID == this.valuelistID[0].realValue;
        } else if ( typeof this.dataProviderID === 'string' ) {
            return this.dataProviderID == '1';
        } else {
            return this.dataProviderID > 0;
        }
    }

    setSelectionFromDataprovider() {
        this.selected = this.getSelectionFromDataprovider();
    }

    needsScrollbarInformation(): boolean {
        return false;
    }
}
