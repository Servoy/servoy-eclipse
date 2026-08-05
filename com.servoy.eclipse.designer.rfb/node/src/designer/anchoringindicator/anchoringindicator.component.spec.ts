import { describe, it, expect, beforeEach, vi } from 'vitest';
import { BehaviorSubject } from 'rxjs';

import { AnchoringIndicatorComponent } from './anchoringindicator.component';

describe('AnchoringIndicatorComponent', () => {
  let component: AnchoringIndicatorComponent;
  let editorSession: any;
  let editorContentService: any;
  let urlParser: any;
  let stateListener: BehaviorSubject<string>;

  beforeEach(() => {
    stateListener = new BehaviorSubject<string>('');
    editorSession = {
      addSelectionChangedListener: vi.fn(),
      stateListener,
      getState: vi.fn().mockReturnValue({ anchoringIndicator: false, dragging: false }),
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
    component.anchoringIndicator = false;
    component.indicator = null;
  });

  describe('ngAfterViewInit', () => {
    it('should subscribe to stateListener', () => {
      component.ngAfterViewInit();
      expect(component.editorStateSubscription).toBeDefined();
    });

    it('should update anchoringIndicator on state change', () => {
      editorSession.getState.mockReturnValue({ anchoringIndicator: true });
      component.ngAfterViewInit();
      stateListener.next('anchoringIndicator');
      expect(component.anchoringIndicator).toBe(true);
    });

    it('should clear indicator when anchoringIndicator turns off', () => {
      component.indicator = { url: 'x', top: 0, left: 0 } as any;
      editorSession.getState.mockReturnValue({ anchoringIndicator: false });
      component.ngAfterViewInit();
      stateListener.next('anchoringIndicator');
      expect(component.indicator).toBeNull();
    });

    it('should null indicator when dragging starts', () => {
      component.indicator = { url: 'x', top: 0, left: 0 } as any;
      editorSession.getState.mockReturnValue({ dragging: true });
      component.ngAfterViewInit();
      stateListener.next('dragging');
      expect(component.indicator).toBeNull();
    });
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
      component.anchoringIndicator = true;
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
      component.anchoringIndicator = true;
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
      component.anchoringIndicator = false;
      component.selectionChanged(['uuid-1']);
      expect(component.indicator).toBeNull();
    });

    it('should clear indicator when selection has multiple items', () => {
      component.anchoringIndicator = true;
      component.selectionChanged(['uuid-1', 'uuid-2']);
      expect(component.indicator).toBeNull();
    });

    it('should clear indicator when element is in responsive container', () => {
      component.anchoringIndicator = true;
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
    it('should unsubscribe and remove listener', () => {
      component.ngAfterViewInit();
      const spy = vi.spyOn(component.editorStateSubscription, 'unsubscribe');
      component.ngOnDestroy();
      expect(spy).toHaveBeenCalled();
      expect(editorContentService.removeContentMessageListener).toHaveBeenCalledWith(component);
    });
  });
});
