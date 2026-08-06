import { describe, it, expect, beforeEach, vi } from 'vitest';

import { ResizerComponent } from './resizer.component';

describe('ResizerComponent', () => {
  let component: ResizerComponent;
  let editorSession: any;
  let renderer: any;
  let doc: any;

  beforeEach(() => {
    editorSession = {
      setDragging: vi.fn(),
    };
    renderer = {
      setStyle: vi.fn(),
    };
    doc = {
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      querySelector: vi.fn().mockReturnValue(document.createElement('div')),
    };

    component = Object.create(ResizerComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).renderer = renderer;
    (component as any).doc = doc;
    component.mousemove = (event: MouseEvent) => {
      const palette = doc.querySelector('.palette');
      renderer.setStyle(palette, 'width', event.pageX + 'px');
    };
    component.mouseup = () => {
      doc.removeEventListener('mousemove', component.mousemove);
      doc.removeEventListener('mouseup', component.mouseup);
      editorSession.setDragging(false);
    };
  });

  describe('mousemove', () => {
    it('should set palette width to event.pageX', () => {
      const palette = document.createElement('div');
      doc.querySelector.mockReturnValue(palette);
      component.mousemove({ pageX: 250 } as MouseEvent);
      expect(renderer.setStyle).toHaveBeenCalledWith(palette, 'width', '250px');
    });
  });

  describe('mouseup', () => {
    it('should remove mousemove and mouseup listeners', () => {
      component.mouseup();
      expect(doc.removeEventListener).toHaveBeenCalledWith('mousemove', component.mousemove);
      expect(doc.removeEventListener).toHaveBeenCalledWith('mouseup', component.mouseup);
    });

    it('should set dragging to false', () => {
      component.mouseup();
      expect(editorSession.setDragging).toHaveBeenCalledWith(false);
    });
  });
});
