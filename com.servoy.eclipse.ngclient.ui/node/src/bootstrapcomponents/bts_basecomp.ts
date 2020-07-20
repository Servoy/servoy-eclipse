
import { ServoyBaseComponent, PropertyUtils } from "../ngclient/servoy_public";
import { Directive, Input, Renderer2, SimpleChanges, AfterViewInit, OnChanges } from "@angular/core";

@Directive()
export class ServoyBootstrapBaseComponent extends ServoyBaseComponent implements AfterViewInit, OnChanges {
    
    @Input() onActionMethodID;
    @Input() onRightClickMethodID;
    @Input() onDoubleClickMethodID;

    @Input() enabled;
    @Input() size;
    @Input() styleClass;
    @Input() tabSeq;
    @Input() text;
    @Input() toolTipText;
    @Input() visible;
    
    timeoutID: number;
    
    constructor(protected readonly renderer: Renderer2) {
        super(renderer);
    }
    
    ngAfterViewInit() {
        super.ngAfterViewInit();
        super.ngOnInit();
        this.attachHandlers();
    }
    
    getFocusElement(): any {
        return this.getNativeElement();
    }

    public requestFocus() {
        this.getFocusElement().focus();
    }
    
    protected attachHandlers() {
        if ( this.onActionMethodID ) {
            if (this.onDoubleClickMethodID) {
                const innerThis: ServoyBootstrapBaseComponent = this;
                this.renderer.listen( this.getNativeElement(), 'click', e => {
                    if (innerThis.timeoutID) {
                        window.clearTimeout(innerThis.timeoutID);
                        innerThis.timeoutID = null;
                        // double click, do nothing
                    } else {
                        innerThis.timeoutID = window.setTimeout(function() {
                            innerThis.timeoutID = null;
                            innerThis.onActionMethodID( e );
                        }, 250); }
                 });
            } else {
                this.renderer.listen( this.getNativeElement(), 'click', e => this.onActionMethodID( e ));
            }
        }
        if ( this.onRightClickMethodID ) {
          this.renderer.listen( this.getNativeElement(), 'contextmenu', e => { this.onRightClickMethodID( e ); return false; });
        }
    }
    public getScrollX(): number {
        return this.getNativeElement().scrollLeft;
    }

    public getScrollY(): number {
        return this.getNativeElement().scrollTop;
    }

    public setScroll(x: number, y: number) {
        this.getNativeElement().scrollLeft = x;
        this.getNativeElement().scrollTop = y;
    }

    needsScrollbarInformation(): boolean {
        return true;
    }

    ngOnChanges( changes: SimpleChanges ) {
      if (changes) {
        for ( const property of Object.keys(changes) ) {
            const change = changes[property];
            switch ( property ) {
                case 'enabled':
                    if ( change.currentValue )
                        this.renderer.removeAttribute(this.getNativeElement(),  'disabled' );
                    else
                        this.renderer.setAttribute(this.getNativeElement(),  'disabled', 'disabled' );
                    break;
                case 'styleClass':
                    if (change.previousValue)
                        this.renderer.removeClass(this.getNativeElement(), change.previousValue );
                    if ( change.currentValue)
                        this.renderer.addClass( this.getNativeElement(), change.currentValue );
                    break;
                case 'visible':
                    PropertyUtils.setVisible( this.getNativeElement(), this.renderer , change.currentValue);
                    break;
            }
        }
      }
    }
    
}