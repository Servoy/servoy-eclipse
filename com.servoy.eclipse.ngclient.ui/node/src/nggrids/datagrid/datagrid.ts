import { AgGridAngular } from '@ag-grid-community/angular';
import { GridOptions } from '@ag-grid-enterprise/all-modules';
import { ChangeDetectorRef, ContentChild, Input, TemplateRef } from '@angular/core';
import { Component, ViewChild } from '@angular/core';
import { FoundsetChangeEvent } from '../../ngclient/converters/foundset_converter';
import { ViewportService } from '../../ngclient/services/viewport.service';
import { FormattingService, ServoyApi } from '../../ngclient/servoy_public';
import { LoggerFactory, LoggerService } from '../../sablo/logger.service';
import { IFoundset } from '../../sablo/spectypes.service';
import { Deferred } from '../../sablo/util/deferred';
import { DatePicker } from './editors/datepicker';
import { FormEditor } from './editors/formeditor';
import { SelectEditor } from './editors/selecteditor';
import { TextEditor } from './editors/texteditor';
import { TypeaheadEditor } from './editors/typeaheadeditor';

const COLUMN_PROPERTIES_DEFAULTS = {
    headerTitle: { colDefProperty: 'headerName', default: null },
    headerTooltip: { colDefProperty: 'headerTooltip', default: null },
    id: { colDefProperty: 'colId', default: null },
    styleClassDataprovider: { colDefProperty: 'cellClass', default: null },
    styleClass: { colDefProperty: 'cellClass', default: null },
    rowGroupIndex: { colDefProperty: 'rowGroupIndex', default: -1 },
    width: { colDefProperty: 'width', default: 0 },
    enableToolPanel: { colDefProperty: 'suppressToolPanel', default: true },
    maxWidth: { colDefProperty: 'maxWidth', default: null },
    minWidth: { colDefProperty: 'minWidth', default: null },
    visible: { colDefProperty: 'hide', default: true },
    enableResize: { colDefProperty: 'resizable', default: true },
    autoResize: { colDefProperty: 'suppressSizeToFit', default: true },
    enableSort: { colDefProperty: 'sortable', default: true }
};


const CHUNK_SIZE = 50;
const CACHED_CHUNK_BLOCKS = 2;

const NULL_VALUE = {displayValue: '', realValue: null};

@Component({
    selector: 'aggrid-groupingtable',
    templateUrl: './datagrid.html',
    styleUrls: ['./datagrid.css']
})
export class DataGrid {

    @ContentChild( TemplateRef  , {static: true})
    templateRef: TemplateRef<any>;

    @ViewChild('agGrid') agGrid: AgGridAngular;

    @Input() myFoundset: IFoundset;
    @Input() columns;
    @Input() readOnly;
    @Input() readOnlyColumnIds;
    @Input() hashedFoundsets;
    @Input() filterModel;
    @Input() rowStyleClassDataprovider;
    @Input() arrowsUpDownMoveWhenEditing;
    @Input() _internalExpandedState;
    @Input() _internalFormEditorValue;
    @Input() enableSorting;
    @Input() enableColumnResize;
    @Input() visible;


    @Input() servoyApi: ServoyApi;


    @Input() onCellClick;
    @Input() onCellDoubleClick;
    @Input() onCellRightClick;
    @Input() onColumnDataChange;
    @Input() onColumnStateChanged;
    @Input() onFooterClick;
    @Input() onReady;
    @Input() onRowGroupOpened;
    @Input() onSelectedRowsChanged;
    @Input() onSort;
    @Input() onColumnFormEditStarted;

    log: LoggerService;
    gridOptions: GridOptions;
    foundset: FoundsetManager;
    groupManager: GroupManager;

    // when the grid is not ready yet set the value to the foundset/column index for which has been edit cell called
    startEditFoundsetIndex = -1;
    startEditColumnIndex = -1;

    /**
     * Store the state of the table. TODO to be persisted
     * */
    state: State = new State();

    dirtyCache: boolean;
    // used in HTML template to toggle sync button
    isGroupView = false;

    // set to true once the grid is rendered and the selection is set
    isSelectionReady = false;

    // set to true during data request from ag grid, from request-start until all data is loaded
    isDataLoading = false;

    scrollToSelectionWhenSelectionReady = false;

    // set to true, if columns needs to be fit after rows are rendered - set to true when purge is called (all rows are rendered)
    columnsToFitAfterRowsRendered = false;

    // flag used to set removing all foundset just before getting data tp display; it is set when doing sort while grouped
    removeAllFoundsetRef = false;

    // foundset sort promise
    sortPromise;
    sortHandlerPromises = new Array();


    // if row autoHeight, we need to do a refresh after first time data are displayed, to allow ag grid to re-calculate the heights
    isRefreshNeededForAutoHeight = false;

    selectionEvent;

    constructor(logFactory: LoggerFactory, public cdRef: ChangeDetectorRef, public formattingService: FormattingService) {
        this.log = logFactory.getLogger('DataGrid');
        this.gridOptions = <GridOptions> {
            context: {
                componentParent: this
            },
            rowModelType: 'serverSide'
        };
    }

    ngOnInit() {
        this.gridOptions.defaultColDef = {
            width: 0,
            filter: false,
            valueGetter: this.displayValueGetter,
            valueFormatter: this.displayValueFormatter,
            sortable: this.enableSorting,
            resizable: this.enableColumnResize
        }
        this.gridOptions.columnDefs = this.getColumnDefs();
        const _this = this;
        this.gridOptions.onGridReady = function(){
            _this.sizeHeaderAndColumnsToFit();
        };
        // TODO this makes the date editor to close instanttly, because its popup steals the focus
        // this.gridOptions.stopEditingWhenGridLosesFocus = true;

        this.gridOptions.suppressAnimationFrame = true;
        this.gridOptions.animateRows = false;
        this.gridOptions.singleClickEdit = false;
        // the group manager
        this.groupManager = new GroupManager(this);
    }

    displayValueGetter(params) {
        const field = params.colDef.field;
        if (field && params.data) {
            let value = params.data[field];

            if (value == null) {
                value = NULL_VALUE; // need to use an object for null, else grouping won't work in ag grid
            }
            return value;
        }

        return undefined;				
    }

    displayValueFormatter(params): string {
        const field = params.colDef.field;
        if (!params.data) {
            return undefined;
        }
        let value = params.data[field];
        if (value && value.displayValue != undefined) {
            value = value.displayValue;
        }
        // skip format for pinned rows (footer), they are always text
        if(!params.node.rowPinned) {
            const dataGrid = params.context.componentParent;
            const column = dataGrid.getColumn(params.column.colId);

            if (column && column.format ) {
                value = dataGrid.formattingService.format(value, column.format, false);
            }
        }

        if (value == null && params.value == NULL_VALUE) {
            value = '';
        }

        return value;
    }

    /**`
     * Resize header and all columns so they can fit the horizontal space
     *  */
    sizeHeaderAndColumnsToFit() {
        this.agGrid.api.sizeColumnsToFit();
        this.sizeHeader();
    }

    /**
     * Update header height based on cells content height
     */
    sizeHeader() {
        // TODO
        // var headerCell = $element.find('.ag-header-cell');
        // var paddingTop = headerCell.length ? parseInt(headerCell.css('padding-top'), 10) : 0;
        // var paddinBottom = headerCell.length ? parseInt(headerCell.css('padding-bottom'), 10) : 0;
        // var headerCellLabels = $element.find('.ag-header-cell-text');
        const minHeight = this.gridOptions.headerHeight >= 0 ? this.gridOptions.headerHeight : 25;

        // if(minHeight > 0) {
        //     for(var i = 0; i < headerCellLabels.length; i++) {
        //         minHeight = Math.max(minHeight, headerCellLabels[i].scrollHeight + paddingTop + paddinBottom);
        //     }
        // }
        this.agGrid.api.setHeaderHeight(minHeight);
    }

    ngAfterViewInit() {
        // init the root foundset manager
        this.initRootFoundset();
    }

    initRootFoundset() {

        this.foundset = new FoundsetManager(this, this.myFoundset, 'root', true);

        const foundsetServer = new FoundsetServer(this, []);
        const datasource = new FoundsetDatasource(this, foundsetServer);
        this.agGrid.api.setServerSideDatasource(datasource);
        this.isSelectionReady = false;
    }

