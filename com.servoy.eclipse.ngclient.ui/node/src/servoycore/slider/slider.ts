import { Component, Renderer2, Input, EventEmitter, Output, ChangeDetectorRef, ChangeDetectionStrategy} from '@angular/core';
import { ServoyBaseComponent } from '@servoy/public';


@Component( {
    selector: 'servoycore-slider',
    templateUrl: './slider.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
} )
export class ServoyCoreSlider extends ServoyBaseComponent<HTMLInputElement> {
    @Input() onChangeMethodID;
    @Input() onCreateMethodID;
    @Input() onSlideMethodID;
    @Input() onStartMethodID;
    @Input() onStopMethodID;

    @Input() enabled: boolean;

    @Input() min;
    @Input() max;
    @Input() orientation;
    @Input() step;
    @Input() range;

    @Input() dataProviderID: any;
    @Output() dataProviderIDChange = new EventEmitter();

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

    svyOnInit() {
        super.svyOnInit();
        if (this.orientation === 'vertical') {
            this.renderer.setStyle(this.getNativeElement(), '-webkit-appearance', 'slider-vertical' );
            this.renderer.setAttribute(this.getNativeElement(), 'orient', 'vertical');
        }
     }

    update( event: Event) {
        this.dataProviderID = (event.target as HTMLInputElement).value;
        this.dataProviderIDChange.emit( this.dataProviderID );
    }

    protected attachHandlers(){
        if ( this.onChangeMethodID ) {
            this.renderer.listen( this.getNativeElement(), 'change', e => this.onChangeMethodID( e ));
        }
    }
}
