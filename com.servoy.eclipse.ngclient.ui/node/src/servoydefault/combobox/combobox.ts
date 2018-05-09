import { Component, Directive,ViewChild,QueryList,OnInit, Input, Output, EventEmitter, Renderer2, ElementRef, OnChanges,SimpleChanges } from '@angular/core';

import {FormattingService} from '../../ngclient/servoy_public'

import {ServoyDefaultBaseField} from '../basefield'

@Component( {
    selector: 'servoydefault-combo',
    templateUrl: './combobox.html'
} )
export class ServoyDefaultCombobox extends ServoyDefaultBaseField {
    constructor(renderer: Renderer2, formattingService : FormattingService) { 
        super(renderer,formattingService);
    }

}

