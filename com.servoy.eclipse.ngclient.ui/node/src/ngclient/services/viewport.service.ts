import { Injectable } from '@angular/core';
import { ChangeType, IFoundset, LoggerService, ViewportChangeEvent, ViewportChangeListener } from '@servoy/public';
import {
    ConverterService, ChangeAwareState, isChanged, instanceOfChangeAwareValue,
    SubpropertyChangeByReferenceHandler, SoftProxyRevoker
} from '../../sablo/converter.service';
import {
    IType, ITypeFromServer, IPropertyContext, PushToServerEnum,
    IWebObjectSpecification, TypesRegistry
} from '../../sablo/types_registry';
import { SabloDeferHelper, IDeferedState } from '../../sablo/defer.service';
import { SabloService } from '../../sablo/sablo.service'

/** this is what the server sends for type info (a compressed way of sending types for all cells) */
export interface ConversionInfoFromServerForViewport {
    'mT': string | null; // main type (main fallback type if cell/column has no type)
    'cT'?: MultipleColumnsConversionInfoFromServer | SingleColumnConversionInfoFromServer;
};

export interface RowUpdate {

    type: ChangeType.ROWS_CHANGED | ChangeType.ROWS_INSERTED | ChangeType.ROWS_DELETED;
    rows: any[];
    startIndex: number;
    endIndex: number;
    _T: ConversionInfoFromServerForViewport;

}

/** if it is a multiple column viewport (like for foundset properties or component properties) */
interface MultipleColumnsConversionInfoFromServer {
    [columnName: string]: SingleColumnConversionInfoFromServer;
};

interface SingleColumnConversionInfoFromServer {
    '_T'?: string | null; // column level type (fallback at column level in case cell type was not defined); not present
    //for single column viewports (like for foundset linked properties; as they only have a single column and they can use main type there is no need for column type)
    'eT'?: ColumnCellConversionInfoFromServer;
};

type ColumnCellConversionInfoFromServer = [
    { '_T': string | null; 'i': [number] }
];


/** this is what the viewport keeps in internal state for easy type access of cells after expanding the ConversionInfoFromServerForViewport that it gets from the server */
interface ExpandedViewportTypes {
    [rowIdx: number]: IType<any> | { [columnName: string]: IType<any> };
}

@Injectable({
    providedIn: 'root'
})
export class ViewportService {

    public static readonly ROW_ID_COL_KEY = '_svyRowId';
    public static readonly ID_KEY = 'id'; // for requestID in case a defer is generated and a promise returned

    // this key/column should be stored as $foundsetTypeConstants.ROW_ID_COL_KEY in the actual row, but this key is sent from server when the foundset property is sending
    // just a partial update, but some of the columns that did change are also pks so they do affect the pk hash; client uses this to distiguish between a full
    // update of a row and a partial update of a row; so if update has $foundsetTypeConstants.ROW_ID_COL_KEY it will consider it to be a full update,
    // and if it has either ROW_ID_COL_KEY_PARTIAL_UPDATE or no rowID then it is a partial update of a row (only some of the columns in that row have changed)
    private static readonly ROW_ID_COL_KEY_PARTIAL_UPDATE = '_svyRowId_p';

    private static NULL_AND_REJECT_PROP_CONTEXT: IPropertyContext = {
        getProperty: (_propertyName: string) => null,
        getPushToServerCalculatedValue: () => PushToServerEnum.REJECT,
        isInsideModel: true
    };

    constructor(private converterService: ConverterService<unknown>,
        private readonly typesRegistry: TypesRegistry, private sabloDeferHelper: SabloDeferHelper) { }

    /**
     * It will update the whole viewport. More precisely it will apply all server-to-client conversions directly on given viewPortUpdate param and return it.
     *
     * @param oldViewPort old viewport
     * @param viewPortUpdate the whole viewport update value from server
     * @param defaultColumnTypes only used for component type viewports - where the default column (component property) types (so non-dynamic types) are
     *                           already known. All other viewports should give null here.
     * @param simpleRowValue true if each row in this viewport is a single value and false if each row in this viewport has columns / multiple values
     *
     * @return the given viewPortUpdate with conversions applied to it
     */
    public updateWholeViewport<T extends any[]>(oldViewPort: T, internalState: FoundsetViewportState, viewPortUpdate: any[],
        viewPortUpdateConversions: ConversionInfoFromServerForViewport,
        defaultColumnTypes: IWebObjectSpecification,
        propertyContextCreator: IPropertyContextCreatorForRow,
        simpleRowValue: boolean,
        rowCreator?: () => any, cellUpdatedFromServerListener?: CellUpdatedFromServerListener): T {

        // clear change notifiers for old smart values in viewport
        for (let i = oldViewPort.length - 1; i >= 0; i--) {
            this.updateChangeAwareNotifiersForRow(i, oldViewPort, internalState, simpleRowValue, propertyContextCreator, true);
        }

        // update conversion info; expand what we get from server to be easy to use on client (like main type and main column type from JSON are kept at cell level)
        internalState.viewportTypes = {};
        const convertedViewPortUpdate = this.expandTypeInfoAndApplyConversions(viewPortUpdateConversions, defaultColumnTypes,
            viewPortUpdate, 0, oldViewPort, internalState, propertyContextCreator, simpleRowValue,
            true, rowCreator, cellUpdatedFromServerListener);

        // we keep oldViewPort reference if available - because, for example it could be a FoundsetLinkedValue (where the 'viewport' is
        // also the full value of the prop) and we don't want to alter it's class type;
        // it could be that we still do alter the reference though due to addViewportOrRowProxiesIfNeeded call below, but it's still just a proxy to the correct class type then
        oldViewPort.splice(0); // clear oldViewport
        for (const rowUpdate of convertedViewPortUpdate) oldViewPort.push(rowUpdate);

        const newViewport = this.addViewportOrRowProxiesIfNeeded(oldViewPort, internalState, propertyContextCreator, simpleRowValue);

        // link new smart viewport values to parent change notifier so that things such as let's say child valuelist.filter(...) do get sent to server correctly
        for (let i = newViewport.length - 1; i >= 0; i--) {
            this.updateChangeAwareNotifiersForRow(i, newViewport, internalState, simpleRowValue, propertyContextCreator, false);
        }

        return newViewport;
    }

