import { Inject, Injectable, SecurityContext, DOCUMENT } from '@angular/core';

import { ServoyPublicService, Callback, BaseCustomObject, TooltipService } from '@servoy/public';
import { createPopper, VirtualElement } from '@popperjs/core';
import { DomSanitizer } from '@angular/platform-browser';
import { timeout } from 'rxjs';

@Injectable()
export class PopupMenuService {

	menu: HTMLElement = null;
	menuPopper: any = null;
	menuItemTosubMenuMap: Map<HTMLElement, HTMLElement> = new Map();
	subMenuToPopperMap: Map<HTMLElement, any> = new Map();

	activeMenu: HTMLElement = null;
	activeMenuItem: HTMLElement = null;
	previousActiveMenuItem: HTMLElement = null;
	visibleSubMenuPath: HTMLElement[] = [];
	hideSubMenusetTimeout: any = null;

	parentNode: HTMLElement[] = [];

	menuZIndex = 15000;

	hoverMenuItemListener = (event: MouseEvent) => {
		const targetElement = event.target as HTMLElement;
		const subMenu = this.menuItemTosubMenuMap.get(targetElement);
		if (event.type == 'mouseenter') {
			if (this.previousActiveMenuItem && this.previousActiveMenuItem !== targetElement) {
				let parent = this.previousActiveMenuItem.parentElement;
				while (parent) {
					if (parent === targetElement.parentElement) {
						this.hideSubMenusOf(this.activeMenu);
						break;
					}
					let parentFound = false;
					this.menuItemTosubMenuMap.forEach((value, key) => {
						if (value === parent) {
							parent = key.parentElement;
							parentFound = true;
						}
					});
					if (!parentFound) parent = null;
				}
				this.previousActiveMenuItem = null;
			}
			this.activeMenuItem = targetElement;
			this.showSubMenu(subMenu);
		} else if (event.type == 'mouseleave') {
			this.previousActiveMenuItem = this.activeMenuItem;
			this.activeMenuItem = null;
			this.hideSubMenus();
		}
	};
	hoverMenuListener = (event: MouseEvent) => {
		if (event.type == 'mouseenter') {
			this.activeMenu = event.target as HTMLElement;
		} else if (event.type == 'mouseleave') {
			if (this.activeMenu !== this.menu) {
				this.activeMenu = this.menu;
			}
			this.hideSubMenus();
		}
	}

	handleMobileDevice = (event: PointerEvent) => {
		const isTouchScreen = (('ontouchstart' in window) || (navigator.maxTouchPoints > 0));
		if (isTouchScreen) {
			const targetElement = (event.target as HTMLElement).closest('div');
			const parentNode = targetElement.parentNode as HTMLElement;
			const prevActiveMenu = this.activeMenu;
			const subMenu = this.menuItemTosubMenuMap.get(targetElement);
			if (subMenu) {
				event.preventDefault();
				this.activeMenu = subMenu;
				if (this.shouldHideMenuMobile(this.activeMenu, prevActiveMenu)) {
					this.hideSubMenuMobile(this.activeMenu);
					const idx = this.parentNode.indexOf(parentNode);
					(idx > -1) && this.parentNode.splice(idx);
					this.activeMenu = this.visibleSubMenuPath.length > 1 ? prevActiveMenu : null;
				} else {
					if (this.visibleSubMenuPath.includes(prevActiveMenu) && this.parentNode.includes(parentNode)) {
						if (this.visibleSubMenuPath.length > 1 && this.visibleSubMenuPath.indexOf(this.activeMenu) === -1) {
							this.hideSubMenuMobile(this.visibleSubMenuPath[this.parentNode.indexOf(parentNode) + 1]);
							this.parentNode.splice(this.parentNode.indexOf(parentNode) + 1);
						}
					}
					this.showSubMenu(this.activeMenu);
					(!this.parentNode.includes(parentNode)) && this.parentNode.push(parentNode);
				}
			} else {
				this.activeMenu = null;
			}
		}
	}

	constructor(private domSanitizer: DomSanitizer, private servoyService: ServoyPublicService, @Inject(DOCUMENT) private doc: Document,private tooltipService: TooltipService) {

	}

	private showSubMenu(subMenu: HTMLElement) {
		const idx = this.visibleSubMenuPath.indexOf(subMenu);
		if (idx === -1) {
			subMenu.style.visibility = 'visible';
			if (subMenu !== this.menu) {
				subMenu.style.zIndex = this.menuZIndex + this.visibleSubMenuPath.length + '';
				if (this.subMenuToPopperMap.has(subMenu)) this.subMenuToPopperMap.get(subMenu).update();
			}
			this.visibleSubMenuPath.push(subMenu);
		}
	}

