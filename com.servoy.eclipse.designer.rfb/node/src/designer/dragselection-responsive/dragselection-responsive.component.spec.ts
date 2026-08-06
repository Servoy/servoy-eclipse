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
});
