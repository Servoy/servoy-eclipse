import { Component, OnInit, Input, Output, EventEmitter, OnChanges, SimpleChanges,Renderer2,ElementRef,ViewChild } from '@angular/core';

import {PropertyUtils, FormattingService} from '../../ngclient/servoy_public'

@Component( {
    selector: 'servoydefault-textfield',
    templateUrl: './textfield.html',
    providers: [FormattingService]
} )
export class ServoyDefaultTextField implements OnInit, OnChanges {
    @Input() name;
    @Input() servoyApi;

    @Input() onActionMethodID;
    @Input() onDataChangeMethodID;
    @Input() onFocusGainedMethodID;
    @Input() onFocusLostMethodID;
    @Input() onRightClickMethodID;

    @Input() background
    @Input() borderType
    @Input() dataProviderID
    @Output() dataProviderIDChange = new EventEmitter();
    @Input() displaysTags
    @Input() editable
    @Input() enabled
    @Input() findmode
    @Input() fontType
    @Input() foreground
    @Input() format
    @Input() horizontalAlignment
    @Input() location
    @Input() margin
    @Input() placeholderText
    @Input() readOnly
    @Input() selectOnEnter
    @Input() size
    @Input() styleClass
    @Input() tabSeq
    @Input() text
    @Input() toolTipText
    @Input() transparent
    @Input() valuelistID
    @Input() visible
    
    @ViewChild('element') elementRef:ElementRef;

    constructor(private readonly renderer: Renderer2, public formattingService : FormattingService) { }

    ngOnInit() {

    }

    ngOnChanges( changes: SimpleChanges ) {
        for ( let property in changes ) {
            let change = changes[property];
            switch ( property ) {
                case "borderType":
                    PropertyUtils.setBorder( this.elementRef.nativeElement,this.renderer ,change.currentValue);
                    break;
                case "background":
                case "transparent":
                    this.renderer.setStyle(this.elementRef.nativeElement, "backgroundColor", this.transparent ? "transparent" : change.currentValue );
                    break;
                case "foreground":
                    this.renderer.setStyle(this.elementRef.nativeElement, "color", change.currentValue );
                    break;
                case "fontType":
                    this.renderer.setStyle(this.elementRef.nativeElement, "font", change.currentValue );
                    break;
                case "format":
//                    if ( formatState )
//                        formatState( value );
//                    else formatState = $formatterUtils.createFormatState( $element, $scope, ngModel, true, value );
                    break;
                case "horizontalAlignment":
                    PropertyUtils.setHorizontalAlignment(  this.elementRef.nativeElement.nativeElement,this.renderer ,change.currentValue);
                    break;
                case "enabled":
                    if ( change.currentValue )
                        this.renderer.removeAttribute(this.elementRef.nativeElement,  "disabled" );
                    else
                        this.renderer.setAttribute(this.elementRef.nativeElement,  "disabled", "disabled" );
                    break;
                case "editable":
                    if ( change.currentValue )
                        this.renderer.removeAttribute(this.elementRef.nativeElement,  "readonly" );
                    else
                        this.renderer.setAttribute(this.elementRef.nativeElement,  "readonly", "readonly" );
                    break;
                case "placeholderText":
                    if ( change.currentValue ) this.renderer.setAttribute(this.elementRef.nativeElement,   'placeholder', change.currentValue );
                    else  this.renderer.removeAttribute(this.elementRef.nativeElement,  'placeholder' );
                    break;
                case "margin":
                    if ( change.currentValue ) {
                        for (let  style in change.currentValue) {
                            this.renderer.setStyle(this.elementRef.nativeElement, style, change.currentValue[style] );
                        }
                    }
                    break;
                case "selectOnEnter":
                    if ( change.currentValue ) PropertyUtils.addSelectOnEnter( this.elementRef.nativeElement, this.renderer );
                    break;
                case "styleClass":
                    if (change.previousValue)
                        this.renderer.removeClass(this.elementRef.nativeElement,change.previousValue );
                    if ( change.currentValue)
                        this.renderer.addClass( this.elementRef.nativeElement, change.currentValue );
                    break;
            }
        }
    }

    update( val: string ) {
        this.dataProviderID = this.formattingService.parse(val, this.format, this.dataProviderID);
        this.dataProviderIDChange.emit( this.dataProviderID );
    }

    myapicall( a1, a2, a3 ) {
        console.log( "api call" + a1 + "," + a2 + "," + a3 );
    }

}