	private shouldHideMenuMobile(activeMenu: HTMLElement, previousActiveMenu: HTMLElement) {
		if (activeMenu === null || previousActiveMenu === null) return false;
		if ((activeMenu === previousActiveMenu && this.visibleSubMenuPath.includes(activeMenu)) || this.visibleSubMenuPath.includes(activeMenu)) {
			return true;
		}
		return false;
	}

	private hideSubMenuMobile(menu: HTMLElement) {
		const idx = this.visibleSubMenuPath.indexOf(menu);
		if (idx > -1) {
			for (let i = idx; i < this.visibleSubMenuPath.length; i++) {
				this.visibleSubMenuPath[i].style.visibility = 'hidden';
			}
			this.visibleSubMenuPath.splice(idx);
		}
	}

	private hideSubMenus() {
		if (this.hideSubMenusetTimeout) {
			clearTimeout(this.hideSubMenusetTimeout);
		}
		this.hideSubMenusetTimeout = setTimeout(() => {
			this.hideSubMenusetTimeout = null;
			let hideSubmenusOf = this.activeMenu;
			if (this.activeMenuItem && this.menuItemTosubMenuMap.get(this.activeMenuItem)) {
				hideSubmenusOf = this.menuItemTosubMenuMap.get(this.activeMenuItem);
			}
			this.hideSubMenusOf(hideSubmenusOf);
		}, 200);
	}

	private hideSubMenusOf(menu: HTMLElement) {
		const idx = this.visibleSubMenuPath.indexOf(menu);
		if (idx > -1 && idx < this.visibleSubMenuPath.length - 1) {
			for (let i = idx + 1; i < this.visibleSubMenuPath.length; i++) {
				this.visibleSubMenuPath[i].style.visibility = 'hidden';
			}
			this.visibleSubMenuPath.splice(idx + 1);
		}
	}

	public initClosePopupHandler(handler: () => void) {
		const listener = (event: MouseEvent) => {
			// don't dispose if target is the dropdown menu, as we need to keep it open for scrolling
			if (event.target instanceof HTMLDivElement && event.target.classList.contains('dropdown-menu')) return;
			this.hideSubMenusOf(this.menu);
			this.visibleSubMenuPath.splice(0, this.visibleSubMenuPath.length);
			this.parentNode.splice(0, this.parentNode.length);
			this.menuItemTosubMenuMap.clear();
			this.subMenuToPopperMap.forEach((popper) => {
				popper.destroy();
			})
			this.subMenuToPopperMap.clear();
			if (this.menuPopper) this.menuPopper.destroy();
			this.doc.querySelectorAll('.svy-popup-menu').forEach(item => {
				item.remove();
				this.menu = null;
			});
			if (handler) {
				handler();
			}
			this.doc.removeEventListener('mousedown', listener);
		};
		this.doc.addEventListener('mousedown', listener);
	}

	public getMenuRect(popup: Popup) {
		if (this.menu != null) {
			return this.menu.getBoundingClientRect();
		}
		return null;
	}

	public initMenu(popup: Popup) {
		this.menu = this.doc.createElement('div');
		this.menu.style.zIndex = this.menuZIndex + '';
		this.menu.classList.add('dropdown-menu');
		this.menu.classList.add('svy-popup-menu');
		this.menu.style.visibility = 'hidden';
		if (popup.cssClass) this.menu.classList.add(popup.cssClass);

		this.generateMenuItems(popup.items, this.menu, false, 'svypopupmenu');

		this.menu.style.left = 0 + 'px';
		this.menu.style.top = 0 + 'px';
		this.menu.style.display = 'block';

		this.doc.body.appendChild(this.menu);
		this.menu.addEventListener('pointerdown', this.handleMobileDevice);
		this.menu.addEventListener('mouseenter', this.hoverMenuListener);
		this.menu.addEventListener('mouseleave', this.hoverMenuListener);
	}

