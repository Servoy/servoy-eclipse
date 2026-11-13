import { vi, describe, beforeEach, it, expect } from 'vitest';
import { signal } from '@angular/core';
import { VariantsPreviewComponent } from './variantspreview.component';

describe('VariantsPreviewComponent', () => {
  let component: VariantsPreviewComponent;
  let editorSession: any;
  let editorContentService: any;
  let doc: any;

  const makeStyleEl = () => ({ style: {} as Record<string, string> });

  // The event-handler fields on this component are arrow-function instance properties,
  // so they only exist on a constructed instance (not the prototype). Rather than run the
  // injecting constructor, we create a bare instance and attach the real implementations
  // from a throw-away instance built via Object.create + the source arrow functions.
  beforeEach(() => {
    editorSession = {
      variantsTrigger: signal(null),
      variantsScroll: signal(null),
      variantsPopup: signal(null)
    };
    doc = {
      getElementById: vi.fn().mockReturnValue(makeStyleEl()),
      getElementsByClassName: vi.fn().mockReturnValue({ item: vi.fn().mockReturnValue(makeStyleEl()) })
    };
    editorContentService = {
      getDocument: vi.fn().mockReturnValue(doc),
      getPallete: vi.fn().mockReturnValue({ scrollTop: 0 })
    };

    component = Object.create(VariantsPreviewComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).editorContentService = editorContentService;
    (component as any).renderer = { setStyle: vi.fn(), setAttribute: vi.fn() };
    (component as any).windowRef = { nativeWindow: { addEventListener: vi.fn(), postMessage: vi.fn(), location: { host: 'localhost' } } };
    (component as any).sanitizer = { bypassSecurityTrustResourceUrl: vi.fn() };
    (component as any).urlParser = { getSolutionName: vi.fn(), getFormName: vi.fn(), getContentClientNr: vi.fn() };
    (component as any).popoverCfgRef = { autoClose: false, triggers: '' };
    (component as any).popover = () => ({ open: vi.fn() });
    (component as any).document = doc;
    (component as any).top = -1000;
    (component as any).left = -1000;
    (component as any).margin = 16;
    (component as any).minPopupWidth = 100;
    (component as any).maxPopupWidth = 240;
    (component as any).maxPopupHeight = 300;
    (component as any).popoverFooterHeight = 0;
    (component as any).isPopoverInitialized = true;
    (component as any).variantsIFrame = { style: { display: '' }, contentWindow: { document: { body: { addEventListener: vi.fn() } } } };
    (component as any).variantItemBeingDragged = null;
    (component as any).variantItemBeingDisplayed = null;
    (component as any).popupParkingPosition = '-10000px';
  });

  describe('hidePopover', () => {
    it('should emit hidden status, hide iframe and park the control', () => {
      const popoverCtrl = { style: { top: '', left: '' } };
      doc.getElementById.mockReturnValue(popoverCtrl);
      component.hidePopover();
      expect(editorSession.variantsPopup()).toEqual({ status: 'hidden' });
      expect((component as any).variantsIFrame.style.display).toBe('none');
      expect(popoverCtrl.style.top).toBe('-10000px');
      expect(popoverCtrl.style.left).toBe('-10000px');
    });

    it('should not touch iframe display when there is no iframe', () => {
      (component as any).variantsIFrame = undefined;
      const popoverCtrl = { style: { top: '', left: '' } };
      doc.getElementById.mockReturnValue(popoverCtrl);
      component.hidePopover();
      expect(editorSession.variantsPopup()).toEqual({ status: 'hidden' });
    });
  });

  describe('showPopover', () => {
    it('should emit visible status and show iframe', () => {
      component.showPopover();
      expect(editorSession.variantsPopup()).toEqual({ status: 'visible' });
      expect((component as any).variantsIFrame.style.display).toBe('block');
    });
  });

  describe('setPopoverSizeAndPosition', () => {
    it('should size the popover body and position the control', () => {
      const popoverBody = { style: {} as Record<string, string>, getElementsByClassName: vi.fn().mockReturnValue({ item: () => ({ style: {} as Record<string, string> }) }) };
      const popoverCtrl = { style: {} as Record<string, string> };
      const contentArea = { style: {} as Record<string, string>, scrollTop: 10 };
      const contentOverlay = { style: {} as Record<string, string> };

      doc.getElementsByClassName.mockImplementation((cls: string) => {
        if (cls === 'popover-body') return { item: () => popoverBody };
        if (cls === 'variant-content-area') return { item: () => contentArea };
        if (cls === 'variant-content-overlay') return { item: () => contentOverlay };
        return { item: () => makeStyleEl() };
      });
      doc.getElementById.mockReturnValue(popoverCtrl);

      component.setPopoverSizeAndPosition(150, 200);

      // width = max(100, 150) = 150; height = min(200, 300) = 200
      expect(popoverBody.style['width']).toBe(150 + 0 + 2 * 16 + 'px');
      expect(contentArea.style['width']).toBe(150 + 'px');
      expect(contentArea.style['height']).toBe('200px');
      expect(contentOverlay.style['width']).toBe('240px');
      expect(contentOverlay.style['height']).toBe('200px');
      expect(popoverCtrl.style['display']).toBe('inline-block');
      expect(contentArea.scrollTop).toBe(0);
    });

    it('should add a scrollbar width when the form is taller than the max popup height', () => {
      const popoverBody = { style: {} as Record<string, string>, getElementsByClassName: vi.fn().mockReturnValue({ item: () => ({ style: {} as Record<string, string> }) }) };
      const contentArea = { style: {} as Record<string, string>, scrollTop: 0 };
      const contentOverlay = { style: {} as Record<string, string> };
      doc.getElementsByClassName.mockImplementation((cls: string) => {
        if (cls === 'popover-body') return { item: () => popoverBody };
        if (cls === 'variant-content-area') return { item: () => contentArea };
        if (cls === 'variant-content-overlay') return { item: () => contentOverlay };
        return { item: () => makeStyleEl() };
      });
      doc.getElementById.mockReturnValue({ style: {} as Record<string, string> });

      component.setPopoverSizeAndPosition(120, 500);
      // height capped at 300, formHeight(500) > height(300) => scrollbarWidth 30
      expect(contentArea.style['width']).toBe(120 + 30 + 'px');
    });
  });

  describe('initPopover', () => {
    it('should open the popover, reset position and hide it', () => {
      const openSpy = vi.fn();
      (component as any).popover = () => ({ open: openSpy });
      (component as any).clientURL = 'url';
      const hideSpy = vi.spyOn(component, 'hidePopover').mockImplementation(() => undefined);
      const sizeSpy = vi.spyOn(component, 'setPopoverSizeAndPosition').mockImplementation(() => undefined);
      component.initPopover();
      expect(openSpy).toHaveBeenCalled();
      expect((component as any).top).toBe(-1000);
      expect((component as any).left).toBe(-1000);
      expect(sizeSpy).toHaveBeenCalledWith(100, 100);
      expect(hideSpy).toHaveBeenCalled();
    });
  });

});
