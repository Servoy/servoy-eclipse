import { vi, describe, beforeEach, it, expect } from 'vitest';
import { signal } from '@angular/core';
import { ResizeEditorHeightComponent } from './resizeeditorheight.component';
import { HeightResizer } from '../resize/heightresizer';

describe('HeightResizer', () => {
  let resizer: HeightResizer;
  let editorSession: any;
  let editorContentService: any;
  let renderer: any;
  let ghostContainer: any;
  let editorContent: any;
  let contentArea: any;
  let glasspane: any;
  let formGhost: any;

  const buildGhostsList = (ghosts: any[]) => ({
    length: ghosts.length,
    item: (i: number) => ghosts[i] ?? null
  });

  beforeEach(() => {
    editorSession = { sendChanges: vi.fn() };
    renderer = { setStyle: vi.fn() };

    ghostContainer = { offsetHeight: 400, offsetTop: 0, style: {} };
    editorContent = { style: {} };
    contentArea = { scrollTop: 0, getElementsByClassName: vi.fn().mockReturnValue(buildGhostsList([])) };
    glasspane = { style: {}, offsetHeight: 600 };
    formGhost = { getAttribute: vi.fn().mockReturnValue('form-id') };

    editorContentService = {
      getDocument: vi.fn(),
      getContentArea: vi.fn().mockReturnValue(contentArea),
      querySelector: vi.fn().mockImplementation((selector: string) => (selector === '.content' ? editorContent : formGhost)),
      querySelectorAll: vi.fn().mockReturnValue([ghostContainer]),
      getGlassPane: vi.fn().mockReturnValue(glasspane)
    };

    resizer = new HeightResizer(renderer, editorSession, editorContentService);
  });

  describe('move', () => {
    it('should apply the full 1:1 step to the height (no damping)', () => {
      resizer.start({ pageY: 100 } as MouseEvent);
      resizer.move({ pageY: 150 } as MouseEvent);
      expect(renderer.setStyle).toHaveBeenCalledWith(ghostContainer, 'height', '450px');
      expect(renderer.setStyle).toHaveBeenCalledWith(editorContent, 'height', '450px');
    });

    it('should grow the glasspane when the form exceeds it while growing', () => {
      resizer.start({ pageY: 0 } as MouseEvent);
      resizer.move({ pageY: 300 } as MouseEvent);
      expect(glasspane.style.height).toBe('700px');
    });

    it('should do nothing when not active', () => {
      resizer.move({ pageY: 999 } as MouseEvent);
      expect(renderer.setStyle).not.toHaveBeenCalled();
    });
  });

  describe('end', () => {
    it('should send the new height change for the form id', () => {
      resizer.start({ pageY: 100 } as MouseEvent);
      resizer.move({ pageY: 150 } as MouseEvent);
      resizer.end();
      expect(editorSession.sendChanges).toHaveBeenCalledWith({ 'form-id': { height: 450 } });
    });

    it('should also send the lowest part y when there is a part ghost', () => {
      const part = {
        getAttribute: vi.fn((name: string) => (name === 'svy-ghosttype' ? 'part' : 'part-id')),
        offsetHeight: 10,
        offsetTop: 0,
        style: {}
      };
      contentArea.getElementsByClassName.mockReturnValue(buildGhostsList([part]));
      resizer.start({ pageY: 100 } as MouseEvent);
      resizer.move({ pageY: 150 } as MouseEvent);
      resizer.end();
      expect(editorSession.sendChanges).toHaveBeenCalledWith({ 'part-id': { y: 450 } });
      expect(editorSession.sendChanges).toHaveBeenCalledWith({ 'form-id': { height: 450 } });
    });

    it('should do nothing when not active', () => {
      resizer.end();
      expect(editorSession.sendChanges).not.toHaveBeenCalled();
    });
  });

  describe('updateLocation', () => {
    it('should advance currentPosition and scroll the content area', () => {
      resizer.start({ pageY: 0 } as MouseEvent);
      resizer.updateLocation(10);
      expect(contentArea.scrollTop).toBe(10);
    });

    it('should not scroll when below the height limit', () => {
      ghostContainer.offsetHeight = 3;
      resizer.start({ pageY: 0 } as MouseEvent);
      resizer.updateLocation(10);
      expect(contentArea.scrollTop).toBe(0);
    });
  });
});

describe('ResizeEditorHeightComponent', () => {
  let component: ResizeEditorHeightComponent;
  let editorSession: any;

  beforeEach(() => {
    editorSession = {
      dragging: signal(false),
      sendChanges: vi.fn(),
      registerAutoscroll: vi.fn(),
      unregisterAutoscroll: vi.fn()
    };

    component = Object.create(ResizeEditorHeightComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).editorContentService = {
      getDocument: vi.fn().mockReturnValue({ addEventListener: vi.fn(), removeEventListener: vi.fn() })
    };
    (component as any).dragingEvent = null;
    (component as any).resizer = {
      start: vi.fn(),
      move: vi.fn(),
      end: vi.fn(),
      updateLocation: vi.fn()
    };
  });

  describe('getAutoscrollLockId', () => {
    it('should return resize-editor-height', () => {
      expect(component.getAutoscrollLockId()).toBe('resize-editor-height');
    });
  });

  describe('updateLocationCallback', () => {
    it('should forward only the y change to the height resizer', () => {
      component.updateLocationCallback(99, 10);
      expect((component as any).resizer.updateLocation).toHaveBeenCalledWith(10);
    });
  });
});
