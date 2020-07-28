import { Component, ViewChild, Input, Renderer2, ElementRef, AfterViewInit, EventEmitter, Output, OnDestroy } from '@angular/core';
import { ServoyBaseComponent } from '../../ngclient/servoy_public'
import { IFoundset } from '../../sablo/spectypes.service';
import { LoggerFactory, LoggerService } from '../../sablo/logger.service';
import { ResizeEvent } from 'angular-resizable-element';
import { FoundsetChangeEvent } from '../../ngclient/converters/foundset_converter';
import { CdkVirtualScrollViewport } from '@angular/cdk/scrolling';

@Component( {
    selector: 'servoyextra-table',
    templateUrl: './table.html',
    styleUrls: ['./table.css']
} )
export class ServoyExtraTable extends ServoyBaseComponent implements AfterViewInit, OnDestroy  {
  
    // this is a hack for test, so that this has a none static child ref because the child is in a nested template
    @ViewChild('child', {static: false}) child:ElementRef;
    @ViewChild('element', {static: false}) elementRef:ElementRef;
    @ViewChild(CdkVirtualScrollViewport) viewPort: CdkVirtualScrollViewport;
    
    @Input() foundset : IFoundset;
    @Output() foundsetChange = new EventEmitter();
    @Input() columns;
    @Input() sortDirection: string;
    @Input() enableSort: boolean = true;
    @Input() sortStyleClass: string;
    @Input() sortdownClass: string = "table-servoyextra-sort-down";
    @Input() sortupClass: string = "table-servoyextra-sort-up";
    @Input() visible: boolean;
    @Input() styleClass: string;
    @Input() servoyApi;
    @Input() minRowHeight: any;
    @Input() enableColumnResize: boolean;
    @Input() pageSize: number;
    @Input() rowStyleClassDataprovider: IFoundset;
    @Input() tabSeq;
    @Input() responsiveHeight;
    @Input() responsiveDynamicHeight;
    @Input() absoluteLayout : boolean;

    @Input() onViewPortChanged;
    @Input() onCellClick;
    @Input() onCellDoubleClick;
    @Input() onCellRightClick;
    @Input() onHeaderClick;
    @Input() onHeaderRightClick;
    @Input() onColumnResize;
    @Input() onFocusGainedMethodID;
    @Input() onFocusLostMethodID;

    timeoutID: number;
    lastClicked: number;
    sortColumnIndex: number;
    private log: LoggerService;
    columnStyleCache: Array<object> = [];
    autoColumnPercentage: any;
    tableWidth: number = 0;
    scrollWidth: number = 0;
    autoColumns: { [x: string]: any; count: any; length?: any; columns?: {}; minWidth?: {}; autoResize?: {}; };
    componentWidth: any;
    needToUpdateAutoColumnsWidth: boolean = false;
    extraWidth: any;
    extraWidthColumnIdx: any;
    columnCSSRules: any[] = [];
    targetStyleSheet: CSSStyleSheet;
    timeoutid: number;
    skipOnce: boolean = false;
    currentSortClass: any[] = [];
    sortClassUpdateTimer: any;
    currentPage: number = 1;
    changeListener: (change: FoundsetChangeEvent) => void;
    rendered: boolean;

    constructor(renderer: Renderer2, logFactory: LoggerFactory) {
        super(renderer);
        this.log = logFactory.getLogger('Table');
    }

    ngAfterViewInit() {
        super.ngAfterViewInit();
        this.rendered = true;

        this.computeTableWidth();
        this.computeTableHeight();
       
        this.setColumnsToInitalWidthAndInitAutoColumns();   
        for (let i = 0; i < this.columns.length; i++)
        {
            this.updateTableColumnStyleClass(i, { width: this.columns[i].width, minWidth: this.columns[i].width, maxWidth: this.columns[i].width });
        }
        
        this.attachHandlers();
        //this.addCellHandlers();

        this.changeListener = this.foundset.addChangeListener((event: FoundsetChangeEvent) => {
            if (event.sortColumnsChanged) {
                let sortSet = false;
                let sortColumnsA = this.foundset.sortColumns.split(/[\s,]+/);
                if (sortColumnsA.length >= 2) {
                    for (let i = 0; i < this.columns.length; i++) {
                        if (this.columns[i].dataprovider && sortColumnsA[0] == this.columns[i].dataprovider.idForFoundset) {
                            this.sortColumnIndex = i;
                            this.sortDirection = sortColumnsA[1].toLowerCase() == 'asc' ? 'up' : 'down';
                            sortSet = true;
                            break;
                        }
                    }
                }
                if(!sortSet) {
                    this.sortColumnIndex = -1;
                    this.sortDirection = null;
                }
            }
        });
    }

