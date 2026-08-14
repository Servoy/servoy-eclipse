import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal } from '@angular/core';

import { SameSizeIndicatorComponent } from './samesizeindicator.component';

describe('SameSizeIndicatorComponent', () => {
  let component: SameSizeIndicatorComponent;
  let editorSession: any;
  let editorContentService: any;

  beforeEach(() => {
    editorSession = {
      addSelectionChangedListener: vi.fn(),
      sameSizeIndicator: signal(false),
      dragging: signal(false),
      getSelection: vi.fn().mockReturnValue([]),
    };
    editorContentService = {
      addContentMessageListener: vi.fn(),
      removeContentMessageListener: vi.fn(),
      executeOnlyAfterInit: vi.fn((cb: () => void) => cb()),
      getContentElement: vi.fn(),
      getAllContentElements: vi.fn().mockReturnValue([]),
      getGlasspaneTopDistance: vi.fn().mockReturnValue(0),
      getGlasspaneLeftDistance: vi.fn().mockReturnValue(0),
    };

    component = Object.create(SameSizeIndicatorComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).editorContentService = editorContentService;
    component.indicators = [];
  });

  describe('contentMessageReceived', () => {
    it('should call selectionChanged on redrawDecorators', () => {
      editorSession.getSelection.mockReturnValue(['uuid-1']);
      const spy = vi.spyOn(component, 'selectionChanged');
      component.contentMessageReceived('redrawDecorators');
      expect(spy).toHaveBeenCalledWith(['uuid-1']);
    });

    it('should not call selectionChanged for other messages', () => {
      const spy = vi.spyOn(component, 'selectionChanged');
      component.contentMessageReceived('otherMessage');
      expect(spy).not.toHaveBeenCalled();
    });
  });

  describe('selectionChanged', () => {
    it('should clear indicators when sameSizeIndicator is off', () => {
      editorSession.sameSizeIndicator.set(false);
      component.selectionChanged(['uuid-1']);
      expect(component.indicators).toEqual([]);
    });

    it('should clear indicators when selection has multiple items', () => {
      editorSession.sameSizeIndicator.set(true);
      component.selectionChanged(['uuid-1', 'uuid-2']);
      expect(component.indicators).toEqual([]);
    });

    it('should clear indicators when selection is empty', () => {
      editorSession.sameSizeIndicator.set(true);
      component.selectionChanged([]);
      expect(component.indicators).toEqual([]);
    });

    it('should find same-width elements and create indicators', () => {
      editorSession.sameSizeIndicator.set(true);
      const selectedElement = createMockElement(10, 20, 100, 50, false);
      const sameWidthElement = createMockElement(10, 80, 100, 30, false);
      const differentElement = createMockElement(10, 140, 200, 70, false);

      editorContentService.getContentElement.mockReturnValue(selectedElement);
      editorContentService.getAllContentElements.mockReturnValue([selectedElement, sameWidthElement, differentElement]);

      component.selectionChanged(['uuid-1']);

      expect(component.indicators.length).toBe(2);
    });

    it('should find same-height elements and create indicators', () => {
      editorSession.sameSizeIndicator.set(true);
      const selectedElement = createMockElement(10, 20, 80, 50, false);
      const sameHeightElement = createMockElement(100, 20, 120, 50, false);
      const differentElement = createMockElement(200, 20, 120, 30, false);

      editorContentService.getContentElement.mockReturnValue(selectedElement);
      editorContentService.getAllContentElements.mockReturnValue([selectedElement, sameHeightElement, differentElement]);

      component.selectionChanged(['uuid-1']);

      expect(component.indicators.length).toBe(2);
    });

    it('should skip elements inside responsive containers', () => {
      editorSession.sameSizeIndicator.set(true);
      const selectedElement = createMockElement(10, 20, 100, 50, false);
      const responsiveElement = createMockElement(10, 80, 100, 50, true);

      editorContentService.getContentElement.mockReturnValue(selectedElement);
      editorContentService.getAllContentElements.mockReturnValue([selectedElement, responsiveElement]);

      component.selectionChanged(['uuid-1']);

      expect(component.indicators).toEqual([]);
    });

    it('should not create indicators for elements smaller than 5px', () => {
      editorSession.sameSizeIndicator.set(true);
      const selectedElement = createMockElement(10, 20, 3, 3, false);
      const matchElement = createMockElement(50, 20, 3, 3, false);

      editorContentService.getContentElement.mockReturnValue(selectedElement);
      editorContentService.getAllContentElements.mockReturnValue([selectedElement, matchElement]);

      component.selectionChanged(['uuid-1']);

      expect(component.indicators).toEqual([]);
    });
  });

  describe('ngOnDestroy', () => {
    it('should remove content message listener', () => {
      component.ngOnDestroy();
      expect(editorContentService.removeContentMessageListener).toHaveBeenCalledWith(component);
    });
  });
});

const createMockElement = (left: number, top: number, width: number, height: number, inResponsive: boolean) => {
  const parentElement = {
    closest: (selector: string) => inResponsive && selector === '.svy-responsivecontainer' ? {} : null,
    classList: { contains: () => false },
    parentElement: null,
  } as any;
  return {
    getBoundingClientRect: () => new DOMRect(left, top, width, height),
    classList: { contains: () => false },
    parentElement,
  } as any;
};
