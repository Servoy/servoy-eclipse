import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { signal } from '@angular/core';
import { ContextMenuComponent, ContextmenuItem } from './contextmenu.component';

describe('ContextMenuComponent', () => {
  let component: ContextMenuComponent;
  let editorSession: any;
  let editorContentService: any;
  let urlParser: any;

  beforeEach(() => {
    editorSession = {
      packages: signal([]),
      getSelection: vi.fn().mockReturnValue([]),
      setSelection: vi.fn(),
      executeAction: vi.fn(),
      sendChanges: vi.fn(),
      isDirty: vi.fn().mockReturnValue(false),
      sameSize: vi.fn(),
      setCssAnchoring: vi.fn(),
      getAllowedChildrenForContainer: vi.fn().mockReturnValue(null),
      getWizardProperties: vi.fn().mockReturnValue(null),
      getDeveloperMenus: vi.fn().mockReturnValue(null),
      getShortcuts: vi.fn().mockResolvedValue({}),
      getSuperForms: vi.fn().mockResolvedValue([]),
      hasCypressFormTest: vi.fn().mockResolvedValue(false),
      keyPressed: vi.fn(),
      openConfigurator: vi.fn(),
      createComponent: vi.fn(),
      executeDeveloperMenu: vi.fn()
    };
    editorContentService = {
      getContentArea: vi.fn().mockReturnValue({ addEventListener: vi.fn() }),
      getContentElement: vi.fn().mockReturnValue(null),
      getContentForm: vi.fn().mockReturnValue(document.createElement('div')),
      getGlassPane: vi.fn().mockReturnValue({ getBoundingClientRect: vi.fn().mockReturnValue({ left: 0, top: 0 }) }),
      getBodyElement: vi.fn().mockReturnValue({ addEventListener: vi.fn() }),
      getPallete: vi.fn().mockReturnValue({ offsetWidth: 200 }),
      querySelector: vi.fn().mockReturnValue({ offsetHeight: 40 }),
      querySelectorAll: vi.fn().mockReturnValue([])
    };
    urlParser = {
      isAbsoluteFormLayout: vi.fn().mockReturnValue(true),
      isCSSPositionFormLayout: vi.fn().mockReturnValue(false),
      isShowingContainer: vi.fn().mockReturnValue(null),
      isFormComponent: vi.fn().mockReturnValue(false)
    };

    component = Object.create(ContextMenuComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).editorContentService = editorContentService;
    (component as any).urlParser = urlParser;
    (component as any).windowRef = { nativeWindow: { addEventListener: vi.fn(), navigator: { clipboard: { read: vi.fn().mockResolvedValue([]) } } } };
    (component as any).element = { nativeElement: document.createElement('div') };
    (component as any).menuItems = [];
    (component as any).selection = [];
    (component as any).selectionAnchor = 0;
  });

  describe('adjustMenuPosition', () => {
    it('should not throw when called without nativeElement arg', () => {
      editorContentService.querySelector.mockReturnValue(null);
      expect(() => component.adjustMenuPosition()).not.toThrow();
    });
  });

  describe('ContextmenuItem', () => {
    it('should execute function and return false', () => {
      const fn = vi.fn();
      const item = new ContextmenuItem('Test', fn);
      const result = item.execute();
      expect(fn).toHaveBeenCalled();
      expect(result).toBe(false);
    });

    it('should store text and isDevMenu', () => {
      const item = new ContextmenuItem('Dev Item', vi.fn(), true);
      expect(item.text).toBe('Dev Item');
      expect(item.isDevMenu).toBe(true);
    });
  });

  // SVY-21581: on the first right-click near the bottom of the form the menu must
  // flip up / clamp inside the canvas. The flip-up logic reads nativeElement.offsetHeight,
  // which under zoneless OnPush is 0 unless a synchronous cdr.detectChanges() renders the
  // @for items *before* adjustMenuPosition measures. These tests reproduce that first-open
  // scenario and guard the detectChanges()-before-measure ordering in both promise branches.
  describe('first-open bottom-of-form positioning (SVY-21581)', () => {
    // Realistic viewport: a 900px-tall window, menu opened near the bottom.
    const VIEWPORT_HEIGHT = 900;
    const MENU_HEIGHT = 400; // tall, populated menu
    const MENU_WIDTH = 220;
    const CURSOR_TOP = 820; // near the bottom -> bounds.bottom (820+400) overflows the viewport
    const CURSOR_LEFT = 100;
    const TOOLBAR_HEIGHT = 40;
    const STATUSBAR_HEIGHT = 30;

    let originalInnerHeight: number;
    let originalInnerWidth: number;
    let originalPageYOffset: number;
    let originalPageXOffset: number;

    // Builds a stub menu element whose offsetHeight/offsetWidth report `height`/`width`.
    // Setting height to 0 models the pre-render (buggy) state; non-zero models a rendered menu.
    const makeMenuElement = (height: number, width: number): HTMLElement => {
      const el = document.createElement('div');
      Object.defineProperty(el, 'offsetHeight', { configurable: true, get: () => height });
      Object.defineProperty(el, 'offsetWidth', { configurable: true, get: () => width });
      // getBoundingClientRect drives getElementOffset(); report the raw cursor top/left so
      // bounds start at the cursor (pageYOffset is 0 in these tests).
      el.getBoundingClientRect = vi.fn().mockReturnValue({
        top: CURSOR_TOP, left: CURSOR_LEFT, right: CURSOR_LEFT + width, bottom: CURSOR_TOP + height,
        width, height, x: CURSOR_LEFT, y: CURSOR_TOP, toJSON: () => ({})
      }) as unknown as typeof el.getBoundingClientRect;
      // ownerDocument.defaultView.pageYOffset/pageXOffset are 0 in jsdom by default.
      el.style.top = CURSOR_TOP + 'px';
      el.style.left = CURSOR_LEFT + 'px';
      return el;
    };

    beforeEach(() => {
      originalInnerHeight = window.innerHeight;
      originalInnerWidth = window.innerWidth;
      originalPageYOffset = window.pageYOffset;
      originalPageXOffset = window.pageXOffset;
      Object.defineProperty(window, 'innerHeight', { configurable: true, value: VIEWPORT_HEIGHT });
      Object.defineProperty(window, 'innerWidth', { configurable: true, value: 1200 });
      Object.defineProperty(window, 'pageYOffset', { configurable: true, value: 0 });
      Object.defineProperty(window, 'pageXOffset', { configurable: true, value: 0 });

      // toolbar/statusbar/palette measurements used by the clamp math.
      editorContentService.querySelector = vi.fn().mockImplementation((sel: string) => {
        if (sel === '.toolbar-area') return { offsetHeight: TOOLBAR_HEIGHT };
        if (sel === '.statusbar-area') return { offsetHeight: STATUSBAR_HEIGHT };
        return null;
      });
      editorContentService.getPallete = vi.fn().mockReturnValue({ offsetWidth: 200 });
    });

    afterEach(() => {
      Object.defineProperty(window, 'innerHeight', { configurable: true, value: originalInnerHeight });
      Object.defineProperty(window, 'innerWidth', { configurable: true, value: originalInnerWidth });
      Object.defineProperty(window, 'pageYOffset', { configurable: true, value: originalPageYOffset });
      Object.defineProperty(window, 'pageXOffset', { configurable: true, value: originalPageXOffset });
    });

    it('flips the menu up (reduces top) when a non-zero-height menu overflows the bottom', () => {
      const el = makeMenuElement(MENU_HEIGHT, MENU_WIDTH);
      (component as any).element = () => ({ nativeElement: el });

      component.adjustMenuPosition(el);

      const resultTop = parseInt(el.style.top, 10);
      // bounds.bottom (820 + 400 = 1220) > viewport.bottom (900) -> flip up by offsetHeight - 10:
      // top = 820 - (400 - 10) = 430. That is already <= maxTop (900 - 400 - 30 = 470) and
      // >= minTop (0 + 40 = 40), so no further clamp. The menu is flipped up well above the
      // raw cursor top of 820, i.e. fully inside the canvas instead of hanging off the bottom.
      expect(resultTop).toBeLessThan(CURSOR_TOP);
      expect(resultTop).toBe(CURSOR_TOP - (MENU_HEIGHT - 10)); // flip-up = 430
      // and it is inside the clamp window (defensive: proves it is not clipped by the status bar)
      expect(resultTop).toBeLessThanOrEqual(VIEWPORT_HEIGHT - MENU_HEIGHT - STATUSBAR_HEIGHT);
    });

    it('does NOT flip up when height reads as 0 (the pre-render bug this fix prevents)', () => {
      // Models the buggy pre-fix state: adjustMenuPosition measuring before the @for items render.
      const el = makeMenuElement(0, MENU_WIDTH);
      (component as any).element = () => ({ nativeElement: el });

      component.adjustMenuPosition(el);

      const resultTop = parseInt(el.style.top, 10);
      // With offsetHeight 0: bounds.bottom (820) < viewport.bottom (900) so no flip-up, and
      // maxTop = 900 - 0 - 30 = 870 >= 820, so no clamp either -> top stays at the raw cursor.
      // This is exactly the clipped-menu symptom; it proves the test is sensitive to height.
      expect(resultTop).toBe(CURSOR_TOP);
    });

    it('calls detectChanges() BEFORE measuring in the hasCypressFormTest success branch', async () => {
      const el = makeMenuElement(MENU_HEIGHT, MENU_WIDTH);
      (component as any).element = () => ({ nativeElement: el });
      (component as any).menuItems = [new ContextmenuItem('Delete', vi.fn())];

      const callOrder: string[] = [];
      const cdr = {
        markForCheck: vi.fn(),
        detectChanges: vi.fn(() => {
          callOrder.push('detectChanges');
        })
      };
      (component as any).cdr = cdr;
      // show() uses cdr.markForCheck + clipboard; keep it harmless and silent.
      vi.spyOn(component as any, 'show').mockImplementation(() => { /* positioned elsewhere */ });
      vi.spyOn(component, 'adjustMenuPosition').mockImplementation(() => {
        callOrder.push('adjustMenuPosition');
      });

      // success branch: hasCypressFormTest resolves.
      editorContentService.getContentForm = vi.fn().mockReturnValue(document.createElement('div'));
      editorContentService.getContentElement = vi.fn().mockReturnValue(null);
      editorSession.getSelection = vi.fn().mockReturnValue([]);
      editorSession.hasCypressFormTest = vi.fn().mockResolvedValue(false);

      const listeners: ((e: MouseEvent) => void)[] = [];
      const contentArea = {
        addEventListener: vi.fn((type: string, cb: (e: MouseEvent) => void) => {
          if (type === 'contextmenu') listeners.push(cb);
        }),
        querySelector: vi.fn().mockReturnValue(null)
      };
      editorContentService.getContentArea = vi.fn().mockReturnValue(contentArea);
      editorContentService.getBodyElement = vi.fn().mockReturnValue({ addEventListener: vi.fn() });

      (component as any).setup({}, []);
      const event = { preventDefault: vi.fn(), stopPropagation: vi.fn(), pageX: CURSOR_LEFT, pageY: CURSOR_TOP } as unknown as MouseEvent;
      listeners[0](event);
      // flush the hasCypressFormTest().then microtask
      await Promise.resolve();
      await Promise.resolve();

      expect(cdr.detectChanges).toHaveBeenCalled();
      expect(component.adjustMenuPosition).toHaveBeenCalled();
      expect(callOrder).toEqual(['detectChanges', 'adjustMenuPosition']);
    });

    it('calls detectChanges() BEFORE measuring in the hasCypressFormTest .catch branch', async () => {
      const el = makeMenuElement(MENU_HEIGHT, MENU_WIDTH);
      (component as any).element = () => ({ nativeElement: el });
      (component as any).menuItems = [new ContextmenuItem('Delete', vi.fn())];

      const callOrder: string[] = [];
      const cdr = {
        markForCheck: vi.fn(),
        detectChanges: vi.fn(() => {
          callOrder.push('detectChanges');
        })
      };
      (component as any).cdr = cdr;
      vi.spyOn(component as any, 'show').mockImplementation(() => { /* positioned elsewhere */ });
      vi.spyOn(component, 'adjustMenuPosition').mockImplementation(() => {
        callOrder.push('adjustMenuPosition');
      });

      editorContentService.getContentForm = vi.fn().mockReturnValue(document.createElement('div'));
      editorContentService.getContentElement = vi.fn().mockReturnValue(null);
      editorSession.getSelection = vi.fn().mockReturnValue([]);
      // .catch branch: hasCypressFormTest rejects.
      editorSession.hasCypressFormTest = vi.fn().mockRejectedValue(new Error('backend down'));

      const listeners: ((e: MouseEvent) => void)[] = [];
      const contentArea = {
        addEventListener: vi.fn((type: string, cb: (e: MouseEvent) => void) => {
          if (type === 'contextmenu') listeners.push(cb);
        }),
        querySelector: vi.fn().mockReturnValue(null)
      };
      editorContentService.getContentArea = vi.fn().mockReturnValue(contentArea);
      editorContentService.getBodyElement = vi.fn().mockReturnValue({ addEventListener: vi.fn() });

      (component as any).setup({}, []);
      const event = { preventDefault: vi.fn(), stopPropagation: vi.fn(), pageX: CURSOR_LEFT, pageY: CURSOR_TOP } as unknown as MouseEvent;
      listeners[0](event);
      await Promise.resolve();
      await Promise.resolve();

      expect(cdr.detectChanges).toHaveBeenCalled();
      expect(component.adjustMenuPosition).toHaveBeenCalled();
      expect(callOrder).toEqual(['detectChanges', 'adjustMenuPosition']);
    });
  });

  describe('hasSelection', () => {
    it('should return false when there is no selection', () => {
      (component as any).selection = [];
      expect((component as any).hasSelection()).toBe(false);
    });

    it('should return true when there is any selection and no size is required', () => {
      (component as any).selection = ['a', 'b'];
      expect((component as any).hasSelection()).toBe(true);
    });

    it('should return true only when the selection matches the required size', () => {
      (component as any).selection = ['a'];
      expect((component as any).hasSelection(1)).toBe(true);
      expect((component as any).hasSelection(2)).toBe(false);
    });
  });

  describe('isInResponsiveContainer', () => {
    it('should return false when there is no selection', () => {
      (component as any).selection = [];
      expect((component as any).isInResponsiveContainer()).toBe(false);
    });

    it('should return true when a selected node is inside a responsive container', () => {
      (component as any).selection = ['a'];
      const node = { parentElement: { closest: (sel: string) => (sel === '.svy-responsivecontainer' ? {} : null) } };
      editorContentService.getContentElement.mockReturnValue(node);
      expect((component as any).isInResponsiveContainer()).toBe(true);
    });

    it('should return false when no selected node is inside a responsive container', () => {
      (component as any).selection = ['a'];
      const node = { parentElement: { closest: () => null } };
      editorContentService.getContentElement.mockReturnValue(node);
      expect((component as any).isInResponsiveContainer()).toBe(false);
    });
  });

  describe('canDeleteSelection', () => {
    it('should return false when there is no selection', () => {
      (component as any).selection = [];
      expect((component as any).canDeleteSelection()).toBe(false);
    });

    it('should return true for a plain selected node', () => {
      (component as any).selection = ['a'];
      const node = { parentElement: { closest: () => null } };
      editorContentService.getContentElement.mockReturnValue(node);
      expect((component as any).canDeleteSelection()).toBe(true);
    });

    it('should return false when the node is inside a list form component', () => {
      (component as any).selection = ['a'];
      const node = { parentElement: { closest: (sel: string) => (sel === '.svy-listformcomponent' ? {} : null) } };
      editorContentService.getContentElement.mockReturnValue(node);
      expect((component as any).canDeleteSelection()).toBe(false);
    });

    it('should return false when the node is inherited', () => {
      (component as any).selection = ['a'];
      const node = { parentElement: { closest: (sel: string) => (sel === '.inherited_element' ? {} : null) } };
      editorContentService.getContentElement.mockReturnValue(node);
      expect((component as any).canDeleteSelection()).toBe(false);
    });
  });

  describe('isAnchored', () => {
    it('should default to top-left anchoring when selectionAnchor is 0', () => {
      (component as any).selection = ['a'];
      (component as any).selectionAnchor = 0;
      // default becomes 1 + 8 = 9 (top|left); anchor 1 (top) is set
      expect((component as any).isAnchored(1)).toBe(true);
      // anchor 2 (right) is not set in the default
      (component as any).selectionAnchor = 0;
      expect((component as any).isAnchored(2)).toBe(false);
    });

    it('should return false when there is not exactly one selected element', () => {
      (component as any).selection = ['a', 'b'];
      expect((component as any).isAnchored(1)).toBe(false);
    });
  });

  describe('setCssAnchoring', () => {
    it('should delegate to editorSession.setCssAnchoring with the selection', () => {
      (component as any).selection = ['a', 'b'];
      (component as any).setCssAnchoring('t', 'r', 'b', 'l');
      expect(editorSession.setCssAnchoring).toHaveBeenCalledWith(['a', 'b'], { top: 't', right: 'r', bottom: 'b', left: 'l' });
    });

    it('should do nothing when there is no selection', () => {
      (component as any).selection = [];
      (component as any).setCssAnchoring('t', 'r', 'b', 'l');
      expect(editorSession.setCssAnchoring).not.toHaveBeenCalled();
    });
  });

  describe('setAnchoring', () => {
    it('should add an anchor flag not yet present', () => {
      (component as any).selection = ['a'];
      const node = { hasAttribute: () => true, getAttribute: () => '9' }; // top|left
      editorContentService.getContentElement.mockReturnValue(node);
      (component as any).setAnchoring(2, 8); // add right, opposite left
      expect(editorSession.sendChanges).toHaveBeenCalledWith({ a: { anchors: 11 } });
    });

    it('should remove an existing anchor flag and add its opposite', () => {
      (component as any).selection = ['a'];
      const node = { hasAttribute: () => true, getAttribute: () => '9' }; // top|left (1+8)
      editorContentService.getContentElement.mockReturnValue(node);
      (component as any).setAnchoring(8, 2); // remove left(8), add opposite right(2)
      expect(editorSession.sendChanges).toHaveBeenCalledWith({ a: { anchors: 3 } });
    });
  });

  describe('findComponentDisplayName', () => {
    it('should return the matching component display name', () => {
      const comps = [{ name: 'button', displayName: 'Button' }, { name: 'label', displayName: 'Label' }];
      expect((component as any).findComponentDisplayName(comps, 'label')).toBe('Label');
    });

    it('should return null when no component matches', () => {
      expect((component as any).findComponentDisplayName([{ name: 'x', displayName: 'X' }], 'y')).toBeNull();
    });

    it('should return null for an empty list', () => {
      expect((component as any).findComponentDisplayName([], 'x')).toBeNull();
    });
  });

  describe('getDisplayName', () => {
    it('should return a generic label for the component wildcard', () => {
      editorSession.packages.set([{ packageName: 'p', components: [] }]);
      expect((component as any).getDisplayName('component')).toBe('Component [...]');
    });

    it('should return a template label', () => {
      editorSession.packages.set([{ packageName: 'p', components: [] }]);
      expect((component as any).getDisplayName('template')).toBe('Template [...]');
    });

    it('should resolve the display name from the package components', () => {
      editorSession.packages.set([{ packageName: 'bootstrap', components: [{ name: 'button', displayName: 'Bootstrap Button' }] }]);
      expect((component as any).getDisplayName('bootstrap.button')).toBe('Bootstrap Button');
    });

    it('should resolve the display name from a package category', () => {
      editorSession.packages.set([{
        packageName: 'bootstrap',
        components: [],
        categories: { Buttons: [{ name: 'button', displayName: 'Bootstrap Button' }] }
      }]);
      expect((component as any).getDisplayName('bootstrap.button')).toBe('Bootstrap Button');
    });

    it('should fall back to the raw name when nothing matches', () => {
      editorSession.packages.set([]);
      expect((component as any).getDisplayName('unknown.thing')).toBe('unknown.thing');
    });
  });

  describe('convertToContentPoint', () => {
    beforeEach(() => {
      editorContentService.getGlassPane.mockReturnValue({ getBoundingClientRect: () => ({ left: 10, top: 20 }) });
    });

    it('should offset x/y by the glasspane rect', () => {
      const result = (component as any).convertToContentPoint({ x: 100, y: 200 });
      expect(result.x).toBe(90);
      expect(result.y).toBe(180);
    });

    it('should offset top/left by the glasspane rect', () => {
      const result = (component as any).convertToContentPoint({ left: 100, top: 200 });
      expect(result.left).toBe(90);
      expect(result.top).toBe(180);
    });
  });

  describe('getElementOffset', () => {
    it('should add the window page offset to the bounding client rect', () => {
      const el = {
        getBoundingClientRect: () => ({ top: 30, left: 40 }),
        ownerDocument: { defaultView: { pageYOffset: 100, pageXOffset: 200 } }
      } as unknown as HTMLElement;
      const result = (component as any).getElementOffset(el);
      expect(result.top).toBe(130);
      expect(result.left).toBe(240);
    });
  });

  describe('show / hide', () => {
    it('hide should set the menu element display to none', () => {
      const el = document.createElement('div');
      el.style.display = 'block';
      (component as any).element = () => ({ nativeElement: el });
      (component as any).hide();
      expect(el.style.display).toBe('none');
    });

    it('show should position the menu at the cursor and request clipboard', () => {
      const el = document.createElement('div');
      (component as any).element = () => ({ nativeElement: el });
      (component as any).cdr = { markForCheck: vi.fn() };
      const read = vi.fn().mockResolvedValue([]);
      (component as any).windowRef = { nativeWindow: { navigator: { clipboard: { read } } } };
      (component as any).show({ pageX: 40, pageY: 60 } as MouseEvent);
      expect(el.style.display).toBe('block');
      expect(el.style.left).toBe('40px');
      expect(el.style.top).toBe('60px');
      expect(read).toHaveBeenCalled();
    });
  });

  describe('ngOnInit', () => {
    it('should load shortcuts and super forms then set up the menu', async () => {
      const setupSpy = vi.spyOn(component as any, 'setup').mockImplementation(() => undefined);
      editorSession.getShortcuts.mockResolvedValue({ a: 'b' });
      editorSession.getSuperForms.mockResolvedValue(['Base']);
      const addEventListener = vi.fn();
      (component as any).windowRef = { nativeWindow: { addEventListener } };
      component.ngOnInit();
      await Promise.resolve();
      await Promise.resolve();
      expect(setupSpy).toHaveBeenCalledWith({ a: 'b' }, ['Base']);
      expect(addEventListener).toHaveBeenCalledWith('contextmenu', expect.any(Function));
    });
  });

  describe('createItems', () => {
    it('should build the base menu items (Delete, Cut/Copy/Paste, etc.)', () => {
      editorSession.packages.set([]);
      (component as any).createItems({}, []);
      const texts = (component as any).menuItems.map((m: any) => m.text);
      expect(texts).toContain('Delete');
      expect(texts.length).toBeGreaterThan(5);
    });

    // Drive every created menu item (and its submenu items) through their closures so the
    // getItemClass / getIconStyle / onclick branches are exercised.
    const walk = (items: any[], visit: (i: any) => void) => {
      for (const item of items) {
        visit(item);
        if (item.subMenu) walk(item.subMenu, visit);
      }
    };

    it('absolute layout: every menu item closure runs without throwing and onclick fires its action', () => {
      urlParser.isAbsoluteFormLayout.mockReturnValue(true);
      urlParser.isCSSPositionFormLayout.mockReturnValue(false);
      urlParser.isShowingContainer.mockReturnValue('container1');
      editorSession.packages.set([]);
      (component as any).selection = ['id1', 'id2'];
      (component as any).selectionAnchor = 9;
      editorContentService.getContentElement.mockReturnValue({
        parentElement: { closest: () => null },
        classList: { contains: () => false },
        hasAttribute: () => true,
        getAttribute: () => '9'
      });
      const shortcuts = {
        'com.servoy.eclipse.designer.rfb.samewidth': 'Ctrl+W',
        'com.servoy.eclipse.designer.rfb.sameheight': 'Ctrl+H'
      };
      (component as any).createItems(shortcuts, ['Base', 'Child']);

      vi.spyOn(component as any, 'hide').mockImplementation(() => undefined);
      editorSession.sameSize = vi.fn();
      walk((component as any).menuItems, (item) => {
        expect(() => item.getItemClass && item.getItemClass()).not.toThrow();
        expect(() => item.getIconStyle && item.getIconStyle()).not.toThrow();
        // execute() runs the stored action; items with a null action (submenu parents,
        // dividers, the "Add" placeholder) are skipped
        if (!item.subMenu && typeof (item as any).functionToExecute === 'function') {
          expect(() => item.execute()).not.toThrow();
        }
      });

      const texts = (component as any).menuItems.map((m: any) => m.text);
      expect(texts).toContain('Anchoring');
      expect(texts).toContain('Arrange');
      expect(texts).toContain('Delete');
    });

    it('responsive layout: builds Zoom in and exercises its closures', () => {
      urlParser.isAbsoluteFormLayout.mockReturnValue(false);
      urlParser.isShowingContainer.mockReturnValue(null);
      editorSession.packages.set([]);
      (component as any).selection = ['id1'];
      editorContentService.getContentElement.mockReturnValue({
        parentElement: { closest: () => null },
        classList: { contains: () => false }
      });
      (component as any).createItems({}, ['Base']);
      walk((component as any).menuItems, (item) => {
        expect(() => item.getItemClass && item.getItemClass()).not.toThrow();
      });
      const texts = (component as any).menuItems.map((m: any) => m.text);
      expect(texts).toContain('Zoom in');
    });

    it('css position layout: builds the Top/Left style anchoring entries', () => {
      urlParser.isAbsoluteFormLayout.mockReturnValue(true);
      urlParser.isCSSPositionFormLayout.mockReturnValue(true);
      editorSession.packages.set([]);
      (component as any).selection = ['id1'];
      (component as any).createItems({}, []);
      const anchoring = (component as any).menuItems.find((m: any) => m.text === 'Anchoring');
      const subTexts = anchoring.subMenu.map((m: any) => m.text);
      expect(subTexts).toContain('Top/Left');
      expect(subTexts).toContain('Other...');
      // exercise one css-anchoring action
      editorSession.setCssAnchoring = vi.fn();
      anchoring.subMenu[0].execute();
      expect(editorSession.setCssAnchoring).toHaveBeenCalled();
    });
  });
});