    private computeTableHeight() {
        const tbody = this.getNativeElement().getElementsByTagName('tbody');
        if (tbody && (tbody[0].scrollHeight > tbody[0].clientHeight && (this.scrollWidth == 0))) {
            this.scrollWidth = tbody[0].offsetWidth - tbody[0].clientWidth + 15; //TODO +2...
        }
        else if (tbody && (tbody[0].scrollHeight <= tbody[0].clientHeight) && (this.scrollWidth > 0)) {
            this.scrollWidth = 0;
        }

        if (!this.absoluteLayout) {
            this.renderer.setStyle(this.getNativeElement(), "position", "relative");

            const pagination = this.getNativeElement().getElementsByTagName('ngb-pagination');
            let paginationHeight = 0;
            if (pagination[0] && pagination[0].children[0]) {
                paginationHeight = pagination[0].children[0].clientHeight;
            }
            if (this.columns) {
                if (this.responsiveDynamicHeight) {
                    //TODO let h = paginationHeight + this.getNativeElement().clientHeight;
                    if (this.responsiveHeight === 0) {
                        //this case does not really work for ng1
                        this.renderer.setStyle(this.getNativeElement(), "height", "100%");
                        this.renderer.setStyle(this.viewPort._contentWrapper.nativeElement.parentElement, "height", "100%");
                    }
                    else {
                        //this doesn't work for cdkVirtualFor because we need to provide it a size to make it display something
                        //and then it doesn't make sense to set the size based on the computed height because it's the one that we have set previously
                        this.renderer.setStyle(this.getNativeElement(), "height", (this.responsiveHeight  - paginationHeight) + "px")//TODO h + "px");
                        this.renderer.setStyle(this.getNativeElement(), "max-height", this.responsiveHeight + "px");
                        this.renderer.setStyle(this.viewPort._contentWrapper.nativeElement.parentElement, "height", this.getNativeElement().clientHeight + "px");
                    }
                }
                else if (this.responsiveHeight === 0) {
                    this.renderer.setStyle(this.getNativeElement(), "height", "100%");
                    this.renderer.setStyle(this.viewPort._contentWrapper.nativeElement.parentElement, "height", (this.getNativeElement().clientHeight - paginationHeight) + "px");
                }
                else {
                    this.renderer.setStyle(this.getNativeElement(), "height", this.responsiveHeight + "px");
                    this.renderer.setStyle(this.viewPort._contentWrapper.nativeElement.parentElement, "height", (this.responsiveHeight - paginationHeight) + "px");
                }
            }
        }
    }

    ngOnDestroy(): void {
        this.foundset.removeChangeListener(this.changeListener);
    }
    
    attachHandlers() {
        if (this.onHeaderClick || this.onHeaderRightClick) {
            let headers = this.getNativeElement().getElementsByTagName('th');
            for (let i = 0; i < headers.length; i++) {
                if (this.onHeaderClick) {
                    this.renderer.listen(headers[i], 'click', e => this.headerClicked(i, e));
                }
                if (this.onHeaderRightClick) {
                    this.renderer.listen(headers[i], 'contextmenu', e => this.onHeaderRightClick(i, this.sortDirection, e));
                }
            }
        }

        if (this.onFocusGainedMethodID)
        {
            this.renderer.listen(this.getNativeElement().getElementsByTagName("table")[0], 'focus', e => this.onFocusGainedMethodID(e));
        }

        if (this.onFocusLostMethodID)
        {
            this.renderer.listen(this.getNativeElement().getElementsByTagName("table")[0], 'blur', e=> this.onFocusLostMethodID(e));
        }
    }

    private headerClicked(i: number, event?: Event):  void {
        this.onHeaderClick(i, this.sortDirection, event)
            .then((ret: string) => {
                if (ret === "override")
                    return;
                if (this.enableSort) {
                    this.doFoundsetSQLSort(i);
                }
            }, (reason: any) => {
                this.log.error(reason);
            });
    }

