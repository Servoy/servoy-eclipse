import { Injectable, Inject } from '@angular/core';
import { SabloService } from '../../sablo/sablo.service';
import { DOCUMENT } from '@angular/common';
import { FormService } from '../form.service';
import { LoggerFactory, LoggerService } from '../../sablo/logger.service';

@Injectable()
export class SvyUtilsService {
    private log: LoggerService;

    constructor(private sabloService: SabloService, @Inject(DOCUMENT) private document: Document, private formservice: FormService, logFactory: LoggerFactory) {
        this.log = logFactory.getLogger('SvyUtilsService');
    }

    public createJSEvent(event: KeyboardEvent, eventType: string, contextFilter?: string, contextFilterElement?: any) : JSEvent{
        if (!event) {
            if (contextFilter || contextFilterElement) return null;
            this.log.error("event is undefined, returning default event");
            return { svyType: 'JSEvent', eventType: eventType, "timestamp": new Date().getTime() };
        }
        const targetEl = event.target as Element;
        let form;
        let parent = targetEl;
        const targetElNameChain = new Array();
        let contextMatch = false;
        while (parent) {
            form = parent.tagName.toLowerCase() === 'svy-form' ? parent.getAttribute('ng-reflect-name') : undefined;
            if (form) {
                //global shortcut or context match
                const shortcuthit = !contextFilter || (contextFilter && form == contextFilter);
                if (!shortcuthit)
                    break;
                contextMatch = true;
                break;
            }
            else if (parent.tagName.toLowerCase() === 'body' && !contextFilter) {
                contextMatch = true;
                break;
            }

            if (parent.getAttribute('ng-reflect-name'))
                targetElNameChain.push(parent.getAttribute('ng-reflect-name'));
            parent = parent.parentElement;
        }

        if (!form) {
            // form not found, search for an active dialog
            let dialog = this.document.querySelector('.svy-dialog.window.active');
            if (dialog) {
                let formInDialog = dialog.querySelector('svy-form');
                if (formInDialog) form = formInDialog.getAttribute('ng-reflect-name');
            }
        }

        if (!contextMatch)
            return null;
        const jsEvent : JSEvent = { svyType: 'JSEvent', eventType : eventType, timestamp: new Date().getTime() } ;
        const modifiers = (event.altKey ? 8 : 0) | (event.shiftKey ? 1 : 0) | (event.ctrlKey ? 2 : 0) | (event.metaKey ? 4 : 0);
        jsEvent.modifiers = modifiers;
        jsEvent.x = event['pageX'];//TODO check
        jsEvent.y = event['pageY'];
        jsEvent.formName = form;
        for (let i = 0; i < targetElNameChain.length; i++) {
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

    public generateUploadUrl(formname, componentName, propertyName) {
        return 'resources/upload/' + this.sabloService.getClientnr() +
            (formname ? '/' + formname : '') +
            (componentName ? '/' + componentName : '') +
            (propertyName ? '/' + propertyName : '');
    }

    /**
     * JS implementation of the $.extend() jQuery method.
     * Source: https://gomakethings.com/vanilla-javascript-version-of-jquery-extend/
     */
    public deepExtend(args: any[]) {
        // Variables
        const extended = {};
        let deep = false;
        let i = 0;
        const length = args.length;

        // Check if a deep merge
        if (Object.prototype.toString.call(args[0]) === '[object Boolean]') {
            deep = args[0];
            i++;
        }

        // Merge the object into the extended object
        const merge = (obj) => {
            for (const prop in obj) {
                if (Object.prototype.hasOwnProperty.call(obj, prop)) {
                    // If deep merge and property is an object, merge properties
                    if (deep && Object.prototype.toString.call(obj[prop]) === '[object Object]') {
                        extended[prop] = this.deepExtend([true, extended[prop], obj[prop]]);
                    } else {
                        extended[prop] = obj[prop];
                    }
                }
            }
        };

        // Loop through each object and conduct a merge
        for (; i < length; i++) {
            const obj = args[i];
            merge(obj);
        }

        return extended;
    };

    public getMainBody() {
        return document.getElementById('mainBody');
    }
}

export class JSEvent {
    public formName?: string;
    public elementName?: string;
    public svyType: string;
    public eventType: string;
    public modifiers?: number;
    public x?: number;
    public y?: number;
    public timestamp: number;
}
