import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { DOCUMENT } from '@angular/core';

import { LocaleService } from './locale.service';
import { SabloService } from '../sablo/sablo.service';
import { SessionStorageService, LoggerFactory } from '@servoy/public';
import { I18NProvider } from './services/i18n_provider.service';

describe('LocaleService', () => {
  let service: LocaleService;
  let sabloService: any;
  let i18nProvider: any;
  let sessionStore: Record<string, string>;

  beforeEach(() => {
    sessionStore = {};
    sabloService = {
      getLocale: vi.fn().mockReturnValue({ language: 'en', country: 'US', full: 'en-US' }),
      setLocale: vi.fn(),
    };
    i18nProvider = { flush: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        LocaleService,
        LoggerFactory,
        { provide: SabloService, useValue: sabloService },
        { provide: I18NProvider, useValue: i18nProvider },
        { provide: SessionStorageService, useValue: { set: (k: string, v: string) => (sessionStore[k] = v), get: (k: string) => sessionStore[k] } },
        { provide: DOCUMENT, useValue: document },
      ],
    });
    service = TestBed.inject(LocaleService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getLocale should default to en', () => {
    expect(service.getLocale()).toBe('en');
  });

  it('getLocaleObject should delegate to the sablo service', () => {
    expect(service.getLocaleObject()).toEqual({ language: 'en', country: 'US', full: 'en-US' });
  });

  it('getAgGridLocale should return the loaded ag grid locale', () => {
    (service as any).agGridLocale = { foo: 'bar' };
    expect(service.getAgGridLocale()).toEqual({ foo: 'bar' });
  });

  describe('setLocale', () => {
    it('should resolve the locale, flush i18n and set the locale on the sablo service', async () => {
      // make setAngularLocale resolve quickly
      vi.spyOn(service as any, 'setAngularLocale').mockResolvedValue('en-US');
      vi.spyOn(service as any, 'setNumbroLocale').mockResolvedValue(undefined);

      service.setLocale('en', 'US', true);
      await service.isLoaded();

      expect(i18nProvider.flush).toHaveBeenCalled();
      expect(sabloService.setLocale).toHaveBeenCalledWith({ language: 'en', country: 'US', full: 'en-US' });
      // initializing=true -> should NOT write to session storage
      expect(sessionStore['locale']).toBeUndefined();
    });

    it('should store the locale in session storage when not initializing', async () => {
      vi.spyOn(service as any, 'setAngularLocale').mockResolvedValue('fr-FR');
      vi.spyOn(service as any, 'setNumbroLocale').mockResolvedValue(undefined);

      service.setLocale('fr', 'FR', false);
      await service.isLoaded();

      expect(sessionStore['locale']).toBe('fr-FR');
    });

    it('should reject the loaded promise when the angular locale cannot be loaded', async () => {
      vi.spyOn(service as any, 'setAngularLocale').mockRejectedValue('no locale');

      service.setLocale('zz', 'ZZ', true);
      await expect(service.isLoaded()).rejects.toContain('Could not set Locale');
    });
  });

  describe('makeFullLocale', () => {
    it('should map a known short locale through the localeMap', () => {
      expect((service as any).makeFullLocale('en')).toBe('en-US');
    });

    it('should derive an uppercase country for an unknown short locale', () => {
      expect((service as any).makeFullLocale('de')).toBe('de-DE');
    });
  });
});
