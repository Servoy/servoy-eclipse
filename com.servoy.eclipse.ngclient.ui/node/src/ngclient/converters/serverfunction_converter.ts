import { IType } from '../../sablo/types_registry';
import { ServoyService } from '../servoy.service';
import { SvyUtilsService } from '../utils.service';
import { EventLike } from '@servoy/public';

export class ServerFunctionType implements IType<(...args: any[]) => unknown> {
  public static readonly TYPE_NAME = 'function';

  public static readonly NATIVE_FUNCTION_TYPE_NAME = 'NativeFunction';

  constructor(
    private servoyService: ServoyService,
    private utils: SvyUtilsService,
  ) {}

  fromServerToClient(serverSentData: { formname: string; script: string; functionhash: string }): (...args: any[]) => unknown {
    if (serverSentData) {
      if (serverSentData.script) {
        const func = (...args: any[]) => {
          let newargs: any[] | undefined;
          if (args) {
            newargs = args.map((element) => {
              if (element instanceof Event) {
                return this.utils.createJSEvent(element as unknown as EventLike, element.type);
              }
              return element;
            });
          }
          return this.servoyService.executeInlineScript(serverSentData.formname, serverSentData.script, newargs!);
        };
        func.formname = serverSentData.formname;
        func.script = serverSentData.script;
        return func;
      } else if (serverSentData.functionhash) {
        const func = (...args: any[]) => {
          let newargs: any[] | undefined;
          if (args) {
            newargs = args.map((element) => {
              if (element instanceof Event) {
                return this.utils.createJSEvent(element as unknown as EventLike, element.type);
              }
              return element;
            });
          }
          return this.servoyService.executeInlineScript(serverSentData.formname, 'hash:' + serverSentData.functionhash, newargs!);
        };
        func.functionhash = serverSentData.functionhash;
        func.formname = serverSentData.formname;
        return func;
      }
    }
    return null!;
  }

  fromClientToServer(_newClientData: { formname: string; script: string; functionhash: string } & (() => any)): [{ formname: string; script: string; functionhash: string }, () => any] | null {
    if (!_newClientData) return null;
    return [{ formname: _newClientData.formname, script: _newClientData.script, functionhash: _newClientData.functionhash }, _newClientData];
  }
}
