import { IType, IPropertyContext } from '../../sablo/types_registry';
import { ConverterService } from '../../sablo/converter.service';

export class JSMenuType implements IType<any> {

    public static readonly TYPE_NAME = 'JSMenu';

    constructor(private readonly converterService: ConverterService<unknown>) { }

    fromServerToClient(serverJSONValue: any, currentClientValue: any, propertyContext: IPropertyContext): any {
        if (serverJSONValue.items) {
            for (const i in serverJSONValue.items) {
                if (serverJSONValue.items[i].extraProperties) {
                    for (const category in serverJSONValue.items[i].extraProperties) {
                        if (serverJSONValue.items[i].extraProperties[category]) {
                            for (const property in serverJSONValue.items[i].extraProperties[category]) {
                                if (serverJSONValue.items[i].extraProperties[category][property] instanceof Object && serverJSONValue.items[i].extraProperties[category][property][ConverterService.CONVERSION_CL_SIDE_TYPE_KEY] !== undefined) {
                                    serverJSONValue.items[i].extraProperties[category][property] = this.converterService.convertFromServerToClient(serverJSONValue.items[i].extraProperties[category][property], undefined,
                                        currentClientValue?.items[i]?.extraProperties[category][property], undefined, undefined, propertyContext);
                                }
                            }
                        }
                    }
                }
            }
        }
        return serverJSONValue;
    }

    fromClientToServer(newClientData: any, oldClientData: any, propertyContext: IPropertyContext): [any, any] {
        return newClientData;
    }

}