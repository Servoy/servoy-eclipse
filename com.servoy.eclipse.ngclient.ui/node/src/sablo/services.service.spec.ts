import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { ServicesService, ServiceProvider } from './services.service';
import { ConverterService, instanceOfChangeAwareValue } from './converter.service';
import { TypesRegistry } from './types_registry';
import { WebsocketService } from './websocket.service';
import { SabloService } from './sablo.service';
import { ClientFunctionService } from './clientfunction.service';
import { LoggerFactory, WindowRefService } from '@servoy/public';

describe('ServicesService', () => {
  let service: ServicesService;
  let converterService: any;
  let typesRegistry: any;
  let sabloService: any;
  let clientFunctionService: any;
  let websocketService: any;
  let setServicesHandlerArg: any;

  beforeEach(() => {
    converterService = {
      convertFromServerToClient: vi.fn().mockImplementation((v: any) => v),
      convertFromClientToServer: vi.fn().mockImplementation((v: any) => [v, v]),
    };
    typesRegistry = {
      getServiceSpecification: vi.fn().mockReturnValue(undefined),
    };
    sabloService = {
      sendServiceChangesJSON: vi.fn(),
      callService: vi.fn(),
    };
    clientFunctionService = {
      waitForLoading: vi.fn().mockReturnValue({ finally: (fn: () => void) => fn() }),
    };
    setServicesHandlerArg = undefined;
    websocketService = {
      getSession: vi.fn().mockReturnValue(
        Promise.resolve({
          setServicesHandler: (handler: any) => {
            setServicesHandlerArg = handler;
          },
        }),
      ),
    };

    TestBed.configureTestingModule({
      providers: [
        ServicesService,
        { provide: ConverterService, useFactory: () => converterService },
        { provide: TypesRegistry, useFactory: () => typesRegistry },
        { provide: SabloService, useFactory: () => sabloService },
        { provide: ClientFunctionService, useFactory: () => clientFunctionService },
        { provide: WebsocketService, useFactory: () => websocketService },
        LoggerFactory,
        WindowRefService,
      ],
    });
    service = TestBed.inject(ServicesService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('service provider', () => {
    it('defaults to a void provider that returns null for any service', () => {
      expect(service.getServiceProvider().getService('anything')).toBeNull();
    });

    it('setServiceProvider stores the provider', () => {
      const provider: ServiceProvider = { getService: (n: string) => ({ name: n }) };
      service.setServiceProvider(provider);
      expect(service.getServiceProvider()).toBe(provider);
    });

    it('setServiceProvider(null) resets to the void provider', () => {
      service.setServiceProvider({ getService: () => ({}) });
      service.setServiceProvider(null as any);
      expect(service.getServiceProvider().getService('x')).toBeNull();
    });
  });

  describe('callServiceApi', () => {
    it('invokes a void (no-return) api with converted arguments', () => {
      const instance = { doThing: vi.fn() };
      service.setServiceProvider({ getService: () => instance });
      // no spec => serviceCallSpec undefined => shouldReturnValue path (undefined spec returns value)
      const ret = service.callServiceApi({ name: 'svc', call: 'doThing', args: [1, 2] }, undefined);
      expect(instance.doThing).toHaveBeenCalledWith(1, 2);
      // with an undefined spec it wraps in a promise
      expect(ret).toBeInstanceOf(Promise);
    });

    it('logs an error when the service instance is missing', () => {
      const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
      service.setServiceProvider({ getService: () => null });
      service.callServiceApi({ name: 'svc', call: 'missing', args: [] }, undefined);
      expect(errorSpy).toHaveBeenCalledWith(expect.anything(), expect.stringContaining('was not found'));
      errorSpy.mockRestore();
    });

    it('logs an error when the api function is missing on an existing service', () => {
      const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
      service.setServiceProvider({ getService: () => ({}) });
      service.callServiceApi({ name: 'svc', call: 'nope', args: [] }, undefined);
      expect(errorSpy).toHaveBeenCalledWith(expect.anything(), expect.stringContaining('api function was not found'));
      errorSpy.mockRestore();
    });

    it('calls a void api (shouldReturnValue false) without wrapping in a promise', () => {
      const instance = { doThing: vi.fn() };
      service.setServiceProvider({ getService: () => instance });
      typesRegistry.getServiceSpecification.mockReturnValue({
        getApiFunction: () => ({ shouldReturnValue: false, getArgumentType: () => undefined }),
      });
      const ret = service.callServiceApi({ name: 'svc', call: 'doThing', args: [] }, undefined);
      expect(instance.doThing).toHaveBeenCalled();
      expect(ret).toBeUndefined();
    });
  });

  describe('updateServiceScopes', () => {
    it('applies converted property values onto the service', () => {
      const instance: Record<string, any> = { a: 'old' };
      service.setServiceProvider({ getService: () => instance });
      converterService.convertFromServerToClient.mockImplementation((v: any) => 'converted:' + v);

      service.updateServiceScopes({ svc: { a: 'new' } });
      expect(instance['a']).toBe('converted:new');
    });

    it('ignores updates for services that are not provided', () => {
      service.setServiceProvider({ getService: () => null });
      expect(() => service.updateServiceScopes({ svc: { a: 1 } })).not.toThrow();
    });

    it('catches and logs conversion errors', () => {
      const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
      service.setServiceProvider({ getService: () => ({ a: 1 }) });
      converterService.convertFromServerToClient.mockImplementation(() => {
        throw new Error('convert failed');
      });
      service.updateServiceScopes({ svc: { a: 2 } });
      expect(errorSpy).toHaveBeenCalled();
      errorSpy.mockRestore();
    });
  });

  describe('sendServiceChanges', () => {
    it('reads the current property value and sends the converted change', () => {
      const instance: Record<string, any> = { prop: 'value' };
      service.setServiceProvider({ getService: () => instance });
      converterService.convertFromClientToServer.mockReturnValue(['json', 'clientVal']);

      service.sendServiceChanges('svc', 'prop');
      expect(sabloService.sendServiceChangesJSON).toHaveBeenCalledWith('svc', { prop: 'json' });
      expect(instance['prop']).toBe('clientVal');
    });

    it('sendServiceChangesWithValue uses the provided value and old value', () => {
      const instance: Record<string, any> = {};
      service.setServiceProvider({ getService: () => instance });
      converterService.convertFromClientToServer.mockReturnValue(['jsonNew', 'newClient']);

      service.sendServiceChangesWithValue('svc', 'p', 'newVal', 'oldVal');
      expect(converterService.convertFromClientToServer).toHaveBeenCalled();
      expect(sabloService.sendServiceChangesJSON).toHaveBeenCalledWith('svc', { p: 'jsonNew' });
      expect(instance['p']).toBe('newClient');
    });
  });

  describe('callServiceServerSideApi', () => {
    it('converts arguments, calls the application server service and converts the return value', async () => {
      typesRegistry.getServiceSpecification.mockReturnValue({
        getApiFunction: () => ({ getArgumentType: () => undefined, returnType: undefined }),
      });
      converterService.convertFromClientToServer.mockImplementation((v: any) => ['converted:' + v, v]);
      sabloService.callService.mockReturnValue(Promise.resolve('serverResult'));
      converterService.convertFromServerToClient.mockImplementation((v: any) => 'client:' + v);

      const result = service.callServiceServerSideApi('svc', 'method', ['arg1']);
      await expect(result).resolves.toBe('client:serverResult');
      expect(sabloService.callService).toHaveBeenCalledWith('applicationServerService', 'callServerSideApi', {
        service: 'svc',
        methodName: 'method',
        args: ['converted:arg1'],
      });
    });
  });

  describe('services handler wiring', () => {
    it('registers a services handler on the session', async () => {
      await Promise.resolve();
      await Promise.resolve();
      expect(setServicesHandlerArg).toBeDefined();
    });

    it('handleServiceApisWithApplyFirst only processes pre_data_service_call entries', async () => {
      await Promise.resolve();
      await Promise.resolve();
      const instance = { pre: vi.fn().mockReturnValue(undefined), normal: vi.fn() };
      service.setServiceProvider({ getService: () => instance });
      typesRegistry.getServiceSpecification.mockReturnValue({ getApiFunction: () => ({ shouldReturnValue: false, getArgumentType: () => undefined }) });

      setServicesHandlerArg.handleServiceApisWithApplyFirst(
        [
          { name: 'svc', call: 'pre', args: [], pre_data_service_call: true },
          { name: 'svc', call: 'normal', args: [] },
        ],
        'prev',
      );
      expect(instance.pre).toHaveBeenCalled();
      expect(instance.normal).not.toHaveBeenCalled();
    });

    it('handleNormalServiceApis processes only non-pre calls and resolves the deferred', async () => {
      await Promise.resolve();
      await Promise.resolve();
      const instance = { normal: vi.fn() };
      service.setServiceProvider({ getService: () => instance });
      typesRegistry.getServiceSpecification.mockReturnValue({ getApiFunction: () => ({ shouldReturnValue: false, getArgumentType: () => undefined }) });

      const result = setServicesHandlerArg.handleNormalServiceApis([{ name: 'svc', call: 'normal', args: [] }], undefined);
      expect(instance.normal).toHaveBeenCalled();
      await expect(result).resolves.toBeUndefined();
    });

    it('handlerServiceUpdatesFromServer waits for client functions before updating scopes', async () => {
      await Promise.resolve();
      await Promise.resolve();
      const updateSpy = vi.spyOn(service, 'updateServiceScopes');
      setServicesHandlerArg.handlerServiceUpdatesFromServer({ svc: { a: 1 } });
      expect(clientFunctionService.waitForLoading).toHaveBeenCalled();
      expect(updateSpy).toHaveBeenCalledWith({ svc: { a: 1 } });
    });
  });

  describe('change-aware smart properties', () => {
    it('registers a change listener on a smart property value', () => {
      const internalState = {
        setChangeListener: vi.fn(),
        hasChanges: vi.fn().mockReturnValue(false),
      };
      const smartValue: any = { getInternalState: () => internalState };
      expect(instanceOfChangeAwareValue(smartValue)).toBe(true);

      const instance: Record<string, any> = { prop: smartValue };
      service.setServiceProvider({ getService: () => instance });
      converterService.convertFromClientToServer.mockReturnValue(['json', smartValue]);

      service.sendServiceChanges('svc', 'prop');
      expect(internalState.setChangeListener).toHaveBeenCalled();
      // hasChanges false => the listener is registered but not triggered again, so one push only
      expect(sabloService.sendServiceChangesJSON).toHaveBeenCalledTimes(1);
    });

    it('the registered change listener pushes changes to server when invoked without doNotPushNow', () => {
      let capturedListener: ((doNotPushNow?: boolean) => void) | undefined;
      const internalState = {
        setChangeListener: vi.fn().mockImplementation((fn: any) => (capturedListener = fn)),
        hasChanges: vi.fn().mockReturnValue(false),
      };
      const smartValue: any = { getInternalState: () => internalState };
      const instance: Record<string, any> = { prop: smartValue };
      service.setServiceProvider({ getService: () => instance });
      converterService.convertFromClientToServer.mockReturnValue(['json', smartValue]);

      service.sendServiceChanges('svc', 'prop');
      sabloService.sendServiceChangesJSON.mockClear();

      capturedListener!(true); // doNotPushNow=true => no push
      expect(sabloService.sendServiceChangesJSON).not.toHaveBeenCalled();

      capturedListener!(false); // push
      expect(sabloService.sendServiceChangesJSON).toHaveBeenCalledTimes(1);
    });
  });
});
