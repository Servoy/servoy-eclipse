import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { SecurityContext } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { ServoyPublicService, TooltipService } from '@servoy/public';

const popperInstances: { update: ReturnType<typeof vi.fn>; destroy: ReturnType<typeof vi.fn> }[] = [];
vi.mock('@popperjs/core', () => ({
    createPopper: vi.fn(() => {
        const inst = { update: vi.fn(), destroy: vi.fn() };
        popperInstances.push(inst);
        return inst;
    })
}));

import { createPopper } from '@popperjs/core';
import { PopupMenuService, Popup, MenuItem } from './popupmenu.service';

describe('PopupMenuService', () => {
    let service: PopupMenuService;
    let servoyService: {
        callServiceServerSideApi: ReturnType<typeof vi.fn>;
        isInTestingMode: ReturnType<typeof vi.fn>;
    };
    let tooltipService: { showTooltip: ReturnType<typeof vi.fn>; hideTooltip: ReturnType<typeof vi.fn> };
    let sanitizer: { sanitize: ReturnType<typeof vi.fn> };

    beforeEach(() => {
        popperInstances.length = 0;
        (createPopper as unknown as ReturnType<typeof vi.fn>).mockClear();
        servoyService = {
            callServiceServerSideApi: vi.fn(),
            isInTestingMode: vi.fn().mockReturnValue(false)
        };
        tooltipService = { showTooltip: vi.fn(), hideTooltip: vi.fn() };
        sanitizer = { sanitize: vi.fn((_ctx: SecurityContext, value: string) => value) };

        TestBed.configureTestingModule({
            providers: [
                PopupMenuService,
                { provide: ServoyPublicService, useValue: servoyService },
                { provide: TooltipService, useValue: tooltipService },
                { provide: DomSanitizer, useValue: sanitizer }
            ]
        });
        service = TestBed.inject(PopupMenuService);
    });

    afterEach(() => {
        // remove any created menus from the body to keep jsdom clean
        document.querySelectorAll('.svy-popup-menu').forEach(el => el.remove());
    });

    const makeItem = (overrides: Partial<MenuItem> = {}): MenuItem => ({
        id: 'id1',
        text: 'Item 1',
        callback: null as any,
        name: 'item1',
        align: 0,
        enabled: true,
        visible: true,
        icon: null as any,
        fa_icon: null as any,
        mnemonic: null as any,
        backgroundColor: null as any,
        foregroundColor: null as any,
        selected: false,
        accelarator: null as any,
        methodArguments: null as any,
        cssClass: null as any,
        items: null as any,
        tooltipText: null as any,
        autoClose: true,
        ...overrides
    });

    const makePopup = (items: MenuItem[], cssClass = ''): Popup => ({ name: 'menu1', cssClass, items });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('initMenu', () => {
        it('should create a dropdown menu element and append it to the body', () => {
            service.initMenu(makePopup([makeItem()]));

            expect(service.menu).toBeTruthy();
            expect(service.menu!.classList.contains('dropdown-menu')).toBe(true);
            expect(service.menu!.classList.contains('svy-popup-menu')).toBe(true);
            expect(service.menu!.style.visibility).toBe('hidden');
            expect(document.body.contains(service.menu)).toBe(true);
        });

        it('should add the popup cssClass to the menu', () => {
            service.initMenu(makePopup([makeItem()], 'my-css'));
            expect(service.menu!.classList.contains('my-css')).toBe(true);
        });

        it('should render an item text sanitized into a span', () => {
            service.initMenu(makePopup([makeItem({ text: 'Hello' })]));
            const span = service.menu!.querySelector('span');
            expect(sanitizer.sanitize).toHaveBeenCalledWith(SecurityContext.HTML, 'Hello');
            expect(span!.innerHTML).toBe('Hello');
        });

        it('should render "no text" when the item has no text', () => {
            service.initMenu(makePopup([makeItem({ text: null as any })]));
            const span = service.menu!.querySelector('span');
            expect(span!.innerHTML).toBe('no text');
        });

        it('should add the disabled class for disabled items', () => {
            service.initMenu(makePopup([makeItem({ enabled: false })]));
            const link = service.menu!.querySelector('a.dropdown-item');
            expect(link!.classList.contains('disabled')).toBe(true);
        });

        it('should add the keep-open class when autoClose is false', () => {
            service.initMenu(makePopup([makeItem({ autoClose: false })]));
            const link = service.menu!.querySelector('a.dropdown-item');
            expect(link!.classList.contains('keep-open')).toBe(true);
        });

        it('should render a divider for a null item', () => {
            service.initMenu(makePopup([null as any]));
            const hr = service.menu!.querySelector('hr.dropdown-divider');
            expect(hr).toBeTruthy();
        });

        it('should skip items with visible === false', () => {
            service.initMenu(makePopup([makeItem({ visible: false }), makeItem({ text: 'Visible' })]));
            const links = service.menu!.querySelectorAll('a.dropdown-item');
            expect(links.length).toBe(1);
        });

        it('should render a fa icon when fa_icon is set', () => {
            service.initMenu(makePopup([makeItem({ fa_icon: 'fas fa-star' })]));
            const icon = service.menu!.querySelector('i');
            expect(icon!.classList.contains('fas')).toBe(true);
            expect(icon!.classList.contains('fa-star')).toBe(true);
        });

        it('should render a checkbox icon for img_checkbox when not selected', () => {
            service.initMenu(makePopup([makeItem({ cssClass: 'img_checkbox', selected: false })]));
            const icon = service.menu!.querySelector('i');
            expect(icon!.classList.contains('fa-square')).toBe(true);
        });

        it('should render a checked checkbox icon for img_checkbox when selected', () => {
            service.initMenu(makePopup([makeItem({ cssClass: 'img_checkbox', selected: true })]));
            const icon = service.menu!.querySelector('i');
            expect(icon!.classList.contains('fa-check-square')).toBe(true);
        });

        it('should render a selected radio icon for img_radio_off when selected', () => {
            service.initMenu(makePopup([makeItem({ cssClass: 'img_radio_off', selected: true })]));
            const icon = service.menu!.querySelector('i');
            expect(icon!.classList.contains('fa-dot-circle')).toBe(true);
        });

        it('should render an image when icon is set', () => {
            service.initMenu(makePopup([makeItem({ icon: 'http://x/y.png' })]));
            const img = service.menu!.querySelector('img') as HTMLImageElement;
            expect(img).toBeTruthy();
            expect(img.src).toContain('y.png');
        });

        it('should apply background and foreground colors', () => {
            service.initMenu(makePopup([makeItem({ backgroundColor: 'rgb(255, 0, 0)', foregroundColor: 'rgb(0, 0, 255)' })]));
            const link = service.menu!.querySelector('a.dropdown-item') as HTMLElement;
            expect(link.style.backgroundColor).toBe('rgb(255, 0, 0)');
            expect(link.style.color).toBe('rgb(0, 0, 255)');
        });

        it('should set data-cy attribute when in testing mode', () => {
            servoyService.isInTestingMode.mockReturnValue(true);
            service.initMenu(makePopup([makeItem({ text: 'Row' })]));
            const menuItem = service.menu!.querySelector('div');
            expect(menuItem!.getAttribute('data-cy')).toBe('svypopupmenu/Row');
        });

        it('should call the server-side api on left mousedown of an enabled item', () => {
            service.initMenu(makePopup([makeItem({ id: 'theId', text: 'Click', callback: {} as any })]));
            const menuItem = service.menu!.querySelector('div')!;
            menuItem.dispatchEvent(new MouseEvent('mousedown', { button: 0, bubbles: true }));
            expect(servoyService.callServiceServerSideApi).toHaveBeenCalledWith('window', 'executeMenuItem', ['theId', 0, -1, false, null, 'Click']);
        });

        it('should not call the server-side api on right mousedown', () => {
            service.initMenu(makePopup([makeItem({ callback: {} as any })]));
            const menuItem = service.menu!.querySelector('div')!;
            menuItem.dispatchEvent(new MouseEvent('mousedown', { button: 2, bubbles: true }));
            expect(servoyService.callServiceServerSideApi).not.toHaveBeenCalled();
        });

        it('should create a nested submenu for items with children', () => {
            const child = makeItem({ text: 'Child' });
            service.initMenu(makePopup([makeItem({ text: 'Parent', items: [child] })]));
            const submenu = document.querySelector('.dropdown-nested-menu');
            expect(submenu).toBeTruthy();
            expect(service.menuItemTosubMenuMap.size).toBe(1);
            expect(service.subMenuToPopperMap.size).toBe(1);
        });

        it('should wire tooltip handlers for items with tooltipText', () => {
            service.initMenu(makePopup([makeItem({ tooltipText: 'help' })]));
            const menuItem = service.menu!.querySelector('div') as HTMLElement;
            menuItem.onmouseenter!(new MouseEvent('mouseenter') as any);
            expect(tooltipService.showTooltip).toHaveBeenCalledWith(expect.anything(), 'help', 750, 5000);
            menuItem.onmouseleave!(new MouseEvent('mouseleave') as any);
            expect(tooltipService.hideTooltip).toHaveBeenCalled();
        });
    });

    describe('getMenuRect', () => {
        it('should return null when no menu is present', () => {
            expect(service.getMenuRect(makePopup([]))).toBeNull();
        });

        it('should return the bounding rect when a menu exists', () => {
            service.initMenu(makePopup([makeItem()]));
            const rect = { x: 1, y: 2 } as DOMRect;
            vi.spyOn(service.menu!, 'getBoundingClientRect').mockReturnValue(rect);
            expect(service.getMenuRect(makePopup([]))).toBe(rect);
        });
    });

    describe('showMenu / showMenuAt', () => {
        it('showMenu should make the menu visible and create a popper', () => {
            service.initMenu(makePopup([makeItem()]));
            service.showMenu(10, 20, false);
            expect(service.menu!.style.visibility).toBe('visible');
            expect(createPopper).toHaveBeenCalled();
        });

        it('showMenuAt should create a popper anchored to the element', () => {
            service.initMenu(makePopup([makeItem()]));
            const el = document.createElement('div');
            service.showMenuAt(el, true);
            expect(service.menu!.style.visibility).toBe('visible');
            expect(createPopper).toHaveBeenCalledWith(el, service.menu, expect.objectContaining({ placement: 'top' }));
        });

        it('showMenuAt should use bottom placement when displayTop is false', () => {
            service.initMenu(makePopup([makeItem()]));
            const el = document.createElement('div');
            service.showMenuAt(el, false);
            expect(createPopper).toHaveBeenCalledWith(el, service.menu, expect.objectContaining({ placement: 'bottom' }));
        });
    });

    describe('initClosePopupHandler', () => {
        it('should register a mousedown listener on the document', () => {
            const addSpy = vi.spyOn(document, 'addEventListener');
            service.initClosePopupHandler(() => { /* noop */ });
            expect(addSpy).toHaveBeenCalledWith('mousedown', expect.any(Function));
            addSpy.mockRestore();
        });

        it('should invoke the handler and clean up the menu on an outside mousedown', () => {
            service.initMenu(makePopup([makeItem()]));
            const handler = vi.fn();
            service.initClosePopupHandler(handler);

            const outside = document.createElement('span');
            document.body.appendChild(outside);
            outside.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));

            expect(handler).toHaveBeenCalledTimes(1);
            expect(document.querySelectorAll('.svy-popup-menu').length).toBe(0);
            document.body.removeChild(outside);
        });

        it('should keep the menu open when mousedown targets the dropdown-menu itself', () => {
            service.initMenu(makePopup([makeItem()]));
            const handler = vi.fn();
            service.initClosePopupHandler(handler);

            service.menu!.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
            expect(handler).not.toHaveBeenCalled();
            // cleanup the lingering listener
            document.body.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
        });

        it('should keep the menu open when mousedown targets a keep-open element', () => {
            service.initMenu(makePopup([makeItem({ autoClose: false })]));
            const handler = vi.fn();
            service.initClosePopupHandler(handler);

            const link = service.menu!.querySelector('a.keep-open') as HTMLElement;
            link.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
            expect(handler).not.toHaveBeenCalled();
            document.body.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
        });

        it('should destroy the menu popper and submenu poppers on close', () => {
            const child = makeItem({ text: 'Child' });
            service.initMenu(makePopup([makeItem({ text: 'Parent', items: [child] })]));
            // creates the main popper
            service.showMenu(1, 2, false);
            const handler = vi.fn();
            service.initClosePopupHandler(handler);

            const outside = document.createElement('span');
            document.body.appendChild(outside);
            outside.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));

            // every created popper should have been destroyed
            popperInstances.forEach(inst => expect(inst.destroy).toHaveBeenCalled());
            expect(service.subMenuToPopperMap.size).toBe(0);
            expect(service.menuItemTosubMenuMap.size).toBe(0);
            document.body.removeChild(outside);
        });
    });

    describe('hover submenu behaviour', () => {
        beforeEach(() => {
            vi.useFakeTimers();
        });
        afterEach(() => {
            vi.useRealTimers();
        });

        it('hovering a menu item with a submenu should show the submenu', () => {
            const child = makeItem({ text: 'Child' });
            service.initMenu(makePopup([makeItem({ text: 'Parent', items: [child] })]));
            const parentMenuItem = Array.from(service.menuItemTosubMenuMap.keys())[0];
            const subMenu = service.menuItemTosubMenuMap.get(parentMenuItem)!;

            service.hoverMenuItemListener({ type: 'mouseenter', target: parentMenuItem } as unknown as MouseEvent);

            expect(subMenu.style.visibility).toBe('visible');
            expect(service.visibleSubMenuPath).toContain(subMenu);
            expect(service.activeMenuItem).toBe(parentMenuItem);
        });

        it('mouseleave on a menu item should mark previous active and schedule a hide pass', () => {
            const grandChild = makeItem({ text: 'GrandChild' });
            const child = makeItem({ text: 'Child', items: [grandChild] });
            service.initMenu(makePopup([makeItem({ text: 'Parent', items: [child] })]));

            const parentMenuItem = Array.from(service.menuItemTosubMenuMap.keys())[0];
            const parentSubMenu = service.menuItemTosubMenuMap.get(parentMenuItem)!;
            // the child menu item lives inside the parent's submenu
            const childMenuItem = Array.from(service.menuItemTosubMenuMap.keys()).find(k => k !== parentMenuItem)!;
            const childSubMenu = service.menuItemTosubMenuMap.get(childMenuItem)!;
            service.activeMenu = service.menu;

            // open parent submenu then the child submenu so the visible path is [menu, parentSub, childSub]
            service.hoverMenuItemListener({ type: 'mouseenter', target: parentMenuItem } as unknown as MouseEvent);
            service.hoverMenuItemListener({ type: 'mouseenter', target: childMenuItem } as unknown as MouseEvent);
            expect(parentSubMenu.style.visibility).toBe('visible');
            expect(childSubMenu.style.visibility).toBe('visible');

            // leaving the child item marks previous active and schedules a hide of everything under its submenu
            service.hoverMenuItemListener({ type: 'mouseleave', target: childMenuItem } as unknown as MouseEvent);
            expect(service.previousActiveMenuItem).toBe(childMenuItem);
            expect(service.activeMenuItem).toBeNull();

            vi.advanceTimersByTime(200);
            // the parent submenu (earlier in the path) stays visible; nothing deeper to hide here
            expect(parentSubMenu.style.visibility).toBe('visible');
        });

        it('hoverMenuListener should track the active menu on enter', () => {
            service.initMenu(makePopup([makeItem()]));
            service.hoverMenuListener({ type: 'mouseenter', target: service.menu! } as unknown as MouseEvent);
            expect(service.activeMenu).toBe(service.menu);
        });

        it('hoverMenuListener should restore active menu to the root menu on leave', () => {
            service.initMenu(makePopup([makeItem()]));
            const other = document.createElement('div');
            service.activeMenu = other;
            service.hoverMenuListener({ type: 'mouseleave', target: other } as unknown as MouseEvent);
            expect(service.activeMenu).toBe(service.menu);
        });
    });
});