    getColumnDefs() {
        //create the column definitions from the specified columns in designer
        const colDefs = [];
        let colDef: any = { };
        let column;
        for (let i = 0; this.columns && i < this.columns.length; i++) {
            column = this.columns[i];

            const field = this.getColumnID(column, i);
            //create a column definition based on the properties defined at design time
            colDef = {
                headerName: column.headerTitle ? column.headerTitle : '',
                field,
                headerTooltip: column.headerTooltip ? column.headerTooltip : null
                //TODO: add cellRenderer support
                //cellRenderer: cellRenderer
            };

            if(column.id) {
                colDef.colId = column.id;
            }

            // styleClass
            colDef.headerClass = 'ag-table-header' + (column.headerStyleClass ? ' ' + column.headerStyleClass : '');
            if (column.styleClassDataprovider) {
                colDef.cellClass = this.getCellClass;
            } else {
                colDef.cellClass = 'ag-table-cell' + (column.styleClass ? ' ' + column.styleClass : '');
            }

            // column grouping
            colDef.enableRowGroup = column.enableRowGroup;
            if (column.rowGroupIndex >= 0) colDef.rowGroupIndex = column.rowGroupIndex;
            //TODO: width
            //if (column.width || column.width === 0) colDef.width = column.width;

            // tool panel
            if (column.enableToolPanel === false) colDef.suppressToolPanel = !column.enableToolPanel;

            // column sizing
            //TODO: min/max width
            //if (column.maxWidth) colDef.maxWidth = column.maxWidth;
            //if (column.minWidth || column.minWidth === 0) colDef.minWidth = column.minWidth;
            if (column.visible === false) colDef.hide = true;

            // column resizing https://www.ag-grid.com/javascript-grid-resizing/
            if (column.enableResize === false) colDef.resizable = false;
            if (column.autoResize === false) colDef.suppressSizeToFit = !column.autoResize;

            // column sort
            if (column.enableSort === false) colDef.sortable = false;

            if (column.editType) {
                colDef.editable = column.editType != 'CHECKBOX' ? this.isColumnEditable : false;

                if(column.editType == 'TEXTFIELD') {
                    colDef.cellEditorFramework = TextEditor;
                } else if(column.editType == 'TYPEAHEAD') {
                    colDef.cellEditorFramework = TypeaheadEditor;
                } else if(column.editType == 'DATEPICKER') {
                    colDef.cellEditorFramework = DatePicker;
                } else if(column.editType == 'COMBOBOX') {
                    colDef.cellEditorFramework = SelectEditor;
                } else if(column.editType == 'FORM') {
                    colDef.cellEditorFramework = FormEditor;
                }

                // const _this = this;
                // colDef.onCellValueChanged = function(params) {
                //     var focused = document.activeElement;
                //     // in case value change is triggered by clicking into another cell
                //     // we need a timeout so the new cell will enter edit mode (updateFoundsetRecord needs
                //     // to know the new editing cell, so it can restore its editing state after update)
                //     if(focused && ($(gridDiv).has($(focused)).length)) {
                //         setTimeout(function() {
                //             _this.updateFoundsetRecord(params);
                //         }, 200);
                //     }
                //     else {
                //         _this.updateFoundsetRecord(params);
                //     }
                // }
            }

            //TODO: add filters
            // if (column.filterType) {
            //     colDef.suppressFilter = false;
            //     colDef.filterParams = { applyButton: true, clearButton: true, newRowsAction: 'keep', suppressAndOrCondition: true, caseSensitive: false };

            //     if(column.filterType == 'TEXT') {
            //         colDef.filter = 'agTextColumnFilter';
            //     }
            //     else if(column.filterType == 'NUMBER') {
            //         colDef.filter = 'agNumberColumnFilter';
            //     }
            //     else if(column.filterType == 'DATE') {
            //         colDef.filter = 'agDateColumnFilter';
            //     }
            //     else if(column.filterType == 'VALUELIST' || column.filterType == 'RADIO') {
            //         colDef.filter = getValuelistFilter();
            //         colDef.filterParams.svyFilterType = column.filterType;
            //     }
            // }

            //TODO: get tooltip
            //colDef.tooltipValueGetter = this.getTooltip;


            let columnOptions = {};
            //TODO: add support for NG Grids services
            // if($injector.has('ngDataGrid')) {
            //     var groupingtableDefaultConfig = $services.getServiceScope('ngDataGrid').model;
            //     if(groupingtableDefaultConfig.columnOptions) {
            //         columnOptions = groupingtableDefaultConfig.columnOptions;
            //     }
            // }

            columnOptions = this.mergeConfig(columnOptions, column.columnDef);

            if(columnOptions) {
                const colDefSetByComponent = {};
                for( const p in COLUMN_PROPERTIES_DEFAULTS) {
                    if(COLUMN_PROPERTIES_DEFAULTS[p]['default'] != column[p]) {
                        colDefSetByComponent[COLUMN_PROPERTIES_DEFAULTS[p]['colDefProperty']] = true;
                    }
                }
                for (const property in columnOptions) {
                    if (columnOptions.hasOwnProperty(property) && !colDefSetByComponent.hasOwnProperty(property)) {
                        colDef[property] = columnOptions[property];
                    }
                }
            }

            colDefs.push(colDef);
        }

        // TODO svyRowId should not be visible. I need the id for the selection
        colDefs.push({
            field: '_svyRowId',
            headerName: '_svyRowId',
            suppressColumnsToolPanel: true,
            suppressMenu: true,
            suppressNavigable: true,
            resizable: false,
            hide: true
        });

        colDefs.push({
            field: '_svyFoundsetUUID',
            headerName: '_svyFoundsetUUID',
            suppressColumnsToolPanel: true,
            suppressMenu: true,
            suppressNavigable: true,
            resizable: false,
            hide: true
        });

        return colDefs;
    }

    /**
     * Returns the column identifier
     *
     * @param column
     * @param idx
     *
     * @return
     *
     * @private
     */
    getColumnID(column, idx) {
        if (column.dataprovider) {
            return column.dataprovider.idForFoundset + ':' + idx;
        } else {
            return 'col_' + idx;
        }
    }

    /**
     * Returns the column with the given fieldName
     *
     * @param field
     * @return
     *
     */
    getColumn(field, columnsModel?) {
        if (!columnsModel && this.state.columns[field]) { // check if is already cached
            return this.state.columns[field];
        } else {
            const columns = columnsModel ? columnsModel : this.columns;
            for (let i = 0; i < columns.length; i++) {
                const column = columns[i];
                if (column.id === field || this.getColumnID(column, i) === field) {
                    // cache it in hashmap for quick retrieval
                    if(!columnsModel) this.state.columns[field] = column;
                    return column;
                }
            }
        }
        return null;
    }

    /**
     * Returns the column with the given fieldName
     *
     * @param field
     * @return
     *
     */
    getColumnIndex(field) {
        let fieldToCompare = field;
        let fieldIdx = 0;
        if (field.indexOf('_') > 0) { // has index
            const fieldParts = field.split('_');
            if('col' != fieldParts[0] && !isNaN(fieldParts[1])) {
                fieldToCompare = fieldParts[0];
                fieldIdx = parseInt(fieldParts[1]);
            }
        }

        for (let i = 0; i < this.columns.length; i++) {
            const column = this.columns[i];
            if (column.id === fieldToCompare || this.getColumnID(column, i) == fieldToCompare) {
                if(fieldIdx < 1) {
                    return i;
                }
                fieldIdx--;
            }
        }
        return -1;
    }

    /**
     * @param idsForFoundset
     * Finds all the columns with the given idForFoundset
     *
     * @return
     *
     * @private
     */
    getColIDs(idsForFoundset) {

        const result = [];
        if (!idsForFoundset) {
            return [];
        }

        for (let i = 0; i < this.columns.length; i++) {
            const column = this.columns[i];
            if (column.dataprovider && column.dataprovider.idForFoundset === idsForFoundset) {
                if (column.id) {
                    // Use the colId if is set
                    result.push(column.id);
                } else {
                    // Use the field if colId is not available
                    result.push(this.getColumnID(column, i));
                }
            }
        }
        return result;
    }

    stripUnsortableColumns(sortString) {
        if (sortString) {
            let newSortString = '';
            const sortColumns = sortString.split(',');
            for (let i = 0; i < sortColumns.length; i++) {
                const sortColumn = sortColumns[i];
                let idForFoundset;
                let sortDirection;
                if (!sortColumn) {
                    continue;
                } else if (sortColumn.substr(sortColumn.length - 5, 5) === ' desc') {
                    idForFoundset = sortColumn.substring(0, sortColumn.length - 5);
                    sortDirection = 'desc';
                } else if (sortColumn.substr(sortColumn.length - 4, 4) === ' asc') {
                    idForFoundset = sortColumn.substring(0, sortColumn.length - 4),
                    sortDirection = 'asc';
                }

                let isSortable = false;
                if (idForFoundset && sortDirection) {
                    const agColIds = this.getColIDs(idForFoundset);
                    for (let j = 0; j < agColIds.length; j++) {
                        isSortable = isSortable || this.getColumn(agColIds[j]).enableSort;
                        if(isSortable) break;
                    }
                }
                if(isSortable) {
                    if(newSortString) newSortString += ',';
                    newSortString += idForFoundset + ' ' + sortDirection;
                }
            }
            return newSortString;
        } else return sortString;
    }

    /**
     * Returns true if table is grouping
     *
     * @return
     *
     */
    isTableGrouped() {
        const rowGroupCols = this.getRowGroupColumns();
        return rowGroupCols.length > 0;
    }

    /**
     * Returns table's rowGroupColumns
     * */
    getRowGroupColumns(): any {
        const rowGroupCols = this.gridOptions.columnApi ? this.gridOptions.columnApi.getRowGroupColumns() : null;
        return rowGroupCols ? rowGroupCols : [];
    }

    /**
     * Returns the group hierarchy for the given node
     *
     * @private
     * @param node
     * @return
     *
     *
     */
    getNodeGroupInfo(node) {
        const rowGroupCols = [];
        //var rowGroupColIdxs = [];
        const groupKeys = [];

        const isExpanded = node.expanded;

        let parentNode = node.parent;
        while (parentNode && parentNode.level >= 0 && parentNode.group === true) {
            // check if all fields are fine
            if (!parentNode.field && !parentNode.data) {
                this.log.warn('cannot resolve group nodes ');
                // exit
                return;
            }

            // is reverse order
            rowGroupCols.unshift(parentNode.field);
            //rowGroupColIdxs.unshift(getColumnIndex(parentNode.field))
            groupKeys.unshift(parentNode.data[parentNode.field]);

            // next node
            parentNode = parentNode.parent;
        }

        const field = node.field;
        const key = node.key;

        rowGroupCols.push(field);
        groupKeys.push(key);

        const result = {
            rowGroupFields: rowGroupCols,
            rowGroupKeys: groupKeys
        };
        return result;
    }

    mergeConfig(target, source) {
        let property;

        // clone target to avoid side effects
        let mergeConfig = {};
        for (property in target) {
            mergeConfig[property] = target[property];
        }

        if(source) {
            if(mergeConfig) {
                for (property in source) {
                    if (source.hasOwnProperty(property)) {
                        mergeConfig[property] = source[property];
                    }
                }
            } else {
                mergeConfig = source;
            }
        }
        return mergeConfig;
    }

    getCellClass(params) {
        const column = this.getColumn(params.colDef.field);

        let cellClass = 'ag-table-cell';
        if(column.styleClass) cellClass += ' ' + column.styleClass;

        return cellClass;
    }

    /**
     * Callback used by ag-grid colDef.tooltip
     */
    // getTooltip(args) {
    //     let tooltip = "";
    //     // skip pinned (footer) nodes
    //     if(!args.node.rowPinned) {
    //         if (!this.isTableGrouped()) {
    //             let column = this.getColumn(args.colDef.field);
    //             if (column && column.tooltip) {
    //                 let index = args.node.rowIndex - foundset.foundset.viewPort.startIndex;
    //                 tooltip = column.tooltip[index];
    //             }
    //         }
    //         else {
    //             let foundsetManager = getFoundsetManagerByFoundsetUUID(args.data._svyFoundsetUUID);
    //             let index = foundsetManager.getRowIndex(args.data) - foundsetManager.foundset.viewPort.startIndex;
    //             if (index >= 0 && foundsetManager.foundset.viewPort.rows[index][args.colDef.field + "_tooltip"] != undefined) {
    //                 tooltip = foundsetManager.foundset.viewPort.rows[index][args.colDef.field + "_tooltip"];
    //             }
    //         }
    //     }
    //     return tooltip;
    // }

    // cellRenderer(params) {
    //     var isGroupColumn = false;
    //     var colId = null;
    //     if(params.colDef.field == undefined) {
    //         isGroupColumn = true;
    //         if(params.colDef.colId.indexOf("ag-Grid-AutoColumn-") == 0) {
    //             colId = params.colDef.colId.substring("ag-Grid-AutoColumn-".length);
    //         }
    //     }
    //     else {
    //         colId = params.colDef.field;
    //     }

