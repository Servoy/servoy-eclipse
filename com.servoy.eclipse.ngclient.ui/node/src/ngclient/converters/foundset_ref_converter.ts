import { IType, IPropertyContext } from '../../sablo/types_registry';
import { FoundsetValue } from '../../ngclient/converters/foundset_converter';

export class FoundsetRefType implements IType<any> {

    public static readonly TYPE_NAME = 'foundsetRef';

	fromServerToClient(serverJSONValue: any, _currentClientValue: any, _propertyContext: IPropertyContext): any {
		// no conversions to be done here; server does send the foundsetId and wiki documents it as such
		return serverJSONValue;
	}

	fromClientToServer(newClientData: FoundsetValue, _oldClientData: any, _propertyContext: IPropertyContext): [any, FoundsetValue] {
		return [newClientData.foundsetId, newClientData];
	}

}
