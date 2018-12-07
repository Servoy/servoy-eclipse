import { Component, Directive,ViewChild,QueryList,OnInit, Input, Output, EventEmitter, Renderer2, ElementRef, OnChanges,SimpleChanges } from '@angular/core';

import {ServoyDefaultBaseLabel} from  '../baselabel'

@Component( {
    selector: 'servoydefault-button',
    templateUrl: './button.html',
    styleUrls: ['./style.scss']
} )
export class ServoyDefaultButton extends ServoyDefaultBaseLabel {

    constructor(renderer: Renderer2) {
        super(renderer);
    }
}

