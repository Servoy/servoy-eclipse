import { Injectable } from '@angular/core';
import { LoggerFactory, LoggerService } from '../sablo/logger.service';
import { WindowRefService } from '../sablo/util/windowref.service';

import * as electron from 'electron';
import { ServoyService } from '../ngclient/servoy.service';

@Injectable()
export class NGDesktopUIService {
    private log: LoggerService;
    private electron: typeof electron;
    private remote: electron.Remote;
    private Menu: typeof electron.Menu;
    private window: electron.BrowserWindow;
    private isMacOS = false;
    private isMacDefaultMenu = false;
    private browserViews = {};
    private browserViewCounter = 0;
    private mainMenuTemplate: Array<(electron.MenuItemConstructorOptions) | (electron.MenuItem)> = [];
    constructor(private servoyService: ServoyService, windowRef: WindowRefService, logFactory: LoggerFactory) {
        this.log = logFactory.getLogger('NGDesktopUtilsService');
        const userAgent = navigator.userAgent.toLowerCase();
        const r = windowRef.nativeWindow['require'];
        if (userAgent.indexOf(' electron/') > -1 && r) {
            this.electron = r('electron');
            this.remote = r('electron').remote;
            this.Menu = this.remote.Menu;
            this.window = this.remote.getCurrentWindow();
            this.isMacOS = ( r('os').platform() === 'darwin');
        } else {
            this.log.warn('ngdesktopui service/plugin loaded in a none electron environment');
        }
    }

    /**
     * Add new menu to the menu bar
     *
     * @param text - menu text
     * @param [index] - menu insert position (zero based)
     *
     * @return - the index of the added menu
     */
    addMenu(text: string, index: number) {
        const result = this.addMenuImpl(text, index);
        this.Menu.setApplicationMenu(this.Menu.buildFromTemplate(this.mainMenuTemplate));
        return result[1];
    }

    /**
     * Add Developer Tools menu to the menu bar
     * This function is adding Developer Tools menu.
     * Use it just for debugging. Remove any call to this function once you're done.
     *
     * @return - the index of the added menu
     */
    addDevToolsMenu() {
        if (this.isMacDefaultMenu) {
            this.mainMenuTemplate = [];
            this.isMacDefaultMenu = false;
        }
        const myMenu = {
            label: 'Developer Tools',
            submenu: [
                {
                    label: 'Open Developer Tools',
                    click: () => {
                        const devTools = new this.remote.BrowserWindow();
                        this.window.webContents.setDevToolsWebContents(devTools.webContents);
                        this.window.webContents.openDevTools({ mode: 'detach' });
                        setTimeout(() => {
                            // set the bounds to be a bit bigger to just force a redraw
                            const bounds = devTools.getBounds();
                            bounds.width = bounds.width + 10;
                            devTools.setBounds(bounds);
                        }, 10)
                    }
                }
            ]
        };

        this.mainMenuTemplate.push(myMenu);

        this.Menu.setApplicationMenu(this.Menu.buildFromTemplate(this.mainMenuTemplate));
        return this.mainMenuTemplate.length - 1;
    }

    /**
     * Delete menu from the specified position from the menu bar
     *
     * @param index - menu position to be deleted
     */
    removeMenu(index: number) {
        if (Number.isInteger(index)) {
            this.mainMenuTemplate.splice(index, 1);
            if (this.mainMenuTemplate.length == 0) {
                this.removeAllMenus();
            } else {
                this.Menu.setApplicationMenu(this.Menu.buildFromTemplate(this.mainMenuTemplate));
            }
        }
    }