    doFoundsetSQLSort(sortColumnIndex: number) {
        if (!this.enableSort) return;
        this.sortColumnIndex = sortColumnIndex;
        if (this.columns[sortColumnIndex].dataprovider) {
            const sortCol = this.columns[sortColumnIndex].dataprovider.idForFoundset;
            let sqlSortDirection: 'asc' | 'desc' = "asc";
            if (this.foundset.sortColumns) {
                let sortColumnsA = this.foundset.sortColumns.split(" ");
                if (sortCol == sortColumnsA[0]) {
                    sqlSortDirection = sortColumnsA[1].toLowerCase() == "asc" ? "desc" : "asc";
                }
            }
            this.foundset.sortColumns = sortCol + " " + sqlSortDirection;
            this.foundset.sort([{ name: sortCol, direction: sqlSortDirection }]);
            this.foundsetChange.emit(this.foundset);
        }
    }

    private addCellHandlers() {
        if (this.onCellClick || this.onCellDoubleClick || this.onCellRightClick) {
            let cells = this.getNativeElement().getElementsByTagName('td');
            for (let i = 0; i < cells.length; i++) {
                let rowIdx = i / this.columns.length + 1;
                let colIdx = i % this.columns.length;
               if (this.onCellDoubleClick && this.onCellClick) {
                    const innerThis: ServoyExtraTable = this;
                    this.renderer.listen(cells[i], 'click', e => {
                        if (innerThis.lastClicked == i) {
                            window.clearTimeout(this.timeoutID);
                            innerThis.lastClicked = -1;
                            innerThis.timeoutID = null;
                            innerThis.onCellDoubleClick(rowIdx, colIdx, innerThis.foundset.viewPort.rows[rowIdx], e);
                        }
                        else {
                            innerThis.lastClicked = i;
                            innerThis.timeoutID = window.setTimeout(() => {
                                innerThis.timeoutID = null;
                                innerThis.lastClicked = -1;
                                innerThis.onCellClick(rowIdx, colIdx, innerThis.foundset.viewPort.rows[rowIdx], e);
                            }, 250);
                        }
                    });
                }
                else if (this.onCellClick) {
                    this.renderer.listen(cells[i], 'click', e => this.onCellClick(rowIdx, colIdx, this.foundset.viewPort.rows[rowIdx], e));
                }
                else if (this.onCellDoubleClick) {
                    this.renderer.listen(cells[i], 'dblclick', e => this.onCellDoubleClick(rowIdx, colIdx, this.foundset.viewPort.rows[rowIdx], e));
                }
                if (this.onCellRightClick)
                    this.renderer.listen(cells[i], 'contextmenu', e => this.onCellRightClick(rowIdx, colIdx, this.foundset.viewPort.rows[rowIdx], e));
            }
        }
    }

    getColumnStyle(column: number) {
        let columnStyle : object = this.columnStyleCache[column];
        if (columnStyle) return columnStyle;
        
        columnStyle = { "overflow": "hidden"};
        this.columnStyleCache[column] = columnStyle;
        const w = this.getNumberFromPxString(this.columns[column].width);
        if (w > -1) {
            columnStyle["min-width"] = columnStyle["max-width"] = columnStyle["width"] = w + "px";
        } else if (this.columns[column].width && (this.columns[column].width) != "auto") {
            columnStyle["width"] = this.columns[column].width;
        } else {
            const autoColumnPercentage = this.getAutoColumnPercentage();
            if(this.autoColumnPercentage) {
                columnStyle["width"] = autoColumnPercentage + "%";
            } else {
                if (!this.autoColumns) this.setColumnsToInitalWidthAndInitAutoColumns();
                columnStyle["min-width"] = columnStyle["max-width"] = columnStyle["width"] = Math.floor( (this.getComponentWidth() - this.tableWidth - this.scrollWidth) / this.autoColumns.count) + "px";
            }
        }
        this.updateTableColumnStyleClass(column, columnStyle);
        return columnStyle;
    }
    getComponentWidth() {
        if (!this.rendered) return 0;
        if (this.componentWidth === undefined && this.getNativeElement().parentElement.parentElement.offsetWidth != 0) {
            this.componentWidth = Math.floor(this.getNativeElement().parentElement.parentElement.offsetWidth);
        }
        return this.componentWidth;
    }
    getAutoColumnPercentage() {
        let nrColumnsWithPercentage = 0;
        let sumColumnsWithPercentage = 0;
        if (!this.autoColumns) return null;

        for (let autoColumnIdx in this.autoColumns["columns"]) {
            var w = this.columns[autoColumnIdx].width;
            if (w) {
                w = w.trim();
                if (w.indexOf("%") == w.length - 1) {
                    w = w.substring(0, w.length - 1);
                    if (!isNaN(Number(w))) {
                        nrColumnsWithPercentage++;
                        sumColumnsWithPercentage += Number(w);
                    }
                }
            }
        }

        return nrColumnsWithPercentage ? (100 - sumColumnsWithPercentage) / (this.autoColumns.length - nrColumnsWithPercentage) : 0;
    }

