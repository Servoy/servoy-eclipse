import { describe, it, expect, afterEach, vi } from 'vitest';

import { BSWindow } from './bswindow';

/**
 * Pure-logic unit tests for BSWindow. jsdom has no layout engine, so the
 * drag/resize mouse-geometry handlers (initHandlers mousedown/mousemove/mouseup,
 * centerWindow, setSize, resize) cannot be meaningfully tested here and are left
 * for browser tests. These tests cover the option parsing, lifecycle, state
 * accessors, active/close/blocker/parent behaviour and the blink timer.
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

/** Build a detached DOM element with the structure initialize() expects. */
const buildTemplate = (): HTMLElement => {
  const el = document.createElement('div');
  el.innerHTML =
    '<div class="window-header"><span class="window-title"></span></div>' + '<div class="window-body"></div>' + '<div class="window-footer"></div>';
  return el;
};

/**
 * Construct a BSWindow bypassing Angular DI, with fake collaborators. Returns
 * the window plus the fakes so tests can assert on them.
 */
const makeWindow = (opts?: {
  renderer?: FakeRenderer;
  mainBody?: HTMLElement;
}): { win: BSWindow; renderer: FakeRenderer; mainBody: HTMLElement } => {
  const renderer = opts?.renderer ?? makeRenderer();
  const mainBody = opts?.mainBody ?? document.createElement('div');

  const utils = {
    getMainBody: () => mainBody,
    deepExtend: (args: any[]) => {
      // Minimal recursive merge matching SvyUtilsService.deepExtend shape.
      const extended: Record<string, any> = {};
      let deep = false;
      let i = 0;
      if (typeof args[0] === 'boolean') {
        deep = args[0];
        i++;
      }
      const merge = (obj: any) => {
        for (const prop in obj) {
          if (Object.prototype.hasOwnProperty.call(obj, prop)) {
            if (deep && Object.prototype.toString.call(obj[prop]) === '[object Object]') {
              extended[prop] = utils.deepExtend([true, extended[prop], obj[prop]]);
            } else {
              extended[prop] = obj[prop];
            }
          }
        }
      };
      for (; i < args.length; i++) {
        merge(args[i]);
      }
      return extended;
    },
  };

  const win = new BSWindow(
    { nativeWindow: window } as any,
    { createRenderer: () => renderer } as any,
    utils as any,
    document,
  );
  return { win, renderer, mainBody };
};

