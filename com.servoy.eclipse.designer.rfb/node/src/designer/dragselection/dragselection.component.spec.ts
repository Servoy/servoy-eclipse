import { vi, describe, beforeEach, it, expect } from 'vitest';
import { DragselectionComponent } from './dragselection.component';

describe('DragselectionComponent', () => {
  let component: DragselectionComponent;
  let editorSession: any;
  let editorContentService: any;

  beforeEach(() => {
    editorSession = {
      getState: vi.fn().mockReturnValue({ dragging: false }),
      getSelection: vi.fn().mockReturnValue([]),
      setDragging: vi.fn(),
      sendChanges: vi.fn(),
      createComponents: vi.fn(),
      updateSelection: vi.fn(),
      registerAutoscroll: vi.fn(),
      unregisterAutoscroll: vi.fn()
    };
    editorContentService = {
      getContentArea: vi.fn().mockReturnValue({
        addEventListener: vi.fn(),
        scrollLeft: 0,
        scrollTop: 0,
        getElementsByClassName: vi.fn().mockReturnValue({ length: 0, item: vi.fn() })
      }),
      getGlassPane: vi.fn().mockReturnValue({ style: {}, offsetHeight: 500, offsetWidth: 500 }),
      getContentElement: vi.fn().mockReturnValue(null),
      getContentBodyElement: vi.fn().mockReturnValue(document.createElement('div')),
      querySelector: vi.fn().mockReturnValue(null),
      querySelectorAll: vi.fn().mockReturnValue([])
    };

    component = Object.create(DragselectionComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).editorContentService = editorContentService;
    (component as any).renderer = { setStyle: vi.fn(), setAttribute: vi.fn(), appendChild: vi.fn(), removeChild: vi.fn() };
    (component as any).urlParser = { isAbsoluteFormLayout: vi.fn().mockReturnValue(true) };
    (component as any).guidesService = { snapDataListener: { subscribe: vi.fn() } };
    (component as any).designerUtilsService = { getNode: vi.fn(), getNodeBasedOnSelectionFCorLFC: vi.fn().mockReturnValue(null) };
    (component as any).selectionToDrag = null;
    (component as any).currentElementsInfo = null;
    (component as any).dragStartEvent = null;
    (component as any).dragNode = null;
    (component as any).dragCopy = false;
    (component as any).scroll = { x: 0, y: 0 };
    (component as any).autoscrollOffset = { x: 0, y: 0 };
    (component as any).autoscrollMargin = 100;
    (component as any).containerOffset = 0;
    (component as any).minimumMargins = { bottom: 0, right: 0 };
    (component as any).selectionRect = { top: 0, left: 0, width: 0, height: 0 };
    (component as any).mousedownpoint = { x: 0, y: 0 };
    (component as any).mouseOffset = { top: 0, left: 0 };
    (component as any).snapData = null;
    (component as any).glasspane = { style: {}, offsetHeight: 500, offsetWidth: 500 };
    (component as any).contentArea = { scrollTop: 0, scrollLeft: 0 };
    (component as any).formComponentBoundary = null;
    (component as any).listFormComponentBoundary = null;
  });

  describe('getAutoscrollLockId', () => {
    it('should return drag-selection', () => {
      expect(component.getAutoscrollLockId()).toBe('drag-selection');
    });
  });

  describe('canMove', () => {
    it('should return true when all elements can move', () => {
      (component as any).selectionToDrag = ['node1'];
      (component as any).currentElementsInfo = new Map([['node1', { x: 50, y: 50 }]]);
      expect(component.canMove(10, 10)).toBe(true);
    });

    it('should return false when element would go negative y', () => {
      (component as any).selectionToDrag = ['node1'];
      (component as any).currentElementsInfo = new Map([['node1', { x: 50, y: 5 }]]);
      expect(component.canMove(0, -10)).toBe(false);
    });

    it('should return false when element would go negative x', () => {
      (component as any).selectionToDrag = ['node1'];
      (component as any).currentElementsInfo = new Map([['node1', { x: 3, y: 50 }]]);
      expect(component.canMove(-10, 0)).toBe(false);
    });
  });

  describe('updateLocation', () => {
    it('should update element positions', () => {
      const el = document.createElement('div');
      el.style.top = '50px';
      el.style.left = '30px';
      el.style.position = 'absolute';
      (component as any).selectionToDrag = ['node1'];
      (component as any).currentElementsInfo = new Map([['node1', { x: 30, y: 50, element: el, width: 100, height: 80 }]]);
      component.updateLocation(10, 5);
      expect((component as any).currentElementsInfo.get('node1').x).toBe(40);
      expect((component as any).currentElementsInfo.get('node1').y).toBe(55);
    });

    it('should not go below zero for x', () => {
      const el = document.createElement('div');
      el.style.top = '5px';
      el.style.left = '3px';
      el.style.position = 'absolute';
      (component as any).selectionToDrag = ['node1'];
      (component as any).currentElementsInfo = new Map([['node1', { x: 3, y: 5, element: el, width: 100, height: 80 }]]);
      component.updateLocation(-10, 0);
      expect((component as any).currentElementsInfo.get('node1').x).toBe(0);
    });

    it('should return early if selectionToDrag is null', () => {
      (component as any).selectionToDrag = null;
      expect(() => component.updateLocation(10, 10)).not.toThrow();
    });

    it('should respect minX', () => {
      const el = document.createElement('div');
      el.style.top = '50px';
      el.style.left = '30px';
      (component as any).selectionToDrag = ['node1'];
      (component as any).currentElementsInfo = new Map([['node1', { x: 30, y: 50, element: el, width: 100, height: 80 }]]);
      (component as any).selectionRect = { top: 50, left: 3, width: 100, height: 80 };
      component.updateLocation(-5, 0, 10);
      expect((component as any).selectionRect.left).toBe(10);
    });
  });

  describe('snap', () => {
    it('should store snapData when selectionToDrag has one item', () => {
      const el = document.createElement('div');
      el.style.top = '50px';
      el.style.left = '30px';
      (component as any).selectionToDrag = ['node1'];
      (component as any).currentElementsInfo = new Map([['node1', { x: 30, y: 50, element: el, width: 100, height: 80 }]]);
      (component as any).selectionRect = { top: 50, left: 30, width: 100, height: 80 };
      const data = { top: 60, left: 40, event: {} } as any;
      component.snap(data);
      expect((component as any).snapData).toBe(data);
    });

    it('should not snap when selectionToDrag is null', () => {
      (component as any).selectionToDrag = null;
      component.snap({ top: 10, left: 10 } as any);
      expect((component as any).snapData).toBeNull();
    });
  });

  describe('getGhostMargins', () => {
    it('should return zero margins when no ghosts', () => {
      (component as any).editorContentService.getContentArea.mockReturnValue({
        getElementsByClassName: vi.fn().mockReturnValue({ length: 0, item: vi.fn() })
      });
      const result = component.getGhostMargins();
      expect(result).toEqual({ bottom: 0, right: 0 });
    });
  });
});
