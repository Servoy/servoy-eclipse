import { describe, it, expect, beforeEach, vi } from 'vitest';

import { EditorContentService } from './editorcontent.service';

describe('EditorContentService', () => {
  let service: EditorContentService;

  beforeEach(() => {
    service = Object.create(EditorContentService.prototype);
    (service as any).afterInitCallbacks = [];
    (service as any).contentMessageListeners = [];
    (service as any).contentWasInit = false;
  });

  describe('executeOnlyAfterInit', () => {
    it('should queue callback when content is not yet initialized', () => {
      const callback = vi.fn();
      service.executeOnlyAfterInit(callback);
      expect(callback).not.toHaveBeenCalled();
      expect((service as any).afterInitCallbacks.length).toBe(1);
    });

    it('should execute callback immediately when content is already initialized', () => {
      (service as any).contentWasInit = true;
      const callback = vi.fn();
      service.executeOnlyAfterInit(callback);
      expect(callback).toHaveBeenCalledOnce();
    });

    it('should queue multiple callbacks and not execute them before init', () => {
      const cb1 = vi.fn();
      const cb2 = vi.fn();
      const cb3 = vi.fn();
      service.executeOnlyAfterInit(cb1);
      service.executeOnlyAfterInit(cb2);
      service.executeOnlyAfterInit(cb3);
      expect(cb1).not.toHaveBeenCalled();
      expect(cb2).not.toHaveBeenCalled();
      expect(cb3).not.toHaveBeenCalled();
      expect((service as any).afterInitCallbacks.length).toBe(3);
    });
  });

  describe('addContentMessageListener / removeContentMessageListener', () => {
    it('should add a listener', () => {
      const listener = { contentMessageReceived: vi.fn() };
      service.addContentMessageListener(listener);
      expect((service as any).contentMessageListeners.length).toBe(1);
    });

    it('should not add the same listener twice', () => {
      const listener = { contentMessageReceived: vi.fn() };
      service.addContentMessageListener(listener);
      service.addContentMessageListener(listener);
      expect((service as any).contentMessageListeners.length).toBe(1);
    });

    it('should remove a listener', () => {
      const listener = { contentMessageReceived: vi.fn() };
      service.addContentMessageListener(listener);
      service.removeContentMessageListener(listener);
      expect((service as any).contentMessageListeners.length).toBe(0);
    });

    it('should only remove the specified listener', () => {
      const listener1 = { contentMessageReceived: vi.fn() };
      const listener2 = { contentMessageReceived: vi.fn() };
      service.addContentMessageListener(listener1);
      service.addContentMessageListener(listener2);
      service.removeContentMessageListener(listener1);
      expect((service as any).contentMessageListeners.length).toBe(1);
      expect((service as any).contentMessageListeners[0]).toBe(listener2);
    });
  });

  describe('DOM accessors via injected document', () => {
    let doc: Record<string, ReturnType<typeof vi.fn>>;
    let bodyEl: HTMLElement;

    beforeEach(() => {
      bodyEl = {} as HTMLElement;
      doc = {
        querySelector: vi.fn(),
        querySelectorAll: vi.fn().mockReturnValue([]),
        getElementById: vi.fn(),
        getElementsByTagName: vi.fn().mockReturnValue([]),
      };
      (doc as any).body = bodyEl;
      (service as any).document = doc;
    });

    it('getContentArea should query .content-area and cache the result', () => {
      const el = { id: 'area' } as unknown as HTMLElement;
      doc.querySelector.mockReturnValue(el);
      const first = service.getContentArea();
      const second = service.getContentArea();
      expect(first).toBe(el);
      expect(second).toBe(el);
      expect(doc.querySelector).toHaveBeenCalledTimes(1);
      expect(doc.querySelector).toHaveBeenCalledWith('.content-area');
    });

    it('getPallete should query .palette and cache the result', () => {
      const el = { id: 'pal' } as unknown as HTMLElement;
      doc.querySelector.mockReturnValue(el);
      expect(service.getPallete()).toBe(el);
      service.getPallete();
      expect(doc.querySelector).toHaveBeenCalledTimes(1);
      expect(doc.querySelector).toHaveBeenCalledWith('.palette');
    });

    it('getContent should query .content and cache the result', () => {
      const el = { id: 'content' } as unknown as HTMLElement;
      doc.querySelector.mockReturnValue(el);
      expect(service.getContent()).toBe(el);
      service.getContent();
      expect(doc.querySelector).toHaveBeenCalledTimes(1);
      expect(doc.querySelector).toHaveBeenCalledWith('.content');
    });

    it('getGlassPane should query .contentframe-overlay and cache the result', () => {
      const el = { id: 'glass' } as unknown as HTMLElement;
      doc.querySelector.mockReturnValue(el);
      expect(service.getGlassPane()).toBe(el);
      service.getGlassPane();
      expect(doc.querySelector).toHaveBeenCalledTimes(1);
      expect(doc.querySelector).toHaveBeenCalledWith('.contentframe-overlay');
    });

    it('getBodyElement should return the document body', () => {
      expect(service.getBodyElement()).toBe(bodyEl);
    });

    it('getDocument should return the injected document', () => {
      expect(service.getDocument()).toBe(doc);
    });

    it('querySelector should delegate to the document', () => {
      const el = { id: 'x' } as unknown as HTMLElement;
      doc.querySelector.mockReturnValue(el);
      expect(service.querySelector('.foo')).toBe(el);
      expect(doc.querySelector).toHaveBeenCalledWith('.foo');
    });

    it('querySelectorAll should return an array from the document', () => {
      const els = [{ id: 'a' }, { id: 'b' }] as unknown as HTMLElement[];
      doc.querySelectorAll.mockReturnValue(els);
      const result = service.querySelectorAll('.foo');
      expect(result).toEqual(els);
      expect(Array.isArray(result)).toBe(true);
      expect(doc.querySelectorAll).toHaveBeenCalledWith('.foo');
    });

    it('getDesignerElementById should delegate to document.getElementById', () => {
      const el = { id: 'd' } as unknown as HTMLElement;
      doc.getElementById.mockReturnValue(el);
      expect(service.getDesignerElementById('d')).toBe(el);
      expect(doc.getElementById).toHaveBeenCalledWith('d');
    });
  });

  describe('iframe-backed accessors', () => {
    let doc: Record<string, ReturnType<typeof vi.fn>>;
    let contentDoc: Record<string, ReturnType<typeof vi.fn>>;
    let frame: HTMLIFrameElement;

    beforeEach(() => {
      contentDoc = {
        querySelector: vi.fn(),
        querySelectorAll: vi.fn().mockReturnValue([]),
        elementsFromPoint: vi.fn().mockReturnValue([]),
        getElementById: vi.fn(),
      };
      const contentWindow = { document: contentDoc, postMessage: vi.fn() };
      frame = {
        id: 'form',
        contentWindow,
        getBoundingClientRect: vi.fn().mockReturnValue({ top: 12, left: 34 }),
      } as unknown as HTMLIFrameElement;
      (contentDoc as any).body = { id: 'contentBody' };
      doc = {
        getElementsByTagName: vi.fn().mockReturnValue([frame]),
      };
      (service as any).document = doc;
    });

    it('getContentElement should query the iframe document by svy-id', () => {
      const el = { id: 'node1' } as unknown as HTMLElement;
      contentDoc.querySelector.mockReturnValue(el);
      expect(service.getContentElement('node1')).toBe(el);
      expect(contentDoc.querySelector).toHaveBeenCalledWith("[svy-id='node1']");
    });

    it('getAllContentElements should return all [svy-id] elements', () => {
      const els = [{ id: '1' }, { id: '2' }];
      contentDoc.querySelectorAll.mockReturnValue(els);
      expect(service.getAllContentElements()).toEqual(els);
      expect(contentDoc.querySelectorAll).toHaveBeenCalledWith('[svy-id]');
    });

    it('querySelectorAllInContent should return matching iframe elements', () => {
      const els = [{ id: 'z' }];
      contentDoc.querySelectorAll.mockReturnValue(els);
      expect(service.querySelectorAllInContent('.x')).toEqual(els);
      expect(contentDoc.querySelectorAll).toHaveBeenCalledWith('.x');
    });

    it('getContentForm should query the iframe for .svy-form', () => {
      const el = { id: 'form' } as unknown as HTMLElement;
      contentDoc.querySelector.mockReturnValue(el);
      expect(service.getContentForm()).toBe(el);
      expect(contentDoc.querySelector).toHaveBeenCalledWith('.svy-form');
    });

    it('getContentElementsFromPoint should call elementsFromPoint with coordinates', () => {
      const els = [{ id: 'hit' }];
      contentDoc.elementsFromPoint.mockReturnValue(els);
      expect(service.getContentElementsFromPoint({ x: 5, y: 7 })).toEqual(els);
      expect(contentDoc.elementsFromPoint).toHaveBeenCalledWith(5, 7);
    });

    it('getTopPositionIframe should return the frame rect top', () => {
      expect(service.getTopPositionIframe()).toBe(12);
    });

    it('getLeftPositionIframe should return the frame rect left', () => {
      expect(service.getLeftPositionIframe()).toBe(34);
    });

    it('getContentBodyElement should return the iframe document body', () => {
      expect(service.getContentBodyElement()).toEqual({ id: 'contentBody' });
    });

    it('sendMessageToIframe should post to the iframe content window', () => {
      const message = { foo: 'bar' };
      service.sendMessageToIframe(message);
      expect(frame.contentWindow!.postMessage).toHaveBeenCalledWith(message, '*');
    });

    it('getContentElementById should delegate to the iframe getElementById', () => {
      const el = { id: 'byid' } as unknown as HTMLElement;
      contentDoc.getElementById.mockReturnValue(el);
      expect(service.getContentElementById('byid')).toBe(el);
      expect(contentDoc.getElementById).toHaveBeenCalledWith('byid');
    });
  });

  describe('initIFrame selection', () => {
    let doc: Record<string, ReturnType<typeof vi.fn>>;

    const makeFrame = (id: string): HTMLIFrameElement => ({ id } as unknown as HTMLIFrameElement);

    beforeEach(() => {
      doc = { getElementsByTagName: vi.fn() };
      (service as any).document = doc;
    });

    it('for non-variants should pick the first frame when it is not VariantsForm', () => {
      const normal = makeFrame('form');
      doc.getElementsByTagName.mockReturnValue([normal]);
      (service as any).initIFrame(false);
      expect((service as any).frameElement).toBe(normal);
    });

    it('for non-variants with VariantsForm first should pick the second frame', () => {
      const variants = makeFrame('VariantsForm');
      const normal = makeFrame('form');
      doc.getElementsByTagName.mockReturnValue([variants, normal]);
      (service as any).initIFrame(false);
      expect((service as any).frameElement).toBe(normal);
    });

    it('for variants should pick the first frame when it is VariantsForm', () => {
      const variants = makeFrame('VariantsForm');
      doc.getElementsByTagName.mockReturnValue([variants]);
      (service as any).initIFrame(true);
      expect((service as any).frameElement).toBe(variants);
    });

    it('for variants without VariantsForm first should pick the second frame', () => {
      const normal = makeFrame('form');
      const variants = makeFrame('VariantsForm');
      doc.getElementsByTagName.mockReturnValue([normal, variants]);
      (service as any).initIFrame(true);
      expect((service as any).frameElement).toBe(variants);
    });
  });

  describe('getGlasspaneTopDistance / getGlasspaneLeftDistance', () => {
    beforeEach(() => {
      const contentArea = { id: 'area' } as unknown as HTMLElement;
      (service as any).contentAreaElement = contentArea;
      (service as any).windowRefService = {
        nativeWindow: {
          getComputedStyle: vi.fn().mockReturnValue({
            getPropertyValue: (prop: string) => (prop === 'padding-left' ? '15px' : '8px'),
          }),
        },
      };
    });

    it('getGlasspaneTopDistance should parse padding-left into topAdjust', () => {
      expect(service.getGlasspaneTopDistance()).toBe(15);
    });

    it('getGlasspaneLeftDistance should parse padding-top into leftAdjust', () => {
      expect(service.getGlasspaneLeftDistance()).toBe(8);
    });

    it('should compute adjustments only once', () => {
      service.getGlasspaneTopDistance();
      service.getGlasspaneLeftDistance();
      expect((service as any).windowRefService.nativeWindow.getComputedStyle).toHaveBeenCalledTimes(1);
    });
  });
});
