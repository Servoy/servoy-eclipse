import { IType } from '../../sablo/types_registry';
import { ServoyService } from '../servoy.service';


export class ServerFunctionType implements IType<(...args) => unknown> {

    public static readonly TYPE_NAME = 'function';
    
    
    constructor( private servoyService: ServoyService) {
    }

    fromServerToClient(serverSentData: {formname: string, script: string}): (...args) => unknown {
        if (serverSentData) {
            const func = (...args) => {
                return this.servoyService.executeInlineScript(serverSentData.formname, serverSentData.script, args);
            };
            func.formname = serverSentData.formname;
            func.script = serverSentData.script;
            return func;
        }
        return null;
    }

    fromClientToServer() {
        return null;
    }
}