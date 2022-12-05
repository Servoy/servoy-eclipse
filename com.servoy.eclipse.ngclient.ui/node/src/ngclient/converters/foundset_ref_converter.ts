import { IType, IPropertyContext } from '../../sablo/types_registry';
import { FoundsetValue } from '../../ngclient/converters/foundset_converter';

export class FoundsetRefType implements IType<any> {

    public static readonly TYPE_NAME = 'foundsetRef';

	fromServerToClient(serverJSONValue: any, _currentClientValue: any, _propertyContext: IPropertyContext): any {
		// no conversions to be done here; server does send the foundsetId and wiki documents it as such
		return serverJSONValue;
	}

	fromClientToServer(newClientData: FoundsetValue | number, _oldClientData: any, _propertyContext: IPropertyContext): [any, FoundsetValue | number] {
        if (typeof newClientData === 'number') return [newClientData, newClientData]; // it is already a foundsetId (probably what was received from server is now sent back as an arg or something)
		else return [newClientData.foundsetId, newClientData];
	}

}
