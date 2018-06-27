import { Component, OnInit, Input, Output, EventEmitter, OnChanges, SimpleChanges,Renderer2,ElementRef,ViewChild } from '@angular/core';

import {FormattingService} from '../../ngclient/servoy_public'

import {ServoyDefaultBaseField} from '../basefield'

@Component( {
    selector: 'servoydefault-textfield',
    templateUrl: './textfield.html'
} )
export class ServoyDefaultTextField extends ServoyDefaultBaseField {
    constructor(renderer: Renderer2, formattingService : FormattingService) { 
        super(renderer,formattingService);
    }

    getNativeInput() {
        return this.elementRef.nativeElement;
    }
}
