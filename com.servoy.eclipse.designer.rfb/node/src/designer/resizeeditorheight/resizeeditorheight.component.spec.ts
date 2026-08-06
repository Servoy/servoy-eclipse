import { vi, describe, beforeEach, it, expect } from 'vitest';
import { signal } from '@angular/core';
import { ResizeEditorHeightComponent } from './resizeeditorheight.component';

describe('ResizeEditorHeightComponent', () => {
  let component: ResizeEditorHeightComponent;
  let editorSession: any;
  let editorContentService: any;

  beforeEach(() => {
    editorSession = {
      dragging: signal(false),
      sendChanges: vi.fn(),
      registerAutoscroll: vi.fn(),
      unregisterAutoscroll: vi.fn()
    };
    editorContentService = {
      getDocument: vi.fn().mockReturnValue({ addEventListener: vi.fn(), removeEventListener: vi.fn() }),
      getContentArea: vi.fn().mockReturnValue({ getElementsByClassName: vi.fn().mockReturnValue({ length: 0, item: vi.fn() }) }),
      querySelector: vi.fn().mockReturnValue({ getAttribute: vi.fn().mockReturnValue('form-id') }),
      querySelectorAll: vi.fn().mockReturnValue([{ offsetHeight: 500, offsetTop: 0, style: {} }]),
      getGlassPane: vi.fn().mockReturnValue({ style: {}, offsetHeight: 600 })
    };

    component = Object.create(ResizeEditorHeightComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).editorContentService = editorContentService;
    (component as any).renderer = { setStyle: vi.fn() };
    (component as any).resizerRef = { nativeElement: { addEventListener: vi.fn() } };
    (component as any).currentPosition = 400;
    (component as any).heightLimit = 5;
    (component as any).heightOffset = 20;
    (component as any).ghostsBottom = 0;
    (component as any).lowestPart = null;
    (component as any).dragingEvent = null;
    (component as any).ghostContainers = [{ offsetHeight: 400, style: {} }];
    (component as any).editorContent = { style: {} };
    (component as any).contentArea = { scrollTop: 0 };
    (component as any).glasspane = { style: {}, offsetHeight: 600 };
    (component as any).mousePoint = { x: 0, y: 0 };
    (component as any).isLowestPart = false;
    (component as any).partTopPosition = 0;
  });

  describe('getAutoscrollLockId', () => {
    it('should return resize-editor-height', () => {
      expect(component.getAutoscrollLockId()).toBe('resize-editor-height');
    });
  });

  describe('updateLocationCallback', () => {
    it('should update position when above height limit', () => {
      (component as any).currentPosition = 100;
      (component as any).heightLimit = 5;
      (component as any).isLowestPart = true;
      (component as any).glasspane = { style: {}, offsetHeight: 80 };
      (component as any).draggingGhostComponent = { style: {} };
      (component as any).formHeight = 100;
      (component as any).ghostContainers = [{ style: {} }];
      (component as any).editorContent = { style: {} };
      component.updateLocationCallback(0, 10);
      expect((component as any).currentPosition).toBe(110);
      expect((component as any).contentArea.scrollTop).toBe(10);
    });

    it('should not update when at height limit', () => {
      (component as any).currentPosition = 3;
      (component as any).heightLimit = 5;
      component.updateLocationCallback(0, 10);
      expect((component as any).contentArea.scrollTop).toBe(0);
    });
  });

  describe('onMouseMove (arrow property)', () => {
    beforeEach(() => {
      (component as any).onMouseMove = ResizeEditorHeightComponent.prototype['onMouseMove'] ||
        function(this: any, event: any) {
          if (this.dragingEvent) {
            event.stopPropagation();
            const step = event.pageY - this.mousePoint.y;
            if (step != 0) {
              this.currentPosition += step;
              if (this.currentPosition >= this.heightLimit) {
                for (const ghostContainer of this.ghostContainers) {
                  this.renderer.setStyle(ghostContainer, 'height', this.currentPosition + 'px');
                }
                this.renderer.setStyle(this.editorContent, 'height', this.currentPosition + 'px');
                if (this.lowestPart) {
                  this.renderer.setStyle(this.lowestPart, 'top', this.currentPosition + 'px');
                }
              }
              this.mousePoint.y = event.pageY;
            }
          }
        };
    });

    it('should update position on drag', () => {
      (component as any).dragingEvent = {};
      (component as any).mousePoint = { x: 0, y: 100 };
      (component as any).currentPosition = 200;
      (component as any).heightLimit = 5;
      (component as any).ghostContainers = [{ style: {} }];
      (component as any).editorContent = { style: {} };
      (component as any).lowestPart = { style: {} };
      (component as any).glasspane = { style: {}, offsetHeight: 300 };
      const event = { pageY: 110, stopPropagation: vi.fn() } as any;
      (component as any).onMouseMove(event);
      expect((component as any).currentPosition).toBe(210);
      expect((component as any).mousePoint.y).toBe(110);
    });

    it('should not do anything when not dragging', () => {
      (component as any).dragingEvent = null;
      const event = { pageY: 110, stopPropagation: vi.fn() } as any;
      (component as any).onMouseMove(event);
      expect(event.stopPropagation).not.toHaveBeenCalled();
    });
  });
});
