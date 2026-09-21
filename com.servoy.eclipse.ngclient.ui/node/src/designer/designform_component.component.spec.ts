import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { DesignFormComponent } from './designform_component.component';
import { StructureCache, ComponentCache } from '../ngclient/types';

describe('DesignFormComponent', () => {
  let component: DesignFormComponent;
  let postMessageSpy: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    postMessageSpy = vi.fn();

    component = Object.create(DesignFormComponent.prototype);
    (component as any).variantContainerMargin = 2;
    (component as any).variantItemMargin = 10;
    (component as any).variantsLoaded = true;
    (component as any).windowRefService = {
      nativeWindow: { parent: { parent: { parent: { postMessage: postMessageSpy } } } },
    };
  });

  describe('sendVariantSizes', () => {
    it('should measure variant elements and post sizes to parent window', () => {
      const variantChild = {
        clientLeft: 2,
        getBoundingClientRect: () => ({ width: 120.4 }),
      };
      const variantElements = [{ firstChild: { firstChild: variantChild } }, { firstChild: { firstChild: { clientLeft: 0, getBoundingClientRect: () => ({ width: 80 }) } } }];
      const container = {
        getBoundingClientRect: () => ({ height: 200.7 }),
      };
      const containerParent = { getBoundingClientRect: container.getBoundingClientRect };

      (component as any).document = {
        getElementsByClassName: (cls: string) => {
          if (cls === 'variant_item') return variantElements;
          if (cls === 'variants_container') return { item: () => ({ parentElement: containerParent }) };
          return [];
        },
      };

      component.sendVariantSizes();

      expect(postMessageSpy).toHaveBeenCalledWith(
        {
          id: 'resizePopover',
          formWidth: Math.max(2 + Math.ceil(120.4) + 2 * 10, 0 + Math.ceil(80) + 2 * 10) + 2 * 2,
          formHeight: Math.ceil(200.7) + 2 * 2,
        },
        '*',
      );
    });

    it('should return early when variantsLoaded is false', () => {
      (component as any).variantsLoaded = false;
      (component as any).document = {
        getElementsByClassName: () => [{ firstChild: { firstChild: { clientLeft: 0, getBoundingClientRect: () => ({ width: 50 }) } } }],
      };

      component.sendVariantSizes();

      expect(postMessageSpy).not.toHaveBeenCalled();
    });

    it('should return early when no variant elements exist', () => {
      (component as any).variantsLoaded = true;
      (component as any).document = {
        getElementsByClassName: (cls: string) => {
          if (cls === 'variant_item') return [];
          return [];
        },
      };

      component.sendVariantSizes();

      expect(postMessageSpy).not.toHaveBeenCalled();
    });

    it('should compute formWidth from the widest variant child', () => {
      const narrowChild = { clientLeft: 0, getBoundingClientRect: () => ({ width: 50 }) };
      const wideChild = { clientLeft: 5, getBoundingClientRect: () => ({ width: 200 }) };
      const variantElements = [{ firstChild: { firstChild: narrowChild } }, { firstChild: { firstChild: wideChild } }];
      const containerParent = { getBoundingClientRect: () => ({ height: 100 }) };

      (component as any).document = {
        getElementsByClassName: (cls: string) => {
          if (cls === 'variant_item') return variantElements;
          if (cls === 'variants_container') return { item: () => ({ parentElement: containerParent }) };
          return [];
        },
      };

      component.sendVariantSizes();

      const expectedWidth = 5 + Math.ceil(200) + 2 * 10 + 2 * 2;
      expect(postMessageSpy).toHaveBeenCalledWith(expect.objectContaining({ formWidth: expectedWidth }), '*');
    });

    it('should work independently of onVariantsMouseDown', () => {
      const variantChild = { clientLeft: 0, getBoundingClientRect: () => ({ width: 100 }) };
      const variantElements = [{ firstChild: { firstChild: variantChild } }];
      const containerParent = { getBoundingClientRect: () => ({ height: 50 }) };

      (component as any).document = {
        getElementsByClassName: (cls: string) => {
          if (cls === 'variant_item') return variantElements;
          if (cls === 'variants_container') return { item: () => ({ parentElement: containerParent }) };
          return [];
        },
      };

      component.sendVariantSizes();

      expect(postMessageSpy).toHaveBeenCalledTimes(1);
      expect(postMessageSpy).toHaveBeenCalledWith(
        {
          id: 'resizePopover',
          formWidth: 0 + Math.ceil(100) + 2 * 10 + 2 * 2,
          formHeight: Math.ceil(50) + 2 * 2,
        },
        '*',
      );
    });
  });

  describe('onVariantsMouseDown', () => {
    let postMessageSpy: ReturnType<typeof vi.fn>;
    let elementFromPointSpy: ReturnType<typeof vi.fn>;
    let originalElementFromPoint: typeof document.elementFromPoint;

    beforeEach(() => {
      postMessageSpy = vi.fn();
      elementFromPointSpy = vi.fn();

      component = Object.create(DesignFormComponent.prototype);
      (component as any).windowRefService = {
        nativeWindow: { parent: { postMessage: postMessageSpy } },
      };

      originalElementFromPoint = document.elementFromPoint;
      document.elementFromPoint = elementFromPointSpy as any;
    });

    afterEach(() => {
      document.elementFromPoint = originalElementFromPoint;
    });

    it('should post onVariantMouseDown message with model size unchanged', () => {
      const model = { size: { width: 150, height: 80 }, name: 'btn' };
      const variantItem = Object.create(ComponentCache.prototype);
      variantItem.model = model;
      const variant = Object.create(StructureCache.prototype);
      variant.id = 'var1';
      variant.items = [variantItem];
      (component as any).insertedVariants = [variant];

      const targetEl = {
        tagName: 'BUTTON',
        attributes: { getNamedItem: (name: string) => (name === 'svy-id' ? { nodeValue: 'var1' } : null) },
        parentElement: null,
      };
      elementFromPointSpy.mockReturnValue(targetEl);

      const event = { pageX: 10, pageY: 20, stopPropagation: vi.fn() } as unknown as MouseEvent;
      component.onVariantsMouseDown(event);

      expect(postMessageSpy).toHaveBeenCalledWith({ id: 'onVariantMouseDown', pageX: 10, pageY: 20, model }, '*');
      expect(model.size.width).toBe(150);
      expect(model.size.height).toBe(80);
    });

    it('should not overwrite model.size with DOM measurements', () => {
      const model = { size: { width: 200, height: 100 }, name: 'lbl' };
      const variantItem = Object.create(ComponentCache.prototype);
      variantItem.model = model;
      const variant = Object.create(StructureCache.prototype);
      variant.id = 'v2';
      variant.items = [variantItem];
      (component as any).insertedVariants = [variant];

      const targetEl = {
        tagName: 'SPAN',
        attributes: { getNamedItem: (name: string) => (name === 'svy-id' ? { nodeValue: 'v2' } : null) },
        parentElement: null,
      };
      elementFromPointSpy.mockReturnValue(targetEl);

      const event = { pageX: 5, pageY: 5, stopPropagation: vi.fn() } as unknown as MouseEvent;
      component.onVariantsMouseDown(event);

      expect(model.size).toEqual({ width: 200, height: 100 });
    });

    it('should return early when clicking a DIV element', () => {
      elementFromPointSpy.mockReturnValue({ tagName: 'DIV' });

      const event = { pageX: 0, pageY: 0, stopPropagation: vi.fn() } as unknown as MouseEvent;
      component.onVariantsMouseDown(event);

      expect(postMessageSpy).not.toHaveBeenCalled();
    });

    it('should find the correct variant by traversing up to svy-id attribute', () => {
      const model = { size: { width: 50, height: 50 }, name: 'inner' };
      const variantItem = Object.create(ComponentCache.prototype);
      variantItem.model = model;
      const variant = Object.create(StructureCache.prototype);
      variant.id = 'deep-var';
      variant.items = [variantItem];
      (component as any).insertedVariants = [variant];

      const grandparent = {
        tagName: 'DIV',
        attributes: { getNamedItem: (name: string) => (name === 'svy-id' ? { nodeValue: 'deep-var' } : null) },
        parentElement: null,
      };
      const parent = {
        tagName: 'SPAN',
        attributes: { getNamedItem: () => null },
        parentElement: grandparent,
      };
      const target = {
        tagName: 'INPUT',
        attributes: { getNamedItem: () => null },
        parentElement: parent,
      };
      elementFromPointSpy.mockReturnValue(target);

      const event = { pageX: 30, pageY: 40, stopPropagation: vi.fn() } as unknown as MouseEvent;
      component.onVariantsMouseDown(event);

      expect(postMessageSpy).toHaveBeenCalledWith({ id: 'onVariantMouseDown', pageX: 30, pageY: 40, model }, '*');
    });

    it('should do nothing when no variant matches the svy-id', () => {
      const variant = Object.create(StructureCache.prototype);
      variant.id = 'existing-id';
      variant.items = [];
      (component as any).insertedVariants = [variant];

      const targetEl = {
        tagName: 'BUTTON',
        attributes: { getNamedItem: (name: string) => (name === 'svy-id' ? { nodeValue: 'non-existing-id' } : null) },
        parentElement: null,
      };
      elementFromPointSpy.mockReturnValue(targetEl);

      const event = { pageX: 0, pageY: 0, stopPropagation: vi.fn() } as unknown as MouseEvent;
      component.onVariantsMouseDown(event);

      expect(postMessageSpy).not.toHaveBeenCalled();
    });
  });

  describe('createDraggedComponent message handler - component size preference', () => {
    // SVY-21483: the non-layout (component) branch of the createDraggedComponent
    // handler must prefer event.data.size (a positive measured size) over model.size,
    // and fall back to model.size then 200x100 when no measured size is supplied.
    // The handler body lives inside the constructor's 'message' listener closure,
    // so it is exercised here by constructing a real instance (via the DI-free
    // Object.create pattern used elsewhere in this file is not possible for the
    // closure itself) - instead we call the constructor with minimal fakes.
    let messageListener: (event: MessageEvent) => void;
    let insertedCloneComponent: ComponentCache;

    beforeEach(() => {
      insertedCloneComponent = Object.create(ComponentCache.prototype);
      (insertedCloneComponent as any).model = { size: undefined };
      insertedCloneComponent.parent = Object.create(StructureCache.prototype);
      (insertedCloneComponent as any).name = 'comp1';
      (insertedCloneComponent as any).specName = 'button';
      (insertedCloneComponent as any).type = 'button';
      (insertedCloneComponent as any).handlers = [];
      insertedCloneComponent.layout = undefined as any;

      const formCache: any = {
        getLayoutContainer: vi.fn().mockReturnValue(undefined),
        getComponent: vi.fn().mockReturnValue(insertedCloneComponent),
      };

      const addEventListenerSpy = vi.fn((_type: string, listener: (event: MessageEvent) => void) => {
        messageListener = listener;
      });
      const windowRefService: any = {
        nativeWindow: { addEventListener: addEventListenerSpy, parent: { postMessage: vi.fn() } },
      };
      const logFactory: any = { getLogger: () => ({ warn: vi.fn(), error: vi.fn(), debug: vi.fn(), info: vi.fn() }) };
      const renderer: any = { setStyle: vi.fn(), addClass: vi.fn(), removeAttribute: vi.fn() };
      const formservice: any = { setDesignerMode: vi.fn() };

      const changeHandler: any = { detectChanges: vi.fn(), markForCheck: vi.fn() };

      component = TestBed.runInInjectionContext(
        () => new DesignFormComponent(formservice, {} as any, logFactory, changeHandler, {} as any, renderer, {} as any, windowRefService, {} as any),
      );
      (component as any).formCache = formCache;
      (component as any).name = 'aForm';
    });

    const dispatchCreateDraggedComponent = (data: Record<string, unknown>) => {
      messageListener({ data: { id: 'createDraggedComponent', uuid: 'comp1', dragCopy: false, ...data } } as MessageEvent);
    };

    it('should prefer a positive measured size over model.size', () => {
      insertedCloneComponent.model.size = { width: 150, height: 80 };

      dispatchCreateDraggedComponent({ size: { width: 42, height: 21 } });

      expect(insertedCloneComponent.layout).toEqual({ width: '42px', height: '21px' });
    });

    it('should use model.size when no measured size is supplied', () => {
      insertedCloneComponent.model.size = { width: 150, height: 80 };

      dispatchCreateDraggedComponent({});

      expect(insertedCloneComponent.layout).toEqual({ width: '150px', height: '80px' });
    });

    it('should fall back to 200x100 when neither measured size nor model.size are present', () => {
      insertedCloneComponent.model.size = undefined;

      dispatchCreateDraggedComponent({});

      expect(insertedCloneComponent.layout).toEqual({ width: '200px', height: '100px' });
    });

    it('should treat a zero-width measured size as absent and fall through to model.size', () => {
      insertedCloneComponent.model.size = { width: 150, height: 80 };

      dispatchCreateDraggedComponent({ size: { width: 0, height: 40 } });

      expect(insertedCloneComponent.layout).toEqual({ width: '150px', height: '80px' });
    });

    it('should treat a negative measured height as absent and fall through to model.size', () => {
      insertedCloneComponent.model.size = { width: 150, height: 80 };

      dispatchCreateDraggedComponent({ size: { width: 40, height: -5 } });

      expect(insertedCloneComponent.layout).toEqual({ width: '150px', height: '80px' });
    });

    it('should treat a non-numeric measured size as absent and fall through to the 200x100 fallback', () => {
      insertedCloneComponent.model.size = undefined;

      dispatchCreateDraggedComponent({ size: { width: 'abc', height: 40 } });

      expect(insertedCloneComponent.layout).toEqual({ width: '200px', height: '100px' });
    });
  });
});