    /**
     * Return the menu text at the specified position.
     *
     * @param (string) text - the text to query for
     *
     * @return - menu index containing the specified text; if not found return -1
     */
    getMenuIndexByText(text: string) {
        const menu = this.Menu.getApplicationMenu();
        let retVal = -1;
        if (menu != null) {
            for (let index = 0; index < menu.items.length; index++) {
                const item = menu.items[index];
                if (item.label == text) {
                    retVal = index;
                    break;
                }
            }
        }
        return retVal;
    }
    /**
     * Return the menu text from the specified menu position.
     *
     * @param position to query for
     *
     * @return - menu's text; if index is out of range - null is returned
     */
    getMenuText(index: number) {
        const menu = this.Menu.getApplicationMenu();
        if (index >= 0 && index < menu.items.length) {
            return menu.items[index].label;
        }
        return null;
    }
    /**
     * Count menus from the menu bar.
     */
    getMenuCount() {
        const menu = this.Menu.getApplicationMenu();
        if (menu == null) {
            return 0;
        }
        return menu.items.length;
    }
    /**
     * Cleanup the menubar. For MacOS that means to display a minimal menu
     */
    removeAllMenus() {
        if (this.isMacOS) {
            this.mainMenuTemplate = [
                {
                    label: 'DefaultMacMenu',
                    submenu: [
                        { role: 'quit' }
                    ]}
            ];
            this.isMacDefaultMenu = true;
        } else {
            this.mainMenuTemplate = [];
        }
        this.Menu.setApplicationMenu(this.Menu.buildFromTemplate(this.mainMenuTemplate));
    }
    /**
     * Show/hide menubar visibility. This function is working only on Windows/Linux
     */
    setMenuBarVisibility(visible: boolean) {
        this.remote.getCurrentWindow().setMenuBarVisibility(visible); //Windows, Linux
    }
    /**
     * Cleanup the specified menu
     *
     * @param index - menu position
     * @param [itemIndex] - submenu index
     */
    removeAllMenuItems(menuIndex: number, itemIndex: number) {
        if (Number.isInteger(menuIndex) && (menuIndex >= 0 && menuIndex < this.mainMenuTemplate.length)) {
            if (Number.isInteger(itemIndex)) {//submenu wanted
                if (itemIndex >= 0 && itemIndex < (this.mainMenuTemplate[menuIndex].submenu as electron.MenuItemConstructorOptions[]).length) {
                    this.mainMenuTemplate[menuIndex].submenu[itemIndex].submenu = [];
                    this.mainMenuTemplate[menuIndex].submenu[itemIndex].type = 'normal';
                    this.Menu.setApplicationMenu(this.Menu.buildFromTemplate(this.mainMenuTemplate));
                }
            } else {//
                this.mainMenuTemplate[menuIndex].submenu = [];
                if (this.mainMenuTemplate.length === 1 && this.isMacOS) {
                    this.removeAllMenus(); // create default MacOS menu
                } else {
                    this.Menu.setApplicationMenu(this.Menu.buildFromTemplate(this.mainMenuTemplate));
                }
            }
        }
    }
    /**
     * Add separator line to the specified menu
     *
     *
     * @param index - menu index
     * @param [position] - insert position
     * @param [itemIndex] - submenu index; when specified the position is relative to this submenu
     *
     * @return - the index of the added separator
     */
    addSeparator(index: number, position: number, itemIndex: number) {
        const result = this.addMenuItemImpl(index, null, null, null, null, position, itemIndex, 'separator');
        this.Menu.setApplicationMenu(this.Menu.buildFromTemplate(this.mainMenuTemplate));
        return result;
    }
    /**
     * Add menu items to existing menu.
     *
     * @param index - menu index
     * @param text - menuitem text
     * @param callback - callback function to call
     *                   The callback function will receive:
     *                       - text of the clicked item
     *                       - type of the clicked item ("normal", "radio", "checkbox")
     *                       - checked value for checkboxes and radio buttons, otherwise undefined
     * @param [position] - insert position
     * @param [itemIndex] - submenu index; when specified the position is relative to this submenu
     *
     * @return - the index of the added menu item
     *
     * Note: when add an item to an existing menuitem, that menuitem will get from type "normal" to type "submenu".
     *       If previously a callback has been set, that callback will no longer be called
     */
    addMenuItem(index: number, text: string, callback: {formname:string, script: string}, position: number, itemIndex: number) {
        const result = this.addMenuItemImpl(index, text, null, null, callback, position, itemIndex, 'normal');
        this.Menu.setApplicationMenu(this.Menu.buildFromTemplate(this.mainMenuTemplate));
        return result;
    }
    /**
     * Remove menu item from existing menu.
     *
     * @param index - menu index
     * @param position - menuitem position to be removed
     * @param [itemIndex] - submenu index; when specified the position is relative to this submenu
     *
     * Note: when the last item from a submenu it is removed that submenu will get from type "submenu" to type "normal".
     *       If previously a callback has been set for the item, that callback will be called further
     */
    removeMenuItem(index: number, position: number, itemIndex: number) {
        this.Menu.setApplicationMenu(this.Menu.buildFromTemplate(this.removeMenuItemImpl(index, position, itemIndex)));
    }
    /**
     * Count menu items for the specified menu
     *
     * @param index - menu index
     * @param (int) [itemIndex] - submenu index; when specified the submenu items will be count
     */
    getMenuItemsCount(index: number, itemIndex: number) {
        const items = this.Menu.getApplicationMenu().items;
        if ((Number.isInteger(index)) &&
            (index >= 0 && index < items.length)) {
            const menuItem = items[index];
            if (Number.isInteger(itemIndex) && (itemIndex >= 0 && itemIndex < menuItem.submenu.items.length)) {//submenu count desired
                const submenuItem = menuItem.submenu.items[itemIndex];
                if (submenuItem.type == 'submenu') {
                    return submenuItem.submenu.items.length;
                } else {
                    return 0;
                }
            }
            return menuItem.submenu.items.length;
        }
        return -1;
    }


