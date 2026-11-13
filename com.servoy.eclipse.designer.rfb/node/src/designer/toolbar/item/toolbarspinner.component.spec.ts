import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal } from '@angular/core';

import { ToolbarSpinnerComponent } from './toolbarspinner.component';

describe('ToolbarSpinnerComponent', () => {
  let component: ToolbarSpinnerComponent;
  let onclick: ReturnType<typeof vi.fn>;

  const setItem = (overrides: Record<string, unknown>) => {
    const item = { onclick, min: 0, max: 100, ...overrides };
    (component as any).item = () => item;
    return item;
  };

  beforeEach(() => {
    component = Object.create(ToolbarSpinnerComponent.prototype);
    (component as any).value = signal<number | undefined>(undefined);
    onclick = vi.fn();
  });

  describe('ngOnInit', () => {
    it('should set value to initialValue when defined', () => {
      setItem({ initialValue: 5 });
      component.ngOnInit();
      expect(component.value()).toBe(5);
    });

    it('should fall back to min when initialValue is undefined', () => {
      setItem({ initialValue: undefined, min: 3 });
      component.ngOnInit();
      expect(component.value()).toBe(3);
    });

    it('should leave value undefined when neither initialValue nor min defined', () => {
      setItem({ initialValue: undefined, min: undefined });
      component.ngOnInit();
      expect(component.value()).toBeUndefined();
    });
  });

  describe('ngOnChanges', () => {
    it('should reset value to initialValue when defined', () => {
      setItem({ initialValue: 9 });
      component.value.set(2);
      component.ngOnChanges();
      expect(component.value()).toBe(9);
    });

    it('should leave value unchanged when initialValue is undefined', () => {
      setItem({ initialValue: undefined });
      component.value.set(2);
      component.ngOnChanges();
      expect(component.value()).toBe(2);
    });
  });

  describe('dec / inc', () => {
    it('dec should decrease value and call onclick with the new value', () => {
      setItem({});
      component.value.set(5);
      component.dec();
      expect(component.value()).toBe(4);
      expect(onclick).toHaveBeenCalledWith('4');
    });

    it('inc should increase value and call onclick with the new value', () => {
      setItem({});
      component.value.set(5);
      component.inc();
      expect(component.value()).toBe(6);
      expect(onclick).toHaveBeenCalledWith('6');
    });
  });

  describe('checkInput', () => {
    it('should reset to initialValue when value is undefined', () => {
      setItem({ initialValue: 7, min: 0, max: 100 });
      component.value.set(undefined);
      component.checkInput();
      expect(component.value()).toBe(7);
    });

    it('should clamp to min when value is below min', () => {
      setItem({ min: 10, max: 100, initialValue: 10 });
      component.value.set(3);
      component.checkInput();
      expect(component.value()).toBe(10);
    });

    it('should clamp to max when value is above max', () => {
      setItem({ min: 0, max: 50, initialValue: 0 });
      component.value.set(80);
      component.checkInput();
      expect(component.value()).toBe(50);
    });

    it('should leave a value within range unchanged', () => {
      setItem({ min: 0, max: 100, initialValue: 0 });
      component.value.set(42);
      component.checkInput();
      expect(component.value()).toBe(42);
    });
  });

  describe('onSet', () => {
    it('should call onclick with the current value when truthy', () => {
      setItem({});
      component.value.set(12);
      component.onSet();
      expect(onclick).toHaveBeenCalledWith('12');
    });

    it('should not call onclick when value is zero/falsy', () => {
      setItem({});
      component.value.set(0);
      component.onSet();
      expect(onclick).not.toHaveBeenCalled();
    });
  });
});
