import { Component, ViewChild, Input, Renderer2, ElementRef, OnDestroy, ChangeDetectorRef, ChangeDetectionStrategy, QueryList, ViewChildren, Directive } from '@angular/core';
import { ServoyBaseComponent } from '../../ngclient/servoy_public';
import { IFoundset, ViewPortRow } from '../../sablo/spectypes.service';
import { LoggerFactory, LoggerService } from '../../sablo/logger.service';
import { ResizeEvent } from 'angular-resizable-element';
import { FoundsetChangeEvent } from '../../ngclient/converters/foundset_converter';
import { CdkVirtualScrollViewport } from '@angular/cdk/scrolling';
import { BehaviorSubject } from 'rxjs';
import { auditTime, tap, first } from 'rxjs/operators';

@Directive({
    selector: '[svyTableRow]'
})
export class TableRow {

    @Input() svyTableRow: number;

    constructor(public elRef: ElementRef) {
    }
}

@Component({
    selector: 'servoyextra-table',
    templateUrl: './table.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyExtraTable extends ServoyBaseComponent<HTMLDivElement> implements OnDestroy {

    // this is a hack for test, so that this has a none static child ref because the child is in a nested template
    @ViewChild('child', { static: false }) child: ElementRef;
    @ViewChild(CdkVirtualScrollViewport) viewPort: CdkVirtualScrollViewport;
    @ViewChildren(TableRow) renderedRows: QueryList<TableRow>;

    @Input() foundset: IFoundset;
    @Input() columns: string | any[];
    @Input() sortDirection: string;
    @Input() enableSort = true;
    @Input() sortStyleClass: string;
    @Input() sortdownClass = 'table-servoyextra-sort-down';
    @Input() sortupClass = 'table-servoyextra-sort-up';
    @Input() styleClass: string;
    @Input() minRowHeight: any;
    @Input() enableColumnResize: boolean;
    @Input() pageSize: number;
    @Input() rowStyleClassDataprovider: IFoundset;
    @Input() tabSeq: number;
    @Input() responsiveHeight: number;
    @Input() responsiveDynamicHeight: boolean;
    @Input() lastSelectionFirstElement: number;
    @Input() keyCodeSettings: { arrowUp: boolean; arrowDown: boolean; end: boolean; enter: boolean; home: boolean; pageDown: boolean; pageUp: boolean };

    @Input() onViewPortChanged: (start: number, end: number) => void;
    @Input() onCellClick: (rowIdx: number, colIdx: number, record?: ViewPortRow, e?: MouseEvent, columnId?: string) => void;
    @Input() onCellDoubleClick: (rowIdx: number, colIdx: number, record?: ViewPortRow, e?: MouseEvent, columnId?: string) => void;
    @Input() onCellRightClick: (rowIdx: number, colIdx: number, record?: ViewPortRow, e?: MouseEvent, columnId?: string) => void;
    @Input() onHeaderClick: (colIdx: number, sortDirection: string, e?: MouseEvent, columnId?: string) => Promise<string>;
    @Input() onHeaderRightClick: (colIdx: number, sortDirection: string, e?: MouseEvent, columnId?: string) => void;
    @Input() onColumnResize: (event?: ResizeEvent) => void;
    @Input() onFocusGainedMethodID: (event: Event) => void;
    @Input() onFocusLostMethodID: (event?: Event) => void;

    timeoutID: number;
    lastClicked: number;
    sortColumnIndex: number;
    columnStyleCache: Array<any> = [];
    autoColumnPercentage: any;
    tableWidth = 0;
    scrollWidth = 0;
    autoColumns: { [x: string]: any; count: any; length?: any; columns?: {}; minWidth?: {}; autoResize?: {} };
    componentWidth: any;
    needToUpdateAutoColumnsWidth = false;
    extraWidth: any;
    extraWidthColumnIdx: any;
    columnCSSRules: any[] = [];
    targetStyleSheet: CSSStyleSheet;
    timeoutid: number;
    skipOnce = false;
    currentSortClass: any[] = [];
    sortClassUpdateTimer: any;
    currentPage = 1;
    rendered: boolean;
    scrollToSelectionNeeded = true;
    averageRowHeight = 25;
    dataStream = new BehaviorSubject<any[]>([]);
    idx: number;
    changedPage = false;
    prevPage: number;
    private log: LoggerService;
    private removeListenerFunction: () => void;
    minBuff: number;
    maxBuff: number;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, logFactory: LoggerFactory) {
        super(renderer, cdRef);
        this.log = logFactory.getLogger('Table');
    }

    svyOnInit() {
        super.svyOnInit();
        this.rendered = true;
        this.minBuff = this.pageSize ? (this.pageSize + 1) * this.getNumberFromPxString(this.minRowHeight) : 200
        this.maxBuff = this.minBuff * 2;

        this.computeTableWidth();
        this.computeTableHeight();

        this.setColumnsToInitalWidthAndInitAutoColumns();
        for (let i = 0; i < this.columns.length; i++) {
            this.updateTableColumnStyleClass(i, { width: this.columns[i].width, minWidth: this.columns[i].width, maxWidth: this.columns[i].width });
        }
        this.attachHandlers();

        this.removeListenerFunction = this.foundset.addChangeListener((event: FoundsetChangeEvent) => {
            if (event.sortColumnsChanged) {
                this.sortColumnsChanged(event);
            }

            if (event.selectedRowIndexesChanged) {
                this.selectedRowIndexesChanged(event.selectedRowIndexesChanged.oldValue);
            }

            let newVal: object[];
            if (event.fullValueChanged) newVal = event.fullValueChanged.newValue.viewPort.rows;
            if (event.viewportRowsCompletelyChanged) newVal = event.viewportRowsCompletelyChanged.newValue;
            if (event.fullValueChanged || event.viewportRowsCompletelyChanged) {
                this.dataStream.next([...newVal]);
            }
        });

        this.idx = 0;
        this.viewPort.scrolledIndexChange.pipe(
            auditTime(300),
            tap(() => {
                this.idx = this.getFirstVisibleIndex();
                this.loadMoreRecords(this.idx);
                this.setCurrentPageIfNeeded();
            })
        ).subscribe();
        this.renderedRows.changes.subscribe(() => {
           const newAvg = this.renderedRows.reduce((a, b) => a + b.elRef.nativeElement.getBoundingClientRect().height, 0) / this.renderedRows.length;
           if (newAvg !== this.averageRowHeight) {
                this.averageRowHeight = newAvg;
                if (this.responsiveDynamicHeight) this.computeTableHeight();
           }
           console.log(this.averageRowHeight);
        });

        setTimeout(() => {
            this.dataStream.next(this.foundset.viewPort.rows);
            this.scrollToSelection();
        }, 50);
    }
    loadMoreRecords(currIndex: number, scroll?: boolean) {
        if ((this.foundset.viewPort.startIndex !== 0 && currIndex < this.pageSize) ||
            currIndex + this.pageSize >= this.foundset.viewPort.rows.length) {
            this.foundset.loadExtraRecordsAsync(currIndex + this.pageSize >= this.foundset.viewPort.rows.length ? this.pageSize : (-1) * this.pageSize, false).then(() => {
                this.recordsLoaded();
                if (scroll) this.scrollToSelection();
            });
        }
    }

    getFirstVisibleIndex(): number {
        const offset = this.getNativeElement().getElementsByTagName('th')[0].getBoundingClientRect().height;
        const first = this.renderedRows.find((element) => element.elRef.nativeElement.getBoundingClientRect().top >= offset);
        if (first) return first.svyTableRow;
        return -1;
    }

    calculateTableHeight(innerThis: ServoyExtraTable): void {
        const rows = innerThis.elementRef.nativeElement.querySelectorAll('tr');
        if (rows.length > 1 || innerThis.foundset.viewPort.rows.length === 0) {
            let height = 4; //the border
            const pagination = innerThis.getNativeElement().getElementsByTagName('ngb-pagination');
            let paginationHeight = 0;
            if (pagination[0] && pagination[0].children[0]) {
                paginationHeight = pagination[0].children[0].clientHeight;
            }
            height += paginationHeight;
            rows.forEach((row) => {
                if (height < innerThis.responsiveHeight) {
                    height += row.clientHeight;
                }
            });
            innerThis.renderer.setStyle(innerThis.getNativeElement(), 'height', height + 'px');
            innerThis.renderer.setStyle(innerThis.viewPort._contentWrapper.nativeElement.parentElement, 'height', (height - paginationHeight) + 'px');
        } else {
            window.setTimeout(innerThis.calculateTableHeight, 100);
        }
    }

    ngOnDestroy(): void {
        if (this.removeListenerFunction != null) {
            this.removeListenerFunction();
            this.removeListenerFunction = null;
        }

    }

    attachHandlers() {
        if (this.onHeaderClick || this.onHeaderRightClick) {
            const headers = this.getNativeElement().getElementsByTagName('th');
            for (let i = 0; i < headers.length; i++) {
                if (this.onHeaderClick) {
                    this.renderer.listen(headers[i], 'click', e => this.headerClicked(i, e));
                }
                if (this.onHeaderRightClick) {
                    this.renderer.listen(headers[i], 'contextmenu', e => this.onHeaderRightClick(i, this.sortDirection, e, this.columns[i].id));
                }
            }
        }

        if (this.onFocusGainedMethodID) {
            this.renderer.listen(this.getNativeElement().getElementsByTagName('table')[0], 'focus', e => {
                this.callFocusGained(e);
            });
        }

        if (this.onFocusLostMethodID) {
            this.renderer.listen(this.getNativeElement().getElementsByTagName('table')[0], 'blur', e => {
                this.callFocusLost(e);
            });
        }
    }


    doFoundsetSQLSort(sortColumnIndex: number) {
        if (!this.enableSort) return;
        this.sortColumnIndex = sortColumnIndex;
        if (this.columns[sortColumnIndex].dataprovider) {
            const sortCol = this.columns[sortColumnIndex].dataprovider.idForFoundset;
            let sqlSortDirection: 'asc' | 'desc' = 'asc';
            if (this.foundset.sortColumns) {
                const sortColumnsA = this.foundset.sortColumns.split(' ');
                if (sortCol === sortColumnsA[0]) {
                    sqlSortDirection = sortColumnsA[1].toLowerCase() === 'asc' ? 'desc' : 'asc';
                }
            }
            this.foundset.sortColumns = sortCol + ' ' + sqlSortDirection;
            this.foundset.sort([{ name: sortCol, direction: sqlSortDirection }]).then(() => {
                this.dataStream.next(this.foundset.viewPort.rows);
            });
        }
    }

    cellClick(rowIdx: number, colIdx: number, record: any, e: MouseEvent, columnId: string) {
        if (this.onCellDoubleClick && this.onCellClick) {
            const innerThis: ServoyExtraTable = this;
            if (innerThis.lastClicked === rowIdx * colIdx) {
                window.clearTimeout(this.timeoutID);
                innerThis.lastClicked = -1;
                innerThis.timeoutID = null;
                innerThis.onCellDoubleClick(rowIdx, colIdx, record, e, columnId);
            } else {
                innerThis.lastClicked = rowIdx * colIdx;
                innerThis.timeoutID = window.setTimeout(() => {
                    innerThis.timeoutID = null;
                    innerThis.lastClicked = -1;
                    innerThis.onCellClick(rowIdx, colIdx, record, e, columnId);
                }, 250);
            }
        } else if (this.onCellClick) {
            this.onCellClick(rowIdx, colIdx, this.foundset.viewPort.rows[rowIdx], e, columnId);
        }
    }

    getColumnStyle(column: number) {
        let columnStyle = this.columnStyleCache[column];
        if (columnStyle) return columnStyle;

        columnStyle = { overflow: 'hidden' };
        this.columnStyleCache[column] = columnStyle;
        const w = this.getNumberFromPxString(this.columns[column].width);
        if (w > -1) {
            columnStyle['min-width'] = columnStyle['max-width'] = columnStyle['width'] = w + 'px';
        } else if (this.columns[column].width && (this.columns[column].width) !== 'auto') {
            columnStyle['width'] = this.columns[column].width;
        } else {
            const autoColumnPercentage = this.getAutoColumnPercentage();
            if (this.autoColumnPercentage) {
                columnStyle['width'] = autoColumnPercentage + '%';
            } else {
                if (!this.autoColumns) this.setColumnsToInitalWidthAndInitAutoColumns();
                columnStyle['min-width'] = columnStyle['max-width'] = columnStyle['width'] =
                    Math.floor((this.getComponentWidth() - this.tableWidth - this.scrollWidth) / this.autoColumns.count) + 'px';
            }
        }
        this.updateTableColumnStyleClass(column, columnStyle);
        return columnStyle;
    }
    getComponentWidth() {
        if (!this.rendered) return 0;
        if (this.componentWidth === undefined && this.getNativeElement().parentElement.parentElement.offsetWidth !== 0) {
            this.componentWidth = Math.floor(this.getNativeElement().parentElement.parentElement.offsetWidth);
        }
        return this.componentWidth;
    }
    getAutoColumnPercentage() {
        let nrColumnsWithPercentage = 0;
        let sumColumnsWithPercentage = 0;
        if (!this.autoColumns) return null;

        for (const autoColumnIdx of Object.keys(this.autoColumns['columns'])) {
            let w = this.columns[autoColumnIdx].width;
            if (w) {
                w = w.trim();
                if (w.indexOf('%') === w.length - 1) {
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

    getNumberFromPxString(s: string) {
        let numberFromPxString = -1;
        if (s) {
            s = s.trim().toLowerCase();
            if (s.indexOf('px') === s.length - 2) {
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
        return this.columns[idx].autoResize || (this.columns[idx].width === 'auto');
    }

    setColumnsToInitalWidthAndInitAutoColumns() {
        const newAutoColumns = { columns: {}, minWidth: {}, autoResize: {}, count: 0 };
        if (this.columns) {
            for (let i = 0; i < this.columns.length; i++) {
                if (this.columns[i].initialWidth === undefined) {
                    this.columns[i].initialWidth = this.columns[i].width === undefined ? '' : this.columns[i].width;
                } else {
                    this.columns[i].width = this.columns[i].initialWidth;
                }

                const minWidth = this.getNumberFromPxString(this.columns[i].width);
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
        if (this.extraWidth) {
            fixedDelta += this.extraWidth;
            let w = this.getNumberFromPxString(this.columns[this.extraWidthColumnIdx].width);
            w += (0 - this.extraWidth);
            this.columns[this.extraWidthColumnIdx].width = w + 'px';
        }

        this.columnStyleCache = [];
        const oldWidth = this.getAutoResizeColumnsWidth();
        const newWidth = oldWidth + fixedDelta;

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
                    this.columns[i].width = w + 'px';
                    usedDelta += (w - oldW);
                    lastAutoColumnIdx = i;
                } else {
                    this.columns[i].width = this.columns[i].initialWidth;
                }
            }
        }

        if (lastAutoColumnIdx > -1) {
            this.extraWidth = Math.round(Math.abs(fixedDelta) - Math.abs(usedDelta));
            this.extraWidthColumnIdx = lastAutoColumnIdx;
            if (this.extraWidth) {
                if (fixedDelta < 0) this.extraWidth = 0 - this.extraWidth;
                let w = this.getNumberFromPxString(this.columns[lastAutoColumnIdx].width);
                w += this.extraWidth;
                this.columns[lastAutoColumnIdx].width = w + 'px';
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

    getSortStyleClass(column: number) {
        let lv_styles = '';
        if (this.enableSort) {
            if ((this.sortColumnIndex === -1 && column === 0) || this.sortColumnIndex === column) {
                lv_styles = this.sortStyleClass;
            }
        }
        return this.columns[column].headerStyleClass === undefined ? lv_styles : lv_styles + ' ' + this.columns[column].headerStyleClass;
    }

    public getSortClass(column: number) {
        let sortClass = 'table-servoyextra-sort-hide';
        if (this.enableSort) {
            let direction: string;
            let isGetSortFromSQL = this.sortColumnIndex < 0;
            if (column === this.sortColumnIndex) {
                direction = this.sortDirection;
                if (!direction) {
                    isGetSortFromSQL = true;
                }
            }
            if (isGetSortFromSQL) {
                if (this.foundset && this.foundset.sortColumns && this.columns[column].dataprovider) {
                    const sortCol = this.columns[column].dataprovider.idForFoundset;
                    const sortColumnsA = this.foundset.sortColumns.split(' ');

                    if (sortCol === sortColumnsA[0]) {
                        direction = sortColumnsA[1].toLowerCase() === 'asc' ? 'up' : 'down';
                    }
                }
            }

            if (direction) {
                sortClass = 'table-servoyextra-sort-show-' + direction + ' ' + this['sort' + direction + 'Class'];
            }
        }
        if (this.currentSortClass.length <= column || this.currentSortClass[column] !== sortClass) {
            if (this.sortClassUpdateTimer) window.clearTimeout(this.sortClassUpdateTimer);

            this.sortClassUpdateTimer = window.setTimeout(() => {
                const tbody = this.elementRef !== undefined ? this.getNativeElement().getElementsByTagName('tbody') : undefined;
                if (tbody) {
                    if (tbody[0].clientHeight > 0) {
                        this.updateTBodyStyle(tbody[0]);
                    } else {
                        this.sortClassUpdateTimer = window.setTimeout(() => {
                            this.updateTBodyStyle(tbody[0]);
                        }, 200);
                    }
                }
            }, 100);
            this.currentSortClass[column] = sortClass;
        }
        return sortClass;
    }
    updateTBodyStyle(tBodyEl: HTMLElement) {
        const tBodyStyle = {};
        const componentWidth = this.getComponentWidth();
        tBodyStyle['width'] = componentWidth + 'px';
        const tblHead = this.getNativeElement().getElementsByTagName('thead')[0];
        if (tblHead.style.display !== 'none') {
            tBodyStyle['top'] = tblHead.offsetHeight + 'px';
        }
        if (this.showPagination()) {
            const pagination = this.getNativeElement().getElementsByTagName('ngb-pagination');
            if (pagination[0] && pagination[0].children[0]) {
                tBodyStyle['bottom'] = (pagination[0].children[0].clientHeight + 2) + 'px';
                this.renderer.setStyle(pagination[0].children[0], 'margin-bottom', '0');
                this.renderer.setStyle(pagination[0].children[0], 'position', 'absolute');
                this.renderer.setStyle(pagination[0].children[0], 'bottom', '0');
                this.computeTableHeight();
            }
        }

        for (const p in tBodyStyle) {
            if (tBodyStyle.hasOwnProperty(p)) {
                this.renderer.setStyle(tBodyEl, p, tBodyStyle[p]);
            }
        }
        this.renderer.setStyle(this.viewPort._contentWrapper.nativeElement.parentElement, 'height', tBodyEl['clientHeight'] + 'px');
    }


    getTHeadStyle() {
        const tHeadStyle = {};
        if (this.enableSort || this.onHeaderClick) {
            tHeadStyle['cursor'] = 'pointer';
        }
        tHeadStyle['left'] = -this.getNativeElement().getElementsByTagName('table')[0].scrollLeft + 'px';
        return tHeadStyle;
    }

    updateTableColumnStyleClass(columnIndex: number, style: any) {
        if (!this.columnCSSRules[columnIndex]) {
            const clsName = '#table_' + this.servoyApi.getMarkupId() + ' .c' + columnIndex;
            if (!this.columnCSSRules[columnIndex]) {
                if (!this.targetStyleSheet) {
                    const elem = document.createElement('style');
                    elem.type = 'text/css';
                    document.getElementsByTagName('head')[0].appendChild(elem);
                    this.targetStyleSheet = document.styleSheets[document.styleSheets.length - 1] as CSSStyleSheet;
                }
                const rules = this.targetStyleSheet.cssRules || this.targetStyleSheet.rules;
                this.targetStyleSheet.insertRule(clsName + '{}', rules.length);
                this.columnCSSRules[columnIndex] = rules[rules.length - 1];
                this.columnCSSRules[columnIndex].style['height'] = this.minRowHeight;
            }
        }

        for (const p in style) {
            if (style.hasOwnProperty(p)) {
                this.columnCSSRules[columnIndex].style[p] = style[p];
            }
        }
    }

    onResizeEnd(event: ResizeEvent, columnIndex: number): void {
        window.clearTimeout(this.timeoutID);
        const headers = this.getNativeElement().getElementsByTagName('th');
        const newWidth = Math.floor(event.rectangle.width) + 'px';
        this.renderer.setStyle(headers[columnIndex], 'width', newWidth);
        this.renderer.setStyle(headers[columnIndex], 'min-width', newWidth);
        this.renderer.setStyle(headers[columnIndex], 'max-width', newWidth);
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
            const idxInSelected = newSelection.indexOf(idxInFs);
            if (idxInSelected === -1) {
                newSelection.push(idxInFs);
            } else if (newSelection.length > 1) {
                newSelection.splice(idxInSelected, 1);
            }
        } else if (event.shiftKey) {
            let start = -1;
            if (this.foundset.selectedRowIndexes) {
                for (const j of Object.keys(this.foundset.selectedRowIndexes)) {
                    if (start === -1 || start > this.foundset.selectedRowIndexes[j]) {
                        start = this.foundset.selectedRowIndexes[j];
                    }
                }
            }
            let stop = idxInFs;
            if (start > idxInFs) {
                stop = start;
                start = idxInFs;
            }
            newSelection = [];
            for (let n = start; n <= stop; n++) {
                newSelection.push(n);
            }
        }
        this.scrollToSelectionNeeded = false; // we don't need to scroll to selection when we select a record by clicking on it
        this.foundset.selectedRowIndexes = newSelection;
        this.foundset.requestSelectionUpdate(newSelection);
    }

    onScroll() {
        if (!this.viewPort) return;
        if (this.onViewPortChanged) {
            this.onViewPortChanged(this.viewPort.getRenderedRange().start, this.viewPort.getRenderedRange().end);
        }
    }

    public setSelectedHeader(columnIndex: number) {
        if (this.onHeaderClick) {
            if (this.enableSort && (this.sortColumnIndex !== columnIndex)) {
                this.sortDirection = null;
            }
            this.headerClicked(columnIndex);
        } else {
            this.sortColumnIndex = columnIndex;
            this.doFoundsetSQLSort(this.sortColumnIndex);
        }
    }

    public getViewPortPosition(): number[] {
        if (!this.viewPort) return null;
        return [this.viewPort.getRenderedRange().start, this.viewPort.getRenderedRange().end];
    }

    public requestFocus(mustExecuteOnFocusGainedMethod: boolean) {
        const tbl = this.getNativeElement().getElementsByTagName('table')[0];
        this.skipOnce = mustExecuteOnFocusGainedMethod === false;
        tbl.focus();
    }

    showPagination() {
        return this.pageSize && this.foundset && (this.foundset.serverSize > this.pageSize || this.foundset.hasMoreRows);
    }

    modifyPage(page: number) {
        this.currentPage = page;
        this.changedPage = true;
        const startIndex = this.pageSize * (this.currentPage - 1);
        if (!this.goToPage(startIndex)) {
            if (this.prevPage < this.currentPage) {
                this.renderedRows.last.elRef.nativeElement.scrollIntoView();
            }
            else {
                this.renderedRows.first.elRef.nativeElement.scrollIntoView();
            }
            this.renderedRows.changes.pipe(first())
                .subscribe(() => {
                    this.goToPage(startIndex);
                });
        }
    }

    goToPage(startIndex: number) {
        const startPage: TableRow = this.renderedRows.find((element) => element.svyTableRow === startIndex);
        if (startPage) {
            startPage.elRef.nativeElement.scrollIntoView();
            console.log(startPage.elRef.nativeElement);
            console.log(this.foundset.viewPort);
            console.log(this.renderedRows.length + " " + this.minBuff + " " + this.maxBuff + " " + this.averageRowHeight);
            this.prevPage = this.currentPage;
            return true;
        }
        console.log("returning false for " + startIndex);
        return false;
    }

    getRowClass(idx: number) {
        let rowClass = '';
        if (this.foundset.selectedRowIndexes.indexOf(idx) > -1) {
            rowClass += 'table-servoyextra-selected ';
        }
        if (this.rowStyleClassDataprovider) {
            rowClass += this.rowStyleClassDataprovider[idx % this.pageSize];
        }
        return rowClass;
    }

    keypressed(event: KeyboardEvent) {
        const fs = this.foundset;
        if (fs.selectedRowIndexes && fs.selectedRowIndexes.length > 0) {
            let selectionChanged = false;
            const oldSelectedIdxs = fs.selectedRowIndexes.slice();
            const selection = fs.selectedRowIndexes[0];
            if (event.key === 'PageUp') { // PAGE UP KEY
                if (this.keyCodeSettings && !this.keyCodeSettings.pageUp) return;

                const firstVisibleIndex = this.showPagination() ? this.pageSize * Math.trunc(selection / this.pageSize) : 1;
                fs.selectedRowIndexes = [firstVisibleIndex];
                selectionChanged = (selection !== firstVisibleIndex);
                this.log.spam('svy extra table * keyPressed; scroll on PG UP');
                this.viewPort.scrollToIndex(firstVisibleIndex);
            } else if (event.key === 'PageDown') { // PAGE DOWN KEY
                if (this.keyCodeSettings && !this.keyCodeSettings.pageDown) return;

                let lastVisibleIndex = this.showPagination() ? this.pageSize * (Math.trunc(selection / this.pageSize) + 1) - 1 : (this.foundset.viewPort.rows.length - 1);
                if (lastVisibleIndex > fs.serverSize - 1) lastVisibleIndex = fs.serverSize - 1;
                fs.selectedRowIndexes = [lastVisibleIndex];
                selectionChanged = (selection !== lastVisibleIndex);
                this.log.spam('svy extra table * keyPressed; scroll on PG DOWN');
                this.viewPort.scrollToIndex(lastVisibleIndex);
            } else if (event.key === 'ArrowUp') { // ARROW UP KEY
                if (this.keyCodeSettings && !this.keyCodeSettings.arrowUp) return;

                if (selection > 0) {
                    fs.selectedRowIndexes = [selection - 1];
                    this.viewPort.scrollToIndex(selection - 1);
                    this.scrollToSelectionNeeded = false;
                    selectionChanged = true;
                }
                event.preventDefault();
            } else if (event.key === 'ArrowDown') { // ARROW DOWN KEY
                if (this.keyCodeSettings && !this.keyCodeSettings.arrowDown) return;

                if (selection + 1 < (fs.viewPort.startIndex + fs.viewPort.size)) {
                    fs.selectedRowIndexes = [selection + 1];
                    this.viewPort.scrollToIndex(selection + 1);
                    this.scrollToSelectionNeeded = false;
                    selectionChanged = true;
                }
                event.preventDefault();
            } else if (event.key === 'Enter') { // ENTER KEY
                if (!this.keyCodeSettings.enter) return;
                if (this.onCellClick) {
                    this.onCellClick(selection + 1, null, fs.viewPort.rows[selection]);
                }
            } else if (event.key === 'Home') { // HOME
                if (this.keyCodeSettings && !this.keyCodeSettings.home) return;
                if (fs.selectedRowIndexes[0] !== 0) {
                    fs.selectedRowIndexes = [0];
                    this.viewPort.scrollToIndex(0);
                    selectionChanged = true;
                }
                event.preventDefault();
                event.stopPropagation();

            } else if (event.key === 'End') { // END
                if (this.keyCodeSettings && !this.keyCodeSettings.end) return;

                const endIndex = fs.viewPort.size - 1 + fs.viewPort.startIndex;
                if (fs.selectedRowIndexes[0] !== endIndex) {
                    fs.selectedRowIndexes = [endIndex];
                    selectionChanged = true;
                    this.scrollToSelectionNeeded = false;
                    this.viewPort.scrollToOffset(this.getNumberFromPxString(this.viewPort._totalContentHeight));
                    setTimeout(() => {
                        const last = this.viewPort._contentWrapper.nativeElement.lastElementChild;
                        last.scrollIntoView(false);
                    }, 100);
                }
                event.preventDefault();
                event.stopPropagation();
            }

            if (selectionChanged) {
                this.selectedRowIndexesChanged(oldSelectedIdxs);
            }
        }
    }

    private setCurrentPageIfNeeded() {
        if (this.changedPage) {
            this.changedPage = false;
            return;
        }
        if (this.showPagination()) {
            if (this.pageSize > 0) {
                this.currentPage = Math.floor(this.idx / this.pageSize) + 1;
                this.cdRef.detectChanges();
            } else {
                window.setTimeout(() => {
                    this.setCurrentPageIfNeeded();
                }, 100);
            }
        }
    }

    private selectedRowIndexesChanged(oldValue: number[]) {
        if (this.foundset.selectedRowIndexes.length > 0) {
            if (this.foundset.selectedRowIndexes !== oldValue || this.lastSelectionFirstElement !== this.foundset.selectedRowIndexes[0]) {
                if (this.lastSelectionFirstElement !== this.foundset.selectedRowIndexes[0]) {
                    this.log.spam('svy extra table * selectedRowIndexes changed; scrollToSelectionNeeded = true');
                    if ((this.lastSelectionFirstElement - this.foundset.viewPort.startIndex) < this.viewPort.getRenderedRange().start ||
                        (this.lastSelectionFirstElement - this.foundset.viewPort.startIndex) > this.viewPort.getRenderedRange().end) {
                        this.loadMoreRecords(this.lastSelectionFirstElement, true);
                    } else {
                        this.lastSelectionFirstElement = this.foundset.selectedRowIndexes[0] + this.foundset.viewPort.startIndex;
                        if (this.scrollToSelectionNeeded) {
                            this.scrollToSelection();
                        } else {
                            this.scrollToSelectionNeeded = true;
                        }
                    }
                }
            }
        } else {
            this.lastSelectionFirstElement = -1;
        }
    }

    private sortColumnsChanged(event: FoundsetChangeEvent) {
        let sortSet = false;
        const sortColumnsA = event.sortColumnsChanged.newValue.split(/[\s,]+/);
        if (sortColumnsA.length >= 2) {
            for (let i = 0; i < this.columns.length; i++) {
                if (this.columns[i].dataprovider && sortColumnsA[0] === this.columns[i].dataprovider.idForFoundset) {
                    this.sortColumnIndex = i;
                    this.sortDirection = sortColumnsA[1].toLowerCase() === 'asc' ? 'up' : 'down';
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
            if (this.viewPort.getDataLength() > 0) {
                if ((this.lastSelectionFirstElement - this.foundset.viewPort.startIndex) < this.viewPort.getRenderedRange().start ||
                    (this.lastSelectionFirstElement - this.foundset.viewPort.startIndex) > this.viewPort.getRenderedRange().end) {
                    this.loadMoreRecords(this.lastSelectionFirstElement, true);
                } else {
                    this.viewPort.scrollToOffset((this.lastSelectionFirstElement - this.foundset.viewPort.startIndex) * this.averageRowHeight);
                    this.currentPage = Math.floor(this.lastSelectionFirstElement / this.pageSize) + 1;
                    this.cdRef.detectChanges();
                }
            } else {
                window.setTimeout(() => {
                    // first time we need to wait a bit before we scroll
                    this.scrollToSelection();
                }, 400);
            }
        }
    }

    private computeTableHeight() {
        const tbody = this.getNativeElement().getElementsByTagName('tbody');
        if (tbody && (tbody[0].scrollHeight > tbody[0].clientHeight && (this.scrollWidth === 0))) {
            this.scrollWidth = tbody[0].offsetWidth - tbody[0].clientWidth + 17;
        } else if (tbody && (tbody[0].scrollHeight <= tbody[0].clientHeight) && (this.scrollWidth > 0)) {
            this.scrollWidth = 0;
        }

        if (!this.servoyApi.isInAbsoluteLayout()) {
            this.renderer.setStyle(this.getNativeElement(), 'position', 'relative');

            const pagination = this.getNativeElement().getElementsByTagName('ngb-pagination');
            let paginationHeight = 0;
            if (pagination[0] && pagination[0].children[0]) {
                paginationHeight = pagination[0].children[0].clientHeight;
            }
            if (this.columns) {
                if (this.responsiveDynamicHeight) {
                    const headerHeight = this.getNativeElement().getElementsByTagName('th')[0].getBoundingClientRect().height;
                    const height = headerHeight + this.pageSize  * this.averageRowHeight + paginationHeight;
                    if (this.responsiveHeight === 0) {
                        this.renderer.setStyle(this.getNativeElement(), 'height', height + 'px');
                        this.renderer.setStyle(this.viewPort._contentWrapper.nativeElement.parentElement, 'height', height + 'px');
                    } else {
                        this.renderer.setStyle(this.getNativeElement(), 'height', this.responsiveHeight + 'px');
                        this.renderer.setStyle(this.getNativeElement(), 'max-height', this.responsiveHeight + 'px');
                        this.renderer.setStyle(this.viewPort._contentWrapper.nativeElement.parentElement, 'height', (this.responsiveHeight - paginationHeight) + 'px');
                        window.setTimeout(this.calculateTableHeight, 100, this);
                    }
                } else if (this.responsiveHeight === 0) {
                    this.renderer.setStyle(this.getNativeElement(), 'height', '100%');
                    this.renderer.setStyle(this.viewPort._contentWrapper.nativeElement.parentElement, 'height', (this.getNativeElement().clientHeight - paginationHeight) + 'px');
                } else {
                    this.renderer.setStyle(this.getNativeElement(), 'height', this.responsiveHeight + 'px');
                    this.renderer.setStyle(this.viewPort._contentWrapper.nativeElement.parentElement, 'height', (this.responsiveHeight - paginationHeight) + 'px');
                }
            }
        }
    }

    private recordsLoaded() {
        this.dataStream.next([...this.foundset.viewPort.rows]);
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

    private headerClicked(i: number, event?: MouseEvent): void {
        this.onHeaderClick(i, this.sortDirection, event, this.columns[i].id)
            .then((ret: string) => {
                if (ret === 'override')
                    return;
                if (this.enableSort) {
                    this.doFoundsetSQLSort(i);
                }
            }, (reason: any) => {
                this.log.error(reason);
            });
    }
}
