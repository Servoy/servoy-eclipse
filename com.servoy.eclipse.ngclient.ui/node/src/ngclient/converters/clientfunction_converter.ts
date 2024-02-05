import { IType, IPropertyContext } from '../../sablo/types_registry';
import { WindowRefService } from '@servoy/public';

export class ClientFunctionType implements IType<() => any> {

    public static readonly TYPE_NAME = 'clientfunction';

    constructor( private windowRef: WindowRefService) {
    }

    fromServerToClient(serverSentData: string, _currentClientValue?: () => any, _propertyContext?: IPropertyContext): () => any {
        if (serverSentData) {
            return (...args: any[]) => {
                const func = this.windowRef.nativeWindow['svyClientSideFunctions'][serverSentData] as (...argss: any[]) => any;
                if (func) return func(...args);
            };
        }
        return null;
    }

    fromClientToServer(_newClientData: () => any, _oldClientData?: () => any, _propertyContext?: IPropertyContext): [any, () => any] | null {
       return [null, _newClientData]; // client functions can't be send back to the server.
    }

}
