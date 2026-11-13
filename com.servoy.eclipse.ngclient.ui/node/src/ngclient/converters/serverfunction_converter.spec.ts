import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ServerFunctionType } from './serverfunction_converter';
import { ServoyService } from '../servoy.service';
import { SvyUtilsService } from '../utils.service';

describe('ServerFunctionConverter', () => {
  let serverFunctionType: ServerFunctionType;
  let servoyService: ServoyService;
  let utils: SvyUtilsService;
  let executeInlineScriptCalls: any[];
  let createJSEventCalls: any[];

  beforeEach(() => {
    executeInlineScriptCalls = [];
    createJSEventCalls = [];
    servoyService = {
      executeInlineScript: vi.fn((formname: string, script: string, args: any[]) => {
        executeInlineScriptCalls.push({ formname, script, args });
        return 'inlineResult';
      }),
    } as unknown as ServoyService;
    utils = {
      createJSEvent: vi.fn((event: any, type: string) => {
        createJSEventCalls.push({ event, type });
        return { type, isJSEvent: true };
      }),
    } as unknown as SvyUtilsService;
    serverFunctionType = new ServerFunctionType(servoyService, utils);
  });

  describe('fromServerToClient', () => {
    it('should return null when no server data is sent', () => {
      expect(serverFunctionType.fromServerToClient(null as any)).toBeNull();
    });

    it('should return null when neither script nor functionhash is present', () => {
      expect(serverFunctionType.fromServerToClient({ formname: 'f', script: '', functionhash: '' })).toBeNull();
    });

    describe('script based function', () => {
      it('should execute the inline script and expose formname/script', () => {
        const func: any = serverFunctionType.fromServerToClient({ formname: 'myForm', script: 'myScript', functionhash: '' });
        expect(func).toBeDefined();
        expect(func.formname).toBe('myForm');
        expect(func.script).toBe('myScript');

        const result = func('a', 2);
        expect(result).toBe('inlineResult');
        expect(executeInlineScriptCalls).toEqual([{ formname: 'myForm', script: 'myScript', args: ['a', 2] }]);
      });

      it('should convert Event arguments via createJSEvent and leave other args untouched', () => {
        const func: any = serverFunctionType.fromServerToClient({ formname: 'myForm', script: 'myScript', functionhash: '' });
        const event = new Event('click');
        func(event, 'plain');

        expect(createJSEventCalls.length).toBe(1);
        expect(createJSEventCalls[0].type).toBe('click');
        const passedArgs = executeInlineScriptCalls[0].args;
        expect(passedArgs[0]).toEqual({ type: 'click', isJSEvent: true });
        expect(passedArgs[1]).toBe('plain');
      });
    });

    describe('functionhash based function', () => {
      it('should execute via hash and expose functionhash/formname', () => {
        const func: any = serverFunctionType.fromServerToClient({ formname: 'myForm', script: '', functionhash: 'abc123' });
        expect(func).toBeDefined();
        expect(func.functionhash).toBe('abc123');
        expect(func.formname).toBe('myForm');

        const result = func(1);
        expect(result).toBe('inlineResult');
        expect(executeInlineScriptCalls).toEqual([{ formname: 'myForm', script: 'hash:abc123', args: [1] }]);
      });

      it('should convert Event arguments via createJSEvent for the hash variant', () => {
        const func: any = serverFunctionType.fromServerToClient({ formname: 'myForm', script: '', functionhash: 'abc123' });
        const event = new Event('keydown');
        func(event);

        expect(createJSEventCalls.length).toBe(1);
        expect(createJSEventCalls[0].type).toBe('keydown');
        expect(executeInlineScriptCalls[0].args[0]).toEqual({ type: 'keydown', isJSEvent: true });
      });
    });
  });

  describe('fromClientToServer', () => {
    it('should return null when no client data is given', () => {
      expect(serverFunctionType.fromClientToServer(null as any)).toBeNull();
    });

    it('should return the formname/script/functionhash descriptor plus the client function', () => {
      const clientFunc: any = () => 'x';
      clientFunc.formname = 'f';
      clientFunc.script = 's';
      clientFunc.functionhash = 'h';

      const result = serverFunctionType.fromClientToServer(clientFunc);
      expect(result).toEqual([{ formname: 'f', script: 's', functionhash: 'h' }, clientFunc]);
    });
  });
});
