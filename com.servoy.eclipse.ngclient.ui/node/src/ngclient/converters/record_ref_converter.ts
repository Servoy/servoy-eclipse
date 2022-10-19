import { IType, IPropertyContext } from '../../sablo/types_registry';
import { RowValue } from '../../ngclient/converters/foundset_converter';
import { ViewportService } from '../services/viewport.service';

export class RecordRefType implements IType<any> {

    public static readonly TYPE_NAME = 'record';

    private static readonly FOUNDSET_ID = 'foundsetId';

    // eslint-disable-next-line @typescript-eslint/ban-types
    static generateRecordRef(rowId: string, foundsetId: number): object {
        const recordRef = {};
        recordRef[ViewportService.ROW_ID_COL_KEY] = rowId;
        recordRef[RecordRefType.FOUNDSET_ID] = foundsetId;
        return recordRef;
    }

	fromServerToClient(serverJSONValue: any, _currentClientValue: any, _propertyContext: IPropertyContext): any {
		// no conversions to be done here; server does support sending records to client but just as references via hashes / and pk hints
		// that currently are not automatically transformed on client into a RowValue instance...
		return serverJSONValue;
	}

	fromClientToServer(newClientData: RowValue, _oldClientData: any, _propertyContext: IPropertyContext): [any, RowValue] {
		return [RecordRefType.generateRecordRef(newClientData.getId(), newClientData.getFoundset().getId()), newClientData];
	}

}
