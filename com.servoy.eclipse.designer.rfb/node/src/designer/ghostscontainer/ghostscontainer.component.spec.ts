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

  describe('renderGhostsInternal', () => {
    beforeEach(() => {
      (component as any).ghostContainers = signal([]);
    });

    it('should position an absolute-layout ghost container at the form size', () => {
      (component as any).formWidth = 400;
      (component as any).formHeight = 300;
      const container: any = {
        uuid: 'c1',
        ghosts: [{ uuid: 'g1', type: GHOST_TYPES.GHOST_TYPE_COMPONENT, location: { x: 10, y: 20 }, size: { width: 50, height: 30 } }]
      };
      (component as any).renderGhostsInternal([container]);
      expect(container.style.left).toBe('20px');
      expect(container.style.width).toBe('400px');
      expect(container.style.height).toBe('300px');
      // component ghost styled with its own location and offset
      expect(container.ghosts[0].style.left).toBe('10px');
    });

    it('should mark a selected ghost with the selection colors', () => {
      editorSession.getSelection.mockReturnValue(['g1']);
      const container: any = {
        uuid: 'c1',
        ghosts: [{ uuid: 'g1', type: GHOST_TYPES.GHOST_TYPE_COMPONENT, location: { x: 0, y: 0 }, size: { width: 10, height: 10 } }]
      };
      (component as any).renderGhostsInternal([container]);
      expect(container.ghosts[0].style.background).toBe('#07f');
      expect(container.ghosts[0].style.color).toBe('#fff');
    });

    it('should hide the form ghost', () => {
      const container: any = {
        uuid: 'c1',
        ghosts: [{ uuid: 'f1', type: GHOST_TYPES.GHOST_TYPE_FORM, location: { x: 0, y: 0 }, size: { width: 10, height: 10 } }]
      };
      (component as any).renderGhostsInternal([container]);
      expect(container.ghosts[0].style.display).toBe('none');
    });
  });

  describe('onMouseDown', () => {
    beforeEach(() => {
      (component as any).ghostContainers = signal([]);
    });

    it('should select the ghost and start dragging on a left-button click', () => {
      editorSession.getSelection.mockReturnValue([]);
      const ghost: any = { uuid: 'g1', type: GHOST_TYPES.GHOST_TYPE_INVISIBLE, location: { x: 0, y: 0 }, size: { width: 10, height: 10 } };
      const container: any = { uuid: 'c1', ghosts: [ghost] };
      const target = document.createElement('div');
      const parent = document.createElement('div');
      parent.appendChild(target);
      component.onMouseDown({ button: 0, pageX: 5, pageY: 6, currentTarget: target } as unknown as MouseEvent, ghost, container);
      expect(editorSession.setSelection).toHaveBeenCalledWith(['g1']);
      expect(editorSession.dragging()).toBe(true);
      expect((component as any).draggingGhost).toBe(ghost);
    });

    it('should toggle selection on ctrl+click without immediately selecting', () => {
      const ghost: any = { uuid: 'g1', type: GHOST_TYPES.GHOST_TYPE_COMPONENT, location: { x: 0, y: 0 }, size: { width: 10, height: 10 } };
      const container: any = { uuid: 'c1', ghosts: [ghost] };
      const target = document.createElement('div');
      document.createElement('div').appendChild(target);
      const parent = document.createElement('div');
      parent.appendChild(target);
      component.onMouseDown({ button: 0, ctrlKey: true, pageX: 5, pageY: 6, clientX: 5, clientY: 6, currentTarget: target } as unknown as MouseEvent, ghost, container);
      expect((component as any).pendingCtrlToggleGhost).toBe(ghost);
      expect(editorSession.setSelection).not.toHaveBeenCalled();
    });
  });

  describe('onMouseUp - part resize', () => {
    beforeEach(() => {
      (component as any).ghostContainers = signal([]);
    });

    it('should send the new part position on drop', () => {
      const ghost: any = { uuid: 'part-1', type: GHOST_TYPES.GHOST_TYPE_PART, location: { x: 0, y: 100 } };
      (component as any).draggingGhost = ghost;
      (component as any).draggingInGhostContainer = { uuid: 'c1', ghosts: [ghost] };
      (component as any).mousedownpoint = { x: 0, y: 0 };
      (component as any).partTopPosition = 150;
      (component as any).topLimit = 10;
      (component as any).bottomLimit = 500;
      (component as any).isLowestPart = false;
      (component as any).glasspane = { style: {} };
      component.onMouseUp({ pageX: 10, pageY: 50 } as MouseEvent);
      expect(editorSession.sendChanges).toHaveBeenCalledWith({ 'part-1': { y: 150 } });
      expect(editorSession.dragging()).toBe(false);
    });
  });

  describe('updateLocationCallback', () => {
    beforeEach(() => {
      (component as any).ghostContainers = signal([{ style: {} }]);
      (component as any).editorContent = document.createElement('div');
      (component as any).contentArea = { scrollTop: 0 };
      (component as any).glasspane = { style: {}, offsetHeight: 500 };
      (component as any).draggingGhostComponent = document.createElement('div');
    });

    it('should bail out when the part is at or above the top limit', () => {
      (component as any).partTopPosition = 5;
      (component as any).topLimit = 10;
      component.updateLocationCallback(0, 10);
      expect((component as any).contentArea.scrollTop).toBe(0);
    });

    it('should advance the lowest part and scroll the content area', () => {
      (component as any).partTopPosition = 400;
      (component as any).topLimit = 10;
      (component as any).bottomLimit = 1000;
      (component as any).isLowestPart = true;
      component.updateLocationCallback(0, 10);
      expect((component as any).partTopPosition).toBe(410);
      expect((component as any).contentArea.scrollTop).toBe(10);
    });
  });

  describe('onKeyup', () => {
    it('should cancel the drag copy when Control is released', () => {
      const ghost: any = { uuid: 'g1', location: { x: 10, y: 10 }, style: {} };
      (component as any).dragCopy = true;
      (component as any).draggingInGhostContainer = { ghosts: [ghost] };
      (component as any).originalGhostPositions = new Map([['g1', { x: 5, y: 5 }]]);
      (component as any).onKeyup({ key: 'Control' } as KeyboardEvent);
      expect((component as any).dragCopy).toBe(false);
      expect(ghost.location.x).toBe(5);
    });
  });

  describe('onMouseMove - component drag', () => {
    it('should move the dragged component element with the cursor', () => {
      const compEl = document.createElement('div');
      (component as any).draggingGhost = { uuid: 'g1', type: GHOST_TYPES.GHOST_TYPE_COMPONENT, size: { width: 50, height: 30 }, location: { x: 0, y: 0 } };
      (component as any).draggingGhostComponent = compEl;
      (component as any).draggingGhostComponents = null;
      (component as any).mousedownpoint = { x: 0, y: 0 };
      (component as any).containerLeftOffset = 0;
      (component as any).containerTopOffset = 0;
      (component as any).leftOffsetRelativeToSelectedGhost = 0;
      (component as any).topOffsetRelativeToSelectedGhost = 0;
      (component as any).frameRect = null;
      editorContentService.getGlassPane.mockReturnValue({ style: {} });
      editorContentService.querySelector.mockReturnValue(compEl);
      component.onMouseMove({ pageX: 100, pageY: 120 } as MouseEvent);
      expect((component as any).renderer.setStyle).toHaveBeenCalledWith(compEl, 'left', '100px');
      expect((component as any).renderer.setStyle).toHaveBeenCalledWith(compEl, 'top', '120px');
    });

    it('should move a part ghost downward within its limits', () => {
      const partEl = document.createElement('div');
      (component as any).draggingGhost = { uuid: 'p1', type: GHOST_TYPES.GHOST_TYPE_PART, location: { x: 0, y: 100 } };
      (component as any).draggingGhostComponent = partEl;
      (component as any).mousedownpoint = { x: 0, y: 0 };
      (component as any).lastMouseY = 100;
      (component as any).partTopPosition = 100;
      (component as any).topLimit = 10;
      (component as any).bottomLimit = 500;
      (component as any).isLowestPart = false;
      editorContentService.getGlassPane.mockReturnValue({ style: {} });
      component.onMouseMove({ pageX: 10, pageY: 120 } as MouseEvent);
      expect((component as any).partTopPosition).toBe(120);
      expect((component as any).renderer.setStyle).toHaveBeenCalledWith(partEl, 'top', '120px');
    });
  });

  describe('ngOnDestroy', () => {
    it('should remove listeners and clean up drag-copy state', () => {
      (component as any).removeSelectionChangedListener = vi.fn();
      (component as any).dragCopy = true;
      component.ngOnDestroy();
      expect((component as any).removeSelectionChangedListener).toHaveBeenCalled();
      expect(editorContentService.removeContentMessageListener).toHaveBeenCalledWith(component);
      expect((component as any).dragCopy).toBe(false);
    });
  });
});