    //     var col = colId != null ? this.getColumn(colId) : null;
    //     var value = params.value;

    //     var returnValueFormatted = false;
    //     var checkboxEl = null;

    //     if(col && col.editType == 'CHECKBOX' && !params.node.group) {
    //         checkboxEl = document.createElement('i');
    //         checkboxEl.className = getIconCheckboxEditor(parseInt(value));
    //     }
    //     else {
    //         if(col != null && col.showAs == 'html') {
    //             value =  value && value.displayValue != undefined ? value.displayValue : value;
    //         } else if(col != null && col.showAs == 'sanitizedHtml') {
    //             value = $sanitize(value && value.displayValue != undefined ? value.displayValue : value)
    //         } else if (value && value.contentType && value.contentType.indexOf('image/') == 0 && value.url) {
    //             value = '<img class="ag-table-image-cell" src="' + value.url + '">';
    //         } else {
    //             returnValueFormatted = true;
    //         }

    //         if(value instanceof Date) returnValueFormatted = true;
    //     }

    //     var styleClassProvider = null;
    //     if(!isGroupColumn) {
    //         if(!params.node.rowPinned) {
    //             if (!this.isTableGrouped()) {
    //                 var column = this.getColumn(params.colDef.field);
    //                 if (column && column.styleClassDataprovider) {
    //                     var index = params.rowIndex - foundset.foundset.viewPort.startIndex;
    //                     styleClassProvider = column.styleClassDataprovider[index];
    //                 }
    //             } else if (params.data && params.data._svyFoundsetUUID) {
    //                     var foundsetManager = getFoundsetManagerByFoundsetUUID(params.data._svyFoundsetUUID);
    //                     var index = foundsetManager.getRowIndex(params.data) - foundsetManager.foundset.viewPort.startIndex;
    //                     if (index >= 0) {
    //                         styleClassProvider = foundsetManager.foundset.viewPort.rows[index][params.colDef.field + "_styleClassDataprovider"];
    //                     } else {
    //                         $log.warn('cannot render styleClassDataprovider for row at index ' + index)
    //                         $log.warn(params.data);
    //                     }
    //             }
    //         } else if(col.footerStyleClass && params.node.rowPinned == "bottom") { // footer
    //             styleClassProvider = col.footerStyleClass;
    //         }
    //     }

    //     if(styleClassProvider) {
    //         var divContainer = document.createElement("div");
    //         divContainer.className = styleClassProvider;
    //         if(checkboxEl) {
    //             divContainer.appendChild(checkboxEl);
    //         }
    //         else {
    //             divContainer.innerHTML = returnValueFormatted ? params.valueFormatted : value;
    //         }

    //         return divContainer;
    //     }
    //     else {
    //         if(checkboxEl) {
    //             return checkboxEl;
    //         }
    //         else {
    //             return returnValueFormatted ? document.createTextNode(params.valueFormatted) : value;
    //         }
    //     }
    // }

    //TODO
    selectedRowIndexesChanged(foundsetManager?): boolean {
        return false;
    }

    /**
     * Update the uiGrid row with given viewPort index
     *
     * @param rowUpdates
     * @param [oldStartIndex]
     * @param oldSize
     *
     * return {Boolean} whatever a purge ($scope.purge();) was done due to update
     *
     */
    updateRows(rowUpdates, oldStartIndex, oldSize) {
    }

    updateFoundsetRecord(params) {
        // var rowIndex = params.node.rowIndex;
        // var colId = params.column.colId;

        // // if we have an invalid cell data, ignore any updates for other cells
        // if((invalidCellDataIndex.rowIndex != -1 && invalidCellDataIndex.rowIndex != rowIndex)
        //     || (invalidCellDataIndex.colKey != '' && invalidCellDataIndex.colKey != colId)) {
        //     return;
        // }

        // var row = params.data;
        // var foundsetManager = getFoundsetManagerByFoundsetUUID(row._svyFoundsetUUID);
        // if (!foundsetManager) foundsetManager = foundset;
        // var foundsetRef = foundsetManager.foundset;
        // var newValue = params.newValue;
        // if(newValue && newValue.realValue !== undefined) {
        //     newValue = newValue.realValue;
        // }
        // var oldValue = params.oldValue;
        // if(oldValue && oldValue.realValue !== undefined) {
        //     oldValue = oldValue.realValue;
        // }
        // var oldValueStr = oldValue;
        // if(oldValueStr == null) oldValueStr = "";

        // var col = getColumn(params.colDef.field);
        // // ignore types in compare only for non null values ("200"/200 are equals, but ""/0 is not)
        // var isValueChanged = newValue != oldValueStr || (!newValue && newValue !== oldValueStr);
        // if(isValueChanged && newValue instanceof Date && oldValue instanceof Date) {
        //     isValueChanged = newValue.toISOString() != oldValue.toISOString();
        // }
        // if(col && col.dataprovider && col.dataprovider.idForFoundset && (isValueChanged || invalidCellDataIndex.rowIndex != -1)) {
        //     if(isValueChanged) {
        //         foundsetRef.updateViewportRecord(row._svyRowId, col.dataprovider.idForFoundset, newValue, oldValue);
        //         if($scope.handlers.onColumnDataChange) {
        //             var currentEditCells = gridOptions.api.getEditingCells();
        //             onColumnDataChangePromise = $scope.handlers.onColumnDataChange(
        //                 getFoundsetIndexFromEvent(params),
        //                 getColumnIndex(params.column.colId),
        //                 oldValue,
        //                 newValue,
        //                 createJSEvent()
        //             );
        //             onColumnDataChangePromise.then(function(r) {
        //                 if(r == false) {
        //                     // if old value was reset, clear invalid state
        //                     var currentValue = gridOptions.api.getValue(colId, params.node);
        //                     if(currentValue && currentValue.realValue !== undefined) {
        //                         currentValue = currentValue.realValue;
        //                     }
        //                     if(oldValue === currentValue) {
        //                         invalidCellDataIndex.rowIndex = -1;
        //                         invalidCellDataIndex.colKey = '';
        //                     }
        //                     else {
        //                         invalidCellDataIndex.rowIndex = rowIndex;
        //                         invalidCellDataIndex.colKey = colId;
        //                     }
        //                     var editCells = gridOptions.api.getEditingCells();
        //                     if(isSelectionReady && (!editCells.length || (editCells[0].rowIndex != rowIndex || editCells[0].column.colId != colId))) {
        //                         gridOptions.api.stopEditing();
        //                         gridOptions.api.startEditingCell({
        //                             rowIndex: rowIndex,
        //                             colKey: colId
        //                         });
        //                         setTimeout(function() {
        //                             selectionEvent = null;
        //                             gridOptions.api.forEachNode( function(node) {
        //                                 if (node.rowIndex === rowIndex) {
        //                                     node.setSelected(true, true);
        //                                 }
        //                             });
        //                         }, 0);
        //                     }
        //                 }
        //                 else {
        //                     invalidCellDataIndex.rowIndex = -1;
        //                     invalidCellDataIndex.colKey = '';
        //                     var editCells = gridOptions.api.getEditingCells();
        //                     if(isSelectionReady && editCells.length == 0 && currentEditCells.length != 0) {
        //                         gridOptions.api.startEditingCell({
        //                             rowIndex: currentEditCells[0].rowIndex,
        //                             colKey: currentEditCells[0].column.colId
        //                         });
        //                     }
        //                 }
        //                 onColumnDataChangePromise = null;
        //             }).catch(function(e) {
        //                 $log.error(e);
        //                 invalidCellDataIndex.rowIndex = -1;
        //                 invalidCellDataIndex.colKey = '';
        //                 onColumnDataChangePromise = null;
        //             });
        //         }
        //     }
        // }
    }

    /**
     * remove the given foundset hash from the model hashmap.
     * User to clear the memory
     *
     * @public
     *  */
    removeFoundSetByFoundsetUUID(foundsetHash) {

        if (foundsetHash === 'root') {
            this.log.error('Trying to remove root foundset');
            return false;
        }

        // remove the hashedFoundsets
        const _this = this;
        this.servoyApi.callServerSideApi('removeGroupedFoundsetUUID', [foundsetHash]).then(function(removed) {
            if (removed) {
                delete _this.state.foundsetManagers[foundsetHash];
            } else {
                _this.log.warn('could not delete hashed foundset ' + foundsetHash);
            }
        }).catch(function(e) {
            _this.log.error(e);
        });

    }

    /**
     * @public
     * Get Foundset in hashMap by UUID
     * */
    getFoundSetByFoundsetUUID(foundsetHash) {
        // TODO return something else here ?
        if (foundsetHash === 'root') return this.myFoundset;
        if (this.hashedFoundsets) {
            for (let i = 0; i < this.hashedFoundsets.length; i++) {
                if (this.hashedFoundsets[i].foundsetUUID == foundsetHash)
                    return this.hashedFoundsets[i].foundset;

            }
        }
        return null;
    }

    /**
     * Returns the foundset manager for the given hash
     *
     * @return
     * @public
     */
    getFoundsetManagerByFoundsetUUID(foundsetHash) {
        if (!foundsetHash) return null;

        if (foundsetHash === 'root') return this.foundset;

        if (this.state.foundsetManagers[foundsetHash]) {
            // double check if foundset hashmap still exists
            if (!this.getFoundSetByFoundsetUUID(foundsetHash)) {
                this.log.error('This should not happen: could not verify foundset exists in foundsetHashmap ' + foundsetHash);
                return null;
            }
            return this.state.foundsetManagers[foundsetHash];
        } else {
            const foundsetRef = this.getFoundSetByFoundsetUUID(foundsetHash);
            const foundsetManager = new FoundsetManager(this, foundsetRef, foundsetHash, false);
            this.state.foundsetManagers[foundsetHash] = foundsetManager;
            return foundsetManager;
        }
    }

    /**
     * @type {SortModelType}
     * Returns the sortString and sortColumns array for the given sortModel
     *
     * @return
     *
     */
    getFoundsetSortModel(sortModel) {
        let sortString;
        const sortColumns = [];
        if (sortModel) {
            sortString = '';
            const addedColumnNames = [];
            for (let i = 0; i < sortModel.length; i++) {
                const sortModelCol = sortModel[i];
                const column = this.getColumn(sortModelCol.colId);
                if (column) {
                    const columnName = column.dataprovider.idForFoundset;
                    if(addedColumnNames.indexOf(columnName) == -1) {
                        addedColumnNames.push(columnName);
                        const direction = sortModelCol.sort;
                        if (i > 0) sortString += ',';
                        sortString += columnName + ' ' + direction + '';
                        sortColumns.push({ name: columnName, direction });
                    }
                }
            }
            sortString = sortString.trim();
        }

        return {
            sortString,
            sortColumns
        };
    }

