import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal } from '@angular/core';

import { EditorSessionService } from './editorsession.service';

describe('EditorSessionService', () => {
  let service: EditorSessionService;
  let wsSession: Record<string, ReturnType<typeof vi.fn>>;
  let editorContentService: Record<string, ReturnType<typeof vi.fn>>;
  let urlParser: Record<string, ReturnType<typeof vi.fn>>;

  beforeEach(() => {
    wsSession = {
      callService: vi.fn().mockResolvedValue(null),
    };
    editorContentService = {
      executeOnlyAfterInit: vi.fn(),
      getContentElement: vi.fn(),
      sendMessageToIframe: vi.fn(),
    };
    urlParser = {
      isAbsoluteFormLayout: vi.fn().mockReturnValue(true),
    };

    service = Object.create(EditorSessionService.prototype);
    (service as any).wsSession = wsSession;
    (service as any).editorContentService = editorContentService;
    (service as any).urlParser = urlParser;
    (service as any).selection = [];
    (service as any).selectionChangedListeners = [];
    (service as any).highlightChangedListeners = [];
    (service as any).dynamicGuidesChangedListeners = [];
    (service as any).statusText = signal('');
    (service as any).dragging = signal(false);
    (service as any).resizing = signal(false);
    (service as any).sameSizeIndicator = signal(false);
    (service as any).anchoringIndicator = signal(false);
    (service as any).packages = signal([]);
    (service as any).allowedChildren = { 'topContainer': ['component'] };
    (service as any).wizardProperties = { 'mySpec': ['prop1', 'prop2'] };
    (service as any).developerMenus = {
      FORM: ['menu1'],
      COMPONENT: { '': ['allMenu'], 'servoydefault-button': ['buttonMenu'] },
    };
    (service as any).bIsDirty = false;
    (service as any).inlineEdit = false;
    (service as any).lockAutoscrollId = '';
  });

  describe('selection management', () => {
    it('getSelection should return current selection', () => {
      (service as any).selection = ['uuid-1', 'uuid-2'];
      expect(service.getSelection()).toEqual(['uuid-1', 'uuid-2']);
    });

    it('updateSelection should update selection and notify listeners', () => {
      const listener = { selectionChanged: vi.fn() };
      service.addSelectionChangedListener(listener);
      service.updateSelection(['uuid-3']);
      expect(service.getSelection()).toEqual(['uuid-3']);
      expect(listener.selectionChanged).toHaveBeenCalledWith(['uuid-3'], undefined, undefined);
    });

    it('updateSelection should pass redrawDecorators flag', () => {
      const listener = { selectionChanged: vi.fn() };
      service.addSelectionChangedListener(listener);
      service.updateSelection(['uuid-1'], true, false);
      expect(listener.selectionChanged).toHaveBeenCalledWith(['uuid-1'], true, false);
    });
  });

  describe('listener management', () => {
    it('addSelectionChangedListener should return unsubscribe function', () => {
      const listener = { selectionChanged: vi.fn() };
      const unsub = service.addSelectionChangedListener(listener);
      service.updateSelection(['x']);
      expect(listener.selectionChanged).toHaveBeenCalledTimes(1);
      unsub();
      service.updateSelection(['y']);
      expect(listener.selectionChanged).toHaveBeenCalledTimes(1);
    });

    it('removeSelectionChangedListener should remove the listener', () => {
      const listener = { selectionChanged: vi.fn() };
      service.addSelectionChangedListener(listener);
      service.removeSelectionChangedListener(listener);
      service.updateSelection(['x']);
      expect(listener.selectionChanged).not.toHaveBeenCalled();
    });

    it('addHighlightChangedListener + fireHighlightChangedListeners', () => {
      const listener = { highlightChanged: vi.fn() };
      service.addHighlightChangedListener(listener);
      service.fireHighlightChangedListeners(true);
      expect(listener.highlightChanged).toHaveBeenCalledWith(true);
    });

    it('addDynamicGuidesChangedListener + fireShowDynamicGuidesChangedListeners', () => {
      const listener = { showDynamicGuidesChanged: vi.fn() };
      service.addDynamicGuidesChangedListener(listener);
      service.fireShowDynamicGuidesChangedListeners(false);
      expect(listener.showDynamicGuidesChanged).toHaveBeenCalledWith(false);
    });
  });

  describe('state management', () => {
    it('signals should return initial values', () => {
      expect(service.dragging()).toBe(false);
      expect(service.resizing()).toBe(false);
      expect(service.statusText()).toBe('');
      expect(service.sameSizeIndicator()).toBe(false);
      expect(service.anchoringIndicator()).toBe(false);
    });

    it('setStatusBarText should update signal', () => {
      service.setStatusBarText('hello');
      expect(service.statusText()).toBe('hello');
    });

    it('setDragging should update signal', () => {
      service.setDragging(true);
      expect(service.dragging()).toBe(true);
    });

    it('setSameSizeIndicator should update signal', () => {
      service.setSameSizeIndicator(true);
      expect(service.sameSizeIndicator()).toBe(true);
    });

    it('setAnchoringIndicator should update signal', () => {
      service.setAnchoringIndicator(true);
      expect(service.anchoringIndicator()).toBe(true);
    });
  });

  describe('dirty state', () => {
    it('isDirty should return false initially', () => {
      expect(service.isDirty()).toBe(false);
    });

    it('setDirty should update dirty state', () => {
      service.setDirty(true);
      expect(service.isDirty()).toBe(true);
    });
  });

  describe('inline edit mode', () => {
    it('isInlineEditMode should return false initially', () => {
      expect(service.isInlineEditMode()).toBe(false);
    });

    it('setInlineEditMode should update mode and call service', () => {
      service.setInlineEditMode(true);
      expect(service.isInlineEditMode()).toBe(true);
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'setInlineEditMode', { 'inlineEdit': true }, true);
    });
  });

  describe('getAllowedChildrenForContainer', () => {
    it('should return allowed children for a known container', () => {
      (service as any).allowedChildren = { 'myLayout': ['component', 'layout'] };
      expect(service.getAllowedChildrenForContainer('myLayout')).toEqual(['component', 'layout']);
    });

    it('should use topContainer key when container is falsy', () => {
      expect(service.getAllowedChildrenForContainer(null as any)).toEqual(['component']);
    });

    it('should return null when allowedChildren is null', () => {
      (service as any).allowedChildren = null;
      expect(service.getAllowedChildrenForContainer('anything')).toBeNull();
    });
  });

  describe('getWizardProperties', () => {
    it('should return properties for known spec', () => {
      expect(service.getWizardProperties('mySpec')).toEqual(['prop1', 'prop2']);
    });

    it('should return undefined for unknown spec', () => {
      expect(service.getWizardProperties('unknown')).toBeUndefined();
    });
  });

  describe('getDeveloperMenus', () => {
    it('should return form menus when isForm is true', () => {
      expect(service.getDeveloperMenus(true, '')).toEqual(['menu1']);
    });

    it('should return combined component menus (all + specific)', () => {
      expect(service.getDeveloperMenus(false, 'servoydefault-button')).toEqual(['allMenu', 'buttonMenu']);
    });

    it('should return only generic menus when no specific menus exist', () => {
      expect(service.getDeveloperMenus(false, 'unknown-comp')).toEqual(['allMenu']);
    });

    it('should return null when no menus exist', () => {
      (service as any).developerMenus = {};
      expect(service.getDeveloperMenus(false, 'anything')).toBeNull();
    });
  });

  describe('isAbsoluteFormLayout', () => {
    it('should delegate to urlParser', () => {
      urlParser.isAbsoluteFormLayout.mockReturnValue(true);
      expect(service.isAbsoluteFormLayout()).toBe(true);
      urlParser.isAbsoluteFormLayout.mockReturnValue(false);
      expect(service.isAbsoluteFormLayout()).toBe(false);
    });
  });

  describe('getFixedKeyEvent', () => {
    it('should return key event properties', () => {
      const event = { keyCode: 65, key: 'a', ctrlKey: true, shiftKey: false, altKey: false, metaKey: false } as KeyboardEvent;
      const result = service.getFixedKeyEvent(event);
      expect(result).toEqual({ keyCode: 65, ctrlKey: true, shiftKey: false, altKey: false, metaKey: false });
    });

    it('should set keyCode to 0 for standalone Meta key', () => {
      const event = { keyCode: 91, key: 'Meta', ctrlKey: false, shiftKey: false, altKey: false, metaKey: true } as KeyboardEvent;
      const result = service.getFixedKeyEvent(event);
      expect(result.keyCode).toBe(0);
    });

    it('should set keyCode to 0 for standalone Ctrl key', () => {
      const event = { keyCode: 17, key: 'Control', ctrlKey: true, shiftKey: false, altKey: false, metaKey: false } as KeyboardEvent;
      const result = service.getFixedKeyEvent(event);
      expect(result.keyCode).toBe(0);
    });

    it('should set keyCode to 0 for standalone Alt key', () => {
      const event = { keyCode: 18, key: 'Alt', ctrlKey: false, shiftKey: false, altKey: true, metaKey: false } as KeyboardEvent;
      const result = service.getFixedKeyEvent(event);
      expect(result.keyCode).toBe(0);
    });
  });

  describe('autoscroll registration', () => {
    it('registerAutoscroll should set lockAutoscrollId and emit', () => {
      const scrollComp = {
        getAutoscrollLockId: vi.fn().mockReturnValue('lock-1'),
        updateLocationCallback: vi.fn(),
        onMouseUp: vi.fn(),
        onMouseMove: vi.fn(),
      };
      (service as any).autoscrollTarget = signal(null);
      service.registerAutoscroll(scrollComp);
      expect((service as any).lockAutoscrollId).toBe('lock-1');
    });

    it('registerAutoscroll should reject if lockId does not match', () => {
      (service as any).lockAutoscrollId = 'existing-lock';
      (service as any).autoscrollTarget = signal(null);
      const scrollComp = {
        getAutoscrollLockId: vi.fn().mockReturnValue('different-lock'),
        updateLocationCallback: vi.fn(),
        onMouseUp: vi.fn(),
        onMouseMove: vi.fn(),
      };
      service.registerAutoscroll(scrollComp);
      expect((service as any).lockAutoscrollId).toBe('existing-lock');
    });

    it('unregisterAutoscroll should clear lock when id matches', () => {
      (service as any).lockAutoscrollId = 'lock-1';
      (service as any).autoscrollTarget = signal(null);
      const scrollComp = {
        getAutoscrollLockId: vi.fn().mockReturnValue('lock-1'),
        updateLocationCallback: vi.fn(),
        onMouseUp: vi.fn(),
        onMouseMove: vi.fn(),
      };
      service.unregisterAutoscroll(scrollComp);
      expect((service as any).lockAutoscrollId).toBe('');
    });
  });

  describe('getService', () => {
    it('should return this for $editorService', () => {
      expect(service.getService('$editorService')).toBe(service);
    });

    it('should return typesRegistry for $typesRegistry', () => {
      const registry = {};
      (service as any).typesRegistry = registry;
      expect(service.getService('$typesRegistry')).toBe(registry);
    });

    it('should return null for unknown service', () => {
      expect(service.getService('unknown')).toBeNull();
    });
  });

  describe('setSelection', () => {
    it('should store selection, call the service and notify listeners', () => {
      const listener = { selectionChanged: vi.fn() };
      service.addSelectionChangedListener(listener);
      service.setSelection(['a', 'b']);
      expect(service.getSelection()).toEqual(['a', 'b']);
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'setSelection', { selection: ['a', 'b'] }, true);
      expect(listener.selectionChanged).toHaveBeenCalledWith(['a', 'b']);
    });

    it('should skip the provided listener', () => {
      const skipped = { selectionChanged: vi.fn() };
      const other = { selectionChanged: vi.fn() };
      service.addSelectionChangedListener(skipped);
      service.addSelectionChangedListener(other);
      service.setSelection(['x'], skipped);
      expect(skipped.selectionChanged).not.toHaveBeenCalled();
      expect(other.selectionChanged).toHaveBeenCalledWith(['x']);
    });
  });

  describe('callService delegating methods', () => {
    it('connect should connect the websocket and request wizard/developer menus', () => {
      const connectedSession = { callService: vi.fn().mockResolvedValue(null) };
      const websocketService = {
        connect: vi.fn().mockReturnValue(connectedSession),
        getURLParameter: vi.fn().mockReturnValue('42'),
      };
      (service as any).websocketService = websocketService;
      urlParser.isAbsoluteFormLayout.mockReturnValue(true);
      service.connect();
      expect(websocketService.connect).toHaveBeenCalledWith('', ['42']);
      expect(connectedSession.callService).toHaveBeenCalledWith('formeditor', 'getWizardProperties');
      expect(connectedSession.callService).toHaveBeenCalledWith('formeditor', 'getDeveloperMenus');
    });

    it('activated should call the activated service', () => {
      service.activated();
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'activated');
    });

    it('initialized should call the initialized service', () => {
      service.initialized();
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'initialized');
    });

    it('keyPressed should map event flags to the service payload', () => {
      service.keyPressed({ ctrlKey: true, shiftKey: false, altKey: true, metaKey: false, keyCode: 65 });
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'keyPressed',
        { ctrl: true, shift: false, alt: true, meta: false, keyCode: 65 }, true);
    });

    it('sendChanges should call setProperties', () => {
      const props = { a: 1 };
      service.sendChanges(props);
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'setProperties', props, true);
    });

    it('moveResponsiveComponent should call moveComponent', () => {
      const props = { id: 'x' };
      service.moveResponsiveComponent(props);
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'moveComponent', props, true);
    });

    it('createComponent should call createComponent', () => {
      const comp = { name: 'button' };
      service.createComponent(comp);
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'createComponent', comp, true);
    });

    it('createComponents should call createComponents', () => {
      const comps = [{ name: 'a' }];
      service.createComponents(comps);
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'createComponents', comps, true);
    });

    it('addStyleVariantFor should wrap the category', () => {
      service.addStyleVariantFor('cat');
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'addStyleVariantFor', { p: 'cat' }, true);
    });

    it('editStyleVariantsFor should wrap the category', () => {
      service.editStyleVariantsFor('cat');
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'editStyleVariantsFor', { p: 'cat' }, true);
    });

    it('getVariantsForCategory should call the service with the category', () => {
      service.getVariantsForCategory('cat');
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'getVariantsForCategory', { variantCategory: 'cat' }, false);
    });

    it('duplicateGhosts should call duplicateGhosts', () => {
      const args = { uuids: ['a'], parentUuid: 'p', dropIndex: 0 };
      service.duplicateGhosts(args);
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'duplicateGhosts', args, true);
    });

    it('openElementWizard should wrap the element type', () => {
      service.openElementWizard('field');
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'openElementWizard', { elementType: 'field' }, true);
    });

    it('updateFieldPositioner should wrap the location', () => {
      service.updateFieldPositioner({ x: 1, y: 2 });
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'updateFieldPositioner', { location: { x: 1, y: 2 } }, true);
    });

    it('executeAction should pass the action name through', () => {
      service.executeAction('myAction', { a: 1 });
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'myAction', { a: 1 }, true);
    });

    it('openContainedForm should wrap the uuid', () => {
      service.openContainedForm('u1');
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'openContainedForm', { uuid: 'u1' }, true);
    });

    it('buildTiNG should call buildTiNG', () => {
      service.buildTiNG();
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'buildTiNG', {}, true);
    });

    it('consoleLog should wrap the message', () => {
      service.consoleLog('hi');
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'consoleLog', { message: 'hi' }, true);
    });

    it('getComponentPropertyWithTags should wrap svyId and propertyName', () => {
      service.getComponentPropertyWithTags('id1', 'prop');
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'getComponentPropertyWithTags', { svyId: 'id1', propertyName: 'prop' }, false);
    });

    it('setCssAnchoring should wrap selection and anchors', () => {
      const anchors = { top: '1', left: '2', bottom: '3', right: '4' };
      service.setCssAnchoring(['a'], anchors);
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'setCssAnchoring', { selection: ['a'], anchors }, true);
    });

    it('getFormFixedSize should call getFormFixedSize', () => {
      service.getFormFixedSize();
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'getFormFixedSize');
    });

    it('setFormFixedSize should pass the args', () => {
      service.setFormFixedSize({ width: '10px', height: '20px' });
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'setFormFixedSize', { width: '10px', height: '20px' });
    });

    it('getZoomLevel should call getZoomLevel', () => {
      service.getZoomLevel();
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'getZoomLevel', {}, false);
    });

    it('setZoomLevel should wrap the value', () => {
      service.setZoomLevel(3);
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'setZoomLevel', { zoomLevel: 3 }, false);
    });

    it('toggleShowData should call toggleShowData', () => {
      service.toggleShowData();
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'toggleShowData', null, true);
    });

    it('toggleShowWireframe should toggle the wireframe setting', () => {
      service.toggleShowWireframe();
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'toggleShow', { show: 'showWireframeInDesigner' }, false);
    });

    it('toggleHighlight should toggle the highlight setting', () => {
      service.toggleHighlight();
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'toggleShow', { show: 'showHighlightInDesigner' }, false);
    });

    it('toggleShowDynamicGuides should toggle the dynamic guides setting', () => {
      service.toggleShowDynamicGuides();
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'toggleShow', { show: 'showDynamicGuidesInDesigner' }, false);
    });

    it('isShowData should query the showData boolean state', () => {
      service.isShowData();
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'getBooleanState', { showData: true }, false);
    });

    it('isShowHighlight should query the showHighlight boolean state', () => {
      service.isShowHighlight();
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'getBooleanState', { showHighlight: true }, false);
    });

    it('getShortcuts should call getShortcuts', () => {
      service.getShortcuts();
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'getShortcuts');
    });

    it('getSuperForms should call getSuperForms', () => {
      service.getSuperForms();
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'getSuperForms');
    });

    it('hasCypressFormTest should call hasCypressFormTest', () => {
      service.hasCypressFormTest();
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'hasCypressFormTest');
    });

    it('getSnapThreshold should call getSnapThresholds', () => {
      service.getSnapThreshold();
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'getSnapThresholds', false);
    });

    it('openPackageManager should call openPackageManager', () => {
      service.openPackageManager();
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'openPackageManager', null, true);
    });
  });

  describe('sameSize', () => {
    it('should not send changes when fewer than 2 elements are selected', () => {
      (service as any).selection = ['only-one'];
      service.sameSize(true);
      expect(wsSession.callService).not.toHaveBeenCalledWith('formeditor', 'setProperties', expect.anything(), true);
    });

    it('should set the width of later elements to the first element width', () => {
      (service as any).selection = ['a', 'b'];
      editorContentService.getContentElement.mockImplementation((id: string) => {
        if (id === 'a') return { getBoundingClientRect: () => new DOMRect(0, 0, 100, 40) };
        if (id === 'b') return { getBoundingClientRect: () => new DOMRect(0, 0, 60, 80) };
        return null;
      });
      service.sameSize(true);
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'setProperties', { b: { width: 100, height: 80 } }, true);
    });

    it('should set the height of later elements to the first element height', () => {
      (service as any).selection = ['a', 'b'];
      editorContentService.getContentElement.mockImplementation((id: string) => {
        if (id === 'a') return { getBoundingClientRect: () => new DOMRect(0, 0, 100, 40) };
        if (id === 'b') return { getBoundingClientRect: () => new DOMRect(0, 0, 60, 80) };
        return null;
      });
      service.sameSize(false);
      expect(wsSession.callService).toHaveBeenCalledWith('formeditor', 'setProperties', { b: { width: 60, height: 40 } }, true);
    });
  });

  describe('palette refresher', () => {
    it('setPaletteRefresher + refreshPalette should delegate', () => {
      const refresher = { refreshPalette: vi.fn() };
      service.setPaletteRefresher(refresher);
      service.refreshPalette();
      expect(refresher.refreshPalette).toHaveBeenCalledOnce();
    });
  });

  describe('getSession', () => {
    it('should return the websocket session', () => {
      expect(service.getSession()).toBe(wsSession);
    });
  });

  describe('getDeveloperMenus edge case', () => {
    it('should return null when developerMenus has no COMPONENT entry', () => {
      (service as any).developerMenus = { FORM: ['f'] };
      expect(service.getDeveloperMenus(false, 'x')).toBeNull();
    });
  });
});
