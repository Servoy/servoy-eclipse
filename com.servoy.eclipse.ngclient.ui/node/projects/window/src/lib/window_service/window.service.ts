import { inject, Injectable, DOCUMENT } from '@angular/core';


import { ShortcutService, Shortcut as Shortcut2 } from './shortcut.service';
import { PopupMenuService, Popup } from './popupmenu.service';
import { LoggerFactory, LoggerService, ServoyPublicService, PopupForm, Callback } from '@servoy/public';

@Injectable()
export class WindowPluginService {
    private _shortcuts!: Shortcut[];
    private _popupmenus!: Popup[];
    private _popupMenuShowCommand!: PopupMenuShowCommand | null;
    private _popupform!: PopupForm;
    private readonly log: LoggerService;

    private readonly shortcutService = inject(ShortcutService);
    private readonly popupMenuService = inject(PopupMenuService);
    private readonly servoyService = inject(ServoyPublicService);
    private readonly doc = inject(DOCUMENT) as Document;

    constructor() {
        const logFactory = inject(LoggerFactory);
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
                        let targetEl: EventTarget = null!;
                        if (e.target) targetEl = e.target;
                        else if (e.srcElement) targetEl = e.srcElement;
                        let retValue = true;
                        
                        for (var j = 0; j < this._shortcuts.length; j++) {
                            if (translatedShortcut == this.translateSwingShortcut(this._shortcuts[j].shortcut)) {
                                const callback = this._shortcuts[j].callback;
                                let contextFilter: string | null = null;
                                let contextFilterElement: string | null = null;
                                if (this._shortcuts[j].contextFilter) {
                                    const contextFilterParts = this._shortcuts[j].contextFilter.split('.');
                                    contextFilter = contextFilterParts[0];
                                    if (contextFilterParts.length > 1) {
                                        contextFilterElement = contextFilterParts[1];
                                    }
                                }

                                const jsEvent = this.servoyService.createJSEvent(e as any, newvalue.shortcut, contextFilter ?? undefined, contextFilterElement ?? undefined);

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
                                // should trigger a change only if the shorcut is a combination of 'CTRL' or 'ALT' or 'META' + any key
                                if (this.checkModifierKey(translatedShortcut)) {
                                    targetEl.dispatchEvent(new CustomEvent('change'));
                                }
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

    get popupMenuShowCommand(): PopupMenuShowCommand | null {
        return this._popupMenuShowCommand;
    }

    set popupMenuShowCommand(popupMenuShowCommand: PopupMenuShowCommand) {
        this._popupMenuShowCommand = popupMenuShowCommand;
        this.showPopupMenuInternal();
    }

    get popupMenus(): Popup[] {
        return this._popupmenus;
    }

    set popupMenus(popupmenus: Popup[]) {
        this._popupmenus = popupmenus;
        this.showPopupMenuInternal();
    }
    
    private checkModifierKey(str: string) {
        return ['ctrl', 'alt', 'meta'].some(item => str.toLowerCase().startsWith(item));
    }
    
    private timeoutId: ReturnType<typeof setTimeout> | number | null = null;
    private showPopupMenuInternal() {
        if (this.timeoutId !== null) {
            clearTimeout(this.timeoutId);
        }

        this.timeoutId = setTimeout(() => {
            this.showPopupMenu();
            this.timeoutId = null;
        }, 0);
    }

    private showPopupMenu() {
        if (this._popupmenus && this._popupMenuShowCommand) {
            for (const i of Object.keys(this._popupmenus)) {
                if (this._popupMenuShowCommand.popupName === this._popupmenus[i as any].name) {
                    this.popupMenuService.initClosePopupHandler(() => {
                        this._popupMenuShowCommand = null;
                        this.servoyService.sendServiceChanges('window', 'popupMenuShowCommand', this._popupMenuShowCommand);
                    });
                    this.popupMenuService.initMenu(this._popupmenus[i as any]);
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
    public shortcut!: string;
    public callback!: Callback;
    public contextFilter!: string;
    public consumeEvent!: boolean;
    public arguments!: Array<any>;

}

export interface PopupMenuShowCommand {
    popupName: string;
    elementId: string;
    height: number;
    positionTop: boolean;
    x: number;
    y: number;
}

