import { vi, describe, beforeEach, it, expect } from 'vitest';
import { ResizeKnobDirective, ElementInfo } from './resizeknob.directive';

describe('ResizeKnobDirective', () => {
  let directive: ResizeKnobDirective;
  let editorSession: any;
  let editorContentService: any;

  beforeEach(() => {
    editorSession = {
      getState: vi.fn().mockReturnValue({ resizing: false }),
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
    (directive as any).guidesService = { snapDataListener: { subscribe: vi.fn() } };
    (directive as any).resizeInfo = { node: { style: {} }, direction: 'se', top: 0, left: 0, width: 1, height: 1 };
    (directive as any).topContentAreaAdjust = 20;
    (directive as any).leftContentAreaAdjust = 20;
    (directive as any).initialElementInfo = null;
    (directive as any).currentElementInfo = null;
    (directive as any).snapData = null;
    (directive as any).subscription = { unsubscribe: vi.fn() };
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
      editorSession.getState.mockReturnValue({ resizing: true });
      const el = document.createElement('div');
      el.style.position = 'absolute';
      const elementInfo = { x: 50, y: 50, element: el };
      (directive as any).currentElementInfo = new Map([['node1', elementInfo]]);
      (directive as any).initialElementInfo = new Map([['node1', elementInfo]]);
      (directive as any).resizeInfo = { node: { style: {} }, direction: 'se', top: 0, left: 0, width: 1, height: 1 };
      const data = { left: 30, top: 40, width: 200, height: 100 } as any;
      directive.snap(data);
      expect((directive as any).snapData).toBe(data);
      expect(el.style.left).toBe('30px');
      expect(el.style.width).toBe('200px');
      expect(el.style.top).toBe('40px');
      expect(el.style.height).toBe('100px');
    });

    it('should not apply snap when not resizing', () => {
      editorSession.getState.mockReturnValue({ resizing: false });
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
});
