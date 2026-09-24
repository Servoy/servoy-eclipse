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
});
