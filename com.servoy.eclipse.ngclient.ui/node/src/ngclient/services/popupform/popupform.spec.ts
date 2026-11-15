import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { DOCUMENT } from '@angular/core';

import { ServoyPublicService } from '@servoy/public';
import { PopupFormService } from '../popupform.service';
import { ServoyFormPopupComponent } from './popupform';

// A minimal PopupForm; individual tests override the fields they exercise.
const makePopup = (over: Record<string, any> = {}): any => ({
  form: 'myForm',
  width: 0,
  height: 0,
  x: null,
  y: null,
  component: null,
  showBackdrop: false,
  ...over,
});

describe('ServoyFormPopupComponent', () => {
  let component: ServoyFormPopupComponent;
  let servoyService: any;
  let popupFormService: any;

  // Keep the window dimensions deterministic for the center-of-window math.
  let originalInnerWidth: number;
  let originalInnerHeight: number;

  beforeEach(() => {
    originalInnerWidth = window.innerWidth;
    originalInnerHeight = window.innerHeight;
    Object.defineProperty(window, 'innerWidth', { value: 1000, configurable: true });
    Object.defineProperty(window, 'innerHeight', { value: 800, configurable: true });

    servoyService = { getFormCacheByName: vi.fn().mockReturnValue({ size: { width: 200, height: 100 } }) };
    popupFormService = { sequencePopup: false, x: 0, y: 0 };

    TestBed.configureTestingModule({
      imports: [ServoyFormPopupComponent],
      providers: [
        { provide: ServoyPublicService, useValue: servoyService },
        { provide: PopupFormService, useValue: popupFormService },
        { provide: DOCUMENT, useValue: document },
      ],
    });
    // Instantiate through TestBed so inject() has a context, but drive the logic directly
    // (the template renders <svy-form>, which we don't need for these unit assertions).
    const fixture = TestBed.createComponent(ServoyFormPopupComponent);
    component = fixture.componentInstance;
    // The component resolves PopupFormService through its own injector, which may be a
    // different instance than the provider value above; read the one it actually holds so
    // the sequencePopup/x/y overrides land on the object initSize() reads.
    popupFormService = (component as any).popupFormService;
    popupFormService.sequencePopup = false;
  });

  afterEach(() => {
    Object.defineProperty(window, 'innerWidth', { value: originalInnerWidth, configurable: true });
    Object.defineProperty(window, 'innerHeight', { value: originalInnerHeight, configurable: true });
    document.body.querySelectorAll('[id]').forEach((e) => {
      if (['tabStop', 'anchorComp'].includes(e.id)) e.remove();
    });
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('setPopupForm / initSize sizing', () => {
    it('uses the explicit popup width/height when provided', () => {
      component.setPopupForm(makePopup({ width: 300, height: 150 }));
      expect(component._width).toBe(300);
      expect(component._height).toBe(150);
    });

    it('falls back to the form cache size when width/height are not set', () => {
      component.setPopupForm(makePopup({ width: 0, height: 0 }));
      expect(component._width).toBe(200);
      expect(component._height).toBe(100);
      expect(servoyService.getFormCacheByName).toHaveBeenCalledWith('myForm');
    });
  });

  describe('initSize positioning', () => {
    it('centers the popup in the window when no x/y and no component', () => {
      component.setPopupForm(makePopup({ width: 200, height: 100 }));
      // left = innerWidth/2 - width/2 = 500 - 100 = 400; top = 400 - 50 = 350
      expect(component._left).toBe(400);
      expect(component._top).toBe(350);
    });

    it('uses the explicit x/y when provided', () => {
      component.setPopupForm(makePopup({ x: 120, y: 60, width: 200, height: 100 }));
      expect(component._left).toBe(120);
      expect(component._top).toBe(60);
    });

    it('anchors to a referenced component element rect', () => {
      const anchor = document.createElement('div');
      anchor.id = 'anchorComp';
      vi.spyOn(anchor, 'getBoundingClientRect').mockReturnValue({ left: 50, top: 70, width: 40, height: 20 } as DOMRect);
      document.body.appendChild(anchor);

      component.setPopupForm(makePopup({ component: 'anchorComp', width: 100, height: 50 }));

      expect(component._left).toBe(50);
      expect(component._top).toBe(70);
    });

    it('flips the popup back when it would overflow the right/bottom edge of the window', () => {
      // x near the right edge so x + width overflows innerWidth (1000); compWidth 0.
      component.setPopupForm(makePopup({ x: 950, y: 780, width: 200, height: 100 }));
      // left: 950 + 200 > 1000 and 950 - 200 + 0 > 0 -> 950 - 200 = 750
      expect(component._left).toBe(750);
      // top: 780 + 100 > 800 and 780 - 100 + 0 > 0 -> 780 - 100 = 680
      expect(component._top).toBe(680);
    });

    it('uses the sequence position from the popup form service when sequencePopup is set', () => {
      popupFormService.sequencePopup = true;
      popupFormService.x = 11;
      popupFormService.y = 22;
      component.setPopupForm(makePopup({ x: 500, y: 500, width: 100, height: 100 }));
      expect(component._left).toBe(11);
      expect(component._top).toBe(22);
    });
  });

  describe('getForm / getStyle', () => {
    it('returns the popup form name', () => {
      component.setPopupForm(makePopup({ form: 'otherForm' }));
      expect(component.getForm()).toBe('otherForm');
    });

    it('builds an absolute positioning style from the computed geometry', () => {
      component.setPopupForm(makePopup({ x: 10, y: 20, width: 30, height: 40 }));
      expect(component.getStyle()).toEqual({
        position: 'absolute',
        zIndex: 1499,
        left: '10px',
        top: '20px',
        width: '30px',
        height: '40px',
      });
    });
  });

  // The focus handlers resolve targets through document.getElementById('tabStop') and
  // document.querySelector("[tabindex=...]"). We drive them by spying on the DOCUMENT
  // getElementById/querySelector the component actually injected, so the tests are not
  // affected by other elements that may exist in the shared jsdom document.
  describe('tab-cycling focus handlers', () => {
    it('firstElementFocused moves focus to the element before the tabStop index', () => {
      const tabStop = document.createElement('div');
      tabStop.setAttribute('tabindex', '5');
      const target = document.createElement('input');
      const focusSpy = vi.spyOn(target, 'focus');

      vi.spyOn(document, 'getElementById').mockImplementation((id) => (id === 'tabStop' ? tabStop : null));
      vi.spyOn(document, 'querySelector').mockImplementation((sel) => (sel === "[tabindex='4']" ? target : null));

      component.firstElementFocused({ target: document.createElement('div') } as any);

      expect(focusSpy).toHaveBeenCalled();
      vi.restoreAllMocks();
    });

    it('firstElementFocused does not re-focus when the resolved target is the event target (no cycle)', () => {
      const tabStop = document.createElement('div');
      tabStop.setAttribute('tabindex', '5');
      const target = document.createElement('input');
      const focusSpy = vi.spyOn(target, 'focus');

      vi.spyOn(document, 'getElementById').mockImplementation((id) => (id === 'tabStop' ? tabStop : null));
      vi.spyOn(document, 'querySelector').mockImplementation((sel) => (sel === "[tabindex='4']" ? target : null));

      component.firstElementFocused({ target } as any);

      expect(focusSpy).not.toHaveBeenCalled();
      vi.restoreAllMocks();
    });

    it('lastElementFocused moves focus to the tabindex=2 element', () => {
      const target = document.createElement('input');
      const focusSpy = vi.spyOn(target, 'focus');

      vi.spyOn(document, 'querySelector').mockImplementation((sel) => (sel === "[tabindex='2']" ? target : null));

      component.lastElementFocused({ target: document.createElement('div') } as any);

      expect(focusSpy).toHaveBeenCalled();
      vi.restoreAllMocks();
    });
  });
});
