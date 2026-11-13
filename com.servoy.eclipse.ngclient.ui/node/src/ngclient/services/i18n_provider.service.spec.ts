import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { I18NProvider } from './i18n_provider.service';
import { SabloService } from '../../sablo/sablo.service';

describe('I18NProvider', () => {
  let sabloService: any;
  let service: I18NProvider;

  beforeEach(() => {
    sabloService = { callService: vi.fn() };
    TestBed.configureTestingModule({
      providers: [I18NProvider, { provide: SabloService, useValue: sabloService }],
    });
    service = TestBed.inject(I18NProvider);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getI18NMessages should resolve from cache without calling the server when all keys are cached', async () => {
    sabloService.callService.mockResolvedValue({ 'key.a': 'A' });
    // first call fetches and caches
    await service.getI18NMessages('key.a');
    sabloService.callService.mockClear();
    // second call for the same key should not hit the server
    const result = await service.getI18NMessages('key.a');
    expect(result).toEqual({ 'key.a': 'A' });
    expect(sabloService.callService).not.toHaveBeenCalled();
  });

  it('getI18NMessages should call the server and cache results for uncached keys', async () => {
    sabloService.callService.mockResolvedValue({ 'key.b': 'B' });
    const result = await service.getI18NMessages('key.b');
    expect(sabloService.callService).toHaveBeenCalledWith('i18nService', 'getI18NMessages', { 0: 'key.b' }, false);
    expect(result).toEqual({ 'key.b': 'B' });
  });

  it('getI18NMessages should reject when the server rejects', async () => {
    sabloService.callService.mockRejectedValue('boom');
    await expect(service.getI18NMessages('key.c')).rejects.toBe('boom');
  });

  it('listenForI18NMessages should deliver cached messages to the callback', async () => {
    sabloService.callService.mockResolvedValue({ 'key.d': 'D' });
    const received = new Promise<Map<string, string>>((resolve) => {
      service.listenForI18NMessages('key.d').messages((messages) => resolve(messages));
    });
    const messages = await received;
    expect(messages.get('key.d')).toBe('D');
  });

  it('addDefaultTranslations should store the translations without throwing', () => {
    expect(() => service.addDefaultTranslations({ a: '1', b: '2' })).not.toThrow();
  });

  it('flush should clear the cached messages so the server is called again', async () => {
    sabloService.callService.mockResolvedValue({ 'key.e': 'E' });
    await service.getI18NMessages('key.e');
    service.flush();
    sabloService.callService.mockClear();
    sabloService.callService.mockResolvedValue({ 'key.e': 'E2' });
    const result = await service.getI18NMessages('key.e');
    expect(sabloService.callService).toHaveBeenCalled();
    expect(result).toEqual({ 'key.e': 'E2' });
  });

  it('a destroyed listener should be removed and not re-fetch on flush', async () => {
    sabloService.callService.mockResolvedValue({ 'key.f': 'F' });
    const listener = service.listenForI18NMessages('key.f');
    await Promise.resolve();
    listener.destroy();
    sabloService.callService.mockClear();
    service.flush();
    expect(sabloService.callService).not.toHaveBeenCalled();
  });
});
