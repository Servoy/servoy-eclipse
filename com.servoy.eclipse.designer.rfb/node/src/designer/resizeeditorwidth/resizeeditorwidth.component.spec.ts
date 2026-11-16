import { vi, describe, beforeEach, it, expect } from 'vitest';
import { signal } from '@angular/core';
import { ResizeEditorWidthComponent } from './resizeeditorwidth.component';
import { WidthResizer } from '../resize/widthresizer';

describe('WidthResizer', () => {
  let resizer: WidthResizer;
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
    item: (i: number) => ghosts[i] ?? null,
    [Symbol.iterator]: function* () {
      for (const g of ghosts) yield g;
    }
  });

  beforeEach(() => {
    editorSession = { sendChanges: vi.fn() };
    renderer = { setStyle: vi.fn() };

    ghostContainer = { offsetWidth: 400, offsetLeft: 0, style: {} };
    editorContent = { style: {} };
    contentArea = { scrollLeft: 0, getElementsByClassName: vi.fn().mockReturnValue(buildGhostsList([])) };
    glasspane = { style: {}, offsetWidth: 600 };
    formGhost = { getAttribute: vi.fn().mockReturnValue('form-id') };

    editorContentService = {
      getDocument: vi.fn(),
      getContentArea: vi.fn().mockReturnValue(contentArea),
      querySelector: vi.fn().mockImplementation((selector: string) => (selector === '.content' ? editorContent : formGhost)),
      querySelectorAll: vi.fn().mockReturnValue([ghostContainer]),
      getGlassPane: vi.fn().mockReturnValue(glasspane)
    };

    resizer = new WidthResizer(renderer, editorSession, editorContentService);
  });

  describe('start', () => {
    it('should seed currentPosition from the ghost container offsetWidth', () => {
      resizer.start({ pageX: 100 } as MouseEvent);
      resizer.move({ pageX: 110 } as MouseEvent);
      expect(renderer.setStyle).toHaveBeenCalledWith(ghostContainer, 'width', '410px');
    });
  });

  describe('move', () => {
    it('should apply the full 1:1 step to the width (no damping)', () => {
      resizer.start({ pageX: 100 } as MouseEvent);
      resizer.move({ pageX: 150 } as MouseEvent);
      expect(renderer.setStyle).toHaveBeenCalledWith(ghostContainer, 'width', '450px');
      expect(renderer.setStyle).toHaveBeenCalledWith(editorContent, 'width', '450px');
    });

    it('should grow the glasspane when the form exceeds it while growing', () => {
      ghostContainer.offsetLeft = 0;
      glasspane.offsetWidth = 600;
      resizer.start({ pageX: 0 } as MouseEvent);
      resizer.move({ pageX: 300 } as MouseEvent);
      expect(glasspane.style.width).toBe('700px');
    });

    it('should do nothing when not active', () => {
      resizer.move({ pageX: 999 } as MouseEvent);
      expect(renderer.setStyle).not.toHaveBeenCalled();
    });

    it('should not resize below the width limit', () => {
      resizer.start({ pageX: 1000 } as MouseEvent);
      resizer.move({ pageX: 0 } as MouseEvent);
      expect(renderer.setStyle).not.toHaveBeenCalled();
    });
  });

  describe('end', () => {
    it('should send the new width change for the form id', () => {
      resizer.start({ pageX: 100 } as MouseEvent);
      resizer.move({ pageX: 150 } as MouseEvent);
      resizer.end();
      expect(editorSession.sendChanges).toHaveBeenCalledWith({ 'form-id': { width: 450 } });
    });

    it('should clamp the sent width to the width limit', () => {
      resizer.start({ pageX: 1000 } as MouseEvent);
      resizer.move({ pageX: 0 } as MouseEvent);
      resizer.end();
      expect(editorSession.sendChanges).toHaveBeenCalledWith({ 'form-id': { width: 5 } });
    });

    it('should do nothing when not active', () => {
      resizer.end();
      expect(editorSession.sendChanges).not.toHaveBeenCalled();
    });
  });

  describe('updateLocation', () => {
    it('should advance currentPosition and scroll the content area', () => {
      resizer.start({ pageX: 0 } as MouseEvent);
      resizer.updateLocation(10);
      expect(contentArea.scrollLeft).toBe(10);
    });

    it('should not scroll when below the width limit', () => {
      ghostContainer.offsetWidth = 3;
      resizer.start({ pageX: 0 } as MouseEvent);
      resizer.updateLocation(10);
      expect(contentArea.scrollLeft).toBe(0);
    });
  });
});

describe('ResizeEditorWidthComponent', () => {
  let component: ResizeEditorWidthComponent;
  let editorSession: any;

  beforeEach(() => {
    editorSession = {
      dragging: signal(false),
      sendChanges: vi.fn(),
      registerAutoscroll: vi.fn(),
      unregisterAutoscroll: vi.fn()
    };

    component = Object.create(ResizeEditorWidthComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).editorContentService = {
      getDocument: vi.fn().mockReturnValue({ addEventListener: vi.fn(), removeEventListener: vi.fn() })
    };
    (component as any).draggingEvent = null;
    (component as any).resizer = {
      start: vi.fn(),
      move: vi.fn(),
      end: vi.fn(),
      updateLocation: vi.fn()
    };
  });

  describe('getAutoscrollLockId', () => {
    it('should return resize-editor-width', () => {
      expect(component.getAutoscrollLockId()).toBe('resize-editor-width');
    });
  });

  describe('updateLocationCallback', () => {
    it('should forward only the x change to the width resizer', () => {
      component.updateLocationCallback(10, 99);
      expect((component as any).resizer.updateLocation).toHaveBeenCalledWith(10);
    });
  });
});
