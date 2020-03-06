import { Injectable } from '@angular/core';
import { ConverterService } from '../../sablo/converter.service';
import { SabloUtils } from '../../sablo/websocket.service';
import { ViewportRowUpdates, FoundsetTypeConstants, ViewPort } from '../../sablo/spectypes.service';


@Injectable()
export class ViewportService {
    
    private static readonly CONVERSIONS = "viewportConversions"; // data conversion info   
    private static readonly CHANGED_IN_LINKED_PROPERTY = 9;
    
    private static readonly DATAPROVIDER_KEY = "dp";
    private static readonly VALUE_KEY = "value";
    
    constructor( private converterService: ConverterService ) {}
    
    public updateWholeViewport(viewPortHolder: ViewPort, viewPortPropertyName: string, internalState: any, viewPortUpdate, viewPortUpdateConversions, propertyContext:(propertyName: string)=>any) {
        if (viewPortUpdateConversions) {
            // do the actual conversion
            viewPortUpdate = this.converterService.convertFromServerToClient(viewPortUpdate, viewPortUpdateConversions, viewPortHolder[viewPortPropertyName], propertyContext);
        }
        viewPortHolder[viewPortPropertyName] = viewPortUpdate;
        // update conversion info
        this.updateAllConversionInfo(viewPortHolder[viewPortPropertyName], internalState, viewPortUpdateConversions);
    }
    
    public updateAllConversionInfo(viewPort, internalState, serverConversionInfo) {
        internalState[ViewportService.CONVERSIONS] = {};
        var i;
        for (i = viewPort.length - 1; i >= 0; i--)
            this.updateRowConversionInfo(i, internalState, serverConversionInfo ? serverConversionInfo[i] : undefined);
    }
    
    public updateViewportGranularly(viewPort: any[], internalState, rowUpdates, rowUpdateConversions,
            propertyContext:(propertyName: string)=>any, simpleRowValue/*not key/value pairs in each row*/, rowPrototype?:any) {
        // partial row updates (remove/insert/update)

        // {
        //   "rows": rowData, // array again
        //   "startIndex": ...,
        //   "endIndex": ...,
        //   "type": ... // ONE OF CHANGE = 0; INSERT = 1; DELETE = 2;
        // }

        // apply them one by one
        let i : number;
        let j : number;
        for (i = 0; i < rowUpdates.length; i++) {
            let rowUpdate = rowUpdates[i];
            if (rowUpdate.type == FoundsetTypeConstants.ROWS_CHANGED) {
                for (j = rowUpdate.startIndex; j <= rowUpdate.endIndex; j++) {
                    // rows[j] = rowUpdate.rows[j - rowUpdate.startIndex];
                    // because of a bug in ngGrid that doesn't detect array item changes if array length doesn't change
                    // we will reuse the existing row object as a workaround for updating (a case was filed for that bug as it's breaking scenarios with
                    // delete and insert as well)

                    let dpName : string;
                    let relIdx = j - rowUpdate.startIndex;

                    // apply the conversions
                    let rowConversionUpdate = (rowUpdateConversions && rowUpdateConversions[i] && rowUpdateConversions[i].rows) ? rowUpdateConversions[i].rows[relIdx] : undefined;
                    if (rowConversionUpdate) rowUpdate.rows[relIdx] = this.converterService.convertFromServerToClient(rowUpdate.rows[relIdx], rowConversionUpdate, viewPort[j], propertyContext);
                    // if the rowUpdate contains '_svyRowId' then we know it's the entire/complete row object
                    if (simpleRowValue || rowUpdate.rows[relIdx][FoundsetTypeConstants.ROW_ID_COL_KEY]) {
                        viewPort[j] = rowUpdate.rows[relIdx];
                        if (rowPrototype) viewPort[j] = SabloUtils.cloneWithDifferentPrototype(viewPort[j], rowPrototype);

                        if (rowConversionUpdate) {
                            // update conversion info
                            if (internalState[ViewportService.CONVERSIONS] === undefined) {
                                internalState[ViewportService.CONVERSIONS] = {};
                            }
                            internalState[ViewportService.CONVERSIONS][j] = rowConversionUpdate;
                        } else if (internalState[ViewportService.CONVERSIONS] !== undefined && internalState[ViewportService.CONVERSIONS][j] !== undefined)
                            delete internalState[ViewportService.CONVERSIONS][j];
                    } else {
                        // key/value pairs in each row
                        // this might be a partial update (so only a column changed for example) - don't drop all other columns, just update the ones we received
                        for (dpName in rowUpdate.rows[relIdx]) {
                            // update value
                            viewPort[j][dpName] = rowUpdate.rows[relIdx][dpName];

                            if (rowConversionUpdate) {
                                // update conversion info
                                if (internalState[ViewportService.CONVERSIONS] === undefined) {
                                    internalState[ViewportService.CONVERSIONS] = {};
                                }
                                if (internalState[ViewportService.CONVERSIONS][j] == undefined)
                                {
                                    internalState[ViewportService.CONVERSIONS][j] = {};
                                }
                                internalState[ViewportService.CONVERSIONS][j][dpName] = rowConversionUpdate[dpName];
                            } else if (internalState[ViewportService.CONVERSIONS] !== undefined && internalState[ViewportService.CONVERSIONS][j] !== undefined
                                    && internalState[ViewportService.CONVERSIONS][j][dpName] !== undefined) delete internalState[ViewportService.CONVERSIONS][j][dpName];
                        }
                    }
                }
            } else if (rowUpdate.type == FoundsetTypeConstants.ROWS_INSERTED) {
                if (rowUpdateConversions && rowUpdateConversions[i]) this.converterService.convertFromServerToClient(rowUpdate, rowUpdateConversions[i], undefined, propertyContext);

                for (j = rowUpdate.rows.length - 1; j >= 0 ; j--) {
                    viewPort.splice(rowUpdate.startIndex, 0, rowUpdate.rows[j]);
                    if (rowPrototype) rowUpdate.rows[j] = SabloUtils.cloneWithDifferentPrototype(rowUpdate.rows[j], rowPrototype);
                    this.updateRowConversionInfo(rowUpdate.startIndex+j, internalState, (rowUpdateConversions && rowUpdateConversions[i] && rowUpdateConversions[i].rows) ? rowUpdateConversions[i].rows[j] : undefined);
                }
                rowUpdate.removedFromVPEnd = 0; // prepare rowUpdate for listener notifications; starting with Servoy 8.4 'removedFromVPEnd' is deprecated and always 0 as server-side code will add a separate delete operation as necessary
                rowUpdate.endIndex = rowUpdate.startIndex + rowUpdate.rows.length - 1; // prepare rowUpdate.endIndex for listener notifications
            } else if (rowUpdate.type == FoundsetTypeConstants.ROWS_DELETED) {
                if (rowUpdateConversions && rowUpdateConversions[i]) this.converterService.convertFromServerToClient(rowUpdate, rowUpdateConversions[i], undefined, propertyContext);

                var oldLength = viewPort.length;
                if (internalState[ViewportService.CONVERSIONS]) {
                    // delete conversion info for deleted rows
                    for (j = rowUpdate.startIndex; j < oldLength; j++)
                    {
                        if (j+(rowUpdate.endIndex - rowUpdate.startIndex) <  oldLength)
                        {
                            internalState[ViewportService.CONVERSIONS][j] = internalState[ViewportService.CONVERSIONS][j+(rowUpdate.endIndex - rowUpdate.startIndex)]
                        }
                        else
                        {
                            delete internalState[ViewportService.CONVERSIONS][j];
                        }
                    }   
                }
                viewPort.splice(rowUpdate.startIndex, rowUpdate.endIndex - rowUpdate.startIndex + 1);
                
                rowUpdate.appendedToVPEnd = 0; // prepare rowUpdate for listener notifications; starting with Servoy 8.4 'appendedToVPEnd' is deprecated and always 0 as server-side code will add a separate insert operation as necessary
            } else if (rowUpdate.type == ViewportService.CHANGED_IN_LINKED_PROPERTY) {
                // just prepare it for the foundset change listener; components will want to handle this type of change as well so we should notify them when it happens
                rowUpdate.type = FoundsetTypeConstants.ROWS_CHANGED;
            }
            delete rowUpdate.rows; // prepare rowUpdate for listener notifications
        }
    }
    
