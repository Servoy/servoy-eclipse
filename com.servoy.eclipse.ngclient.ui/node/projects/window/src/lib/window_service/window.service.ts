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
                    let x: number;
                    let y: number;
                    this.popupMenuService.initClosePopupHandler(() => {
                        this._popupMenuShowCommand = null;
                        this.servoyService.sendServiceChanges('window', 'popupMenuShowCommand', this._popupMenuShowCommand);
                    });
                    this.popupMenuService.initMenu(this._popupmenus[i]);
                    if (this._popupMenuShowCommand.elementId) {
                        const element = this.doc.getElementById(this._popupMenuShowCommand.elementId);
                        if (element) {
                            //get element bounds relativ to viewport
                            const compRect = element.getBoundingClientRect();
                            const roomAbove = compRect.top - 1;
							const roomBelow = document.documentElement.clientHeight - compRect.top - this._popupMenuShowCommand.height;
                            //get menu region
                            const menuRect = this.popupMenuService.getMenuRect(this._popupmenus[i]);
                            const menuHeight = menuRect.bottom - menuRect.top;
                            const xyReceived = this._popupMenuShowCommand.y !== undefined;
							const mx = xyReceived ? this._popupMenuShowCommand.x : 0;
							const my = xyReceived ? this._popupMenuShowCommand.y : 0;
							if ((this._popupMenuShowCommand.positionTop === true && menuHeight <= roomAbove) || //top position wanted
							    (this._popupMenuShowCommand.positionTop === false && (menuHeight > roomBelow) && (menuHeight <= roomAbove))) {//no space below
								//draw on component's top
                                x = element.scrollLeft + compRect.left + mx;
                                y = element.scrollTop + compRect.top + my - menuHeight;
							} else if (menuHeight <= roomBelow) { //default we are drawing below component
                                x = element.scrollLeft + compRect.left + mx;
                                y = element.scrollTop + compRect.top + my + (xyReceived ? 0 : this._popupMenuShowCommand.height);
							} else {//no room for popup menu so let's browser decide
                                x = element.scrollLeft + compRect.left + mx;
                                y = element.scrollTop + compRect.top + my;
							}
                        } else {
                            this.log.error('Cannot display popup, element with id:' + this._popupMenuShowCommand.elementId + ' , not found');
                            return;
                        }
                    } else {
                        x = this._popupMenuShowCommand.x;
                        y = this._popupMenuShowCommand.y;
                    }
                    this.popupMenuService.showMenu(x, y);
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

