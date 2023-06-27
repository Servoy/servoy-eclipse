import { ConverterService } from "../../sablo/converter.service";
import { IPropertyContext, IType } from "../../sablo/types_registry";

export class MapType implements IType<any> {
    
    public static readonly TYPE_NAME = 'map';
    
    constructor(private readonly converterService: ConverterService) {
        
    }

    fromServerToClient(serverJSONValue: any, currentClientValue: any, propertyContext: IPropertyContext) {
        if (serverJSONValue) {
                for(const key in serverJSONValue) {
                    const value = serverJSONValue[key];
                    if (value[ConverterService.CONVERSION_CL_SIDE_TYPE_KEY]) {
                        serverJSONValue[key] = this.converterService.convertFromServerToClient(value, null, currentClientValue != null? currentClientValue[key]: null, null, null, propertyContext);;
                    }
                }
        }
        return serverJSONValue;
    }

    fromClientToServer(newClientData: any, oldClientData: any, propertyContext: IPropertyContext): [any, any] {
        return newClientData;
    }
    
}