    getFoundsetIndexFromEvent(params): number {
        let foundsetIndex;
        if (this.isTableGrouped()) {
            this.log.warn('select grouped record not supported yet');
            foundsetIndex = -1;
            // TODO use serverside API getRecordIndex
        } else {
            foundsetIndex = params.node.rowIndex + 1;
        }
        return foundsetIndex;
    }

    /**
     * TODO parametrize foundset or add it into foundsetManager object
     * Returns the sort model for the root foundset
     *
     * @return
     *
     */
    getSortModel() {
        const sortModel = [];
        let sortColumns: any = this.foundset.getSortColumns();
        if (sortColumns) {
            sortColumns = sortColumns.split(',');
            for (let i = 0; i < sortColumns.length; i++) {
                // TODO parse sortColumns into default sort string
                /** @type {String} */
                const sortColumn = sortColumns[i];
                let idForFoundset;
                let sortDirection;
                if (!sortColumn) {
                    continue;
                } else if (sortColumn.substr(sortColumn.length - 5, 5) === ' desc') {
                    idForFoundset = sortColumn.substring(0, sortColumn.length - 5);
                    sortDirection = 'desc';
                } else if (sortColumn.substr(sortColumn.length - 4, 4) === ' asc') {
                    idForFoundset = sortColumn.substring(0, sortColumn.length - 4),
                    sortDirection = 'asc';
                }

                // add it into the sort model
                if (idForFoundset && sortDirection) {
                    const agColIds = this.getColIDs(idForFoundset);

                    for (let j = 0; j < agColIds.length; j++) {
                        sortModel.push({
                            colId: agColIds[j],
                            sort: sortDirection
                        });
                    }
                }
            }
        }
        return sortModel;
    }

    purgeImpl() {
        //console.log(gridOptions.api.getInfinitePageState())

        // an hard refresh is necessary to show the groups
        if (this.isTableGrouped()) {
            this.groupManager.removeFoundsetRefAtLevel(0);
        }
        // reset root foundset
        this.foundset.foundset = this.myFoundset;

        const currentEditCells = this.agGrid.api.getEditingCells();
        if(currentEditCells.length != 0) {
            this.startEditFoundsetIndex = currentEditCells[0].rowIndex + 1;
            this.startEditColumnIndex = this.getColumnIndex(currentEditCells[0].column.getColId());
        }

        this.agGrid.api.purgeServerSideCache();
        this.dirtyCache = false;
        this.isSelectionReady = false;
        this.scrollToSelectionWhenSelectionReady = true;
        this.columnsToFitAfterRowsRendered = true;
        // $log.warn('purge cache');

        // TODO expand previously expanded rows
        //
        //					var columns = state.expanded.columns;
        //					for (var field in columns) {
        //						// FIXME there is no ag-grid method to force group expand for a specific key value
        //					}
    }

    getValuelist(params): any {
        return this.getValuelistEx(params.node.data, params.column.colId);
    }

    getValuelistEx(row, colId): any {
        let column;
        let foundsetRows;

        const foundsetManager = this.getFoundsetManagerByFoundsetUUID(row._svyFoundsetUUID);
        // if not root, it should use the column/foundsetRows from the hashed map
        if (foundsetManager.isRoot) {
            column = this.getColumn(colId);
            foundsetRows = this.myFoundset.viewPort.rows;
        } else if (this.hashedFoundsets) {
            for (let i = 0; i < this.hashedFoundsets.length; i++) {
                if (this.hashedFoundsets[i].foundsetUUID == foundsetManager.foundsetUUID) {
                    column = this.getColumn(colId, this.hashedFoundsets[i].columns);
                    foundsetRows = foundsetManager.foundset.viewPort.rows;
                    break;
                }
            }
        }
        if(!column || !foundsetRows) {
            this.log.error('Cannot find column/foundset to read the valuelist.');
            return null;
        }

        // if it's a foundset linked prop (starting with Servoy 8.3.2) or not (prior to 8.3.2)
        if (column.valuelist && column.valuelist.state
                && column.valuelist.state['recordLinked'] != undefined) {
            // _svyRowId: "5.10643;_0"
            const rowId = row[ViewportService.ROW_ID_COL_KEY];

            if (column.valuelist.length == 0 && foundsetRows.length > 0) {
                // this if is just for backwards compatilility editing comboboxes with valuelists with Servoy < 8.3.3 (there the foundset-linked-in-spec valuelists in custom objects
                // would not be able to reference their foundset from client-side JS => for valuelists that were not actually linked
                // client side valuelist.js would simulate a viewport that has as many items as the foundset rows containing really the same valuelist object
                // and this did not work until the fix of SVY-12718 (valuelist.js was not able to find foundset from the same custom object) => valuelist viewport
                // was length 0; this whole if can be removed once groupingtable's package will require Servoy >= 8.3.3

                // fall back to what was done previously - use root valuelist and foundset to resolve stuff (which will probably work except for related valuelists)
                column = this.getColumn(colId);
                foundsetRows = this.myFoundset.viewPort.rows;
            }

            let idxInFoundsetViewport: any = -1;
            for (const idx in foundsetRows)
                if (foundsetRows[idx][ViewportService.ROW_ID_COL_KEY].indexOf(rowId) == 0) {
                    idxInFoundsetViewport = idx;
                    break;
                }

            if (idxInFoundsetViewport >= 0 && idxInFoundsetViewport < column.valuelist.length) return column.valuelist[idxInFoundsetViewport];
            else if (!column.valuelist.state['recordLinked'] && column.valuelist.length > 0) return column.valuelist[0];
            else {
                this.log.error('Cannot find the valuelist entry for the row that was clicked.');
                return null;
            }
        } else return column.valuelist;
    }

    /**
     * Callback used by ag-grid colDef.editable
     */
    isColumnEditable(args) {

        const _this = args.context.componentParent;
        // skip pinned (footer) nodes
        if(args.node.rowPinned) return false;

        // if read-only and no r-o columns
        if(_this.readOnly && !_this.readOnlyColumnIds) return false;

        const rowGroupCols = _this.getRowGroupColumns();
        for (let i = 0; i < rowGroupCols.length; i++) {
            if (args.colDef.field == rowGroupCols[i].colDef.field) {
                return false;	// don't allow editing columns used for grouping
            }
        }

        let isColumnEditable = true;
        if (!_this.isTableGrouped()) {
            const column = _this.getColumn(args.colDef.field);
            if (column && column.isEditableDataprovider) {
                const index = args.node.rowIndex - _this.foundset.foundset.viewPort.startIndex;
                isColumnEditable = column.isEditableDataprovider[index];
            }
        } else {
            const foundsetManager = _this.getFoundsetManagerByFoundsetUUID(args.data._svyFoundsetUUID);
            const index = foundsetManager.getRowIndex(args.data) - foundsetManager.foundset.viewPort.startIndex;
            if (index >= 0 && foundsetManager.foundset.viewPort.rows[index][args.colDef.field + '_isEditableDataprovider'] != undefined) {
                isColumnEditable = foundsetManager.foundset.viewPort.rows[index][args.colDef.field + '_isEditableDataprovider'];
            }
        }

        // if editable check the r-o state from the runtime map
        if(isColumnEditable && _this.readOnlyColumnIds && args.colDef.colId && _this.readOnlyColumnIds['_' + args.colDef.colId] != undefined) {
            return !_this.readOnlyColumnIds['_' + args.colDef.colId];
        }

        return isColumnEditable && !_this.readOnly;
    }

    editCellAtWithTimeout(foundsetindex, columnindex) {
    }
}

class State {
    waitFor: WaitForInfo = new WaitForInfo();
    /** column mapping by field name e.g. state.columns[field] */
    columns = {};
    foundsetManagers = {};
    /** valuelists stored per field */
    valuelists = {};
    expanded: ExpandedInfo = new ExpandedInfo();
    /** Store the latest column group, as an ordered array of colId  */
    grouped: GroupedInfo = new GroupedInfo();
    /** Store the latest rowGroupCols */
    rowGroupCols = [];
    /** Stor the latest groupKeys*/
    groupKeys = [];
    /** Sort state of the root group */
    rootGroupSort = null;
}

class WaitForInfo {
    sort = 0;
    loadRecords = 0;
}

class ExpandedInfo {
    /** The column collapsed
     *
     * @deprecated */
    columns = {};
    /** the group fields in order
     * This is a re-duntant info. I can obtain it via:
     *
     * var groupedColumns = gridOptions.columnApi.getRowGroupColumns();
     * var groupFields = [];
     * for (var j = 0; j < groupedColumns.length; j++) {
     *	    groupFields.push(groupedColumns[j].colDef.field);
     * }
     *  */
    fields = [];
}

class GroupedInfo {
    columns = [];
}

/**
 * Handle viewPort, row, sort, isLastRow of a foundsetReference object
 */
class FoundsetManager {

    constructor(public dataGrid: DataGrid, public foundset: IFoundset, public foundsetUUID: string, public isRoot: boolean) {
        if (!isRoot) {
            // add the change listener to the component
            foundset.addChangeListener(this.foundsetListener);
        }
    }

    /**
     *  return the viewPort data in a new object
     *
     * @param [startIndex]
     * @param [endIndex]
     *
     */
    getViewPortData(startIndex: number, endIndex: number) {
        const result = [];
        startIndex = startIndex ? startIndex : 0;
        endIndex = endIndex && (endIndex < this.foundset.viewPort.rows.length) ? endIndex : this.foundset.viewPort.rows.length;

        // index cannot exceed ServerSize
        startIndex = Math.min(startIndex, this.foundset.serverSize);
        endIndex = Math.min(endIndex, this.foundset.serverSize);

        let columnsModel;
        if (this.isRoot) {
            columnsModel = this.dataGrid.columns;
        } else if (this.dataGrid.hashedFoundsets) {
            for (let i = 0; i < this.dataGrid.hashedFoundsets.length; i++) {
                if (this.dataGrid.hashedFoundsets[i].foundsetUUID == this.foundsetUUID) {
                    columnsModel = this.dataGrid.hashedFoundsets[i].columns;
                    break;
                }
            }
        }

        for (let j = startIndex; j < endIndex; j++) {
            const row = this.getViewPortRow(j, columnsModel);
            if (row) result.push(row);
        }

        return result;
    }