    // see comment above, before updateWholeViewport()
    public updateViewportGranularly(viewPort: any[], internalState: FoundsetViewportState, rowUpdates: RowUpdate[], defaultColumnTypes: IWebObjectSpecification,
        propertyContextCreator: IPropertyContextCreatorForRow,
        simpleRowValue: boolean/*not key/value pairs in each row*/,
        rowCreator?: () => any, cellUpdatedFromServerListener?: CellUpdatedFromServerListener): void {
        // partial row updates (remove/insert/update)

        const worksWithRowLevelProxies = !simpleRowValue && this.needsRowProxies(internalState, propertyContextCreator);

        // {
        //   "rows": rowData, // array again
        //   "startIndex": ...,
        //   "endIndex": ...,
        //   "type": ... // ONE OF CHANGE = 0; INSERT = 1; DELETE = 2;
        // }

        // apply granular updates one by one
        for (let i = 0; i < rowUpdates.length; i++) {
            const rowUpdate = rowUpdates[i];
            if (rowUpdate.type === ChangeType.ROWS_CHANGED) {
                // if the rowUpdate rows contain '_svyRowId' then we know it's the entire/complete row object; same if it's a one value per row (foundset linked);
                // one granular "CHANGE" operation can only have all changed rows with ROW_ID_COL_KEY or all changed rows without ROW_ID_COL_KEY, that is why we
                // can check only rowUpdate.rows[0] below, see FoundsetTypeRowDataProvider.populateRowData() which always uses "columnNames" to decide that which is
                // the same for all rows that are written in a "CHANGE" op
                const wholeRowUpdates = simpleRowValue || rowUpdate.rows[0][ViewportService.ROW_ID_COL_KEY];

                // clear notifiers for any old smart values
                for (let j = rowUpdate.startIndex; j <= rowUpdate.endIndex; j++) {
                    this.updateChangeAwareNotifiersForRow(j, viewPort, internalState, simpleRowValue, propertyContextCreator, true);
                }

                const convertedRowChangeData = this.expandTypeInfoAndApplyConversions(rowUpdate._T, defaultColumnTypes,
                    rowUpdate.rows, rowUpdate.startIndex, viewPort, internalState, propertyContextCreator,
                    simpleRowValue, wholeRowUpdates, rowCreator, cellUpdatedFromServerListener);

                convertedRowChangeData.forEach((newRowValue, rowUpdateIndex) => {
                    const viewportIndex = rowUpdate.startIndex + rowUpdateIndex;
                    if (wholeRowUpdates) {
                        viewPort[viewportIndex] = newRowValue;

                        if (worksWithRowLevelProxies) {
                            if (internalState.rowLevelProxyStates[viewportIndex]) internalState.rowLevelProxyStates[viewportIndex].softProxyRevoker.getRevokeFunction()();
                            viewPort[viewportIndex] = this.addRowProxyTo(viewPort, viewportIndex, propertyContextCreator, internalState);
                        }
                    } else {
                        // key/value pairs in each row and this is a partial row update (maybe just one col. in a row has changed; leave the rest as they were)
                        for (const dpName of Object.keys(newRowValue)) {
                            // update value
                            viewPort[viewportIndex][dpName] = newRowValue[dpName];
                        }
                    }

                    // add back notifiers for any new smart values
                    this.updateChangeAwareNotifiersForRow(viewportIndex, viewPort, internalState, simpleRowValue, propertyContextCreator, false);
                });
            } else if (rowUpdate.type === ChangeType.ROWS_INSERTED) {
                if (internalState.viewportTypes || internalState.rowLevelProxyStates) {
                    // shift conversion info of other rows if needed (viewportTypes is an object with number keys, can't use array splice directly
                    // and rowLevelProxyStates contain the indexes that need to be updated in the elements as well (those are used by proxy handlers), so we still need to iterate
                    for (let j = viewPort.length - 1; j >= rowUpdate.startIndex; j--) {
                        if (internalState.viewportTypes) {
                            if (internalState.viewportTypes[j]) internalState.viewportTypes[j + rowUpdate.rows.length] = internalState.viewportTypes[j];
                            else if (internalState.viewportTypes[j + rowUpdate.rows.length]) delete internalState.viewportTypes[j + rowUpdate.rows.length];
    
                            delete internalState.viewportTypes[j];
                        }
                        if (internalState.rowLevelProxyStates) {
                            if (internalState.rowLevelProxyStates[j]) {
                                internalState.rowLevelProxyStates[j].rowIndex += rowUpdate.rows.length;
                                internalState.rowLevelProxyStates[j + rowUpdate.rows.length] = internalState.rowLevelProxyStates[j];
                            } else if (internalState.rowLevelProxyStates[j + rowUpdate.rows.length]) delete internalState.rowLevelProxyStates[j + rowUpdate.rows.length]; // this else will never happen I think (either all rows are proxied or none)
    
                            delete internalState.rowLevelProxyStates[j];
                        }
                    }
                }
                const convertedRowChangeData = this.expandTypeInfoAndApplyConversions(rowUpdate._T, defaultColumnTypes,
                    rowUpdate.rows, rowUpdate.startIndex, null, internalState, propertyContextCreator,
                    simpleRowValue, true, rowCreator, cellUpdatedFromServerListener);

                viewPort.splice(rowUpdate.startIndex, 0, ...convertedRowChangeData);

                rowUpdate.endIndex = rowUpdate.startIndex + rowUpdate.rows.length - 1; // prepare rowUpdate.endIndex for listener notifications

                // add change notifiers and proxies for inserted rows if needed and update the ones that were shifted right after insert
                for (let j = rowUpdate.startIndex; j < rowUpdate.startIndex + rowUpdate.rows.length; j++) {
                    if (worksWithRowLevelProxies)
                        viewPort[j] = this.addRowProxyTo(viewPort, j, propertyContextCreator, internalState);

                    this.updateChangeAwareNotifiersForRow(j, viewPort, internalState, simpleRowValue, propertyContextCreator, false);
                }
                for (let j = rowUpdate.startIndex + rowUpdate.rows.length; j < viewPort.length; j++) {
                    this.updateChangeAwareNotifiersForRow(j, viewPort, internalState, simpleRowValue, propertyContextCreator, false);
                }

            } else if (rowUpdate.type === ChangeType.ROWS_DELETED) {
                const oldLength = viewPort.length;
                const numberOfDeletedRows = rowUpdate.endIndex - rowUpdate.startIndex + 1;
                if (internalState.viewportTypes) {
                    // delete conversion info for deleted rows and shift left what is after deletion
                    for (let j = rowUpdate.startIndex; j <= rowUpdate.endIndex; j++)
                        delete internalState.viewportTypes[j];
                    for (let j = rowUpdate.endIndex + 1; j < oldLength; j++) {
                        if (internalState.viewportTypes[j]) internalState.viewportTypes[j - numberOfDeletedRows] = internalState.viewportTypes[j];
                        else if (internalState.viewportTypes[j - numberOfDeletedRows]) delete internalState.viewportTypes[j - numberOfDeletedRows];

                        delete internalState.viewportTypes[j];
                    }
                }

                // remove any change notifiers for the rows that were deleted
                for (let j = rowUpdate.startIndex; j <= rowUpdate.endIndex; j++) {
                    if (worksWithRowLevelProxies)
                        internalState.rowLevelProxyStates[j].softProxyRevoker.getRevokeFunction()(); // disable proxy notifications for deleted row

                    this.updateChangeAwareNotifiersForRow(j, viewPort, internalState, simpleRowValue, propertyContextCreator, true);
                }

                if (worksWithRowLevelProxies)
                    internalState.rowLevelProxyStates.splice(rowUpdate.startIndex, numberOfDeletedRows);

                viewPort.splice(rowUpdate.startIndex, numberOfDeletedRows);

                // update any change notifiers for the rows that were shifted left after delete and update any proxies
                for (let j = rowUpdate.startIndex; j < viewPort.length; j++) {
                    if (worksWithRowLevelProxies)
                        internalState.rowLevelProxyStates[j].rowIndex = j;

                    this.updateChangeAwareNotifiersForRow(j, viewPort, internalState, simpleRowValue, propertyContextCreator, false);
                }
            }

            delete rowUpdate._T;
            delete rowUpdate.rows; // prepare rowUpdate for listener notifications
        }
    }

