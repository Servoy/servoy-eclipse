import { Injectable } from '@angular/core';
import { ChangeAwareState, ChangeType, ColumnRef, isChanged, IFoundset } from '@servoy/public';
import { ConverterService, PropertyContext } from '../../sablo/converter.service';
import { SabloUtils } from '../../sablo/websocket.service';
import { instanceOfChangeAwareValue, IChangeAwareValue } from '@servoy/public';


@Injectable({
  providedIn: 'root'
})
export class ViewportService {
    public static readonly ROW_ID_COL_KEY = '_svyRowId';
    private static readonly ROW_ID_COL_KEY_PARTIAL_UPDATE = '_svyRowId_p'; // same as $foundsetTypeConstants.ROW_ID_COL_KEY but sent when the foundset
    // property is sending just a partial update of it's columns, and some of the columns that did change are also pks - so they do affect the pk hash/svyRowId;
    // it is used to differentiate between the two situations in an if below

    constructor(private converterService: ConverterService) { }

    public updateWholeViewport(viewPort: any[], internalState: FoundsetViewportState, viewPortUpdate, viewPortUpdateConversions,
        propertyContext: PropertyContext, simpleRowValue: boolean): any[] {

        // clear change notifiers for old smart values in viewport
        for (let i = viewPort.length - 1; i >= 0; i--) {
            this.updateChangeAwareNotifiersForRow(i, viewPort, internalState, simpleRowValue, true);
        }

        if (viewPortUpdateConversions) {
            // do the actual conversion
            viewPortUpdate = this.converterService.convertFromServerToClient(viewPortUpdate, viewPortUpdateConversions, viewPort, propertyContext);
        }
        // update conversion info
        this.updateAllConversionInfo(viewPortUpdate, internalState, viewPortUpdateConversions);

        // link new smart viewport values to parent change notifier so that things such as let's say child valuelist.filter(...) do get sent to server correctly
        for (let i = viewPortUpdate.length - 1; i >= 0; i--) {
            this.updateChangeAwareNotifiersForRow(i, viewPortUpdate, internalState, simpleRowValue, false);
        }

        return viewPortUpdate;
    }

    public updateAllConversionInfo(viewPort: any[], internalState: FoundsetViewportState, serverConversionInfo) {
        internalState.viewportConversions = [];
        for (let i = viewPort.length - 1; i >= 0; i--) {
            internalState.viewportConversions[i] = serverConversionInfo ? serverConversionInfo[i] : undefined;
        }
    }