    getNumberFromPxString(s:string) {
        let numberFromPxString = -1;
        if (s) {
            s = s.trim().toLowerCase();
            if (s.indexOf("px") == s.length - 2) {
                s = s.substring(0, s.length - 2);
            }
            if (!isNaN(Number(s))) {
                numberFromPxString = Number(s);
            }
        }
        return numberFromPxString;
    }

    computeTableWidth() {
        this.tableWidth = 0;
        if (this.columns) {
            for (let i = 0; i < this.columns.length; i++) {
                if (!this.isAutoResizeColumn(i) && this.getNumberFromPxString(this.columns[i].initialWidth) > 0) {
                    let w = this.getNumberFromPxString(this.columns[i].width);
                    if (w > -1) {
                        this.tableWidth += w;
                    }
                }
            }
        }
        return this.tableWidth;
    }
    isAutoResizeColumn(idx: number) {
        return this.columns[idx].autoResize || (this.columns[idx].width == "auto");
    }

    setColumnsToInitalWidthAndInitAutoColumns() {
        let newAutoColumns = { columns: { }, minWidth: { }, autoResize: {}, count: 0 };
        if (this.columns) {
            for (var i = 0; i < this.columns.length; i++) {
                if (this.columns[i].initialWidth == undefined) {
                    this.columns[i].initialWidth = this.columns[i].width == undefined ? "" : this.columns[i].width;
                } else {
                    this.columns[i].width = this.columns[i].initialWidth;
                }

                var minWidth = this.getNumberFromPxString(this.columns[i].width);
                if (this.isAutoResizeColumn(i) || minWidth < 0) {
                    newAutoColumns.columns[i] = true;
                    newAutoColumns.minWidth[i] = minWidth;
                    newAutoColumns.autoResize[i] = this.isAutoResizeColumn(i);
                    newAutoColumns.count += 1;
                }
            }
        }

        this.autoColumns = newAutoColumns;
        this.needToUpdateAutoColumnsWidth = true;
        this.columnStyleCache = [];
    }

    updateAutoColumnsWidth(delta: number) {
        let fixedDelta = delta;

        // if extraWidth was appended to last auto-resize column then remove it, and append it to delta
        if(this.extraWidth) {
            fixedDelta += this.extraWidth;
            let w = this.getNumberFromPxString(this.columns[this.extraWidthColumnIdx].width);
            w += (0 -this.extraWidth);
           this.columns[this.extraWidthColumnIdx].width = w + "px";				
        }

        this.columnStyleCache = [];
        let oldWidth = this.getAutoResizeColumnsWidth();
        let newWidth = oldWidth + fixedDelta;

        let usedDelta = 0;
        let lastAutoColumnIdx = -1;
        for (let i = 0; i < this.columns.length; i++) {
            if (this.autoColumns.autoResize[i]) {
                if (this.autoColumns.minWidth[i] > 0) {
                    var oldW = this.getNumberFromPxString(this.columns[i].width);
                    var w = Math.floor(oldW * newWidth / oldWidth);
                    
                    if (w < this.autoColumns.minWidth[i]) {
                        w = this.autoColumns.minWidth[i];
                    }
                    this.columns[i].width = w + "px";
                    usedDelta += (w - oldW);
                    lastAutoColumnIdx = i;
                } else {
                    this.columns[i].width = this.columns[i].initialWidth;
                }
            }
        }

        if(lastAutoColumnIdx > -1) {
            this.extraWidth = Math.round(Math.abs(fixedDelta) - Math.abs(usedDelta));
            this.extraWidthColumnIdx = lastAutoColumnIdx;
            if(this.extraWidth) {
                if(fixedDelta < 0) this.extraWidth = 0 - this.extraWidth;
                let w = this.getNumberFromPxString(this.columns[lastAutoColumnIdx].width);
                w += this.extraWidth;
                this.columns[lastAutoColumnIdx].width = w + "px";
            }
        }
    }
    

