import { vi, describe, beforeEach, it, expect } from 'vitest';
import { signal } from '@angular/core';
import { MouseSelectionComponent } from './mouseselection.component';

describe('MouseSelectionComponent', () => {
  let component: MouseSelectionComponent;
  let editorSession: any;
  let editorContentService: any;
  let designerUtilsService: any;
  let urlParser: any;

  beforeEach(() => {
    editorSession = {
      dragging: signal(false),
      ghosthandle: signal(false),
      showWireframe: signal(false),
      resizing: signal(false),
      getSelection: vi.fn().mockReturnValue([]),
      setSelection: vi.fn(),
      requestSelection: vi.fn().mockResolvedValue(undefined),
      addSelectionChangedListener: vi.fn().mockReturnValue(() => undefined),
      updateFieldPositioner: vi.fn(),
      executeAction: vi.fn(),
      keyPressed: vi.fn(),
      openConfigurator: vi.fn(),
      createComponent: vi.fn(),
      getWizardProperties: vi.fn().mockReturnValue(null),
      updateSelection: vi.fn()
    };
    editorContentService = {
      addContentMessageListener: vi.fn(),
      removeContentMessageListener: vi.fn(),
      getGlassPane: vi.fn().mockReturnValue({ addEventListener: vi.fn(), style: {} }),
      getContentArea: vi.fn().mockReturnValue({ getBoundingClientRect: vi.fn().mockReturnValue({ left: 0, top: 0 }), scrollLeft: 0, scrollTop: 0 }),
      executeOnlyAfterInit: vi.fn((cb: any) => cb()),
      getAllContentElements: vi.fn().mockReturnValue([]),
      getContentElement: vi.fn().mockReturnValue(undefined),
      getLeftPositionIframe: vi.fn().mockReturnValue(0),
      getTopPositionIframe: vi.fn().mockReturnValue(0)
    };
    designerUtilsService = {
      getNode: vi.fn().mockReturnValue(null),
      getNodeBasedOnSelectionFCorLFC: vi.fn().mockReturnValue(null),
      adjustElementRect: vi.fn((_, r) => r)
    };
    urlParser = {
      isAbsoluteFormLayout: vi.fn().mockReturnValue(true),
      isMarqueeSelectOuter: vi.fn().mockReturnValue(false)
    };

    component = Object.create(MouseSelectionComponent.prototype);
    (component as any).cdr = { markForCheck: vi.fn() };
    (component as any).editorSession = editorSession;
    (component as any).editorContentService = editorContentService;
    (component as any).designerUtilsService = designerUtilsService;
    (component as any).urlParser = urlParser;
    (component as any).renderer = { setStyle: vi.fn(), addClass: vi.fn(), removeClass: vi.fn(), setAttribute: vi.fn() };
    (component as any).nodes = signal([]);
    (component as any).contentInit = true;
    (component as any).topAdjust = 20;
    (component as any).leftAdjust = 20;
    (component as any).lassostarted = false;
    (component as any).lastTimestamp = 0;
    (component as any).moveFCorLFC = false;
    (component as any).mouseDownEvent = null;
    (component as any).lassoRef = { nativeElement: document.createElement('div') };
    (component as any).selectedRef = [];
  });

  describe('rectanglesIntersect', () => {
    it('should detect overlapping rectangles', () => {
      const r1 = new DOMRect(0, 0, 100, 100);
      const r2 = new DOMRect(50, 50, 100, 100);
      expect((component as any).rectanglesIntersect(r1, r2, false)).toBe(true);
    });

    it('should detect non-overlapping rectangles', () => {
      const r1 = new DOMRect(0, 0, 50, 50);
      const r2 = new DOMRect(100, 100, 50, 50);
      expect((component as any).rectanglesIntersect(r1, r2, false)).toBe(false);
    });

    it('should check full containment when compFullInside is true', () => {
      const r1 = new DOMRect(0, 0, 200, 200);
      const r2 = new DOMRect(10, 10, 50, 50);
      expect((component as any).rectanglesIntersect(r1, r2, true)).toBe(true);
    });

    it('should return false for partial overlap with compFullInside', () => {
      const r1 = new DOMRect(0, 0, 100, 100);
      const r2 = new DOMRect(50, 50, 100, 100);
      expect((component as any).rectanglesIntersect(r1, r2, true)).toBe(false);
    });
  });

  describe('selectionChanged', () => {
    it('should call createNodes when contentInit is true', () => {
      const spy = vi.spyOn(component as any, 'createNodes' as any).mockImplementation(() => undefined);
      component.selectionChanged(['id1']);
      expect(spy).toHaveBeenCalledWith(['id1']);
    });

    it('should not call createNodes when contentInit is false', () => {
      (component as any).contentInit = false;
      const spy = vi.spyOn(component as any, 'createNodes' as any).mockImplementation(() => undefined);
      component.selectionChanged(['id1']);
      expect(spy).not.toHaveBeenCalled();
    });
  });

  describe('contentMessageReceived', () => {
    it('should call selectionChanged on redrawDecorators message', () => {
      const spy = vi.spyOn(component, 'selectionChanged').mockImplementation(() => undefined);
      editorSession.getSelection.mockReturnValue(['abc']);
      component.contentMessageReceived('redrawDecorators', { property: '' });
      expect(spy).toHaveBeenCalledWith(['abc'], true);
    });
  });

  describe('deleteAction', () => {
    it('should stop propagation and call keyPressed with delete keyCode', () => {
      const event = { stopPropagation: vi.fn() } as any;
      component.deleteAction(event);
      expect(event.stopPropagation).toHaveBeenCalled();
      expect(editorSession.keyPressed).toHaveBeenCalledWith({ keyCode: 46 });
    });
  });

  describe('zoomInAction', () => {
    it('should execute zoomIn action', () => {
      const event = { stopPropagation: vi.fn() } as any;
      component.zoomInAction(event);
      expect(editorSession.executeAction).toHaveBeenCalledWith('zoomIn');
    });
  });

  describe('copyAction', () => {
    it('should execute copy action', () => {
      const event = { stopPropagation: vi.fn() } as any;
      component.copyAction(event);
      expect(editorSession.executeAction).toHaveBeenCalledWith('copy');
    });
  });

  describe('updateMoveFCorLFC', () => {
    it('should set moveFCorLFC to true', () => {
      component.updateMoveFCorLFC();
      expect((component as any).moveFCorLFC).toBe(true);
    });
  });

  describe('checkIfNodeIsVisible', () => {
    it('should return false when element has zero dimensions', () => {
      editorContentService.getContentElement.mockReturnValue({ getBoundingClientRect: () => ({ height: 0, width: 0 }) });
      const node = { svyid: 'test' } as any;
      expect(component.checkIfNodeIsVisible(node)).toBe(false);
    });

    it('should return true when element has dimensions', () => {
      editorContentService.getContentElement.mockReturnValue({ getBoundingClientRect: () => ({ height: 50, width: 100 }) });
      const node = { svyid: 'test' } as any;
      expect(component.checkIfNodeIsVisible(node)).toBe(true);
    });

    it('should return false when element is not found', () => {
      editorContentService.getContentElement.mockReturnValue(null);
      const node = { svyid: 'test' } as any;
      expect(component.checkIfNodeIsVisible(node)).toBe(false);
    });
  });

  describe('notInsideFormComponent', () => {
    it('should return true when not inside form component', () => {
      const el = document.createElement('div');
      el.closest = vi.fn().mockReturnValue(null);
      editorContentService.getContentElement.mockReturnValue(el);
      const node = { svyid: 'test' } as any;
      expect(component.notInsideFormComponent(node)).toBe(true);
    });

    it('should return false when inside svy-listformcomponent', () => {
      const el = document.createElement('div');
      el.closest = vi.fn((selector: string) => selector === '.svy-listformcomponent' ? el : null);
      editorContentService.getContentElement.mockReturnValue(el);
      const node = { svyid: 'test' } as any;
      expect(component.notInsideFormComponent(node)).toBe(false);
    });
  });

  describe('redrawDecorators', () => {
    it('should update styles for existing nodes', () => {
      const mockEl = document.createElement('div');
      Object.defineProperty(mockEl, 'getBoundingClientRect', { value: () => ({ height: 50, width: 100, top: 10, left: 20 }) });
      editorContentService.getContentElement.mockReturnValue(mockEl);
      designerUtilsService.adjustElementRect.mockReturnValue({ height: 50, width: 100, top: 10, left: 20 });
      (component as any).nodes.set([{ svyid: 'node1', style: {} }]);
      component.redrawDecorators();
      expect((component as any).nodes()[0].style.height).toBe('50px');
      expect((component as any).nodes()[0].style.width).toBe('100px');
    });

    it('should skip nodes not found in content', () => {
      editorContentService.getContentElement.mockReturnValue(undefined);
      (component as any).nodes.set([{ svyid: 'missing', style: {} }]);
      component.redrawDecorators();
      expect((component as any).nodes()[0].style.height).toBeUndefined();
    });
  });
});