    /** return the row in viewport at the given index */
    getViewPortRow(index: number, columnsModel) {
        let r;
        try {
            r = new Object();
            // push the id so the rows can be merged
            const viewPortRow = this.foundset.viewPort.rows[index];
            if (!viewPortRow) {
                this.dataGrid.log.error('Cannot find row ' + index + ' in foundset ' + this.foundsetUUID + ' size ' + this.foundset.viewPort.size + ' startIndex ' + this.foundset.viewPort.startIndex);
                return null;
            }

            r._svyRowId = viewPortRow._svyRowId;
            r._svyFoundsetUUID = this.foundsetUUID;
            r._svyFoundsetIndex = this.foundset.viewPort.startIndex + index;

            const columns = columnsModel ? columnsModel : this.dataGrid.columns;

            // push each dataprovider
            for (let i = 0; i < columns.length; i++) {
                const header = columns[i];
                const field = header.id == 'svycount' ? header.id : this.dataGrid.getColumnID(header, i);

                const value = header.dataprovider ? header.dataprovider[index] : null;
                r[field] = value;
            }
            return r;

        } catch (e) {
            this.dataGrid.log.error(e);
            r = null;
        }
        return r;
    }

    hasMoreRecordsToLoad() {
        return this.foundset.hasMoreRows || (this.foundset.viewPort.startIndex + this.foundset.viewPort.size) < this.foundset.serverSize;
    }

    getLastRowIndex() {
        if (this.hasMoreRecordsToLoad()) {
            return -1;
        } else {
            return this.foundset.serverSize;
        }
    }

    loadExtraRecordsAsync(startIndex: number, size: number, dontNotifyYet) {
        // TODO use loadExtraRecordsAsync to keep cache small
        size = (size * CACHED_CHUNK_BLOCKS) + size;
        if (this.hasMoreRecordsToLoad() === false) {
            size = this.foundset.serverSize - startIndex;
        }
        if (size < 0) {
            this.dataGrid.log.error('Load size should not be negative: startIndex ' + startIndex + ' server size ' + this.foundset.serverSize);
            size = 0;
        }

        // Wait for response
        const isRootFoundset = this.isRoot;
        const requestId = 1 + Math.random();
        this.dataGrid.state.waitFor.loadRecords = isRootFoundset ? requestId : 0; // we use state.waitfor.loadRecords only in the root foundset change listener
        // TODO can it handle multiple requests ?
        const _this = this;
        const promise = this.foundset.loadRecordsAsync(startIndex, size);
        promise.finally(() => {
            // foundset change listener that checks for 'state.waitfor.loadRecords' is executed later,
            // as last step when the response is processed, so postpone clearing the flag
            if(isRootFoundset) {
                setTimeout(function() {
                    if (_this.dataGrid.state.waitFor.loadRecords !== requestId) {
                        // FIXME if this happen reduce parallel async requests to 1
                        _this.dataGrid.log.warn('Load record request id \'' + _this.dataGrid.state.waitFor.loadRecords + '\' is different from the resolved promise \'' + requestId + '\'; this should not happen !!!');
                    }
                    _this.dataGrid.state.waitFor.loadRecords = 0;
                }, 0);
            }
        });

        return promise;
    }

    getSortColumns() {
        return this.foundset ? this.foundset.sortColumns : null;
    }

    sort(sortString) {
        if (sortString) {
            // TODO check sort
            return this.foundset.sort(sortString);
        }
    }

    /**
     * @return return the foundset index of the given row in viewPort (includes the startIndex diff)
     *
     */
    getRowIndex(row) {
        const id = row._svyRowId;
        const viewPortRows = this.foundset.viewPort.rows;
        for (let i = 0; i < viewPortRows.length; i++) {
            if (viewPortRows[i]._svyRowId === id) {
                return i + this.foundset.viewPort.startIndex;
            }
        }
        return -1;
    }

    foundsetListener(change: FoundsetChangeEvent) {
        this.dataGrid.log.debug('child foundset changed listener ' + this.foundset);

        if (change.sortColumnsChanged) {
            const newSort = change.sortColumnsChanged.newValue;
            const oldSort = change.sortColumnsChanged.oldValue;

            // sort changed
            this.dataGrid.log.debug('Change Group Sort Model ' + newSort);
            return;
        }

        if (change.sortColumnsChanged) {
            this.dataGrid.selectedRowIndexesChanged(this);
        }

        if (change.viewportRowsUpdated) {
            const updates = change.viewportRowsUpdated;
            this.dataGrid.log.debug(updates);
            this.dataGrid.updateRows(updates, null, null);
        }

    }

    destroy() {
        this.dataGrid.log.debug('destroy ' + this.foundsetUUID);

        // remove the listener
        this.foundset.removeChangeListener(this.foundsetListener);

        // persistently remove the foundset from other cached objects (model.hashedFoundsets, state.foundsetManager);
        this.dataGrid.removeFoundSetByFoundsetUUID(this.foundsetUUID);
    }
}

class FoundsetServer {

    constructor(public dataGrid: DataGrid, public allData) {
    }

    /**
     * @param request
     * @param groupKeys
     * @param callback callback(data, isLastRow)
     * @protected
     */
    getData(request, groupKeys, callback) {

        this.dataGrid.log.debug(request);

        // the row group cols, ie the cols that the user has dragged into the 'group by' zone, eg 'Country' and 'Customerid'
        const rowGroupCols = request.rowGroupCols;

        // if going aggregation, contains the value columns, eg ['gold','silver','bronze']
        const valueCols = request.valueCols;

        // rowGroupCols cannot be 2 level deeper than groupKeys
        // rowGroupCols = rowGroupCols.slice(0, groupKeys.length + 1);

        const allPromises = [];

        const filterModel = this.dataGrid.agGrid.api.getFilterModel();
        // create filter model with column indexes that we can send to the server
        const updatedFilterModel = {};
        for(const c in filterModel) {
            const columnIndex = this.dataGrid.getColumnIndex(c);
            if(columnIndex != -1) {
                updatedFilterModel[columnIndex] = filterModel[c];
            }
        }
        const sUpdatedFilterModel = JSON.stringify(updatedFilterModel);
        // if filter is changed, apply it on the root foundset, and clear the foundset cache if grouped
        if (sUpdatedFilterModel != this.dataGrid.filterModel && !(sUpdatedFilterModel == '{}' && this.dataGrid.filterModel == undefined)) {
            this.dataGrid.filterModel = sUpdatedFilterModel;
            const filterMyFoundsetArg = [];
            filterMyFoundsetArg.push(sUpdatedFilterModel);

            if(rowGroupCols.length) {
                this.dataGrid.groupManager.removeFoundsetRefAtLevel(0);
                filterMyFoundsetArg.push('{}');
            } else {
                filterMyFoundsetArg.push(sUpdatedFilterModel);
            }
            allPromises.push(this.dataGrid.servoyApi.callServerSideApi('filterMyFoundset', filterMyFoundsetArg));
        }

        let sortModel =  this.dataGrid.agGrid.api.getSortModel();

        let result;
        let sortRootGroup = false;

        // if clicking sort on the grouping column
        if (rowGroupCols.length > 0 && sortModel[0] &&
            (sortModel[0].colId === ('ag-Grid-AutoColumn-' + rowGroupCols[0].id) || sortModel[0].colId === rowGroupCols[0].id)) {
            // replace colFd with the id of the grouped column
            sortModel = [{ colId: rowGroupCols[0].field, sort: sortModel[0].sort }];
            if(!this.dataGrid.state.rootGroupSort  || this.dataGrid.state.rootGroupSort.colId != sortModel[0].colId || this.dataGrid.state.rootGroupSort.sort != sortModel[0].sort) {
                sortRootGroup = true;
            }
        }
        let foundsetSortModel = this.dataGrid.getFoundsetSortModel(sortModel);
        const sortString = foundsetSortModel.sortString;

        this.dataGrid.log.debug('Group ' + (rowGroupCols[0] ? rowGroupCols[0].displayName : '/') + ' + ' + (groupKeys[0] ? groupKeys[0] : '/') + ' # ' + request.startRow + ' # ' + request.endRow);

        // init state of grouped columns. Is normally done by onRowColumnsChanged but is not triggered if rowGroupIndex is set at design time
        // FIXME should handle runtime changes to model.columns. It does not at the moment
        if (!this.dataGrid.state.grouped.columns.length) {
            for (let i = 0; i < rowGroupCols.length; i++) {
                this.dataGrid.state.grouped.columns.push(rowGroupCols[i].field);
            }
            // is in group view first time the form is shown ?
            this.dataGrid.isGroupView = rowGroupCols.length > 0;
        }

        // Sort on the foundset Group
        if (sortRootGroup) { // no sort need to be applied
            // Should change the foundset with a different sort order
            allPromises.push(this.dataGrid.groupManager.createOrReplaceFoundsetRef(rowGroupCols, groupKeys, sortModel[0].sort));
        } else {
            // get the foundset reference
            allPromises.push(this.dataGrid.groupManager.getFoundsetRef(rowGroupCols, groupKeys));
        }
        Promise.all(allPromises).then(getFoundsetRefSuccess).catch(getFoundsetRefError);

        const _this = this;
        /**
         * GetFoundserRef Promise Callback
         *
         * @param foundsetUUID
         * @protected
         */
        function getFoundsetRefSuccess(args) {

            const foundsetUUID = args[args.length - 1];

            // TODO search in state first ?
            // The foundsetUUID exists in the
            // foundsetHashmap
            // groupManager (UUID)
            // group, in the foundsetHashmap and in the state ?
            const foundsetRefManager = _this.dataGrid.getFoundsetManagerByFoundsetUUID(foundsetUUID);

            if (sortString === '') {
                // TODO restore a default sort order when sort is removed
                // $log.warn(" Use the default foundset sort.. which is ? ");
            }

            if(sortRootGroup) {
                _this.dataGrid.state.rootGroupSort = sortModel[0];
            }

            const currentGridSort = _this.dataGrid.getFoundsetSortModel(_this.dataGrid.agGrid.api.getSortModel());
            const foundsetSort = _this.dataGrid.stripUnsortableColumns(_this.dataGrid.foundset.getSortColumns());
            const isSortChanged = _this.dataGrid.onSort /*&& rowGroupCols.length === groupKeys.length*/ && sortString != foundsetSort
            && currentGridSort.sortString != foundsetSort;

            if(isSortChanged) {
                _this.dataGrid.log.debug('CHANGE SORT REQUEST');
                let isColumnSortable = false;
                // check sort columns in both the reques and model, because it is disable in the grid, it will be only in the model
                const sortColumns = sortModel.concat(_this.dataGrid.getSortModel());
                for(let i = 0; i < sortColumns.length; i++) {
                    const col = _this.dataGrid.gridOptions.columnApi.getColumn(sortColumns[i].colId);
                    if(col && col.getColDef().sortable) {
                        isColumnSortable = true;
                        break;
                    }
                }

                if(isColumnSortable) {
                    // send sort request if header is clicked; skip if is is not from UI (isSelectionReady == false) or if it from a sort handler or a group column sort
                    if(_this.dataGrid.isSelectionReady || sortString) {
                        foundsetSortModel = _this.dataGrid.getFoundsetSortModel(sortModel);
                        _this.dataGrid.sortPromise = foundsetRefManager.sort(foundsetSortModel.sortColumns);
                        _this.dataGrid.sortPromise.then(function() {
                            getDataFromFoundset(foundsetRefManager);
                            // give time to the foundset change listener to know it was a client side requested sort
                            setTimeout(function() {
                                _this.dataGrid.sortPromise = null;
                            }, 0);
                        }).catch(function(e) {
                            _this.dataGrid.sortPromise = null;
                        });
                    }
                    // set the grid sorting if foundset sort changed from the grid initialization (like doing foundset sort on form's onShow)
                    else {
                        _this.dataGrid.agGrid.api.setSortModel(_this.dataGrid.getSortModel());
                        _this.dataGrid.agGrid.api.purgeServerSideCache();
                    }
                } else {
                    getDataFromFoundset(foundsetRefManager);
                }
            } else {
                getDataFromFoundset(foundsetRefManager);
            }
        }

        /**
         * GetDataFromFoundset Promise Callback
         *
         * @param foundsetRef the foundsetManager object
         * @protected
         */
        function getDataFromFoundset(foundsetManager: FoundsetManager) {
            // test cache blocks
            //if (!isTableGrouped()) test_validateCache();

            // load record if endRow is not in viewPort
            const startIndex = foundsetManager.foundset.viewPort.startIndex; // start index of view port (0-based)
            const viewPortSize = foundsetManager.foundset.viewPort.size; // viewport size
            const endIndex = startIndex + viewPortSize; // end index of the view port (0-based)

            // index in the cached viewPort (0-based);
            let viewPortStartIndex = request.startRow - startIndex;
            let viewPortEndIndex = request.endRow - startIndex;

            if (request.startRow < startIndex || (request.endRow > endIndex && foundsetManager.getLastRowIndex() === -1)) {

                const errorTimeout = setTimeout(function() {
                        _this.dataGrid.log.error('Could not load records for foundset ' + foundsetManager.foundsetUUID + ' Start ' + request.startRow + ' End ' + request.endRow);
                    }, 10000); // TODO set timeout

                let requestViewPortStartIndex;
                // keep the previous chunk in cache
                if (request.startRow >= CHUNK_SIZE && request.endRow >= endIndex) {
                    requestViewPortStartIndex = request.startRow - CHUNK_SIZE;
                } else {
                    requestViewPortStartIndex = request.startRow;
                }

                const requestViewPortEndIndex = request.endRow - requestViewPortStartIndex;
                const size = request.endRow - request.startRow;

                _this.dataGrid.log.debug('Load async ' + requestViewPortStartIndex + ' - ' + requestViewPortEndIndex + ' with size ' + size);
                const promise = foundsetManager.loadExtraRecordsAsync(requestViewPortStartIndex, size, false);
                promise.then(function() {

                    // load complete
                    if (errorTimeout) {
                        clearTimeout(errorTimeout);
                    }

                    // get the index of the last row
                    const lastRowIndex = foundsetManager.getLastRowIndex();

                    // update viewPortStatIndex
                    viewPortStartIndex = request.startRow - foundsetManager.foundset.viewPort.startIndex;
                    viewPortEndIndex = request.endRow - foundsetManager.foundset.viewPort.startIndex;

                    _this.dataGrid.log.debug('Get View Port ' + viewPortStartIndex + ' - ' + viewPortEndIndex + ' on ' + foundsetManager.foundset.viewPort.startIndex + ' with size ' + foundsetManager.foundset.viewPort.size);

                    result = foundsetManager.getViewPortData(viewPortStartIndex, viewPortEndIndex);
                    callback(result, lastRowIndex);

                    // TODO data is ready here ?

                }).catch(getFoundsetRefError);
            } else {
                callback(foundsetManager.getViewPortData(viewPortStartIndex, viewPortEndIndex), foundsetManager.getLastRowIndex());
            }
        }

        function getFoundsetRefError(e) {
            _this.dataGrid.log.error(e);
            _this.dataGrid.isDataLoading = false;
            _this.dataGrid.gridOptions.columnApi.setRowGroupColumns([]);
        }
    }; // End getData
}