    getAutoResizeColumnsWidth() {
        let autoColumnsWidth = 0;
        for (let i = 0; i < this.columns.length; i++) {
            if (this.autoColumns.autoResize[i] && this.autoColumns.minWidth[i] > 0) {
                autoColumnsWidth += this.getNumberFromPxString(this.columns[i].width);
            }
        }
        return autoColumnsWidth;
    }

    getSortStyleClass(column: number){
        let lv_styles = "";
        if (this.enableSort) {
           if ((this.sortColumnIndex == -1 &&  column == 0) || this.sortColumnIndex == column) 
           {
              lv_styles = this.sortStyleClass;
           }
        }
		return this.columns[column].headerStyleClass == undefined ? lv_styles : lv_styles + " " + this.columns[column].headerStyleClass;
    }

    public getSortClass(column: number) {
        let sortClass = "table-servoyextra-sort-hide";
        if (this.enableSort) {
            let direction;
            let isGetSortFromSQL = this.sortColumnIndex < 0;
            if (column == this.sortColumnIndex) {
                direction = this.sortDirection;
                if (!direction) {
                    isGetSortFromSQL = true;
                }
            }
            if (isGetSortFromSQL) {
                if (this.foundset && this.foundset.sortColumns && this.columns[column].dataprovider) {
                    let sortCol = this.columns[column].dataprovider.idForFoundset;
                    let sortColumnsA = this.foundset.sortColumns.split(" ");

                    if (sortCol == sortColumnsA[0]) {
                        direction = sortColumnsA[1].toLowerCase() == "asc" ? "up" : "down";
                    }
                }
            }

            if (direction) {
                sortClass = "table-servoyextra-sort-show-" + direction + " " + this["sort" + direction + "Class"];
            }
        }
        if (this.currentSortClass.length <= column || this.currentSortClass[column] != sortClass) {
            if (this.sortClassUpdateTimer) window.clearTimeout(this.sortClassUpdateTimer);

            this.sortClassUpdateTimer = window.setTimeout(() => {
                const tbody = this.elementRef !== undefined ? this.getNativeElement().getElementsByTagName('tbody') : undefined;
                if (tbody) {
                    if (tbody) this.updateTBodyStyle(tbody[0]);
                }
            }, 50);
            this.currentSortClass[column] = sortClass;
        }
        return sortClass;
    }
    updateTBodyStyle(tBodyEl: ElementRef) {
        let tBodyStyle = {};
        let componentWidth = this.getComponentWidth();
        tBodyStyle['width'] = componentWidth + "px";
        const tblHead = this.getNativeElement().getElementsByTagName("thead")[0];
        if (tblHead.style.display !== 'none') {
            tBodyStyle['top'] = tblHead.offsetHeight + "px";
        }
       if (this.showPagination()) {
            var pagination = this.getNativeElement().getElementsByTagName('ngb-pagination');
            if (pagination[0] && pagination[0].children[0]) {
                tBodyStyle['margin-bottom'] = (pagination[0].children[0].clientHeight + 2) + "px";
                this.renderer.setStyle(pagination[0].children[0], "margin-bottom", "0");
                this.renderer.setStyle(pagination[0].children[0], "position", "absolute");
                this.renderer.setStyle(pagination[0].children[0], "bottom", "0");
                this.computeTableHeight();
            }
        }

        for (let p in tBodyStyle) {
            this.renderer.setStyle(tBodyEl, p, tBodyStyle[p]);
        }
        this.renderer.setStyle(this.viewPort._contentWrapper.nativeElement.parentElement, "height", tBodyEl['clientHeight']+"px");
    }


    getTHeadStyle() {
        let tHeadStyle = {};
        if (this.enableSort || this.onHeaderClick) {
            tHeadStyle["cursor"] = "pointer";
        }
        tHeadStyle["left"] = -this.getNativeElement().getElementsByTagName("table")[0].scrollLeft() + "px";
        return tHeadStyle;
    }