    public getClientSideTypeFor(rowId: any, columnName: string, internalState: FoundsetViewportState, viewPortRowsInCaseCallerIsActualFoundsetProp?: any[]): IType<any> {
        // find the index for this rowId
        let idx = -1;
        if (internalState.forFoundset !== undefined) {
            idx = internalState.forFoundset().viewPort.rows.findIndex((val: any) => val[ViewportService.ROW_ID_COL_KEY] === rowId);
        } else {
            // if it doesn't have internalState.forFoundset then it's probably the foundset property's viewport directly which has those in the viewport
            idx = viewPortRowsInCaseCallerIsActualFoundsetProp.findIndex((val: any) => val[ViewportService.ROW_ID_COL_KEY] === rowId);
        }

        let clientSideType: IType<any>;
        if (idx >= 0) {
            const clientSideTypes = internalState.viewportTypes ? internalState.viewportTypes[idx] : undefined;
            if (clientSideTypes && (!columnName || clientSideTypes[columnName]))
                clientSideType = (columnName ? clientSideTypes[columnName] : clientSideTypes) as IType<any>;
        }
        return clientSideType;
    }

    /**
     * An explicit call from the component to send changes for a cell of the viewport (only makes sense I think for dumb (pure JSON, 'object' property types)
     * nested structures that have DEEP PushToServer in .spec)
     *
     * @param deferredState this can be null/undefined; when this is given, queueChange will generate a defer for this request and return a promise.
     * @returns undefined if deferredState is not given, if you are trying to update the rowId column, if push to server setting does not allow this change or if the value has not changed;
     *                    it will return a promise otherwise - if the update was sent to server.
     */
    public sendCellChangeToServerBasedOnRowId(viewPort: any[], internalState: FoundsetViewportState, deferredState: IDeferedState, rowID: string, columnName: string,
        propertyContextCreator: IPropertyContextCreatorForRow, newValue: any, oldValue?: any): Promise<any> {

        if (this.getCellPropertyContextFor(propertyContextCreator, undefined, columnName).getPushToServerCalculatedValue() < PushToServerEnum.ALLOW) {
            internalState.log.spam(internalState.log.buildMessage(() => ('svy viewport * sendCellChangeToServerBasedOnRowId denied because pushToServer < ALLOW in .spec ('
                + rowID + ', ' + columnName + ', ' + newValue)));
            return;
        }

        if (columnName === ViewportService.ROW_ID_COL_KEY) {
            internalState.log.spam(internalState.log.buildMessage(() => ('svy viewport * sendCellChangeToServerBasedOnRowId denied; cannot send changes to _svyRowId columns!')));
            return;
        }
        if (!isChanged(newValue, oldValue)) {
            internalState.log.spam(internalState.log.buildMessage(() => ('svy viewport * sendCellChangeToServerBasedOnRowId denied; no changes detected between new and old value!')));
            return;
        }

        const viewportThatContainsRowIDs = (internalState.forFoundset !== undefined ? internalState.forFoundset().viewPort.rows : viewPort);
        const rowIndex = viewportThatContainsRowIDs.findIndex((row) => (row._svyRowId === rowID));
        if (rowIndex === -1) internalState.log.error(internalState.log.buildMessage(() =>
            ('svy viewport * sendCellChangeToServerBasedOnRowId cannot find row index from given rowID ("' + rowID + '"')));

        return this.reallyQueueChange(viewPort, internalState, deferredState, rowIndex, columnName,
            this.getCellPropertyContextFor(propertyContextCreator, rowIndex >= 0 ? viewPort[rowIndex] : undefined, columnName),
            newValue, oldValue);
    }

