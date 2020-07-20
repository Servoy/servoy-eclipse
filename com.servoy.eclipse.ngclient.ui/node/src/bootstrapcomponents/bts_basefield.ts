import { ServoyBootstrapBaseComponent } from "./bts_basecomp";
import { Directive, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges, Renderer2 } from "@angular/core";

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

    update( val: string ) {
          this.dataProviderID = val;
          this.dataProviderIDChange.emit(this.dataProviderID);
    }
       
    ngOnChanges( changes: SimpleChanges ) {
        if (changes) {
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
                      if ( change.currentValue ) this.renderer.setAttribute(this.getNativeElement(),   'placeholder', change.currentValue );
                      else  this.renderer.removeAttribute(this.getNativeElement(),  'placeholder' );
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