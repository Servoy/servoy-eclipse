import { Component, Input } from '@angular/core';
import { AbstractFormComponent } from '../../ngclient/form/form_component.component';

@Component({
    selector: 'servoycore-formcomponent-responsive-container',
    templateUrl: './formcomponent-responsive-container.html',
    standalone: false
})
export class ServoyCoreFormcomponentResponsiveCotainer {
    @Input() items: any[];
    @Input() formComponent: AbstractFormComponent;
}