    /**
     * Add checkbox to the specified menu
     *
     *
     * @param index - menu index
     * @param text - checkbox label
     * @param callback - callback function to call
     *                   The callback function will receive:
     *                       - text of the clicked item
     *                       - type of the clicked item ("normal", "radio", "checkbox")
     *                       - checked value for checkboxes and radio buttons, otherwise undefined
     * @param [checked] - checkbox initial status (unchecked by defaul)
     * @param [position] - insert position
     * @param [itemIndex] - submenu index; when specified the position is relative to this submenu
     *
     * @return - the index of the added checkbox
     *
     * Note: when add the checkbox to an existing menuitem, that menuitem will get from type "normal" to type "submenu".
     *       If previously a callback has been set, that callback will no longer be called
     */
    addCheckBox(index: number, text: string, callback: {formname:string, script: string}, checked: boolean, position: number, itemIndex: number) {
        const result = this.addMenuItemImpl(index, text, null, checked, callback, position, itemIndex, 'checkbox');
        this.Menu.setApplicationMenu(this.Menu.buildFromTemplate(this.mainMenuTemplate));
        return result;
    }
    /**
     * Add radio button to the specified menu
     *
     * @param index - menu index
     * @param text - checkbox label
     * @param callback - callback function to call
     *                   The callback function will receive:
     *                       - text of the clicked item
     *                       - type of the clicked item ("normal", "radio", "checkbox")
     *                       - checked value for checkboxes and radio buttons, otherwise undefined
     * @param [selected] - initial selected status
     * @param [position] - insert position
     * @param [itemIndex] - submenu index; when specified the position is relative to this submenu
     *
     * @return - the index of the added radio button
     *
     * Note: when add the checkbox to an existing menuitem, that menuitem will get from type "normal" to type "submenu".
     *       If previously a callback has been set, that callback will no longer be called
     * Note: For the first added radio button in a group, the radio button is selected regardless the selected param
     */
    addRadioButton(index: number, text: string, callback: {formname:string, script: string}, selected:boolean, position: number, itemIndex: number) {
        const result = this.addMenuItemImpl(index, text, null, selected, callback, position, itemIndex, 'radio');
        this.Menu.setApplicationMenu(this.Menu.buildFromTemplate(this.mainMenuTemplate));
        return result;
    }
    /**
     * Add a menuitem with standard native system behavior.
     * For complete allowed value list: https://github.com/Servoy/ngdesktopui
     *
     * @param index - menu index
     * @param role - item role.
     * @param [text] - menuitem text; when not specified the System will provide a standard (localized) one
     * @param [position] - insert position
     * @param [itemIndex] - submenu index; when specified the position is relative to this submenu
     *
     * @return - the index of the added role item
     *
     */
    addRoleItem(index: number, role: ('undo' | 'redo' | 'cut' | 'copy' | 'paste' | 'pasteAndMatchStyle' | 'delete' | 'selectAll' | 'reload' | 'forceReload' | 'toggleDevTools' | 'resetZoom' | 'zoomIn' | 'zoomOut' | 'togglefullscreen' | 'window' | 'minimize' | 'close' | 'help' | 'about' | 'services' | 'hide' | 'hideOthers' | 'unhide' | 'quit' | 'startSpeaking' | 'stopSpeaking' | 'zoom' | 'front' | 'appMenu' | 'fileMenu' | 'editMenu' | 'viewMenu' | 'recentDocuments' | 'toggleTabBar' | 'selectNextTab' | 'selectPreviousTab' | 'mergeAllWindows' | 'clearRecentDocuments' | 'moveTabToNewWindow' | 'windowMenu'),
                         text: string, position: number, itemIndex: number) {
        const result = this.addMenuItemImpl(index, text, role, null, null, position, itemIndex, 'role');
        this.Menu.setApplicationMenu(this.Menu.buildFromTemplate(this.mainMenuTemplate));
        return result;
    }
    /**
     * Get menuitem index from the specified menu
     *
     * @param index - menu index
     * @param text - menuitem text to query for index
     */
    getMenuItemIndexByText(index: number, text: string) {
        const appMenu = this.Menu.getApplicationMenu();
        let retVal = -1;
        if (Number.isInteger(index) && (index >= 0 && index < appMenu.items.length)) {
            const menuitems = appMenu.items[index].submenu.items;
            for (let itemIndex = 0; itemIndex < menuitems.length; itemIndex++) {
                if (menuitems[itemIndex].label == text) {
                    retVal = itemIndex;
                    break;
                }
            }
        }
        return retVal;
    }
    /**
     * Get menuitem text from the specified menu
     *
     * @param index - menu index
     * @param itemIndex - menuitem index to query for text
     */
    getMenuItemText(index: number, itemIndex: number) {
        const appMenu = this.Menu.getApplicationMenu();
        if (Number.isInteger(index) && (index >= 0 && index < appMenu.items.length)) {
            const menuItems = appMenu.items[index].submenu.items;
            if (Number.isInteger(itemIndex) && (itemIndex >= 0 && itemIndex < menuItems.length)) {
                return menuItems[itemIndex].label;
            }
        }
        return null;
    }
    /**
     * Creates a BrowserView (looks like an iframe) and adds this to the current window at the given coordinates with the given width and height.
     * It returns and id that can be used to close/clean up this view later on, or to target that view to inject some javascript.
     *
     * @param x - the X coordinate to position this view
     * @param y - the Y coordinate to position this view
     * @param width - the width of this view
     * @param height - the height of this view
     * @param url - the url to load into this view
     * @return the id to target this view later on.
     */
    createBrowserView(x: number, y: number, width: number, height: number, url: string): string {
        const id = this.browserViewCounter++ + '';
        const view = new this.remote.BrowserView();
        this.browserViews[id] = view;
        this.remote.getCurrentWindow().addBrowserView(view);
        view.setBounds({ x, y, width, height });
        view.webContents.loadURL(url);
        return id;
    }
    /**
     * Closes a and destroys a previously created BrowserView by the given id.
     *
     * @param id - the id of the view to close.
     */
    closeBrowserView(id: string) {
        const view = this.browserViews[id];
        if (view) {
            this.remote.getCurrentWindow().removeBrowserView(view);
            delete this.browserViews[id];
        }
    }
    /**
     * Injects the given javascript into the content of the BrowserView of the given id.
     * The javascript can be a function declaration that is then called later on.
     * The last statement return value is given back to the callback as a first argument.
     * If something goes wrong then the callback is called where the first argument is null and a second argument has the message of the exception.
     *
     * @sample
     * // open google.com<br/>
     * var id = plugins.ngdesktopui.createBrowserView(100,200,700,500,"https://www.google.com/");<br/>
     * // get the value of the search field and return this.<br/>
     * plugins.ngdesktopui.injectJSIntoBrowserView(id, "function test() { return document.getElementsByName('q')[0].value};test();", callback);
     *
     * @param id - the id of the view to execute javascript in.
     * @param js - the piece of javascript that is injected into this view.
     * @param callback - the callback function that is used to get the results or exception if the call fails.
     */
    injectJSIntoBrowserView(id: string, js: string, callback: {formname:string, script: string}) {
        const view = this.browserViews[id];
        if (view) {
            view.webContents.executeJavaScript(js).then((result: any) => {
                if (callback) this.servoyService.executeInlineScript(callback.formname, callback.script, [result]);
            }).catch((e) => {
                if (callback) this.servoyService.executeInlineScript(callback.formname, callback.script, [null, e.message]);
            });
        }
    }

