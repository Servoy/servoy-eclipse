import { vi, describe, beforeEach, it, expect } from 'vitest';
import { signal } from '@angular/core';
import { GhostsContainerComponent, GHOST_TYPES } from './ghostscontainer.component';

describe('GhostsContainerComponent', () => {
  let component: GhostsContainerComponent;
  let editorSession: any;
  let editorContentService: any;
  let mockDoc: any;

  beforeEach(() => {
    editorSession = {
      dragging: signal(false),
      ghosthandle: signal(false),
      getSelection: vi.fn().mockReturnValue([]),
      setSelection: vi.fn(),
      addSelectionChangedListener: vi.fn().mockReturnValue(() => undefined),
      getGhostComponents: vi.fn().mockResolvedValue({ ghostContainers: [] }),
      sendChanges: vi.fn(),
      duplicateGhosts: vi.fn(),
      openContainedForm: vi.fn(),
      updateSelection: vi.fn(),
      registerAutoscroll: vi.fn(),
      unregisterAutoscroll: vi.fn()
    };
    mockDoc = {
      addEventListener: vi.fn(),
      listeners: {} as Record<string, ((...args: any[]) => void)[]>,
    };
    mockDoc.addEventListener.mockImplementation((event: string, handler: (...args: any[]) => void) => {
      if (!mockDoc.listeners[event]) mockDoc.listeners[event] = [];
      mockDoc.listeners[event].push(handler);
    });

    editorContentService = {
      addContentMessageListener: vi.fn(),
      removeContentMessageListener: vi.fn(),
      getDocument: vi.fn().mockReturnValue(mockDoc),
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
    (component as any).elementRef = () => ({ nativeElement: { classList: { value: 'ghostcontainer' } } });
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
      const spy = vi.spyOn(component, 'renderGhosts').mockImplementation(() => undefined);
      component.contentMessageReceived('renderGhosts', { property: '' });
      expect(spy).toHaveBeenCalled();
    });

    it('should update formWidth/formHeight on updateFormSize', () => {
      vi.spyOn(component, 'renderGhosts').mockImplementation(() => undefined);
      component.contentMessageReceived('updateFormSize', { property: '', width: 1024, height: 768 });
      expect((component as any).formWidth).toBe(1024);
      expect((component as any).formHeight).toBe(768);
    });

    it('should call hideShowGhosts with hidden on hideGhostContainer', () => {
      const spy = vi.spyOn(component, 'hideShowGhosts').mockImplementation(() => undefined);
      component.contentMessageReceived('hideGhostContainer', { property: '' });
      expect(spy).toHaveBeenCalledWith('hidden');
    });

    it('should call hideShowGhosts with visible on other messages', () => {
      const spy = vi.spyOn(component, 'hideShowGhosts').mockImplementation(() => undefined);
      vi.spyOn(component, 'renderGhosts').mockImplementation(() => undefined);
      component.contentMessageReceived('renderGhosts', { property: '' });
      expect(spy).toHaveBeenCalledWith('visible');
    });
  });

  describe('selectionChanged', () => {
    it('should call renderGhosts on designerChange', () => {
      const spy = vi.spyOn(component, 'renderGhosts').mockImplementation(() => undefined);
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
      (component as any).elementRef = () => undefined;
      expect(() => component.hideShowGhosts('visible')).not.toThrow();
    });
  });

  describe('ghost drag-copy (Ctrl+drag duplication)', () => {
    let configGhost: any;
    let ghostContainer: any;
    let draggingClone: HTMLElement;

    beforeEach(() => {
      configGhost = {
        uuid: 'ghost-1',
        text: 'Column 1',
        type: GHOST_TYPES.GHOST_TYPE_CONFIGURATION,
        class: '',
        location: { x: 0, y: 0 },
        size: { width: 100, height: 30 },
        style: { left: '20px', top: '20px' }
      };
      ghostContainer = {
        uuid: 'parent-1',
        ghosts: [
          configGhost,
          {
            uuid: 'ghost-2', text: 'Column 2', type: GHOST_TYPES.GHOST_TYPE_CONFIGURATION,
            class: '', location: { x: 100, y: 0 }, size: { width: 100, height: 30 },
            style: { left: '120px', top: '20px' }
          }
        ],
        style: {} as CSSStyleDeclaration
      };
      draggingClone = document.createElement('div');
      component.draggingGhost = configGhost;
      component.draggingInGhostContainer = ghostContainer;
      component.draggingClone = draggingClone;
      component.mousedownpoint = { x: 50, y: 50 };
      component.containerLeftOffset = 0;
      component.containerTopOffset = 0;
      component.leftOffsetRelativeToSelectedGhost = 10;
      component.topOffsetRelativeToSelectedGhost = 10;
      component.dragCopy = false;
      (component as any).dragCopyInitialized = false;
      (component as any).originalGhostPositions = null;
      (component as any).ghostContainers = signal([ghostContainer]);
    });

    it('should activate copy mode on Ctrl+mousemove for GHOST_TYPE_CONFIGURATION', () => {
      const moveEvent = { pageX: 60, pageY: 70, ctrlKey: true, metaKey: false } as MouseEvent;
      component.onMouseMove(moveEvent);
      expect(component.dragCopy).toBe(true);
    });

    it('should preserve original ghost positions during copy-drag', () => {
      const moveEvent = { pageX: 60, pageY: 70, ctrlKey: true, metaKey: false } as MouseEvent;
      component.onMouseMove(moveEvent);
      expect((component as any).originalGhostPositions).toBeInstanceOf(Map);
      expect((component as any).originalGhostPositions.get('ghost-1')).toEqual({ x: 0, y: 0 });
      expect((component as any).originalGhostPositions.get('ghost-2')).toEqual({ x: 100, y: 0 });

      const moveEvent2 = { pageX: 80, pageY: 90, ctrlKey: true, metaKey: false } as MouseEvent;
      component.onMouseMove(moveEvent2);
      expect(configGhost.location.x).toBe(0);
      expect(configGhost.location.y).toBe(0);
    });

    it('should cancel copy mode when Ctrl is released before drop', () => {
      (component as any).urlParser.isAbsoluteFormLayout.mockReturnValue(true);
      component.ngOnInit();

      const moveEvent = { pageX: 60, pageY: 70, ctrlKey: true, metaKey: false } as MouseEvent;
      component.onMouseMove(moveEvent);
      expect(component.dragCopy).toBe(true);

      const keyupHandlers = mockDoc.listeners['keyup'] || [];
      keyupHandlers.forEach((h: (event: KeyboardEvent) => void) => h(new KeyboardEvent('keyup', { key: 'Control' })));
      expect(component.dragCopy).toBe(false);
    });

    it('should call duplicateGhosts on mouseUp when dragCopy is true', () => {
      editorSession.getSelection.mockReturnValue(['ghost-1']);
      component.dragCopy = true;
      (component as any).dragCopyInitialized = true;
      vi.spyOn(component, 'renderGhosts').mockImplementation(() => undefined);

      const upEvent = { pageX: 80, pageY: 90 } as MouseEvent;
      component.onMouseUp(upEvent);

      expect(editorSession.duplicateGhosts).toHaveBeenCalledWith({
        uuids: ['ghost-1'],
        parentUuid: 'parent-1',
        dropIndex: expect.any(Number)
      });
      expect(editorSession.sendChanges).not.toHaveBeenCalled();
    });

    it('should not activate copy mode for GHOST_TYPE_PART', () => {
      const partGhost = {
        uuid: 'part-1', text: 'Body', type: GHOST_TYPES.GHOST_TYPE_PART,
        class: '', location: { x: 0, y: 300 }, size: { width: 800, height: 20 },
        style: { top: '300px', right: '-90px' }
      };
      component.draggingGhost = partGhost as any;
      component.draggingClone = null!;
      (component as any).draggingGhostComponent = document.createElement('div');
      (component as any).lastMouseY = 50;

      const moveEvent = { pageX: 60, pageY: 70, ctrlKey: true, metaKey: false } as MouseEvent;
      component.onMouseMove(moveEvent);
      expect(component.dragCopy).toBe(false);
    });

    it('should not activate copy mode for inherited ghosts', () => {
      configGhost.class = 'ghost inherited_element';
      const moveEvent = { pageX: 60, pageY: 70, ctrlKey: true, metaKey: false } as MouseEvent;
      component.onMouseMove(moveEvent);
      expect(component.dragCopy).toBe(false);
    });

    it('should reset drag copy state on cleanupDragCopyState', () => {
      const moveEvent = { pageX: 60, pageY: 70, ctrlKey: true, metaKey: false } as MouseEvent;
      component.onMouseMove(moveEvent);
      expect(component.dragCopy).toBe(true);
      expect((component as any).dragCopyInitialized).toBe(true);
      expect((component as any).originalGhostPositions).toBeInstanceOf(Map);

      (component as any).cleanupDragCopyState();
      expect(component.dragCopy).toBe(false);
      expect((component as any).dragCopyInitialized).toBe(false);
      expect((component as any).originalGhostPositions).toBeNull();
    });
  });
});
