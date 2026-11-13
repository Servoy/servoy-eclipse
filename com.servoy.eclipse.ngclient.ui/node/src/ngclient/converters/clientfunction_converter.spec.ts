import { describe, it, expect, beforeEach } from 'vitest';
import { WindowRefService } from '@servoy/public';
import { ClientFunctionType } from './clientfunction_converter';

describe('ClientFunctionConverter', () => {
  let windowRef: WindowRefService;
  let clientFunctionType: ClientFunctionType;
  let nativeWindow: any;

  beforeEach(() => {
    nativeWindow = { svyClientSideFunctions: {} };
    windowRef = { nativeWindow } as WindowRefService;
    clientFunctionType = new ClientFunctionType(windowRef);
  });

  describe('fromServerToClient', () => {
    it('should return null when no server data is sent', () => {
      expect(clientFunctionType.fromServerToClient(null as any)).toBeNull();
      expect(clientFunctionType.fromServerToClient(undefined as any)).toBeNull();
      expect(clientFunctionType.fromServerToClient('')).toBeNull();
    });

    it('should return a function that calls the registered client side function', () => {
      let called: any[] | undefined;
      nativeWindow.svyClientSideFunctions['myHash'] = (...args: any[]) => {
        called = args;
        return 'result';
      };

      const func = clientFunctionType.fromServerToClient('myHash');
      expect(func).toBeDefined();

      const returnValue = (func as any)(1, 'two', true);
      expect(returnValue).toBe('result');
      expect(called).toEqual([1, 'two', true]);
    });

    it('should not throw and return undefined when the referenced function does not exist', () => {
      const func = clientFunctionType.fromServerToClient('missingHash');
      expect(func).toBeDefined();
      expect((func as any)()).toBeUndefined();
    });
  });

  describe('fromClientToServer', () => {
    it('should never send client functions back to server (null value) but keep the client data', () => {
      const clientFunc = () => 'hi';
      const result = clientFunctionType.fromClientToServer(clientFunc);
      expect(result).toEqual([null, clientFunc]);
    });
  });
});
