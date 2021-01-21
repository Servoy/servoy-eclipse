import { AgGridAngular } from '@ag-grid-community/angular';
import { GridOptions } from '@ag-grid-community/core';
import { ChangeDetectorRef, Component, ElementRef, Input, Renderer2, ViewChild } from '@angular/core';
import { ServoyBaseComponent } from '../../ngclient/servoy_public';
import { LoggerFactory, LoggerService } from '../../sablo/logger.service';
import { PowergridService } from './powergrid.service';

const TABLE_PROPERTIES_DEFAULTS = {
    rowHeight: { gridOptionsProperty: 'rowHeight', default: 25 },
    headerHeight: { gridOptionsProperty: 'headerHeight', default: 33 },
    multiSelect: { gridOptionsProperty: 'rowSelection', default: false }
};

const COLUMN_PROPERTIES_DEFAULTS = {
    id: { colDefProperty: 'colId', default: null },
    headerTitle: { colDefProperty: 'headerName', default: null },
    headerTooltip: { colDefProperty: 'headerTooltip', default: null },
    headerStyleClass: { colDefProperty: 'headerClass', default: null },
    tooltip: {colDefProperty: 'tooltipField', default: null},
    styleClass: { colDefProperty: 'cellClass', default: null },
    enableRowGroup: { colDefProperty: 'enableRowGroup', default: true },
    rowGroupIndex: { colDefProperty: 'rowGroupIndex', default: -1 },
    enablePivot: { colDefProperty: 'enablePivot', default: false },
    pivotIndex: { colDefProperty: 'pivotIndex', default: -1 },
    aggFunc: { colDefProperty: 'aggFunc', default: '' },
    width: { colDefProperty: 'width', default: 0 },
    enableToolPanel: { colDefProperty: 'suppressToolPanel', default: true },
    maxWidth: { colDefProperty: 'maxWidth', default: null },
    minWidth: { colDefProperty: 'minWidth', default: null },
    visible: { colDefProperty: 'hide', default: true },
    enableResize: { colDefProperty: 'resizable', default: true },
    autoResize: { colDefProperty: 'suppressSizeToFit', default: true },
    enableSort: { colDefProperty: 'sortable', default: true },
    cellStyleClassFunc: { colDefProperty: 'cellClass', default: null },
    cellRendererFunc: { colDefProperty: 'cellRenderer', default: null }
};

@Component({
    selector: 'aggrid-datasettable',
    templateUrl: './powergrid.html'
})
export class PowerGrid extends ServoyBaseComponent<HTMLDivElement> {

    @ViewChild('element') agGrid: AgGridAngular;
    @ViewChild('element', { read: ElementRef }) agGridElementRef: ElementRef;

    @Input() columns;
    @Input() styleClass: string;

    @Input() toolPanelConfig;
    @Input() iconConfig;
    @Input() localeText;
    @Input() mainMenuItemsConfig;
    @Input() gridOptions;
    @Input() showColumnsMenuTab;

