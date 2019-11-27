import {OnInit, Input, OnChanges, SimpleChanges,Renderer2,ElementRef,ViewChild } from '@angular/core';

import {PropertyUtils, ServoyBaseComponent } from '../ngclient/servoy_public'


export class ServoyDefaultBaseComponent extends ServoyBaseComponent implements OnInit, OnChanges {

    @Input() onActionMethodID;
    @Input() onRightClickMethodID;
    @Input() onDoubleClickMethodID;
    
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
    
    timeoutID:number;
    
    constructor(protected readonly renderer: Renderer2) { 
        super(renderer);
    }

    ngOnInit() {
      super.ngOnInit();
      this.attachHandlers(); 
    }

    protected attachHandlers(){
      if ( this.onActionMethodID ) {
          if (this.onDoubleClickMethodID){
              let innerThis : ServoyDefaultBaseComponent = this;
              this.renderer.listen( this.getNativeElement(), 'click', e => {
                  if(innerThis.timeoutID){
                      window.clearTimeout(innerThis.timeoutID);
                      innerThis.timeoutID=null;
                      //double click, do nothing
                  }
                  else{
                      innerThis.timeoutID=window.setTimeout(function(){
                          innerThis.timeoutID=null;
                          innerThis.onActionMethodID( e );
                      },250)}
               });
          }
          else {
              this.renderer.listen( this.getNativeElement(), 'click', e => this.onActionMethodID( e ));
          }    
      }
      if ( this.onRightClickMethodID ) {
        this.renderer.listen( this.getNativeElement(), 'contextmenu', e => { this.onRightClickMethodID( e ); return false; });
      }
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
                    PropertyUtils.setFont( this.getNativeElement(),this.renderer ,change.currentValue);
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
                case "visible":
                    PropertyUtils.setVisible( this.getNativeElement(),this.renderer ,change.currentValue);
                    break;
            }
        }
    }
}