class FoundsetDatasource {

    constructor(public dataGrid: DataGrid, public foundsetServer) {
    }

    getRows(params) {
        this.dataGrid.log.debug('FoundsetDatasource.getRows: params = ', params);

        this.dataGrid.isDataLoading = true;

        // the row group cols, ie the cols that the user has dragged into the 'group by' zone, eg 'Country' and 'Customerid'
        const rowGroupCols = params.request.rowGroupCols;
        // the keys we are looking at. will be empty if looking at top level (either no groups, or looking at top level groups). eg ['United States','2002']
        const groupKeys = params.request.groupKeys;

        // resolve valuelist display values to real values
        const filterPromises = [];

        function handleFilterCallback(groupKeys, idx, valuelistValues) {
            if(valuelistValues) {
                for (let i = 0; i < valuelistValues.length; i++) {
                    if (valuelistValues[i].displayValue == groupKeys[idx] && valuelistValues[i].realValue != undefined) {
                        groupKeys[idx] = valuelistValues[i].realValue;
                        break;
                    }
                }
            }
        }

        let removeAllFoundsetRefPostponed = false;
        const _this = this;
        for (let i = 0; i < groupKeys.length; i++) {
            if (groupKeys[i] == NULL_VALUE) {
                groupKeys[i] = null;	// reset to real null, so we use the right value for grouping
            } else {
                const vl = this.dataGrid.getValuelistEx(params.parentNode.data, rowGroupCols[i]['id']);
                if(vl) {
                    filterPromises.push(vl.filterList(groupKeys[i]));
                    const idx = i;
                    filterPromises[filterPromises.length - 1].then(function(valuelistValues) {
                        handleFilterCallback(groupKeys, idx, valuelistValues);
                        if(_this.dataGrid.removeAllFoundsetRef) {
                            _this.dataGrid.groupManager.removeFoundsetRefAtLevel(0);
                        }
                    });
                    removeAllFoundsetRefPostponed = true;
                }
            }
        }

        if(this.dataGrid.removeAllFoundsetRef && !removeAllFoundsetRefPostponed) {
            this.dataGrid.groupManager.removeFoundsetRefAtLevel(0);
        }

        const allPromisses = this.dataGrid.sortHandlerPromises.concat(filterPromises);
        Promise.all(allPromisses).then(function() {
            _this.dataGrid.removeAllFoundsetRef = false;
            _this.foundsetServer.getData(params.request, groupKeys,
                function successCallback(resultForGrid, lastRow) {
                    params.successCallback(resultForGrid, lastRow);

                    // if row autoHeight is on, we need to refresh first time the data are loaded, that means,
                    // the first block has the state == "loaded"
                    if(_this.dataGrid.isRefreshNeededForAutoHeight) {
                        const model = this.dataGrid.agGrid.api.getModel();
                        if(model.rootNode.childrenCache) {
                            const sortedBlockIds = model.rootNode.childrenCache.getBlockIdsSorted();
                            if(sortedBlockIds.length) {
                                const firstBlock = model.rootNode.childrenCache.getBlock(sortedBlockIds[0]);
                                if(firstBlock.state == 'loaded') {
                                    _this.dataGrid.isRefreshNeededForAutoHeight = false;
                                    setTimeout(function() {
                                        _this.dataGrid.purgeImpl();
                                    }, 150);
                                    return;
                                }
                            }
                        }
                    }

                    _this.dataGrid.isDataLoading = false;
                    // if selection did not changed, mark the selection ready
                    if(!_this.dataGrid.selectedRowIndexesChanged()) {
                        _this.dataGrid.isSelectionReady = true;
                    }
                    // rows are rendered, if there was an editCell request, now it is the time to apply it
                    if(_this.dataGrid.startEditFoundsetIndex > -1 && _this.dataGrid.startEditColumnIndex > -1) {
                        _this.dataGrid.editCellAtWithTimeout(_this.dataGrid.startEditFoundsetIndex, _this.dataGrid.startEditColumnIndex);
                    }


                    // Preserve Group State
                    // https://www.ag-grid.com/javascript-grid-server-side-model-grouping/#preserving-group-state

                    let expandedState = _this.dataGrid._internalExpandedState;
                    const groupFields = _this.dataGrid.state.expanded.fields;

                    if (resultForGrid && resultForGrid.length && _this.dataGrid.isTableGrouped() && groupFields && expandedState) {

                        // get the fs manager for the group
                        //var foundsetRefManager = getFoundsetManagerByFoundsetUUID(resultForGrid[0]._svyFoundsetUUID);

                        // to preserve group state we expand any previously expanded groups for this block
                        for (let i = 0; i < resultForGrid.length; i++) {

                            const row = resultForGrid[i];
                            try {

                                // get group levels, in order
//											var groupedColumns = gridOptions.columnApi.getRowGroupColumns();
//											var groupFields = [];
//											for (var j = 0; j < groupedColumns.length; j++) {
//												groupFields.push(groupedColumns[j].colDef.field);
//											}


                                // TODO do i need to retrieve the node before to know if column is expanded or not ?
                                const node = _this.dataGrid.agGrid.api.getRowNode(row._svyFoundsetUUID + '_' + row._svyFoundsetIndex);
                                if (!node) break;

                                const rowGroupInfo = _this.dataGrid.getNodeGroupInfo(node);
                                const rowGroupFields = rowGroupInfo.rowGroupFields;
                                const rowGroupKeys = rowGroupInfo.rowGroupKeys;

                                // check if node is expanded
                                let isExpanded;


                                // check if the expanded columns matches the expanded columns in cache
//											for (var j = 0; j < rowGroupFields.length; j++) {
//												if (rowGroupFields[j] != groupFields[j]) {
//													isExpanded = false;
//													break;
//												}
//											}
//											if (isExpanded === false) {
//												break;
//											}

                                // check if the node is expanded
                                expandedState = _this.dataGrid._internalExpandedState;

                                for (let j = 0; expandedState && j < rowGroupKeys.length; j++) {
                                    expandedState = expandedState[rowGroupKeys[j]];
                                    if (!expandedState) {
                                        isExpanded = false;
                                        break;
                                    } else {
                                        isExpanded = true;
                                    }
                                }

                                // expand the node
                                if (isExpanded) {
                                    node.setExpanded(true);
                                }

                            } catch (e) {
                                console.log(e);
                            }
                        }
                    }
                });
        }, function(reason) {
            _this.dataGrid.log.error('Can not get realValues for groupKeys ' + reason);
        });
    };
}

