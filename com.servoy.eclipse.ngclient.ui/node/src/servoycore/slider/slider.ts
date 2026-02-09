import { Component, Renderer2, ChangeDetectorRef, ChangeDetectionStrategy, input, output, signal } from '@angular/core';
import { ServoyBaseComponent } from '@servoy/public';


@Component( {
    selector: 'servoycore-slider',
    templateUrl: './slider.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
} )
export class ServoyCoreSlider extends ServoyBaseComponent<HTMLInputElement> {
    readonly onChangeMethodID = input(undefined);
    readonly onCreateMethodID = input(undefined);
    readonly onSlideMethodID = input(undefined);
    readonly onStartMethodID = input(undefined);
    readonly onStopMethodID = input(undefined);

    readonly min = input(undefined);
    readonly max = input(undefined);
    readonly orientation = input(undefined);
    readonly step = input(undefined);

    readonly dataProviderID = input<any>(undefined);
    readonly dataProviderIDChange = output();
    
    _dataProviderID = signal<any>(undefined);

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

    svyOnInit() {
        super.svyOnInit();
        if (this.orientation() === 'vertical') {
            this.renderer.setStyle(this.getNativeElement(), '-webkit-appearance', 'slider-vertical' );
            this.renderer.setAttribute(this.getNativeElement(), 'orient', 'vertical');
        }
     }

    update( event: Event) {
        this._dataProviderID.set((event.target as HTMLInputElement).value);
        this.dataProviderIDChange.emit( this._dataProviderID() );
    }

    protected attachHandlers(){
        if ( this.onChangeMethodID() ) {
            this.renderer.listen( this.getNativeElement(), 'change', e => this.onChangeMethodID()( e ));
        }
    }
}
