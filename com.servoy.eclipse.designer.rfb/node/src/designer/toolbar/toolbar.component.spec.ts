import { vi, describe, beforeEach, it, expect } from 'vitest';
import { signal } from '@angular/core';
import { ToolbarComponent, TOOLBAR_CATEGORIES } from './toolbar.component';

describe('ToolbarComponent', () => {
  let component: ToolbarComponent;
  let editorSession: any;
  let urlParser: any;

    let contentElement: any;

  beforeEach(() => {
    editorSession = {
      dragging: signal(false),
      packages: signal([]),
      showWireframe: signal(false),
      getSelection: vi.fn().mockReturnValue([]),
      addSelectionChangedListener: vi.fn(),
      executeAction: vi.fn(),
      getSession: vi.fn().mockReturnValue({ onopen: vi.fn() })
    };
    urlParser = {
      isAbsoluteFormLayout: vi.fn().mockReturnValue(true),
      isShowingContainer: vi.fn().mockReturnValue(null),
      isCSSPositionFormLayout: vi.fn().mockReturnValue(false),
      isFormComponent: vi.fn().mockReturnValue(false)
    };

    component = Object.create(ToolbarComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).urlParser = urlParser;
    (component as any).renderer = { setStyle: vi.fn(), addClass: vi.fn(), removeClass: vi.fn() };
    contentElement = null;
    (component as any).editorContentService = {
      getContentElement: vi.fn().mockImplementation(() => contentElement),
      executeOnlyAfterInit: vi.fn(),
      sendMessageToIframe: vi.fn(),
      querySelectorAllInContent: vi.fn().mockReturnValue([]),
      querySelectorAll: vi.fn().mockReturnValue([]),
      getDesignerElementById: vi.fn().mockReturnValue(null),
      getPallete: vi.fn().mockReturnValue({ style: {} })
    };
    (component as any).designSize = { setEditor: vi.fn(), createItems: vi.fn(), setupItems: vi.fn() };
    (component as any).items = new Map();
    (component as any).TOOLBAR_CATEGORIES = TOOLBAR_CATEGORIES;
  });

  describe('add', () => {
    it('should create a new category array and add item', () => {
      const item = { text: 'Test' } as any;
      component.add(item, TOOLBAR_CATEGORIES.ELEMENTS);
      expect(component.hasCategoryItems(TOOLBAR_CATEGORIES.ELEMENTS)).toBe(true);
      expect(component.getCategoryItems(TOOLBAR_CATEGORIES.ELEMENTS)).toContain(item);
    });

    it('should add to existing category', () => {
      const item1 = { text: 'A' } as any;
      const item2 = { text: 'B' } as any;
      component.add(item1, TOOLBAR_CATEGORIES.FORM);
      component.add(item2, TOOLBAR_CATEGORIES.FORM);
      expect(component.getCategoryItems(TOOLBAR_CATEGORIES.FORM).length).toBe(2);
    });
  });

  describe('hasCategoryItems', () => {
    it('should return false for empty category', () => {
      expect(component.hasCategoryItems(TOOLBAR_CATEGORIES.ZOOM)).toBe(false);
    });
  });

  describe('getCategoryItems', () => {
    it('should return empty array for missing category', () => {
      expect(component.getCategoryItems(TOOLBAR_CATEGORIES.STICKY)).toEqual([]);
    });
  });

  describe('selectionChanged', () => {
    it('should update btnZoomOut enabled based on showingContainer', () => {
      (component as any).btnZoomOut = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveUp = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveDown = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnZoomIn = { enabled: signal<(() => boolean) | boolean>(false) };
      urlParser.isShowingContainer.mockReturnValue('container1');
      urlParser.isAbsoluteFormLayout.mockReturnValue(true);
      component.selectionChanged(['id1']);
      expect((component as any).btnZoomOut.enabled()).toBe(true);
    });

    it('should enable move buttons for single selection in responsive', () => {
      (component as any).btnZoomOut = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveUp = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveDown = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnZoomIn = { enabled: signal<(() => boolean) | boolean>(false) };
      urlParser.isAbsoluteFormLayout.mockReturnValue(false);
      urlParser.isShowingContainer.mockReturnValue(null);
      component.selectionChanged(['id1']);
      expect((component as any).btnMoveUp.enabled()).toBe(true);
      expect((component as any).btnMoveDown.enabled()).toBe(true);
    });

    it('should enable btnZoomIn when a layout container is selected in responsive', () => {
      (component as any).btnZoomOut = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveUp = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveDown = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnZoomIn = { enabled: signal<(() => boolean) | boolean>(false) };
      urlParser.isAbsoluteFormLayout.mockReturnValue(false);
      urlParser.isShowingContainer.mockReturnValue(null);
      contentElement = {
        classList: { contains: (c: string) => c === 'svy-layoutcontainer' },
        getAttribute: () => null
      };
      component.selectionChanged(['id1']);
      expect((component as any).btnZoomIn.enabled()).toBe(true);
    });

    it('should disable btnZoomIn when nothing is selected in responsive', () => {
      (component as any).btnZoomOut = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveUp = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveDown = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnZoomIn = { enabled: signal<(() => boolean) | boolean>(true) };
      urlParser.isAbsoluteFormLayout.mockReturnValue(false);
      urlParser.isShowingContainer.mockReturnValue(null);
      component.selectionChanged([]);
      expect((component as any).btnZoomIn.enabled()).toBe(false);
    });

    it('should disable btnZoomIn when the selected element is not a layout container', () => {
      (component as any).btnZoomOut = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveUp = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveDown = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnZoomIn = { enabled: signal<(() => boolean) | boolean>(true) };
      urlParser.isAbsoluteFormLayout.mockReturnValue(false);
      urlParser.isShowingContainer.mockReturnValue(null);
      contentElement = {
        classList: { contains: () => false },
        getAttribute: () => null
      };
      component.selectionChanged(['id1']);
      expect((component as any).btnZoomIn.enabled()).toBe(false);
    });

    it('should disable btnZoomIn for the main container', () => {
      (component as any).btnZoomOut = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveUp = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnMoveDown = { enabled: signal<(() => boolean) | boolean>(false) };
      (component as any).btnZoomIn = { enabled: signal<(() => boolean) | boolean>(true) };
      urlParser.isAbsoluteFormLayout.mockReturnValue(false);
      urlParser.isShowingContainer.mockReturnValue(null);
      contentElement = {
        classList: { contains: (c: string) => c === 'svy-layoutcontainer' },
        getAttribute: (a: string) => a === 'data-maincontainer' ? 'true' : null
      };
      component.selectionChanged(['id1']);
      expect((component as any).btnZoomIn.enabled()).toBe(false);
    });
  });

  describe('isZoomableLayoutContainerSelected', () => {
    it('should return false when nothing is selected', () => {
      expect((component as any).isZoomableLayoutContainerSelected([])).toBe(false);
    });

    it('should return false for a multi-element selection', () => {
      expect((component as any).isZoomableLayoutContainerSelected(['a', 'b'])).toBe(false);
    });

    it('should return false when the element cannot be resolved', () => {
      contentElement = null;
      expect((component as any).isZoomableLayoutContainerSelected(['id1'])).toBe(false);
    });

    it('should return true for a single layout container', () => {
      contentElement = {
        classList: { contains: (c: string) => c === 'svy-layoutcontainer' },
        getAttribute: () => null
      };
      expect((component as any).isZoomableLayoutContainerSelected(['id1'])).toBe(true);
    });

    it('should return false for a responsive container', () => {
      contentElement = {
        classList: { contains: (c: string) => c === 'svy-layoutcontainer' || c === 'svy-responsivecontainer' },
        getAttribute: () => null
      };
      expect((component as any).isZoomableLayoutContainerSelected(['id1'])).toBe(false);
    });
  });

  describe('createItems (absolute layout)', () => {
    beforeEach(() => {
      (component as any).cdr = { markForCheck: vi.fn() };
      editorSession.toggleHighlight = vi.fn().mockResolvedValue(true);
      editorSession.toggleShowDynamicGuides = vi.fn().mockResolvedValue(true);
      editorSession.toggleShowData = vi.fn();
      editorSession.toggleShowWireframe = vi.fn().mockResolvedValue(true);
      editorSession.openElementWizard = vi.fn();
      editorSession.fireHighlightChangedListeners = vi.fn();
      editorSession.fireShowDynamicGuidesChangedListeners = vi.fn();
      editorSession.showWireframe.set(false);
      urlParser.isAbsoluteFormLayout.mockReturnValue(true);
      component.createItems();
    });

    it('should create the core absolute-layout toolbar items', () => {
      expect((component as any).btnPlaceField).toBeDefined();
      expect((component as any).btnHighlightWebcomponents).toBeDefined();
      expect((component as any).btnToggleDynamicGuides).toBeDefined();
      expect((component as any).btnAlignActions).toBeDefined();
      expect((component as any).btnSpaceDistributionActions).toBeDefined();
    });

    it('btnPlaceField click should open the field wizard', () => {
      (component as any).btnPlaceField.onclick();
      expect(editorSession.openElementWizard).toHaveBeenCalledWith('field');
    });

    it('btnHighlightWebcomponents click should toggle highlight and fire listeners', async () => {
      (component as any).btnHighlightWebcomponents.onclick();
      await Promise.resolve();
      expect(editorSession.toggleHighlight).toHaveBeenCalled();
      expect(editorSession.fireHighlightChangedListeners).toHaveBeenCalledWith(true);
    });

    it('btnToggleShowData click should call toggleShowData', () => {
      (component as any).btnToggleShowData.onclick();
      expect(editorSession.toggleShowData).toHaveBeenCalled();
    });

    it('btnReload click should execute the reload action', () => {
      (component as any).btnReload.onclick();
      expect(editorSession.executeAction).toHaveBeenCalledWith('reload');
    });

    it('btnOpenInBrowser click should execute the openInBrowser action', () => {
      (component as any).btnOpenInBrowser.onclick();
      expect(editorSession.executeAction).toHaveBeenCalledWith('openInBrowser');
    });

    it('btnTabSequence click should not act with a single selection', () => {
      editorSession.getSelection.mockReturnValue(['only']);
      (component as any).btnTabSequence.onclick();
      expect(editorSession.executeAction).not.toHaveBeenCalledWith('setTabSequence');
    });

    it('btnTabSequence click should set tab sequence for multiple selection', () => {
      editorSession.getSelection.mockReturnValue(['a', 'b']);
      (component as any).btnTabSequence.onclick();
      expect(editorSession.executeAction).toHaveBeenCalledWith('setTabSequence');
    });

    it('btnZoomIn click should execute the zoomIn action', () => {
      (component as any).btnZoomIn.onclick();
      expect(editorSession.executeAction).toHaveBeenCalledWith('zoomIn');
    });

    it('btnZoomOut click should execute the zoomOut action', () => {
      (component as any).btnZoomOut.onclick();
      expect(editorSession.executeAction).toHaveBeenCalledWith('zoomOut');
    });

    it('btnOrderingActionsCSSForm onselection should map each ordering action', () => {
      const btn = (component as any).btnOrderingActionsCSSForm;
      btn.onselection('Bring forward');
      expect(editorSession.executeAction).toHaveBeenCalledWith('z_order_bring_to_front_one_step');
      btn.onselection('Send backward');
      expect(editorSession.executeAction).toHaveBeenCalledWith('z_order_send_to_back_one_step');
      btn.onselection('Bring to front');
      expect(editorSession.executeAction).toHaveBeenCalledWith('z_order_bring_to_front');
      btn.onselection('Send to back');
      expect(editorSession.executeAction).toHaveBeenCalledWith('z_order_send_to_back');
    });

    it('btnSpaceDistributionActions onselection should map each distribution action', () => {
      const btn = (component as any).btnSpaceDistributionActions;
      vi.spyOn(component, 'updateSelection').mockImplementation(() => undefined);
      btn.onselection('Horizontal Spacing');
      expect(editorSession.executeAction).toHaveBeenCalledWith('horizontal_spacing');
      btn.onselection('Vertical Centers');
      expect(editorSession.executeAction).toHaveBeenCalledWith('vertical_centers');
      btn.onselection('Upward');
      expect(editorSession.executeAction).toHaveBeenCalledWith('vertical_pack');
    });

    it('btnAlignActions onselection SAME_WIDTH should call sameSize(true)', () => {
      editorSession.sameSize = vi.fn();
      (component as any).btnAlignActions.onselection('Same Width');
      expect(editorSession.sameSize).toHaveBeenCalledWith(true);
    });

    it('btnAlignActions onselection SAME_HEIGHT should call sameSize(false)', () => {
      editorSession.sameSize = vi.fn();
      (component as any).btnAlignActions.onselection('Same Height');
      expect(editorSession.sameSize).toHaveBeenCalledWith(false);
    });

    it('btnSolutionCss getIconStyle returns the right icon per selection', () => {
      const btn = (component as any).btnSolutionCss;
      expect(btn.getIconStyle('Solution CSS')).toHaveProperty('background-image');
      expect(btn.getIconStyle('unknown')).toEqual({});
    });

    it('btnToggleI18NValues click should toggle the label and call the service', () => {
      editorSession.toggleShowI18NValues = vi.fn().mockResolvedValue(undefined);
      const btn = (component as any).btnToggleI18NValues;
      btn.state.set(false);
      btn.onclick();
      expect(btn.state()).toBe(true);
      expect(btn.text).toBe('Show I18N text as keys');
      expect(editorSession.toggleShowI18NValues).toHaveBeenCalled();
    });

    it('btnHideInheritedElements click should toggle state and execute the action', () => {
      vi.spyOn(component, 'applyHideInherited').mockImplementation(() => undefined);
      const btn = (component as any).btnHideInheritedElements;
      btn.state.set(false);
      btn.onclick();
      expect(btn.state()).toBe(true);
      expect(editorSession.executeAction).toHaveBeenCalledWith('toggleHideInherited');
    });
  });

  describe('createItems (responsive layout)', () => {
    beforeEach(() => {
      (component as any).cdr = { markForCheck: vi.fn() };
      editorSession.openElementWizard = vi.fn();
      urlParser.isAbsoluteFormLayout.mockReturnValue(false);
      component.createItems();
    });

    it('should not create the dynamic guides button in responsive layout', () => {
      expect((component as any).btnToggleDynamicGuides).toBeUndefined();
    });

    it('btnMoveUp click should execute responsive_move_up', () => {
      (component as any).btnMoveUp.onclick();
      expect(editorSession.executeAction).toHaveBeenCalledWith('responsive_move_up');
    });

    it('btnMoveDown click should execute responsive_move_down', () => {
      (component as any).btnMoveDown.onclick();
      expect(editorSession.executeAction).toHaveBeenCalledWith('responsive_move_down');
    });

    it('btnSetMaxLevelContainer onclick should set the zoom/max level', () => {
      editorSession.maxLevel = signal(0);
      editorSession.setZoomLevel = vi.fn();
      (component as any).btnSetMaxLevelContainer.onclick('5');
      expect(editorSession.maxLevel()).toBe(5);
      expect((component as any).editorContentService.sendMessageToIframe).toHaveBeenCalledWith({ id: 'maxLevel', value: '5' });
      expect(editorSession.setZoomLevel).toHaveBeenCalledWith(5);
    });
  });

  describe('toggleShowSolutionLayoutsCss / toggleShowSolutionCss', () => {
    beforeEach(() => {
      editorSession.showSolutionSpecificLayoutContainerClasses = signal(false);
      editorSession.showSolutionCss = signal(false);
      editorSession.toggleShowSolutionLayoutsCss = vi.fn().mockResolvedValue(true);
      editorSession.toggleShowSolutionCss = vi.fn().mockResolvedValue(true);
    });

    it('toggleShowSolutionLayoutsCss should set the signal and apply styles', async () => {
      vi.spyOn(component, 'setSolutionLayoutsCss').mockImplementation(() => undefined);
      component.toggleShowSolutionLayoutsCss();
      await Promise.resolve();
      expect(editorSession.showSolutionSpecificLayoutContainerClasses()).toBe(true);
      expect(component.setSolutionLayoutsCss).toHaveBeenCalledWith(true);
    });

    it('toggleShowSolutionCss should set the signal and apply styles', async () => {
      vi.spyOn(component, 'setShowSolutionCss').mockImplementation(() => undefined);
      component.toggleShowSolutionCss();
      await Promise.resolve();
      expect(editorSession.showSolutionCss()).toBe(true);
      expect(component.setShowSolutionCss).toHaveBeenCalledWith(true);
    });
  });

  describe('setSolutionLayoutsCss', () => {
    it('should add classes when state is true', () => {
      const element = { getAttribute: () => 'classA classB' };
      (component as any).editorContentService.querySelectorAllInContent.mockReturnValue([element]);
      component.setSolutionLayoutsCss(true);
      expect((component as any).renderer.addClass).toHaveBeenCalledWith(element, 'classA');
      expect((component as any).renderer.addClass).toHaveBeenCalledWith(element, 'classB');
    });

    it('should remove classes when state is false', () => {
      const element = { getAttribute: () => 'classA' };
      (component as any).editorContentService.querySelectorAllInContent.mockReturnValue([element]);
      component.setSolutionLayoutsCss(false);
      expect((component as any).renderer.removeClass).toHaveBeenCalledWith(element, 'classA');
    });
  });

  describe('setShowSolutionCss', () => {
    it('should enable/disable stylesheet links based on state', () => {
      const link = { disabled: false } as HTMLLinkElement;
      (component as any).editorContentService.querySelectorAllInContent.mockReturnValue([link]);
      component.setShowSolutionCss(false);
      expect(link.disabled).toBe(true);
      component.setShowSolutionCss(true);
      expect(link.disabled).toBe(false);
    });
  });

  describe('btnVisualFeedbackOptions / btnSolutionCss selection logic', () => {
    beforeEach(() => {
      (component as any).cdr = { markForCheck: vi.fn() };
      editorSession.openElementWizard = vi.fn();
      editorSession.sameSizeIndicator = signal(false);
      editorSession.anchoringIndicator = signal(false);
      editorSession.setSameSizeIndicator = vi.fn((v: boolean) => editorSession.sameSizeIndicator.set(v));
      editorSession.setAnchoringIndicator = vi.fn((v: boolean) => editorSession.anchoringIndicator.set(v));
      editorSession.showSolutionCss = signal(false);
      editorSession.showSolutionSpecificLayoutContainerClasses = signal(false);
      urlParser.isAbsoluteFormLayout.mockReturnValue(true);
      component.createItems();
    });

    it('btnVisualFeedbackOptions onselection should toggle the same-size indicator', () => {
      (component as any).btnVisualFeedbackOptions.onselection('Selected Element Same Size Indicator');
      expect(editorSession.setSameSizeIndicator).toHaveBeenCalledWith(true);
      expect((component as any).btnVisualFeedbackOptions.list[1].iconStyle).toBeDefined();
    });

    it('btnVisualFeedbackOptions onselection should toggle the anchoring indicator', () => {
      (component as any).btnVisualFeedbackOptions.onselection('Selected Element Anchoring Indicator');
      expect(editorSession.setAnchoringIndicator).toHaveBeenCalledWith(true);
    });

    it('btnSolutionCss onclick with "Solution CSS" should enable both css toggles', () => {
      const toggleCss = vi.spyOn(component, 'toggleShowSolutionCss').mockImplementation(() => undefined);
      const toggleLayouts = vi.spyOn(component, 'toggleShowSolutionLayoutsCss').mockImplementation(() => undefined);
      (component as any).btnSolutionCss.onclick('Solution CSS');
      expect(toggleCss).toHaveBeenCalled();
      expect(toggleLayouts).toHaveBeenCalled();
    });

    it('btnSolutionCss onclick with "No Solution CSS" should disable css when currently on', () => {
      editorSession.showSolutionCss.set(true);
      editorSession.showSolutionSpecificLayoutContainerClasses.set(true);
      const toggleCss = vi.spyOn(component, 'toggleShowSolutionCss').mockImplementation(() => undefined);
      (component as any).btnSolutionCss.onclick('No Solution CSS');
      expect(toggleCss).toHaveBeenCalled();
    });

    it('btnSolutionCss getIconStyle returns distinct icons for each option', () => {
      const btn = (component as any).btnSolutionCss;
      expect(btn.getIconStyle('Solution CSS')).not.toEqual(btn.getIconStyle('No Solution CSS'));
      expect(btn.getIconStyle('No Solution CSS for Layouts')).toHaveProperty('background-image');
    });
  });

  describe('btnAlignActions onselection alignment branches', () => {
    beforeEach(() => {
      (component as any).cdr = { markForCheck: vi.fn() };
      editorSession.openElementWizard = vi.fn();
      editorSession.sendChanges = vi.fn();
      urlParser.isAbsoluteFormLayout.mockReturnValue(true);
      component.createItems();
      vi.spyOn(component, 'updateSelection').mockImplementation(() => undefined);
      vi.spyOn(component, 'updateElementPositionUsingParentPosition').mockImplementation(() => undefined);
    });

    const twoElements = (a: DOMRect, b: DOMRect) => {
      editorSession.getSelection.mockReturnValue(['a', 'b']);
      (component as any).editorContentService.getContentElement = vi.fn().mockImplementation((id: string) => ({
        getBoundingClientRect: () => (id === 'a' ? a : b)
      }));
    };

    it('ALIGN_LEFT should move all elements to the leftmost x', () => {
      twoElements(new DOMRect(10, 0, 50, 20), new DOMRect(40, 0, 50, 20));
      (component as any).btnAlignActions.onselection('Align Left');
      expect(editorSession.sendChanges).toHaveBeenCalled();
      const changes = editorSession.sendChanges.mock.calls[0][0];
      // the element at x=40 gets moved to x=10
      expect(Object.values(changes).some((c: any) => c.x === 10)).toBe(true);
    });

    it('ALIGN_RIGHT should align right edges', () => {
      twoElements(new DOMRect(10, 0, 50, 20), new DOMRect(40, 0, 80, 20));
      (component as any).btnAlignActions.onselection('Align Right');
      expect(editorSession.sendChanges).toHaveBeenCalled();
    });

    it('ALIGN_TOP should align the top edges', () => {
      twoElements(new DOMRect(0, 10, 50, 20), new DOMRect(0, 40, 50, 20));
      (component as any).btnAlignActions.onselection('Align Top');
      expect(editorSession.sendChanges).toHaveBeenCalled();
    });

    it('ALIGN_BOTTOM should align the bottom edges', () => {
      twoElements(new DOMRect(0, 10, 50, 20), new DOMRect(0, 40, 50, 30));
      (component as any).btnAlignActions.onselection('Align Bottom');
      expect(editorSession.sendChanges).toHaveBeenCalled();
    });

    it('ALIGN_CENTER should center elements horizontally', () => {
      editorSession.getSelection.mockReturnValue(['a', 'b', 'c']);
      (component as any).editorContentService.getContentElement = vi.fn().mockImplementation((id: string) => ({
        getBoundingClientRect: () => new DOMRect(id === 'a' ? 0 : id === 'b' ? 100 : 50, 0, 40, 20)
      }));
      (component as any).btnAlignActions.onselection('Align Center');
      expect(editorSession.sendChanges).toHaveBeenCalled();
    });

    it('ALIGN_MIDDLE should center elements vertically', () => {
      editorSession.getSelection.mockReturnValue(['a', 'b', 'c']);
      (component as any).editorContentService.getContentElement = vi.fn().mockImplementation((id: string) => ({
        getBoundingClientRect: () => new DOMRect(0, id === 'a' ? 0 : id === 'b' ? 100 : 50, 40, 20)
      }));
      (component as any).btnAlignActions.onselection('Align Middle');
      expect(editorSession.sendChanges).toHaveBeenCalled();
    });

    it('should do nothing for align with a single selection', () => {
      editorSession.getSelection.mockReturnValue(['only']);
      (component as any).btnAlignActions.onselection('Align Left');
      expect(editorSession.sendChanges).not.toHaveBeenCalled();
    });
  });

  describe('setupItems', () => {
    let cdr: { markForCheck: ReturnType<typeof vi.fn> };

    beforeEach(() => {
      cdr = { markForCheck: vi.fn() };
      (component as any).cdr = cdr;
      editorSession.openElementWizard = vi.fn();
      urlParser.isAbsoluteFormLayout.mockReturnValue(true);
      component.createItems();
      // the promise-returning state queries used across setupItems
      editorSession.showSameSizeIndicator = vi.fn().mockResolvedValue(false);
      editorSession.showAnchoringIndicator = vi.fn().mockResolvedValue(false);
      editorSession.isShowDynamicGuides = vi.fn().mockResolvedValue(true);
      editorSession.isShowData = vi.fn().mockResolvedValue(false);
      editorSession.isShowWireframe = vi.fn().mockResolvedValue(false);
      editorSession.isShowHighlight = vi.fn().mockResolvedValue(true);
      editorSession.isHideInherited = vi.fn().mockResolvedValue(false);
      editorSession.isShowSolutionLayoutsCss = vi.fn().mockResolvedValue(true);
      editorSession.isShowSolutionCss = vi.fn().mockResolvedValue(true);
      editorSession.getZoomLevel = vi.fn().mockResolvedValue(0);
      editorSession.isShowI18NValues = vi.fn().mockResolvedValue(false);
      editorSession.setSameSizeIndicator = vi.fn();
      editorSession.setAnchoringIndicator = vi.fn();
      editorSession.fireShowDynamicGuidesChangedListeners = vi.fn();
      editorSession.fireHighlightChangedListeners = vi.fn();
      editorSession.setZoomLevel = vi.fn();
      editorSession.showWireframe = signal(false);
      editorSession.showSolutionSpecificLayoutContainerClasses = signal(false);
      editorSession.showSolutionCss = signal(false);
      editorSession.maxLevel = signal(0);
      editorSession.isShowingContainer = urlParser.isShowingContainer;
    });

    it('should populate the sticky categories and query editor state for absolute layout', async () => {
      component.setupItems();
      expect(component.elements).toBeDefined();
      expect(component.form).toBeDefined();
      expect(editorSession.showSameSizeIndicator).toHaveBeenCalled();
      expect(editorSession.isShowDynamicGuides).toHaveBeenCalled();
      await Promise.resolve();
      await Promise.resolve();
      expect(editorSession.fireShowDynamicGuidesChangedListeners).toHaveBeenCalledWith(true);
    });

    it('should hide the place field/tab sequence buttons for responsive layout', () => {
      urlParser.isAbsoluteFormLayout.mockReturnValue(false);
      component.setupItems();
      expect((component as any).btnPlaceField.hide).toBe(true);
      expect((component as any).btnTabSequence.hide).toBe(true);
      expect((component as any).btnHideInheritedElements.hide).toBe(true);
    });
  });

  describe('ngOnInit', () => {
    it('should set up items when the websocket session opens', () => {
      (component as any).cdr = { markForCheck: vi.fn() };
      const onopen = vi.fn();
      editorSession.getSession.mockReturnValue({ onopen });
      const setupSpy = vi.spyOn(component, 'setupItems').mockImplementation(() => undefined);
      component.ngOnInit();
      // invoke the registered onopen callback
      const cb = onopen.mock.calls[0][0];
      cb();
      expect(setupSpy).toHaveBeenCalled();
      expect((component as any).designSize.setupItems).toHaveBeenCalled();
    });
  });

  describe('applyHideInherited', () => {
    it('should hide inherited elements when hideInherited is true', () => {
      (component as any).cdr = { markForCheck: vi.fn() };
      const inheritedEl = {};
      (component as any).editorContentService.executeOnlyAfterInit = vi.fn((cb: () => void) => cb());
      (component as any).editorContentService.querySelectorAllInContent = vi.fn().mockReturnValue([inheritedEl]);
      (component as any).editorContentService.querySelectorAll = vi.fn().mockReturnValue([]);
      editorSession.getAllContentElements = vi.fn().mockReturnValue([]);
      editorSession.setSelection = vi.fn();
      vi.spyOn(component, 'updateSelection').mockImplementation(() => undefined);
      vi.spyOn(component, 'updateSameSizeIndicator').mockImplementation(() => undefined);
      component.applyHideInherited(true);
      expect((component as any).renderer.setStyle).toHaveBeenCalledWith(inheritedEl, 'visibility', 'hidden');
    });
  });

  describe('updateElementPositionUsingParentPosition', () => {
    it('should offset x/y by the parent form-component rect', () => {
      const parentFC = { getBoundingClientRect: () => new DOMRect(10, 20, 100, 100) };
      const element = {
        closest: (sel: string) => (sel === '.svy-formcomponent' ? parentFC : null),
        classList: { contains: () => false }
      } as unknown as HTMLElement;
      const rect = new DOMRect(50, 70, 30, 30);
      component.updateElementPositionUsingParentPosition(element, rect, true, true);
      expect(rect.x).toBe(40);
      expect(rect.y).toBe(50);
    });

    it('should not offset when the element itself is the form component', () => {
      const element = {
        closest: () => ({ getBoundingClientRect: () => new DOMRect(10, 20, 100, 100) }),
        classList: { contains: () => true }
      } as unknown as HTMLElement;
      const rect = new DOMRect(50, 70, 30, 30);
      component.updateElementPositionUsingParentPosition(element, rect, true, true);
      expect(rect.x).toBe(50);
      expect(rect.y).toBe(70);
    });
  });
});