class GroupManager {

    hashTree: GroupHashCache;

    groupedColumns = [];
    groupedValues = new Object();

    constructor(public dataGrid: DataGrid) {
        this.hashTree = new GroupHashCache(this.dataGrid);
    }

    /**
     * Returns the foundset with the given grouping criteria is already exists in cache
     *
     * @param rowGroupCols
     * @param groupKeys
     * @param [sort] desc or asc. Default asc
     *
     * @return returns the UUID of the foundset if exists in cache
     *
     */
    getCachedFoundsetUUID(rowGroupCols, groupKeys) {
        return this.hashTree.getCachedFoundset(rowGroupCols, groupKeys);
    }

    /**
     * Returns the foundset with the given grouping criteria
     *
     * @param rowGroupCols
     * @param groupKeys
     * @param [sort] desc or asc. Default asc
     *
     * @return returns a promise
     *
     */
    getFoundsetRef(rowGroupCols, groupKeys, sort?) {

        // create a promise
        const resultPromise = new Deferred();

        // return the root foundset if no grouping criteria
        if (rowGroupCols.length === 0 && groupKeys.length === 0) { // no group return root foundset
            resultPromise.resolve('root');
            return resultPromise.promise;
        }

        let idx; // the index of the group dept
        let columnIndex; // the index of the grouped column

        // ignore rowGroupColumns which are still collapsed (don't have a matchig key)
        rowGroupCols = rowGroupCols.slice(0, groupKeys.length + 1);

        // possibilities

        // is a root group CustomerID

        // is a second level group CustomerID, ShipCity

        // is a third level group CustomerID, ShipCity, ShipCountry

        // recursevely load hashFoundset. this is done so the whole tree is generated without holes in the structure. Do i actually need to get a foundset for it ? Probably no, can i simulate it ?

        const groupLevels = rowGroupCols.length;

        // create groups starting from index 0
        getRowColumnHashFoundset(0);

        function getRowColumnHashFoundset(index) {

            const groupCols = rowGroupCols.slice(0, index + 1);
            const keys = groupKeys.slice(0, index + 1);

            this.dataGrid.log.debug(groupCols);
            this.dataGrid.log.debug(keys);

            // get a foundset for each grouped level, resolve promise when got to the last level

            // TODO loop over columns
            let columnId = groupCols[groupCols.length - 1].field; //
            columnIndex = this.dataGrid.getColumnIndex(columnId);

            // get the foundset Reference
            const foundsetHash = this.dataGrid.hashTree.getCachedFoundset(groupCols, keys);
            if (foundsetHash) { // the foundsetReference is already cached
                if (index === rowGroupCols.length - 1) { // resolve when last rowColumn foundset has been loaded
                    resultPromise.resolve(foundsetHash);
                } else {
                    // FIXME do i need to get multiple hashed foundsets ? probably not
                    getRowColumnHashFoundset(index + 1); // load the foundset for the next group
                }

            } else { // need to get a new foundset reference
                // create the subtree

                // FIXME i will miss information about the root columns. I need an array of matching column, not an index. e.g. [ALFKI, Italy, Roma]

                // get the index of each grouped column
                const groupColumnIndexes = [];
                for (let idx = 0; idx < groupCols.length; idx++) {
                    columnId = rowGroupCols[idx].field;
                    columnIndex = this.dataGrid.getColumnIndex(columnId);
                    groupColumnIndexes.push(columnIndex);
                }

                if (index === groupLevels - 1) { // if is the last level, ask for the foundset hash
                    const promise = this.getHashFoundset(groupColumnIndexes, keys, sort);
                    promise.then(getHashFoundsetSuccess);
                    promise.catch(promiseError);
                } else { // set null inner foundset
                    this.dataGrid.hashTree.setCachedFoundset(groupCols, keys, null);
                    getRowColumnHashFoundset(index + 1);
                }
            }

            /**
             * @return returns the foundsetRef object
             */
            function getHashFoundsetSuccess(foundsetUUID) {

                if (!foundsetUUID) {
                    this.dataGrid.log.error('why i don\'t have a foundset ref ?');
                    return;
                } else {
                    this.dataGrid.debug('Get hashed foundset success ' + foundsetUUID);
                }

                // the hash of the parent foundset
                // var foundsetUUID = childFoundset.foundsetUUID;
                // var foundsetRef = childFoundset.foundsetRef;

                // cache the foundsetRef
                this.dataGrid.hashTree.setCachedFoundset(groupCols, keys, foundsetUUID);

                this.dataGrid.log.debug('success ' + foundsetUUID);

                if (index === rowGroupCols.length - 1) { // resolve when last rowColumn foundset has been loaded
                    resultPromise.resolve(foundsetUUID);
                } else {
                    getRowColumnHashFoundset(index + 1); // load the foundset for the next group
                }
            }

        }

        function promiseError(e) {
            // propagate the error
            resultPromise.reject(e);
        }

        return resultPromise.promise;
    }

    /**
     * Handle ChildFoundsets
     * Returns the foundset in a promise
     *
     * @param groupColumns index of all grouped columns
     * @param groupKeys value for each grouped column
     * @param [sort]
     *
     * @return
     *
     */
    getHashFoundset(groupColumns, groupKeys, sort) {

        const resultDeferred = new Deferred();

        let childFoundsetPromise;

        // TODO store it in cache. Requires to be updated each time column array Changes
        const idForFoundsets = [];
        for (let i = 0; i < this.dataGrid.columns.length; i++) {
            idForFoundsets.push(this.dataGrid.getColumnID(this.dataGrid.columns[i], i));
        }

        const hasRowStyleClassDataprovider = this.dataGrid.rowStyleClassDataprovider ? true : false;

        let sortColumn;
        let sortColumnDirection;
        const sortModel = this.dataGrid.agGrid.api.getSortModel();
        if(sortModel && sortModel[0]) {
            sortColumn = this.dataGrid.getColumnIndex(sortModel[0].colId);
            sortColumnDirection = sortModel[0].sort;
        }

        const _this = this;
        childFoundsetPromise = this.dataGrid.servoyApi.callServerSideApi('getGroupedFoundsetUUID',
            [groupColumns, groupKeys, idForFoundsets, sort, this.dataGrid.filterModel, hasRowStyleClassDataprovider, sortColumn, sortColumnDirection]);

        childFoundsetPromise.then(function(childFoundsetUUID) {
            _this.dataGrid.log.debug(childFoundsetUUID);
                if (!childFoundsetUUID) {
                    _this.dataGrid.log.error('why i don\'t have a childFoundset ?');
                    resultDeferred.reject('can\'t retrieve the child foundset');
                }

                // FIXME add listener somewhere else
                //childFoundset.addChangeListener(childChangeListener);
                resultDeferred.resolve(childFoundsetUUID);
            }, function(e) {
                // propagate the error
                resultDeferred.reject(e);
            });

        return resultDeferred.promise;
    }

    updateFoundsetRefs(rowGroupCols) {
        // TODO update all foundset refs
        // results in closing all nodes and refresh all foundsets
        this.clearAll();
        return this.getFoundsetRef([rowGroupCols[0].colDef], []);
    }

    /**
     * Creates a new foundset reference with the given group criterias.
     * If a foundset reference with the given references already exists, will be overriden
     *
     * */
    createOrReplaceFoundsetRef(groupColumns, groupKeys, sort) {
        const foundsetHash = this.hashTree.getCachedFoundset(groupColumns, groupKeys);
        if (foundsetHash) {
            this.removeFoundsetRef(foundsetHash);

        }
        return this.getFoundsetRef(groupColumns, groupKeys, sort);
    }
    /**
     * @private
     * Should this method be used ?
     *  */
    removeFoundsetRef(foundsetUUID) {
        return this.hashTree.removeCachedFoundset(foundsetUUID);
    }

    /**
     * @param level
     *
     *
     */
    removeFoundsetRefAtLevel(level) {
        return this.hashTree.removeCachedFoundsetAtLevel(level);
    }

    /**
     * @param foundsetUUID
     * @param [field] if given delete only the child having field equal to value
     * @param [value] if given delete only the child having field equal to value
     *
     *
     */
    removeChildFoundsetRef(foundsetUUID, field, value) {
        return this.hashTree.removeChildFoundset(foundsetUUID, field, value);
    }

    clearAll() {
        this.hashTree.clearAll();
    }
}

/**
 * This object is used to keep track of cached foundset depending on rowGroupCol and groupKeys criteria.
 * Any time a foundset is retrieved is persisted in this object.
 *
 * Question: can i use an hash instead of a tree structure ? e.g hash of columnName:keyValue,columnName:keyValue..
 *
 * TODO is not stateful (lost once is refreshed) while the foundset are statefull, potentially can create memory leaks (too many foundset for the same criteria retrieved)
 * TODO desist foundset from memory. Remove foundset
 * 		Clear ALL
 * 		Clear Node
 * 		Clear ALL subnodes
 * */
class GroupHashCache {

    rootGroupNode: GroupNode;

    constructor(public dataGrid: DataGrid) {
        this.rootGroupNode = new GroupNode(this.dataGrid, 'root');
    }

    getCachedFoundset(rowGroupCols, groupKeys) {
        const node = this.getTreeNode(this.rootGroupNode, rowGroupCols, groupKeys);
        return node ? node.foundsetUUID : null;
    }

    setCachedFoundset(rowGroupCols, groupKeys, foundsetUUID) {
        const tree = this.getTreeNode(this.rootGroupNode, rowGroupCols, groupKeys, true);
        tree.foundsetUUID = foundsetUUID;
    }

    /**
     * @param foundsetUUID
     * Remove the node
     */
    removeCachedFoundset(foundsetUUID) {
        return this.removeFoundset(this.rootGroupNode, foundsetUUID);
    }

    /**
     * @param level
     * Remove the node
     */
    removeCachedFoundsetAtLevel(level) {
        return this.removeFoundsetAtLevel(this.rootGroupNode, level);
    }

    /**
     * @param foundsetUUID
     * @param [field]
     * @param [value]
     * Remove all it's child node
     */
    removeChildFoundset(foundsetUUID, field, value) {
        return this.removeChildFoundsets(this.rootGroupNode, foundsetUUID, field, value);
    }

    /** @deprecated
     * Use removeFoundsetRefAtLevel(0) instead
     *  */
    clearAll() {

        const _this = this;
        this.rootGroupNode.forEach(function(node) {
            if (node.foundsetUUID) {
                _this.removeFoundset(_this.rootGroupNode, node.foundsetUUID);
            } else {
                // TODO is it this possible
                _this.dataGrid.log.error('There is a root node without a foundset UUID, it should not happen');
            }

        });
        if (this.dataGrid.hashedFoundsets.length > 0) {
            this.dataGrid.log.error('Clear All was not successful, please debug');
        }
    }