    public updateViewportGranularly(viewPort: any[], internalState: FoundsetViewportState, rowUpdates, rowUpdateConversions,
        propertyContext: PropertyContext, simpleRowValue: boolean, rowPrototype?: any) {
        // partial row updates (remove/insert/update)

        // {
        //   "rows": rowData, // array again
        //   "startIndex": ...,
        //   "endIndex": ...,
        //   "type": ... // ONE OF CHANGE = ChangeType.ROWS_CHANGED;
        //                         INSERT = ChangeType.ROWS_INSERTED;
        //                         DELETE = ChangeType.ROWS_DELETED;
        // }

        // apply them one by one
        for (let i = 0; i < rowUpdates.length; i++) {
            let rowUpdate = rowUpdates[i];
            if (rowUpdate.type === ChangeType.ROWS_CHANGED) {
                for (let j = rowUpdate.startIndex; j <= rowUpdate.endIndex; j++) {
                    let dpName: string;
                    const relIdx = j - rowUpdate.startIndex;

                    // clear notifiers for any old smart values
                    this.updateChangeAwareNotifiersForRow(j, viewPort, internalState, simpleRowValue, true);

                    // apply the conversions
                    const rowConversionUpdate = (rowUpdateConversions && rowUpdateConversions[i] && rowUpdateConversions[i].rows) ? rowUpdateConversions[i].rows[relIdx] : undefined;
                    if (rowConversionUpdate) rowUpdate.rows[relIdx] = this.converterService.convertFromServerToClient(rowUpdate.rows[relIdx], rowConversionUpdate, viewPort[j], propertyContext);
                    // if the rowUpdate contains '_svyRowId' then we know it's the entire/complete row object
                    if (simpleRowValue || rowUpdate.rows[relIdx][ViewportService.ROW_ID_COL_KEY]) {
                        // Because of this ViewportService.ROW_ID_COL_KEY check the ViewportService.ROW_ID_COL_KEY_PARTIAL_UPDATE constant also exists,
                        // that actually updates the same thing (_svyRowId) but is not a full row update and goes into "else" for a partial row (with multiple columns) update;
                        // So if we reach this code due to || rowUpdate.rows[relIdx][ViewportService.ROW_ID_COL_KEY] (so it's not a
                        // simpleRowValue) that means server sent a full row update for the foundset
                        // property type with all columns in it... and we can assign the full value from server to the row as well
                        viewPort[j] = rowUpdate.rows[relIdx];
                        if (rowPrototype) viewPort[j] = SabloUtils.cloneWithDifferentPrototype(viewPort[j], rowPrototype);

                        if (rowConversionUpdate) {
                            // update conversion info
                            if (internalState.viewportConversions === undefined) {
                                internalState.viewportConversions = [];
                            }
                            internalState.viewportConversions[j] = rowConversionUpdate;
                        } else if (internalState.viewportConversions !== undefined && internalState.viewportConversions[j] !== undefined)
                            delete internalState.viewportConversions[j];
                    } else {
                        // key/value pairs in each row
                        // this might be a partial update (so only a column changed for example) - don't drop all other columns, just update the ones we received
                        for (dpName of Object.keys(rowUpdate.rows[relIdx])) {
                            // update value
                            viewPort[j][dpName === ViewportService.ROW_ID_COL_KEY_PARTIAL_UPDATE ? ViewportService.ROW_ID_COL_KEY : dpName] = rowUpdate.rows[relIdx][dpName];

                            if (rowConversionUpdate) {
                                // update conversion info
                                if (internalState.viewportConversions === undefined) {
                                    internalState.viewportConversions = [];
                                }
                                if (internalState.viewportConversions[j] === undefined) {
                                    internalState.viewportConversions[j] = {};
                                }
                                internalState.viewportConversions[j][dpName] = rowConversionUpdate[dpName];
                            } else if (internalState.viewportConversions !== undefined && internalState.viewportConversions[j] !== undefined
                                && internalState.viewportConversions[j][dpName] !== undefined) delete internalState.viewportConversions[j][dpName];
                        }
                    }

                    // add back notifiers for any new smart values
                    this.updateChangeAwareNotifiersForRow(j, viewPort, internalState, simpleRowValue, false);
                }
            } else if (rowUpdate.type === ChangeType.ROWS_INSERTED) {
                // apply conversions
                if (rowUpdateConversions && rowUpdateConversions[i]) rowUpdate = this.converterService.convertFromServerToClient(rowUpdate, rowUpdateConversions[i], undefined, propertyContext);

                if (internalState.viewportConversions === undefined) {
                    internalState.viewportConversions = [];
                }

                for (let j = rowUpdate.rows.length - 1; j >= 0; j--) {
                    viewPort.splice(rowUpdate.startIndex, 0, rowUpdate.rows[j]);
                    if (rowPrototype) rowUpdate.rows[j] = SabloUtils.cloneWithDifferentPrototype(rowUpdate.rows[j], rowPrototype);

                    if (internalState.viewportConversions.length > rowUpdate.startIndex) {
                        // insert conversion at the right location and shift old ones after insert index
                        internalState.viewportConversions.splice(rowUpdate.startIndex, 0,
                            rowUpdateConversions && rowUpdateConversions[i] && rowUpdateConversions[i].rows ? rowUpdateConversions[i].rows[j] : undefined);
                    } else if (rowUpdateConversions && rowUpdateConversions[i] && rowUpdateConversions[i].rows && rowUpdateConversions[i].rows[j]) {
                        internalState.viewportConversions[rowUpdate.startIndex] = rowUpdateConversions[i].rows[j];
                    }
                }
                rowUpdate.endIndex = rowUpdate.startIndex + rowUpdate.rows.length - 1; // prepare rowUpdate.endIndex for listener notifications

                // add change notifiers for inserted rows if needed and update the ones that were shifted right after insert
                for (let j = rowUpdate.startIndex; j < viewPort.length; j++) {
                    this.updateChangeAwareNotifiersForRow(j, viewPort, internalState, simpleRowValue, false);
                }
            } else if (rowUpdate.type === ChangeType.ROWS_DELETED) {
                const numberOfDeletedRows = rowUpdate.endIndex - rowUpdate.startIndex + 1;
                if (internalState.viewportConversions) {
                    // delete conversion info for deleted rows
                    if (internalState.viewportConversions.length > rowUpdate.startIndex) {
                        // delete conversion info for deleted rows and shift left what is after deletion
                        internalState.viewportConversions.splice(rowUpdate.startIndex, Math.min(numberOfDeletedRows, internalState.viewportConversions.length - rowUpdate.startIndex));
                    }
                }

                // remove any change notifiers for the rows that were deleted
                for (let j = rowUpdate.startIndex; j <= rowUpdate.endIndex; j++) {
                    this.updateChangeAwareNotifiersForRow(j, viewPort, internalState, simpleRowValue, true);
                }

                viewPort.splice(rowUpdate.startIndex, numberOfDeletedRows);

                // update any change notifiers for the rows that were shifted left after delete
                for (let j = rowUpdate.startIndex; j < viewPort.length; j++) {
                    this.updateChangeAwareNotifiersForRow(j, viewPort, internalState, simpleRowValue, false);
                }
            }
            delete rowUpdate.rows; // prepare rowUpdate for listener notifications
        }
    }

