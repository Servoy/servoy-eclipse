import { vi, describe, beforeEach, it, expect } from 'vitest';
import { signal } from '@angular/core';
import { ToolbarComponent, TOOLBAR_CATEGORIES } from './toolbar.component';

describe('ToolbarComponent', () => {
  let component: ToolbarComponent;
  let editorSession: any;
  let urlParser: any;

    let contentElement: any;

  beforeEach(() => {
    editorSession = {
      dragging: signal(false),
      packages: signal([]),
      showWireframe: signal(false),
      getSelection: vi.fn().mockReturnValue([]),
      addSelectionChangedListener: vi.fn(),
      executeAction: vi.fn(),
      getSession: vi.fn().mockReturnValue({ onopen: vi.fn() })
    };
    urlParser = {
      isAbsoluteFormLayout: vi.fn().mockReturnValue(true),
      isShowingContainer: vi.fn().mockReturnValue(null),
      isCSSPositionFormLayout: vi.fn().mockReturnValue(false),
      isFormComponent: vi.fn().mockReturnValue(false)
    };

    component = Object.create(ToolbarComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).urlParser = urlParser;
    (component as any).renderer = { setStyle: vi.fn(), addClass: vi.fn(), removeClass: vi.fn() };
    contentElement = null;
    (component as any).editorContentService = {
      getContentElement: vi.fn().mockImplementation(() => contentElement),
      executeOnlyAfterInit: vi.fn(),
      sendMessageToIframe: vi.fn(),
      querySelectorAllInContent: vi.fn().mockReturnValue([]),
      querySelectorAll: vi.fn().mockReturnValue([]),
      getDesignerElementById: vi.fn().mockReturnValue(null),
      getPallete: vi.fn().mockReturnValue({ style: {} })
    };
    (component as any).designSize = { setEditor: vi.fn(), createItems: vi.fn(), setupItems: vi.fn() };
    (component as any).items = new Map();
    (component as any).TOOLBAR_CATEGORIES = TOOLBAR_CATEGORIES;
  });

  describe('add', () => {
    it('should create a new category array and add item', () => {
      const item = { text: 'Test' } as any;
      component.add(item, TOOLBAR_CATEGORIES.ELEMENTS);
      expect(component.hasCategoryItems(TOOLBAR_CATEGORIES.ELEMENTS)).toBe(true);
      expect(component.getCategoryItems(TOOLBAR_CATEGORIES.ELEMENTS)).toContain(item);
    });

    it('should add to existing category', () => {
      const item1 = { text: 'A' } as any;
      const item2 = { text: 'B' } as any;
      component.add(item1, TOOLBAR_CATEGORIES.FORM);
      component.add(item2, TOOLBAR_CATEGORIES.FORM);
      expect(component.getCategoryItems(TOOLBAR_CATEGORIES.FORM).length).toBe(2);
    });
  });

  describe('hasCategoryItems', () => {
    it('should return false for empty category', () => {
      expect(component.hasCategoryItems(TOOLBAR_CATEGORIES.ZOOM)).toBe(false);
    });
  });

  describe('getCategoryItems', () => {
    it('should return empty array for missing category', () => {
      expect(component.getCategoryItems(TOOLBAR_CATEGORIES.STICKY)).toEqual([]);
    });
  });

  describe('selectionChanged', () => {
    it('should update btnZoomOut enabled based on showingContainer', () => {
      (component as any).btnZoomOut = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveUp = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveDown = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnZoomIn = { enabled: signal<(() => boolean) | boolean>(false) };
      urlParser.isShowingContainer.mockReturnValue('container1');
      urlParser.isAbsoluteFormLayout.mockReturnValue(true);
      component.selectionChanged(['id1']);
      expect((component as any).btnZoomOut.enabled()).toBe(true);
    });

    it('should enable move buttons for single selection in responsive', () => {
      (component as any).btnZoomOut = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveUp = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveDown = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnZoomIn = { enabled: signal<(() => boolean) | boolean>(false) };
      urlParser.isAbsoluteFormLayout.mockReturnValue(false);
      urlParser.isShowingContainer.mockReturnValue(null);
      component.selectionChanged(['id1']);
      expect((component as any).btnMoveUp.enabled()).toBe(true);
      expect((component as any).btnMoveDown.enabled()).toBe(true);
    });

    it('should enable btnZoomIn when a layout container is selected in responsive', () => {
      (component as any).btnZoomOut = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveUp = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveDown = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnZoomIn = { enabled: signal<(() => boolean) | boolean>(false) };
      urlParser.isAbsoluteFormLayout.mockReturnValue(false);
      urlParser.isShowingContainer.mockReturnValue(null);
      contentElement = {
        classList: { contains: (c: string) => c === 'svy-layoutcontainer' },
        getAttribute: () => null
      };
      component.selectionChanged(['id1']);
      expect((component as any).btnZoomIn.enabled()).toBe(true);
    });

    it('should disable btnZoomIn when nothing is selected in responsive', () => {
      (component as any).btnZoomOut = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveUp = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveDown = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnZoomIn = { enabled: signal<(() => boolean) | boolean>(true) };
      urlParser.isAbsoluteFormLayout.mockReturnValue(false);
      urlParser.isShowingContainer.mockReturnValue(null);
      component.selectionChanged([]);
      expect((component as any).btnZoomIn.enabled()).toBe(false);
    });

    it('should disable btnZoomIn when the selected element is not a layout container', () => {
      (component as any).btnZoomOut = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveUp = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveDown = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnZoomIn = { enabled: signal<(() => boolean) | boolean>(true) };
      urlParser.isAbsoluteFormLayout.mockReturnValue(false);
      urlParser.isShowingContainer.mockReturnValue(null);
      contentElement = {
        classList: { contains: () => false },
        getAttribute: () => null
      };
      component.selectionChanged(['id1']);
      expect((component as any).btnZoomIn.enabled()).toBe(false);
    });

    it('should disable btnZoomIn for the main container', () => {
      (component as any).btnZoomOut = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveUp = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveDown = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnZoomIn = { enabled: signal<(() => boolean) | boolean>(true) };
      urlParser.isAbsoluteFormLayout.mockReturnValue(false);
      urlParser.isShowingContainer.mockReturnValue(null);
      contentElement = {
        classList: { contains: (c: string) => c === 'svy-layoutcontainer' },
        getAttribute: (a: string) => a === 'data-maincontainer' ? 'true' : null
      };
      component.selectionChanged(['id1']);
      expect((component as any).btnZoomIn.enabled()).toBe(false);
    });
  });

  describe('isZoomableLayoutContainerSelected', () => {
    it('should return false when nothing is selected', () => {
      expect((component as any).isZoomableLayoutContainerSelected([])).toBe(false);
    });

    it('should return false for a multi-element selection', () => {
      expect((component as any).isZoomableLayoutContainerSelected(['a', 'b'])).toBe(false);
    });

    it('should return false when the element cannot be resolved', () => {
      contentElement = null;
      expect((component as any).isZoomableLayoutContainerSelected(['id1'])).toBe(false);
    });

    it('should return true for a single layout container', () => {
      contentElement = {
        classList: { contains: (c: string) => c === 'svy-layoutcontainer' },
        getAttribute: () => null
      };
      expect((component as any).isZoomableLayoutContainerSelected(['id1'])).toBe(true);
    });

    it('should return false for a responsive container', () => {
      contentElement = {
        classList: { contains: (c: string) => c === 'svy-layoutcontainer' || c === 'svy-responsivecontainer' },
        getAttribute: () => null
      };
      expect((component as any).isZoomableLayoutContainerSelected(['id1'])).toBe(false);
    });
  });
});
