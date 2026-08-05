import { vi, describe, beforeEach, it, expect } from 'vitest';
import { ResizeEditorWidthComponent } from './resizeeditorwidth.component';

describe('ResizeEditorWidthComponent', () => {
  let component: ResizeEditorWidthComponent;
  let editorSession: any;

  beforeEach(() => {
    editorSession = {
      getState: vi.fn().mockReturnValue({ dragging: false }),
      sendChanges: vi.fn(),
      registerAutoscroll: vi.fn(),
      unregisterAutoscroll: vi.fn()
    };

    component = Object.create(ResizeEditorWidthComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).editorContentService = {
      getDocument: vi.fn().mockReturnValue({ addEventListener: vi.fn(), removeEventListener: vi.fn() }),
      getContentArea: vi.fn().mockReturnValue({ getElementsByClassName: vi.fn().mockReturnValue({ length: 0, item: vi.fn() }) }),
      querySelector: vi.fn().mockReturnValue(null),
      querySelectorAll: vi.fn().mockReturnValue([{ offsetWidth: 500, offsetLeft: 0, style: {} }]),
      getGlassPane: vi.fn().mockReturnValue({ style: {}, offsetWidth: 600 })
    };
    (component as any).renderer = { setStyle: vi.fn() };
    (component as any).elementRef = { nativeElement: { addEventListener: vi.fn() } };
    (component as any).currentPosition = 400;
    (component as any).widthLimit = 5;
    (component as any).widthOffset = 20;
    (component as any).ghostsRight = 0;
    (component as any).draggingEvent = null;
    (component as any).ghostContainers = [{ offsetWidth: 400, style: {} }];
    (component as any).editorContent = { style: {} };
    (component as any).contentArea = { scrollLeft: 0 };
    (component as any).glasspane = { style: {}, offsetWidth: 600 };
    (component as any).mousePoint = { x: 0, y: 0 };
  });

  describe('getAutoscrollLockId', () => {
    it('should return resize-editor-width', () => {
      expect(component.getAutoscrollLockId()).toBe('resize-editor-width');
    });
  });

  describe('updateLocationCallback', () => {
    it('should update position when above width limit', () => {
      (component as any).currentPosition = 100;
      (component as any).widthLimit = 5;
      (component as any).glasspane = { style: {}, offsetWidth: 80 };
      (component as any).ghostContainers = [{ style: {} }];
      (component as any).editorContent = { style: {} };
      component.updateLocationCallback(10, 0);
      expect((component as any).currentPosition).toBe(110);
      expect((component as any).contentArea.scrollLeft).toBe(10);
    });

    it('should not update when below width limit', () => {
      (component as any).currentPosition = 3;
      (component as any).widthLimit = 5;
      component.updateLocationCallback(10, 0);
      expect((component as any).contentArea.scrollLeft).toBe(0);
    });
  });

  describe('onMouseMove (arrow property)', () => {
    beforeEach(() => {
      (component as any).onMouseMove = function(this: any, event: any) {
        if (this.draggingEvent) {
          event.stopPropagation();
          const step = event.pageX - this.mousePoint.x;
          if (step != 0) {
            this.currentPosition += step;
            if (this.currentPosition >= this.widthLimit) {
              for (const ghostContainer of this.ghostContainers) {
                this.renderer.setStyle(ghostContainer, 'width', this.currentPosition + 'px');
              }
              this.renderer.setStyle(this.editorContent, 'width', this.currentPosition + 'px');
              if (step > 0 && this.currentPosition + this.widthOffset > this.glasspane.offsetWidth) {
                this.glasspane.style.width = this.currentPosition + this.widthOffset + 'px';
              }
            }
            this.mousePoint.x = event.pageX;
          }
        }
      }.bind(component);
      (component as any).onMouseUp = function(this: any, event: any) {
        if (this.draggingEvent) {
          event.stopPropagation();
          this.editorContentService.getDocument().removeEventListener('mousemove', this.onMouseMove);
          this.editorContentService.getDocument().removeEventListener('mouseup', this.onMouseUp);
          if (this.currentPosition < this.widthLimit) {
            this.currentPosition = this.widthLimit;
          }
          const changes: Record<string, any> = {};
          const id = document.querySelector('.ghost[svy-ghosttype="form"]')!.getAttribute('svy-id')!;
          changes[id] = { width: this.currentPosition };
          this.glasspane.style.width = Math.max(this.currentPosition + this.widthOffset, this.ghostsRight) + 'px';
          this.editorSession.sendChanges(changes);
          this.editorSession.getState().dragging = false;
          this.editorSession.unregisterAutoscroll(this);
          this.draggingEvent = null;
        }
      }.bind(component);
    });

    it('should update position on drag', () => {
      (component as any).draggingEvent = {};
      (component as any).mousePoint = { x: 100, y: 0 };
      (component as any).currentPosition = 200;
      (component as any).widthLimit = 5;
      (component as any).ghostContainers = [{ style: {} }];
      (component as any).editorContent = { style: {} };
      (component as any).glasspane = { style: {}, offsetWidth: 300 };
      const event = { pageX: 115, stopPropagation: vi.fn() } as any;
      (component as any).onMouseMove(event);
      expect((component as any).currentPosition).toBe(215);
      expect((component as any).mousePoint.x).toBe(115);
    });

    it('should not do anything when not dragging', () => {
      (component as any).draggingEvent = null;
      const event = { pageX: 110, stopPropagation: vi.fn() } as any;
      (component as any).onMouseMove(event);
      expect(event.stopPropagation).not.toHaveBeenCalled();
    });
  });

  describe('onMouseUp (arrow property)', () => {
    beforeEach(() => {
      (component as any).onMouseUp = function(this: any, event: any) {
        if (this.draggingEvent) {
          event.stopPropagation();
          if (this.currentPosition < this.widthLimit) {
            this.currentPosition = this.widthLimit;
          }
          const changes: Record<string, any> = {};
          const id = document.querySelector('.ghost[svy-ghosttype="form"]')!.getAttribute('svy-id')!;
          changes[id] = { width: this.currentPosition };
          this.glasspane.style.width = Math.max(this.currentPosition + this.widthOffset, this.ghostsRight) + 'px';
          this.editorSession.sendChanges(changes);
          this.editorSession.getState().dragging = false;
          this.editorSession.unregisterAutoscroll(this);
          this.draggingEvent = null;
        }
      }.bind(component);
    });

    it('should send changes and reset state', () => {
      (component as any).draggingEvent = {};
      (component as any).currentPosition = 300;
      (component as any).glasspane = { style: {} };
      (component as any).widthOffset = 20;
      (component as any).ghostsRight = 0;
      const mockFormEl = { getAttribute: vi.fn().mockReturnValue('form-id') };
      vi.spyOn(document, 'querySelector').mockReturnValue(mockFormEl as any);
      const event = { stopPropagation: vi.fn() } as any;
      (component as any).onMouseUp(event);
      expect(editorSession.sendChanges).toHaveBeenCalledWith({ 'form-id': { width: 300 } });
      expect((component as any).draggingEvent).toBeNull();
    });

    it('should clamp position to widthLimit', () => {
      (component as any).draggingEvent = {};
      (component as any).currentPosition = 2;
      (component as any).widthLimit = 5;
      (component as any).glasspane = { style: {} };
      (component as any).widthOffset = 20;
      (component as any).ghostsRight = 0;
      const mockFormEl = { getAttribute: vi.fn().mockReturnValue('form-id') };
      vi.spyOn(document, 'querySelector').mockReturnValue(mockFormEl as any);
      const event = { stopPropagation: vi.fn() } as any;
      (component as any).onMouseUp(event);
      expect((component as any).currentPosition).toBe(5);
    });
  });
});
