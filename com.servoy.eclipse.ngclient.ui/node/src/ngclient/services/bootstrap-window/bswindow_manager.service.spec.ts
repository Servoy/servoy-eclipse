import { describe, it, expect, afterEach, vi } from 'vitest';

import { BSWindowManager } from './bswindow_manager.service';
import { BSWindow } from './bswindow';

/**
 * Pure-logic unit tests for BSWindowManager. The manager is almost entirely
 * list/stack bookkeeping plus a few renderer/DOM side effects, so we construct
 * it directly (bypassing Angular DI) and inject fake collaborators.
 */

interface FakeRenderer {
  listen: ReturnType<typeof vi.fn>;
  setStyle: ReturnType<typeof vi.fn>;
  addClass: ReturnType<typeof vi.fn>;
  removeClass: ReturnType<typeof vi.fn>;
  appendChild: ReturnType<typeof vi.fn>;
  setAttribute: ReturnType<typeof vi.fn>;
  removeAttribute: ReturnType<typeof vi.fn>;
}

const makeRenderer = (): FakeRenderer => ({
  listen: vi.fn(() => () => {
    /* unregister noop */
  }),
  setStyle: vi.fn(),
  addClass: vi.fn(),
  removeClass: vi.fn(),
  appendChild: vi.fn(),
  setAttribute: vi.fn(),
  removeAttribute: vi.fn(),
});

/**
 * Build a BSWindowManager without running the real constructor (which calls
 * inject()). We assign the private collaborators directly and then invoke the
 * constructor body's essential setup manually.
 */
/** Build a detached DOM element with the structure BSWindow.initialize expects. */
const buildTemplate = (): HTMLElement => {
  const el = document.createElement('div');
  el.innerHTML = '<div class="window-header"><span class="window-title"></span></div><div class="window-body"></div><div class="window-footer"></div>';
  return el;
};

const makeManager = (opts?: {
  doc?: Document;
  mainBody?: HTMLElement;
  renderer?: FakeRenderer;
  deepExtend?: (args: any[]) => any;
}): { mgr: BSWindowManager; renderer: FakeRenderer; mainBody: HTMLElement } => {
  const renderer = opts?.renderer ?? makeRenderer();
  const doc = opts?.doc ?? document;
  const mainBody = opts?.mainBody ?? document.createElement('div');

  const utils = {
    getMainBody: () => mainBody,
    deepExtend: opts?.deepExtend ?? ((args: any[]) => Object.assign({}, ...args.filter((a) => typeof a !== 'boolean'))),
  };

  const mgr = Object.create(BSWindowManager.prototype) as BSWindowManager;
  (mgr as any).zIndexIncrement = 100;
  (mgr as any).windows = [];
  (mgr as any).modalStack = [];
  (mgr as any).doc = doc;
  (mgr as any).rendererFactory = { createRenderer: () => renderer };
  (mgr as any).utils = utils;
  (mgr as any).windowRefService = { nativeWindow: window };
  (mgr as any).renderer = renderer;
  // setNextFocused is a class-field arrow function, not on the prototype, so
  // recreate it on this Object.create()-based instance.
  (mgr as any).setNextFocused = function (this: BSWindowManager) {
    this.setFocused(this.windows[this.windows.length - 1]);
  };
  mgr.initialize({});

  return { mgr, renderer, mainBody };
};

/** A lightweight fake BSWindow implementing just the surface the manager touches. */
const makeFakeWindow = (id: any = '', isModal = false): any => {
  const element = document.createElement('div');
  element.style.zIndex = '0';
  return {
    id,
    element,
    options: { isModal },
    blocker: undefined as any,
    tab: undefined as any,
    getElement: vi.fn(() => element),
    setIndex: vi.fn((idx: number) => {
      element.style.zIndex = String(idx);
    }),
    setActive: vi.fn(),
    getBlocker: vi.fn(function (this: any) {
      return this.blocker;
    }),
    setManager: vi.fn(),
    getWindowTab: vi.fn(function (this: any) {
      return this.tab;
    }),
    setWindowTab: vi.fn(),
    getSticky: vi.fn(() => false),
    close: vi.fn(),
  };
};

