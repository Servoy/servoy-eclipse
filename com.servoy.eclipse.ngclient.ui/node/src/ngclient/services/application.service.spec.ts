import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { DOCUMENT } from '@angular/core';

import { ApplicationService } from './application.service';
import { ServoyService } from '../servoy.service';
import { LoggerFactory, WindowRefService, LocalStorageService, MainViewRefService } from '@servoy/public';
import { SabloService } from '../../sablo/sablo.service';
import { BSWindowManager } from './bootstrap-window/bswindow_manager.service';
import { LocaleService } from '../locale.service';
import { ServerDataService } from './serverdata.service';

describe('ApplicationService', () => {
  let service: ApplicationService;
  let localStorage: Record<string, string>;
  let uiProps: Record<string, any>;
  let servoyService: any;
  let localStorageService: any;
  let localeService: any;
  let windowRef: any;
  let mainViewRef: any;
  let sabloService: any;
  let bsWindowManager: any;
  let serverData: any;

  beforeEach(() => {
    localStorage = {};
    uiProps = {};
    servoyService = {
      getUIProperties: () => ({
        getUIProperty: (k: string) => uiProps[k] ?? null,
        setUIProperty: (k: string, v: any) => (uiProps[k] = v),
      }),
      getSolutionSettings: () => ({ solutionName: 'mySolution' }),
    };
    localStorageService = {
      get: (k: string) => localStorage[k] ?? null,
      set: (k: string, v: string) => (localStorage[k] = v),
      remove: (k: string) => delete localStorage[k],
    };
    localeService = { setLocale: vi.fn(), isLoaded: vi.fn().mockResolvedValue(undefined) };
    windowRef = {
      nativeWindow: {
        location: { origin: 'https://host:8080', host: 'host:8080', protocol: 'https:', hostname: 'host', href: 'https://host:8080/app?x=1' },
        navigator: { userAgent: 'Mozilla', platform: 'Win32', clipboard: { writeText: vi.fn(), readText: vi.fn().mockResolvedValue('clip') } },
        screen: { width: 1920, height: 1080 },
        orientation: 0,
        innerWidth: 1000,
        open: vi.fn(),
      },
    };
    mainViewRef = { mainContainer: { createComponent: vi.fn() } };
    sabloService = { getClientnr: vi.fn().mockReturnValue('42'), getLocale: vi.fn().mockReturnValue({ full: 'en-US', language: 'en', country: 'US' }), callService: vi.fn() };
    bsWindowManager = { createWindow: vi.fn() };
    serverData = { getIPAdress: () => '1.2.3.4', getHostAdress: () => 'remotehost' };

    TestBed.configureTestingModule({
      providers: [
        ApplicationService,
        LoggerFactory,
        { provide: ServoyService, useValue: servoyService },
        { provide: LocalStorageService, useValue: localStorageService },
        { provide: LocaleService, useValue: localeService },
        { provide: WindowRefService, useValue: windowRef },
        { provide: MainViewRefService, useValue: mainViewRef },
        { provide: SabloService, useValue: sabloService },
        { provide: BSWindowManager, useValue: bsWindowManager },
        { provide: ServerDataService, useValue: serverData },
        { provide: DOCUMENT, useValue: document },
      ],
    });
    service = TestBed.inject(ApplicationService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('user properties', () => {
    it('setUserProperty should persist and getUserProperty should read it back', () => {
      service.setUserProperty('foo', 'bar');
      expect(service.getUserProperty('foo')).toBe('bar');
      expect(JSON.parse(localStorage['userProperties'])).toEqual({ foo: 'bar' });
    });

    it('setUserProperty with null should delete the property', () => {
      service.setUserProperty('foo', 'bar');
      service.setUserProperty('foo', null);
      expect(service.getUserProperty('foo')).toBeUndefined();
    });

    it('removeUserProperty should delete the property', () => {
      service.setUserProperty('a', '1');
      service.removeUserProperty('a');
      expect(service.getUserProperty('a')).toBeUndefined();
    });

    it('removeAllUserProperties should write an empty object to local storage', () => {
      service.setUserProperty('a', '1');
      service.removeAllUserProperties();
      expect(JSON.parse(localStorage['userProperties'])).toEqual({});
    });

    it('getUserPropertyNames should list the stored keys', () => {
      service.setUserProperty('a', '1');
      service.setUserProperty('b', '2');
      expect(service.getUserPropertyNames().sort()).toEqual(['a', 'b']);
    });

    it('getUserProperties should load existing JSON from local storage', () => {
      localStorage['userProperties'] = JSON.stringify({ existing: 'v' });
      expect(service.getUserProperty('existing')).toBe('v');
    });
  });

  describe('UI properties', () => {
    it('getUIProperty and setUIProperty should delegate to the servoy service', () => {
      service.setUIProperty('myKey', 'myVal');
      expect(service.getUIProperty('myKey')).toBe('myVal');
    });

    it('setUIProperties should set each property', () => {
      service.setUIProperties({ a: 'x', b: 'y' });
      expect(service.getUIProperty('a')).toBe('x');
      expect(service.getUIProperty('b')).toBe('y');
    });
  });

  describe('url helpers', () => {
    it('generateUploadUrl should build a resources upload url', () => {
      expect(service.generateUploadUrl('frm', 'comp', 'prop')).toBe('resources/upload/42/frm/comp/prop/');
    });

    it('generateUploadUrl with tus should build a tus upload url', () => {
      expect(service.generateUploadUrl(null!, null!, null!, true)).toBe('tus/upload/42/');
    });

    it('generateServiceUploadUrl should use the svy_services form name', () => {
      expect(service.generateServiceUploadUrl('mySvc', 'myApi')).toBe('resources/upload/42/svy_services/mySvc/myApi/');
    });

    it('generateMediaDownloadUrl should strip the media:// prefix and prefix with the solution', () => {
      expect(service.generateMediaDownloadUrl('media:///img.png')).toBe('resources/fs/mySolution/img.png');
    });

    it('generateMediaDownloadUrl should pass a plain path through', () => {
      expect(service.generateMediaDownloadUrl('/plain.png')).toBe('resources/fs/mySolution/plain.png');
    });
  });

  describe('login credentials', () => {
    it('rememberUser should store the username and token', () => {
      service.rememberUser({ username: 'joe', id_token: 'tok' });
      expect(localStorage['servoy_username']).toBe('joe');
      expect(localStorage['servoy_id_token']).toBe('tok');
    });

    it('clearDefaultLoginCredentials should remove stored credentials', () => {
      localStorage['servoy_username'] = 'joe';
      localStorage['servoy_password'] = 'p';
      localStorage['servoy_id_token'] = 't';
      service.clearDefaultLoginCredentials();
      expect(localStorage['servoy_username']).toBeUndefined();
      expect(localStorage['servoy_password']).toBeUndefined();
      expect(localStorage['servoy_id_token']).toBeUndefined();
    });
  });

  describe('browser information', () => {
    it('getScreenSize should return the window screen size', () => {
      expect(service.getScreenSize()).toEqual({ width: 1920, height: 1080, orientation: 0 });
    });

    it('getScreenSize should return null when there is no screen', () => {
      windowRef.nativeWindow.screen = undefined;
      expect(service.getScreenSize()).toBeNull();
    });

    it('getUserAgentAndPlatform should return the navigator values', () => {
      expect(service.getUserAgentAndPlatform()).toEqual({ userAgent: 'Mozilla', platform: 'Win32' });
    });

    it('getClientBrowserInformation should include the remote address and locale', () => {
      const base = document.createElement('base');
      base.setAttribute('href', '/ctx/');
      document.getElementsByTagName('head')[0].appendChild(base);
      const info = service.getClientBrowserInformation();
      expect(info.remote_ipaddress).toBe('1.2.3.4');
      expect(info.remote_host).toBe('remotehost');
      expect(info.locale).toBe('en-US');
      expect(info.serverURL).toContain('host:8080');
      base.remove();
    });
  });

  it('getSolutionName should delegate to the servoy service', () => {
    expect(service.getSolutionName()).toBe('mySolution');
  });

  describe('trustAsHtml', () => {
    it('should return the component clientProperty value', () => {
      expect(service.trustAsHtml({ clientProperty: { trustDataAsHtml: true } })).toBe(true);
    });

    it('should fall back to the UI property', () => {
      uiProps['trustDataAsHtml'] = 'fallback';
      expect(service.trustAsHtml({})).toBe('fallback');
    });
  });

  it('setLocale should delegate to the locale service', () => {
    service.setLocale('fr', 'FR');
    expect(localeService.setLocale).toHaveBeenCalledWith('fr', 'FR');
  });

  it('setStatusText should set the window status', () => {
    service.setStatusText('hello');
    expect(windowRef.nativeWindow.status).toBe('hello');
  });

  it('setClipboardContent should write to the clipboard', () => {
    service.setClipboardContent('copy me');
    expect(windowRef.nativeWindow.navigator.clipboard.writeText).toHaveBeenCalledWith('copy me');
  });

  it('getClipboardContent should read from the clipboard', async () => {
    await expect(service.getClipboardContent()).resolves.toBe('clip');
  });

  describe('setStyleSheets', () => {
    afterEach(() => {
      document.head.querySelectorAll('link[svy-stylesheet]').forEach((l) => l.remove());
    });

    it('should append link elements for each path', () => {
      service.setStyleSheets(['a.css', 'b.css']);
      const links = document.head.querySelectorAll('link[svy-stylesheet]');
      expect(links.length).toBe(2);
    });

    it('should remove previously added stylesheets on a second call', () => {
      service.setStyleSheets(['a.css']);
      service.setStyleSheets(['b.css']);
      const links = document.head.querySelectorAll('link[svy-stylesheet]');
      expect(links.length).toBe(1);
      expect(links[0].getAttribute('href')).toBe('b.css');
    });
  });

  describe('showDefaultLogin', () => {
    it('should attempt an encrypted login when credentials are stored', () => {
      localStorage['servoy_username'] = 'joe';
      localStorage['servoy_password'] = 'secret';
      sabloService.callService.mockResolvedValue(true);
      service.showDefaultLogin();
      expect(sabloService.callService).toHaveBeenCalledWith('applicationServerService', 'login', expect.objectContaining({ username: 'joe', encrypted: true }), false);
    });

    it('should show the default login window when no credentials are stored', () => {
      mainViewRef.mainContainer.createComponent.mockReturnValue({ instance: { setOnLoginCallback: vi.fn() }, destroy: vi.fn() });
      service.showDefaultLogin();
      expect(mainViewRef.mainContainer.createComponent).toHaveBeenCalled();
    });
  });
});
