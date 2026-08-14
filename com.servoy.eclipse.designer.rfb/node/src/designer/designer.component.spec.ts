import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal } from '@angular/core';

import { DesignerComponent } from './designer.component';

describe('DesignerComponent', () => {
  let component: DesignerComponent;
  let editorSession: any;
  let urlParser: any;
  let renderer: any;

  beforeEach(() => {
    editorSession = {
      connect: vi.fn(),
      registerCallback: signal(null),
    };
    urlParser = {
      isAbsoluteFormLayout: vi.fn().mockReturnValue(true),
    };
    renderer = {
      listen: vi.fn(),
    };

    component = Object.create(DesignerComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).urlParser = urlParser;
    (component as any).renderer = renderer;
    (component as any).contentArea = () => undefined;
  });

  describe('ngOnInit', () => {
    it('should call editorSession.connect()', () => {
      component.ngOnInit();
      expect(editorSession.connect).toHaveBeenCalled();
    });

    it('should register window mouseup listener', () => {
      component.ngOnInit();
      expect(renderer.listen).toHaveBeenCalledWith('window', 'mouseup', expect.any(Function));
    });

    it('should block mouse buttons > 2 on window mouseup', () => {
      component.ngOnInit();
      const mouseUpHandler = renderer.listen.mock.calls.find((c: any[]) => c[0] === 'window' && c[1] === 'mouseup')[2];
      const event = { button: 4, preventDefault: vi.fn(), stopPropagation: vi.fn() } as any;
      mouseUpHandler(event);
      expect(event.preventDefault).toHaveBeenCalled();
      expect(event.stopPropagation).toHaveBeenCalled();
    });

    it('should not block normal mouse buttons', () => {
      component.ngOnInit();
      const mouseUpHandler = renderer.listen.mock.calls.find((c: any[]) => c[0] === 'window' && c[1] === 'mouseup')[2];
      const event = { button: 0, preventDefault: vi.fn(), stopPropagation: vi.fn() } as any;
      mouseUpHandler(event);
      expect(event.preventDefault).not.toHaveBeenCalled();
      expect(event.stopPropagation).not.toHaveBeenCalled();
    });
  });
});
