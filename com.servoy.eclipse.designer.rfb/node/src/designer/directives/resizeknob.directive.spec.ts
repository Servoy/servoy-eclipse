import { vi, describe, beforeEach, it, expect } from 'vitest';
import { provideZonelessChangeDetection, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ResizeKnobDirective, ElementInfo } from './resizeknob.directive';
import { DynamicGuidesService, SnapData } from '../services/dynamicguides.service';
import { EditorSessionService } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';

describe('ResizeKnobDirective', () => {
  let directive: ResizeKnobDirective;
  let editorSession: any;
  let editorContentService: any;

  beforeEach(() => {
    editorSession = {
      resizing: signal(false),
      getSelection: vi.fn().mockReturnValue([]),
      sendChanges: vi.fn()
    };
    editorContentService = {
      getContentArea: vi.fn().mockReturnValue({
        addEventListener: vi.fn(),
        removeEventListener: vi.fn()
      }),
      getGlassPane: vi.fn().mockReturnValue({ style: {} }),
      getContentElement: vi.fn().mockReturnValue(null),
      getAllContentElements: vi.fn().mockReturnValue([]),
      getContentForm: vi.fn().mockReturnValue({ getBoundingClientRect: () => ({ width: 800, height: 600 }) })
    };

    directive = Object.create(ResizeKnobDirective.prototype);
    (directive as any).editorSession = editorSession;
    (directive as any).editorContentService = editorContentService;
    (directive as any).guidesService = { snapData: signal(null) };
      (directive as any).resizeInfo = signal({ node: { style: {} }, direction: 'se', top: 0, left: 0, width: 1, height: 1 });
    (directive as any).topContentAreaAdjust = 20;
    (directive as any).leftContentAreaAdjust = 20;
    (directive as any).initialElementInfo = null;
    (directive as any).currentElementInfo = null;
    (directive as any).snapData = null;
  });

  describe('ElementInfo', () => {
    it('should capture element position and dimensions', () => {
      const el = document.createElement('div');
      Object.defineProperty(el, 'offsetLeft', { value: 30 });
      Object.defineProperty(el, 'offsetTop', { value: 40 });
      Object.defineProperty(el, 'getBoundingClientRect', { value: () => ({ width: 100, height: 50 }) });
      const info = new ElementInfo(el);
      expect(info.x).toBe(30);
      expect(info.y).toBe(40);
      expect(info.width).toBe(100);
      expect(info.height).toBe(50);
      expect(info.element).toBe(el);
    });
  });

  describe('snap', () => {
    it('should apply snap data to element when resizing with single selection', () => {
      editorSession.resizing.set(true);
      const el = document.createElement('div');
      el.style.position = 'absolute';
      const elementInfo = { x: 50, y: 50, element: el };
      (directive as any).currentElementInfo = new Map([['node1', elementInfo]]);
      (directive as any).initialElementInfo = new Map([['node1', elementInfo]]);
    (directive as any).resizeInfo = signal({ node: { style: {} }, direction: 'se', top: 0, left: 0, width: 1, height: 1 });
      const data = { left: 30, top: 40, width: 200, height: 100 } as any;
      directive.snap(data);
      expect((directive as any).snapData).toBe(data);
      expect(el.style.left).toBe('30px');
      expect(el.style.width).toBe('200px');
      expect(el.style.top).toBe('40px');
      expect(el.style.height).toBe('100px');
    });

    it('should not apply snap when not resizing', () => {
      editorSession.resizing.set(false);
      (directive as any).currentElementInfo = new Map();
      directive.snap({ left: 30, top: 40, width: 200, height: 100 } as any);
      expect((directive as any).snapData).toBeNull();
    });

    it('should not apply snap when currentElementInfo is null', () => {
      (directive as any).currentElementInfo = null;
      directive.snap({ left: 30, top: 40, width: 200 } as any);
      expect((directive as any).snapData).toBeNull();
    });
  });

  describe('snap null clearing during active resize (SVY-21493)', () => {
    const startActiveResize = (): void => {
      editorSession.resizing.set(true);
      const el = document.createElement('div');
      el.style.position = 'absolute';
      const elementInfo = { x: 50, y: 50, element: el };
      (directive as any).currentElementInfo = new Map([['node1', elementInfo]]);
      (directive as any).initialElementInfo = new Map([['node1', elementInfo]]);
      (directive as any).resizeInfo = signal({ node: { style: {} }, direction: 'se', top: 0, left: 0, width: 1, height: 1 });
    };

    const resizeGuardOpen = (): boolean =>
      !(directive as any).snapData?.width && !(directive as any).snapData?.height;

    it('should reset snapData to null when snap(null) is received during an active resize', () => {
      startActiveResize();

      const data = { left: 30, top: 40, width: 200, height: 100 } as any;
      directive.snap(data);
      expect((directive as any).snapData).toBe(data);
      expect(resizeGuardOpen()).toBe(false);

      directive.snap(null);
      expect((directive as any).snapData).toBeNull();
      expect(resizeGuardOpen()).toBe(true);
    });

    it('should reopen the resize guard for a width-only snap followed by snap(null)', () => {
      startActiveResize();

      directive.snap({ left: 30, top: 40, width: 200 } as any);
      expect(resizeGuardOpen()).toBe(false);

      directive.snap(null);
      expect((directive as any).snapData).toBeNull();
      expect(resizeGuardOpen()).toBe(true);
    });

    it('should reopen the resize guard for a height-only snap followed by snap(null)', () => {
      startActiveResize();

      directive.snap({ left: 30, top: 40, height: 100 } as any);
      expect(resizeGuardOpen()).toBe(false);

      directive.snap(null);
      expect((directive as any).snapData).toBeNull();
      expect(resizeGuardOpen()).toBe(true);
    });

    it('should leave snapData untouched when snap(null) arrives while not resizing', () => {
      editorSession.resizing.set(false);
      (directive as any).currentElementInfo = new Map();
      (directive as any).snapData = null;

      directive.snap(null);
      expect((directive as any).snapData).toBeNull();
    });

    it('should not clear snapData when snap(null) arrives with no active selection', () => {
      (directive as any).currentElementInfo = null;
      (directive as any).snapData = null;

      directive.snap(null);
      expect((directive as any).snapData).toBeNull();
    });
  });

  describe('constructor effect forwards guidesService.snapData to snap (SVY-21493)', () => {
    let guidesService: { snapData: ReturnType<typeof signal<SnapData | null>> };
    let effectEditorSession: any;
    let effectEditorContentService: any;

    const buildRealDirective = (): ResizeKnobDirective => {
      guidesService = { snapData: signal<SnapData | null>(null) };
      effectEditorSession = {
        resizing: signal(false),
        getSelection: vi.fn().mockReturnValue([]),
        sendChanges: vi.fn()
      };
      effectEditorContentService = {
        getContentArea: vi.fn().mockReturnValue({ addEventListener: vi.fn(), removeEventListener: vi.fn() }),
        getGlassPane: vi.fn().mockReturnValue({ style: {} }),
        getContentElement: vi.fn().mockReturnValue(null),
        getAllContentElements: vi.fn().mockReturnValue([]),
        getContentForm: vi.fn().mockReturnValue({ getBoundingClientRect: () => ({ width: 800, height: 600 }) })
      };

      TestBed.configureTestingModule({
        providers: [
          provideZonelessChangeDetection(),
          { provide: EditorSessionService, useValue: effectEditorSession },
          { provide: EditorContentService, useValue: effectEditorContentService },
          { provide: DynamicGuidesService, useValue: guidesService }
        ]
      });

      const created = TestBed.runInInjectionContext(() => new ResizeKnobDirective());
      created.resizeInfo = signal({ node: { style: {} }, direction: 'se', top: 0, left: 0, width: 1, height: 1 }) as any;
      (created as any).topContentAreaAdjust = 20;
      (created as any).leftContentAreaAdjust = 20;
      return created;
    };

    const startActiveResize = (target: ResizeKnobDirective): void => {
      effectEditorSession.resizing.set(true);
      const el = document.createElement('div');
      el.style.position = 'absolute';
      const elementInfo = { x: 50, y: 50, element: el };
      (target as any).currentElementInfo = new Map([['node1', elementInfo]]);
      (target as any).initialElementInfo = new Map([['node1', elementInfo]]);
    };

    it('should call snap with null when guidesService.snapData is cleared during an active resize', () => {
      const target = buildRealDirective();
      const snapSpy = vi.spyOn(target, 'snap');
      startActiveResize(target);
      TestBed.tick();

      const data = { left: 30, top: 40, width: 200, height: 100 } as SnapData;
      guidesService.snapData.set(data);
      TestBed.tick();
      expect(snapSpy).toHaveBeenCalledWith(data);
      expect(target.snapData).toBe(data);

      guidesService.snapData.set(null);
      TestBed.tick();
      expect(snapSpy).toHaveBeenCalledWith(null);
      expect(target.snapData).toBeNull();
      expect(!target.snapData?.width && !target.snapData?.height).toBe(true);
    });

    it('should reopen the resize guard for a width-only snap cleared via the effect', () => {
      const target = buildRealDirective();
      startActiveResize(target);
      TestBed.tick();

      guidesService.snapData.set({ left: 30, top: 40, width: 200 } as SnapData);
      TestBed.tick();
      expect(!target.snapData?.width && !target.snapData?.height).toBe(false);

      guidesService.snapData.set(null);
      TestBed.tick();
      expect(target.snapData).toBeNull();
      expect(!target.snapData?.width && !target.snapData?.height).toBe(true);
    });
  });

  describe('ngOnInit / setGlasspaneSize', () => {
    beforeEach(() => {
      vi.spyOn(window, 'getComputedStyle').mockReturnValue({
        getPropertyValue: (prop: string) => (prop === 'padding-left' ? '10px' : '12px')
      } as unknown as CSSStyleDeclaration);
    });

    it('should read content-area padding and size the glasspane to cover all components', () => {
      const glasspane = { style: {} as Record<string, string> };
      const contentArea = { clientWidth: 500, clientHeight: 400 };
      editorContentService.getContentArea.mockReturnValue(contentArea);
      editorContentService.getGlassPane.mockReturnValue(glasspane);
      editorContentService.getAllContentElements.mockReturnValue([
        { getBoundingClientRect: () => ({ left: 100, top: 50, width: 900, height: 700 }) }
      ]);
      editorContentService.getContentForm.mockReturnValue({ getBoundingClientRect: () => ({ width: 800, height: 600 }) });

      directive.ngOnInit();

      expect((directive as any).topContentAreaAdjust).toBe(10);
      expect((directive as any).leftContentAreaAdjust).toBe(12);
      // maxWidth = 1000, visibleWidth 500, formWidth 800 => 1000 + 100
      expect(glasspane.style['width']).toBe('1100px');
      // maxHeight = 750, visibleHeight 400, formHeight 600 => 750 + 100
      expect(glasspane.style['height']).toBe('850px');
    });
  });

  describe('resizeSelection', () => {
    it('should resize elements based on the cursor delta and resize direction', () => {
      const el = document.createElement('div');
      el.style.position = 'absolute';
      Object.defineProperty(el, 'closest', { value: () => null });
      const elementInfo = { x: 100, y: 100, width: 50, height: 40, element: el };
      (directive as any).currentElementInfo = new Map([['node1', elementInfo]]);
      (directive as any).snapData = null;
      (directive as any).lastresizeStartPosition = { x: 0, y: 0 };
      // se-resize: width/height grow, x/y stay
      (directive as any).resizeInfo = signal({ node: { style: {} }, direction: 'se', top: 0, left: 0, width: 1, height: 1 });
      (directive as any).resizeSelection({ clientX: 10, clientY: 20 } as MouseEvent);
      expect(elementInfo.width).toBe(60);
      expect(elementInfo.height).toBe(60);
      expect((directive as any).lastresizeStartPosition).toEqual({ x: 10, y: 20 });
    });

    it('should clamp negative positions and sizes to minimums', () => {
      const el = document.createElement('div');
      Object.defineProperty(el, 'closest', { value: () => null });
      const elementInfo = { x: 2, y: 2, width: 3, height: 3, element: el };
      (directive as any).currentElementInfo = new Map([['node1', elementInfo]]);
      (directive as any).snapData = null;
      (directive as any).lastresizeStartPosition = { x: 0, y: 0 };
      // nw-resize: moving top-left, shrinking width/height
      (directive as any).resizeInfo = signal({ node: { style: {} }, direction: 'nw', top: 1, left: 1, width: -1, height: -1 });
      (directive as any).resizeSelection({ clientX: -100, clientY: -100 } as MouseEvent);
      // x/y go negative first and are clamped to 0, which zeroes the delta before
      // it is applied to width/height, so those keep their original values
      expect(elementInfo.x).toBe(0);
      expect(elementInfo.y).toBe(0);
      expect(elementInfo.width).toBe(3);
      expect(elementInfo.height).toBe(3);
    });

    it('should do nothing when a snap width/height is active', () => {
      const el = document.createElement('div');
      const elementInfo = { x: 10, y: 10, width: 50, height: 40, element: el };
      (directive as any).currentElementInfo = new Map([['node1', elementInfo]]);
      (directive as any).snapData = { width: 100 };
      (directive as any).lastresizeStartPosition = { x: 0, y: 0 };
      (directive as any).resizeSelection({ clientX: 10, clientY: 20 } as MouseEvent);
      expect(elementInfo.width).toBe(50);
    });
  });

  describe('sendChanges', () => {
    it('should do nothing when elementInfos is null', () => {
      (directive as any).sendChanges(null);
      expect(editorSession.sendChanges).not.toHaveBeenCalled();
    });

    it('should send plain x/y/width/height when there is no snap data', () => {
      (directive as any).snapData = null;
      const infos = new Map([['node1', { x: 10, y: 20, width: 100, height: 50 }]]);
      (directive as any).sendChanges(infos);
      expect(editorSession.sendChanges).toHaveBeenCalledWith({
        node1: { x: 10, y: 20, width: 100, height: 50, move: false }
      });
    });

    it('should use rounded snap values for a single element with snap data', () => {
      (directive as any).snapData = { left: 30.4, top: 40.6, width: 200.2, height: 100.8, cssPosition: { left: 'x' } };
      const infos = new Map([['node1', { x: 1, y: 2, width: 3, height: 4 }]]);
      (directive as any).sendChanges(infos);
      expect(editorSession.sendChanges).toHaveBeenCalledWith({
        node1: { x: 30, y: 41, width: 200, height: 101, cssPos: { left: 'x' } }
      });
    });
  });

  describe('cleanResizeState', () => {
    it('should clear element info, remove listeners and stop resizing', () => {
      const removeEventListener = vi.fn();
      editorContentService.getContentArea.mockReturnValue({ addEventListener: vi.fn(), removeEventListener });
      editorSession.resizing.set(true);
      (directive as any).initialElementInfo = new Map();
      (directive as any).currentElementInfo = new Map();
      (directive as any).cleanResizeState();
      expect((directive as any).initialElementInfo).toBeNull();
      expect((directive as any).currentElementInfo).toBeNull();
      expect(removeEventListener).toHaveBeenCalled();
      expect(editorSession.resizing()).toBe(false);
    });
  });

  describe('onMouseUp', () => {
    it('should send changes, reset the cursor and clean the resize state', () => {
      const glasspane = { style: { cursor: 'se-resize' } };
      editorContentService.getGlassPane.mockReturnValue(glasspane);
      editorContentService.getContentArea.mockReturnValue({ addEventListener: vi.fn(), removeEventListener: vi.fn() });
      editorSession.resizing.set(true);
      (directive as any).snapData = null;
      (directive as any).currentElementInfo = new Map([['node1', { x: 1, y: 2, width: 3, height: 4 }]]);
      directive.onMouseUp({} as MouseEvent);
      expect(editorSession.sendChanges).toHaveBeenCalled();
      expect(glasspane.style.cursor).toBe('');
      expect(editorSession.resizing()).toBe(false);
    });
  });

  describe('onMouseDown', () => {
    it('should ignore non-left-button clicks', () => {
      directive.onMouseDown({ button: 2 } as MouseEvent);
      expect(editorSession.resizing()).toBe(false);
    });

    it('should start a resize on a left-button click, capturing selection info', () => {
      const contentArea = { addEventListener: vi.fn(), removeEventListener: vi.fn() };
      editorContentService.getContentArea.mockReturnValue(contentArea);
      editorContentService.getGlassPane.mockReturnValue({ style: {} });
      const wrapper = document.createElement('div');
      wrapper.classList.add('svy-wrapper');
      Object.defineProperty(wrapper, 'offsetLeft', { value: 5 });
      Object.defineProperty(wrapper, 'offsetTop', { value: 6 });
      Object.defineProperty(wrapper, 'getBoundingClientRect', { value: () => ({ width: 80, height: 30 }) });
      editorSession.getSelection.mockReturnValue(['node1']);
      editorContentService.getContentElement.mockReturnValue(wrapper);

      directive.onMouseDown({ button: 0, clientX: 100, clientY: 200, preventDefault: vi.fn(), stopPropagation: vi.fn() } as unknown as MouseEvent);

      expect(editorSession.resizing()).toBe(true);
      expect((directive as any).initialElementInfo.has('node1')).toBe(true);
      expect((directive as any).lastresizeStartPosition).toEqual({ x: 100, y: 200 });
      expect(contentArea.addEventListener).toHaveBeenCalled();
    });
  });
});
