import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { WebsocketService } from '../sablo/websocket.service';
import { SabloService } from '../sablo/sablo.service';
import { ConverterService } from '../sablo/converter.service';
import { LoggerFactory } from '@servoy/public';
import { WindowRefService } from '@servoy/public';

import { FormService } from './form.service';
import { ServoyService } from './servoy.service';
import { ClientFunctionService } from '../sablo/clientfunction.service';
import { ComponentCache, FormCache } from './types';

describe('FormService', () => {
  let websocketService: any;
  let sabloService: any;
  let converterService: any;
  let servoyService: any;
  let uiBlocker: any;

  beforeEach(() => {
    websocketService = { getSession: vi.fn().mockReturnValue(Promise.resolve({ onMessageObject: vi.fn() })) } as any;
    uiBlocker = { shouldBlockDuplicateEvents: vi.fn().mockReturnValue(false), eventExecuted: vi.fn() };
    sabloService = {
      connect: vi.fn(),
      callService: vi.fn().mockReturnValue(Promise.resolve('serverResult')),
      isExpectingAFormToShowSoon: vi.fn().mockReturnValue(false),
    } as any;
    converterService = {
      convertFromClientToServer: vi.fn().mockImplementation((v: any) => [v, v]),
      convertFromServerToClient: vi.fn().mockImplementation((v: any) => v),
      getEventArgs: vi.fn().mockImplementation((args: any) => args),
    } as any;
    servoyService = { setFindMode: vi.fn(), getUIBlockerService: () => uiBlocker } as any;
    TestBed.configureTestingModule({
      providers: [
        FormService,
        LoggerFactory,
        WindowRefService,
        ClientFunctionService,
        { provide: WebsocketService, useValue: websocketService },
        { provide: SabloService, useValue: sabloService },
        { provide: ConverterService, useValue: converterService },
        { provide: ServoyService, useValue: servoyService },
      ],
    });
  });

  const getService = () => TestBed.inject(FormService);

  it('should be created', () => {
    const service = getService();
    expect(service).toBeTruthy();
    expect(websocketService.getSession).toHaveBeenCalled();
  });

  describe('form cache management', () => {
    it('hasFormCacheEntry should reflect whether a cache was created', () => {
      const service = getService();
      expect(service.hasFormCacheEntry('frm')).toBe(false);
      service.createFormCache('frm', { size: { width: 1, height: 1 }, responsive: false, children: [] }, 'url');
      expect(service.hasFormCacheEntry('frm')).toBe(true);
    });

    it('getFormCacheByName should return the created cache', async () => {
      const service = getService();
      service.createFormCache('frm', { size: { width: 1, height: 1 }, responsive: false, children: [] }, 'url');
      await Promise.resolve();
      const cache = service.getFormCacheByName('frm');
      expect(cache).toBeInstanceOf(FormCache);
      expect(cache.formname).toBe('frm');
    });

    it('destroyFormCache should remove the cache entry', async () => {
      const service = getService();
      service.createFormCache('frm', { size: { width: 1, height: 1 }, responsive: false, children: [] }, 'url');
      await Promise.resolve();
      service.destroyFormCache('frm');
      expect(service.hasFormCacheEntry('frm')).toBe(false);
    });

    it('createFormCache should build the component tree from children', async () => {
      const service = getService();
      service.createFormCache(
        'frm',
        {
          size: { width: 1, height: 1 },
          responsive: false,
          children: [{ name: 'btn', specName: 'pkg-button', elType: null, handlers: [], model: {}, position: {} }],
        },
        'url',
      );
      await Promise.resolve();
      const cache = service.getFormCacheByName('frm');
      expect(cache.getComponent('btn')).toBeInstanceOf(ComponentCache);
    });
  });

  describe('form component instance management', () => {
    const makeFormComponent = (name: string) => ({
      name,
      detectChanges: vi.fn(),
      formCacheChanged: vi.fn(),
      callApi: vi.fn(),
      updateFormStyleClasses: vi.fn(),
      triggerNgOnChangeWithSameRefDueToSmartPropUpdate: vi.fn(),
    });

    it('resolveComponentCache should register the component and call formLoaded', () => {
      const service = getService();
      const fc = makeFormComponent('frm');
      service.resolveComponentCache(fc as any);
      expect(sabloService.callService).toHaveBeenCalledWith('formService', 'formLoaded', { formname: 'frm' }, true);
      expect(service.isFormAngularComponentPresent('frm')).toBe(true);
      expect(service.getFormComponentInstance('frm')).toBe(fc);
    });

    it('getFormComponentInstance should return null for an unknown form', () => {
      const service = getService();
      expect(service.getFormComponentInstance('unknown')).toBeNull();
    });

    it('destroy should unload the form component and call formUnloaded', () => {
      const service = getService();
      const fc = makeFormComponent('frm');
      service.resolveComponentCache(fc as any);
      service.destroy('frm', true);
      expect(sabloService.callService).toHaveBeenCalledWith('formService', 'formUnloaded', { formname: 'frm' }, true);
      expect(service.isFormAngularComponentPresent('frm')).toBe(false);
    });

    it('setFormStyleClasses should update style classes on known form components', () => {
      const service = getService();
      const fc = makeFormComponent('frm');
      service.resolveComponentCache(fc as any);
      service.setFormStyleClasses({ frm: 'cls-a cls-b' });
      expect(fc.updateFormStyleClasses).toHaveBeenCalledWith('cls-a cls-b');
      expect(service.getFormStyleClasses('frm')).toBe('cls-a cls-b');
    });

    it('getFormStyleClasses should return null when nothing was set', () => {
      const service = getService();
      expect(service.getFormStyleClasses('frm')).toBeNull();
    });
  });

  describe('form visibility', () => {
    it('formWillShow should throw when the form name is missing', () => {
      const service = getService();
      expect(() => service.formWillShow(undefined!)).toThrow('formname is undefined');
    });

    it('formWillShow with notify should call the formvisibility service', () => {
      const service = getService();
      service.formWillShow('frm', true, 'parent', 'bean', 'rel', 2);
      expect(sabloService.callService).toHaveBeenCalledWith(
        'formService',
        'formvisibility',
        { formname: 'frm', visible: true, parentForm: 'parent', bean: 'bean', relation: 'rel', formIndex: 2 },
        false,
      );
    });

    it('formWillShow without notify should resolve to true without a server call', async () => {
      const service = getService();
      sabloService.callService.mockClear();
      await expect(service.formWillShow('frm')).resolves.toBe(true);
      expect(sabloService.callService).not.toHaveBeenCalled();
    });

    it('hideForm should throw when the form name is missing', () => {
      const service = getService();
      expect(() => service.hideForm(undefined!)).toThrow('formname is undefined');
    });

    it('hideForm should call formvisibility with visible false', () => {
      const service = getService();
      service.hideForm('frm', 'parent', 'bean', 'rel', 1);
      const call = sabloService.callService.mock.calls.find((c: any[]) => c[1] === 'formvisibility');
      expect(call[2]).toEqual(expect.objectContaining({ formname: 'frm', visible: false, parentForm: 'parent' }));
    });

    it('hideForm should include a formShow block when a next form is given', () => {
      const service = getService();
      service.hideForm('frm', 'parent', 'bean', 'rel', 1, 'nextForm', 'nextRel', 3);
      const call = sabloService.callService.mock.calls.find((c: any[]) => c[1] === 'formvisibility');
      expect(call[2].show).toEqual({ formname: 'nextForm', relation: 'nextRel', formIndex: 3 });
    });

    it('goToForm should delegate to the gotoform service', () => {
      const service = getService();
      service.goToForm('frm');
      expect(sabloService.callService).toHaveBeenCalledWith('formService', 'gotoform', { formname: 'frm' });
    });
  });

  describe('executeEvent', () => {
    const buildCacheWithComponent = (service: FormService) => {
      service.createFormCache(
        'frm',
        {
          size: { width: 1, height: 1 },
          responsive: false,
          children: [{ name: 'btn', specName: 'pkg-button', elType: null, handlers: ['onClick'], model: {}, position: {} }],
        },
        'url',
      );
    };

    it('should call executeEvent on the server when no component is cached', async () => {
      const service = getService();
      await service.executeEvent('frm', 'missing', 'onClick', [1]);
      const call = sabloService.callService.mock.calls.find((c: any[]) => c[1] === 'executeEvent');
      expect(call[2]).toEqual(expect.objectContaining({ formname: 'frm', beanname: 'missing', event: 'onClick' }));
    });

    it('should block duplicate events via the UI blocker', async () => {
      const service = getService();
      buildCacheWithComponent(service);
      await Promise.resolve();
      uiBlocker.shouldBlockDuplicateEvents.mockReturnValue(true);
      sabloService.callService.mockClear();
      const result = await service.executeEvent('frm', 'btn', 'onClick', [1]);
      expect(result).toBeNull();
      const execCall = sabloService.callService.mock.calls.find((c: any[]) => c[1] === 'executeEvent');
      expect(execCall).toBeUndefined();
    });

    it('should execute the event for a cached component when not blocked', async () => {
      const service = getService();
      buildCacheWithComponent(service);
      await Promise.resolve();
      uiBlocker.shouldBlockDuplicateEvents.mockReturnValue(false);
      sabloService.callService.mockClear();
      await service.executeEvent('frm', 'btn', 'onClick', [1]);
      const execCall = sabloService.callService.mock.calls.find((c: any[]) => c[1] === 'executeEvent');
      expect(execCall).toBeDefined();
      expect(uiBlocker.eventExecuted).toHaveBeenCalled();
    });
  });

  describe('sendChanges', () => {
    const buildCache = (service: FormService) => {
      service.createFormCache(
        'frm',
        {
          size: { width: 1, height: 1 },
          responsive: false,
          children: [{ name: 'btn', specName: 'pkg-button', elType: null, handlers: [], model: { text: 'old' }, position: {} }],
        },
        'url',
      );
    };

    it('should push a plain property change to the server (dataPush)', async () => {
      const service = getService();
      buildCache(service);
      await Promise.resolve();
      sabloService.callService.mockClear();
      service.sendChanges('frm', 'btn', 'text', 'new', 'old', false);
      const call = sabloService.callService.mock.calls.find((c: any[]) => c[1] === 'dataPush');
      expect(call).toBeDefined();
      expect(call[2]).toEqual(expect.objectContaining({ formname: 'frm', beanname: 'btn' }));
    });

    it('should not push when the designer mode is active', async () => {
      const service = getService();
      buildCache(service);
      await Promise.resolve();
      service.setDesignerMode();
      sabloService.callService.mockClear();
      service.sendChanges('frm', 'btn', 'text', 'new', 'old', false);
      const call = sabloService.callService.mock.calls.find((c: any[]) => c[1] === 'dataPush');
      expect(call).toBeUndefined();
    });
  });

  describe('callComponentServerSideApi', () => {
    it('should call the callServerSideApi service and convert the result', async () => {
      const service = getService();
      service.createFormCache(
        'frm',
        { size: { width: 1, height: 1 }, responsive: false, children: [{ name: 'btn', specName: 'pkg-button', elType: null, handlers: [], model: {}, position: {} }] },
        'url',
      );
      await Promise.resolve();
      sabloService.callService.mockClear();
      await service.callComponentServerSideApi('frm', 'btn', 'doIt', [1, 2]);
      const call = sabloService.callService.mock.calls.find((c: any[]) => c[1] === 'callServerSideApi');
      expect(call[2]).toEqual(expect.objectContaining({ formname: 'frm', beanname: 'btn', methodName: 'doIt' }));
    });
  });

  describe('static pushEditingStarted', () => {
    it('should send the property name through for a plain property', () => {
      const send = vi.fn();
      FormService.pushEditingStarted({ myDp: 'x' }, 'myDp', send);
      expect(send).toHaveBeenCalledWith(undefined, 'myDp');
    });
  });
});
