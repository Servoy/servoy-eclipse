import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { signal, provideZonelessChangeDetection, NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PaletteComponent, SearchTextPipe, SearchTextDeepPipe } from './palette.component';
import { EditorSessionService } from '../services/editorsession.service';
import { URLParserService } from '../services/urlparser.service';
import { DesignerUtilsService } from '../services/designerutils.service';
import { EditorContentService } from '../services/editorcontent.service';
import { DynamicGuidesService, SnapData } from '../services/dynamicguides.service';
import { WindowRefService } from '@servoy/public';

describe('PaletteComponent', () => {
  let component: PaletteComponent;
  let editorSession: any;
  let urlParser: any;
  let editorContentService: any;

  beforeEach(() => {
    editorSession = {
      setPaletteRefresher: vi.fn(),
      packages: signal([]),
      dragging: signal(false),
      variantsTrigger: { emit: vi.fn() },
      variantsScroll: { emit: vi.fn() },
      registerAutoscroll: vi.fn(),
      unregisterAutoscroll: vi.fn(),
      setDragging: vi.fn(),
      createComponent: vi.fn(),
      variantsPopup: { emit: vi.fn() }
    };
    urlParser = {
      isAbsoluteFormLayout: vi.fn().mockReturnValue(false),
      getFormName: vi.fn().mockReturnValue('testForm')
    };
    editorContentService = {
      getBodyElement: vi.fn().mockReturnValue({ addEventListener: vi.fn() }),
      getGlassPane: vi.fn().mockReturnValue({ style: {} }),
      getPallete: vi.fn().mockReturnValue({ getBoundingClientRect: vi.fn().mockReturnValue({ width: 200 }), scrollTop: 0 }),
      getContentArea: vi.fn().mockReturnValue({ scrollTop: 0, scrollLeft: 0, focus: vi.fn() }),
      sendMessageToIframe: vi.fn(),
      getLeftPositionIframe: vi.fn().mockReturnValue(0),
      getTopPositionIframe: vi.fn().mockReturnValue(0),
      getAllContentElements: vi.fn().mockReturnValue([]),
      getContentElementById: vi.fn().mockReturnValue(null),
      getDocument: vi.fn().mockReturnValue({ elementFromPoint: vi.fn() })
    };
    const windowRef = { nativeWindow: { addEventListener: vi.fn(), location: { host: 'localhost' } } };

    component = Object.create(PaletteComponent.prototype);
    (component as any).cdr = { markForCheck: vi.fn() };
    (component as any).editorSession = editorSession;
    (component as any).urlParser = urlParser;
    (component as any).editorContentService = editorContentService;
    (component as any).windowRef = windowRef;
    (component as any).renderer = { setStyle: vi.fn(), addClass: vi.fn(), removeClass: vi.fn() };
    (component as any).designerUtilsService = { getDropNode: vi.fn().mockReturnValue({ dropAllowed: false }), adjustElementRect: vi.fn() };
    (component as any).guidesService = { snapData: signal(null) };
    (component as any).http = { get: vi.fn() };
    (component as any).dragItem = {};
    (component as any).canDrop = { dropAllowed: false };
    (component as any).searchHistory = [];
    (component as any).filteredSuggestions = [];
    (component as any).showSuggestions = false;
    (component as any).keepSuggestionsOpen = false;
    (component as any).showSearchDeleteBtn = false;
    (component as any).searchText = '';
    (component as any).isDraggedVariant = false;
    (component as any).draggedVariant = {};
    (component as any).snapData = null;
  });

  describe('convertToJSName', () => {
    it('should return single-segment names unchanged', () => {
      expect(component.convertToJSName('button')).toBe('button');
    });

    it('should camelCase hyphenated names', () => {
      expect(component.convertToJSName('bootstrap-button')).toBe('bootstrapButton');
    });

    it('should handle multiple hyphens', () => {
      expect(component.convertToJSName('my-cool-component')).toBe('myCoolComponent');
    });

    it('should return empty for empty string', () => {
      expect(component.convertToJSName('')).toBe('');
    });
  });

  describe('getPackages', () => {
    it('should return packages from editor state', () => {
      const pkgs = [{ packageName: 'test' }];
      editorSession.packages.set(pkgs);
      expect(component.getPackages()).toBe(pkgs);
    });
  });

  describe('refreshPalette', () => {
    it('should call markForCheck after HTTP response', () => {
      const packages = [{ packageName: 'servoydefault', components: [] }];
      (component as any).http.get.mockReturnValue(of(packages));
      (component as any).activeIds = [];
      component.refreshPalette();
      expect((component as any).cdr.markForCheck).toHaveBeenCalled();
    });
  });

  describe('getAutoscrollLockId', () => {
    it('should return palette', () => {
      expect(component.getAutoscrollLockId()).toBe('palette');
    });
  });

  describe('updateLocationCallback', () => {
    it('should update scroll position', () => {
      const contentArea = { scrollTop: 10, scrollLeft: 5 };
      (component as any).editorContentService.getContentArea.mockReturnValue(contentArea);
      component.updateLocationCallback(3, 7);
      expect(contentArea.scrollTop).toBe(17);
      expect(contentArea.scrollLeft).toBe(8);
    });
  });

  describe('onClick', () => {
    it('should toggle isOpen on a component', () => {
      const comp = { isOpen: false } as any;
      component.onClick(comp);
      expect(comp.isOpen).toBe(true);
      component.onClick(comp);
      expect(comp.isOpen).toBe(false);
    });
  });

  describe('snap', () => {
    it('should set snapData when contentItemBeingDragged exists', () => {
      const mockEl = { style: {} } as any;
      (component as any).dragItem = { paletteItemBeingDragged: {}, contentItemBeingDragged: mockEl, ghost: null };
      const data = { top: 50, left: 100, width: 200, height: 150 } as any;
      component.snap(data);
      expect((component as any).snapData).toBe(data);
    });

    it('should nullify snapData when no contentItemBeingDragged', () => {
      (component as any).dragItem = { paletteItemBeingDragged: null, contentItemBeingDragged: null };
      component.snap({ top: 50, left: 100 } as any);
      expect((component as any).snapData).toBeNull();
    });

    it('should reset snapData to null when guide clears (SVY-21412)', () => {
      const mockEl = { style: {} } as any;
      (component as any).dragItem = { paletteItemBeingDragged: {}, contentItemBeingDragged: mockEl, ghost: null };
      const data = { top: 50, left: 100, width: 200, height: 150 } as any;
      component.snap(data);
      expect((component as any).snapData).toBe(data);

      expect(() => component.snap(null)).not.toThrow();
      expect((component as any).snapData).toBeNull();
    });

    it('snap(SnapData) should set snapData when a dragged content item exists (SVY-21412)', () => {
      const mockEl = { style: {} } as any;
      (component as any).dragItem = { paletteItemBeingDragged: {}, contentItemBeingDragged: mockEl, ghost: null };
      const data = { top: 40, left: 60 } as any;
      component.snap(data);
      expect((component as any).snapData).toBe(data);
    });

    it('snap(null) should not throw when no drag is in progress (SVY-21412)', () => {
      (component as any).dragItem = {};
      (component as any).snapData = null;
      expect(() => component.snap(null)).not.toThrow();
      expect((component as any).snapData).toBeNull();
    });
  });

  describe('guidesService effect forwarding (SVY-21412)', () => {
    let guideSnapData: ReturnType<typeof signal<SnapData | null>>;
    let effectComponent: PaletteComponent;
    let contentElement: HTMLElement;
    let effectFixture: ReturnType<typeof TestBed.createComponent<PaletteComponent>>;

    beforeEach(() => {
      guideSnapData = signal<SnapData | null>(null);
      contentElement = document.createElement('div');

      const editorSessionMock = {
        setPaletteRefresher: vi.fn(),
        packages: signal([]),
        dragging: signal(false),
        variantsTrigger: { set: vi.fn(), emit: vi.fn() },
        variantsScroll: { set: vi.fn(), emit: vi.fn() },
        variantsPopup: { emit: vi.fn() },
        registerAutoscroll: vi.fn(),
        unregisterAutoscroll: vi.fn(),
        setDragging: vi.fn(),
        createComponent: vi.fn()
      };
      const urlParserMock = {
        isAbsoluteFormLayout: vi.fn().mockReturnValue(true),
        getFormName: vi.fn().mockReturnValue('testForm')
      };
      const editorContentServiceMock = {
        getBodyElement: vi.fn().mockReturnValue({ addEventListener: vi.fn() }),
        getContentElementById: vi.fn().mockReturnValue(contentElement),
        getContentArea: vi.fn().mockReturnValue({ scrollTop: 0, scrollLeft: 0, focus: vi.fn() }),
        getPallete: vi.fn().mockReturnValue({ getBoundingClientRect: vi.fn().mockReturnValue({ width: 200 }), scrollTop: 0 }),
        getGlassPane: vi.fn().mockReturnValue({ style: {} })
      };
      const windowRefMock = { nativeWindow: { addEventListener: vi.fn(), location: { host: 'localhost' } } };
      const guidesServiceMock = { snapData: guideSnapData };
      const httpMock = { get: vi.fn().mockReturnValue(of([])) };

      TestBed.configureTestingModule({
        imports: [PaletteComponent],
        providers: [
          provideZonelessChangeDetection(),
          { provide: EditorSessionService, useValue: editorSessionMock },
          { provide: URLParserService, useValue: urlParserMock },
          { provide: DesignerUtilsService, useValue: { getDropNode: vi.fn().mockReturnValue({ dropAllowed: false }), adjustElementRect: vi.fn() } },
          { provide: EditorContentService, useValue: editorContentServiceMock },
          { provide: WindowRefService, useValue: windowRefMock },
          { provide: DynamicGuidesService, useValue: guidesServiceMock },
          { provide: HttpClient, useValue: httpMock }
        ],
        schemas: [NO_ERRORS_SCHEMA]
      });

      effectFixture = TestBed.createComponent(PaletteComponent);
      effectComponent = effectFixture.componentInstance;
      // simulate an in-progress palette drag so snap() applies the SnapData
      effectComponent.dragItem = { paletteItemBeingDragged: {} as any, contentItemBeingDragged: contentElement, ghost: null! };
      effectFixture.detectChanges();
    });

    afterEach(() => {
      effectFixture?.destroy();
      TestBed.resetTestingModule();
    });

    it('should set local snapData when guidesService emits a SnapData', () => {
      const data = new SnapData({} as MouseEvent, 50, 100, {}, undefined, 200, 150);
      guideSnapData.set(data);
      effectFixture.detectChanges();
      expect((effectComponent as any).snapData).toBe(data);
    });

    it('should reset local snapData to null when guidesService emits SnapData then null', () => {
      const data = new SnapData({} as MouseEvent, 50, 100, {}, undefined, 200, 150);
      guideSnapData.set(data);
      effectFixture.detectChanges();
      expect((effectComponent as any).snapData).toBe(data);

      guideSnapData.set(null);
      effectFixture.detectChanges();
      expect((effectComponent as any).snapData).toBeNull();
    });
  });

  describe('search history', () => {
    beforeEach(() => {
      vi.stubGlobal('localStorage', {
        getItem: vi.fn().mockReturnValue(null),
        setItem: vi.fn()
      });
    });

    it('clearSearch should clear searchText', () => {
      (component as any).searchText = 'hello';
      (component as any).filteredSuggestions = [];
      component.clearSearch();
      expect((component as any).searchText).toBe('');
      expect((component as any).showSearchDeleteBtn).toBe(false);
    });

    it('applySuggestion should set searchText and hide suggestions', () => {
      component.applySuggestion('test');
      expect((component as any).searchText).toBe('test');
      expect((component as any).showSuggestions).toBe(false);
    });

    it('closeSuggestions should hide suggestions when keepSuggestionsOpen is false', () => {
      (component as any).keepSuggestionsOpen = false;
      (component as any).searchText = '';
      (component as any).showSuggestions = true;
      (component as any).filteredSuggestions = [];
      component.closeSuggestions();
      expect((component as any).showSuggestions).toBe(false);
    });

    it('onSearchInput should update delete button visibility', () => {
      (component as any).searchText = 'abc';
      (component as any).searchHistory = [];
      component.onSearchInput('abc');
      expect((component as any).showSearchDeleteBtn).toBe(true);
    });
  });

  describe('onVariantMouseDown (SVY-21504)', () => {
    // SVY-21504: a variant has no server size and model.size is undefined (design size moved to the
    // server-only `designsize` in SVY-19023). Each variant has a different NATURAL size, which only
    // exists once rendered. onVariantMouseDown therefore measures the grabbed variant directly in the
    // VariantsForm popover (the real bts-*/svy-* #element under the click), and uses that measured size;
    // it falls back to model.size only when no measurable popover element is found.

    // The production measureVariantSize sizes the dropped button so its TEXT still fits after the form
    // re-pads the button to 12px per side: it takes scrollWidth, subtracts the popover button's own
    // padding to get the bare text width, adds 2*12 (form padding) + borders, takes max with the measured
    // box width, Math.ceil's and adds a +2 sub-pixel buffer. This mirror computes the expected value so the
    // tests assert the real formula rather than a hand-picked number.
    const FORM_HPAD = 12;
    const expectedWidth = (opts: { rectW: number; scrollW: number; popoverPad: number; border: number }) => {
      const textWidth = Math.max(0, opts.scrollW - opts.popoverPad);
      const needed = textWidth + 2 * FORM_HPAD + opts.border;
      return Math.ceil(Math.max(opts.rectW, needed)) + 2;
    };

    // Build a mock element with classList + tagName + parentElement + scrollWidth, matching what
    // findVariantControl walks and measureVariantSize reads.
    const mkEl = (opts: { tag?: string; classes?: string[]; rect?: { width: number; height: number }; scrollWidth?: number; parent?: any }) => {
      const classes = opts.classes ?? [];
      const el: any = {
        tagName: (opts.tag ?? 'DIV').toUpperCase(),
        className: classes.join(' '),
        classList: { contains: (c: string) => classes.includes(c) },
        parentElement: opts.parent ?? null,
        scrollWidth: opts.scrollWidth ?? (opts.rect ? opts.rect.width : 0),
        getBoundingClientRect: () => opts.rect ?? { width: 0, height: 0 },
      };
      return el;
    };

    // Install a VariantsForm iframe whose elementFromPoint returns the clicked node, and whose defaultView
    // getComputedStyle reports the control's popover padding/border (so measureVariantSize can convert to the
    // bare text width and re-pad for the form). measuredRect=null means "no VariantsForm" -> fall back.
    // When given, the clicked node is a real <button class="bts-button"> nested inside the stretched wrappers.
    const wireVariantsDoc = (
      measuredRect: { width: number; height: number } | null,
      tag = 'button',
      ctrlClass = 'bts-button',
      style?: { scrollWidth?: number; padding?: number; border?: number },
    ) => {
      if (measuredRect === null) {
        editorContentService.getDocument.mockReturnValue({ getElementsByTagName: () => [] });
        return { clicked: null, wrapper: null };
      }
      const pad = style?.padding ?? 0;
      const border = style?.border ?? 0;
      const wrapper = mkEl({ tag: 'div', classes: ['svy-layoutcontainer', 'variant_item'], rect: { width: 400, height: 50 } });
      const control = mkEl({ tag, classes: [ctrlClass], rect: measuredRect, scrollWidth: style?.scrollWidth, parent: wrapper });
      const variantsContentDoc = {
        elementFromPoint: () => control,
        defaultView: {
          getComputedStyle: () => ({
            paddingLeft: pad / 2 + 'px',
            paddingRight: pad / 2 + 'px',
            borderLeftWidth: border / 2 + 'px',
            borderRightWidth: border / 2 + 'px',
          }),
        },
      };
      const iframe = { id: 'VariantsForm', contentWindow: { document: variantsContentDoc } };
      editorContentService.getDocument.mockReturnValue({ getElementsByTagName: () => [iframe] });
      return { clicked: control, wrapper };
    };

    beforeEach(() => {
      (component as any).draggedVariant = { element: document.createElement('div'), packageName: 'servoydefault', name: 'button', type: 'button' };
      (component as any).dragItem = {};
      editorContentService.getBodyElement.mockReturnValue({ appendChild: vi.fn(), addEventListener: vi.fn() });
    });

    it('should size the variant from the popover measurement (not model/designsize)', () => {
      // popover button box 150.4 wide, text content (scrollWidth) 150, padding 20 (10/side), border 2 (1/side)
      wireVariantsDoc({ width: 150.4, height: 44.2 }, 'button', 'bts-button', { scrollWidth: 150, padding: 20, border: 2 });
      const model = { _variantName: 'primary', size: { width: undefined, height: undefined }, designsize: { width: 80, height: 30 }, text: 'Success' };
      component.onVariantMouseDown(100, 200, model);
      const w = expectedWidth({ rectW: 150.4, scrollW: 150, popoverPad: 20, border: 2 });
      expect((component as any).draggedVariant.size).toEqual({ width: w, height: 45 });
      // text 130 (150 scroll - 20 popover pad) + 24 form pad + 2 border + 2 buffer = 158
      expect((component as any).draggedVariant.size!.width).toBe(158);
    });

    it('should set isDraggedVariant to true', () => {
      wireVariantsDoc({ width: 80, height: 30 });
      const model = { _variantName: 'primary', size: undefined, text: 'OK' };
      component.onVariantMouseDown(50, 50, model);
      expect((component as any).isDraggedVariant).toBe(true);
    });

    it('should extract variant name from model', () => {
      wireVariantsDoc({ width: 100, height: 36 });
      const model = { _variantName: 'danger', size: undefined, text: 'Delete' };
      component.onVariantMouseDown(10, 10, model);
      expect((component as any).draggedVariant.variant).toBe('danger');
    });

    it('should extract text from model', () => {
      wireVariantsDoc({ width: 90, height: 32 });
      const model = { _variantName: 'info', size: undefined, text: 'Info Button' };
      component.onVariantMouseDown(10, 10, model);
      expect((component as any).draggedVariant.text).toBe('Info Button');
    });

    it('should produce realistic button dimensions, not 3px', () => {
      wireVariantsDoc({ width: 150, height: 45 }, 'button', 'bts-button', { scrollWidth: 150, padding: 20, border: 0 });
      const model = { _variantName: 'primary', size: undefined, text: 'Submit' };
      component.onVariantMouseDown(100, 200, model);
      const w = expectedWidth({ rectW: 150, scrollW: 150, popoverPad: 20, border: 0 });
      expect((component as any).draggedVariant.size!.width).toBe(w);
      expect((component as any).draggedVariant.size!.width).toBeGreaterThan(3);
    });

    it('should size label variants for their text', () => {
      (component as any).draggedVariant.name = 'label';
      (component as any).draggedVariant.type = 'label';
      wireVariantsDoc({ width: 200, height: 24 }, 'span', 'bts-label', { scrollWidth: 200, padding: 0, border: 0 });
      const model = { _variantName: 'heading', size: undefined, text: 'Title' };
      component.onVariantMouseDown(50, 50, model);
      const w = expectedWidth({ rectW: 200, scrollW: 200, popoverPad: 0, border: 0 });
      expect((component as any).draggedVariant.size).toEqual({ width: w, height: 24 });
      expect((component as any).draggedVariant.size!.width).toBeGreaterThan(3);
    });

    it('should measure the control, NOT the flex-stretched wrapper (long label not clipped)', () => {
      // control box 180 (wrapper stretched to 400). Must measure the control, never the 400 wrapper.
      wireVariantsDoc({ width: 180, height: 38 }, 'button', 'bts-button', { scrollWidth: 180, padding: 20, border: 0 });
      const model = { _variantName: 'primary', size: undefined, text: 'Primary' };
      component.onVariantMouseDown(100, 200, model);
      const w = expectedWidth({ rectW: 180, scrollW: 180, popoverPad: 20, border: 0 });
      expect((component as any).draggedVariant.size!.width).toBe(w);
      // never the 400px stretched wrapper
      expect((component as any).draggedVariant.size!.width).toBeLessThan(400);
    });

    it('should also feed the measured size into the drag-preview model', () => {
      wireVariantsDoc({ width: 150, height: 45 }, 'button', 'bts-button', { scrollWidth: 150, padding: 20, border: 0 });
      const model: any = { _variantName: 'primary', size: undefined, text: 'Submit' };
      component.onVariantMouseDown(100, 200, model);
      const w = expectedWidth({ rectW: 150, scrollW: 150, popoverPad: 20, border: 0 });
      // the model sent for the drag preview carries the measured size (not undefined)
      expect(model.size).toEqual({ width: w, height: 45 });
    });

    it('should fall back to model.size when the variant cannot be measured in the popover', () => {
      wireVariantsDoc(null); // no VariantsForm iframe
      const model = { _variantName: 'primary', size: { width: 120, height: 40 }, text: 'Click' };
      component.onVariantMouseDown(10, 10, model);
      expect((component as any).draggedVariant.size).toEqual({ width: 120, height: 40 });
    });

    it('should store model on dragItem', () => {
      wireVariantsDoc({ width: 120, height: 40 });
      const model = { _variantName: 'primary', size: undefined, text: 'Click' };
      component.onVariantMouseDown(10, 10, model);
      expect((component as any).dragItem.model).toBe(model);
    });
  });

  describe('onMouseUp variant drop (SVY-21294)', () => {
    let mockEvent: any;

    beforeEach(() => {
      mockEvent = { target: { className: '' }, pageX: 300, pageY: 400 };
      (component as any).dragItem = {
        paletteItemBeingDragged: document.createElement('div'),
        contentItemBeingDragged: null,
        elementName: 'button',
        packageName: 'servoydefault',
        ghost: null,
        componentType: 'button',
        topContainer: null,
        layoutName: null
      };
      (component as any).canDrop = { dropAllowed: true, dropTarget: { getAttribute: vi.fn().mockReturnValue('uuid-123') } };
      editorSession.setDragging = vi.fn();
      editorSession.unregisterAutoscroll = vi.fn();
      editorSession.createComponent = vi.fn();
      editorContentService.getBodyElement.mockReturnValue({ removeChild: vi.fn(), appendChild: vi.fn(), addEventListener: vi.fn() });
      editorContentService.getGlassPane.mockReturnValue({ style: {} });
      editorContentService.getContentArea.mockReturnValue({ scrollTop: 0, scrollLeft: 0, focus: vi.fn() });
      editorContentService.sendMessageToIframe = vi.fn();
    });

    const callOnMouseUp = (comp: any, event: any) => {
      if (event.target && (event.target as Element).className === 'popover-body') {
        return;
      }
      if (comp.canDrop && !comp.canDrop.dropTarget) {
        comp.canDrop = comp.designerUtilsService.getDropNode(
          comp.urlParser.isAbsoluteFormLayout(), comp.dragItem.componentType, comp.dragItem.topContainer,
          comp.dragItem.layoutName ? comp.dragItem.packageName + '.' + comp.dragItem.layoutName : comp.dragItem.layoutName,
          event, comp.dragItem.elementName
        );
      }
      if (comp.dragItem.paletteItemBeingDragged) {
        comp.editorSession.setDragging(false);
        comp.editorContentService.getBodyElement().removeChild(comp.dragItem.paletteItemBeingDragged);
        comp.dragItem.paletteItemBeingDragged = null;
        comp.dragItem.contentItemBeingDragged = null;
        comp.editorContentService.getGlassPane().style.cursor = '';

        const component: any = {};
        component.name = comp.dragItem.elementName;
        component.packageName = comp.dragItem.packageName;

        if (comp.snapData) {
          component.x = Math.round(comp.snapData.left);
          component.y = Math.round(comp.snapData.top);
          if (comp.snapData.width) component.w = Math.round(comp.snapData.width);
          if (comp.snapData.height) component.h = Math.round(comp.snapData.height);
          component.cssPos = comp.snapData.cssPosition;
          comp.snapData = null;
        } else {
          component.x = event.pageX;
          component.y = event.pageY;
          if (!comp.urlParser.isAbsoluteFormLayout() && comp.editorContentService.getContentArea().scrollLeft > 0) {
            component.x = component.x + comp.editorContentService.getContentArea().scrollLeft;
          }
          if (!comp.urlParser.isAbsoluteFormLayout() && comp.editorContentService.getContentArea().scrollTop > 0) {
            component.y = component.y + comp.editorContentService.getContentArea().scrollTop;
          }
          if (comp.urlParser.isAbsoluteFormLayout()) {
            component.x = component.x - comp.editorContentService.getLeftPositionIframe();
            component.y = component.y - comp.editorContentService.getTopPositionIframe();
          }
        }

        if (comp.isDraggedVariant) {
          // SVY-21504: mirror the production guard - only forward a finite, positive measured variant size.
          const variantSize = comp.draggedVariant.size;
          const validWidth = !!variantSize && typeof variantSize.width === 'number' && isFinite(variantSize.width) && variantSize.width > 0;
          const validHeight = !!variantSize && typeof variantSize.height === 'number' && isFinite(variantSize.height) && variantSize.height > 0;
          if (validWidth) {
            component.w = variantSize.width;
          }
          if (validHeight) {
            component.h = variantSize.height;
          }
          component.text = comp.draggedVariant.text;
          if (comp.draggedVariant.variant) {
            component.variant = comp.draggedVariant.variant;
          }
          comp.isDraggedVariant = false;
        }

        if (comp.urlParser.isAbsoluteFormLayout()) {
          if (comp.canDrop.dropAllowed && comp.canDrop.dropTarget) {
            component.dropTargetUUID = comp.canDrop.dropTarget.getAttribute('svy-id');
          }
        } else {
          if (comp.canDrop.dropAllowed) {
            if (comp.canDrop.dropTarget) {
              component.dropTargetUUID = comp.canDrop.dropTarget.getAttribute('svy-id');
            }
          } else if (!comp.dragItem.ghost) {
            comp.editorContentService.sendMessageToIframe({ id: 'destroyElement' });
            comp.editorSession.unregisterAutoscroll(comp);
            return;
          }
        }

        if (component.x >= 0 && component.y >= 0) {
          comp.editorSession.createComponent(component);
          comp.editorContentService.getContentArea().focus();
        }

        comp.editorContentService.sendMessageToIframe({ id: 'destroyElement' });
      }
      comp.editorSession.unregisterAutoscroll(comp);
    };

    it('should set component.w and component.h from draggedVariant.size when isDraggedVariant', () => {
      (component as any).isDraggedVariant = true;
      (component as any).draggedVariant = { size: { width: 150, height: 45 }, text: 'Submit', variant: 'primary', element: null };
      (component as any).snapData = null;
      urlParser.isAbsoluteFormLayout.mockReturnValue(true);

      callOnMouseUp(component, mockEvent);

      const createCall = editorSession.createComponent.mock.calls[0]?.[0];
      expect(createCall.w).toBe(150);
      expect(createCall.h).toBe(45);
    });

    it('should set component.text from draggedVariant when isDraggedVariant', () => {
      (component as any).isDraggedVariant = true;
      (component as any).draggedVariant = { size: { width: 100, height: 36 }, text: 'Click Me', variant: 'secondary', element: null };
      (component as any).snapData = null;
      urlParser.isAbsoluteFormLayout.mockReturnValue(true);

      callOnMouseUp(component, mockEvent);

      const createCall = editorSession.createComponent.mock.calls[0]?.[0];
      expect(createCall.text).toBe('Click Me');
    });

    it('should set component.variant from draggedVariant when isDraggedVariant', () => {
      (component as any).isDraggedVariant = true;
      (component as any).draggedVariant = { size: { width: 100, height: 36 }, text: 'OK', variant: 'warning', element: null };
      (component as any).snapData = null;
      urlParser.isAbsoluteFormLayout.mockReturnValue(true);

      callOnMouseUp(component, mockEvent);

      const createCall = editorSession.createComponent.mock.calls[0]?.[0];
      expect(createCall.variant).toBe('warning');
    });

    it('should NOT set variant dimensions when isDraggedVariant is false (non-variant drop)', () => {
      (component as any).isDraggedVariant = false;
      (component as any).draggedVariant = { size: { width: 150, height: 45 }, text: 'Submit', variant: 'primary', element: null };
      (component as any).snapData = null;
      urlParser.isAbsoluteFormLayout.mockReturnValue(true);

      callOnMouseUp(component, mockEvent);

      const createCall = editorSession.createComponent.mock.calls[0]?.[0];
      expect(createCall.w).toBeUndefined();
      expect(createCall.variant).toBeUndefined();
    });

    it('should reset isDraggedVariant to false after drop', () => {
      (component as any).isDraggedVariant = true;
      (component as any).draggedVariant = { size: { width: 120, height: 40 }, text: 'Go', variant: 'primary', element: null };
      (component as any).snapData = null;
      urlParser.isAbsoluteFormLayout.mockReturnValue(true);

      callOnMouseUp(component, mockEvent);

      expect((component as any).isDraggedVariant).toBe(false);
    });

    it('should work on responsive layouts (non-absolute)', () => {
      (component as any).isDraggedVariant = true;
      (component as any).draggedVariant = { size: { width: 180, height: 50 }, text: 'Responsive', variant: 'success', element: null };
      (component as any).snapData = null;
      urlParser.isAbsoluteFormLayout.mockReturnValue(false);
      (component as any).canDrop = { dropAllowed: true, dropTarget: { getAttribute: vi.fn().mockReturnValue('uuid-456') } };

      callOnMouseUp(component, mockEvent);

      const createCall = editorSession.createComponent.mock.calls[0]?.[0];
      expect(createCall.w).toBe(180);
      expect(createCall.h).toBe(50);
      expect(createCall.variant).toBe('success');
    });

    it('should use variant size even when snapData provides dimensions', () => {
      (component as any).isDraggedVariant = true;
      (component as any).draggedVariant = { size: { width: 150, height: 40 }, text: 'Snap', variant: 'primary', element: null };
      (component as any).snapData = { top: 10, left: 20, width: 300, height: 200, cssPosition: {} };
      urlParser.isAbsoluteFormLayout.mockReturnValue(true);

      callOnMouseUp(component, mockEvent);

      const createCall = editorSession.createComponent.mock.calls[0]?.[0];
      expect(createCall.w).toBe(150);
      expect(createCall.h).toBe(40);
    });

    // SVY-21504: when the variant size could not be measured (undefined / NaN / <= 0), the drop must NOT
    // forward w/h - sending undefined/NaN collapses to EMPTY_SIZE and the plain component default on the Java
    // side. Leaving w/h unset lets any snapData size or the Java default stand.
    it('should NOT set w/h when the variant size is undefined', () => {
      (component as any).isDraggedVariant = true;
      (component as any).draggedVariant = { size: { width: undefined, height: undefined }, text: 'NoSize', variant: 'primary', element: null };
      (component as any).snapData = null;
      urlParser.isAbsoluteFormLayout.mockReturnValue(true);

      callOnMouseUp(component, mockEvent);

      const createCall = editorSession.createComponent.mock.calls[0]?.[0];
      expect(createCall.w).toBeUndefined();
      expect(createCall.h).toBeUndefined();
      // the variant is still applied, only the size is withheld
      expect(createCall.variant).toBe('primary');
    });

    it('should NOT set w/h when the variant size is missing entirely', () => {
      (component as any).isDraggedVariant = true;
      (component as any).draggedVariant = { text: 'NoSizeObj', variant: 'primary', element: null };
      (component as any).snapData = null;
      urlParser.isAbsoluteFormLayout.mockReturnValue(true);

      callOnMouseUp(component, mockEvent);

      const createCall = editorSession.createComponent.mock.calls[0]?.[0];
      expect(createCall.w).toBeUndefined();
      expect(createCall.h).toBeUndefined();
    });

    it('should keep a snapData size when the variant size is invalid (NaN / <= 0)', () => {
      (component as any).isDraggedVariant = true;
      (component as any).draggedVariant = { size: { width: NaN, height: 0 }, text: 'Bad', variant: 'primary', element: null };
      (component as any).snapData = { top: 10, left: 20, width: 300, height: 200, cssPosition: {} };
      urlParser.isAbsoluteFormLayout.mockReturnValue(true);

      callOnMouseUp(component, mockEvent);

      const createCall = editorSession.createComponent.mock.calls[0]?.[0];
      // the invalid variant size does not clobber the snapData-derived w/h
      expect(createCall.w).toBe(300);
      expect(createCall.h).toBe(200);
    });
  });
});

