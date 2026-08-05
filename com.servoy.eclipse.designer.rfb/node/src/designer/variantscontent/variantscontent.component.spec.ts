import { vi, describe, beforeEach, it, expect } from 'vitest';
import { signal } from '@angular/core';
import { VariantsContentComponent } from './variantscontent.component';

describe('VariantsContentComponent', () => {
  let component: VariantsContentComponent;

  beforeEach(() => {
    component = Object.create(VariantsContentComponent.prototype);
    (component as any).editorSession = {
      variantsTrigger: { subscribe: vi.fn() },
      variantsPopup: { subscribe: vi.fn() },
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
