import { describe, it, expect, beforeEach, vi } from 'vitest';
import { BehaviorSubject } from 'rxjs';

import { SameSizeIndicatorComponent } from './samesizeindicator.component';

describe('SameSizeIndicatorComponent', () => {
  let component: SameSizeIndicatorComponent;
  let editorSession: any;
  let editorContentService: any;
  let stateListener: BehaviorSubject<string>;

  beforeEach(() => {
    stateListener = new BehaviorSubject<string>('');
    editorSession = {
      addSelectionChangedListener: vi.fn(),
      stateListener,
      getState: vi.fn().mockReturnValue({ sameSizeIndicator: false, dragging: false }),
      getSelection: vi.fn().mockReturnValue([]),
    };
    editorContentService = {
      addContentMessageListener: vi.fn(),
      executeOnlyAfterInit: vi.fn((cb: () => void) => cb()),
      getContentElement: vi.fn(),
      getAllContentElements: vi.fn().mockReturnValue([]),
      getGlasspaneTopDistance: vi.fn().mockReturnValue(0),
      getGlasspaneLeftDistance: vi.fn().mockReturnValue(0),
    };

    component = Object.create(SameSizeIndicatorComponent.prototype);
    (component as any).cdr = { markForCheck: vi.fn() };
    (component as any).editorSession = editorSession;
    (component as any).editorContentService = editorContentService;
    component.sameSizeIndicator = false;
    component.indicators = [];
  });

  describe('ngAfterViewInit', () => {
    it('should subscribe to stateListener', () => {
      component.ngAfterViewInit();
      expect(component.editorStateSubscription).toBeDefined();
    });

    it('should update sameSizeIndicator when state emits sameSizeIndicator', () => {
      editorSession.getState.mockReturnValue({ sameSizeIndicator: true });
      component.ngAfterViewInit();
      stateListener.next('sameSizeIndicator');
      expect(component.sameSizeIndicator).toBe(true);
      expect((component as any).cdr.markForCheck).toHaveBeenCalled();
    });

    it('should clear indicators when sameSizeIndicator is turned off', () => {
      component.indicators = [{ url: 'x', top: 0, left: 0 }] as any;
      editorSession.getState.mockReturnValue({ sameSizeIndicator: false });
      component.ngAfterViewInit();
      stateListener.next('sameSizeIndicator');
      expect(component.indicators).toEqual([]);
      expect((component as any).cdr.markForCheck).toHaveBeenCalled();
    });

    it('should null indicators when dragging starts', () => {
      editorSession.getState.mockReturnValue({ dragging: true });
      component.ngAfterViewInit();
      stateListener.next('dragging');
      expect(component.indicators).toBeNull();
      expect((component as any).cdr.markForCheck).toHaveBeenCalled();
    });

    it('should call selectionChanged when dragging stops', () => {
      editorSession.getState.mockReturnValue({ dragging: false });
      editorSession.getSelection.mockReturnValue(['uuid-1']);
      const spy = vi.spyOn(component, 'selectionChanged');
      component.ngAfterViewInit();
      stateListener.next('dragging');
      expect(spy).toHaveBeenCalledWith(['uuid-1']);
    });
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
      component.sameSizeIndicator = false;
      component.selectionChanged(['uuid-1']);
      expect(component.indicators).toEqual([]);
      expect((component as any).cdr.markForCheck).toHaveBeenCalled();
    });

    it('should clear indicators when selection has multiple items', () => {
      component.sameSizeIndicator = true;
      component.selectionChanged(['uuid-1', 'uuid-2']);
      expect(component.indicators).toEqual([]);
      expect((component as any).cdr.markForCheck).toHaveBeenCalled();
    });

    it('should clear indicators when selection is empty', () => {
      component.sameSizeIndicator = true;
      component.selectionChanged([]);
      expect(component.indicators).toEqual([]);
      expect((component as any).cdr.markForCheck).toHaveBeenCalled();
    });

    it('should find same-width elements and create indicators', () => {
      component.sameSizeIndicator = true;
      const selectedElement = createMockElement(10, 20, 100, 50, false);
      const sameWidthElement = createMockElement(10, 80, 100, 30, false);
      const differentElement = createMockElement(10, 140, 200, 70, false);

      editorContentService.getContentElement.mockReturnValue(selectedElement);
      editorContentService.getAllContentElements.mockReturnValue([selectedElement, sameWidthElement, differentElement]);

      component.selectionChanged(['uuid-1']);

      expect(component.indicators.length).toBe(2);
    });

    it('should find same-height elements and create indicators', () => {
      component.sameSizeIndicator = true;
      const selectedElement = createMockElement(10, 20, 80, 50, false);
      const sameHeightElement = createMockElement(100, 20, 120, 50, false);
      const differentElement = createMockElement(200, 20, 120, 30, false);

      editorContentService.getContentElement.mockReturnValue(selectedElement);
      editorContentService.getAllContentElements.mockReturnValue([selectedElement, sameHeightElement, differentElement]);

      component.selectionChanged(['uuid-1']);

      expect(component.indicators.length).toBe(2);
    });

    it('should skip elements inside responsive containers', () => {
      component.sameSizeIndicator = true;
      const selectedElement = createMockElement(10, 20, 100, 50, false);
      const responsiveElement = createMockElement(10, 80, 100, 50, true);

      editorContentService.getContentElement.mockReturnValue(selectedElement);
      editorContentService.getAllContentElements.mockReturnValue([selectedElement, responsiveElement]);

      component.selectionChanged(['uuid-1']);

      expect(component.indicators).toEqual([]);
    });

    it('should not create indicators for elements smaller than 5px', () => {
      component.sameSizeIndicator = true;
      const selectedElement = createMockElement(10, 20, 3, 3, false);
      const matchElement = createMockElement(50, 20, 3, 3, false);

      editorContentService.getContentElement.mockReturnValue(selectedElement);
      editorContentService.getAllContentElements.mockReturnValue([selectedElement, matchElement]);

      component.selectionChanged(['uuid-1']);

      expect(component.indicators).toEqual([]);
    });
  });

  describe('ngOnDestroy', () => {
    it('should unsubscribe', () => {
      component.ngAfterViewInit();
      const spy = vi.spyOn(component.editorStateSubscription, 'unsubscribe');
      component.ngOnDestroy();
      expect(spy).toHaveBeenCalled();
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
}
