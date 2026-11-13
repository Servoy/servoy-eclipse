import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { DOCUMENT, Renderer2 } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { PlatformLocation } from '@angular/common';

import { WindowService, SvyWindow } from './window.service';
import { FormService } from '../form.service';
import { ServoyService } from '../servoy.service';
import { BSWindowManager } from './bootstrap-window/bswindow_manager.service';
import { ApplicationService } from './application.service';
import { WindowRefService, LocalStorageService, SessionStorageService, MainViewRefService, PopupStateService } from '@servoy/public';
import { SabloService } from '../../sablo/sablo.service';
import { WebsocketService } from '../../sablo/websocket.service';
import { LoadingIndicatorService } from '../../sablo/util/loading-indicator/loading-indicator.service';

describe('WindowService', () => {
  let service: WindowService;
  let sessionStore: Record<string, any>;
  let localStore: Record<string, any>;
  let sessionStorageService: any;
  let localStorageService: any;
  let bsWindowManager: any;
  let platformLocation: any;

  beforeEach(() => {
    sessionStore = {};
    localStore = {};
    sessionStorageService = {
      get: (k: string) => (k in sessionStore ? sessionStore[k] : null),
      set: (k: string, v: any) => (sessionStore[k] = v),
      remove: (k: string) => delete sessionStore[k],
    };
    localStorageService = {
      get: (k: string) => localStore[k] ?? null,
      set: (k: string, v: any) => (localStore[k] = v),
      remove: (k: string) => delete localStore[k],
    };
    bsWindowManager = { createWindow: vi.fn(), setFocused: vi.fn(), sendToBack: vi.fn() };
    platformLocation = { onPopState: vi.fn(), hash: '' };

    TestBed.configureTestingModule({
      providers: [
        WindowService,
        { provide: FormService, useValue: { goToForm: vi.fn(), createFormCache: vi.fn(), getFormCacheByName: vi.fn(), formWillShow: vi.fn() } },
        { provide: ServoyService, useValue: { getSolutionSettings: () => ({ solutionName: 'sol', windowName: 'main' }), loaded: () => Promise.resolve() } },
        { provide: WindowRefService, useValue: { nativeWindow: { innerWidth: 800, innerHeight: 600 } } },
        { provide: MainViewRefService, useValue: { mainContainer: { createComponent: vi.fn() } } },
        { provide: LocalStorageService, useValue: localStorageService },
        { provide: SessionStorageService, useValue: sessionStorageService },
        { provide: Title, useValue: { setTitle: vi.fn() } },
        { provide: SabloService, useValue: { callService: vi.fn() } },
        { provide: BSWindowManager, useValue: bsWindowManager },
        { provide: ApplicationService, useValue: { getUIProperty: vi.fn() } },
        { provide: PlatformLocation, useValue: platformLocation },
        { provide: WebsocketService, useValue: { isConnected: () => false } },
        { provide: LoadingIndicatorService, useValue: { isShowing: () => false, hideLoading: vi.fn(), showLoading: vi.fn() } },
        { provide: PopupStateService, useValue: { isAnyPopupActive: () => false } },
        { provide: DOCUMENT, useValue: document },
      ],
    });
    service = TestBed.inject(WindowService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('create should register an instance and store window metadata', () => {
    service.create('w1', WindowService.WINDOW_TYPE_DIALOG);
    expect(sessionStore['window0']).toEqual({ name: 'w1', type: WindowService.WINDOW_TYPE_DIALOG });
    // creating again should not duplicate
    expect(() => service.create('w1', WindowService.WINDOW_TYPE_DIALOG)).not.toThrow();
  });

  it('destroy should remove the instance', () => {
    service.create('w1', WindowService.WINDOW_TYPE_DIALOG);
    service.destroy('w1');
    // setTitle on a destroyed window goes to the title service instead
    service.setTitle('w1', 'T');
    expect(TestBed.inject(Title).setTitle).toHaveBeenCalledWith('T');
  });

  describe('setters delegating to the instance', () => {
    beforeEach(() => service.create('w1', WindowService.WINDOW_TYPE_DIALOG));

    it('setCloseOnEscape should update the instance flag', () => {
      service.setCloseOnEscape('w1', true);
      expect(() => service.setCloseOnEscape('w1', false)).not.toThrow();
    });

    it('setResizable should not throw', () => {
      expect(() => service.setResizable('w1', true)).not.toThrow();
    });

    it('setStoreBounds should not throw', () => {
      expect(() => service.setStoreBounds('w1', true)).not.toThrow();
    });

    it('resetBounds should clear the stored bounds', () => {
      localStore['solw1.storedBounds.location'] = { x: 1, y: 2 };
      localStore['solw1.storedBounds.size'] = { width: 3, height: 4 };
      service.resetBounds('w1');
      expect(localStore['solw1.storedBounds.location']).toBeUndefined();
      expect(localStore['solw1.storedBounds.size']).toBeUndefined();
    });

    it('toFront should focus the window in the manager', () => {
      service.create('w1', WindowService.WINDOW_TYPE_DIALOG);
      service.toFront('w1');
      expect(bsWindowManager.setFocused).toHaveBeenCalled();
    });

    it('toBack should send the window to the back in the manager', () => {
      service.toBack('w1');
      expect(bsWindowManager.sendToBack).toHaveBeenCalled();
    });

    it('getSize should fall back to the window size when there is no bs window instance', () => {
      expect(service.getSize('w1')).toEqual({ width: 800, height: 600 });
    });

    it('getSize should fall back to the window ref size for an unknown instance', () => {
      expect(service.getSize('unknown')).toEqual({ width: 800, height: 600 });
    });
  });

  describe('setTitle', () => {
    it('should set the document title for a WINDOW type instance', () => {
      service.create('mainWin', WindowService.WINDOW_TYPE_WINDOW);
      service.setTitle('mainWin', 'My title');
      expect(TestBed.inject(Title).setTitle).toHaveBeenCalledWith('My title');
    });
  });

  describe('centerWindow', () => {
    let originalClientWidth: PropertyDescriptor | undefined;
    let originalClientHeight: PropertyDescriptor | undefined;

    beforeEach(() => {
      originalClientWidth = Object.getOwnPropertyDescriptor(document.documentElement, 'clientWidth');
      originalClientHeight = Object.getOwnPropertyDescriptor(document.documentElement, 'clientHeight');
      Object.defineProperty(document.documentElement, 'clientWidth', { configurable: true, value: 1000 });
      Object.defineProperty(document.documentElement, 'clientHeight', { configurable: true, value: 800 });
    });

    afterEach(() => {
      if (originalClientWidth) Object.defineProperty(document.documentElement, 'clientWidth', originalClientWidth);
      if (originalClientHeight) Object.defineProperty(document.documentElement, 'clientHeight', originalClientHeight);
    });

    it('should center the window within the viewport', () => {
      expect(service.centerWindow({ width: 400, height: 200 })).toEqual({ x: 300, y: 300 });
    });

    it('should clamp negative positions to 0', () => {
      expect(service.centerWindow({ width: 2000, height: 2000 })).toEqual({ x: 0, y: 0 });
    });
  });

  it('destroyController should delegate to the form service', () => {
    const formService = TestBed.inject(FormService) as any;
    formService.destroyFormCache = vi.fn();
    service.destroyController('frm');
    expect(formService.destroyFormCache).toHaveBeenCalledWith('frm');
  });

  it('requireFormLoaded should be a no-op', () => {
    expect(() => service.requireFormLoaded()).not.toThrow();
  });

  it('updateController should create the form cache from the parsed structure', () => {
    const formService = TestBed.inject(FormService) as any;
    service.updateController('frmA', JSON.stringify({ frmA: { foo: 'bar' } }), 'url');
    expect(formService.createFormCache).toHaveBeenCalledWith('frmA', { foo: 'bar' }, 'url');
  });
});

describe('SvyWindow', () => {
  let renderer: Renderer2;
  let windowService: any;
  let win: SvyWindow;

  beforeEach(() => {
    renderer = { setStyle: vi.fn(), removeStyle: vi.fn() } as any;
    windowService = {
      localStorageService: { set: vi.fn(), remove: vi.fn() },
      servoyService: { getSolutionSettings: () => ({ solutionName: 'sol' }) },
      sabloService: { callService: vi.fn() },
    };
    win = new SvyWindow('w1', WindowService.WINDOW_TYPE_DIALOG, windowService, renderer);
  });

  it('should default its signals', () => {
    expect(win.title()).toBe('');
    expect(win.opacity()).toBe(1);
    expect(win.undecorated()).toBe(false);
    expect(win.name).toBe('w1');
  });

  it('setTitle should update the title signal', () => {
    win.setTitle('hello');
    expect(win.title()).toBe('hello');
  });

  it('setLocation without a bs window should store the location when storeBounds is on', () => {
    win.storeBounds = true;
    win.setLocation({ x: 5, y: 6 });
    expect(win.location).toEqual({ x: 5, y: 6 });
    expect(windowService.localStorageService.set).toHaveBeenCalledWith('solw1.storedBounds.location', { x: 5, y: 6 });
  });

  it('setLocation with a bs window should apply the styles', () => {
    win.bsWindowInstance = { element: {} } as any;
    win.setLocation({ x: 7, y: 8 });
    expect(renderer.setStyle).toHaveBeenCalledWith(win.bsWindowInstance!.element, 'left', '7px');
    expect(renderer.setStyle).toHaveBeenCalledWith(win.bsWindowInstance!.element, 'top', '8px');
  });

  it('setSize should set the size and forward to the bs window', () => {
    const setSize = vi.fn();
    win.bsWindowInstance = { setSize } as any;
    win.setSize({ width: 100, height: 200 });
    expect(win.getSize()).toEqual({ width: 100, height: 200 });
    expect(setSize).toHaveBeenCalledWith({ width: 100, height: 200 });
  });

  it('onResize should store the size and call the resize service', () => {
    win.onResize({ width: 10, height: 20 });
    expect(win.getSize()).toEqual({ width: 10, height: 20 });
    expect(windowService.sabloService.callService).toHaveBeenCalledWith('$windowService', 'resize', { name: 'w1', size: { width: 10, height: 20 } }, true);
  });

  it('onMove should store the location and call the move service', () => {
    win.onMove({ x: 1, y: 2 });
    expect(win.location).toEqual({ x: 1, y: 2 });
    expect(windowService.sabloService.callService).toHaveBeenCalledWith('$windowService', 'move', { name: 'w1', location: { x: 1, y: 2 } }, true);
  });

  it('clearBounds should remove both stored bound keys', () => {
    win.clearBounds();
    expect(windowService.localStorageService.remove).toHaveBeenCalledWith('solw1.storedBounds.location');
    expect(windowService.localStorageService.remove).toHaveBeenCalledWith('solw1.storedBounds.size');
  });

  it('hide should close the bs window and clear bounds when storeBounds is off', () => {
    const close = vi.fn();
    const destroy = vi.fn();
    win.bsWindowInstance = { close } as any;
    win.componentRef = { destroy } as any;
    win.location = { x: 1, y: 2 };
    win.size = { width: 1, height: 1 };
    win.hide();
    expect(close).toHaveBeenCalled();
    expect(destroy).toHaveBeenCalled();
    expect(win.location).toBeNull();
    expect(win.size).toBeNull();
    expect(win.bsWindowInstance).toBeNull();
  });

  it('hide should keep the bounds when storeBounds is on', () => {
    win.storeBounds = true;
    win.bsWindowInstance = { close: vi.fn() } as any;
    win.location = { x: 1, y: 2 };
    win.size = { width: 3, height: 4 };
    win.hide();
    expect(win.location).toEqual({ x: 1, y: 2 });
    expect(win.size).toEqual({ width: 3, height: 4 });
  });
});