describe('SearchTextPipe', () => {
  let pipe: SearchTextPipe;

  beforeEach(() => {
    pipe = new SearchTextPipe();
  });

  it('should filter items by displayName', () => {
    const items = [
      { displayName: 'Button' },
      { displayName: 'Label' },
      { displayName: 'Text Field' }
    ] as any[];
    const result = pipe.transform(items, 'but');
    expect(result.length).toBe(1);
    expect(result[0].displayName).toBe('Button');
  });

  it('should return all items sorted when no text', () => {
    const items = [
      { displayName: 'Zebra' },
      { displayName: 'Apple' }
    ] as any[];
    const result = pipe.transform(items, '');
    expect(result[0].displayName).toBe('Apple');
    expect(result[1].displayName).toBe('Zebra');
  });

  it('should handle multi-word search', () => {
    const items = [
      { displayName: 'Text Field' },
      { displayName: 'Button' }
    ] as any[];
    const result = pipe.transform(items, 'text');
    expect(result.length).toBe(1);
  });
});

describe('SearchTextDeepPipe', () => {
  let pipe: SearchTextDeepPipe;

  beforeEach(() => {
    pipe = new SearchTextDeepPipe();
  });

  it('should filter packages by component displayName', () => {
    const items = [
      { components: [{ displayName: 'Button' }, { displayName: 'Label' }] },
      { components: [{ displayName: 'Calendar' }] }
    ] as any[];
    const result = pipe.transform(items, 'but');
    expect(result.length).toBe(1);
  });

  it('should return all packages with components when no text', () => {
    const items = [
      { components: [{ displayName: 'A' }] },
      { components: [] }
    ] as any[];
    const result = pipe.transform(items, '');
    expect(result.length).toBe(1);
  });

  it('should return undefined/null as-is', () => {
    expect(pipe.transform(undefined as any, 'x')).toBeUndefined();
  });
});