    log: LoggerService;
    agGridOptions: GridOptions;
    agMainMenuItemsConfig;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, logFactory: LoggerFactory,
        private powergridService: PowergridService) {
        super(renderer, cdRef);
        this.log = logFactory.getLogger('PowerGrid');
    }

    ngOnInit() {
        super.ngOnInit();
        // if nggrids service is present read its defaults
        let toolPanelConfig = this.powergridService.toolPanelConfig ? this.powergridService.toolPanelConfig : null;
        let iconConfig = this.powergridService.iconConfig ? this.powergridService.iconConfig : null;
        let userGridOptions = this.powergridService.gridOptions ? this.powergridService.gridOptions : null;
        let localeText = this.powergridService.localeText ? this.powergridService.localeText : null;
        const mainMenuItemsConfig = this.powergridService.mainMenuItemsConfig ? this.powergridService.mainMenuItemsConfig : null;

        toolPanelConfig = this.mergeConfig(toolPanelConfig, this.toolPanelConfig);
        iconConfig = this.mergeConfig(iconConfig, this.iconConfig);
        userGridOptions = this.mergeConfig(userGridOptions, this.gridOptions);
        localeText = this.mergeConfig(localeText, this.localeText);
        this.agMainMenuItemsConfig = this.mergeConfig(mainMenuItemsConfig, this.mainMenuItemsConfig);

        const vMenuTabs = ['generalMenuTab','filterMenuTab'];

        if(this.showColumnsMenuTab) vMenuTabs.push('columnsMenuTab');

        let sideBar;
        if (toolPanelConfig && toolPanelConfig.suppressSideButtons === true) {
            sideBar = false;
        } else {
            sideBar = {
                toolPanels: [
                {
                    id: 'columns',
                    labelDefault: 'Columns',
                    labelKey: 'columns',
                    iconKey: 'columns',
                    toolPanel: 'agColumnsToolPanel',
                    toolPanelParams: {
                        suppressRowGroups: toolPanelConfig ? toolPanelConfig.suppressRowGroups : false,
                        suppressValues: toolPanelConfig ? toolPanelConfig.suppressValues : false,
                        suppressPivots: toolPanelConfig ? toolPanelConfig.suppressPivots : false,
                        suppressPivotMode: toolPanelConfig ? toolPanelConfig.suppressPivotMode : false,
                        suppressSideButtons: toolPanelConfig ? toolPanelConfig.suppressSideButtons : false,
                        suppressColumnFilter: toolPanelConfig ? toolPanelConfig.suppressColumnFilter : false,
                        suppressColumnSelectAll: toolPanelConfig ? toolPanelConfig.suppressColumnSelectAll : false,
                        suppressColumnExpandAll: toolPanelConfig ? toolPanelConfig.suppressColumnExpandAll : false
                    }
                }
            ]};
        }

        //const columnDefs = this.getColumnDefs();
    }

    svyOnInit() {
        super.svyOnInit();
    }

    // getColumnDefs() {
    //     //create the column definitions from the specified columns in designer
    //     const colDefs = [];
    //     let colDef: any = { };
    //     const colGroups = { };
    //     let column;
    //     for (let i = 0; this.columns && i < this.columns.length; i++) {
    //         column = this.columns[i];

    //         //create a column definition based on the properties defined at design time
    //         colDef = {
    //             headerName: "" + (column["headerTitle"] ? column["headerTitle"] : "") + "",
    //             headerTooltip: column["headerTooltip"] ? column["headerTooltip"] : null,
    //             field: column["dataprovider"],
    //             tooltipField: column["tooltip"] ? column["tooltip"] : null
    //         };

    //         // set id if defined
    //         if(column.id) {
    //             colDef.colId = column.id;
    //         }

    //         // styleClass
    //         colDef.headerClass = 'ag-table-header ' + column.headerStyleClass;
    //         colDef.cellClass = 'ag-table-cell ' + column.styleClass;


    //         // column grouping & pivoting
    //         colDef.enableRowGroup = column.enableRowGroup;
    //         if (column.rowGroupIndex >= 0) colDef.rowGroupIndex = column.rowGroupIndex;

    //         colDef.enablePivot = column.enablePivot;
    //         if (column.pivotIndex >= 0) colDef.pivotIndex = column.pivotIndex;

    //         if(column.aggFunc) colDef.aggFunc = column.aggFunc;
    //         // tool panel
    //         if (column.enableToolPanel === false) colDef.suppressToolPanel = !column.enableToolPanel;

    //         // column sizing
    //         if (column.width || column.width === 0) colDef.width = column.width;
    //         if (column.maxWidth) colDef.maxWidth = column.maxWidth;
    //         if (column.minWidth || column.minWidth === 0) colDef.minWidth = column.minWidth;

    //         // column resizing https://www.ag-grid.com/javascript-grid-resizing/
    //         if (column.enableResize === false) colDef.resizable = column.enableResize;
    //         if (column.autoResize === false) colDef.suppressSizeToFit = !column.autoResize;
    //         // sorting
    //         if (column.enableSort === false) colDef.sortable = false;
    //         // visibility
    //         if (column.visible === false) colDef.hide = true;
    //         if (column.format) {
    //             var parsedFormat = column.format;
    //             if(column.formatType == 'DATETIME') {
    //                 var useLocalDateTime = false;
    //                 try {
    //                     var jsonFormat = JSON.parse(column.format);
    //                     parsedFormat = jsonFormat.displayFormat;
    //                     useLocalDateTime = jsonFormat.useLocalDateTime;
    //                 }
    //                 catch(e){}
    //                 if(useLocalDateTime) {
    //                     colDef.valueGetter = function(params) {
    //                         var field = params.colDef.field;
    //                         if (field && params.data) {
    //                             return new Date(params.data[field]);
    //                         }
    //                         return undefined;
    //                     };
    //                 }
    //             }
    //             colDef.valueFormatter = createValueFormatter(parsedFormat, column.formatType);
    //         }

    //         if(column.cellStyleClassFunc) {
    //             colDef.cellClass = createColumnCallbackFunctionFromString(column.cellStyleClassFunc);
    //         }

    //         if(column.cellRendererFunc) {
    //             colDef.cellRenderer = createColumnCallbackFunctionFromString(column.cellRendererFunc);
    //         }
    //         else {
    //             colDef.cellRenderer = getDefaultCellRenderer(column);
    //         }

    //         if (column.filterType) {
    //             colDef.filter = true;

    //             if(column.filterType == 'TEXT') {
    //                 colDef.filter = 'agTextColumnFilter';
    //             }
    //             else if(column.filterType == 'NUMBER') {
    //                 colDef.filter = 'agNumberColumnFilter';
    //             }
    //             else if(column.filterType == 'DATE') {
    //                 colDef.filter = 'agDateColumnFilter';
    //             }
    //             colDef.filterParams = { applyButton: true, clearButton: true, newRowsAction: 'keep', suppressAndOrCondition: true, caseSensitive: false };
    //         }

    //         if (column.editType) {
    //             colDef.editable = !$scope.model.readOnly && (column.editType != 'CHECKBOX');

    //             if(column.editType == 'TEXTFIELD') {
    //                 colDef.cellEditor = getTextEditor();
    //             }
    //             else if(column.editType == 'DATEPICKER') {
    //                 colDef.cellEditor = getDatePicker();
    //             }
    //             else if(column.editType == 'FORM') {
    //                 colDef.cellEditor = getFormEditor();
    //             }

    //             colDef.onCellValueChanged = onCellValueChanged;
    //         }

    //         var columnOptions = {};
    //         if($injector.has('ngPowerGrid')) {
    //             var datasettableDefaultConfig = $services.getServiceScope('ngPowerGrid').model;
    //             if(datasettableDefaultConfig.columnOptions) {
    //                 columnOptions = datasettableDefaultConfig.columnOptions;
    //             }
    //         }

    //         columnOptions = mergeConfig(columnOptions, column.columnDef);

    //         if(columnOptions) {
    //             var colDefSetByComponent = {};
    //             for( var p in COLUMN_PROPERTIES_DEFAULTS) {
    //                 if(COLUMN_PROPERTIES_DEFAULTS[p]["default"] != column[p]) {
    //                     colDefSetByComponent[COLUMN_PROPERTIES_DEFAULTS[p]["colDefProperty"]] = true;
    //                 }
    //             }
    //             for (var property in columnOptions) {
    //                 if (columnOptions.hasOwnProperty(property) && !colDefSetByComponent.hasOwnProperty(property)) {
    //                     colDef[property] = columnOptions[property];
    //                 }
    //             }
    //         }

    //         if(column.headerGroup) {
    //             if(!colGroups[column.headerGroup]) {
    //                 colGroups[column.headerGroup] = {}
    //                 colGroups[column.headerGroup]['headerClass'] = column.headerGroupStyleClass;
    //                 colGroups[column.headerGroup]['children'] = [];

    //             }
    //             colGroups[column.headerGroup]['children'].push(colDef);
    //         }
    //         else {
    //             colDefs.push(colDef);
    //         }
    //     }

    //     for(var groupName in colGroups) {
    //         var group = {};
    //         group.headerName = groupName;
    //         group.headerClass = colGroups[groupName]['headerClass'];
    //         group.children = colGroups[groupName]['children'];
    //         colDefs.push(group);
    //     }

    //     return colDefs;
    // }

    mergeConfig(target, source) {
        let property;

        // clone target to avoid side effects
        let mergeConfig = {};
        for (property in target) {
            if(target.hasOwnProperty(property)) {
                mergeConfig[property] = target[property];
            }
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

}
