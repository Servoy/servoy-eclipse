import { IType, IPropertyContext } from '../../sablo/types_registry';

export class JSMenuType implements IType<any> {

    public static readonly TYPE_NAME = 'JSMenu';
 
    constructor() {}

    fromServerToClient(serverJSONValue: any, currentClientValue: any, propertyContext: IPropertyContext): any {

        return serverJSONValue;
    }

    fromClientToServer(newClientData: any, oldClientData: any, propertyContext: IPropertyContext): [any, any] {
        return newClientData;
    }

}