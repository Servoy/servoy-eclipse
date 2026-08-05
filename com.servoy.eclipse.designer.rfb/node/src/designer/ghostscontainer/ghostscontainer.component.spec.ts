import { vi, describe, beforeEach, it, expect } from 'vitest';
import { GhostsContainerComponent, GHOST_TYPES } from './ghostscontainer.component';

describe('GhostsContainerComponent', () => {
  let component: GhostsContainerComponent;
  let editorSession: any;
  let editorContentService: any;

  beforeEach(() => {
    editorSession = {
      getState: vi.fn().mockReturnValue({ dragging: false, ghosthandle: false }),
      getSelection: vi.fn().mockReturnValue([]),
      setSelection: vi.fn(),
      addSelectionChangedListener: vi.fn().mockReturnValue(() => {}),
      getGhostComponents: vi.fn().mockResolvedValue({ ghostContainers: [] }),
      sendChanges: vi.fn(),
      openContainedForm: vi.fn(),
      updateSelection: vi.fn(),
      registerAutoscroll: vi.fn(),
      unregisterAutoscroll: vi.fn()
    };
    editorContentService = {
      addContentMessageListener: vi.fn(),
      removeContentMessageListener: vi.fn(),
      getDocument: vi.fn().mockReturnValue({ addEventListener: vi.fn() }),
      querySelector: vi.fn().mockReturnValue(null),
      querySelectorAll: vi.fn().mockReturnValue([]),
      getContentArea: vi.fn().mockReturnValue({ getElementsByClassName: vi.fn().mockReturnValue({ length: 0, item: vi.fn() }) }),
      getContentElement: vi.fn().mockReturnValue(undefined),
      getGlassPane: vi.fn().mockReturnValue({ style: {}, offsetHeight: 500 }),
      getBodyElement: vi.fn().mockReturnValue(document.createElement('div')),
      getContent: vi.fn().mockReturnValue({ getBoundingClientRect: vi.fn().mockReturnValue({ left: 0, right: 500, top: 0, bottom: 500 }) })
    };

    component = Object.create(GhostsContainerComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).editorContentService = editorContentService;
    (component as any).renderer = { setStyle: vi.fn(), setAttribute: vi.fn() };
    (component as any).urlParser = { isAbsoluteFormLayout: vi.fn().mockReturnValue(true), getFormWidth: vi.fn().mockReturnValue(800), getFormHeight: vi.fn().mockReturnValue(600) };
    (component as any).ghostContainers = null;
    (component as any).ghostOffset = 20;
    (component as any).draggingGhost = null;
    (component as any).elementRef = { nativeElement: { classList: { value: 'ghostcontainer' } } };
    (component as any).formWidth = 800;
    (component as any).formHeight = 600;
    (component as any).partTopPosition = 0;
    (component as any).topLimit = 0;
    (component as any).bottomLimit = 0;
    (component as any).isLowestPart = false;
  });

  describe('getAutoscrollLockId', () => {
    it('should return ghost-container', () => {
      expect(component.getAutoscrollLockId()).toBe('ghost-container');
    });
  });

  describe('contentMessageReceived', () => {
    it('should call renderGhosts on renderGhosts message', () => {
      const spy = vi.spyOn(component, 'renderGhosts').mockImplementation(() => {});
      component.contentMessageReceived('renderGhosts', { property: '' });
      expect(spy).toHaveBeenCalled();
    });

    it('should update formWidth/formHeight on updateFormSize', () => {
      vi.spyOn(component, 'renderGhosts').mockImplementation(() => {});
      component.contentMessageReceived('updateFormSize', { property: '', width: 1024, height: 768 });
      expect((component as any).formWidth).toBe(1024);
      expect((component as any).formHeight).toBe(768);
    });

    it('should call hideShowGhosts with hidden on hideGhostContainer', () => {
      const spy = vi.spyOn(component, 'hideShowGhosts').mockImplementation(() => {});
      component.contentMessageReceived('hideGhostContainer', { property: '' });
      expect(spy).toHaveBeenCalledWith('hidden');
    });

    it('should call hideShowGhosts with visible on other messages', () => {
      const spy = vi.spyOn(component, 'hideShowGhosts').mockImplementation(() => {});
      vi.spyOn(component, 'renderGhosts').mockImplementation(() => {});
      component.contentMessageReceived('renderGhosts', { property: '' });
      expect(spy).toHaveBeenCalledWith('visible');
    });
  });

  describe('selectionChanged', () => {
    it('should call renderGhosts on designerChange', () => {
      const spy = vi.spyOn(component, 'renderGhosts').mockImplementation(() => {});
      component.selectionChanged(['id1'], false, true);
      expect(spy).toHaveBeenCalled();
    });
  });

  describe('openContainedForm', () => {
    it('should call editorSession.openContainedForm for non-part ghosts', () => {
      const ghost = { uuid: 'abc', type: GHOST_TYPES.GHOST_TYPE_COMPONENT } as any;
      component.openContainedForm(ghost);
      expect(editorSession.openContainedForm).toHaveBeenCalledWith('abc');
    });

    it('should not call openContainedForm for part ghosts', () => {
      const ghost = { uuid: 'abc', type: GHOST_TYPES.GHOST_TYPE_PART } as any;
      component.openContainedForm(ghost);
      expect(editorSession.openContainedForm).not.toHaveBeenCalled();
    });
  });

  describe('hideShowGhosts', () => {
    it('should not throw when elementRef is undefined', () => {
      (component as any).elementRef = undefined;
      expect(() => component.hideShowGhosts('visible')).not.toThrow();
    });
  });
});
