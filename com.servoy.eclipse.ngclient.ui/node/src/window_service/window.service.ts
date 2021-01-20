import { Injectable } from '@angular/core';

import { ShortcutService } from './shortcut.service';
import { SvyUtilsService } from '../ngclient/servoy_public';
import { ServoyService } from '../ngclient/servoy.service';

@Injectable()
export class WindowService {
    private _shortcuts: Shortcut[];

    constructor(private shortcutService: ShortcutService, private utils: SvyUtilsService, private servoyService: ServoyService) {

    }

    get shortcuts(): Shortcut[] {
        return this._shortcuts;
    }

    set shortcuts(shortcuts: Shortcut[]) {
        this._shortcuts = shortcuts;
        if (this._shortcuts) {
            this._shortcuts.forEach((newvalue) => {
                var translatedShortcut = this.translateSwingShortcut(newvalue.shortcut);
                if (!this.shortcutService.all_shortcuts[translatedShortcut]) {
                    this.shortcutService.add(translatedShortcut, (e) => {
                        let targetEl;
                        if (e.target) targetEl = e.target;
                        else if (e.srcElement) targetEl = e.srcElement;
                        let retValue = true;
                        let callback = newvalue.callback;
                        let contextFilter = null;
                        let contextFilterElement = null;
                        if (newvalue.contextFilter) {
                            let contextFilterParts = newvalue.contextFilter.split('.');
                            contextFilter = contextFilterParts[0];
                            if (contextFilterParts.length > 1) {
                                contextFilterElement = contextFilterParts[1];
                            }
                        }

                        var jsEvent = this.utils.createJSEvent(e, newvalue.shortcut, contextFilter, contextFilterElement);

                        if (!jsEvent) return retValue;

                        let args = newvalue.arguments;
                        let argsWithEvent: Array<any> = [jsEvent];// append args
                        if (args != null) {
                            if (args.length) {
                                argsWithEvent = argsWithEvent.concat(args);
                            }
                            else {
                                argsWithEvent.push(args);
                            }
                        }
                        targetEl.dispatchEvent(new CustomEvent("change"));
                        //$sabloTestability.block(true);
                        setTimeout((callback, argsWithEvent) => {
                            var formName = argsWithEvent[0].formName;
                            if (!formName) formName = callback.formname;
                            this.servoyService.executeInlineScript(formName, callback.script, argsWithEvent);
                            //$sabloTestability.block(false);
                        }, 10, callback, argsWithEvent);
                        if (retValue && newvalue.consumeEvent) retValue = false;
                        return retValue;

                    }
                    , { 'propagate': true, 'disable_in_input': false });
                }
            })
        }
    }

    private translateSwingShortcut(shortcutcombination: string): string {
        var shortcutParts = shortcutcombination.split(" ");
        var translatedShortcut = '';
        for (let i = 0; i < shortcutParts.length; i++) {
            if (i > 0) {
                translatedShortcut += '+';
            }
            if (shortcutParts[i] == 'control' || shortcutParts[i] == 'ctrl') {
                translatedShortcut += 'CTRL';
            }
            else if (shortcutParts[i] == 'meta') {
                translatedShortcut += 'META';
            }
            else if (shortcutParts[i] == 'shift') {
                translatedShortcut += 'SHIFT';
            }
            else if (shortcutParts[i] == 'alt') {
                translatedShortcut += 'ALT';
            }
            else if (shortcutParts[i].toLowerCase().indexOf('numpad') == 0) {
                //numpad0 to numpad9
                if (shortcutParts[i].length == 7) {
                    shortcutParts[i] = shortcutParts[i].toLowerCase();
                    shortcutParts[i] = shortcutParts[i].replace("numpad", "numpad-");
                    translatedShortcut += shortcutParts[i];
                }
                else {
                    translatedShortcut += shortcutParts[i];
                }
            }
            else {
                translatedShortcut += shortcutParts[i];
            }
        }
        return translatedShortcut;
    }
}

class Shortcut {
    public shortcut: string;
    public callback: string;
    public contextFilter: string;
    public consumeEvent: boolean;
    public arguments: Array<any>;

}
