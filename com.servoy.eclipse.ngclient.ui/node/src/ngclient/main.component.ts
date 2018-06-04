import { Component } from '@angular/core';

import { ServoyService } from './servoy.service';
import { AllServiceService } from './allservices.service';
import { FormService } from './form.service';

@Component( {
    selector: 'servoy-main',
    templateUrl: './main.component.html'
} )

export class MainComponent {
    title = 'Servoy NGClient';

    constructor( private servoyService: ServoyService, private allService: AllServiceService,  private formservice: FormService  ) {
        this.servoyService.connect();
    }

    public get mainForm() {
        const mainForm = this.servoyService.getSolutionSettings().mainForm;
        if ( mainForm && mainForm.name ) return mainForm.name;
        return null;
    }

    hasDefaultNavigator():boolean {
        return this.mainForm && this.formservice.getFormCacheByName( this.mainForm.toString() ).getComponent('svy_default_navigator') != null;
    }
}
