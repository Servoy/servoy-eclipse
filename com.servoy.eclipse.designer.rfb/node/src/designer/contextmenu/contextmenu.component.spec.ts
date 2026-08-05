import { vi, describe, beforeEach, it, expect } from 'vitest';
import { ContextMenuComponent, ContextmenuItem } from './contextmenu.component';

describe('ContextMenuComponent', () => {
  let component: ContextMenuComponent;
  let editorSession: any;
  let editorContentService: any;
  let urlParser: any;

  beforeEach(() => {
    editorSession = {
      getState: vi.fn().mockReturnValue({ packages: [] }),
      getSelection: vi.fn().mockReturnValue([]),
      setSelection: vi.fn(),
      executeAction: vi.fn(),
      sendChanges: vi.fn(),
      isDirty: vi.fn().mockReturnValue(false),
      sameSize: vi.fn(),
      setCssAnchoring: vi.fn(),
      getAllowedChildrenForContainer: vi.fn().mockReturnValue(null),
      getWizardProperties: vi.fn().mockReturnValue(null),
      getDeveloperMenus: vi.fn().mockReturnValue(null),
      getShortcuts: vi.fn().mockResolvedValue({}),
      getSuperForms: vi.fn().mockResolvedValue([]),
      hasCypressFormTest: vi.fn().mockResolvedValue(false),
      keyPressed: vi.fn(),
      openConfigurator: vi.fn(),
      createComponent: vi.fn(),
      executeDeveloperMenu: vi.fn()
    };
    editorContentService = {
      getContentArea: vi.fn().mockReturnValue({ addEventListener: vi.fn() }),
      getContentElement: vi.fn().mockReturnValue(null),
      getContentForm: vi.fn().mockReturnValue(document.createElement('div')),
      getGlassPane: vi.fn().mockReturnValue({ getBoundingClientRect: vi.fn().mockReturnValue({ left: 0, top: 0 }) }),
      getBodyElement: vi.fn().mockReturnValue({ addEventListener: vi.fn() }),
      getPallete: vi.fn().mockReturnValue({ offsetWidth: 200 }),
      querySelector: vi.fn().mockReturnValue({ offsetHeight: 40 }),
      querySelectorAll: vi.fn().mockReturnValue([])
    };
    urlParser = {
      isAbsoluteFormLayout: vi.fn().mockReturnValue(true),
      isCSSPositionFormLayout: vi.fn().mockReturnValue(false),
      isShowingContainer: vi.fn().mockReturnValue(null),
      isFormComponent: vi.fn().mockReturnValue(false)
    };

    component = Object.create(ContextMenuComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).editorContentService = editorContentService;
    (component as any).urlParser = urlParser;
    (component as any).windowRef = { nativeWindow: { addEventListener: vi.fn(), navigator: { clipboard: { read: vi.fn().mockResolvedValue([]) } } } };
    (component as any).element = { nativeElement: document.createElement('div') };
    (component as any).menuItems = [];
    (component as any).selection = [];
    (component as any).selectionAnchor = 0;
  });

  describe('adjustMenuPosition', () => {
    it('should not throw when called without nativeElement arg', () => {
      editorContentService.querySelector.mockReturnValue(null);
      expect(() => component.adjustMenuPosition()).not.toThrow();
    });
  });

  describe('ContextmenuItem', () => {
    it('should execute function and return false', () => {
      const fn = vi.fn();
      const item = new ContextmenuItem('Test', fn);
      const result = item.execute();
      expect(fn).toHaveBeenCalled();
      expect(result).toBe(false);
    });

    it('should store text and isDevMenu', () => {
      const item = new ContextmenuItem('Dev Item', vi.fn(), true);
      expect(item.text).toBe('Dev Item');
      expect(item.isDevMenu).toBe(true);
    });
  });
});