    private getCellPropertyContextFor(propertyContextCreator: IPropertyContextCreatorForRow, rowValue: any, propertyName: string): IPropertyContext {
        const propertyContext = propertyContextCreator.withRowValueAndPushToServerFor(rowValue, propertyName);
        return propertyContext ? propertyContext : ViewportService.NULL_AND_REJECT_PROP_CONTEXT;
    };

    // IMPORTANT: This comment should always match ConversionInfoFromServerForViewport declared above AND the comment and impl of ViewportClientSideTypes.getClientSideTypes() java method
    // and the code in foundsetLinked.ts -> generateWholeViewportFromOneValue.
    //
    // This is what we get as conversion info from server for viewports or viewport updates; indexes are relative to the received data (either full viewport or viewport update data).
    // Basically the type of one cell in received data is the one in CELL_TYPES of COL_TYPES that matches FOR_ROW_IDXS; if that is not present it falls back to the main CONVERSION_CL_SIDE_TYPE_KEY
    // in that column, if that is not present it falls back to MAIN_TYPE. If it's missing completely then no data in the viewport needs client side conversions.
    //
    // *  "mT": "date",
    // *  "cT": {
    // *     "b": { "_T": null},
    // *     "c": {`
    // *         "eT":
    // *           [
    // *             { "_T": null, "i": [4] },
    // *             { "_T": "zyx", "i": [2,5,9] },
    // *             {"_T": "xyz", "i": [0] }
    // *           ]
    // *       }
    // *   }
    // * }
    // * or if it is a single column (like for foundset linked properties it will be something like)
    // *  "mT": "date",
    // *  "cT": {
    // *         "eT":
    // *           [
    // *             { "_T": null, "i": [4] },
    // *             { "_T": "zyx", "i": [2,5,9] },
    // *             {"_T": "xyz", "i": [0] }
    // *           ]
    // *   }
    // *
    // * where
    // *   ISabloConverters.CONVERSION_CL_SIDE_TYPE_KEY   == "_T"
    // *   ViewportService.MAIN_TYPE       == "mT"
    // *   ViewportService.COL_TYPES       == "cT"
    // *   ViewportService.CELL_TYPES      == "eT"
    // *   ViewportService.FOR_ROW_IDXS    == "i"

