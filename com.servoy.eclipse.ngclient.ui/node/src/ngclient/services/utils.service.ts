import { Injectable } from '@angular/core';

@Injectable()
export class SvyUtilsService {
    public createJSEvent(event:KeyboardEvent, eventType:string, contextFilter?:string, contextFilterElement?:any) {
        let targetEl:Element = event.srcElement;
// TODO check        if (event.target)
//            targetEl = event.target;
//        else if (event.srcElement)
//            targetEl = event.srcElement;
//        
        let form;
        let parent:Element = targetEl;
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
// TODO       if (!form || form == 'MainController') {
//            // form not found, search for an active dialog
//            var formInDialog = $('.svy-dialog.window.active').find("svy-formload").attr("formname");
//            if (formInDialog)
//                form = formInDialog;
//        }
        if (!contextMatch)
            return null;
        var jsEvent = { svyType: 'JSEvent', eventType: eventType, "timestamp": new Date().getTime() };
        var modifiers = (event.altKey ? 8 : 0) | (event.shiftKey ? 1 : 0) | (event.ctrlKey ? 2 : 0) | (event.metaKey ? 4 : 0);
        jsEvent['modifiers'] = modifiers;
        jsEvent['x'] = event['pageX'];//TODO check
        jsEvent['y'] = event['pageY'];
        if (form != 'MainController') {
            jsEvent['formName'] = form;
//TODO            var formScope = angular.element(parent).scope();
//            for (var i = 0; i < targetElNameChain.length; i++) {
//                if (formScope['model'][targetElNameChain[i]]) {
//                    jsEvent['elementName'] = targetElNameChain[i];
//                    break;
//                }
//            }
            if (contextFilterElement && (contextFilterElement != jsEvent['elementName'])) {
                return null;
            }
        }
        return jsEvent;
    }
}