import { Component, Directive, ViewChild, QueryList, OnInit, Input, Output, EventEmitter, Renderer2, ElementRef, OnChanges, AfterViewInit, SimpleChanges } from '@angular/core';
import { ServoyBaseComponent } from '../../ngclient/servoy_public'

@Component( {
    selector: 'servoydefault-table',
    templateUrl: './table.html'
} )
export class ServoyDefaultTable extends ServoyBaseComponent implements AfterViewInit {
  
    // this is a hack for test, so that this has a none static child ref because the child is in a nested template
    @ViewChild('child', {static: false}) child:ElementRef;
    @ViewChild('element', {static: false}) elementRef:ElementRef;

    private changes: SimpleChanges;
    
    @Input() foundset;
    @Input() columns;

    constructor(renderer: Renderer2 ) {
        super(renderer);
    }
    
    ngAfterViewInit() {
        super.ngAfterViewInit();
    }
}