    /**
     * See comment above for what we get from server. We expand that / translate it to conversion info for each non-null cell conversion so that any future updates to the viewport can
     * easily be merged (that is why we don't keep the server unexpanded format on client which could be used directly to determine each cell's type - to avoid complicated merges
     * when we receive granular updates).<br/><br/>
     *
     * This method also remembers the types and applies the conversions to given array of rows, returning the array of rows converted.<br/>
     * The types are stored at indexes shifted with startIdxInViewportForRowsToBeConverted; old values are taken from the oldViewportRows shifted with the same number.
     *
     * @param rowsToBeConverted the data from server for received rows or row granular updates array.
     * @param simpleRowValue true if each row in this viewport is a single value and false if each row in this viewport has columns / multiple values
     * @return the converted-to-client rows for rowsToBeConverted param; it's actually the same reference as given param - to which any server->client conversions were also applied.
     */
    private expandTypeInfoAndApplyConversions(serverConversionInfo: ConversionInfoFromServerForViewport, defaultColumnTypes: IWebObjectSpecification,
        rowsToBeConverted: any[], startIdxInViewportForRowsToBeConverted: number, oldViewportRows: any[], internalState: FoundsetViewportState,
        propertyContextCreator: IPropertyContextCreatorForRow, simpleRowValue: boolean, fullRowUpdates: boolean, rowCreator?: () => any,
        cellUpdatedFromServerListener?: CellUpdatedFromServerListener): any[] {

        if (rowsToBeConverted) { // if it's a simpleRowValue without serverConversionInfo we don't need to alter what we received at all
            rowsToBeConverted.forEach((rowData, index) => {
                if (simpleRowValue) {
                    // without columns, so a foundset linked prop's rows
                    // foundsetLinked viewport; rowCreator should be undefined here so we ignore it
                    const cellConversion = serverConversionInfo ? this.getCellTypeFromServer(serverConversionInfo, index) : undefined; // this is the whole row in this case (only one cell)
                    // defaultColumnTypes should be null here because it's not a component prop's viewport so no need to check for it
                    const oldCellVal = oldViewportRows ? oldViewportRows[startIdxInViewportForRowsToBeConverted + index] : undefined;

                    rowsToBeConverted[index] = this.converterService.convertFromServerToClient(rowsToBeConverted[index],
                        cellConversion, oldCellVal,
                        undefined /*dynamic types are already handled via serverConversionInfo here*/, undefined,
                        this.getCellPropertyContextFor(propertyContextCreator, undefined, undefined));
                    this.updateRowTypes(startIdxInViewportForRowsToBeConverted + index, internalState, cellConversion);

                    if (cellUpdatedFromServerListener) cellUpdatedFromServerListener(startIdxInViewportForRowsToBeConverted + index, undefined, oldCellVal, rowsToBeConverted[index]);
                } else {
                    // with columns; so a foundset prop's rows or a component type prop's rows
                    let rowConversions = (fullRowUpdates || !internalState.viewportTypes ? undefined : internalState.viewportTypes[startIdxInViewportForRowsToBeConverted + index]);
                    const newRowData = rowCreator ? (rowCreator()) : rowData;

                    Object.keys(rowData).forEach(columnName => {
                        let cellConversion: IType<any> = serverConversionInfo ? this.getCellTypeFromServer(serverConversionInfo, index, columnName) : undefined;
                        if (!cellConversion && defaultColumnTypes) cellConversion = defaultColumnTypes.getPropertyType(columnName);

                        // ignore null or undefined type of cell; otherwise remember it in expanded
                        if (cellConversion) {
                            if (!rowConversions) rowConversions = {};
                            rowConversions[columnName] = cellConversion;
                        } else if (rowConversions && rowConversions[columnName]) delete rowConversions[columnName];

                        const oldCellVal = oldViewportRows ? (oldViewportRows[startIdxInViewportForRowsToBeConverted + index] ?
                            (oldViewportRows[startIdxInViewportForRowsToBeConverted + index][columnName])
                            : undefined)
                            : undefined;

                        newRowData[columnName] = this.converterService.convertFromServerToClient(rowData[columnName],
                            cellConversion,
                            oldCellVal,
                            undefined /*dynamic types are already handled via serverConversionInfo here*/, undefined,
                            this.getCellPropertyContextFor(propertyContextCreator, newRowData, columnName));

                        if (cellUpdatedFromServerListener) cellUpdatedFromServerListener(startIdxInViewportForRowsToBeConverted + index, columnName, oldCellVal, rowData[columnName]);
                    });
                    if (newRowData[ViewportService.ROW_ID_COL_KEY_PARTIAL_UPDATE] !== undefined) {
                        // see comment of ROW_ID_COL_KEY_PARTIAL_UPDATE
                        newRowData[ViewportService.ROW_ID_COL_KEY] = newRowData[ViewportService.ROW_ID_COL_KEY_PARTIAL_UPDATE];
                        delete newRowData[ViewportService.ROW_ID_COL_KEY_PARTIAL_UPDATE];
                    }

                    rowsToBeConverted[index] = newRowData;

                    if (rowConversions && Object.keys(rowConversions).length === 0) rowConversions = undefined; // in case all conversion infos from one row were deleted due to the update
                    this.updateRowTypes(startIdxInViewportForRowsToBeConverted + index, internalState, rowConversions);
                }
            });
        }

        return rowsToBeConverted;
    }

    private getCellTypeFromServer(serverConversionInfo: ConversionInfoFromServerForViewport, rowIndex: number, columnName?: string): IType<any> {
        const PROCESSED_CELL_TYPES = 'pct'; // just to turn stuff like [{ "_T": "zyx", "i": [2,3] }, {"_T": "xyz", "i": [9] }] into an easier to use { 2: "zyx", 3: "zyx", 9: "xyz" }
        const mainType = serverConversionInfo.mT; // main fallback type
        const columnTypes = serverConversionInfo.cT; // column & cell types

        let cellConversion: ITypeFromServer;
        if (columnTypes) {
            // foundset/component viewport (multi col) or foundset linked viewport (single col) data
            const colType = (columnName ? (columnTypes as MultipleColumnsConversionInfoFromServer)[columnName] : (columnTypes as SingleColumnConversionInfoFromServer));
            if (colType) {
                let processed = colType[PROCESSED_CELL_TYPES];
                if (!processed) {
                    processed = colType[PROCESSED_CELL_TYPES] = {};
                    if (colType.eT /* cell types */)
                        colType.eT.forEach(
                            value => value.i.forEach(
                                ri => processed[ri] = value._T));
                }

                cellConversion = processed[rowIndex]; // look at cell type; null is a valid type in what server can send so we check with === undefined
                if (cellConversion === undefined) cellConversion = colType._T; // fallback to column type
                if (cellConversion === undefined) cellConversion = mainType; // fallback to main type
            } else cellConversion = mainType;
        } else cellConversion = mainType;

        if (cellConversion) return this.typesRegistry.getAlreadyRegisteredType(cellConversion);

        return undefined;
    }

