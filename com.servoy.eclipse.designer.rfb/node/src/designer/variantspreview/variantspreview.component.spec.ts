import { vi, describe, beforeEach, it, expect } from 'vitest';
import { VariantsPreviewComponent } from './variantspreview.component';

describe('VariantsPreviewComponent', () => {
  let component: VariantsPreviewComponent;
  let editorSession: any;
  let editorContentService: any;

  beforeEach(() => {
    editorSession = {
      variantsTrigger: { subscribe: vi.fn(), emit: vi.fn() },
      variantsScroll: { subscribe: vi.fn() },
      variantsPopup: { emit: vi.fn() }
    };
    editorContentService = {
      getDocument: vi.fn().mockReturnValue({
        getElementById: vi.fn().mockReturnValue({ style: {} }),
        getElementsByClassName: vi.fn().mockReturnValue({ item: vi.fn().mockReturnValue({ style: {}, getElementsByClassName: vi.fn().mockReturnValue({ item: vi.fn() }) }) })
      }),
      getPallete: vi.fn().mockReturnValue({ scrollTop: 0 })
    };

    component = Object.create(VariantsPreviewComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).editorContentService = editorContentService;
    (component as any).renderer = { setStyle: vi.fn(), setAttribute: vi.fn() };
    (component as any).windowRef = { nativeWindow: { addEventListener: vi.fn(), location: { host: 'localhost' } } };
    (component as any).sanitizer = { bypassSecurityTrustResourceUrl: vi.fn() };
    (component as any).urlParser = { getSolutionName: vi.fn(), getFormName: vi.fn(), getContentClientNr: vi.fn() };
    (component as any).popoverCfgRef = { autoClose: false, triggers: '' };
    (component as any).popover = { open: vi.fn() };
    (component as any).document = editorContentService.getDocument();
    (component as any).top = -1000;
    (component as any).left = -1000;
    (component as any).isPopoverInitialized = true;
    (component as any).variantsIFrame = { style: { display: '' }, contentWindow: { document: { body: { addEventListener: vi.fn() } } } };
    (component as any).variantItemBeingDragged = null;
    (component as any).variantItemBeingDisplayed = null;
    (component as any).popupParkingPosition = '-10000px';
  });

  describe('hidePopover', () => {
    it('should emit hidden status and hide iframe', () => {
      const popoverCtrl = { style: { top: '', left: '' } };
      (component as any).document.getElementById = vi.fn().mockReturnValue(popoverCtrl);
      component.hidePopover();
      expect(editorSession.variantsPopup.emit).toHaveBeenCalledWith({ status: 'hidden' });
      expect((component as any).variantsIFrame.style.display).toBe('none');
      expect(popoverCtrl.style.top).toBe('-10000px');
    });
  });

  describe('showPopover', () => {
    it('should emit visible status and show iframe', () => {
      component.showPopover();
      expect(editorSession.variantsPopup.emit).toHaveBeenCalledWith({ status: 'visible' });
      expect((component as any).variantsIFrame.style.display).toBe('block');
    });
  });

  describe('onMouseUp (arrow property)', () => {
    beforeEach(() => {
      (component as any).onMouseUp = function(this: any, event: any) {
        event.stopPropagation();
        if (this.variantItemBeingDisplayed) {
          this.variantsIFrame.contentWindow.document.body.removeChild(this.variantItemBeingDisplayed);
          this.windowRef.nativeWindow.postMessage({ id: 'onVariantMouseUp' });
        }
        this.variantItemBeingDragged = null;
        this.variantItemBeingDisplayed = null;
      }.bind(component);
    });

    it('should clean up dragged variant and post message', () => {
      const body = { removeChild: vi.fn() };
      (component as any).variantsIFrame = { contentWindow: { document: { body } } };
      (component as any).windowRef = { nativeWindow: { postMessage: vi.fn() } };
      const displayedNode = document.createElement('div');
      (component as any).variantItemBeingDisplayed = displayedNode;
      const event = { stopPropagation: vi.fn() } as any;
      (component as any).onMouseUp(event);
      expect(body.removeChild).toHaveBeenCalledWith(displayedNode);
      expect((component as any).variantItemBeingDragged).toBeNull();
      expect((component as any).variantItemBeingDisplayed).toBeNull();
    });
  });

  describe('onMouseMove (arrow property)', () => {
    beforeEach(() => {
      (component as any).onMouseMove = function(this: any) {
        if (this.variantItemBeingDisplayed) {
          this.variantsIFrame.contentWindow.document.body.removeChild(this.variantItemBeingDisplayed);
          this.variantItemBeingDragged = null;
          this.variantItemBeingDisplayed = null;
          this.hidePopover();
        }
      }.bind(component);
    });

    it('should clean up and hide popover when dragging', () => {
      const body = { removeChild: vi.fn() };
      (component as any).variantsIFrame = { contentWindow: { document: { body } }, style: { display: '' } };
      const displayedNode = document.createElement('div');
      (component as any).variantItemBeingDisplayed = displayedNode;
      const spy = vi.spyOn(component, 'hidePopover').mockImplementation(() => {});
      (component as any).onMouseMove();
      expect(body.removeChild).toHaveBeenCalled();
      expect((component as any).variantItemBeingDragged).toBeNull();
      expect(spy).toHaveBeenCalled();
    });

    it('should not do anything when not dragging', () => {
      (component as any).variantItemBeingDisplayed = null;
      expect(() => (component as any).onMouseMove()).not.toThrow();
    });
  });

  describe('onAreaMouseUp (arrow property)', () => {
    beforeEach(() => {
      (component as any).onAreaMouseUp = function(_this: any, event: any) {
        event.stopPropagation();
      };
    });

    it('should stop propagation', () => {
      const event = { stopPropagation: vi.fn() } as any;
      (component as any).onAreaMouseUp(null, event);
      expect(event.stopPropagation).toHaveBeenCalled();
    });
  });
});
