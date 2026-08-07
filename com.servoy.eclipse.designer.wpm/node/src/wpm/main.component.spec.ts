import { describe, it, expect, beforeEach, vi } from 'vitest';

import { MainComponent } from './main.component';

describe('MainComponent', () => {
  let component: MainComponent;
  let wpmService: Record<string, any>;

  beforeEach(() => {
    wpmService = {
      isDarkTheme: vi.fn(() => false),
      isContentAvailable: vi.fn(() => true),
    };

    component = Object.create(MainComponent.prototype);
    (component as any).wpmService = wpmService;
  });

  describe('constructor behavior', () => {
    it('should have wpmService injected', () => {
      expect(component.wpmService).toBe(wpmService);
    });
  });

  describe('isContentAvailable', () => {
    it('should delegate to wpmService', () => {
      expect(component.isContentAvailable()).toBe(true);
      wpmService.isContentAvailable.mockReturnValue(false);
      expect(component.isContentAvailable()).toBe(false);
    });
  });
});
