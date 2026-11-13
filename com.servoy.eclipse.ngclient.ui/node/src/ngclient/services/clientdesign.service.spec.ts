import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { DOCUMENT } from '@angular/core';

import { ClientDesignService } from './clientdesign.service';
import { SabloService } from '../../sablo/sablo.service';
import { SvyUtilsService } from '../utils.service';

describe('ClientDesignService', () => {
  let service: ClientDesignService;
  let sabloService: any;
  let utils: any;
  let formEl: HTMLElement;

  beforeEach(() => {
    sabloService = { callService: vi.fn().mockResolvedValue(true) };
    utils = { createJSEvent: vi.fn().mockReturnValue({ elementName: 'comp1' }) };

    formEl = document.createElement('div');
    formEl.setAttribute('name', 'formA');
    document.body.appendChild(formEl);

    TestBed.configureTestingModule({
      providers: [ClientDesignService, { provide: SabloService, useValue: sabloService }, { provide: SvyUtilsService, useValue: utils }, { provide: DOCUMENT, useValue: document }],
    });
    service = TestBed.inject(ClientDesignService);
  });

  afterEach(() => {
    formEl.remove();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('setFormInDesign should return false when the form element is not in the DOM', () => {
    expect(service.setFormInDesign('missingForm', ['comp1'])).toBe(false);
  });

  it('setFormInDesign should register a drag resize and return true when the element exists', () => {
    expect(service.setFormInDesign('formA', ['comp1'])).toBe(true);
    expect(service.isFormInDesign('formA')).toBe(true);
  });

  it('setFormInDesign should be idempotent, returning true for an already registered form', () => {
    expect(service.setFormInDesign('formA', ['comp1'])).toBe(true);
    expect(service.setFormInDesign('formA', ['comp1'])).toBe(true);
  });

  it('isFormInDesign should return falsy for an unknown form', () => {
    expect(service.isFormInDesign('nope')).toBeFalsy();
  });

  it('removeFormDesign should destroy and remove a registered form', () => {
    service.setFormInDesign('formA', ['comp1']);
    expect(service.removeFormDesign('formA')).toBe(true);
    expect(service.isFormInDesign('formA')).toBeFalsy();
  });

  it('removeFormDesign should return false for an unknown form', () => {
    expect(service.removeFormDesign('nope')).toBe(false);
  });

  it('recreateUI should re-register the form after the scheduled timeout', () => {
    vi.useFakeTimers();
    try {
      service.setFormInDesign('formA', ['comp1']);
      service.recreateUI('formA', ['comp1']);
      // after destroy the form is not registered yet
      expect(service.isFormInDesign('formA')).toBeFalsy();
      vi.advanceTimersByTime(1);
      expect(service.isFormInDesign('formA')).toBe(true);
    } finally {
      vi.useRealTimers();
    }
  });

  it('recreateUI should be a no-op for an unregistered form', () => {
    expect(() => service.recreateUI('nope', [])).not.toThrow();
  });
});
