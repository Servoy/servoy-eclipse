import { Component, input } from '@angular/core';
import { AbstractFormComponent } from '../../ngclient/form/form_component.component';

@Component({
    selector: 'servoycore-formcomponent-responsive-container',
    templateUrl: './formcomponent-responsive-container.html',
    standalone: false
})
export class ServoyCoreFormcomponentResponsiveCotainer {
    readonly items = input<any[]>(undefined);
    readonly formComponent = input<AbstractFormComponent>(undefined);
}
