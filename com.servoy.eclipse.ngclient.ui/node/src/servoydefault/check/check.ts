import { Renderer2, Component, OnInit, SimpleChanges, AfterViewInit, Input } from '@angular/core';
import { FormattingService } from "../../ngclient/servoy_public";
import { ServoyDefaultBaseChoice } from "../basechoice";

@Component( {
    selector: 'servoydefault-check',
    templateUrl: './check.html',
    styleUrls: ['./check.css']
} )
export class ServoyDefaultCheck extends ServoyDefaultBaseChoice {
    @Input() horizontalAlignment;

    selected: boolean = false;
    constructor( renderer: Renderer2, formattingService: FormattingService ) {
        super( renderer, formattingService );
    }

    ngAfterViewInit() {
        this.attachEventHandlers( this.getNativeElement(), 0 )
    }

    ngOnChanges( changes: SimpleChanges ) {
        this.setHorizontalAlignmentFlexbox( this.getNativeElement(), this.renderer, this.horizontalAlignment );
        super.ngOnChanges( changes );
    }

    public setHorizontalAlignmentFlexbox( element: any, renderer: Renderer2, halign ) {
        if ( halign != -1 ) {
            if ( halign == 0 ) {
                renderer.setStyle( element, 'justify-content', 'center' );
            }
            else if ( halign == 4 ) {
                renderer.setStyle( element, 'justify-content', 'flex-end' );
            }
            else {
                renderer.setStyle( element, 'justify-content', 'flex-start' );
            }
        }
    }

    attachEventHandlers( element, index ) {
        if ( !element )
            element = this.getNativeElement();
        this.renderer.listen( element, 'click', ( e ) => {
            this.itemClicked( e );
            if ( this.onActionMethodID ) this.onActionMethodID( e );
        } );
    }

    itemClicked( event ) {
        if ( event.target.localName === 'span' || event.target.localName === 'label' )
            this.selected = !this.selected;

        if ( this.valuelistID && this.valuelistID[0] )
            this.dataProviderID = this.dataProviderID == this.valuelistID[0].realValue ? null : this.valuelistID[0].realValue;
        else if ( typeof this.dataProviderID === "string" )
            this.dataProviderID = this.dataProviderID == "1" ? "0" : "1";
        else
            this.dataProviderID = this.dataProviderID > 0 ? 0 : 1;
        super.baseItemClicked( event, true, this.dataProviderID );
    }

    getSelectionFromDataprovider() {
        if ( !this.dataProviderID )
            return false;
        if ( this.valuelistID && this.valuelistID[0] ) {
            return this.dataProviderID == this.valuelistID[0].realValue;
        } else if ( typeof this.dataProviderID === "string" ) {
            return this.dataProviderID == "1";
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
