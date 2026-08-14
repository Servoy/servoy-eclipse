import { describe, it, expect, beforeEach, vi } from 'vitest';

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
});
