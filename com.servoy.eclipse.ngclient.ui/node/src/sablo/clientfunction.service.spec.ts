import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { DOCUMENT } from '@angular/core';

import { ClientFunctionService } from './clientfunction.service';
import { SabloService } from './sablo.service';

describe('ClientFunctionService', () => {
  let service: ClientFunctionService;
  let sabloService: any;
  let baseElement: HTMLBaseElement;

  beforeEach(() => {
    sabloService = { getClientnr: vi.fn().mockReturnValue('42') };

    baseElement = document.createElement('base');
    baseElement.setAttribute('href', '/myapp');
    document.head.appendChild(baseElement);

    TestBed.configureTestingModule({
      providers: [ClientFunctionService, { provide: SabloService, useFactory: () => sabloService }],
    });
    service = TestBed.inject(ClientFunctionService);
  });

  afterEach(() => {
    baseElement.remove();
    document.querySelectorAll('script[src*="clientfunctions.js"]').forEach((s) => s.remove());
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('waitForLoading returns an immediately-resolving PromiseLike when nothing is loading', () => {
    const run = vi.fn();
    service.waitForLoading().finally(run);
    expect(run).toHaveBeenCalledTimes(1);
  });

  it('reloadClientFunctions should append a script with context, client number and a cache-busting stamp', () => {
    service.reloadClientFunctions();
    const script = document.querySelector('script[src*="clientfunctions.js"]') as HTMLScriptElement;
    expect(script).toBeTruthy();
    expect(script.type).toBe('text/javascript');
    expect(script.src).toContain('/myapp/clientfunctions.js?clientnr=42&stamp=');
    expect(sabloService.getClientnr).toHaveBeenCalled();
  });

  it('reloadClientFunctions should append a trailing slash to a context that does not end with one', () => {
    baseElement.setAttribute('href', '/noSlash');
    service.reloadClientFunctions();
    const script = document.querySelector('script[src*="clientfunctions.js"]') as HTMLScriptElement;
    expect(script.src).toContain('/noSlash/clientfunctions.js');
  });

  it('waitForLoading should return the pending deferred promise once a reload is in progress', () => {
    service.reloadClientFunctions();
    const promiseLike = service.waitForLoading();
    expect(promiseLike).toBeInstanceOf(Promise);
  });

  it('the pending promise resolves when the script fires onload', async () => {
    service.reloadClientFunctions();
    const promise = service.waitForLoading() as Promise<void>;
    const script = document.querySelector('script[src*="clientfunctions.js"]') as HTMLScriptElement;
    script.onload!(new Event('load'));
    await expect(promise).resolves.toBeUndefined();
  });

  it('reloadClientFunctions called again should remove the previous script element', () => {
    service.reloadClientFunctions();
    const first = document.querySelector('script[src*="clientfunctions.js"]') as HTMLScriptElement;
    const removeSpy = vi.spyOn(first, 'remove');
    service.reloadClientFunctions();
    expect(removeSpy).toHaveBeenCalled();
    expect(document.querySelectorAll('script[src*="clientfunctions.js"]').length).toBe(1);
  });

  it('should use the injected DOCUMENT', () => {
    const doc = TestBed.inject(DOCUMENT);
    expect(doc).toBe(document);
  });
});
