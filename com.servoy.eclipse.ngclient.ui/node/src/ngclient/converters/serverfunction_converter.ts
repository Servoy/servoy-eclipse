import { IType } from '../../sablo/types_registry';
import { ServoyService } from '../servoy.service';


export class ServerFunctionType implements IType<(...args) => unknown> {

    public static readonly TYPE_NAME = 'function';
    
    public static readonly NATIVE_FUNCTION_TYPE_NAME = 'NativeFunction';
    
    
    constructor( private servoyService: ServoyService) {
    }

    fromServerToClient(serverSentData: {formname: string, script: string, functionhash:string}): (...args) => unknown {
        if (serverSentData) {
            if (serverSentData.script) {
                const func = (...args) => {
                    return this.servoyService.executeInlineScript(serverSentData.formname, serverSentData.script, args);
                };
                func.formname = serverSentData.formname;
                func.script = serverSentData.script;
                return func;
            } else if (serverSentData.functionhash) {
                const func = (...args) => {
                    return this.servoyService.executeInlineScript(serverSentData.formname, 'hash:' + serverSentData.functionhash, args);
                };
                func.functionhash = serverSentData.functionhash;
                func.formname = serverSentData.formname;
                return func;
            }
        }
        return null;
    }

    fromClientToServer(_newClientData: {formname:string,script:string,functionhash:string} & (() => any) ): 
        [{formname: string, script: string, functionhash:string}, () => any] | null {
        if (!_newClientData) return null;
        return [{formname:_newClientData.formname,script:_newClientData.script,functionhash:_newClientData.functionhash}, _newClientData];
    }
}