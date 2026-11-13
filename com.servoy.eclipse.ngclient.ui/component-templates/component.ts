import { Component, input, SimpleChanges, Renderer2, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ServoyBaseComponent } from '@servoy/public';

@Component({
    selector: '##packagename##-##componentdashname##',
    templateUrl: './##componentname##.html',
     changeDetection: ChangeDetectionStrategy.OnPush,
     standalone: true,
     imports: [CommonModule]
})
export class ##componentclassname## extends ServoyBaseComponent<HTMLDivElement>{

    yourName = input<string>(undefined);

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