    /**
     * @param types can be one IType<?>  or an object of IType<?> for each column on that row.
     */
    private updateRowTypes(idx: number, internalState: FoundsetViewportState, types: IType<any> | { [colName: string]: IType<any> }) {
        if (internalState.viewportTypes === undefined) {
            internalState.viewportTypes = {};
        }
        if (types == undefined && internalState.viewportTypes[idx] != undefined) delete internalState.viewportTypes[idx];
        else if (types != undefined) internalState.viewportTypes[idx] = types;
    }

    private updateChangeAwareNotifiersForRow(rowIdx: number, viewPort: any[], internalState: FoundsetViewportState,
        simpleRowValue: boolean, propertyContextCreator: IPropertyContextCreatorForRow, clearNotifier: boolean) {
        if (simpleRowValue) {
            this.updateChangeAwareNotifiersForCell(viewPort[rowIdx], viewPort, internalState, rowIdx, undefined,
                this.getCellPropertyContextFor(propertyContextCreator, undefined, undefined), clearNotifier);
        } else {
            for (const columnName of Object.getOwnPropertyNames(viewPort[rowIdx])) { // (in case of a child component viewport) we only want to iterate on properties from the viewport, not from the model that is not record dependent (that is a prototype of the row here)
                if (columnName !== ViewportService.ROW_ID_COL_KEY && viewPort[rowIdx].propertyIsEnumerable(columnName))
                    this.updateChangeAwareNotifiersForCell(viewPort[rowIdx][columnName], viewPort, internalState,
                        rowIdx, columnName,
                        this.getCellPropertyContextFor(propertyContextCreator, viewPort[rowIdx], columnName),
                        clearNotifier);
            }
        }
    }

    private updateChangeAwareNotifiersForCell(cellValue: any, viewPort: any[], internalState: FoundsetViewportState,
        rowIdx: number, columnName: string, propertyContext: IPropertyContext, clearNotifier: boolean) {
        if (instanceOfChangeAwareValue(cellValue)) {
            // child is able to handle it's own change mechanism
            cellValue.getInternalState().setChangeListener(clearNotifier ? undefined : (_doNotPush?: boolean) => {
                this.reallyQueueChange(viewPort, internalState, undefined, rowIdx, columnName, propertyContext, cellValue, cellValue);
            });
        }
    }

    /**
     * Sends a cell update/change to server.
     *
     * @param deferredState this can be null/undefined; when this is given, queueChange will generate a defer for this request and return a promise.
     * @returns undefined if deferredState is not given, if you are trying to update the rowId column, if push to server does setting not allow this change or if the value has not changed;
     *                    it will return a promise otherwise - if the update was sent to server.
     */
    private reallyQueueChange(viewPort: any[], internalState: FoundsetViewportState, deferredState: IDeferedState, idx: number, columnName: string,
        propertyContext: IPropertyContext, newValue: any, oldValue?: any, doNotPush?: boolean): Promise<any> {
        let promise: Promise<any>;

        const previousIgnoreChanges = internalState.ignoreChanges;
        internalState.ignoreChanges = true; // we want to disable the viewport and viewport row proxies here, as below we do assign the new conversion result back into the viewport - and that should not result in a stack overflow
        
        try {
            // if it doesn't have internalState.forFoundset then it's probably the foundset property's viewport directly which has those in the viewport
            const r: CellChangeToServer = {
                _svyRowId: (internalState.forFoundset !== undefined ? internalState.forFoundset().viewPort.rows[idx]._svyRowId : viewPort[idx]._svyRowId),
                value: newValue
            };
            if (columnName !== undefined) r.dp = columnName;
    
            // convert new data if necessary
            const clientSideTypesForRow = internalState.viewportTypes?.[idx];
    
            const convResult = this.converterService.convertFromClientToServer(r.value, columnName ? clientSideTypesForRow?.[columnName] : clientSideTypesForRow, oldValue, propertyContext);
            r.value = convResult[0];
            if (columnName !== undefined) viewPort[idx][columnName] = convResult[1];
            else viewPort[idx] = convResult[1];
    
            const req = { viewportDataChanged: r };
            if (deferredState) {
                const requestID = this.sabloDeferHelper.getNewDeferId(deferredState);
                req[ViewportService.ID_KEY] = requestID;
                promise = deferredState.deferred[requestID].defer.promise;
            }
    
            internalState.requests.push(req);
        } finally {
            internalState.ignoreChanges = previousIgnoreChanges;
        }
        internalState.notifyChangeListener(doNotPush);

        return promise;
    }

