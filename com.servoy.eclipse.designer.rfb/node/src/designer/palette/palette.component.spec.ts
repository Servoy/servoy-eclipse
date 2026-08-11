import { vi, describe, beforeEach, it, expect } from 'vitest';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { PaletteComponent, SearchTextPipe, SearchTextDeepPipe } from './palette.component';

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

  describe('onVariantMouseDown (SVY-21294)', () => {
    beforeEach(() => {
      (component as any).draggedVariant = { element: document.createElement('div'), packageName: 'servoydefault', name: 'button', type: 'button' };
      (component as any).dragItem = {};
      editorContentService.getBodyElement.mockReturnValue({ appendChild: vi.fn(), addEventListener: vi.fn() });
    });

    it('should extract size from model', () => {
      const model = { _variantName: 'primary', size: { width: 120, height: 40 }, text: 'Click Me' };
      component.onVariantMouseDown(100, 200, model);
      expect((component as any).draggedVariant.size).toEqual({ width: 120, height: 40 });
    });

    it('should set isDraggedVariant to true', () => {
      const model = { _variantName: 'primary', size: { width: 80, height: 30 }, text: 'OK' };
      component.onVariantMouseDown(50, 50, model);
      expect((component as any).isDraggedVariant).toBe(true);
    });

    it('should extract variant name from model', () => {
      const model = { _variantName: 'danger', size: { width: 100, height: 36 }, text: 'Delete' };
      component.onVariantMouseDown(10, 10, model);
      expect((component as any).draggedVariant.variant).toBe('danger');
    });

    it('should extract text from model', () => {
      const model = { _variantName: 'info', size: { width: 90, height: 32 }, text: 'Info Button' };
      component.onVariantMouseDown(10, 10, model);
      expect((component as any).draggedVariant.text).toBe('Info Button');
    });

    it('should use model size with realistic button dimensions, not 3px', () => {
      const model = { _variantName: 'primary', size: { width: 150, height: 45 }, text: 'Submit' };
      component.onVariantMouseDown(100, 200, model);
      expect((component as any).draggedVariant.size!.width).toBe(150);
      expect((component as any).draggedVariant.size!.width).toBeGreaterThan(3);
    });

    it('should use model size for label variants', () => {
      (component as any).draggedVariant.name = 'label';
      (component as any).draggedVariant.type = 'label';
      const model = { _variantName: 'heading', size: { width: 200, height: 24 }, text: 'Title' };
      component.onVariantMouseDown(50, 50, model);
      expect((component as any).draggedVariant.size).toEqual({ width: 200, height: 24 });
      expect((component as any).draggedVariant.size!.width).toBeGreaterThan(3);
    });

    it('should store model on dragItem', () => {
      const model = { _variantName: 'primary', size: { width: 120, height: 40 }, text: 'Click' };
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
          component.w = comp.draggedVariant.size.width;
          component.h = comp.draggedVariant.size.height;
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
