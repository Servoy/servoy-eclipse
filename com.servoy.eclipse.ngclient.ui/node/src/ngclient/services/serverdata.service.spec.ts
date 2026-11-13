import { describe, it, expect, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { ServerDataService } from './serverdata.service';
import { SabloService } from '../../sablo/sablo.service';
import { WindowRefService } from '@servoy/public';
import { WebsocketService } from '../../sablo/websocket.service';
import { ServoyService } from '../servoy.service';
import { I18NProvider } from './i18n_provider.service';

describe('ServerDataService', () => {
  let websocketService: any;
  let sabloService: any;
  let i18nProvider: any;
  let solutionSettings: any;

  const configure = (svyData: any, locale = { language: 'en' }) => {
    websocketService = { setQueryString: vi.fn(), setPathname: vi.fn() };
    sabloService = { getLocale: () => locale };
    i18nProvider = { addDefaultTranslations: vi.fn() };
    solutionSettings = { ltrOrientation: true };
    TestBed.configureTestingModule({
      providers: [
        ServerDataService,
        { provide: WindowRefService, useValue: { nativeWindow: { svyData } } },
        { provide: WebsocketService, useValue: websocketService },
        { provide: SabloService, useValue: sabloService },
        { provide: ServoyService, useValue: { getSolutionSettings: () => solutionSettings } },
        { provide: I18NProvider, useValue: i18nProvider },
      ],
    });
    return TestBed.inject(ServerDataService);
  };

  it('should apply query string and pathname and expose the remote addresses', () => {
    const service = configure({ querystring: 'a=1', pathName: '/app', ipaddr: '1.2.3.4', hostaddr: 'host', orientation: 1 });
    expect(websocketService.setQueryString).toHaveBeenCalledWith('a=1');
    expect(websocketService.setPathname).toHaveBeenCalledWith('/app');
    expect(service.getIPAdress()).toBe('1.2.3.4');
    expect(service.getHostAdress()).toBe('host');
  });

  it('orientation 2 should set ltrOrientation false', () => {
    configure({ orientation: 2 });
    expect(solutionSettings.ltrOrientation).toBe(false);
  });

  it('orientation 3 with an RTL language should set ltrOrientation false', () => {
    configure({ orientation: 3 }, { language: 'ar' });
    expect(solutionSettings.ltrOrientation).toBe(false);
  });

  it('orientation 3 with an LTR language should set ltrOrientation true', () => {
    configure({ orientation: 3 }, { language: 'en' });
    expect(solutionSettings.ltrOrientation).toBe(true);
  });

  it('a default orientation should set ltrOrientation true', () => {
    configure({ orientation: 1 });
    expect(solutionSettings.ltrOrientation).toBe(true);
  });

  it('should add default translations when present', () => {
    configure({ orientation: 1, defaultTranslations: { k: 'v' } });
    expect(i18nProvider.addDefaultTranslations).toHaveBeenCalledWith({ k: 'v' });
  });

  it('init should be a no-op', () => {
    const service = configure({ orientation: 1 });
    expect(() => service.init()).not.toThrow();
  });
});
