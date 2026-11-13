import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { signal } from '@angular/core';

import { AutoscrollComponent } from './autoscroll.component';

describe('AutoscrollComponent', () => {
  let component: AutoscrollComponent;
  let editorSession: any;
  let scrollTarget: any;

  beforeEach(() => {
    editorSession = {
      autoscrollTarget: signal(null),
      pointerEvents: signal('none'),
    };

    component = Object.create(AutoscrollComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).editorContent = { getDesignerElementById: vi.fn(), getBodyElement: vi.fn() };
    (component as any).renderer = { setStyle: vi.fn() };
    (component as any).scrollTarget = null;
    (component as any).isAutoscrollActive = false;
    (component as any).stopAutoscrollOffset = 3;
    (component as any).step = 0;
    (component as any).speed = 0;
    (component as any).placement = signal<string | undefined>(undefined);

    scrollTarget = {
      updateLocationCallback: vi.fn(),
      onMouseUp: vi.fn(),
      onMouseMove: vi.fn(),
      getAutoscrollLockId: vi.fn().mockReturnValue('lock-1'),
    };
  });

  afterEach(() => {
    component.stopAutoscroll();
  });

  describe('onMouseEnter', () => {
    it('should set speed and mousePoint when scrollTarget exists', () => {
      (component as any).scrollTarget = scrollTarget;
      component.onMouseEnter({ pageX: 100, pageY: 200 } as MouseEvent);
      expect((component as any).speed).toBe(2);
      expect((component as any).mousePoint).toEqual({ x: 100, y: 200 });
    });

    it('should do nothing when scrollTarget is null', () => {
      (component as any).scrollTarget = null;
      component.onMouseEnter({ pageX: 100, pageY: 200 } as MouseEvent);
      expect((component as any).speed).toBe(0);
    });
  });

  describe('onMouseLeave', () => {
    it('should stop autoscroll when active and scrollTarget exists', () => {
      (component as any).scrollTarget = scrollTarget;
      (component as any).isAutoscrollActive = true;
      (component as any).handler = setInterval(() => undefined, 1000);
      component.onMouseLeave({} as MouseEvent);
      expect((component as any).isAutoscrollActive).toBe(false);
    });

    it('should do nothing when autoscroll is not active', () => {
      (component as any).scrollTarget = scrollTarget;
      (component as any).isAutoscrollActive = false;
      component.onMouseLeave({} as MouseEvent);
      expect((component as any).isAutoscrollActive).toBe(false);
    });
  });

  describe('onMouseUp', () => {
    it('should stop autoscroll and call scrollTarget.onMouseUp', () => {
      (component as any).scrollTarget = scrollTarget;
      (component as any).isAutoscrollActive = true;
      (component as any).handler = setInterval(() => undefined, 1000);
      const event = {} as MouseEvent;
      component.onMouseUp(event);
      expect((component as any).isAutoscrollActive).toBe(false);
      expect(scrollTarget.onMouseUp).toHaveBeenCalledWith(event);
    });
  });

  describe('onMouseDown', () => {
    it('should set speed and mousePoint', () => {
      component.onMouseDown({ pageX: 50, pageY: 75 } as MouseEvent);
      expect((component as any).speed).toBe(2);
      expect((component as any).mousePoint).toEqual({ x: 50, y: 75 });
    });
  });

  describe('autoscroll', () => {
    it('should increment speed and call updateLocationCallback for bottom placement', () => {
      (component as any).scrollTarget = scrollTarget;
      (component as any).placement = signal('bottom');
      (component as any).direction = 1;
      (component as any).speed = 2;
      component.autoscroll();
      expect((component as any).speed).toBe(3);
      expect(scrollTarget.updateLocationCallback).toHaveBeenCalledWith(0, 3);
    });

    it('should cap speed at 15', () => {
      (component as any).scrollTarget = scrollTarget;
      (component as any).placement = signal('top');
      (component as any).direction = -1;
      (component as any).speed = 15;
      component.autoscroll();
      expect((component as any).speed).toBe(15);
      expect(scrollTarget.updateLocationCallback).toHaveBeenCalledWith(0, -15);
    });

    it('should call updateLocationCallback with x for left/right placement', () => {
      (component as any).scrollTarget = scrollTarget;
      (component as any).placement = signal('right');
      (component as any).direction = 1;
      (component as any).speed = 5;
      component.autoscroll();
      expect(scrollTarget.updateLocationCallback).toHaveBeenCalledWith(6, 0);
    });
  });

  describe('startAutoScroll / stopAutoscroll', () => {
    it('startAutoScroll should set isAutoscrollActive to true', () => {
      vi.useFakeTimers();
      component.startAutoScroll();
      expect((component as any).isAutoscrollActive).toBe(true);
      component.stopAutoscroll();
      vi.useRealTimers();
    });

    it('startAutoScroll should not start multiple intervals', () => {
      vi.useFakeTimers();
      component.startAutoScroll();
      const handler = (component as any).handler;
      component.startAutoScroll();
      expect((component as any).handler).toBe(handler);
      component.stopAutoscroll();
      vi.useRealTimers();
    });

    it('stopAutoscroll should set isAutoscrollActive to false', () => {
      vi.useFakeTimers();
      component.startAutoScroll();
      component.stopAutoscroll();
      expect((component as any).isAutoscrollActive).toBe(false);
      vi.useRealTimers();
    });
  });

  describe('onMouseMove', () => {
    it('should start autoscroll when moving forward past threshold', () => {
      vi.useFakeTimers();
      (component as any).scrollTarget = scrollTarget;
      (component as any).placement = signal('bottom');
      (component as any).direction = 1;
      (component as any).mousePoint = { x: 100, y: 100 };
      component.onMouseMove({ pageX: 100, pageY: 110 } as MouseEvent);
      expect((component as any).isAutoscrollActive).toBe(true);
      expect(scrollTarget.onMouseMove).toHaveBeenCalled();
      component.stopAutoscroll();
      vi.useRealTimers();
    });

    it('should stop autoscroll when moving backward past stopAutoscrollOffset', () => {
      vi.useFakeTimers();
      (component as any).scrollTarget = scrollTarget;
      (component as any).placement = signal('bottom');
      (component as any).direction = 1;
      (component as any).mousePoint = { x: 100, y: 100 };
      (component as any).isAutoscrollActive = true;
      (component as any).handler = setInterval(() => undefined, 1000);
      component.onMouseMove({ pageX: 100, pageY: 95 } as MouseEvent);
      expect((component as any).isAutoscrollActive).toBe(false);
      vi.useRealTimers();
    });

    it('should use the horizontal step for left/right placement', () => {
      (component as any).scrollTarget = scrollTarget;
      (component as any).placement = signal('right');
      (component as any).direction = 1;
      (component as any).mousePoint = { x: 100, y: 100 };
      component.onMouseMove({ pageX: 101, pageY: 100 } as MouseEvent);
      // step = 100 - 101 = -1, step*direction = -1 < 0 -> forward -> starts autoscroll
      expect(scrollTarget.onMouseMove).toHaveBeenCalled();
      component.stopAutoscroll();
    });

    it('should do nothing when there is no scroll target', () => {
      (component as any).scrollTarget = null;
      component.onMouseMove({ pageX: 10, pageY: 10 } as MouseEvent);
      expect(scrollTarget.onMouseMove).not.toHaveBeenCalled();
    });
  });

  describe('ngOnInit', () => {
    it('should set direction to -1 for top placement', () => {
      (component as any).placement = signal('top');
      component.ngOnInit();
      expect((component as any).direction).toBe(-1);
    });

    it('should set direction to 1 for bottom placement', () => {
      (component as any).placement = signal('bottom');
      component.ngOnInit();
      expect((component as any).direction).toBe(1);
    });

    it('should set direction to -1 for left placement', () => {
      (component as any).placement = signal('left');
      component.ngOnInit();
      expect((component as any).direction).toBe(-1);
    });
  });

  describe('setPosition', () => {
    it('should position the left autoscroll area beside the palette', () => {
      (component as any).placement = signal('left');
      const autoscrollEl = { nativeElement: document.createElement('div') };
      (component as any).autoscrollElement = () => autoscrollEl;
      (component as any).editorContent.getDesignerElementById = vi.fn().mockReturnValue({ offsetWidth: 200, offsetLeft: 10 });
      (component as any).editorContent.getBodyElement = vi.fn().mockReturnValue({
        getElementsByClassName: (cls: string) => ({
          item: () => (cls === 'top' ? { offsetTop: 0, offsetHeight: 40 } : { offsetTop: 500 })
        })
      });
      component.setPosition();
      expect((component as any).renderer.setStyle).toHaveBeenCalledWith(autoscrollEl.nativeElement, 'left', '210px');
    });

    it('should set the height for a non top/bottom placement', () => {
      (component as any).placement = signal('right');
      const autoscrollEl = { nativeElement: document.createElement('div') };
      (component as any).autoscrollElement = () => autoscrollEl;
      (component as any).editorContent.getBodyElement = vi.fn().mockReturnValue({
        getElementsByClassName: (cls: string) => ({
          item: () => (cls === 'top' ? { offsetTop: 0, offsetHeight: 40 } : { offsetTop: 500 })
        })
      });
      component.setPosition();
      // height = 500 - (0 + 40) - 10 = 450
      expect((component as any).renderer.setStyle).toHaveBeenCalledWith(autoscrollEl.nativeElement, 'height', '450px');
    });
  });
});