    public updateRowConversionInfo(idx:number, internalState, serverConversionInfo) {
        if (internalState[ViewportService.CONVERSIONS] === undefined) {
            internalState[ViewportService.CONVERSIONS] = {};
        }
        internalState[ViewportService.CONVERSIONS][idx] = serverConversionInfo;
    }

    public queueChange(viewPort: any[], internalState: any, deep:boolean, idx: number, columnName: string, newValue: any, oldValue?:any) {
        if (columnName === FoundsetTypeConstants.ROW_ID_COL_KEY) return;
        
        let conversionInfo = internalState[ViewportService.CONVERSIONS] ? internalState[ViewportService.CONVERSIONS][idx] : undefined;
        if ( deep && !this.isChanged(newValue, oldValue, columnName, conversionInfo)) return;

        let r = {};
		if (internalState.forFoundset !== undefined) {
			r[FoundsetTypeConstants.ROW_ID_COL_KEY] = internalState.forFoundset().viewPort.rows[idx][FoundsetTypeConstants.ROW_ID_COL_KEY];
		} else {
            // if it doesn't have internalState.forFoundset then it's probably the foundset property's viewport directly which has those in the viewport
            r[FoundsetTypeConstants.ROW_ID_COL_KEY] = viewPort[idx][FoundsetTypeConstants.ROW_ID_COL_KEY];         
        }
        r[ViewportService.DATAPROVIDER_KEY] = columnName;
		r[ViewportService.VALUE_KEY] = newValue;

		// convert new data if necessary
        if (conversionInfo && (!columnName || conversionInfo[columnName])){
            r[ViewportService.VALUE_KEY] = this.converterService.convertFromClientToServer(r[ViewportService.VALUE_KEY], columnName ? conversionInfo[columnName] : conversionInfo, oldValue);
        }
        else {
            r[ViewportService.VALUE_KEY] = this.converterService.convertClientObject(r[ViewportService.VALUE_KEY]);
        }

		internalState.requests.push({viewportDataChanged: r});
		if (internalState.changeNotifier) internalState.changeNotifier();
    }

    private isChanged(newValue: any, oldValue: any, columnName:string, conversionInfo: any) {
        if (newValue !== oldValue) {
             /* this doesn't seem to work correctly for 2 identical Date objects in Chrome when debugging; but it should */
			if (typeof newValue == "object") {
				return ConverterService.isChanged(newValue, oldValue, conversionInfo ? (columnName ? conversionInfo[columnName] : conversionInfo) : undefined);
            }
            return true;
        }
        return false;
    }
}