	public showMenuAt(element: HTMLElement, displayTop: boolean) {
		this.showSubMenu(this.menu);
		this.updateMenuHeight(element.getBoundingClientRect().top, element.getBoundingClientRect().height, this.menu.getBoundingClientRect().height);
		this.menuPopper = createPopper(element, this.menu, {
			placement: (displayTop ? 'top' : 'bottom'),
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

	public showMenu(x: number, y: number, displayTop: boolean) {
		this.showSubMenu(this.menu);
		this.updateMenuHeight(y, 0, this.menu.getBoundingClientRect().height);
		const virtualElement: VirtualElement = {
			getBoundingClientRect: () => {
				return {
					width: 0,
					height: 0,
					top: y,
					right: x,
					bottom: y,
					left: x
				} as DOMRect;
			}
		};
		this.menuPopper = createPopper(virtualElement, this.menu, {
			placement: (displayTop ? 'top' : 'bottom'),
			modifiers: [
				{
					name: 'preventOverflow',
					options: {
						padding: 8,
						rootBoundary: 'document'
					},
				},
				{
					name: 'offset',
					options: {
						offset: ({ placement, reference, popper }) => {
							if (reference.x + popper.width / 2 < this.doc.documentElement.clientWidth) {
								return [popper.width / 2, 0];
							} else {
								return [];
							}
						},
					},
				},
			],
		});
	}

	private generateMenuItems(items: Array<MenuItem>, parent: HTMLElement, generateList: boolean, parentId: string): void {
		if (generateList) {
			const subMenu = this.doc.createElement('div');
			subMenu.classList.add('dropdown-menu');
			subMenu.classList.add('svy-popup-menu');
			subMenu.classList.add('dropdown-nested-menu');
			this.doc.body.appendChild(subMenu);
			this.menuItemTosubMenuMap.set(parent, subMenu);
			this.subMenuToPopperMap.set(subMenu, createPopper(parent, subMenu, {
				placement: 'right-start'
			}));
			parent.addEventListener('mouseenter', this.hoverMenuItemListener);
			parent.addEventListener('mouseleave', this.hoverMenuItemListener);
			subMenu.addEventListener('pointerdown', this.handleMobileDevice);
			subMenu.addEventListener('mouseenter', this.hoverMenuListener);
			subMenu.addEventListener('mouseleave', this.hoverMenuListener);
			parent = subMenu;
		}
		items.filter(item => !item || item.visible !== false).forEach((item, index) => {

			const menuItem = this.doc.createElement('div');
			const link = this.doc.createElement('a');
			link.classList.add('dropdown-item');
			menuItem.appendChild(link);
			if (item) {
				if (item.enabled === false) link.classList.add('disabled');
				if (item.callback && item.enabled !== false) {
					menuItem.addEventListener('mousedown', (event) => {
						if (event.button == 0) this.servoyService.callServiceServerSideApi("window", "executeMenuItem", [item.id, index, -1, item.selected, null, item.text]);
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
				span.innerHTML = item.text ? this.domSanitizer.sanitize(SecurityContext.HTML, item.text) : 'no text';
				link.appendChild(span);
				const menuItemId = parentId + '/' + span.innerHTML;
				if (this.servoyService.isInTestingMode()) {
					menuItem.setAttribute('data-cy', menuItemId);
				}
				if (item.tooltipText) {
					menuItem.onmouseenter = (jsEvent: MouseEvent) => {
						this.tooltipService.showTooltip(jsEvent, item.tooltipText, 750, 5000);
					};
					menuItem.onmouseleave = (jsEvent: MouseEvent) => {
						this.tooltipService.hideTooltip()
					};
				}
				if (item.items) {
					menuItem.classList.add('dropdown-submenu');
					this.generateMenuItems(item.items, menuItem, true, menuItemId);
				}
			} else {
				const hr = this.doc.createElement('hr');
				hr.classList.add('dropdown-divider');
				link.appendChild(hr);
			}
			parent.appendChild(menuItem);
		});
	}

	private updateMenuHeight(elementTop: number, elementHeight: number, menuHeight: number) {
		const topValue = elementTop;
		const bottomValue = window.innerHeight - (elementTop + elementHeight);
		if (menuHeight >= topValue && menuHeight >= bottomValue) {
			if (topValue >= bottomValue) {
				this.menu.style.maxHeight = `${topValue - 10}px`;
			}
			else {
				this.menu.style.maxHeight = `${bottomValue - 10}px`;
			}
			this.menu.style.overflow = 'auto';
		}
	}
}

export class Popup extends BaseCustomObject {
	public name: string;
	public cssClass: string;
	public items: MenuItem[];
}

export class MenuItem extends BaseCustomObject {
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
	public tooltipText: string;
}
