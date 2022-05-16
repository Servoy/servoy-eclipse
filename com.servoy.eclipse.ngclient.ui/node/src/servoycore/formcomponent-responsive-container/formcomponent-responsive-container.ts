import { Component, Input } from '@angular/core';
import { FormComponent } from '../../ngclient/form/form_component.component';

@Component({
    selector: 'servoycore-formcomponent-responsive-container',
    templateUrl: './formcomponent-responsive-container.html'
})
export class ServoyCoreFormcomponentResponsiveCotainer {
    @Input() items: any[];
    @Input() formComponent: FormComponent;
}
