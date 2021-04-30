import { Component, Output, EventEmitter, Input, ChangeDetectionStrategy, Renderer2, ChangeDetectorRef } from '@angular/core';
import { ServoyBaseComponent, JSEvent, EventLike, ServoyPublicService } from '@servoy/public';

@Component({
    selector: 'bootstrapextracomponents-rating',
    templateUrl: './rating.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyBootstrapExtraRating extends ServoyBaseComponent<HTMLDivElement> {
    @Input() onLeave: (e: JSEvent, data?: any) => void;
    @Input() onHover: (e: JSEvent, data?: any) => void;

    @Input() enabled: boolean;
    @Input() dataProviderID: number;
    @Output() dataProviderIDChange = new EventEmitter();
    @Input() max: number;
    @Input() showPercentageOnHover: boolean;
    @Input() stateOn: string;
    @Input() stateOff: string;

    overStar = false;
    percent: number;

    constructor( renderer: Renderer2, cdRef: ChangeDetectorRef, private servoyService: ServoyPublicService ) {
        super( renderer, cdRef );
    }


    svyOnInit() {
        super.svyOnInit();
        this.percent = this.dataProviderID * 100 / this.max ;
    }

    onLeaveEvent() {
        this.overStar = false;
        if (this.onLeave) {
            const jsEvent = this.servoyService.createJSEvent( {target : this.getNativeElement()} as EventLike, 'onLeave' );

            this.onLeave(jsEvent, this.dataProviderID);
        }
    }

    onHoverEvent(value: number) {
        if (this.enabled !== false) {
            this.percent = value / this.max * 100;
            this.overStar = true;
            if (this.onHover) {
                const jsEvent = this.servoyService.createJSEvent( {target : this.getNativeElement()}  as EventLike, 'onHover' );

                this.onHover(jsEvent, this.dataProviderID);
            }

        }
    }

    onChange(){
        this.dataProviderIDChange.emit(this.dataProviderID);
    }
}
