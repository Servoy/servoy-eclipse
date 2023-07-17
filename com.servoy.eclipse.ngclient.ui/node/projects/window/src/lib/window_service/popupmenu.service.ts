import { Inject, Injectable, SecurityContext } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { ServoyPublicService, Callback, BaseCustomObject } from '@servoy/public';
import { createPopper } from '@popperjs/core';
import { DomSanitizer } from '@angular/platform-browser';

@Injectable()
export class PopupMenuService {

    menu: HTMLElement = null;

    constructor(private domSanitizer: DomSanitizer, private servoyService: ServoyPublicService, @Inject(DOCUMENT) private doc: Document) {

    }

    public initClosePopupHandler(handler: () => void) {
        const listener = () => {
            this.doc.querySelectorAll('.svy-popup-menu').forEach(item => {
                item.remove();
                this.menu = null;
            });
            if (handler) {
                handler();
            }
            this.doc.removeEventListener('mouseup', listener);
        };
        this.doc.addEventListener('mouseup', listener);
    }

    public getMenuRect(popup: Popup) {
        if (this.menu != null) {
            return this.menu.getBoundingClientRect();
        }
        return null;
    }

    public initMenu(popup: Popup) {
        this.menu = this.doc.createElement('ul');
        this.menu.style.zIndex = '15000';
        this.menu.classList.add('dropdown-menu');
        this.menu.classList.add('svy-popup-menu');
        this.menu.style.visibility = 'hidden';
        if (popup.cssClass)this. menu.classList.add(popup.cssClass);

        this.generateMenuItems(popup.items, this.menu, false);

        this.menu.style.left = 0 + 'px';
        this.menu.style.top = 0 + 'px';
        this.menu.style.display = 'block';

        this.doc.body.appendChild(this.menu);
    }

    public showMenuAt(element: HTMLElement) {
        this.menu.style.visibility = 'visible';
        createPopper(element, this.menu, {
            modifiers: [
                {
                  name: 'preventOverflow',
                  options: {
                    padding: 8,
                    rootBoundary: 'document'
                  },
                },
              ],
          });
    }

    public showMenu(x: number, y: number) {
        this.menu.style.left = x + 'px';
        this.menu.style.top = y + 'px';
        this.menu.style.visibility = 'visible';
        this.doc.body.appendChild(this.menu);
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
                    li.addEventListener('mouseup', (event) => {
                        if (event.button == 0) this.servoyService.callServiceServerSideApi("window","executeMenuItem",[item.id, index, -1, item.selected, null, item.text]);
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
                span.innerHTML= item.text ? this.domSanitizer.sanitize(SecurityContext.HTML, item.text) : 'no text';
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