describe('BSWindowManager', () => {
  afterEach(() => {
    vi.useRealTimers();
    // clean up any backdrops left in the real document
    document.querySelectorAll('.modal-backdrop').forEach((el) => el.remove());
  });

  describe('findWindowByID', () => {
    it('returns the matching window', () => {
      const { mgr } = makeManager();
      const w1 = makeFakeWindow('a');
      const w2 = makeFakeWindow('b');
      mgr.windows.push(w1, w2);
      expect(mgr.findWindowByID('b')).toBe(w2);
    });

    it('returns null when no window matches', () => {
      const { mgr } = makeManager();
      mgr.windows.push(makeFakeWindow('a'));
      expect(mgr.findWindowByID('zzz')).toBeNull();
    });
  });

  describe('initialize', () => {
    it('adds the window-pane class to matching container elements', () => {
      const container = document.createElement('div');
      container.className = 'my-container';
      document.body.appendChild(container);
      try {
        const { mgr, renderer } = makeManager();
        mgr.initialize({ container: '.my-container' });
        expect(renderer.addClass).toHaveBeenCalledWith(container, 'window-pane');
      } finally {
        container.remove();
      }
    });

    it('creates a modal backdrop element when no backdrop supplied', () => {
      const { mgr } = makeManager();
      expect(mgr.options.modalBackdrop).toBeInstanceOf(HTMLElement);
      expect(mgr.options.modalBackdrop.getAttribute('class')).toBe('modal-backdrop fade');
      expect(mgr.options.modalBackdrop.getAttribute('style')).toBe('z-index: 899');
    });

    it('does not create a backdrop when one is provided', () => {
      const { mgr } = makeManager();
      mgr.initialize({ backdrop: true });
      expect(mgr.options.modalBackdrop).toBeUndefined();
    });
  });

  describe('resortWindows', () => {
    it('assigns ascending z-index starting at 900', () => {
      const { mgr } = makeManager();
      const w1 = makeFakeWindow('a');
      const w2 = makeFakeWindow('b');
      const w3 = makeFakeWindow('c');
      mgr.windows.push(w1, w2, w3);
      mgr.resortWindows();
      expect(w1.setIndex).toHaveBeenCalledWith(900);
      expect(w2.setIndex).toHaveBeenCalledWith(1000);
      expect(w3.setIndex).toHaveBeenCalledWith(1100);
    });

    it('updates the modal-backdrop z-index to one below the top modal window', () => {
      const backdrop = document.createElement('div');
      backdrop.className = 'modal-backdrop';
      document.body.appendChild(backdrop);
      try {
        const { mgr, renderer } = makeManager();
        const w1 = makeFakeWindow('a');
        mgr.windows.push(w1);
        mgr.modalStack.push(w1);
        mgr.resortWindows();
        // w1 got zIndex 900 from the resort loop
        expect(renderer.setStyle).toHaveBeenCalledWith(backdrop, 'z-index', 899);
      } finally {
        backdrop.remove();
      }
    });

    it('does not touch backdrop styles when no modal is open', () => {
      const { mgr, renderer } = makeManager();
      mgr.windows.push(makeFakeWindow('a'));
      mgr.resortWindows();
      expect(renderer.setStyle).not.toHaveBeenCalled();
    });
  });

  describe('setFocused', () => {
    it('deactivates all windows then activates and moves the focused one to the end', () => {
      const { mgr } = makeManager();
      const w1 = makeFakeWindow('a');
      const w2 = makeFakeWindow('b');
      const w3 = makeFakeWindow('c');
      mgr.windows.push(w1, w2, w3);
      mgr.setFocused(w1);
      expect(w1.setActive).toHaveBeenCalledWith(false);
      expect(w1.setActive).toHaveBeenLastCalledWith(true);
      expect(w2.setActive).toHaveBeenLastCalledWith(false);
      expect(w3.setActive).toHaveBeenLastCalledWith(false);
      // w1 moved to the end
      expect(mgr.windows).toEqual([w2, w3, w1]);
    });

    it('follows the blocker chain to focus the blocking window', () => {
      const { mgr } = makeManager();
      const base = makeFakeWindow('base');
      const blocker = makeFakeWindow('blocker');
      base.blocker = blocker;
      mgr.windows.push(base, blocker);
      mgr.setFocused(base);
      expect(blocker.setActive).toHaveBeenLastCalledWith(true);
      expect(base.setActive).toHaveBeenLastCalledWith(false);
      expect(mgr.windows[mgr.windows.length - 1]).toBe(blocker);
    });

    it('does not reorder when the focused window is on the modal stack', () => {
      const { mgr } = makeManager();
      const w1 = makeFakeWindow('a');
      const w2 = makeFakeWindow('b');
      mgr.windows.push(w1, w2);
      mgr.modalStack.push(w2);
      mgr.setFocused(w2);
      expect(mgr.windows).toEqual([w1, w2]);
      expect(w2.setActive).toHaveBeenLastCalledWith(true);
    });
  });

  describe('sendToBack', () => {
    it('moves the window to index 0 when there are no modals', () => {
      const { mgr } = makeManager();
      const w1 = makeFakeWindow('a');
      const w2 = makeFakeWindow('b');
      const w3 = makeFakeWindow('c');
      mgr.windows.push(w1, w2, w3);
      mgr.sendToBack(w3);
      expect(mgr.windows[0]).toBe(w3);
    });

    it('inserts the window just after the top modal window', () => {
      const { mgr } = makeManager();
      const modal = makeFakeWindow('m');
      const w1 = makeFakeWindow('a');
      const w2 = makeFakeWindow('b');
      mgr.windows.push(modal, w1, w2);
      mgr.modalStack.push(modal);
      mgr.sendToBack(w2);
      // modal stays first; w2 slots directly after the modal
      expect(mgr.windows[0]).toBe(modal);
      expect(mgr.windows[1]).toBe(w2);
    });
  });

  describe('setNextFocused', () => {
    it('focuses the last window in the stack', () => {
      const { mgr } = makeManager();
      const w1 = makeFakeWindow('a');
      const w2 = makeFakeWindow('b');
      mgr.windows.push(w1, w2);
      const spy = vi.spyOn(mgr, 'setFocused');
      mgr.setNextFocused();
      expect(spy).toHaveBeenCalledWith(w2);
    });
  });

  describe('addWindow', () => {
    it('pushes the window, sets the manager and focuses it', () => {
      const { mgr } = makeManager();
      const w1 = makeFakeWindow('a');
      const result = mgr.addWindow(w1);
      expect(result).toBe(w1);
      expect(mgr.windows).toContain(w1);
      expect(w1.setManager).toHaveBeenCalledWith(mgr);
      expect(w1.setActive).toHaveBeenLastCalledWith(true);
    });

    it('registers focused and close listeners on the element', () => {
      const { mgr, renderer } = makeManager();
      const w1 = makeFakeWindow('a');
      mgr.addWindow(w1);
      const events = renderer.listen.mock.calls.map((c) => c[1]);
      expect(events).toContain('focused');
      expect(events).toContain('close');
    });

    it('creates a window tab and appends it to the container', () => {
      const container = document.createElement('div');
      const { mgr } = makeManager();
      // addWindow treats options.container as a DOM element (appendChild), so set
      // it directly rather than through initialize() (which uses it as a selector).
      mgr.options.container = container;
      const w1 = makeFakeWindow('a');
      // capture the tab created for this window
      let createdTab: HTMLElement | undefined;
      w1.setWindowTab = vi.fn((tab: HTMLElement) => {
        createdTab = tab;
        w1.tab = tab;
      });
      mgr.addWindow(w1);
      expect(createdTab).toBeInstanceOf(HTMLElement);
      expect(container.contains(createdTab!)).toBe(true);
    });
  });

  describe('createWindow', () => {
    it('merges options via deepExtend, creates a BSWindow and adds it', () => {
      const { mgr } = makeManager({ deepExtend: (args: any[]) => Object.assign({}, ...args.filter((a) => typeof a !== 'boolean')) });
      const template = buildTemplate();
      const win = mgr.createWindow({ template, title: 'Hello' });
      expect(win).toBeInstanceOf(BSWindow);
      expect(mgr.windows).toContain(win);
    });

    it('falls back to the manager windowTemplate when the options have none', () => {
      const template = buildTemplate();
      const { mgr } = makeManager();
      mgr.initialize({ windowTemplate: template });
      const spy = vi.spyOn((mgr as any).utils, 'deepExtend');
      mgr.createWindow({ title: 'x' });
      // the first merged object (after the boolean) should carry the template
      const mergedArgs = spy.mock.calls[0][0] as any[];
      expect(mergedArgs[1].template).toBe(template);
    });

    it('adds a modal to the modal stack when options.isModal is set', () => {
      const template = buildTemplate();
      const { mgr } = makeManager();
      const win = mgr.createWindow({ template, isModal: true, title: 'm' });
      expect(mgr.modalStack).toContain(win);
    });
  });

  describe('addModal / removeModal', () => {
    it('appends the backdrop to the main body and pushes to the modal stack', () => {
      vi.useFakeTimers();
      const mainBody = document.createElement('div');
      const { mgr } = makeManager({ mainBody });
      const w1 = makeFakeWindow('a', true);
      w1.options.modalBackdrop = mgr.options.modalBackdrop;
      mgr.addModal(w1);
      expect(mainBody.contains(mgr.options.modalBackdrop)).toBe(true);
      expect(mgr.modalStack).toContain(w1);
    });

    it('adds the "in" class to the backdrop after the 50ms timeout', () => {
      vi.useFakeTimers();
      const backdrop = document.createElement('div');
      backdrop.className = 'modal-backdrop';
      document.body.appendChild(backdrop);
      try {
        const { mgr, renderer } = makeManager();
        const w1 = makeFakeWindow('a', true);
        w1.options.modalBackdrop = mgr.options.modalBackdrop;
        mgr.addModal(w1);
        expect(renderer.addClass).not.toHaveBeenCalledWith(backdrop, 'in');
        vi.advanceTimersByTime(50);
        expect(renderer.addClass).toHaveBeenCalledWith(backdrop, 'in');
      } finally {
        backdrop.remove();
      }
    });

    it('does not re-append the backdrop for a second modal', () => {
      vi.useFakeTimers();
      const mainBody = document.createElement('div');
      const appendSpy = vi.spyOn(mainBody, 'appendChild');
      const { mgr } = makeManager({ mainBody });
      const w1 = makeFakeWindow('a', true);
      const w2 = makeFakeWindow('b', true);
      w1.options.modalBackdrop = mgr.options.modalBackdrop;
      w2.options.modalBackdrop = mgr.options.modalBackdrop;
      mgr.addModal(w1);
      appendSpy.mockClear();
      mgr.addModal(w2);
      expect(appendSpy).not.toHaveBeenCalled();
      expect(mgr.modalStack).toEqual([w1, w2]);
    });

    it('removeModal splices from the stack and schedules backdrop removal', () => {
      vi.useFakeTimers();
      const backdrop = document.createElement('div');
      backdrop.className = 'modal-backdrop';
      document.body.appendChild(backdrop);
      try {
        const { mgr, renderer } = makeManager();
        const w1 = makeFakeWindow('a', true);
        mgr.modalStack.push(w1);
        mgr.removeModal(w1);
        expect(mgr.modalStack).not.toContain(w1);
        expect(renderer.removeClass).toHaveBeenCalledWith(backdrop, 'in');
        // backdrop still present before the 50ms timer
        expect(document.body.contains(backdrop)).toBe(true);
        vi.advanceTimersByTime(50);
        expect(document.body.contains(backdrop)).toBe(false);
      } finally {
        backdrop.remove();
      }
    });

    it('removeModal does nothing when the window is not on the stack', () => {
      const { mgr, renderer } = makeManager();
      const w1 = makeFakeWindow('a', true);
      mgr.removeModal(w1);
      expect(renderer.removeClass).not.toHaveBeenCalled();
    });

    it('keeps the backdrop when other modals remain on the stack', () => {
      vi.useFakeTimers();
      const backdrop = document.createElement('div');
      backdrop.className = 'modal-backdrop';
      document.body.appendChild(backdrop);
      try {
        const { mgr, renderer } = makeManager();
        const w1 = makeFakeWindow('a', true);
        const w2 = makeFakeWindow('b', true);
        mgr.modalStack.push(w1, w2);
        mgr.removeModal(w1);
        expect(mgr.modalStack).toEqual([w2]);
        expect(renderer.removeClass).not.toHaveBeenCalledWith(backdrop, 'in');
      } finally {
        backdrop.remove();
      }
    });
  });

  describe('destroyWindow', () => {
    it('removes the window from the list and resorts the remaining ones', () => {
      const { mgr } = makeManager();
      const w1 = makeFakeWindow('a');
      const w2 = makeFakeWindow('b');
      mgr.windows.push(w1, w2);
      const resortSpy = vi.spyOn(mgr, 'resortWindows');
      mgr.destroyWindow(w1);
      expect(mgr.windows).toEqual([w2]);
      expect(resortSpy).toHaveBeenCalled();
    });

    it('calls removeModal for a modal window', () => {
      const { mgr } = makeManager();
      const w1 = makeFakeWindow('a', true);
      mgr.windows.push(w1);
      mgr.modalStack.push(w1);
      const removeModalSpy = vi.spyOn(mgr, 'removeModal');
      mgr.destroyWindow(w1);
      expect(removeModalSpy).toHaveBeenCalledWith(w1);
      expect(mgr.windows).toEqual([]);
    });
  });
});
