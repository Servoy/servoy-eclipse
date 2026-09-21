import { vi, describe, beforeEach, it, expect } from 'vitest';
import { signal } from '@angular/core';
import { DragselectionResponsiveComponent } from './dragselection-responsive.component';

describe('DragselectionResponsiveComponent', () => {
  let component: DragselectionResponsiveComponent;
  let editorSession: any;
  let editorContentService: any;

  beforeEach(() => {
    editorSession = {
      dragging: signal(false),
      drop_highlight: signal(''),
      getSelection: vi.fn().mockReturnValue([]),
      setDragging: vi.fn(),
      registerAutoscroll: vi.fn(),
      unregisterAutoscroll: vi.fn(),
      getSession: vi.fn().mockReturnValue({ callService: vi.fn() }),
      createComponents: vi.fn()
    };
    editorContentService = {
      getContentArea: vi.fn().mockReturnValue({ addEventListener: vi.fn(), scrollTop: 0, scrollLeft: 0 }),
      getGlassPane: vi.fn().mockReturnValue({
        style: {},
        getBoundingClientRect: vi.fn().mockReturnValue({ left: 10, top: 10 }),
        parentElement: { style: { paddingLeft: '0px' } }
      }),
      sendMessageToIframe: vi.fn(),
      executeOnlyAfterInit: vi.fn((cb: any) => cb()),
      getContentElementById: vi.fn().mockReturnValue(null),
      getContentElement: vi.fn().mockReturnValue(null)
    };

    component = Object.create(DragselectionResponsiveComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).editorContentService = editorContentService;
    (component as any).renderer = { setStyle: vi.fn(), addClass: vi.fn(), removeAttribute: vi.fn() };
    (component as any).designerUtilsService = {
      getNode: vi.fn().mockReturnValue(null),
      getNodeBasedOnSelectionFCorLFC: vi.fn().mockReturnValue(null),
      getDropNode: vi.fn().mockReturnValue({ dropAllowed: false }),
      isTopContainer: vi.fn().mockReturnValue(false),
      getParent: vi.fn().mockReturnValue(null),
      getNextElementSibling: vi.fn().mockReturnValue(null)
    };
    (component as any).urlParser = { isAbsoluteFormLayout: vi.fn().mockReturnValue(false) };
    (component as any).dragNode = null;
    (component as any).dragStartEvent = null;
    (component as any).dragging = false;
    (component as any).canDrop = { dropAllowed: false };
    (component as any).dragItem = {};
    (component as any).dragCopy = false;
    (component as any).currentPoint = { x: 0, y: 0 };
  });

  describe('getAutoscrollLockId', () => {
    it('should return drag-selection-responsive', () => {
      expect(component.getAutoscrollLockId()).toBe('drag-selection-responsive');
    });
  });

  describe('updateLocationCallback', () => {
    it('should update currentPoint and scroll when contentItemBeingDragged exists', () => {
      const mockEl = document.createElement('div');
      (component as any).dragItem = { contentItemBeingDragged: mockEl };
      (component as any).currentPoint = { x: 100, y: 200 };
      const wrapper = document.createElement('div');
      wrapper.appendChild(document.createElement('div'));
      const glasspane = wrapper.firstElementChild as HTMLElement;
      editorContentService.getGlassPane.mockReturnValue(glasspane);
      const contentArea = { scrollTop: 0, scrollLeft: 0 };
      editorContentService.getContentArea.mockReturnValue(contentArea);
      component.updateLocationCallback(5, 10);
      expect((component as any).currentPoint.x).toBe(105);
      expect((component as any).currentPoint.y).toBe(210);
      expect(contentArea.scrollTop).toBe(10);
      expect(contentArea.scrollLeft).toBe(5);
    });

    it('should do nothing when no contentItemBeingDragged', () => {
      (component as any).dragItem = {};
      expect(() => component.updateLocationCallback(5, 10)).not.toThrow();
    });
  });

  describe('getMeasuredDragSize', () => {
    // SVY-21483: measure the dragged element's rendered size so the responsive
    // drag preview matches the actual element, not model.size / 200x100 fallback.
    const setDragNode = (node: HTMLElement) => {
      (component as any).dragNode = node;
    };
    const stubClientBox = (el: HTMLElement, width: number, height: number) => {
      Object.defineProperty(el, 'clientWidth', { value: width, configurable: true });
      Object.defineProperty(el, 'clientHeight', { value: height, configurable: true });
    };
    const stubRect = (el: HTMLElement, rect: Partial<DOMRect>) => {
      el.getBoundingClientRect = vi.fn().mockReturnValue({ width: 0, height: 0, ...rect });
    };

    it('should measure the dragNode directly when it has a non-zero client box', () => {
      const node = document.createElement('button');
      stubClientBox(node, 84, 32);
      stubRect(node, { width: 84, height: 32 });
      setDragNode(node);

      const size = (component as any).getMeasuredDragSize();

      expect(size).toEqual({ width: 84, height: 32 });
    });

    it('should measure firstElementChild when dragNode reports a zero client box but has a child', () => {
      const wrapper = document.createElement('div');
      stubClientBox(wrapper, 0, 0);
      stubRect(wrapper, { width: 0, height: 0 });

      const child = document.createElement('span');
      stubRect(child, { width: 60, height: 24 });
      wrapper.appendChild(child);

      setDragNode(wrapper);

      const size = (component as any).getMeasuredDragSize();

      expect(size).toEqual({ width: 60, height: 24 });
    });

    it('should measure parentElement when dragNode is a zero-box component with no children and no svy-layoutname', () => {
      const parent = document.createElement('div');
      stubRect(parent, { width: 120, height: 40 });

      const node = document.createElement('div');
      stubClientBox(node, 0, 0);
      stubRect(node, { width: 0, height: 0 });
      // no firstElementChild, no svy-layoutname attribute -> it's a plain component
      parent.appendChild(node);

      setDragNode(node);

      const size = (component as any).getMeasuredDragSize();

      expect(size).toEqual({ width: 120, height: 40 });
    });

    it('should not fall back to parentElement when dragNode is a zero-box layout container', () => {
      const parent = document.createElement('div');
      stubRect(parent, { width: 500, height: 500 });

      const node = document.createElement('div');
      node.setAttribute('svy-layoutname', 'flex-row');
      stubClientBox(node, 0, 0);
      stubRect(node, { width: 0, height: 0 });
      parent.appendChild(node);

      setDragNode(node);

      const size = (component as any).getMeasuredDragSize();

      // measures node itself (0x0, no child, has svy-layoutname so parent fallback is skipped) -> undefined
      expect(size).toBeUndefined();
    });

    it('should return undefined when the measured rect has zero width and height', () => {
      const node = document.createElement('div');
      stubClientBox(node, 40, 40);
      stubRect(node, { width: 0, height: 0 });
      setDragNode(node);

      const size = (component as any).getMeasuredDragSize();

      expect(size).toBeUndefined();
    });

    it('should return undefined when the measured rect has a negative width', () => {
      const node = document.createElement('div');
      stubClientBox(node, 40, 40);
      stubRect(node, { width: -10, height: 20 });
      setDragNode(node);

      const size = (component as any).getMeasuredDragSize();

      expect(size).toBeUndefined();
    });

    it('should return undefined when the measured rect has a non-finite dimension', () => {
      const node = document.createElement('div');
      stubClientBox(node, 40, 40);
      stubRect(node, { width: Infinity, height: 20 });
      setDragNode(node);

      const size = (component as any).getMeasuredDragSize();

      expect(size).toBeUndefined();
    });
  });

  describe('onMouseMove sends measured size in createDraggedComponent message', () => {
    const buildDragStartState = (dragNode: HTMLElement, dragStartEvent: MouseEvent) => {
      (component as any).dragNode = dragNode;
      (component as any).dragStartEvent = dragStartEvent;
      (component as any).dragItem = { topContainer: false, layoutName: '', componentType: 'component' };
      (component as any).canDrop = { dropAllowed: false };
      editorContentService.getGlassPane.mockReturnValue({
        style: {},
        getBoundingClientRect: vi.fn().mockReturnValue({ left: 0, top: 0 }),
        parentElement: { style: { paddingLeft: '0px' } },
      });
      (component as any).designerUtilsService.getDropNode = vi.fn().mockReturnValue({ dropAllowed: false });
      (component as any).designerUtilsService.getNextElementSibling = vi.fn().mockReturnValue(null);
    };

    it('should include a positive measured size in the createDraggedComponent message', () => {
      const node = document.createElement('button');
      Object.defineProperty(node, 'clientWidth', { value: 84, configurable: true });
      Object.defineProperty(node, 'clientHeight', { value: 32, configurable: true });
      node.getBoundingClientRect = vi.fn().mockReturnValue({ width: 84, height: 32 });
      node.setAttribute('svy-id', 'btn1');

      const startEvent = { clientX: 0, clientY: 0 } as MouseEvent;
      buildDragStartState(node, startEvent);

      const moveEvent = { buttons: 1, clientX: 20, clientY: 20, pageX: 20, pageY: 20, ctrlKey: false, metaKey: false } as MouseEvent;
      component.onMouseMove(moveEvent);

      expect(editorContentService.sendMessageToIframe).toHaveBeenCalledWith(
        expect.objectContaining({ id: 'createDraggedComponent', uuid: 'btn1', size: { width: 84, height: 32 } }),
      );
    });

    it('should omit size from the createDraggedComponent message when nothing measurable', () => {
      const node = document.createElement('div');
      Object.defineProperty(node, 'clientWidth', { value: 0, configurable: true });
      Object.defineProperty(node, 'clientHeight', { value: 0, configurable: true });
      node.getBoundingClientRect = vi.fn().mockReturnValue({ width: 0, height: 0 });
      node.setAttribute('svy-layoutname', 'flex-row');
      node.setAttribute('svy-id', 'row1');

      const startEvent = { clientX: 0, clientY: 0 } as MouseEvent;
      buildDragStartState(node, startEvent);

      const moveEvent = { buttons: 1, clientX: 20, clientY: 20, pageX: 20, pageY: 20, ctrlKey: false, metaKey: false } as MouseEvent;
      component.onMouseMove(moveEvent);

      expect(editorContentService.sendMessageToIframe).toHaveBeenCalledWith(
        expect.objectContaining({ id: 'createDraggedComponent', uuid: 'row1', size: undefined }),
      );
    });
  });
});
