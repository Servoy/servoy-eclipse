import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { MainComponent } from './main.component';
import { WpmService } from './wpm.service';

describe('MainComponent', () => {
  let wpmService: Record<string, any>;

  const createComponent = (): MainComponent => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        MainComponent,
        { provide: WpmService, useValue: wpmService },
      ],
    });
    return TestBed.inject(MainComponent);
  };

  beforeEach(() => {
    wpmService = {
      isDarkTheme: vi.fn(() => false),
      isContentAvailable: vi.fn(() => true),
    };
  });

  afterEach(() => {
    document.body.classList.remove('dark');
  });

  describe('constructor behavior', () => {
    it('should have wpmService injected', () => {
      const component = createComponent();
      expect(component.wpmService).toBe(wpmService);
    });

    it('should build a URL from the current location', () => {
      const component = createComponent();
      expect(component.url).toBeInstanceOf(URL);
      expect(component.url.href).toBe(window.location.href);
    });

    it('should not add the dark class when the theme is light', () => {
      wpmService.isDarkTheme.mockReturnValue(false);
      const component = createComponent();

      expect(component.darkTheme).toBe(false);
      expect(document.body.classList.contains('dark')).toBe(false);
    });

    it('should add the dark class to the body when the theme is dark', () => {
      wpmService.isDarkTheme.mockReturnValue(true);
      const component = createComponent();

      expect(component.darkTheme).toBe(true);
      expect(document.body.classList.contains('dark')).toBe(true);
    });
  });

  describe('isContentAvailable', () => {
    it('should delegate to wpmService', () => {
      const component = createComponent();
      expect(component.isContentAvailable()).toBe(true);
      wpmService.isContentAvailable.mockReturnValue(false);
      expect(component.isContentAvailable()).toBe(false);
    });
  });
});
