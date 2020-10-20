import { Component, ViewChild, Input, Renderer2, ElementRef, EventEmitter, Output, OnDestroy, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBaseComponent } from '../../ngclient/servoy_public'
import { IFoundset } from '../../sablo/spectypes.service';
import { LoggerFactory, LoggerService } from '../../sablo/logger.service';
import { ResizeEvent } from 'angular-resizable-element';
import { FoundsetChangeEvent } from '../../ngclient/converters/foundset_converter';
import { CdkVirtualScrollViewport } from '@angular/cdk/scrolling';
import { BehaviorSubject} from 'rxjs';
import { auditTime, tap } from 'rxjs/operators';
import {AsyncPipe} from '@angular/common';

@Component( {
    selector: 'servoyextra-table',
    templateUrl: './table.html',
    changeDetection: ChangeDetectionStrategy.OnPush
} )
export class ServoyExtraTable extends ServoyBaseComponent implements OnDestroy  {
  
    // this is a hack for test, so that this has a none static child ref because the child is in a nested template
    @ViewChild('child', {static: false}) child:ElementRef;
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
    @Input() lastSelectionFirstElement: number;
    @Input() keyCodeSettings : {arrowUp: boolean , arrowDown: boolean, end:boolean, enter:boolean, home: boolean, pageDown: boolean, pageUp: boolean};

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
    scrollToSelectionNeeded: boolean = true;
    averageRowHeight: number;
    actualPageSize: number = -1;
    dataStream = new BehaviorSubject<any[]>([]);
    idx: number;
    changedPage: boolean = false;
    prevPage: number;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, logFactory: LoggerFactory) { 
        super(renderer, cdRef);
        this.log = logFactory.getLogger('Table');
    }

    svyOnInit() {
        super.svyOnInit();
        this.rendered = true;

        this.computeTableWidth();
        this.computeTableHeight();
       
        this.setColumnsToInitalWidthAndInitAutoColumns();   
        for (let i = 0; i < this.columns.length; i++)
        {
            this.updateTableColumnStyleClass(i, { width: this.columns[i].width, minWidth: this.columns[i].width, maxWidth: this.columns[i].width });
        }
        this.attachHandlers();

        this.changeListener = this.foundset.addChangeListener((event: FoundsetChangeEvent) => {
            if (event.sortColumnsChanged) {
                this.sortColumnsChanged(event);
            }

            if (event.selectedRowIndexesChanged) {
               this.selectedRowIndexesChanged(event.selectedRowIndexesChanged.oldValue);
            }
        });

        window.setTimeout(() => {
            //first time we need to wait a bit before we scroll
            this.computeAverageRowHeight();
        }, 100);

        this.idx = 0;
        this.viewPort.scrolledIndexChange.pipe(
            auditTime(300),
            tap((currIndex: number) => {
                if (currIndex > this.viewPort.getRenderedRange().end + this.actualPageSize) {
                    this.loadMoreRecords(this.viewPort.getRenderedRange().end + this.actualPageSize);
                }
                else {
                    this.loadMoreRecords(currIndex);
                }
                this.idx = Math.min(currIndex, this.foundset.serverSize);
                this.setCurrentPageIfNeeded();
            })
          ).subscribe();

          setTimeout(() => this.dataStream.next(this.foundset.viewPort.rows), 50);
          window.setTimeout(() => {
            //first time we need to wait a bit before we scroll
            this.scrollToSelection();
        }, 400);
    }
    loadMoreRecords(currIndex : number) {
        if (currIndex < (this.foundset.viewPort.startIndex - this.actualPageSize) || currIndex >= this.foundset.viewPort.rows.length) {
            this.foundset.loadExtraRecordsAsync(currIndex >= this.foundset.viewPort.rows.length ? this.actualPageSize : (-1) * this.actualPageSize, false).then(() => {
                this.recordsLoaded();
            });
            this.foundsetChange.emit(this.foundset);
        }
    }

    private recordsLoaded() {
        this.dataStream.next([...this.foundset.viewPort.rows]);
    }

    computeAverageRowHeight() {
        if (!this.rendered) return;
        if (this.actualPageSize == -1) {
            const children = this.getNativeElement().getElementsByTagName('tr');
            const realRowCount = children.length;
            if (realRowCount > 0) {
                const firstChild = children[0];
                const lastChild = children[children.length - 1];
                this.averageRowHeight = Math.round((lastChild.offsetTop + lastChild.offsetHeight - firstChild.offsetTop) / realRowCount);
            } else {
                this.averageRowHeight = 25; // it won't be relevant anyway; it is equal to the default minRowHeight from .spec
            }
            const tbody = this.getNativeElement().getElementsByTagName('tbody')[0];
            this.actualPageSize = Math.ceil(tbody.clientHeight / this.averageRowHeight) - 1;
            this.setCurrentPageIfNeeded();
        }
    }

    private selectedRowIndexesChanged(oldValue: number[]) {
        if (this.foundset.selectedRowIndexes.length > 0) {
            if (this.foundset.selectedRowIndexes != oldValue || this.lastSelectionFirstElement != this.foundset.selectedRowIndexes[0]) {
                if (this.lastSelectionFirstElement != this.foundset.selectedRowIndexes[0]) {
                    this.log.spam("svy extra table * selectedRowIndexes changed; scrollToSelectionNeeded = true");
                    this.lastSelectionFirstElement = this.foundset.selectedRowIndexes[0];
                    if (this.scrollToSelectionNeeded) {
                        this.scrollToSelection();
                    }
                    else {
                        this.scrollToSelectionNeeded = true;
                    }
                }
            }
        }
        else {
            this.lastSelectionFirstElement = -1;
        }
    }

    private sortColumnsChanged(event: FoundsetChangeEvent) {
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
        if (!sortSet) {
            this.sortColumnIndex = -1;
            this.sortDirection = null;
        }
    }

    private scrollToSelection() {
        if (this.lastSelectionFirstElement !== -1) {
            this.viewPort.scrollToOffset(this.lastSelectionFirstElement * this.averageRowHeight);
            this.currentPage = Math.floor(this.lastSelectionFirstElement / this.actualPageSize);
        }
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
                    this.renderer.listen(headers[i], 'contextmenu', e => this.onHeaderRightClick(i, this.sortDirection, e, this.columns[i].id));
                }
            }
        }

        if (this.onFocusGainedMethodID)
        {
            this.renderer.listen(this.getNativeElement().getElementsByTagName("table")[0], 'focus', e => {
                this.callFocusGained(e);
            });
        }

        if (this.onFocusLostMethodID)
        {
            this.renderer.listen(this.getNativeElement().getElementsByTagName("table")[0], 'blur', e => {
                this.callFocusLost(e);   
            });
        }
    }

    private callFocusLost(e: any) {
        if (!this.skipOnce) {
            this.onFocusLostMethodID(e);
        }
        this.skipOnce = false;
    }

    private callFocusGained(e: any) {
        if (!this.skipOnce) {
            this.onFocusGainedMethodID(e);
        }
        this.skipOnce = false;
    }

    private headerClicked(i: number, event?: Event):  void {
        this.onHeaderClick(i, this.sortDirection, event, this.columns[i].id)
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
            this.foundset.sort([{ name: sortCol, direction: sqlSortDirection }]).then(()=>{
                this.dataStream.next(this.foundset.viewPort.rows);
            });
            this.foundsetChange.emit(this.foundset);
        }
    }

    cellClick(rowIdx: number, colIdx: number, record: any, e: Event, columnId:string) {
        if (this.onCellDoubleClick && this.onCellClick) {
            const innerThis: ServoyExtraTable = this;
            if (innerThis.lastClicked == rowIdx * colIdx) {
                window.clearTimeout(this.timeoutID);
                innerThis.lastClicked = -1;
                innerThis.timeoutID = null;
                innerThis.onCellDoubleClick(rowIdx, colIdx, record, e, columnId);
            }
            else {
                innerThis.lastClicked = rowIdx * colIdx;
                innerThis.timeoutID = window.setTimeout(() => {
                    innerThis.timeoutID = null;
                    innerThis.lastClicked = -1;
                    innerThis.onCellClick(rowIdx, colIdx, record, e, columnId);
                }, 250);
            }
        }
        else if (this.onCellClick) {
            this.onCellClick(rowIdx, colIdx, this.foundset.viewPort.rows[rowIdx], e, columnId);
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
            let w = this.columns[autoColumnIdx].width;
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
                    const w = this.getNumberFromPxString(this.columns[i].width);
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
            for (let i = 0; i < this.columns.length; i++) {
                if (this.columns[i].initialWidth == undefined) {
                    this.columns[i].initialWidth = this.columns[i].width == undefined ? "" : this.columns[i].width;
                } else {
                    this.columns[i].width = this.columns[i].initialWidth;
                }

                let minWidth = this.getNumberFromPxString(this.columns[i].width);
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
                    const oldW = this.getNumberFromPxString(this.columns[i].width);
                    let w = Math.floor(oldW * newWidth / oldWidth);
                    
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
            const pagination = this.getNativeElement().getElementsByTagName('ngb-pagination');
            if (pagination[0] && pagination[0].children[0]) {
                tBodyStyle['bottom'] = (pagination[0].children[0].clientHeight + 2) + "px";
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

        for (let p in style) {
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
        this.scrollToSelectionNeeded = false; //we don't need to scroll to selection when we select a record by clicking on it
        this.foundset.requestSelectionUpdate(newSelection);
        this.foundsetChange.emit(this.foundset);
    }

    onScroll(){
        if(!this.viewPort) return;
        if (this.onViewPortChanged) 
        {
            this.onViewPortChanged(this.viewPort.getRenderedRange().start, this.viewPort.getRenderedRange().end);
        }
    }

    private setCurrentPageIfNeeded() {
        if (this.changedPage) {
            this.changedPage = false;
            return;
        }
        if (this.showPagination()) {
            if (this.actualPageSize > 0) {
                this.currentPage =  Math.floor(this.idx / this.actualPageSize);
            }
            else {
                window.setTimeout(() => {
                    this.setCurrentPageIfNeeded();
                }, 100);
            }
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
        this.changedPage = true;
        const startIndex = this.actualPageSize * (this.currentPage - 1);
        const endIndex = Math.min(this.foundset.serverSize, this.actualPageSize * this.currentPage);
        const offset = this.getNativeElement().getElementsByTagName('tbody')[0].clientHeight * (this.currentPage - 1);
        if (this.prevPage > this.currentPage) {
            
            this.viewPort.scrollToOffset(offset - 2);
        }
        else {
            if (this.idx + this.actualPageSize > this.foundset.serverSize) {
                this.viewPort.scrollToOffset(this.viewPort.measureScrollOffset() + Math.trunc(this.averageRowHeight * this.idx % this.actualPageSize));
            }
            else
            {
                this.viewPort.scrollToOffset(offset);
            }
        }
        this.prevPage = this.currentPage;
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

    keypressed(event: KeyboardEvent) {
        let fs = this.foundset;
        if (fs.selectedRowIndexes && fs.selectedRowIndexes.length > 0) {
            let selectionChanged = false;
            let oldSelectedIdxs = fs.selectedRowIndexes.slice();
            let selection = fs.selectedRowIndexes[0];
            if (event.keyCode == 33) { // PAGE UP KEY
                if (this.keyCodeSettings && !this.keyCodeSettings.pageUp) return;
                
                const firstVisibleIndex = this.showPagination() ? this.actualPageSize * Math.trunc(selection/this.actualPageSize) : 1;
                fs.selectedRowIndexes = [firstVisibleIndex];
                selectionChanged = (selection != firstVisibleIndex);
                this.log.spam("svy extra table * keyPressed; scroll on PG UP");
                this.viewPort.scrollToIndex(firstVisibleIndex);
            }
            else if (event.keyCode == 34) { // PAGE DOWN KEY
                if (this.keyCodeSettings && !this.keyCodeSettings.pageDown) return;

                let lastVisibleIndex = this.showPagination() ? this.actualPageSize * (Math.trunc(selection/this.actualPageSize) + 1 ) -1 : (this.foundset.viewPort.rows.length - 1);
                if (lastVisibleIndex > fs.serverSize -1 ) lastVisibleIndex = fs.serverSize -1;
                fs.selectedRowIndexes = [lastVisibleIndex];
                selectionChanged = (selection != lastVisibleIndex);
                this.log.spam("svy extra table * keyPressed; scroll on PG DOWN");
                this.viewPort.scrollToIndex(lastVisibleIndex);
            } else if (event.keyCode == 38) { // ARROW UP KEY
                if (this.keyCodeSettings && !this.keyCodeSettings.arrowUp) return;
                
                if (selection > 0) {
                    fs.selectedRowIndexes = [selection - 1];
                    this.viewPort.scrollToIndex(selection - 1);
                    this.scrollToSelectionNeeded = false;
                    selectionChanged = true;
                }
                event.preventDefault();
            } else if (event.keyCode == 40) { // ARROW DOWN KEY
                if (this.keyCodeSettings && !this.keyCodeSettings.arrowDown) return;
                
                if (selection + 1 < (fs.viewPort.startIndex + fs.viewPort.size)) {
                    fs.selectedRowIndexes = [selection + 1];
                    this.viewPort.scrollToIndex(selection + 1);
                    this.scrollToSelectionNeeded = false;
                    selectionChanged = true;
                }
                event.preventDefault();
            } else if (event.keyCode == 13) { // ENTER KEY
                if (!this.keyCodeSettings.enter) return;
                if (this.onCellClick) {
                    this.onCellClick(selection + 1, null, fs.viewPort.rows[selection])
                }
            } else if (event.keyCode == 36) { // HOME
                if (this.keyCodeSettings && !this.keyCodeSettings.home) return;
                if (fs.selectedRowIndexes[0] != 0)
                {
                    fs.selectedRowIndexes = [0];
                    this.viewPort.scrollToIndex(0);
                    selectionChanged = true;
                }
                event.preventDefault();
                event.stopPropagation();

            } else if (event.keyCode == 35) { // END
                if (this.keyCodeSettings && !this.keyCodeSettings.end) return;

                const endIndex = fs.viewPort.size -1;
                if (fs.selectedRowIndexes[0] != endIndex) {
                    fs.selectedRowIndexes = [endIndex];
                    selectionChanged = true;
                    this.viewPort.scrollToIndex(endIndex);
                }

                if (fs.hasMoreRows){
                    //if it has more rows, then load at most one more page if paging is used,  or the remaining records otherwise
                    this.foundset.loadRecordsAsync(endIndex, this.actualPageSize > 0 ? Math.min(this.actualPageSize, this.foundset.serverSize-endIndex) : this.foundset.serverSize-endIndex).then(()=>{
                        this.dataStream.next(this.foundset.viewPort.rows);
                    });
                    this.foundsetChange.emit(this.foundset);
                }
                event.preventDefault();
                event.stopPropagation();
            }

            if (selectionChanged) {
                this.selectedRowIndexesChanged(oldSelectedIdxs);
            }
        }
    }

    trackByIdx(i: number) {
        return i;
      }
}