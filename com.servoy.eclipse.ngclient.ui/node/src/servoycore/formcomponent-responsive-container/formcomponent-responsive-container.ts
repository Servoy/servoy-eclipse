import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { AbstractFormComponent } from '../../ngclient/form/form_component.component';

@Component({
    selector: 'servoycore-formcomponent-responsive-container',
    templateUrl: './formcomponent-responsive-container.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: true,
    imports: [NgTemplateOutlet]
})
export class ServoyCoreFormcomponentResponsiveCotainer {
    readonly items = input<any[]>(undefined!);
    readonly formComponent = input<AbstractFormComponent>(undefined!);
}
