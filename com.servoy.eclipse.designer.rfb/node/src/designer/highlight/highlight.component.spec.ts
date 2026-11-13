import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

import { HighlightComponent } from './highlight.component';

describe('HighlightComponent', () => {
  let component: HighlightComponent;
  let editorSession: any;
  let editorContentService: any;
  let renderer: any;
  let urlParser: any;

  beforeEach(() => {
    editorSession = {
      addHighlightChangedListener: vi.fn(),
      setStatusBarText: vi.fn(),
    };
    editorContentService = {
      addContentMessageListener: vi.fn(),
      removeContentMessageListener: vi.fn(),
      executeOnlyAfterInit: vi.fn((cb: () => void) => cb()),
      getContentArea: vi.fn().mockReturnValue({ addEventListener: vi.fn() }),
      getAllContentElements: vi.fn().mockReturnValue([]),
      getLeftPositionIframe: vi.fn().mockReturnValue(0),
      getTopPositionIframe: vi.fn().mockReturnValue(0),
    };
    renderer = {
      addClass: vi.fn(),
      removeClass: vi.fn(),
    };
    urlParser = {
      isAbsoluteFormLayout: vi.fn().mockReturnValue(true),
    };

    component = Object.create(HighlightComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).editorContentService = editorContentService;
    (component as any).renderer = renderer;
    (component as any).urlParser = urlParser;
    (component as any).showPermanentHighlight = false;
    (component as any).shouldRepeatHighlight = false;
  });

  describe('contentMessageReceived', () => {
    it('should set shouldRepeatHighlight on redrawDecorators', () => {
      component.contentMessageReceived('redrawDecorators', { property: '' });
      expect((component as any).shouldRepeatHighlight).toBe(true);
    });

    it('should not set shouldRepeatHighlight for other messages', () => {
      component.contentMessageReceived('otherMessage', { property: '' });
      expect((component as any).shouldRepeatHighlight).toBe(false);
    });
  });

  describe('highlightChanged', () => {
    it('should set showPermanentHighlight', () => {
      component.highlightChanged(true);
      expect(component.showPermanentHighlight).toBe(true);
    });

    it('should add highlight_element class to all elements when showHighlight is true', () => {
      const node1 = {
        parentElement: {
          classList: { contains: vi.fn().mockReturnValue(false) },
          parentElement: {
            classList: { contains: vi.fn().mockReturnValue(false) },
            parentElement: { classList: { contains: vi.fn().mockReturnValue(false) } }
          }
        },
      };
      editorContentService.getAllContentElements.mockReturnValue([node1]);
      component.highlightChanged(true);
      expect(renderer.addClass).toHaveBeenCalledWith(node1, 'highlight_element');
    });

    it('should remove highlight_element class from all elements when showHighlight is false', () => {
      const node1 = {
        parentElement: {
          classList: { contains: vi.fn().mockReturnValue(false) },
          parentElement: {
            classList: { contains: vi.fn().mockReturnValue(false) },
            parentElement: { classList: { contains: vi.fn().mockReturnValue(false) } }
          }
        },
      };
      editorContentService.getAllContentElements.mockReturnValue([node1]);
      component.highlightChanged(false);
      expect(renderer.removeClass).toHaveBeenCalledWith(node1, 'highlight_element');
    });

    it('should use svy-wrapper parent when available', () => {
      const wrapper = { classList: { contains: (cls: string) => cls === 'svy-wrapper' } };
      const node = {
        parentElement: wrapper,
      };
      editorContentService.getAllContentElements.mockReturnValue([node]);
      component.highlightChanged(true);
      expect(renderer.addClass).toHaveBeenCalledWith(wrapper, 'highlight_element');
    });
  });

  describe('ngOnDestroy', () => {
    it('should remove content message listener', () => {
      component.ngOnDestroy();
      expect(editorContentService.removeContentMessageListener).toHaveBeenCalledWith(component);
    });
  });

  describe('ngOnInit', () => {
    it('should attach a mousemove listener to the content area', () => {
      const addEventListener = vi.fn();
      editorContentService.getContentArea.mockReturnValue({ addEventListener });
      component.ngOnInit();
      expect(addEventListener).toHaveBeenCalledWith('mousemove', expect.any(Function));
    });
  });

  describe('onMouseMove / drawHighlightOnMove', () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });
    afterEach(() => {
      vi.useRealTimers();
    });

    const makeNode = (rect: Partial<DOMRect>, attrs: Record<string, string | null> = {}): any => ({
      getBoundingClientRect: () => ({ x: 0, y: 0, width: 100, height: 50, ...rect }),
      getAttribute: (n: string) => (n in attrs ? attrs[n] : null),
      parentElement: null,
      children: []
    });

    it('should debounce the highlight drawing by 300ms', () => {
      const node = makeNode({ x: 0, y: 0, width: 200, height: 100 });
      editorContentService.getAllContentElements.mockReturnValue([node]);
      (component as any).onMouseMove({ pageX: 50, pageY: 25 } as MouseEvent);
      expect(renderer.addClass).not.toHaveBeenCalled();
      vi.advanceTimersByTime(300);
      expect(renderer.addClass).toHaveBeenCalledWith(node, 'highlight_element');
    });

    it('should clear the previous timer on a rapid second move', () => {
      const clearSpy = vi.spyOn(globalThis, 'clearTimeout');
      (component as any).onMouseMove({ pageX: 10, pageY: 10 } as MouseEvent);
      (component as any).onMouseMove({ pageX: 20, pageY: 20 } as MouseEvent);
      expect(clearSpy).toHaveBeenCalled();
    });

    it('should remove the highlight from a previously highlighted component', () => {
      const prev = makeNode({});
      const hit = makeNode({ x: 0, y: 0, width: 200, height: 100 });
      (component as any).highlightedComponent = prev;
      editorContentService.getAllContentElements.mockReturnValue([hit]);
      (component as any).drawHighlightOnMove({ pageX: 50, pageY: 25 } as MouseEvent);
      expect(renderer.addClass).toHaveBeenCalledWith(hit, 'highlight_element');
      expect(renderer.removeClass).toHaveBeenCalledWith(prev, 'highlight_element');
    });

    it('should build a status bar breadcrumb in responsive layout', () => {
      urlParser.isAbsoluteFormLayout.mockReturnValue(false);
      const node: any = {
        getBoundingClientRect: () => ({ x: 0, y: 0, width: 200, height: 100 }),
        getAttribute: (n: string) => {
          if (n === 'svy-id') return 'id1';
          if (n === 'svy-layoutname') return 'bootstrap.row';
          if (n === 'svy-name') return 'myRow';
          return null;
        },
        children: [],
        parentElement: null
      };
      editorContentService.getAllContentElements.mockReturnValue([node]);
      (component as any).drawHighlightOnMove({ pageX: 50, pageY: 25 } as MouseEvent);
      expect(editorSession.setStatusBarText).toHaveBeenCalled();
      const txt = editorSession.setStatusBarText.mock.calls[0][0];
      expect(txt).toContain('row');
      expect(txt).toContain('myRow');
    });
  });
});
