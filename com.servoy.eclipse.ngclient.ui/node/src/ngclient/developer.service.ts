import { Inject, Injectable, DOCUMENT } from '@angular/core';
import { environment } from '../environments/environment';

import { SabloService } from '../sablo/sablo.service';
import { SvyUtilsService } from './utils.service';

@Injectable({
  providedIn: 'root'
})
export class DeveloperService {
    constructor(@Inject(DOCUMENT)doc: Document, sabloService: SabloService, svyUtilsService: SvyUtilsService) {
        if (!environment.production) {
            doc.addEventListener('keydown',(event) => {
                if (event.ctrlKey && event.key === 'l') {
                    const jsevent = svyUtilsService.createJSEvent(event, 'keydown');
                    let formname = jsevent.formName;
                    if (!formname) {
                      const mainForm = doc.querySelector('svy-form');
                      if (mainForm) formname = mainForm.getAttribute('name');
                    }
                    if (formname) sabloService.callService('developerService', 'openFormInDesigner', {formname},true);
                    event.cancelBubble = true;
                    //e.stopPropagation works in Firefox.
                    if (event.stopPropagation) {
                        event.stopPropagation();
                        event.preventDefault();
                    }
                    return false;
                }
            } ,false);
        }
    }
}
