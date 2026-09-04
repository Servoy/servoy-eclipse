import { vi, describe, beforeEach, it, expect } from 'vitest';
import { signal } from '@angular/core';
import { DragselectionComponent } from './dragselection.component';

describe('DragselectionComponent', () => {
  let component: DragselectionComponent;
  let editorSession: any;
  let editorContentService: any;

  beforeEach(() => {
    editorSession = {
      dragging: signal(false),
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
    (component as any).guidesService = { snapData: signal(null) };
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

  describe('SVY-21150: Ctrl+drag ghost column guard', () => {
    const makeMockEvent = (overrides: Partial<MouseEvent> = {}): MouseEvent => ({
      clientX: 100,
      clientY: 100,
      pageX: 100,
      pageY: 100,
      ctrlKey: false,
      metaKey: false,
      button: 0,
      preventDefault: vi.fn(),
      stopPropagation: vi.fn(),
      ...overrides
    } as unknown as MouseEvent);

    const makeSvyWrapper = (svyId: string): HTMLElement => {
      const el = document.createElement('div');
      el.classList.add('svy-wrapper');
      el.setAttribute('svy-id', svyId);
      el.style.top = '50px';
      el.style.left = '30px';
      el.style.position = 'absolute';
      el.style.width = '100px';
      el.style.height = '80px';
      return el;
    };

    const setupDragState = () => {
      (component as any).dragStartEvent = makeMockEvent({ clientX: 50, clientY: 50 });
      editorSession.dragging = signal(true);
      (component as any).currentElementsInfo = new Map();
    };

    it('should not initiate drag-copy when Ctrl+drag with ghost-only selection', () => {
      setupDragState();
      editorSession.getSelection.mockReturnValue(['ghost-col-1', 'ghost-col-2']);
      editorContentService.getContentElement.mockReturnValue(null);

      const dragNode = makeSvyWrapper('aggrid-parent');
      (component as any).dragNode = dragNode;

      component.onMouseMove(makeMockEvent({ ctrlKey: true }));

      expect((component as any).dragCopy).toBe(false);
      expect((component as any).selectionToDrag).toEqual([]);
      expect(editorSession.createComponents).not.toHaveBeenCalled();
    });

    it('should proceed with drag-copy when Ctrl+drag with real content elements', () => {
      setupDragState();
      const realEl = makeSvyWrapper('real-btn-1');
      editorSession.getSelection.mockReturnValue(['real-btn-1']);
      editorContentService.getContentElement.mockReturnValue(realEl);
      editorContentService.getContentBodyElement.mockReturnValue(document.createElement('div'));
      editorContentService.querySelector.mockReturnValue(null);
      editorContentService.getContentArea.mockReturnValue({
        getElementsByClassName: vi.fn().mockReturnValue({ length: 0, item: vi.fn() }),
        scrollTop: 0,
        scrollLeft: 0,
        offsetTop: 0,
        offsetLeft: 0
      });

      const dragNode = makeSvyWrapper('real-btn-1');
      dragNode.setAttribute('svy-id', 'real-btn-1');
      (component as any).dragNode = dragNode;

      component.onMouseMove(makeMockEvent({ ctrlKey: true }));

      expect((component as any).dragCopy).toBe(true);
      expect((component as any).selectionToDrag!.length).toBeGreaterThan(0);
    });

    it('should only process real content elements in mixed ghost+real selection', () => {
      setupDragState();
      editorSession.getSelection.mockReturnValue(['real-btn-1', 'ghost-col-1', 'ghost-col-2']);

      const realEl = makeSvyWrapper('real-btn-1');
      editorContentService.getContentElement.mockImplementation((id: string) =>
        id === 'real-btn-1' ? realEl : null
      );
      editorContentService.getContentBodyElement.mockReturnValue(document.createElement('div'));
      editorContentService.querySelector.mockReturnValue(null);
      editorContentService.getContentArea.mockReturnValue({
        getElementsByClassName: vi.fn().mockReturnValue({ length: 0, item: vi.fn() }),
        scrollTop: 0,
        scrollLeft: 0,
        offsetTop: 0,
        offsetLeft: 0
      });

      const dragNode = makeSvyWrapper('real-btn-1');
      (component as any).dragNode = dragNode;

      component.onMouseMove(makeMockEvent({ ctrlKey: true }));

      expect((component as any).dragCopy).toBe(true);
      expect((component as any).selectionToDrag!.length).toBe(1);
    });

    it('should be a no-op when non-Ctrl drag with ghost-only selection', () => {
      setupDragState();
      editorSession.getSelection.mockReturnValue(['ghost-col-1', 'ghost-col-2']);
      editorContentService.getContentElement.mockReturnValue(null);

      const dragNode = makeSvyWrapper('aggrid-parent');
      dragNode.setAttribute('svy-id', 'aggrid-parent');
      (component as any).dragNode = dragNode;

      expect(() => {
        component.onMouseMove(makeMockEvent({ ctrlKey: false }));
      }).not.toThrow();

      expect((component as any).dragCopy).toBe(false);
      expect((component as any).selectionToDrag).toBeNull();
      expect(editorSession.sendChanges).not.toHaveBeenCalled();
      expect(editorSession.createComponents).not.toHaveBeenCalled();
    });

    it('should move elements on non-Ctrl drag with real content elements', () => {
      setupDragState();
      const realEl = makeSvyWrapper('real-btn-1');
      realEl.style.top = '50px';
      realEl.style.left = '30px';
      editorSession.getSelection.mockReturnValue(['real-btn-1']);
      editorContentService.getContentElement.mockReturnValue(realEl);
      editorContentService.querySelector.mockReturnValue(null);
      editorContentService.getContentArea.mockReturnValue({
        getElementsByClassName: vi.fn().mockReturnValue({ length: 0, item: vi.fn() }),
        scrollTop: 0,
        scrollLeft: 0,
        offsetTop: 0,
        offsetLeft: 0
      });

      const dragNode = makeSvyWrapper('real-btn-1');
      (component as any).dragNode = dragNode;

      component.onMouseMove(makeMockEvent({ ctrlKey: false }));

      expect((component as any).dragCopy).toBe(false);
      expect((component as any).selectionToDrag).not.toBeNull();
      expect((component as any).selectionToDrag!.length).toBe(1);
      expect((component as any).currentElementsInfo.size).toBe(1);
    });

    it('should reset state when initSelectionToDrag returns empty result on non-Ctrl path', () => {
      setupDragState();
      editorSession.getSelection.mockReturnValue(['ghost-col-1']);
      editorContentService.getContentElement.mockReturnValue(null);
      editorContentService.querySelector.mockReturnValue(null);
      editorContentService.getContentArea.mockReturnValue({
        getElementsByClassName: vi.fn().mockReturnValue({ length: 0, item: vi.fn() }),
        scrollTop: 0,
        scrollLeft: 0,
        offsetTop: 0,
        offsetLeft: 0
      });

      (component as any).dragNode = makeSvyWrapper('ghost-col-1');

      component.onMouseMove(makeMockEvent({ ctrlKey: false }));

      expect((component as any).selectionToDrag).toBeNull();
      expect((component as any).dragCopy).toBe(false);
    });
  });
});
