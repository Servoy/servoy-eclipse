import { Inject, Injectable } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { ServoyService } from '../ngclient/servoy.service';

@Injectable()
export class PopupMenuService {

    constructor(private servoyService: ServoyService, @Inject(DOCUMENT) private document: Document) {

    }

    public initClosePopupHandler(handler: () => void) {
        const listener = () => {
            document.querySelectorAll('.svy-popup-menu').forEach(item => {
                item.remove();
            });
            if (handler) {
                handler();
            }
            document.removeEventListener('click', listener);
        };
        document.addEventListener('click', listener);
    }

    public showMenu(x: number, y: number, popup: Popup) {
        document.querySelectorAll('.svy-popup-menu').forEach(item => {
            item.remove();
        });

        let menu = this.document.createElement('ul');
        menu.style.zIndex = '15000';
        menu.classList.add('dropdown-menu');
        menu.classList.add('svy-popup-menu');
        if (popup.cssClass) menu.classList.add(popup.cssClass);

        this.generateMenuItems(popup.items, menu, false);

        menu.style.left = x + 'px';
        menu.style.top = y + 'px';
        menu.style.display = 'block';
        document.body.appendChild(menu);
    }

    private generateMenuItems(items: Array<MenuItem>, parent: HTMLElement, generateList: boolean): void {
        if (generateList) {
            const ul = this.document.createElement('ul');
            ul.classList.add('dropdown-menu');
            parent.appendChild(ul);
            parent = ul;
        }
        items.filter(item => { return !item || item.visible != false }).forEach((item, index) => {

            const li = document.createElement('LI');
            const link = document.createElement('a');
            link.classList.add('dropdown-item');
            li.appendChild(link);
            if (item) {
                if (item.enabled == false) link.classList.add('disabled');
                if (item.callback) {
                    li.addEventListener('click', () => {
                        let args = [index, -1, item.selected, null, item.text];
                        if (item.methodArguments && item.methodArguments.length) {
                            args = args.concat(item.methodArguments);
                        }
                        this.servoyService.executeInlineScript(item.callback.formname, item.callback.script, args);
                    });
                }
                let faicon = item.fa_icon;
                if (item.cssClass == 'img_checkbox') {
                    if (item.selected != true) {
                        // not selected checkbox
                        faicon = 'far fa-square';
                    }
                    else {
                        // selected checkbox
                        faicon = 'far fa-check-square';
                    }

                }
                if (item.cssClass == 'img_radio_off') {
                    if (item.selected == true) {
                        // selected radio
                        faicon = 'far fa-dot-circle';
                    }
                    else {
                        // not selected radio
                        faicon = 'far fa-circle';
                    }

                }
                if (faicon) {
                    const i = document.createElement('i');
                    i.classList.add(...faicon.split(' '));
                    link.appendChild(i);
                }
                if (item.icon) {
                    const img = document.createElement('img');
                    img.src = item.icon;
                    img.style.border = 'none';
                    link.appendChild(img);
                }
                if (item.backgroundColor) {
                    link.style.backgroundColor = item.backgroundColor;
                }
                if (item.foregroundColor) {
                    link.style.color = item.foregroundColor;
                }
                const span = document.createElement('span');
                span.textContent = item.text ? item.text : 'no text';
                link.appendChild(span);

                if (item.items) {
                    li.classList.add('dropdown-submenu');
                    this.generateMenuItems(item.items, li, true);
                }
            }
            else {
                const hr = document.createElement('hr');
                hr.classList.add('dropdown-divider');
                link.appendChild(hr);
            }
            parent.appendChild(li);
        });
    }
}

export class Popup {
    public name: string;
    public cssClass: string;
    public items: MenuItem[];
}

export class MenuItem {
    public text: string;
    public callback: Callback;
    public name: string;
    public align: number;
    public enabled: boolean;
    public visible: boolean;
    public icon: string;
    public fa_icon: string;
    public mnemonic: string;
    public backgroundColor: string;
    public foregroundColor: string;
    public selected: boolean;
    public accelarator: string;
    public methodArguments: Array<any>;
    public cssClass: string;
    public items: MenuItem[]
}

export class Callback {
    public formname: string;
    public script: string;
}