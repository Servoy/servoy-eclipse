import { Injectable } from '@angular/core';

import {FormService,FormCache,StructureCache,ComponentCache} from '../form.service';
import {ServoyService} from '../servoy.service'

@Injectable()
export class WindowService {
    
    constructor(private formService:FormService,private servoyService:ServoyService) {
    }
    
    public updateController(formName,formStructure) {
        var formState = JSON.parse(formStructure)[formName];
        this.formService.createFormCache(formName, formState);
    }
    
   public switchForm(name,form,navigatorForm) {
        // if first show of this form in browser window then request initial data (dataproviders and such)
        this.formService.formWillShow(form.name, false); // false because form was already made visible server-side
        if (navigatorForm && navigatorForm.name && navigatorForm.name.lastIndexOf("default_navigator_container.html") == -1) {
            // if first show of this form in browser window then request initial data (dataproviders and such)
            this.formService.formWillShow(navigatorForm.name, false); // false because form was already made visible server-side
        }

//        if(instances[name] && instances[name].type != WindowType.WINDOW) {
//            instances[name].form = form;
//            instances[name].navigatorForm = navigatorForm;
//        }
//        else 
            if (this.servoyService.getSolutionSettings().windowName == name) { // main window form switch
                this.servoyService.getSolutionSettings().mainForm = form;
                this.servoyService.getSolutionSettings().navigatorForm = navigatorForm;
//            var formparam = 'f=' + form.name;
//            if (($location.url().indexOf(formparam+'&') === -1) && ($location.url().indexOf(formparam,$location.url().length - formparam.length) === -1))
//                $location.url($location.path() + '?f=' + form.name);
//            else
//                $location.url($location.url());
        }
    }
}