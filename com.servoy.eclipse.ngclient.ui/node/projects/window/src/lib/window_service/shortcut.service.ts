import { Inject, Injectable, DOCUMENT } from '@angular/core';

import {BaseCustomObject} from '@servoy/public';

@Injectable()
export class ShortcutService {
    all_shortcuts: any = {};

    constructor(@Inject(DOCUMENT) private doc: Document) {

    }

    add(shortcut_combination: string, callback: (e: KeyboardEvent) => boolean, opt?: Shortcut): void {
        //Provide a set of default options
        const default_options: Shortcut = {
            type: 'keydown',
            propagate: false,
            disable_in_input: false,
            target: this.doc,
            keycode: false
        } as Shortcut;
        if (!opt) opt = default_options;
        else {
            for (const dfo in default_options) {
                if (typeof opt[dfo] == 'undefined') opt[dfo] = default_options[dfo];
            }
        }

        let ele = opt.target;
        if (typeof opt.target == 'string') ele = this.doc.getElementById(opt.target);

        //The function to be called at keypress
        const func = (e: KeyboardEvent) => {
            e = e || window.event as KeyboardEvent;

            if (opt['disable_in_input']) { //Don't enable shortcut keys in Input, Textarea fields
                let element: Element;
                if (e.target) element = e.target as Element;
                else if (e.srcElement) element = e.srcElement as Element;
                if (element.nodeType === 3) element = element.parentNode as Element;

                if (element.tagName === 'INPUT' || element.tagName === 'TEXTAREA') return;
            }

            //Find Which key is pressed
            let code = null;
            if (e.keyCode) code = e.keyCode;
            else if (e.which) code = e.which;
            let character = String.fromCharCode(code);

            if (code === 188) character = ','; //If the user presses , when the type is onkeydown
            else if (code === 190) character = '.'; //If the user presses , when the type is onkeydown
            else if (code === 189) character = '-'; //If the user presses - when the type is onkeydown
            else if (code === 187) character = '='; //If the user presses = when the type is onkeydown
            else if (code === 191) character = '/'; //If the user presses / when the type is onkeydown
            else if (code === 220) character = '\\'; //If the user presses \ when the type is onkeydown

            const keys = shortcut_combination.split('+');
            //Key Pressed - counts the number of valid keypresses - if it is same as the number of keys, the shortcut function is invoked
            let kp = 0;

            //Work around for stupid Shift key bug created by using lowercase - as a result the shift+num combination was broken
            const shift_nums = {
                '`': '~',
                1: '!',
                2: '@',
                3: '#',
                4: '$',
                5: '%',
                6: '^',
                7: '&',
                8: '*',
                9: '(',
                0: ')',
                '-': '_',
                '=': '+',
                ';': ':',
                '\'': '"',
                ',': '<',
                '.': '>',
                '/': '?',
                '\\': '|'
            };
            //Special Keys - and their codes
            const special_keys = {
                esc: 27,
                escape: 27,
                tab: 9,
                space: 32,
                return: 13,
                enter: 13,
                backspace: 8,

                scrolllock: 145,
                scroll_lock: 145,
                scroll: 145,
                capslock: 20,
                caps_lock: 20,
                caps: 20,
                numlock: 144,
                num_lock: 144,
                num: 144,

                pause: 19,
                break: 19,

                insert: 45,
                home: 36,
                delete: 46,
                end: 35,

                pageup: 33,
                page_up: 33,
                pu: 33,

                pagedown: 34,
                page_down: 34,
                pd: 34,

                left: 37,
                up: 38,
                right: 39,
                down: 40,

                f1: 112,
                f2: 113,
                f3: 114,
                f4: 115,
                f5: 116,
                f6: 117,
                f7: 118,
                f8: 119,
                f9: 120,
                f10: 121,
                f11: 122,
                f12: 123,

                'numpad-0': 96,
                'numpad-1': 97,
                'numpad-2': 98,
                'numpad-3': 99,
                'numpad-4': 100,
                'numpad-5': 101,
                'numpad-6': 102,
                'numpad-7': 103,
                'numpad-8': 104,
                'numpad-9': 105,
                multiply: 106,
                add: 107,
                subtract: 109,
                decimal: 110,
                divide: 111,
                'semi-colon': 186
            };

            const modifiers = {
                shift: { wanted: false, pressed: false },
                ctrl: { wanted: false, pressed: false },
                alt: { wanted: false, pressed: false },
                meta: { wanted: false, pressed: false }   //Meta is Mac specific
            };

            if (e.ctrlKey) modifiers.ctrl.pressed = true;
            if (e.shiftKey) modifiers.shift.pressed = true;
            if (e.altKey) modifiers.alt.pressed = true;
            if (e.metaKey) modifiers.meta.pressed = true;

            for (const key of Object.keys(keys)) {
                //Modifiers
                const k = keys[key];
                if (k === 'CTRL' || k === 'CONTROL') {
                    kp++;
                    modifiers.ctrl.wanted = true;

                } else if (k === 'SHIFT') {
                    kp++;
                    modifiers.shift.wanted = true;

                } else if (k === 'ALT') {
                    kp++;
                    modifiers.alt.wanted = true;
                } else if (k === 'META') {
                    kp++;
                    modifiers.meta.wanted = true;
                } else if (k.length > 1) { //If it is a special key
                    if (special_keys[k.toLowerCase()] === code) kp++;

                } else if (opt['keycode']) {
                    if (opt['keycode'] === code) kp++;

                } else { //The special keys did not match
                    if (character === k) kp++;
                    else {
                        if (shift_nums[character] && e.shiftKey) { //Stupid Shift key bug created by using lowercase
                            character = shift_nums[character];
                            if (character === k) kp++;
                        }
                    }
                }
            }

            if (kp === keys.length &&
                modifiers.ctrl.pressed === modifiers.ctrl.wanted &&
                modifiers.shift.pressed === modifiers.shift.wanted &&
                modifiers.alt.pressed === modifiers.alt.wanted &&
                modifiers.meta.pressed === modifiers.meta.wanted) {
                const retVal = callback(e);

                if (!opt['propagate'] || !retVal) { //Stop the event
                    //e.cancelBubble is supported by IE - this will kill the bubbling process.
                    e.cancelBubble = true;
                    e.returnValue = false;

                    //e.stopPropagation works in Firefox.
                    if (e.stopPropagation) {
                        e.stopPropagation();
                        e.preventDefault();
                    }
                    return false;
                }
            }
        };
        this.all_shortcuts[shortcut_combination] = {
            callback: func,
            target: ele,
            event: opt['type']
        };
        //Attach the function with the event
        if (ele.addEventListener) ele.addEventListener(opt['type'], func, false);
        else if (ele.attachEvent) ele.attachEvent('on' + opt['type'], func);
        else ele['on' + opt['type']] = func;
    }

    remove(shortcut_combination: string): void {
        const binding = this.all_shortcuts[shortcut_combination];
        delete (this.all_shortcuts[shortcut_combination]);
        if (!binding) return;
        const type = binding['event'];
        const ele = binding['target'];
        const callback = binding['callback'];

        if (ele.detachEvent) ele.detachEvent('on' + type, callback);
        else if (ele.removeEventListener) ele.removeEventListener(type, callback, false);
        else ele['on' + type] = false;
    }
}

export class Shortcut extends BaseCustomObject{
    public type?: string;
    public propagate?: boolean;
    public disable_in_input?: boolean;
    public target?: any;
    public keycode?: boolean;
}
