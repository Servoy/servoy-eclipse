import { Component, Output, EventEmitter, Input, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBaseComponent } from 'servoy-public';

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
    @Input() max: number;
    @Input() showPercentageOnHover: boolean;
    @Input() stateOn: string;
    @Input() stateOff: string;

    overStar: boolean = false;
    percent: number;

    @Output() dataProviderIDChange = new EventEmitter();
 
    svyOnInit() {
        super.svyOnInit();
        this.percent = this.dataProviderID * 100 / this.max ;
    }

    onLeaveEvent() {
        this.overStar = false;
        if (this.onLeave) {
            var jsEvent = this.createJSEvent('onLeave');

            this.onLeave(jsEvent, this.dataProviderID);
        }
    }

    onHoverEvent(value : number) {
        if (this.enabled !== false) {
            this.percent = value / this.max * 100;
            this.overStar = true;
            if (this.onHover) {
                let jsEvent = this.createJSEvent('onHover');

                this.onHover(jsEvent, this.dataProviderID);
            }

        }
    }

    onChange(){
        this.dataProviderIDChange.emit(this.dataProviderID);   
    }
    
    createJSEvent(eventType: string): JSEvent {
        //create JSEvent
        let jsEvent : JSEvent = { svyType: 'JSEvent' };

        jsEvent.elementName = this.getNativeElement().getAttribute('name');

        //get event type
        jsEvent.eventType = eventType;

        jsEvent.data = null;
        return jsEvent;
    }

}

class JSEvent {
    svyType: string;
    elementName?: string;
    eventType?: string;
    x?: number;
    y?: number;
    data?: any;
}
