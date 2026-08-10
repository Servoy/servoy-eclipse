import { describe, beforeEach, it, expect, vi } from 'vitest';
import { signal, WritableSignal } from '@angular/core';
import { MouseSelectionComponent, SelectionNode } from './mouseselection.component';

/**
 * Integration-style tests that verify the signal contract for zoneless change detection.
 *
 * These tests ensure that template-bound state uses signals, so that Angular's
 * zoneless scheduler picks up changes made from non-Angular contexts (addEventListener,
 * service callbacks, promise .then()). If someone regresses `nodes` to a plain array,
 * these tests will fail.
 */
describe('MouseSelectionComponent (zoneless CD contract)', () => {
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
      adjustElementRect: vi.fn((_: any, r: any) => r)
    };
    urlParser = {
      isAbsoluteFormLayout: vi.fn().mockReturnValue(true),
      isMarqueeSelectOuter: vi.fn().mockReturnValue(false)
    };

    component = Object.create(MouseSelectionComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).editorContentService = editorContentService;
    (component as any).designerUtilsService = designerUtilsService;
    (component as any).urlParser = urlParser;
    (component as any).renderer = { setStyle: vi.fn(), addClass: vi.fn(), removeClass: vi.fn(), setAttribute: vi.fn() };
    (component as any).nodes = signal<SelectionNode[]>([]);
    (component as any).contentInit = true;
    (component as any).topAdjust = 20;
    (component as any).leftAdjust = 20;
    (component as any).lassostarted = false;
    (component as any).lastTimestamp = 0;
    (component as any).moveFCorLFC = false;
    (component as any).mouseDownEvent = null;
    (component as any).lassoRef = vi.fn().mockReturnValue({ nativeElement: document.createElement('div') });
    (component as any).selectedRef = vi.fn().mockReturnValue([]);
  });

  describe('nodes is a signal (zoneless CD requirement)', () => {
    it('nodes should be a writable signal', () => {
      const nodes = (component as any).nodes;
      expect(typeof nodes).toBe('function');
      expect(typeof nodes.set).toBe('function');
      expect(nodes()).toEqual([]);
    });

    it('selectionChanged should update nodes signal with selection data', () => {
      const fakeElement = document.createElement('div');
      fakeElement.setAttribute('svy-id', 'comp1');
      fakeElement.getBoundingClientRect = () => ({ top: 10, left: 20, width: 100, height: 50, x: 20, y: 10, bottom: 60, right: 120 } as DOMRect);
      fakeElement.closest = vi.fn().mockReturnValue(null);
      Object.defineProperty(fakeElement, 'parentElement', { value: { closest: () => null }, configurable: true });

      editorContentService.getAllContentElements.mockReturnValue([fakeElement]);

      component.selectionChanged(['comp1']);

      const nodes = (component as any).nodes as WritableSignal<SelectionNode[]>;
      expect(nodes().length).toBe(1);
      expect(nodes()[0].svyid).toBe('comp1');
      expect(nodes()[0].style.width).toBe('100px');
      expect(nodes()[0].style.height).toBe('50px');
    });

    it('selectionChanged with empty array should clear nodes signal', () => {
      const nodes = (component as any).nodes as WritableSignal<SelectionNode[]>;
      nodes.set([{ svyid: 'existing', style: {} } as SelectionNode]);

      component.selectionChanged([]);

      expect(nodes().length).toBe(0);
    });

    it('redrawDecorators should update nodes signal (not mutate in-place)', () => {
      const mockEl = document.createElement('div');
      mockEl.getBoundingClientRect = () => ({ height: 80, width: 120, top: 30, left: 40 } as DOMRect);
      editorContentService.getContentElement.mockReturnValue(mockEl);
      designerUtilsService.adjustElementRect.mockReturnValue({ height: 80, width: 120, top: 30, left: 40 });

      const nodes = (component as any).nodes as WritableSignal<SelectionNode[]>;
      const originalNode = { svyid: 'node1', style: {} } as SelectionNode;
      nodes.set([originalNode]);

      component.redrawDecorators();

      const updatedNodes = nodes();
      expect(updatedNodes.length).toBe(1);
      expect(updatedNodes[0].style.height).toBe('80px');
      expect(updatedNodes[0].style.width).toBe('120px');
      expect(updatedNodes[0]).not.toBe(originalNode);
    });

    it('redrawDecorators produces new array reference (triggers signal notification)', () => {
      const mockEl = document.createElement('div');
      mockEl.getBoundingClientRect = () => ({ height: 50, width: 100, top: 10, left: 20 } as DOMRect);
      editorContentService.getContentElement.mockReturnValue(mockEl);
      designerUtilsService.adjustElementRect.mockReturnValue({ height: 50, width: 100, top: 10, left: 20 });

      const nodes = (component as any).nodes as WritableSignal<SelectionNode[]>;
      const initialArray = [{ svyid: 'node1', style: {} } as SelectionNode];
      nodes.set(initialArray);

      component.redrawDecorators();

      expect(nodes()).not.toBe(initialArray);
    });
  });
});
