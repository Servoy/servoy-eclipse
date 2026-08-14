import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal } from '@angular/core';

import { AnchoringIndicatorComponent } from './anchoringindicator.component';

describe('AnchoringIndicatorComponent', () => {
  let component: AnchoringIndicatorComponent;
  let editorSession: any;
  let editorContentService: any;
  let urlParser: any;

  beforeEach(() => {
    editorSession = {
      addSelectionChangedListener: vi.fn(),
      anchoringIndicator: signal(false),
      dragging: signal(false),
      getSelection: vi.fn().mockReturnValue([]),
    };
    editorContentService = {
      addContentMessageListener: vi.fn(),
      removeContentMessageListener: vi.fn(),
      executeOnlyAfterInit: vi.fn((cb: () => void) => cb()),
      getContentElement: vi.fn(),
      getGlasspaneTopDistance: vi.fn().mockReturnValue(0),
      getGlasspaneLeftDistance: vi.fn().mockReturnValue(0),
    };
    urlParser = {
      isCSSPositionFormLayout: vi.fn().mockReturnValue(false),
    };

    component = Object.create(AnchoringIndicatorComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).editorContentService = editorContentService;
    (component as any).urlParser = urlParser;
    component.indicator = null;
  });

  describe('contentMessageReceived', () => {
    it('should call selectionChanged on redrawDecorators', () => {
      const spy = vi.spyOn(component, 'selectionChanged');
      editorSession.getSelection.mockReturnValue(['uuid-1']);
      component.contentMessageReceived('redrawDecorators');
      expect(spy).toHaveBeenCalledWith(['uuid-1']);
    });

    it('should not call selectionChanged for other messages', () => {
      const spy = vi.spyOn(component, 'selectionChanged');
      component.contentMessageReceived('other');
      expect(spy).not.toHaveBeenCalled();
    });
  });

  describe('selectionChanged — absolute layout (svy-anchors)', () => {
    const createElementWithAnchor = (anchor: number) => ({
      getBoundingClientRect: () => new DOMRect(100, 50, 200, 100),
      getAttribute: (attr: string) => attr === 'svy-anchors' ? String(anchor) : null,
      parentElement: { closest: () => null },
      closest: () => null,
    });

    beforeEach(() => {
      editorSession.anchoringIndicator.set(true);
      urlParser.isCSSPositionFormLayout.mockReturnValue(false);
    });

    it('should show TOP_LEFT for anchor 0', () => {
      editorContentService.getContentElement.mockReturnValue(createElementWithAnchor(0));
      component.selectionChanged(['uuid-1']);
      expect(component.indicator).not.toBeNull();
      expect(component.indicator!.url).toBe(component.TOP_LEFT_IMAGE);
    });

    it('should show TOP_LEFT for anchor 9', () => {
      editorContentService.getContentElement.mockReturnValue(createElementWithAnchor(9));
      component.selectionChanged(['uuid-1']);
      expect(component.indicator!.url).toBe(component.TOP_LEFT_IMAGE);
    });

    it('should show TOP_RIGHT for anchor 3', () => {
      editorContentService.getContentElement.mockReturnValue(createElementWithAnchor(3));
      component.selectionChanged(['uuid-1']);
      expect(component.indicator!.url).toBe(component.TOP_RIGHT_IMAGE);
    });

    it('should show BOTTOM_LEFT for anchor 12', () => {
      editorContentService.getContentElement.mockReturnValue(createElementWithAnchor(12));
      component.selectionChanged(['uuid-1']);
      expect(component.indicator!.url).toBe(component.BOTTOM_LEFT_IMAGE);
    });

    it('should show BOTTOM_RIGHT for anchor 6', () => {
      editorContentService.getContentElement.mockReturnValue(createElementWithAnchor(6));
      component.selectionChanged(['uuid-1']);
      expect(component.indicator!.url).toBe(component.BOTTOM_RIGHT_IMAGE);
    });

    it('should show BOTTOM_RIGHT_LEFT for anchor 14', () => {
      editorContentService.getContentElement.mockReturnValue(createElementWithAnchor(14));
      component.selectionChanged(['uuid-1']);
      expect(component.indicator!.url).toBe(component.BOTTOM_RIGHT_LEFT_IMAGE);
    });

    it('should show TOP_RIGHT_LEFT for anchor 11', () => {
      editorContentService.getContentElement.mockReturnValue(createElementWithAnchor(11));
      component.selectionChanged(['uuid-1']);
      expect(component.indicator!.url).toBe(component.TOP_RIGHT_LEFT_IMAGE);
    });

    it('should show TOP_LEFT_BOTTOM for anchor 13', () => {
      editorContentService.getContentElement.mockReturnValue(createElementWithAnchor(13));
      component.selectionChanged(['uuid-1']);
      expect(component.indicator!.url).toBe(component.TOP_LEFT_BOTTOM_IMAGE);
    });

    it('should show TOP_RIGHT_BOTTOM for anchor 7', () => {
      editorContentService.getContentElement.mockReturnValue(createElementWithAnchor(7));
      component.selectionChanged(['uuid-1']);
      expect(component.indicator!.url).toBe(component.TOP_RIGHT_BOTTOM_IMAGE);
    });

    it('should show TOP_RIGHT_LEFT_BOTTOM for anchor 15', () => {
      editorContentService.getContentElement.mockReturnValue(createElementWithAnchor(15));
      component.selectionChanged(['uuid-1']);
      expect(component.indicator!.url).toBe(component.TOP_RIGHT_LEFT_BOTTOM_IMAGE);
    });
  });

  describe('selectionChanged — CSS position layout', () => {
    beforeEach(() => {
      editorSession.anchoringIndicator.set(true);
      urlParser.isCSSPositionFormLayout.mockReturnValue(true);
    });

    const createCssPosElement = (style: Record<string, string>) => ({
      getBoundingClientRect: () => new DOMRect(100, 50, 200, 100),
      getAttribute: () => null,
      parentElement: { closest: () => null },
      closest: (selector: string) => {
        if (selector === '.svy-wrapper') return { style, getBoundingClientRect: () => new DOMRect(100, 50, 200, 100) };
        return null;
      },
      classList: { contains: () => false },
    });

    it('should show TOP_LEFT for top+left wrapper', () => {
      editorContentService.getContentElement.mockReturnValue(createCssPosElement({ top: '10px', left: '20px', bottom: '', right: '' }));
      component.selectionChanged(['uuid-1']);
      expect(component.indicator!.url).toBe(component.TOP_LEFT_IMAGE);
    });

    it('should show TOP_RIGHT_LEFT for top+left+right wrapper', () => {
      editorContentService.getContentElement.mockReturnValue(createCssPosElement({ top: '10px', left: '20px', bottom: '', right: '30px' }));
      component.selectionChanged(['uuid-1']);
      expect(component.indicator!.url).toBe(component.TOP_RIGHT_LEFT_IMAGE);
    });

    it('should show TOP_LEFT_BOTTOM for top+left+bottom wrapper', () => {
      editorContentService.getContentElement.mockReturnValue(createCssPosElement({ top: '10px', left: '20px', bottom: '30px', right: '' }));
      component.selectionChanged(['uuid-1']);
      expect(component.indicator!.url).toBe(component.TOP_LEFT_BOTTOM_IMAGE);
    });

    it('should show TOP_RIGHT_LEFT_BOTTOM for all anchors', () => {
      editorContentService.getContentElement.mockReturnValue(createCssPosElement({ top: '10px', left: '20px', bottom: '30px', right: '40px' }));
      component.selectionChanged(['uuid-1']);
      expect(component.indicator!.url).toBe(component.TOP_RIGHT_LEFT_BOTTOM_IMAGE);
    });

    it('should show BOTTOM_RIGHT for no-top+no-left wrapper', () => {
      editorContentService.getContentElement.mockReturnValue(createCssPosElement({ top: '', left: '', bottom: '30px', right: '40px' }));
      component.selectionChanged(['uuid-1']);
      expect(component.indicator!.url).toBe(component.BOTTOM_RIGHT_IMAGE);
    });
  });

  describe('selectionChanged — edge cases', () => {
    it('should clear indicator when anchoringIndicator is off', () => {
      editorSession.anchoringIndicator.set(false);
      component.selectionChanged(['uuid-1']);
      expect(component.indicator).toBeNull();
    });

    it('should clear indicator when selection has multiple items', () => {
      editorSession.anchoringIndicator.set(true);
      component.selectionChanged(['uuid-1', 'uuid-2']);
      expect(component.indicator).toBeNull();
    });

    it('should clear indicator when element is in responsive container', () => {
      editorSession.anchoringIndicator.set(true);
      const element = {
        getBoundingClientRect: () => new DOMRect(0, 0, 100, 50),
        getAttribute: () => null,
        parentElement: { closest: (s: string) => s === '.svy-responsivecontainer' ? {} : null },
        closest: () => null,
      };
      editorContentService.getContentElement.mockReturnValue(element);
      component.selectionChanged(['uuid-1']);
      expect(component.indicator).toBeNull();
    });
  });

  describe('ngOnDestroy', () => {
    it('should remove content message listener', () => {
      component.ngOnDestroy();
      expect(editorContentService.removeContentMessageListener).toHaveBeenCalledWith(component);
    });
  });
});