describe('BSWindow', () => {
  afterEach(() => {
    vi.useRealTimers();
    document.querySelectorAll('.modal-backdrop').forEach((el) => el.remove());
  });

  describe('setOptions + initialize', () => {
    it('uses the provided template, resolves the handle/title/body/footer selectors and sets title html', () => {
      const template = buildTemplate();
      const { win } = makeWindow();
      win.setOptions({ template, title: 'My Title' });
      expect(win.getElement()).toBe(template);
      expect(win.options.elements.handle).toBe(template.querySelector('.window-header'));
      expect(win.options.elements.title).toBe(template.querySelector('.window-title'));
      expect(win.options.elements.body).toBe(template.querySelector('.window-body'));
      expect(win.options.elements.footer).toBe(template.querySelector('.window-footer'));
      expect(win.options.elements.title.innerHTML).toBe('My Title');
    });

    it('injects bodyContent and footerContent when supplied', () => {
      const template = buildTemplate();
      const { win } = makeWindow();
      win.setOptions({ template, bodyContent: '<p>body</p>', footerContent: '<span>foot</span>' });
      expect(win.options.elements.body.innerHTML).toBe('<p>body</p>');
      expect(win.options.elements.footer.innerHTML).toBe('<span>foot</span>');
    });

    it('uses fromElement when no template is supplied', () => {
      const el = buildTemplate();
      const { win } = makeWindow();
      win.setOptions({ fromElement: el });
      expect(win.getElement()).toBe(el);
    });

    it('throws when neither template nor fromElement is supplied', () => {
      const { win } = makeWindow();
      expect(() => win.setOptions({})).toThrow('No template specified for window.');
    });

    it('sets the window id from options', () => {
      const template = buildTemplate();
      const { win } = makeWindow();
      win.setOptions({ template, id: 'win-42' });
      expect(win.id).toBe('win-42');
    });

    it('defaults the id to an empty string when none is provided', () => {
      const template = buildTemplate();
      const { win } = makeWindow();
      win.setOptions({ template });
      expect(win.id).toBe('');
    });
  });

  describe('show', () => {
    it('makes the element visible, displayed and transitioned', () => {
      const template = buildTemplate();
      const { win, renderer } = makeWindow();
      win.setOptions({ template });
      renderer.setStyle.mockClear();
      win.show();
      expect(renderer.setStyle).toHaveBeenCalledWith(template, 'visibility', 'visible');
      expect(renderer.setStyle).toHaveBeenCalledWith(template, 'display', 'block');
      expect(renderer.setStyle).toHaveBeenCalledWith(template, 'transition', 'opacity 400ms');
    });
  });

  describe('setIndex', () => {
    it('sets the element zIndex style', () => {
      const template = buildTemplate();
      const { win, renderer } = makeWindow();
      win.setOptions({ template });
      win.setIndex(1234);
      expect(renderer.setStyle).toHaveBeenCalledWith(template, 'zIndex', 1234);
    });
  });

  describe('setWindowTab / getWindowTab', () => {
    it('stores and returns the window tab', () => {
      const template = buildTemplate();
      const { win } = makeWindow();
      win.setOptions({ template });
      const tab = document.createElement('span');
      win.setWindowTab(tab);
      expect(win.getWindowTab()).toBe(tab);
    });
  });

  describe('setTitle / getTitle', () => {
    it('updates the options title and the title element html', () => {
      const template = buildTemplate();
      const { win } = makeWindow();
      win.setOptions({ template, title: 'Old' });
      win.setTitle('New Title');
      expect(win.getTitle()).toBe('New Title');
      expect(win.options.elements.title.innerHTML).toBe('New Title');
    });
  });

  describe('getElement', () => {
    it('returns the backing element', () => {
      const template = buildTemplate();
      const { win } = makeWindow();
      win.setOptions({ template });
      expect(win.getElement()).toBe(template);
    });
  });

  describe('setSticky / getSticky', () => {
    it('stores sticky=true and uses fixed positioning', () => {
      const template = buildTemplate();
      const { win, renderer } = makeWindow();
      win.setOptions({ template });
      renderer.setStyle.mockClear();
      win.setSticky(true);
      expect(win.getSticky()).toBe(true);
      expect(renderer.setStyle).toHaveBeenCalledWith(template, 'position', 'fixed');
    });

    it('stores sticky=false and uses absolute positioning', () => {
      const template = buildTemplate();
      const { win, renderer } = makeWindow();
      win.setOptions({ template });
      renderer.setStyle.mockClear();
      win.setSticky(false);
      expect(win.getSticky()).toBe(false);
      expect(renderer.setStyle).toHaveBeenCalledWith(template, 'position', 'absolute');
    });
  });

  describe('setManager', () => {
    it('stores the window_manager on the options', () => {
      const template = buildTemplate();
      const { win } = makeWindow();
      win.setOptions({ template });
      const mgr = {} as any;
      win.setManager(mgr);
      expect(win.options.window_manager).toBe(mgr);
    });
  });

  describe('setActive', () => {
    it('adds the active class and dispatches bswin.active(true)', () => {
      const template = buildTemplate();
      const { win, renderer } = makeWindow();
      win.setOptions({ template });
      const events: boolean[] = [];
      template.addEventListener('bswin.active', (e) => events.push((e as CustomEvent).detail));
      win.setActive(true);
      expect(renderer.addClass).toHaveBeenCalledWith(template, 'active');
      expect(events).toEqual([true]);
    });

    it('removes the active class and dispatches bswin.active(false)', () => {
      const template = buildTemplate();
      const { win, renderer } = makeWindow();
      win.setOptions({ template });
      const events: boolean[] = [];
      template.addEventListener('bswin.active', (e) => events.push((e as CustomEvent).detail));
      win.setActive(false);
      expect(renderer.removeClass).toHaveBeenCalledWith(template, 'active');
      expect(events).toEqual([false]);
    });

    it('toggles the window-tab label classes when a tab is present', () => {
      const template = buildTemplate();
      const { win, renderer } = makeWindow();
      win.setOptions({ template });
      const tab = document.createElement('span');
      win.setWindowTab(tab);
      win.setActive(true);
      expect(renderer.addClass).toHaveBeenCalledWith(tab, 'label-primary');
      renderer.addClass.mockClear();
      win.setActive(false);
      expect(renderer.removeClass).toHaveBeenCalledWith(tab, 'label-primary');
      expect(renderer.addClass).toHaveBeenCalledWith(tab, 'label-default');
    });
  });

  describe('setBlocker / getBlocker / clearBlocker', () => {
    it('stores the blocker, shades the body/footer and back-links the parent', () => {
      const template = buildTemplate();
      const { win, renderer } = makeWindow();
      win.setOptions({ template });

      const blockerTemplate = buildTemplate();
      const { win: blocker } = makeWindow();
      blocker.setOptions({ template: blockerTemplate });

      win.setBlocker(blocker);
      expect(win.getBlocker()).toBe(blocker);
      // disable-scroll is added to the body element while blocked
      expect(renderer.addClass).toHaveBeenCalledWith(win.options.elements.body, 'disable-scroll');
      // setBlocker sets the blocker's parent back to this window
      expect(blocker.getParent()).toBe(win);
    });

    it('clearBlocker removes the blocker reference', () => {
      const template = buildTemplate();
      const { win } = makeWindow();
      win.setOptions({ template });
      const { win: blocker } = makeWindow();
      blocker.setOptions({ template: buildTemplate() });
      win.setBlocker(blocker);
      win.clearBlocker();
      expect(win.getBlocker()).toBeUndefined();
    });
  });

  describe('setParent / getParent', () => {
    it('stores the parent and makes the parent block this window', () => {
      const template = buildTemplate();
      const { win: child } = makeWindow();
      child.setOptions({ template });
      const { win: parent } = makeWindow();
      parent.setOptions({ template: buildTemplate() });

      child.setParent(parent);
      expect(child.getParent()).toBe(parent);
      // setParent makes the parent block the child (since parent had no blocker)
      expect(parent.getBlocker()).toBe(child);
    });

    it('getParent returns undefined when no parent is set', () => {
      const template = buildTemplate();
      const { win } = makeWindow();
      win.setOptions({ template });
      expect(win.getParent()).toBeUndefined();
    });
  });

  describe('close', () => {
    it('dispatches a close event and removes the element', () => {
      const template = buildTemplate();
      document.body.appendChild(template);
      const { win } = makeWindow();
      win.setOptions({ template });
      let closed = false;
      template.addEventListener('close', () => (closed = true));
      win.close();
      expect(closed).toBe(true);
      expect(document.body.contains(template)).toBe(false);
    });

    it('invokes the onClose callback', () => {
      const template = buildTemplate();
      const { win } = makeWindow();
      win.setOptions({ template });
      win.onClose = vi.fn();
      win.close();
      expect(win.onClose).toHaveBeenCalled();
    });

    it('clears the parent blocker and refocuses the parent through the manager', () => {
      const template = buildTemplate();
      const { win } = makeWindow();
      win.setOptions({ template });
      const parent = { clearBlocker: vi.fn() } as any;
      const mgr = { setFocused: vi.fn(), windows: [], modalStack: [] } as any;
      win.options.parent = parent;
      win.options.window_manager = mgr;
      win.close();
      expect(parent.clearBlocker).toHaveBeenCalled();
      expect(mgr.setFocused).toHaveBeenCalledWith(parent);
    });

    it('asks the manager for the next focused window when there is no parent but windows remain', () => {
      const template = buildTemplate();
      const { win } = makeWindow();
      win.setOptions({ template });
      const mgr = { setNextFocused: vi.fn(), windows: [{}], modalStack: [] } as any;
      win.options.window_manager = mgr;
      win.close();
      expect(mgr.setNextFocused).toHaveBeenCalled();
    });

    it('removes modal backdrops when closing the last modal window', () => {
      const backdrop = document.createElement('div');
      backdrop.className = 'modal-backdrop';
      document.body.appendChild(backdrop);
      const template = buildTemplate();
      const { win } = makeWindow();
      win.setOptions({ template });
      win.options.isModal = true;
      win.options.window_manager = { modalStack: [win], windows: [], setFocused: vi.fn(), setNextFocused: vi.fn() } as any;
      win.close();
      expect(document.querySelectorAll('.modal-backdrop').length).toBe(0);
    });

    it('removes the window tab when one is present', () => {
      const template = buildTemplate();
      const { win } = makeWindow();
      win.setOptions({ template });
      const tab = document.createElement('span');
      document.body.appendChild(tab);
      win.setWindowTab(tab);
      win.close();
      expect(document.body.contains(tab)).toBe(false);
    });
  });

  describe('blink', () => {
    it('toggles the active class every 250ms and restores it after 1000ms', () => {
      vi.useFakeTimers();
      const template = buildTemplate();
      const { win, renderer } = makeWindow();
      win.setOptions({ template });
      template.classList.add('active');
      win.blink();
      // After 250ms the active class is toggled off
      vi.advanceTimersByTime(250);
      expect(template.classList.contains('active')).toBe(false);
      // After 500ms toggled back on
      vi.advanceTimersByTime(250);
      expect(template.classList.contains('active')).toBe(true);
      // After the 1000ms timeout the interval stops and active is restored
      renderer.addClass.mockClear();
      vi.advanceTimersByTime(500);
      expect(renderer.addClass).toHaveBeenCalledWith(template, 'active');
    });
  });

  describe('getInteger (private, via resize)', () => {
    it('parses numeric style values and treats empty values as 0', () => {
      const template = buildTemplate();
      const { win } = makeWindow();
      win.setOptions({ template });
      const getInteger = (win as any).getInteger.bind(win);
      expect(getInteger('12px')).toBe(12);
      expect(getInteger('')).toBe(0);
      expect(getInteger(undefined)).toBe(0);
      expect(getInteger('not-a-number')).toBe(0);
    });
  });

  describe('addClassToBodyChildren / removeClassToBodyChildren (private)', () => {
    it('adds and removes a class on each element child of the main body', () => {
      const mainBody = document.createElement('div');
      const childA = document.createElement('div');
      const childB = document.createElement('span');
      mainBody.appendChild(childA);
      mainBody.appendChild(childB);
      mainBody.appendChild(document.createTextNode('ignored text'));
      const template = buildTemplate();
      const { win } = makeWindow({ mainBody });
      win.setOptions({ template });

      (win as any).addClassToBodyChildren('disable-select');
      expect(childA.classList.contains('disable-select')).toBe(true);
      expect(childB.classList.contains('disable-select')).toBe(true);

      (win as any).removeClassToBodyChildren('disable-select');
      expect(childA.classList.contains('disable-select')).toBe(false);
      expect(childB.classList.contains('disable-select')).toBe(false);
    });
  });
});