    /**
     * @param tree
     * @param foundsetUUID
     * @return Boolean
     *
     */
    removeFoundset(tree, foundsetUUID) {
        if (!tree) {
            return true;
        }

        if (!foundsetUUID) {
            return true;
        }

        // remove the node
        const parentNode = this.getParentGroupNode(tree, foundsetUUID);
        const node = this.getGroupNodeByFoundsetUUID(parentNode, foundsetUUID);
        if (parentNode && node) {
            node.destroy();
            // TODO should be moved inside the destroy method ?, each time should ask for each parent
            delete parentNode.nodes[node.id];
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param tree
     * @param level
     * @return
     *
     */
    removeFoundsetAtLevel(tree, level) {
        if (!tree) {
            return true;
        }

        if (isNaN(level) || level === null) {
            return true;
        }

        let success = true;

        const _this = this;
        tree.forEach(function(node) {

            // remove the foundset and all it's child nodes if foundsetUUID or level === 0
            if (level === 0) {
                const id = node.id;
                node.destroy();
                delete tree.nodes[id];
                return true;
            } else {
                success = node.forEach(function(subNode) {
                    return _this.removeFoundsetAtLevel(node, level - 1);
                }) && success;
                return success;
            }
        });
        return success;
    }

    /**
     * @param tree
     * @param foundsetUUID
     * @param [field]
     * @param [value]
     *
     */
    removeChildFoundsets(tree, foundsetUUID, field?, value?) {

        if (foundsetUUID) {
            // remove all child nodes
            const node = this.getGroupNodeByFoundsetUUID(tree, foundsetUUID);
            if (node) {
                node.removeAllSubNodes();
                return true;
            } else {
                return false;
            }
        } else {

            // TODO Refactor this part of code
            let success = true;
            const _this = this;
            tree.forEach(function(node) {
                if (node.foundsetUUID === foundsetUUID) {
                    // delete all subnodes
                    success = true;
                    node.forEach(function(subNode) {
                        const childFoundsetUUID = subNode.foundsetUUID;
                        const foundsetRef = _this.dataGrid.getFoundsetManagerByFoundsetUUID(childFoundsetUUID);
                        // FIXME this solution is horrible, can break if rows.length === 0 or...
                        // A better solution is to retrieve the proper childFoundsetUUID by rowGroupCols/groupKeys
                        if (foundsetRef && ( (field === null || field === undefined) || (field !== null && field !== undefined && foundsetRef.foundset.viewPort.rows[0] && foundsetRef.foundset.viewPort.rows[0][field] == value))) {
                            success = (_this.removeFoundset(node, childFoundsetUUID) && success);
                        } else {
                            _this.dataGrid.log.debug('ignore the child foundset');
                        }
                    });
                } else if (node.hasNodes()) { // search in subnodes
                    success = success && node.forEachUntilSuccess(function(subNode) {
                        return _this.removeChildFoundsets(node, foundsetUUID);
                    });
                }
            });
        }
    }

    /**
     * @param tree
     * @param rowGroupCols
     * @param groupKeys
     * @param [create]
     *
     * @return
     *
     *
     */
    getTreeNode(tree, rowGroupCols, groupKeys, create?) {

        let result = null;

        if (rowGroupCols.length > groupKeys.length + 1) {
            //							$log.warn('discard row groups ' + (rowGroupCols.length - groupKeys.length));
            rowGroupCols = rowGroupCols.slice(0, groupKeys.length + 1);
        }

        /*
         * {
         * 	columnId {
         * 		foundsetUUID: uuid
         * 		nodes: {
         * 			keyValue : {
         * 				foundsetUUID : uuid
         * 				nodes : {
         * 					subColumnId { ... }
         * 				}
         * 			},
         * 			keyValue2 : { ... }
         * 		}
         * 	  }
         * }
         *
         *
         * */

        if (!tree || !tree.nodes) {
            return null;
        }

        // the column id e.g. customerid, shipcity
        const columnId = rowGroupCols[0].field;

        // the tree for the given column
        let colTree = tree.nodes[columnId];

        // create the tree node if does not exist
        if (!colTree && create) {
            colTree = new GroupNode(this.dataGrid, columnId);
            tree.nodes[columnId] = colTree;
        } else if (!colTree) { // or return null
            return null;
        }

        if (rowGroupCols.length === 1) { // the last group

            if (groupKeys.length === 0) { // is a leaf child
                result = colTree;
            } else if (groupKeys.length === 1) { // is a leaf child

                // get the subtree matching the rowGroupCols
                const key = groupKeys[0];
                let keyTree = colTree.nodes[key];

                // create the key tree node if does not exist
                if (!keyTree && create) {
                    keyTree = new GroupNode(this.dataGrid, key);
                    colTree.nodes[key] = keyTree;
                } else if (!keyTree) { // or return null
                    return null;
                }

                result = keyTree;

            } else { // no group key criteria
                this.dataGrid.log.warn('this should not happen');
            }

        } else if (rowGroupCols.length > 1) { // is not the last group
            const key = groupKeys.length ? groupKeys[0] : null;

            if (!colTree) {
                this.dataGrid.log.warn('this should not happen');
                return null;
            }

            let subTree = colTree;

            if (key !== null) {
                let keyTree = colTree.nodes[key];

                // create the key tree node if does not exist
                if (!keyTree && create) {
                    keyTree = new GroupNode(this.dataGrid, key);
                    colTree.nodes[key] = keyTree;
                } else if (!keyTree) {
                    return null;
                }

                subTree = keyTree;

            } else {
                // if is not the last group, should always have a key criteria
                this.dataGrid.log.warn('this should not happen');
            }

            rowGroupCols = rowGroupCols.slice(1);
            groupKeys = groupKeys.slice(1);

            result = this.getTreeNode(subTree, rowGroupCols, groupKeys, create);

        } else {
            this.dataGrid.log.warn('No group criteria, should not happen');
        }

        return result;
    }

    /**
     * @param tree
     * @param foundsetUUID
     * @return
     *
     *
     */
    getGroupNodeByFoundsetUUID(tree: GroupNode, foundsetUUID: string): GroupNode {
        if (!tree) {
            return null;
        }

        if (!foundsetUUID) {
            return null;
        }

        let resultNode = null;
        const _this = this;
        tree.forEachUntilSuccess(function(node) {
            if (node.foundsetUUID === foundsetUUID) {
                resultNode = node;
                return true;
            } else if (node.hasNodes()) { // search in subnodes
                return node.forEachUntilSuccess(function(subNode) {
                    resultNode = _this.getGroupNodeByFoundsetUUID(node, foundsetUUID);
                    if (resultNode) { // if has found the result
                        return true;
                    } else { // keep searching
                        return false;
                    }
                });
            } else { // didn't find the node in all it's childs
                return false;
            }
        });
        return resultNode;
    }

    /**
     * @param tree
     * @param foundsetUUID
     * @return
     *
     *
     */
    getParentGroupNode(tree, foundsetUUID) {
        if (!tree) {
            return null;
        }

        if (!foundsetUUID) {
            return null;
        }

        let parentNode = null;
        const _this = this;
        tree.forEachUntilSuccess(function(node) {
            // found in the child
            if (parentNode) { // already found the tree
                return true;
            }
            if (node.foundsetUUID === foundsetUUID) {
                parentNode = tree;
                return true;
            } else if (node.hasNodes()) { // search in subnodes
                node.forEachUntilSuccess(function(subNode) {
                    parentNode = _this.getParentGroupNode(node, foundsetUUID);
                    if (parentNode) { // break the for each if has found the result
                        return true;
                    } else { // keep searching
                        return false;
                    }
                });
            } else if (parentNode) {
                return true;
            } else { // didn't find the node in all it's childs
                return false;
            }
        });
        return parentNode;
    }

    /**
     * @param tree
     * @param foundsetUUID
     * @return
     *
     * @deprecated
     */
    getTreeNodePath(tree, foundsetUUID) {
        if (!tree) {
            return null;
        }

        if (!foundsetUUID) {
            return null;
        }

        const path = [];

        const resultNode = null;
        const _this = this;
        tree.forEachUntilSuccess(function(node) {
            if (node.foundsetUUID === foundsetUUID) {
                path.push(node);
                return true;
            } else if (node.hasNodes()) { // search in subnodes
                let subPath;
                const isInSubNodes = node.forEachUntilSuccess(function(subNode) {
                    subPath = _this.getTreeNodePath(node, foundsetUUID);
                    if (resultNode) { // if has found the result
                        return true;
                    } else { // keep searching
                        return false;
                    }
                });

                if (isInSubNodes) {
                    path.concat(subPath);
                }

            } else { // didn't find the node in all it's childs
                return false;
            }
        });

        return path;
    }
}

class GroupNode {

    nodes = new Object();
    foundsetUUID = undefined;

    constructor(public dataGrid: DataGrid, public id: string) {
    }

    /**
     * @public
     * @param callback execute function for each subnode. Arguments GroupNode
     *
     */
    forEach(callback) {
        for (const key in this.nodes) {
            callback.call(this, this.nodes[key]);
        }
    }

    /**
     * @public
     * @return returns true if the callback ever returns true
     * @param callback execute function for each subnode until returns true. Arguments GroupNode
     *
     */
    forEachUntilSuccess(callback) {
        for (const key in this.nodes) {
            if (callback.call(this, this.nodes[key]) === true) {
                return true;
            }
        }
        // return true only if there are no subnodes ?
        return false;
    }

    /**
     * @public
     * @return returns true if the callback ever returns true
     *
     */
    hasNodes() {
        for (const key in this.nodes) {
            return true;
        }
        return false;
    }

    /**
     * @public
     * @remove the node
     * */
    destroy() {

        this.dataGrid.log.debug('--Destroy ' + this.foundsetUUID + ' - id : ' + this.id);
        // destroy all it's sub nodes
        this.removeAllSubNodes();

        // do nothing if the foundset doesn't exist
        if (this.foundsetUUID && this.foundsetUUID !== 'root') {
            // TODO should this method access the foundsetManager ? is not a good encapsulation
            //		if (this.onDestroy) {
            //			this.onDestroy.call(this, [this.id, this.foundsetUUID]);
            //		}
            const foundsetManager = this.dataGrid.getFoundsetManagerByFoundsetUUID(this.foundsetUUID);
            foundsetManager.destroy();
        }
    }

    removeAllSubNodes() {
        this.forEach(function(subNode) {
            subNode.destroy();
        });
        this.nodes = [];
    }
}
