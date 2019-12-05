import { Component, Renderer2, Input, EventEmitter, Output} from '@angular/core';

import {ServoyDefaultBaseComponent} from '../../servoydefault/basecomponent'

@Component( {
    selector: 'servoycore-slider',
    templateUrl: './slider.html'
} )
export class ServoyCoreSlider extends ServoyDefaultBaseComponent {
    @Input() onChangeMethodID;
    @Input() onCreateMethodID;
    @Input() onSlideMethodID;
    @Input() onStartMethodID;
    @Input() onStopMethodID;
    
    @Input() min;
    @Input() max;
    @Input() orientation;
    @Input() step;
    
    @Output() dataProviderIDChange = new EventEmitter();
    
    constructor(renderer: Renderer2) { 
        super(renderer);
    }
   
    ngOnInit() {
        super.ngOnInit();
        if (this.orientation == 'vertical')
        {
            this.renderer.setStyle(this.getNativeElement(), "-webkit-appearance", 'slider-vertical' );
            this.renderer.setAttribute(this.getNativeElement(), 'orient', 'vertical');
        }    
     }
    
    protected attachHandlers(){
        if ( this.onChangeMethodID ) {
            this.renderer.listen( this.getNativeElement(), 'change', e => this.onChangeMethodID( e ));
        }
    }
    update( val: string ) {
        this.dataProviderID = val;
        this.dataProviderIDChange.emit( this.dataProviderID );
    }
}
