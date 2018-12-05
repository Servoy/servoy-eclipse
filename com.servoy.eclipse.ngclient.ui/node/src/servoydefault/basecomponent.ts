import {OnInit, Input, OnChanges, SimpleChanges,Renderer2,ElementRef,ViewChild } from '@angular/core';

import {PropertyUtils} from '../ngclient/servoy_public'
import {ComponentContributor} from '../ngclient/component_contributor.service';

export class ServoyDefaultBaseComponent implements OnInit, OnChanges {
    @Input() name;
    @Input() servoyApi;
    @Input() servoyAttributes;

    @Input() onActionMethodID;
    @Input() onRightClickMethodID;

    @Input() background;
    @Input() borderType;
    @Input() dataProviderID;
    @Input() displaysTags;
    @Input() enabled;
    @Input() fontType;
    @Input() foreground;
    @Input() format;
    @Input() horizontalAlignment;
    @Input() location;
    @Input() margin;
    @Input() size;
    @Input() styleClass;
    @Input() tabSeq;
    @Input() text;
    @Input() toolTipText;
    @Input() transparent;
    @Input() visible;
    @Input() scrollbars;
    
    @ViewChild('element') elementRef:ElementRef;
    private componentContributor:ComponentContributor;

    constructor(protected readonly renderer: Renderer2) { 
        this.componentContributor = new ComponentContributor();
    }

    ngOnInit() {
      this.attachHandlers(); 
      this.addAttributes(); 
      this.componentContributor.componentCreated(this.getNativeChild(), this.renderer);
    }

    protected attachHandlers(){
      if ( this.onActionMethodID ) {
        this.renderer.listen( this.getNativeElement(), 'click', ( e ) => {
          this.onActionMethodID( e );
        } );
      }
      if ( this.onRightClickMethodID ) {
        this.renderer.listen( this.getNativeElement(), 'contextmenu', ( e ) => {
          this.onRightClickMethodID( e );
        } );
      }
    }
    
    protected addAttributes() {
        if (!this.servoyAttributes) return;
        this.servoyAttributes.forEach( attribute => this.renderer.setAttribute(this.getNativeElement(), attribute.key,  attribute.value));
    }

    ngOnChanges( changes: SimpleChanges ) {
        for ( let property in changes ) {
            let change = changes[property];
            switch ( property ) {
                case "borderType":
                    PropertyUtils.setBorder( this.getNativeElement(),this.renderer ,change.currentValue);
                    break;
                case "background":
                case "transparent":
                    this.renderer.setStyle(this.getNativeElement(), "backgroundColor", this.transparent ? "transparent" : change.currentValue );
                    break;
                case "foreground":
                    this.renderer.setStyle(this.getNativeElement(), "color", change.currentValue );
                    break;
                case "fontType":
                    this.renderer.setStyle(this.getNativeElement(), "font", change.currentValue );
                    break;
                case "format":
//                    if ( formatState )
//                        formatState( value );
//                    else formatState = $formatterUtils.createFormatState( $element, $scope, ngModel, true, value );
                    break;
                case "horizontalAlignment":
                    PropertyUtils.setHorizontalAlignment(  this.getNativeChild(),this.renderer ,change.currentValue);
                    break;
                case "scrollbars":
                    PropertyUtils.setScrollbars(this.getNativeChild(), change.currentValue);
                    break;
                case "enabled":
                    if ( change.currentValue )
                        this.renderer.removeAttribute(this.getNativeElement(),  "disabled" );
                    else
                        this.renderer.setAttribute(this.getNativeElement(),  "disabled", "disabled" );
                    break;
                case "margin":
                    if ( change.currentValue ) {
                        for (let  style in change.currentValue) {
                            this.renderer.setStyle(this.getNativeElement(), style, change.currentValue[style] );
                        }
                    }
                    break;
                case "styleClass":
                    if (change.previousValue)
                        this.renderer.removeClass(this.getNativeElement(),change.previousValue );
                    if ( change.currentValue)
                        this.renderer.addClass( this.getNativeElement(), change.currentValue );
                    break;
            }
        }
    }
    
    /**
     * this should return the main native element (like the first div) 
     */
    protected getNativeElement():any {
        return this.elementRef.nativeElement;
    }

   /**
    * sub classes can return a different native child then the default main element.
    * used currently only for horizontal aligment
    */
    protected getNativeChild():any {
        return this.elementRef.nativeElement;
    }
}
