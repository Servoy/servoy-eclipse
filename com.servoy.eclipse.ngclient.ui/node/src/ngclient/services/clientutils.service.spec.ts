import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { DOCUMENT } from '@angular/core';

import { ClientUtilsService } from './clientutils.service';
import { WindowRefService } from '@servoy/public';

describe('ClientUtilsService', () => {
  let service: ClientUtilsService;
  let windowRef: any;
  let originalGetElementById: typeof document.getElementById;

  beforeEach(() => {
    windowRef = { nativeWindow: { scrollX: 10, scrollY: 20 } };
    originalGetElementById = document.getElementById;
    TestBed.configureTestingModule({
      providers: [ClientUtilsService, { provide: WindowRefService, useValue: windowRef }, { provide: DOCUMENT, useValue: document }],
    });
    service = TestBed.inject(ClientUtilsService);
  });

  afterEach(() => {
    document.getElementById = originalGetElementById;
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getBounds should return null when the component is not found', () => {
    document.getElementById = vi.fn().mockReturnValue(null);
    expect(service.getBounds('missing', null!)).toBeNull();
  });

  it('getBounds should return the element rect adjusted by the window scroll offsets', () => {
    const el = { getBoundingClientRect: () => ({ left: 100, top: 200, width: 50, height: 60 }) } as any;
    document.getElementById = vi.fn().mockReturnValue(el);
    expect(service.getBounds('comp', null!)).toEqual({ x: 110, y: 220, width: 50, height: 60 });
  });

  it('getBounds should drill into the selector when provided', () => {
    const inner = { getBoundingClientRect: () => ({ left: 1, top: 2, width: 3, height: 4 }) } as any;
    const el = { querySelector: vi.fn().mockReturnValue(inner) } as any;
    document.getElementById = vi.fn().mockReturnValue(el);
    const bounds = service.getBounds('comp', '.child');
    expect(el.querySelector).toHaveBeenCalledWith('.child');
    expect(bounds).toEqual({ x: 11, y: 22, width: 3, height: 4 });
  });

  it('getBounds should return null when the selector matches nothing', () => {
    const el = { querySelector: vi.fn().mockReturnValue(null) } as any;
    document.getElementById = vi.fn().mockReturnValue(el);
    expect(service.getBounds('comp', '.child')).toBeNull();
  });
});
