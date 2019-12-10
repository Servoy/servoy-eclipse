import {
  OnInit,
  Input,
  Output,
  EventEmitter,
  SimpleChanges,
  Renderer2,
} from '@angular/core';

import {PropertyUtils, FormattingService} from '../ngclient/servoy_public'

import {ServoyDefaultBaseComponent} from './basecomponent'

export class ServoyDefaultBaseField extends  ServoyDefaultBaseComponent implements OnInit{

    @Input() onDataChangeMethodID;
    @Input() onFocusGainedMethodID;
    @Input() onFocusLostMethodID;

    @Output() dataProviderIDChange = new EventEmitter();
    @Input() editable;
    @Input() findmode;
    @Input() placeholderText;
    @Input() readOnly;
    @Input() selectOnEnter;
    @Input() valuelistID;

    valueBeforeChange: any;

    constructor(renderer: Renderer2, public formattingService : FormattingService) {
        super(renderer);
    }

    ngOnInit() {
      super.ngOnInit();
      this.attachFocusListeners(this.getFocusElement());
    }
    
    attachFocusListeners(nativeElement : any){
        if(this.onFocusGainedMethodID)
            this.renderer.listen( nativeElement, 'focus', ( e ) => {
                this.onFocusGainedMethodID(e);
                this.valueBeforeChange = nativeElement.value;
            } );
        if(this.onFocusLostMethodID)
            this.renderer.listen( nativeElement, 'blur', ( e ) => {
                this.onFocusLostMethodID(e);
                this.valueBeforeChange = nativeElement.value;
            } ); 
    }

    ngOnChanges( changes: SimpleChanges ) {
        for ( let property in changes ) {
            let change = changes[property];
            switch ( property ) {
                case "editable":
                    if ( change.currentValue )
                        this.renderer.removeAttribute(this.getFocusElement(),  "readonly" );
                    else
                        this.renderer.setAttribute(this.getFocusElement(),  "readonly", "readonly" );
                    break;
                case "placeholderText":
                    if ( change.currentValue ) this.renderer.setAttribute(this.getNativeElement(),   'placeholder', change.currentValue );
                    else  this.renderer.removeAttribute(this.getNativeElement(),  'placeholder' );
                    break;
                case "selectOnEnter":
                    if ( change.currentValue ) PropertyUtils.addSelectOnEnter( this.getFocusElement(), this.renderer );
                    break;
                case "enabled":
                    if ( change.currentValue )
                        this.renderer.removeAttribute(this.getFocusElement(),  "disabled" );
                    else
                        this.renderer.setAttribute(this.getFocusElement(),  "disabled", "disabled" );
                    break;

            }
        }
        super.ngOnChanges(changes);
    }

    update( val: string ) {
        if(!this.findmode && this.format) {
            var newDataProviderID = this.formattingService.parse(val, this.format, this.dataProviderID);
            if(this.dataProviderID == newDataProviderID) {
                this.getNativeElement().value = this.valueBeforeChange;
            }
            else {
                this.dataProviderID = newDataProviderID;
            }
        }
        else this.dataProviderID = val;
        this.dataProviderIDChange.emit( this.dataProviderID );
    }
    
    public selectAll() {
        this.getNativeElement().select();
    }
    
    public getSelectedText() : string{
        return window.getSelection().toString();
    }
    
    public replaceSelectedText(text: string){
        let elem = this.getNativeElement();
        var startPos =  elem.selectionStart;
        var endPos = elem.selectionEnd;
        
        var beginning = elem.value.substring(0, startPos);
        var end = elem.value.substring(endPos);
        elem.value = beginning + text + end;
        elem.selectionStart = startPos;
        elem.selectionEnd = startPos + text.length;
        
        var evt = document.createEvent("HTMLEvents");
        evt.initEvent("change", false, true);
        elem.dispatchEvent(evt);
    }
    
    public getAsPlainText() : string {
        if (this.dataProviderID)
        {
            return this.dataProviderID.replace(/<[^>]*>/g, ''); 
        }    
        return this.dataProviderID;
    }
}
