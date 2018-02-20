import { Component, Directive,ViewChild,QueryList,OnInit, Input, Output, EventEmitter, Renderer2, ElementRef, OnChanges,AfterViewInit,SimpleChanges } from '@angular/core';

import {PropertyUtils} from '../../servoyapi/utils/property_utils'

@Component( {
    selector: 'servoydefault-label',
    templateUrl: './label.html'
} )
export class ServoyDefaultLabel implements OnInit, OnChanges,AfterViewInit {
    @Input() name;
    @Input() servoyApi;

    @Input() onActionMethodID;
    @Input() onDoubleClickMethodID;
    @Input() onRightClickMethodID;

    @Input() background;
    @Input() borderType;
    @Input() dataProviderID;
    @Input() displaysTags;
    @Input() enabled;
    @Input() fontType;
    @Input() foreground;
    @Input() format;
    @Input() hideText;
    @Input() horizontalAlignment;
    @Input() imageMediaID;
    @Input() labelFor; 
    @Input() location;
    @Input() margin;
    @Input() mediaOptions;
    @Input() mnemonic;
    @Input() rolloverCursor;
    @Input() rolloverImageMediaID;
    @Input() showFocus;
    @Input() size;
    @Input() styleClass;
    @Input() tabSeq;
    @Input() text;
    @Input() textRotation;
    @Input() toolTipText;
    @Input() transparent;
    @Input() verticalAlignment;
    @Input() visible;
    
    @ViewChild('child') child:ElementRef;
    @ViewChild('element') elementRef:ElementRef;
    
    private changes: SimpleChanges;

    constructor(private readonly renderer: Renderer2) {
    }
    ngAfterViewInit() {
        console.log(this.child)
        console.log(this.elementRef)
        this.ngOnChanges(this.changes);
    }

    ngOnChanges( changes: SimpleChanges ) {
        if (!this.elementRef) {
            if (this.changes == null) {
                this.changes = changes;
            }
            else {
                for ( let property in changes ) {
                    this.changes[property] = changes[property];
                }
            }
        }
        else  for ( let property in changes ) {
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
                case "rolloverCursor":
                    this.renderer.setStyle(this.elementRef.nativeElement,  'cursor', change.currentValue == 12 ? 'pointer' : 'default' );
                    break;
                case "mnemonic":
                    if ( change.currentValue ) this.renderer.setAttribute(this.elementRef.nativeElement,   'accesskey', change.currentValue );
                    else  this.renderer.removeAttribute(this.elementRef.nativeElement,  'accesskey' );
                    break;
                case "horizontalAlignment":
                    PropertyUtils.setHorizontalAlignment( this.child.nativeElement,this.renderer ,change.currentValue);
                    break;
                case "enabled":
                    if ( change.currentValue )
                        this.renderer.removeAttribute(this.elementRef.nativeElement,  "disabled" );
                    else
                        this.renderer.setAttribute(this.elementRef.nativeElement,  "disabled", "disabled" );
                    break;
                case "margin":
                    if ( change.currentValue ) {
                        for (let  style in change.currentValue) {
                            this.renderer.setStyle(this.elementRef.nativeElement, style, change.currentValue[style] );
                        }
                    }
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
    
    ngOnInit() {
        if ( this.onActionMethodID ) {
            this.renderer.listen( this.elementRef.nativeElement, 'click', ( e ) => {
                this.onActionMethodID( e );
            } );
        }
        if ( this.onDoubleClickMethodID ) {
            this.renderer.listen( this.elementRef.nativeElement, 'dblclick', ( e ) => {
                this.onDoubleClickMethodID( e );
            } );
        }
        if ( this.onRightClickMethodID ) {
            this.renderer.listen( this.elementRef.nativeElement, 'contextmenu', ( e ) => {
                this.onRightClickMethodID( e );
            } );
        }
    }
}

