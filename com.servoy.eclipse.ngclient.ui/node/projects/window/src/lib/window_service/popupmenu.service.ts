import { Inject, Injectable } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { ServoyPublicService, Callback, BaseCustomObject } from '@servoy/public';

@Injectable()
export class PopupMenuService {

    constructor(private servoyService: ServoyPublicService, @Inject(DOCUMENT) private doc: Document) {

    }

    public initClosePopupHandler(handler: () => void) {
        const listener = () => {
            this.doc.querySelectorAll('.svy-popup-menu').forEach(item => {
                item.remove();
            });
            if (handler) {
                handler();
            }
            this.doc.removeEventListener('click', listener);
        };
        this.doc.addEventListener('click', listener);
    }

    public showMenu(x: number, y: number, popup: Popup) {
        this.doc.querySelectorAll('.svy-popup-menu').forEach(item => {
            item.remove();
        });

        const menu = this.doc.createElement('ul');
        menu.style.zIndex = '15000';
        menu.classList.add('dropdown-menu');
        menu.classList.add('svy-popup-menu');
        if (popup.cssClass) menu.classList.add(popup.cssClass);

        this.generateMenuItems(popup.items, menu, false);

        menu.style.left = x + 'px';
        menu.style.top = y + 'px';
        menu.style.display = 'block';
        this.doc.body.appendChild(menu);
    }

    private generateMenuItems(items: Array<MenuItem>, parent: HTMLElement, generateList: boolean): void {
        if (generateList) {
            const ul = this.doc.createElement('ul');
            ul.classList.add('dropdown-menu');
            parent.appendChild(ul);
            parent = ul;
        }
        items.filter(item => !item || item.visible !== false).forEach((item, index) => {

            const li = this.doc.createElement('LI');
            const link = this.doc.createElement('a');
            link.classList.add('dropdown-item');
            li.appendChild(link);
            if (item) {
                if (item.enabled === false) link.classList.add('disabled');
                if (item.callback) {
                    li.addEventListener('click', () => {
                        this.servoyService.callServiceServerSideApi("window","executeMenuItem",[item.id, index, -1, item.selected, null, item.text]);
                    });
                }
                let faicon = item.fa_icon;
                if (item.cssClass === 'img_checkbox') {
                    if (item.selected !== true) {
                        // not selected checkbox
                        faicon = 'far fa-square';
                    } else {
                        // selected checkbox
                        faicon = 'far fa-check-square';
                    }

                }
                if (item.cssClass === 'img_radio_off') {
                    if (item.selected === true) {
                        // selected radio
                        faicon = 'far fa-dot-circle';
                    } else {
                        // not selected radio
                        faicon = 'far fa-circle';
                    }

                }
                if (faicon) {
                    const i = this.doc.createElement('i');
                    i.classList.add(...faicon.split(' '));
                    link.appendChild(i);
                }
                if (item.icon) {
                    const img = this.doc.createElement('img');
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
                const span = this.doc.createElement('span');
                span.textContent = item.text ? item.text : 'no text';
                link.appendChild(span);

                if (item.items) {
                    li.classList.add('dropdown-submenu');
                    this.generateMenuItems(item.items, li, true);
                }
            } else {
                const hr = this.doc.createElement('hr');
                hr.classList.add('dropdown-divider');
                link.appendChild(hr);
            }
            parent.appendChild(li);
        });
    }
}

export class Popup extends BaseCustomObject {
    public name: string;
    public cssClass: string;
    public items: MenuItem[];
}

export class MenuItem extends BaseCustomObject{
    public id: string;
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
    public items: MenuItem[];
}
