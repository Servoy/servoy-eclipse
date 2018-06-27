import { Component, OnInit, Input, Output, EventEmitter, OnChanges, SimpleChanges,Renderer2,ElementRef,ViewChild } from '@angular/core';

import {PropertyUtils, FormattingService} from '../ngclient/servoy_public'

import {ServoyDefaultBaseComponent} from './basecomponent'

export class ServoyDefaultBaseField extends  ServoyDefaultBaseComponent {

    @Input() onDataChangeMethodID;
    @Input() onFocusGainedMethodID;
    @Input() onFocusLostMethodID;

    @Output() dataProviderIDChange = new EventEmitter();
    @Input() editable
    @Input() findmode
    @Input() placeholderText
    @Input() readOnly
    @Input() selectOnEnter
    @Input() valuelistID
    
    private funcOnFocus: any;

    constructor(renderer: Renderer2, private formattingService : FormattingService) {
        super(renderer);
    }

    ngOnInit() {
        let nativeInput = this.getNativeInput();
        if(nativeInput) {
            this.funcOnFocus = this.renderer.listen(nativeInput, 'focus', (evt) => {
                if(this.servoyApi) {
                    // we could get the property string from the native element's attribute,
                    // ( let dpString = this.getNativeElement().getAttribute('[dataProviderID]'); ...)
                    // but now it is anyway hard-coded in form_component.component.ts
                    this.servoyApi.startEdit("dataProviderID");
                }
            })
        }
    }

    ngOnChanges( changes: SimpleChanges ) {
        for ( let property in changes ) {
            let change = changes[property];
            switch ( property ) {
                case "editable":
                    if ( change.currentValue )
                        this.renderer.removeAttribute(this.elementRef.nativeElement,  "readonly" );
                    else
                        this.renderer.setAttribute(this.elementRef.nativeElement,  "readonly", "readonly" );
                    break;
                case "placeholderText":
                    if ( change.currentValue ) this.renderer.setAttribute(this.getNativeElement(),   'placeholder', change.currentValue );
                    else  this.renderer.removeAttribute(this.getNativeElement(),  'placeholder' );
                    break;
                case "selectOnEnter":
                    if ( change.currentValue ) PropertyUtils.addSelectOnEnter( this.elementRef.nativeElement, this.renderer );
                    break;

            }
        }
        super.ngOnChanges(changes);
    }

    update( val: string ) {
        this.dataProviderID = this.formattingService.parse(val, this.format, this.dataProviderID);
        this.dataProviderIDChange.emit( this.dataProviderID );
    }

    /**
     * this should return the native input element that will be used for startEdit 
     */
    protected getNativeInput():any {
        return null;
    }

    ngOnDestroy() {
        if(this.funcOnFocus) this.funcOnFocus();
        super.ngOnDestroy();
    }
}