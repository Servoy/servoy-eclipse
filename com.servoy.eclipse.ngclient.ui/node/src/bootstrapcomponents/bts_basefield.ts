import { ServoyBootstrapBaseComponent } from "./bts_basecomp";
import { Directive, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges, Renderer2 } from "@angular/core";
import { PropertyUtils } from '../ngclient/servoy_public';

@Directive()
export class ServoyBootstrapBasefield extends ServoyBootstrapBaseComponent implements OnInit, OnChanges {
 
    @Input() onDataChangeMethodID;
    @Input() onFocusGainedMethodID;
    @Input() onFocusLostMethodID;

    @Output() dataProviderIDChange = new EventEmitter();
    @Input() dataProviderID;
    @Input() readOnly;
    @Input() editable;
    @Input() placeholderText;

    storedTooltip: any;

    constructor(renderer: Renderer2) {
        super(renderer);
    }

    ngOnInit() {
        super.ngOnInit();
        this.attachFocusListeners(this.getFocusElement());
        if (this.dataProviderID === undefined) {
            this.dataProviderID = null;
        }
    }
  
    attachFocusListeners(nativeElement: any) {
          if (this.onFocusGainedMethodID)
              this.renderer.listen( nativeElement, 'focus', ( e ) => {
                  this.onFocusGainedMethodID(e);
              } );
          if (this.onFocusLostMethodID)
              this.renderer.listen( nativeElement, 'blur', ( e ) => {
                  this.onFocusLostMethodID(e);
              } );
    }

    onDataChangeCallback(event, returnval) {
        var stringValue = (typeof returnval === 'string' || returnval instanceof String);
        if (returnval === false || stringValue) {
            this.renderer.removeClass(this.elementRef.nativeElement, 'ng-valid');
            this.renderer.addClass(this.elementRef.nativeElement, 'ng-invalid');
            if (stringValue) {
                if (this.storedTooltip === false) { 
                    this.storedTooltip = this.toolTipText; 
                }
                this.toolTipText = returnval;
            }
        }
        else {
            this.renderer.removeClass(this.elementRef.nativeElement, 'ng-invalid');
            this.renderer.addClass(this.elementRef.nativeElement, 'ng-valid');
            if (this.storedTooltip !== false) this.toolTipText = this.storedTooltip;
            this.storedTooltip = false;
        }
    }

    update( val: string ) {
          this.dataProviderID = val;
          this.dataProviderIDChange.emit(this.dataProviderID);
    }
       
    ngOnChanges( changes: SimpleChanges ) {
        if (changes && this.elementRef) {
          for ( const property of Object.keys(changes) ) {
              const change = changes[property];
              switch ( property ) {
                  case 'editable':
                      if ( change.currentValue )
                          this.renderer.removeAttribute(this.getFocusElement(),  'readonly' );
                      else
                          this.renderer.setAttribute(this.getFocusElement(),  'readonly', 'readonly' );
                      break;
                  case 'placeholderText':
                      if ( change.currentValue ) this.renderer.setAttribute(this.getFocusElement(),   'placeholder', change.currentValue );
                      else  this.renderer.removeAttribute(this.getFocusElement(),  'placeholder' );
                      break;
                  case 'selectOnEnter': 
                      if ( change.currentValue ) PropertyUtils.addSelectOnEnter(this.getFocusElement(), this.renderer);
                      break;    
                }
            }
            super.ngOnChanges(changes);
        }
    }

    public selectAll() {
        this.getNativeElement().select();
    }

    public getSelectedText(): string {
        return window.getSelection().toString();
    }

    public replaceSelectedText(text: string) {
        const elem = this.getNativeElement();
        const startPos =  elem.selectionStart;
        const endPos = elem.selectionEnd;

        const beginning = elem.value.substring(0, startPos);
        const end = elem.value.substring(endPos);
        elem.value = beginning + text + end;
        elem.selectionStart = startPos;
        elem.selectionEnd = startPos + text.length;

        const evt = document.createEvent('HTMLEvents');
        evt.initEvent('change', false, true);
        elem.dispatchEvent(evt);
    }

    public getAsPlainText(): string {
        if (this.dataProviderID) {
            return this.dataProviderID.replace(/<[^>]*>/g, '');
        }
        return this.dataProviderID;
    }
    
}