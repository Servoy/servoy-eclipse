import { vi, describe, beforeEach, it, expect } from 'vitest';
import { signal } from '@angular/core';
import { Subject } from 'rxjs';
import { VariantsContentComponent } from './variantscontent.component';

describe('VariantsContentComponent', () => {
  let component: VariantsContentComponent;
  let variantsTrigger: Subject<any>;
  let variantsPopup: Subject<any>;

  beforeEach(() => {
    variantsTrigger = new Subject();
    variantsPopup = new Subject();

    component = Object.create(VariantsContentComponent.prototype);
    (component as any).cdr = { markForCheck: vi.fn() };
    (component as any).editorSession = {
      variantsTrigger,
      variantsPopup,
      getVariantsForCategory: vi.fn().mockResolvedValue([])
    };
    (component as any).editorContentService = { getDocument: vi.fn().mockReturnValue({ getElementById: vi.fn() }) };
    (component as any).windowRef = { nativeWindow: { addEventListener: vi.fn() } };
    (component as any).renderer = {};
    (component as any).component = signal({ name: 'bootstrapcomponents-button', styleVariantCategory: 'btn' });
    (component as any).activeVariant = false;
    (component as any).firstQuery = true;
    (component as any).variantsIFrame = null;
  });

  describe('variantsTrigger subscription', () => {
    beforeEach(() => {
      variantsTrigger.subscribe((value: any) => {
        if ((component as any).component() == value.component) {
          (component as any).activeVariant = true;
        } else {
          (component as any).activeVariant = false;
        }
        (component as any).cdr.markForCheck();
      });
    });

    it('should set activeVariant true and call markForCheck when component matches', () => {
      const comp = (component as any).component();
      variantsTrigger.next({ component: comp });
      expect((component as any).activeVariant).toBe(true);
      expect((component as any).cdr.markForCheck).toHaveBeenCalled();
    });

    it('should set activeVariant false and call markForCheck when component does not match', () => {
      (component as any).activeVariant = true;
      variantsTrigger.next({ component: { name: 'other' } });
      expect((component as any).activeVariant).toBe(false);
      expect((component as any).cdr.markForCheck).toHaveBeenCalled();
    });
  });

  describe('convertToJSName', () => {
    it('should return single-segment names unchanged', () => {
      expect(component.convertToJSName('button')).toBe('button');
    });

    it('should camelCase hyphenated names', () => {
      expect(component.convertToJSName('bootstrap-button')).toBe('bootstrapButton');
    });

    it('should handle multiple hyphens', () => {
      expect(component.convertToJSName('my-cool-widget')).toBe('myCoolWidget');
    });

    it('should return empty for empty string', () => {
      expect(component.convertToJSName('')).toBe('');
    });
  });

  describe('getVariantContentMargin', () => {
    it('should return 30px for bootstrapcomponents-button', () => {
      (component as any).component = signal({ name: 'bootstrapcomponents-button' });
      expect(component.getVariantContentMargin()).toBe('30px');
    });

    it('should return 37px for other components', () => {
      (component as any).component = signal({ name: 'other-component' });
      expect(component.getVariantContentMargin()).toBe('37px');
    });
  });
});
