import { OnInit, Input, Output, EventEmitter, SimpleChanges, Renderer2, Directive, OnChanges } from '@angular/core';

import {PropertyUtils, FormattingService} from '../ngclient/servoy_public';

import {ServoyDefaultBaseComponent} from './basecomponent';

import {IValuelist} from '../sablo/spectypes.service';

@Directive()
export class ServoyDefaultBaseField extends  ServoyDefaultBaseComponent implements OnInit, OnChanges {

    @Input() onDataChangeMethodID;
    @Input() onFocusGainedMethodID;
    @Input() onFocusLostMethodID;

    @Output() dataProviderIDChange = new EventEmitter();
    @Input() editable;
    @Input() findmode;
    @Input() placeholderText;
    @Input() readOnly;
    @Input() selectOnEnter;
    @Input() valuelistID: IValuelist;

    constructor(renderer: Renderer2, public formattingService: FormattingService) {
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
                case 'selectOnEnter':
                    if ( change.currentValue ) PropertyUtils.addSelectOnEnter( this.getFocusElement(), this.renderer );
                    break;
            }
        }
      }
      super.ngOnChanges(changes);
    }

    update( val: string ) {
      console.log("update:" + val);
        if (!this.findmode && this.format) {
            this.dataProviderID = this.formattingService.parse(val, this.format, this.dataProviderID);
        } else this.dataProviderID = val;
        this.dataProviderIDChange.emit( this.dataProviderID );
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
