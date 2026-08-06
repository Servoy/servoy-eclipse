import { vi, describe, beforeEach, it, expect } from 'vitest';
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
      getState: vi.fn().mockReturnValue({ packages: [], dragging: false }),
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
    (component as any).guidesService = { snapDataListener: { subscribe: vi.fn() } };
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
      editorSession.getState.mockReturnValue({ packages: pkgs });
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
