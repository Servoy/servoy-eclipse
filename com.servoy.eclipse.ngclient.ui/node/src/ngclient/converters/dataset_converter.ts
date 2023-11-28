import { IType, IPropertyContext, ITypesRegistryForSabloConverters, ITypeFromServer } from '../../sablo/types_registry';
import { ConverterService } from '../../sablo/converter.service';

export class DatasetType implements IType<any> {

    public static readonly TYPE_NAME = 'dataset';

	private static readonly VALUE_KEY = 'v';
	private static readonly TYPES_KEY = 't';
	private static readonly INCLUDES_COLUMN_NAMES_KEY = 'i';

	constructor(private readonly typesRegistry: ITypesRegistryForSabloConverters,
			private readonly converterService: ConverterService<unknown>) {}

	fromServerToClient(serverJSONValue: any, _currentClientValue: any, propertyContext: IPropertyContext): any {
		let datasetValue: any = serverJSONValue;

		if (datasetValue) {
			const columnTypesFromServer: { [columnIndex: string]: ITypeFromServer } = datasetValue[DatasetType.TYPES_KEY];
			let columnTypes: { [columnIndex: string]: IType<any> };

			datasetValue = datasetValue[DatasetType.VALUE_KEY];

			if (columnTypesFromServer) {
				// find the actual client side types; these will be only basic types so no need to
				// do this.typesRegistry.processTypeFromServer(...) instead of this.typesRegistry.getAlreadyRegisteredType(columnTypesFromServer[colIdx]) 
				columnTypes = {};
				for (const colIdx of Object.getOwnPropertyNames(columnTypesFromServer))
					columnTypes[colIdx] = this.typesRegistry.getAlreadyRegisteredType(columnTypesFromServer[colIdx]);
			}

            // first row might be just the column names; those don't need any server-to-client conversions and shouldn't use column conversions on them
			let rowNo = (datasetValue[DatasetType.INCLUDES_COLUMN_NAMES_KEY] ? 1 : 0);
			while (rowNo < datasetValue.length) {
				const row: [any] = datasetValue[rowNo];
				row.forEach((cellValue: any, columnIndex: number) => {
					// apply either default conversion or the one from spec (for each column) if present
					row[columnIndex] = this.converterService.convertFromServerToClient(cellValue, columnTypes ? columnTypes[columnIndex] : undefined, undefined,
			        		undefined, undefined, propertyContext);
				});
				rowNo++;
			}
		}
		return datasetValue;
	}

	fromClientToServer(_newClientData: any, _oldClientData: any, _propertyContext: IPropertyContext): [any, any] | null {
		// not supported
		return null;
	}

}
