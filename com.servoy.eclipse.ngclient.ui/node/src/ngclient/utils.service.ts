import { Injectable, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { EventLike, JSEvent, LoggerFactory, LoggerService } from '@servoy/public';

@Injectable({
    providedIn: 'root'
})
export class SvyUtilsService {
    private log: LoggerService;
    private doc: Document;

    constructor(@Inject(DOCUMENT) _doc: any, logFactory: LoggerFactory) {
        this.log = logFactory.getLogger('SvyUtilsService');
        this.doc = _doc;
    }

    public createJSEvent(event: EventLike, eventType: string, contextFilter?: string, contextFilterElement?: any): JSEvent {
        if (!event) {
            if (contextFilter || contextFilterElement) return null;
            this.log.error('event is undefined, returning default event');
            return { svyType: 'JSEvent', eventType, timestamp: new Date().getTime() };
        }
        const targetEl = event.target as Element;
        let form: string;
        let elementName: string;
        let parent = targetEl;
        let contextMatch = false;
        while (parent) {
            form = parent.tagName.toLowerCase() === 'svy-form' ? parent.getAttribute('name') : undefined;
            if (form) {
                //global shortcut or context match
                const shortcuthit = !contextFilter || (contextFilter && form === contextFilter);
                if (!shortcuthit)
                    break;
                contextMatch = true;
                break;
            } else if (parent.tagName.toLowerCase() === 'body' && !contextFilter) {
                contextMatch = true;
                break;
            }
            if (!elementName) {
                if (parent.getAttribute('name'))
                    elementName = parent.getAttribute('name');
                else if (parent['svyHostComponent'] && parent['svyHostComponent']['name']) {
                    elementName = parent['svyHostComponent']['name'];
                }
            }
            parent = parent.parentElement;
        }

        if (!form) {
            // form not found, search for an active dialog
            const dialog = this.doc.querySelector('.svy-dialog.window.active');
            if (dialog) {
                const formInDialog = dialog.querySelector('svy-form');
                if (formInDialog) form = formInDialog.getAttribute('name');
            }
        }

        if (!contextMatch)
            return null;
        const jsEvent: JSEvent = { svyType: 'JSEvent', eventType, timestamp: new Date().getTime() };
        // eslint-disable-next-line no-bitwise
        const modifiers = (event.altKey ? 8 : 0) | (event.shiftKey ? 1 : 0) | (event.ctrlKey ? 2 : 0) | (event.metaKey ? 4 : 0);
        jsEvent.modifiers = modifiers;
        jsEvent.x = event['pageX'];//TODO check
        jsEvent.y = event['pageY'];
        jsEvent.formName = form;
        jsEvent.elementName = elementName;
        if (contextFilterElement && (contextFilterElement !== jsEvent['elementName'])) {
            return null;
        }

        return jsEvent;
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
        const merge = (obj: any) => {
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
        return this.doc.getElementById('mainBody');
    }
}
