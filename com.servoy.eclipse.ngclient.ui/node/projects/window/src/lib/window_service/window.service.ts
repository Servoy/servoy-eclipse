import { Injectable, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';

import { ShortcutService, Shortcut as Shortcut2 } from './shortcut.service';
import { PopupMenuService, Popup } from './popupmenu.service';
import { LoggerFactory, LoggerService, ServoyPublicService, PopupForm, Callback, BaseCustomObject } from '@servoy/public';

@Injectable()
export class WindowPluginService {
    private _shortcuts: Shortcut[];
    private _popupmenus: Popup[];
    private _popupMenuShowCommand: PopupMenuShowCommand;
    private _popupform: PopupForm;
    private log: LoggerService;

    constructor(private shortcutService: ShortcutService,
        private popupMenuService: PopupMenuService,
        private servoyService: ServoyPublicService,
        @Inject(DOCUMENT) private doc: Document,
        logFactory: LoggerFactory) {
        this.log = logFactory.getLogger('WindowService');
    }


    cancelFormPopup(): void {
        this.cancelFormPopupInternal(false);
    }

    cancelFormPopupInternal(disableClearPopupFormCallToServer: boolean): void {
        this.servoyService.cancelFormPopup(disableClearPopupFormCallToServer);
    }
	
	cancelForm(form: string) {
		this.servoyService.cancelFormPopup(form);
	}
	
    get shortcuts(): Shortcut[] {
        return this._shortcuts;
    }

    set shortcuts(shortcuts: Shortcut[]) {
        this._shortcuts = shortcuts;
        if (this._shortcuts) {
            this._shortcuts.forEach((newvalue) => {
                const translatedShortcut = this.translateSwingShortcut(newvalue.shortcut);
                if (!this.shortcutService.all_shortcuts[translatedShortcut]) {
                    this.shortcutService.add(translatedShortcut, (e: KeyboardEvent) => {
                        let targetEl: EventTarget;
                        if (e.target) targetEl = e.target;
                        else if (e.srcElement) targetEl = e.srcElement;
                        let retValue = true;
                        
                        for (var j = 0; j < this._shortcuts.length; j++) {
                            if (translatedShortcut == this.translateSwingShortcut(this._shortcuts[j].shortcut)) {
                                const callback = this._shortcuts[j].callback;
                                let contextFilter = null;
                                let contextFilterElement = null;
                                if (this._shortcuts[j].contextFilter) {
                                    const contextFilterParts = this._shortcuts[j].contextFilter.split('.');
                                    contextFilter = contextFilterParts[0];
                                    if (contextFilterParts.length > 1) {
                                        contextFilterElement = contextFilterParts[1];
                                    }
                                }

                                const jsEvent = this.servoyService.createJSEvent(e, newvalue.shortcut, contextFilter, contextFilterElement);

                                if (!jsEvent) continue;

                                const args = this._shortcuts[j].arguments;
                                let argsWithEvent: Array<any> = [jsEvent];// append args
                                if (args != null) {
                                    if (args.length) {
                                        argsWithEvent = argsWithEvent.concat(args);
                                    } else {
                                        argsWithEvent.push(args);
                                    }
                                }
                                targetEl.dispatchEvent(new CustomEvent('change'));
                                //$sabloTestability.block(true);
                                setTimeout((clb: { script: string; formname?: string }, clbArgs: Array<any>) => {
                                    let formName = clbArgs[0].formName;
                                    if (!formName) formName = clb.formname;
                                    this.servoyService.executeInlineScript(formName, clb.script, clbArgs);
                                    //$sabloTestability.block(false);
                                }, 10, callback, argsWithEvent);
                                if (retValue && newvalue.consumeEvent) retValue = false;
                            }
                        }
                        return retValue;

                    }
                        , { propagate: true, disable_in_input: false } as Shortcut2);
                }
            });
        }
    }

    get popupMenuShowCommand(): PopupMenuShowCommand {
        return this._popupMenuShowCommand;
    }

    set popupMenuShowCommand(popupMenuShowCommand: PopupMenuShowCommand) {
        this._popupMenuShowCommand = popupMenuShowCommand;
        this.showPopupMenu();
    }

    get popupMenus(): Popup[] {
        return this._popupmenus;
    }

    set popupMenus(popupmenus: Popup[]) {
        this._popupmenus = popupmenus;
        this.showPopupMenu();
    }

    private showPopupMenu() {
        if (this._popupmenus && this._popupMenuShowCommand) {
            for (const i of Object.keys(this._popupmenus)) {
                if (this._popupMenuShowCommand.popupName === this._popupmenus[i].name) {
                    this.popupMenuService.initClosePopupHandler(() => {
                        this._popupMenuShowCommand = null;
                        this.servoyService.sendServiceChanges('window', 'popupMenuShowCommand', this._popupMenuShowCommand);
                    });
                    this.popupMenuService.initMenu(this._popupmenus[i]);
                    if (this._popupMenuShowCommand?.elementId) {
                        const element = this.doc.querySelector("[id^="+this._popupMenuShowCommand.elementId+"]") as HTMLElement;
                        if (element && this._popupMenuShowCommand.x && this._popupMenuShowCommand.y) {
							const x = element.getBoundingClientRect().x + this._popupMenuShowCommand.x;
							const y = element.getBoundingClientRect().y + this._popupMenuShowCommand.y;
							this.popupMenuService.showMenu(x, y, this._popupMenuShowCommand?.positionTop || false);
						}
                        else if (element) {
                            this.popupMenuService.showMenuAt(element, this._popupMenuShowCommand?.positionTop || false);
                        } else {
                            this.log.error('Cannot display popup, element with id:' + this._popupMenuShowCommand.elementId + ' , not found');
                        }
                    } else {
                        this.popupMenuService.showMenu(this._popupMenuShowCommand.x, this._popupMenuShowCommand.y, this._popupMenuShowCommand?.positionTop || false);
                    }
                    break;
                }
            }
        }
    }

    private translateSwingShortcut(shortcutcombination: string): string {
        const shortcutParts = shortcutcombination.split(' ');
        let translatedShortcut = '';
        for (let i = 0; i < shortcutParts.length; i++) {
            if (i > 0) {
                translatedShortcut += '+';
            }
            if (shortcutParts[i] === 'control' || shortcutParts[i] === 'ctrl') {
                translatedShortcut += 'CTRL';
            } else if (shortcutParts[i] === 'meta') {
                translatedShortcut += 'META';
            } else if (shortcutParts[i] === 'shift') {
                translatedShortcut += 'SHIFT';
            } else if (shortcutParts[i] === 'alt') {
                translatedShortcut += 'ALT';
            } else if (shortcutParts[i].toLowerCase().indexOf('numpad') === 0) {
                //numpad0 to numpad9
                if (shortcutParts[i].length === 7) {
                    shortcutParts[i] = shortcutParts[i].toLowerCase();
                    shortcutParts[i] = shortcutParts[i].replace('numpad', 'numpad-');
                    translatedShortcut += shortcutParts[i];
                } else {
                    translatedShortcut += shortcutParts[i];
                }
            } else {
                translatedShortcut += shortcutParts[i];
            }
        }
        return translatedShortcut;
    }

    get popupform(): PopupForm {
        return this._popupform;
    }

    set popupform(popup: PopupForm) {
        this._popupform = popup;
        if (popup) this.servoyService.showForm(popup);
    }
}

class Shortcut {
    public shortcut: string;
    public callback: Callback;
    public contextFilter: string;
    public consumeEvent: boolean;
    public arguments: Array<any>;

}

export class PopupMenuShowCommand extends BaseCustomObject {
    public popupName: string;
    public elementId: string;
    public height: number;
    public positionTop: boolean
    public x: number;
    public y: number;
}