    private addMenuImpl(text: string, index: number) {
        let addResultIndex = -1;
        if (this.isMacDefaultMenu) {
            this.mainMenuTemplate = [];
            this.isMacDefaultMenu = false;
        }
        const myMenu = {
            label: text,
            submenu: []
        };
        if (Number.isInteger(index)) {
            this.mainMenuTemplate.splice(index, 0, myMenu);
            addResultIndex = index;
        } else {
            this.mainMenuTemplate.push(myMenu);
            addResultIndex = this.mainMenuTemplate.length - 1;
        }
        return [this.mainMenuTemplate, addResultIndex];
    }

    private removeMenuItemImpl(menuIndex: number, position: number, itemIndex: number) {
        if (Number.isInteger(menuIndex) && (menuIndex >= 0 && menuIndex < this.mainMenuTemplate.length)) {
            let submenu = this.mainMenuTemplate[menuIndex].submenu as electron.MenuItemConstructorOptions[];
            let isSubmenu = false;
            if (Number.isInteger(itemIndex) && (itemIndex >= 0 && itemIndex < (this.mainMenuTemplate[menuIndex].submenu as electron.MenuItemConstructorOptions[]).length)) {
                submenu = submenu[itemIndex].submenu as electron.MenuItemConstructorOptions[];
                isSubmenu = true;
            }
            if (Number.isInteger(position)) {
                submenu.splice(position, 1);
                if (isSubmenu) {
                    if (submenu.length == 0) {//no submenu items => switch item from submenu to normal
                        this.mainMenuTemplate[menuIndex].submenu[itemIndex].type = 'normal';
                    }
                    this.mainMenuTemplate[menuIndex].submenu[itemIndex].submenu = submenu;
                } else {
                    this.mainMenuTemplate[menuIndex].submenu = submenu;
                }
            }
        }
        return this.mainMenuTemplate;
    }
    private addMenuItemImpl(menuIndex: number, text: string, role: ('undo' | 'redo' | 'cut' | 'copy' | 'paste' | 'pasteAndMatchStyle' | 'delete' | 'selectAll' | 'reload' | 'forceReload' | 'toggleDevTools' | 'resetZoom' | 'zoomIn' | 'zoomOut' | 'togglefullscreen' | 'window' | 'minimize' | 'close' | 'help' | 'about' | 'services' | 'hide' | 'hideOthers' | 'unhide' | 'quit' | 'startSpeaking' | 'stopSpeaking' | 'zoom' | 'front' | 'appMenu' | 'fileMenu' | 'editMenu' | 'viewMenu' | 'recentDocuments' | 'toggleTabBar' | 'selectNextTab' | 'selectPreviousTab' | 'mergeAllWindows' | 'clearRecentDocuments' | 'moveTabToNewWindow' | 'windowMenu'),
                checked: boolean, callback: {formname:string, script: string}, position: number, itemIndex: number, type: ('normal' | 'separator' | 'submenu' | 'checkbox' | 'radio') | 'role'): number {
        let addResultIndex = -1;
        if (Number.isInteger(menuIndex) && (menuIndex >= 0 && menuIndex < this.mainMenuTemplate.length)) {
            let submenu = this.mainMenuTemplate[menuIndex].submenu as electron.MenuItemConstructorOptions[];
            let isSubmenu = false;
            if (Number.isInteger(itemIndex) && (itemIndex >= 0 && itemIndex < (this.mainMenuTemplate[menuIndex].submenu as electron.MenuItemConstructorOptions[]).length)) {
                submenu = submenu[itemIndex].submenu as electron.MenuItemConstructorOptions[];
                isSubmenu = true;
            }
            const myItem: electron.MenuItemConstructorOptions = {};
            if (type === 'role') {
                if (text != null) {
                    myItem.label = text;
                }
                myItem.role = role;
            } else {
                if (text != null) {
                    myItem.label = text;
                }
                if (type != null) {
                    myItem.type = type;
                }
                if (checked != null) {
                    myItem.checked = checked;
                }
                if (callback != null) {
                    myItem.click = (clickedItem) => {
                        this.servoyService.executeInlineScript(callback.formname, callback.script, [clickedItem.label, clickedItem.type, clickedItem.checked]);
                    };
                }
            }
            if (Number.isInteger(position)) {
                if (submenu === undefined) {
                    submenu = [].concat(myItem);
                    addResultIndex = 0;
                } else {
                    if (position < submenu.length) {
                        submenu.splice(position, 0, myItem);
                        addResultIndex = position;
                    } else {
                        submenu = submenu.concat(myItem);
                        addResultIndex = submenu.length - 1;
                    }
                }

                if (isSubmenu) {
                    this.mainMenuTemplate[menuIndex].submenu[itemIndex].type = 'submenu';
                    this.mainMenuTemplate[menuIndex].submenu[itemIndex].submenu = submenu;
                } else {
                    this.mainMenuTemplate[menuIndex].submenu = submenu;
                }
            } else {
                if (isSubmenu) {
                    this.mainMenuTemplate[menuIndex].submenu[itemIndex].submenu = submenu.concat([myItem]);
                    addResultIndex = this.mainMenuTemplate[menuIndex].submenu[itemIndex].submenu.length - 1;
                } else {
                    this.mainMenuTemplate[menuIndex].submenu = submenu.concat([myItem]);
                    addResultIndex = (this.mainMenuTemplate[menuIndex].submenu as electron.MenuItemConstructorOptions[]).length - 1;
                }
            }
        }
        return addResultIndex;
    }
}