    updateTableColumnStyleClass(columnIndex:number, style:any) {
        if (!this.columnCSSRules[columnIndex]) {
            let clsName = "#table_" + this.servoyApi.getMarkupId() + " .c" + columnIndex;
            if (!this.columnCSSRules[columnIndex]) {
                if (!this.targetStyleSheet) {
                    let elem = document.createElement('style');
                    elem.type = 'text/css';
                    document.getElementsByTagName('head')[0].appendChild(elem);
                    this.targetStyleSheet = document.styleSheets[document.styleSheets.length-1] as CSSStyleSheet;
                }
                let rules = this.targetStyleSheet.cssRules || this.targetStyleSheet.rules;
                this.targetStyleSheet.insertRule(clsName + '{}', rules.length);
                this.columnCSSRules[columnIndex] = rules[rules.length - 1];
                this.columnCSSRules[columnIndex].style["height"] = this.minRowHeight;
            }
        }

        for (var p in style) {
            this.columnCSSRules[columnIndex].style[p] = style[p];
        }
    }

    onResizeEnd(event: ResizeEvent, columnIndex:number): void {
        window.clearTimeout(this.timeoutID);
        const headers = this.getNativeElement().getElementsByTagName('th');
        const newWidth = Math.floor(event.rectangle.width) + "px";
        this.renderer.setStyle(headers[columnIndex], "width", newWidth);
        this.renderer.setStyle(headers[columnIndex], "min-width", newWidth);
        this.renderer.setStyle(headers[columnIndex], "max-width", newWidth);
        this.updateTableColumnStyleClass(columnIndex, { width: newWidth, minWidth: newWidth, maxWidth: newWidth });
        const innerThis: ServoyExtraTable = this;
        this.timeoutID = window.setTimeout(() => {
            innerThis.onColumnResize(event);
            this.timeoutID = null;
        });
    }

    selectRow(idxInFs: number, event: MouseEvent) {
        let newSelection = [idxInFs];
        if (event.ctrlKey) {
            newSelection = this.foundset.selectedRowIndexes ? this.foundset.selectedRowIndexes.slice() : [];
            let idxInSelected = newSelection.indexOf(idxInFs);
            if (idxInSelected == -1) {
                newSelection.push(idxInFs);
            } else if (newSelection.length > 1) {
                newSelection.splice(idxInSelected, 1);
            }
        } else if (event.shiftKey) {
            let start = -1;
            if (this.foundset.selectedRowIndexes) {
                for (let j = 0; j < this.foundset.selectedRowIndexes.length; j++) {
                    if (start == -1 || start > this.foundset.selectedRowIndexes[j]) {
                        start = this.foundset.selectedRowIndexes[j];
                    }
                }
            }
            let stop = idxInFs;
            if (start > idxInFs) {
                stop = start;
                start = idxInFs;
            }
            newSelection = []
            for (let n = start; n <= stop; n++) {
                newSelection.push(n);
            }
        }

        this.foundset.requestSelectionUpdate(newSelection);
        this.foundsetChange.emit(this.foundset);
    }

    onScroll(){
        if(!this.viewPort) return;
        if (this.onViewPortChanged) {
            this.currentPage = this.viewPort.getRenderedRange().start / this.pageSize +1;
            this.onViewPortChanged(this.viewPort.getRenderedRange().start, this.viewPort.getRenderedRange().end);
        }
    }

    public setSelectedHeader(columnIndex: number) {
        if (this.onHeaderClick) {
            if (this.enableSort && (this.sortColumnIndex != columnIndex)) {
                this.sortDirection = null;
            }
            this.headerClicked(columnIndex);
        } else {
            this.sortColumnIndex = columnIndex;
            this.doFoundsetSQLSort(this.sortColumnIndex);
        }
    }

    public getViewPortPosition() : number[] {
        if (!this.viewPort) return null;
        return [this.viewPort.getRenderedRange().start, this.viewPort.getRenderedRange().end];
    }

    public requestFocus(mustExecuteOnFocusGainedMethod:boolean) {
        let tbl = this.getNativeElement().getElementsByTagName("table")[0];
        this.skipOnce = mustExecuteOnFocusGainedMethod === false;
        tbl.focus();
    }

    showPagination() {
        return this.pageSize && this.foundset && (this.foundset.serverSize > this.pageSize || this.foundset.hasMoreRows);

    }

    modifyPage() {
        this.viewPort.setRenderedRange({start: this.pageSize * (this.currentPage - 1), end: this.pageSize * this.currentPage - 1});
    }

    getRowClass(idx: number) {
        let rowClass = "";
        if (this.foundset.selectedRowIndexes.indexOf(idx) > -1) {
            rowClass += "table-servoyextra-selected ";
        }
        if (this.rowStyleClassDataprovider) {
            rowClass += this.rowStyleClassDataprovider[idx % this.pageSize];
        }
        return rowClass;
    }
}