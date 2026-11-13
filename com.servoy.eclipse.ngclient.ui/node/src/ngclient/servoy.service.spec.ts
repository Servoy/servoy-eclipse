import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { ServoyService, SessionProblem } from './servoy.service';
import { WebsocketService } from '../sablo/websocket.service';
import { SabloService } from '../sablo/sablo.service';
import { ConverterService } from '../sablo/converter.service';
import { TypesRegistry } from '../sablo/types_registry';
import { SabloDeferHelper } from '../sablo/defer.service';
import { ViewportService } from './services/viewport.service';
import { LocaleService } from './locale.service';
import { ClientFunctionService } from '../sablo/clientfunction.service';
import { SvyUtilsService } from './utils.service';
import { WindowRefService, LoggerFactory, SessionStorageService, SpecTypesService } from '@servoy/public';

describe('ServoyService', () => {
  let service: ServoyService;
  let sessionStorage: Record<string, string>;
  let nativeWindow: any;
  let localeService: any;
  let clientFunctionService: any;

  beforeEach(() => {
    sessionStorage = {};
    nativeWindow = { addEventListener: vi.fn(), removeEventListener: vi.fn(), location: { search: '' } };
    localeService = { setLocale: vi.fn(), isLoaded: vi.fn().mockResolvedValue(undefined) };
    clientFunctionService = { waitForLoading: vi.fn().mockResolvedValue(undefined) };

    TestBed.configureTestingModule({
      providers: [
        ServoyService,
        LoggerFactory,
        TypesRegistry,
        SpecTypesService,
        SabloDeferHelper,
        ViewportService,
        { provide: WebsocketService, useValue: { getURLParameter: vi.fn(), getPathname: vi.fn() } },
        { provide: SabloService, useValue: { getWindownr: vi.fn(), getLocale: vi.fn().mockReturnValue({ language: 'en' }), callService: vi.fn() } },
        { provide: ConverterService, useValue: { convertFromServerToClient: (v: any) => v } },
        { provide: WindowRefService, useValue: { nativeWindow } },
        { provide: SessionStorageService, useValue: { get: (k: string) => sessionStorage[k], set: (k: string, v: string) => (sessionStorage[k] = v) } },
        { provide: LocaleService, useValue: localeService },
        { provide: ClientFunctionService, useValue: clientFunctionService },
        { provide: SvyUtilsService, useValue: {} },
      ],
    });
    service = TestBed.inject(ServoyService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getSolutionSettings should return the default solution settings', () => {
    const settings = service.getSolutionSettings();
    expect(settings.solutionName).toBe('');
    expect(settings.ltrOrientation).toBe(true);
    expect(settings.enableAnchoring).toBe(true);
  });

  it('getUIBlockerService should expose a UIBlockerService', () => {
    expect(service.getUIBlockerService()).toBeTruthy();
    expect(typeof service.getUIBlockerService().shouldBlockDuplicateEvents).toBe('function');
  });

  describe('getUIProperties', () => {
    it('should read and write UI properties through session storage', () => {
      const props = service.getUIProperties();
      props.setUIProperty('k', 'v');
      expect(props.getUIProperty('k')).toBe('v');
      expect(JSON.parse(sessionStorage['uiProperties'])).toEqual({ k: 'v' });
    });

    it('should return null for an unknown UI property', () => {
      expect(service.getUIProperties().getUIProperty('missing')).toBeNull();
    });

    it('setting a UI property to null should delete it', () => {
      const props = service.getUIProperties();
      props.setUIProperty('k', 'v');
      props.setUIProperty('k', null);
      expect(props.getUIProperty('k')).toBeNull();
    });
  });

  it('loaded should resolve once locale and client functions are loaded', async () => {
    await expect(service.loaded()).resolves.toBeDefined();
    expect(localeService.isLoaded).toHaveBeenCalled();
    expect(clientFunctionService.waitForLoading).toHaveBeenCalled();
  });

  describe('setFindMode', () => {
    it('should register a keyup listener when enabling find mode', () => {
      service.setFindMode('frm', true);
      expect(nativeWindow.addEventListener).toHaveBeenCalledWith('keyup', expect.any(Function));
    });

    it('should remove the keyup listener when disabling find mode', () => {
      service.setFindMode('frm', true);
      service.setFindMode('frm', false);
      expect(nativeWindow.removeEventListener).toHaveBeenCalledWith('keyup', expect.any(Function));
    });

    it('should not register twice when already in find mode', () => {
      service.setFindMode('frm', true);
      nativeWindow.addEventListener.mockClear();
      service.setFindMode('frm', true);
      expect(nativeWindow.addEventListener).not.toHaveBeenCalled();
    });
  });
});

describe('SessionProblem', () => {
  it('should default the view url to an empty string', () => {
    expect(new SessionProblem().viewUrl).toBe('');
  });
});
