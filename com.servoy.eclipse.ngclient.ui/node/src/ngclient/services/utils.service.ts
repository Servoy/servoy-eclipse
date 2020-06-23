import { Injectable, Inject } from '@angular/core';
import { SabloService } from "../../sablo/sablo.service";
import { DOCUMENT } from '@angular/common';
import { FormService } from '../form.service';

@Injectable()
export class SvyUtilsService {
    
    constructor(private sabloService: SabloService, @Inject(DOCUMENT) private document: Document, private formservice: FormService) {}
    
    public createJSEvent(event:KeyboardEvent, eventType:string, contextFilter?:string, contextFilterElement?:any) {
        let targetEl = event.target as Element
        let form;
        let parent = targetEl;
        let targetElNameChain = new Array();
        let contextMatch = false;
        while (parent) {
            form = parent.tagName.toLowerCase() === "svy-form" ? parent.getAttribute("ng-reflect-name") : undefined;
            if (form) {
                //global shortcut or context match
                var shortcuthit = !contextFilter || (contextFilter && form == contextFilter);
                if (!shortcuthit)
                    break;
                contextMatch = true;
                break;
            }
            if (parent.getAttribute("ng-reflect-name"))
                targetElNameChain.push(parent.getAttribute("ng-reflect-name"));
            parent = parent.parentElement;
        }

        if (!contextMatch)
            return null;
        var jsEvent = { svyType: 'JSEvent', eventType: eventType, "timestamp": new Date().getTime() };
        var modifiers = (event.altKey ? 8 : 0) | (event.shiftKey ? 1 : 0) | (event.ctrlKey ? 2 : 0) | (event.metaKey ? 4 : 0);
        jsEvent['modifiers'] = modifiers;
        jsEvent['x'] = event['pageX'];//TODO check
        jsEvent['y'] = event['pageY'];
        jsEvent['formName'] = form;
        for (var i = 0; i < targetElNameChain.length; i++) {
            if (this.formservice.getFormCacheByName(form).getComponent(targetElNameChain[i])) {
                jsEvent['elementName'] = targetElNameChain[i];
                break;
            }
        }
        if (contextFilterElement && (contextFilterElement != jsEvent['elementName'])) {
            return null;
        }

        return jsEvent;
    }
    
    public generateUploadUrl (formname, componentName, propertyName) {
        return "resources/upload/" + this.sabloService.getClientnr() + 
            (formname ? "/" + formname : "") + 
            (componentName ? "/" + componentName : "") + 
            (propertyName ? "/" + propertyName : "");
    }
    
    /**
     * JS implementation of the $.extend() jQuery method. 
     * Source: https://gomakethings.com/vanilla-javascript-version-of-jquery-extend/
     */
    public deepExtend(args: any []) {
     // Variables
        var extended = {};
        var deep = false;
        var i = 0;
        var length = args.length;

        // Check if a deep merge
        if ( Object.prototype.toString.call( args[0] ) === '[object Boolean]' ) {
            deep = args[0];
            i++;
        }

        // Merge the object into the extended object
        let merge = (obj) => {
            for ( var prop in obj ) {
                if ( Object.prototype.hasOwnProperty.call( obj, prop ) ) {
                    // If deep merge and property is an object, merge properties
                    if ( deep && Object.prototype.toString.call(obj[prop]) === '[object Object]' ) {
                        extended[prop] = this.deepExtend( [true, extended[prop], obj[prop]] );
                    } else {
                        extended[prop] = obj[prop];
                    }
                }
            }
        };

        // Loop through each object and conduct a merge
        for ( ; i < length; i++ ) {
            var obj = args[i];
            merge(obj);
        }

        return extended;
      };

      public getMainBody() {
        return document.getElementById("mainBody");
      }
}