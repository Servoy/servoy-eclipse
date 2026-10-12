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
});
