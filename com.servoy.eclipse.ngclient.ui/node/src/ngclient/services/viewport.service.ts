import { Injectable } from '@angular/core';
import { ChangeAwareState, ChangeType, ColumnRef, isChanged, IFoundset } from '@servoy/public';
import { ConverterService, PropertyContext } from '../../sablo/converter.service';
import { SabloUtils } from '../../sablo/websocket.service';

@Injectable({
  providedIn: 'root'
})
export class ViewportService {
  public static readonly ROW_ID_COL_KEY = '_svyRowId';

  private static readonly CHANGED_IN_LINKED_PROPERTY = 9;

  constructor(private converterService: ConverterService) { }

  public updateWholeViewport(viewPort: any[], internalState: FoundsetViewportState, viewPortUpdate, viewPortUpdateConversions, propertyContext: PropertyContext): any[] {
    if (viewPortUpdateConversions) {
      // do the actual conversion
      // TODO that "null" should be a changehandler, where do we get that here?
      viewPortUpdate = this.converterService.convertFromServerToClient(viewPortUpdate, viewPortUpdateConversions, viewPort, propertyContext);
    }
    // update conversion info
    this.updateAllConversionInfo(viewPortUpdate, internalState, viewPortUpdateConversions);
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
      const rowUpdate = rowUpdates[i];
      if (rowUpdate.type === ChangeType.ROWS_CHANGED) {
        for (let j = rowUpdate.startIndex; j <= rowUpdate.endIndex; j++) {
          let dpName: string;
          const relIdx = j - rowUpdate.startIndex;

          // apply the conversions
          const rowConversionUpdate = (rowUpdateConversions && rowUpdateConversions[i] && rowUpdateConversions[i].rows) ? rowUpdateConversions[i].rows[relIdx] : undefined;
          // TODO that "null" should be a changehandler, where do we get that here?
          if (rowConversionUpdate) rowUpdate.rows[relIdx] = this.converterService.convertFromServerToClient(rowUpdate.rows[relIdx], rowConversionUpdate, viewPort[j], propertyContext);
          // if the rowUpdate contains '_svyRowId' then we know it's the entire/complete row object
          if (simpleRowValue || rowUpdate.rows[relIdx]._svyRowId) {
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
            // eslint-disable-next-line guard-for-in
            for (dpName in rowUpdate.rows[relIdx]) {
              // update value
              viewPort[j][dpName] = rowUpdate.rows[relIdx][dpName];

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
        }
      } else if (rowUpdate.type === ChangeType.ROWS_INSERTED) {
        if (rowUpdateConversions && rowUpdateConversions[i])
            this.converterService.convertFromServerToClient(rowUpdate, rowUpdateConversions[i], null,propertyContext);
        if (internalState.viewportConversions === undefined) {
          internalState.viewportConversions = [];
        }

        for (let j = rowUpdate.rows.length - 1; j >= 0; j--) {
          viewPort.splice(rowUpdate.startIndex, 0, rowUpdate.rows[j]);
          if (rowPrototype) rowUpdate.rows[j] = SabloUtils.cloneWithDifferentPrototype(rowUpdate.rows[j], rowPrototype);
          if (rowUpdateConversions && rowUpdateConversions[i]) {
            internalState.viewportConversions[rowUpdate.startIndex + j] = (rowUpdateConversions && rowUpdateConversions[i] && rowUpdateConversions[i].rows) ?
             rowUpdateConversions[i].rows[j] : undefined;
          }
        }
        rowUpdate.endIndex = rowUpdate.startIndex + rowUpdate.rows.length - 1; // prepare rowUpdate.endIndex for listener notifications
      } else if (rowUpdate.type === ChangeType.ROWS_DELETED) {
        if (rowUpdateConversions && rowUpdateConversions[i])
            this.converterService.convertFromServerToClient(rowUpdate, rowUpdateConversions[i], null, propertyContext);

        const oldLength = viewPort.length;
        if (internalState.viewportConversions) {
          // delete conversion info for deleted rows
          for (let j = rowUpdate.startIndex; j < oldLength; j++) {
            if (j + (rowUpdate.endIndex - rowUpdate.startIndex) < oldLength) {
              internalState.viewportConversions[j] = internalState.viewportConversions[j + (rowUpdate.endIndex - rowUpdate.startIndex)];
            } else {
              delete internalState.viewportConversions[j];
            }
          }
        }
        viewPort.splice(rowUpdate.startIndex, rowUpdate.endIndex - rowUpdate.startIndex + 1);
      } else if (rowUpdate.type === ViewportService.CHANGED_IN_LINKED_PROPERTY) {
        // just prepare it for the foundset change listener; components will want to handle this type of change as well so we should notify them when it happens
        rowUpdate.type = ChangeType.ROWS_CHANGED;
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