    private addViewportOrRowProxiesIfNeeded<T extends any[]>(viewPort: T, internalState: FoundsetViewportState, propertyContextCreator: IPropertyContextCreatorForRow,
                            simpleRowValue: boolean): T {

        let resultingViewport = viewPort;

        if (simpleRowValue) {
            // foundset linked probably - where the viewport is just an array of values; if the values have PushToServer SHALLOW OR DEEP (same as shallow in ng2)
            // so in that case we need a proxy on the viewport itself to detect cell (which is the same as row) value changes
            const cellPropertyContext = this.getCellPropertyContextFor(propertyContextCreator, undefined, undefined);
            if (cellPropertyContext.getPushToServerCalculatedValue() > PushToServerEnum.ALLOW && !internalState.viewportLevelProxyRevokerFunc) {
                // add the actual viewport proxy for listening to cell change-by-ref
                resultingViewport = new Proxy(viewPort, this.getViewportLevelProxyHandler(viewPort, cellPropertyContext, internalState));
            }
        } else {
            // component viewport or foundset viewport; we don't need to proxy the viewport itself to detect change-by-ref for cells
            // but rather each row needs to be proxied

            // if a full new viewport if being created, disable the row proxy notifications for the old viewport rows (if present); those are now obsolete - so that the old viewport
            // cannot trigger wrong SHALLOW change notifications for cells that are not changed in the new viewport
            if (internalState.rowLevelProxyStates) internalState.rowLevelProxyStates.forEach((rowLevelProxyState) => rowLevelProxyState.softProxyRevoker.getRevokeFunction()());
            internalState.rowLevelProxyStates = [];

            // see if we have any columns
            if (this.needsRowProxies(internalState, propertyContextCreator)) {
                for (let i = viewPort.length - 1; i >= 0; i--) {
                    viewPort[i] = this.addRowProxyTo(viewPort, i, propertyContextCreator, internalState);
                }
            }
        }

        return resultingViewport;
    }

    private addRowProxyTo(viewPort: any[], rowIndex: number, propertyContextCreator: IPropertyContextCreatorForRow, internalState: FoundsetViewportState) {
        // add the actual row proxy for listening to cell change-by-ref
        return new Proxy(viewPort[rowIndex], this.getRowLevelProxyHandler(viewPort, rowIndex, propertyContextCreator, internalState));
    }

    private needsRowProxies(internalState: FoundsetViewportState, propertyContextCreator: IPropertyContextCreatorForRow): boolean {

        if (internalState.needsRowProxies !== undefined)
            return internalState.needsRowProxies;

        // it can be a foundset type viewport; it then only has 1 property context for all columns currently - so just check it
        // or component type for which we should return false anyway (and .getCellPropertyContextFor() with undefined column name in component_converter.ts will give REJECT); see comment below
        internalState.needsRowProxies = (this.getCellPropertyContextFor(propertyContextCreator, undefined, undefined)
            .getPushToServerCalculatedValue() >= PushToServerEnum.SHALLOW);

        // if this is the viewport for a child "component" property type value, we don't really need to proxy it then; components in ng2 are responsible
        // for "emiting" changes of root component properties and the one that is using the "component" property (for example a list form component) is responsible
        // for linking that emit of the child component to the ChildComponentPropertyValue.sendChanges of the component property value; so there is no need to
        // send changes automatically then via a proxy (root properties of components will not be updated in the models anyway by the component itself, child
        // component only has access to the @Input and @Output - so it can't set root props. directly in the model itself (it does not have access to it)
        // so those will not change anyway until an emit happens...)

        return internalState.needsRowProxies;
        // undefined is turned into false if we cannot decide at this point if viewport rows need proxies or not
    }

    /**
     * Handler for the Proxy object that will detect reference changes in the viewport itself when it is needed. It DOES NOT NEED to listen
     * for any operations that imply length changes (add/insert/delete) because that is not controlled by the user (component code) but by the foundset
     * property type and viewport property type (and other prop types that use them).
     *
     * This implements the shallow PushToServer behavior for singleValue / foundset linked types.
     */
    private getViewportLevelProxyHandler<T extends any[]>(viewPort: T, cellPropertyContext: IPropertyContext, internalState: FoundsetViewportState) {
        const softProxyRevoker = new SoftProxyRevoker(internalState.log);
        internalState.viewportLevelProxyRevokerFunc = softProxyRevoker.getRevokeFunction(); // in the end, if we never call the revoke function we could even use here a boolean instead

        const changeHandlerForWholeViewportProxy: SubpropertyChangeByReferenceHandler = new SubpropertyChangeByReferenceHandler({

            shouldIgnoreChangesBecauseFromOrToServerIsInProgress: () => internalState.ignoreChanges,

            changeNeedsToBePushedToServer: (viewportIndex: number, oldValue: any, doNotPushNow?: boolean) => {
                this.reallyQueueChange(viewPort, internalState, undefined, viewportIndex, undefined, cellPropertyContext,
                    viewPort[viewportIndex], oldValue, doNotPushNow);
            }

        });

        return {
            set: (underlyingViewport: T, prop: any, v: any, receiver: any) => {
                if (softProxyRevoker.isProxyDisabled() || internalState.ignoreChanges) return Reflect.set(underlyingViewport, prop, v, receiver);

                // eslint-disable-next-line radix
                const i = Number.parseInt(prop);
                if (Number.isInteger(i)) {
                    if (i < underlyingViewport.length) {
                        changeHandlerForWholeViewportProxy.setPropertyAndHandleChanges(underlyingViewport, i, v); // 1 element has changed by ref
                        return true;
                    }
                }

                return Reflect.set(underlyingViewport, prop, v, receiver);
            },

            deleteProperty: (underlyingViewport: T, prop: any) => {
                if (softProxyRevoker.isProxyDisabled() || internalState.ignoreChanges) return Reflect.deleteProperty(underlyingViewport, prop);

                // eslint-disable-next-line radix
                const i = Number.parseInt(prop);
                if (Number.isInteger(i) && i < underlyingViewport.length) {
                    changeHandlerForWholeViewportProxy.setPropertyAndHandleChanges(underlyingViewport, i, undefined); // 1 element deleted
                    return true;
                }

                return Reflect.deleteProperty(underlyingViewport, prop);
            }
        };
    }

