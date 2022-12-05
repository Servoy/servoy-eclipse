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
		// currently server sends something like:
		// { recordhash: ..., foundsetId: ..., _svyRowId: ... }
		// but it is not meant to be used yet to identify the record on client (that is why we return any and NOT ServerSentRecordRef), but just
		// to send it back to server as a reference; it can be changed in the future if needed
		return serverJSONValue;
	}

	fromClientToServer(newClientData: RowValue | ServerSentRecordRef, _oldClientData: any, _propertyContext: IPropertyContext): [any, RowValue | ServerSentRecordRef] {
        if (instanceOfServerSentRecordRef(newClientData)) return [newClientData, newClientData];
		else return [RecordRefType.generateRecordRef(newClientData.getId(), newClientData.getFoundset().getId()), newClientData];
	}

}

export const instanceOfServerSentRecordRef = (obj: any): obj is ServerSentRecordRef =>
    obj != null && (obj as ServerSentRecordRef).recordhash !== undefined;

class ServerSentRecordRef {
    recordhash: string;
    foundsetId: number;
     _svyRowId: string;
}
