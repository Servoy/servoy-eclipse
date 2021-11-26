import { Component, Input, SimpleChanges, Renderer2, ChangeDetectorRef } from '@angular/core';
import { ServoyBaseComponent } from '@servoy/public';

@Component({
    selector: '##packagename##-##componentname##',
    templateUrl: './##componentname##.html'
})
export class ##componentclassname## extends ServoyBaseComponent<HTMLDivElement>{

    @Input() yourName: string;

    constructor(protected readonly renderer: Renderer2, protected cdRef: ChangeDetectorRef) {
         super(renderer, cdRef);
    }
    
    svyOnInit() {
        super.svyOnInit();
    }
    
    svyOnChanges( changes: SimpleChanges ) {
        super.svyOnChanges(changes);
    }
    
}