    /**
     * Handler for the Proxy object that will detect reference changes in a row of the viewport - when it is needed.
     * This implements the shallow PushToServer behavior for multiple cells on one row (foundset viewport/component viewport).
     */
    private getRowLevelProxyHandler(viewPort: any[], initialRowIndex: number, propertyContextCreator: IPropertyContextCreatorForRow, internalState: FoundsetViewportState) {
        const proxyState = internalState.rowLevelProxyStates[initialRowIndex] = new RowProxyState(new SoftProxyRevoker(internalState.log), initialRowIndex);

        const changeHandlerForRowProxy: SubpropertyChangeByReferenceHandler = new SubpropertyChangeByReferenceHandler({

            shouldIgnoreChangesBecauseFromOrToServerIsInProgress: () => internalState.ignoreChanges,

            changeNeedsToBePushedToServer: (prop: string, oldValue: any, doNotPushNow?: boolean) => {
                this.reallyQueueChange(viewPort, internalState, undefined, proxyState.rowIndex, prop,
                    this.getCellPropertyContextFor(propertyContextCreator, viewPort[proxyState.rowIndex], prop), viewPort[proxyState.rowIndex][prop], oldValue, doNotPushNow);
            }

        });

        return {
            set: (row: any, prop: any, v: any, receiver: any) => {
                if (proxyState.softProxyRevoker.isProxyDisabled() || internalState.ignoreChanges) return Reflect.set(row, prop, v, receiver);

                if (instanceOfISomePropertiesInRowAreNotFoundsetLinked(internalState) && !internalState.isFoundsetLinkedProperty(prop))
                    return Reflect.set(row, prop, v, receiver);   // the viewport proxy doesn't want to intercept changes on shared part (that is done elsewhere)
                // of the component's model - in case this is a viewport for a component property type

                if (this.getCellPropertyContextFor(propertyContextCreator, viewPort[proxyState.rowIndex], prop).getPushToServerCalculatedValue() > PushToServerEnum.ALLOW) {
                    changeHandlerForRowProxy.setPropertyAndHandleChanges(row, prop, v); // 1 element has changed by ref
                    return true;
                } else return Reflect.set(row, prop, v);
            },

            deleteProperty: (row: any, prop: any) => {
                if (proxyState.softProxyRevoker.isProxyDisabled() || internalState.ignoreChanges) return Reflect.deleteProperty(row, prop);

                if (instanceOfISomePropertiesInRowAreNotFoundsetLinked(internalState) && !internalState.isFoundsetLinkedProperty(prop))
                    return Reflect.deleteProperty(row, prop);   // the viewport proxy doesn't want to intercept changes on shared part (that is done elsewhere)
                // of the component's model - in case this is a viewport for a component property type

                if (this.getCellPropertyContextFor(propertyContextCreator, viewPort[proxyState.rowIndex], prop).getPushToServerCalculatedValue() > PushToServerEnum.ALLOW) {
                    changeHandlerForRowProxy.setPropertyAndHandleChanges(row, prop, undefined); // 1 element deleted
                    return true;
                } else return Reflect.deleteProperty(row, prop);
            }
        };
    }

}

export abstract class FoundsetViewportState extends ChangeAwareState {

    changeListeners: ViewportChangeListener[] = [];

    viewportTypes: ExpandedViewportTypes;
    unwatchData: { [idx: number]: Array<() => void> };

    requests: Array<any> = [];

    ignoreChanges = true;

    viewportLevelProxyRevokerFunc?: () => void; // this can be just a boolean I think; we never call it, we just check if it's there or not
    rowLevelProxyStates?: Array<RowProxyState>;
    /** just a value that is cached here for foundset type viewports - so we don't need to search for it each time it's needed */
    needsRowProxies: boolean;

    constructor(public readonly forFoundset: () => IFoundset, public readonly log: LoggerService, protected sabloService: SabloService) {
        super();
    }

    public hasChanges(): boolean {
        return super.hasChanges() || this.requests.length > 0;
    }

    public clearChanges(): void {
        super.clearChanges();
        this.requests = [];
    }

    public addChangeListener(listener: ViewportChangeListener): () => void {
        this.changeListeners.push(listener);
        return () => this.removeChangeListener(listener);
    }

    public removeChangeListener(listener: ViewportChangeListener) {
        const index = this.changeListeners.indexOf(listener);
        if (index > -1) {
            this.changeListeners.splice(index, 1);
        }
    }

    public fireChanges(changes: ViewportChangeEvent) {
        this.sabloService.addIncomingMessageHandlingDoneTask(() => {
            for (const cl of this.changeListeners) {
                cl(changes);
            }
        });
    }

}

class RowProxyState {
    
    public constructor(public readonly softProxyRevoker: SoftProxyRevoker, public rowIndex: number) {}

}

interface CellChangeToServer {
    _svyRowId: string;
    dp?: string;
    value: string;
}

export type CellUpdatedFromServerListener = (relativeRowIndex: number, columnName: string, oldValue: any, newValue: any) => void;

export interface IPropertyContextCreatorForRow {
    withRowValueAndPushToServerFor(rowValue: any, propertyName: string): IPropertyContext;
}

export interface ISomePropertiesInRowAreNotFoundsetLinked {
    isFoundsetLinkedProperty(propertyName: string): boolean;
}

const instanceOfISomePropertiesInRowAreNotFoundsetLinked = (obj: any): obj is ISomePropertiesInRowAreNotFoundsetLinked =>
    obj != null && obj.isFoundsetLinkedProperty instanceof Function;
