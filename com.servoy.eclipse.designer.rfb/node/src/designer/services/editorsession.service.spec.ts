import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

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
      (service as any).autoscrollBehavior = new BehaviorSubject(null);
      service.registerAutoscroll(scrollComp);
      expect((service as any).lockAutoscrollId).toBe('lock-1');
    });

    it('registerAutoscroll should reject if lockId does not match', () => {
      (service as any).lockAutoscrollId = 'existing-lock';
      (service as any).autoscrollBehavior = new BehaviorSubject(null);
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
      (service as any).autoscrollBehavior = new BehaviorSubject(null);
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
});