    public queueChange(viewPort: any[], internalState: FoundsetViewportState, deep: boolean, idx: number, columnName: string, newValue: any, oldValue?: any) {
        if (columnName === ViewportService.ROW_ID_COL_KEY) return;

        const conversionInfo = internalState.viewportConversions ? internalState.viewportConversions[idx] : undefined;
        if (deep && !this.isChanged(newValue, oldValue, columnName, conversionInfo)) return;

        // if it doesn't have internalState.forFoundset then it's probably the foundset property's viewport directly which has those in the viewport
        const r: ColumnRef = {
            _svyRowId: (internalState.forFoundset !== undefined ? internalState.forFoundset().viewPort.rows[idx]._svyRowId : viewPort[idx]._svyRowId),
            dp: columnName, value: newValue
        };

        // convert new data if necessary
        if (conversionInfo && (!columnName || conversionInfo[columnName])) {
            r.value = this.converterService.convertFromClientToServer(r.value, columnName ? conversionInfo[columnName] : conversionInfo, oldValue);
        } else {
            r.value = this.converterService.convertClientObject(r.value);
        }

        internalState.requests.push({ viewportDataChanged: r });
        internalState.notifyChangeListener();
    }

    private updateChangeAwareNotifiersForRow(rowIdx: number, viewPort: any[], internalState: FoundsetViewportState, simpleRowValue: boolean, clearNotifier: boolean) {
        if (simpleRowValue) {
            this.updateChangeAwareNotifiersForCell(viewPort[rowIdx], viewPort, internalState, rowIdx, undefined, clearNotifier);
        } else {
            for (const columnName in viewPort[rowIdx]) {
                if (columnName !== ViewportService.ROW_ID_COL_KEY) this.updateChangeAwareNotifiersForCell(viewPort[rowIdx][columnName], viewPort, internalState, rowIdx, columnName, clearNotifier);
            }
        }
    }

    private updateChangeAwareNotifiersForCell(cellValue: any, viewPort: any[], internalState: FoundsetViewportState, rowIdx: number, columnName: string, clearNotifier: boolean) {
        if (instanceOfChangeAwareValue(cellValue)) {
            // child is able to handle it's own change mechanism
            cellValue.getStateHolder().setChangeListener(clearNotifier ? undefined : () => {
                this.queueChange(viewPort, internalState, false, rowIdx, columnName, cellValue, cellValue);
            });
        }
    }

    private isChanged(newValue: any, oldValue: any, columnName: string, conversionInfo: any) {
        if (newValue !== oldValue) {
            /* this doesn't seem to work correctly for 2 identical Date objects in Chrome when debugging; but it should */
            if (typeof newValue === 'object') {
                return isChanged(newValue, oldValue, conversionInfo ? (columnName ? conversionInfo[columnName] : conversionInfo) : undefined);
            }
            return true;
        }
        return false;
    }
}

export class FoundsetViewportState extends ChangeAwareState {
  viewportConversions: Record<string, any>[] = [];
  forFoundset: () => IFoundset;
  requests = [];
}
