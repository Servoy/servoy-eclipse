/* eslint-disable max-len */
import {
    Component, Input, TemplateRef, ViewChild, ElementRef, AfterViewInit, Renderer2,
    HostListener, ChangeDetectorRef, OnDestroy, Inject, SimpleChange, ChangeDetectionStrategy, SimpleChanges, Injector
} from '@angular/core';
import { AbstractFormComponent, FormComponent } from '../../ngclient/form/form_component.component';
import { DesignFormComponent } from '../../designer/designform_component.component';
import { ViewportService } from '../../ngclient/services/viewport.service';
import { ServoyBaseComponent } from '@servoy/public';
import { FormComponentValue } from '../../ngclient/converters/formcomponent_converter';
import { FormService } from '../../ngclient/form.service';
import { ServoyService } from '../../ngclient/servoy.service';
import { ComponentCache, FormComponentCache, IApiExecutor, instanceOfApiExecutor, StructureCache } from '../../ngclient/types';
import { LoggerFactory, LoggerService, ChangeType, ViewPortRow, FoundsetChangeEvent, IFoundset, IChildComponentPropertyValue } from '@servoy/public';
import { isEmpty } from 'lodash-es';
import { DOCUMENT } from '@angular/common';
import { ServoyApi } from '../../ngclient/servoy_api';
import { GridOptions, IServerSideDatasource, IServerSideGetRowsParams } from '@ag-grid-community/core';
import { RowRenderer } from './row-renderer.component';
import { AgGridAngular } from '@ag-grid-community/angular';
import { TypesRegistry } from '../../sablo/types_registry';
import { ConverterService } from '../../sablo/converter.service';

const AGGRID_CACHE_BLOCK_SIZE = 50;
const AGGRID_MAX_BLOCKS_IN_CACHE = 2;

@Component({
    selector: 'servoycore-listformcomponent',
    styleUrls: ['./listformcomponent.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
    <div class="svy-listformcomponent" [ngClass]='styleClass' #element>
    <ng-container *ngIf="useScrolling">
        <ag-grid-angular #aggrid
            [gridOptions]="agGridOptions"
            [ngStyle]="getAGGridStyle()">
        </ag-grid-angular>
    </ng-container>

    <ng-container *ngIf="!useScrolling">
        <div *ngIf="containedForm&&containedForm.absoluteLayout">
            <div tabindex="-1" (click)="onRowClick(row, $event)" *ngFor="let row of getViewportRows(); let i = index" [class]="getRowClasses(i)" [ngStyle]="{'height.px': getRowHeight(), 'width' : getRowWidth()}" style="display:inline-block; position: relative">
                <div *ngFor="let item of cache.items" [svyContainerStyle]="item" [svyContainerLayout]="item.layout" class="svy-wrapper" style="position:absolute"> <!-- wrapper div -->
                    <ng-template [ngTemplateOutlet]="getRowItemTemplate(item)" [ngTemplateOutletContext]="{ state:getRowItemState(item, row, i), callback:this, row:row, i:i }"></ng-template>  <!-- component  -->
                </div>
            </div>
        </div>
        <div *ngIf="containedForm&&!containedForm.absoluteLayout">
            <div tabindex="-1" (click)="onRowClick(row, $event)" *ngFor="let row of getViewportRows(); let i = index; trackBy: trackByFn" [class]="getRowClasses(i)" [ngStyle]="{'width' : getRowWidth()}" style="display:inline-block">
                <ng-template *ngFor="let item of cache.items" [ngTemplateOutlet]="getRowItemTemplate(item)" [ngTemplateOutletContext]="{ state: getRowItemState(item, row, i), callback:this, row:row, i:i}"></ng-template>  <!-- component or responsive div  -->
            </div>
        </div>
        <div class='svyPagination'><div #firstelement style='text-align:center;cursor:pointer;display:inline;visibility:hidden;padding:3px;white-space:nowrap;vertical-align:middle;background-color:rgb(255, 255, 255, 0.6);' (click)='firstPage()' ><i class='fa fa-backward' aria-hidden="true"></i></div><div  #leftelement style='text-align:center;cursor:pointer;visibility:hidden;display:inline;padding:3px;white-space:nowrap;vertical-align:middle;background-color:rgb(255, 255, 255, 0.6);' (click)='moveLeft()' ><i class='fa fa-chevron-left' aria-hidden="true"></i></div><div #rightelement style='text-align:center;cursor:pointer;visibility:hidden;display:inline;padding:3px;white-space:nowrap;vertical-align:middle;background-color:rgb(255, 255, 255, 0.6);' (click)='moveRight()'><i class='fa fa-chevron-right' aria-hidden="true"></i></div></div>
    </ng-container>
</div>

<ng-template  #svyResponsiveDiv  let-state="state" let-row="row" let-i="i">
    <div [svyContainerStyle]="state" [svyContainerClasses]="state.classes" [ngClass]="getDesignNGClass(state)" [svyContainerAttributes]="state.attributes" class="svy-layoutcontainer">
        <ng-template *ngFor="let item of state.items" [ngTemplateOutlet]="getRowItemTemplate(item)" [ngTemplateOutletContext]="{ state:getRowItemState(item, row, i), callback:this, row:row, i:i}"></ng-template>
    </div>
</ng-template>
<ng-template  #formComponentAbsoluteDiv  let-state="state" let-row="row" let-i="i">
          <div [svyContainerStyle]="state.formComponentProperties" [svyContainerLayout]="state.formComponentProperties.layout" [svyContainerClasses]="state.formComponentProperties.classes" [svyContainerAttributes]="state.formComponentProperties.attributes" style="position:relative" class="svy-formcomponent">
               <div *ngFor="let item of state.items" [svyContainerStyle]="item" [svyContainerLayout]="item.layout" class="svy-wrapper" [ngStyle]="item.model.visible === false && {'display': 'none'}" style="position:absolute"> <!-- wrapper div -->
                   <ng-template [ngTemplateOutlet]="getRowItemTemplate(item)" [ngTemplateOutletContext]="{ state:getRowItemState(item, row, i), callback:this, row:row, i:i}"></ng-template>  <!-- component  -->
               </div>
          </div>
</ng-template>
<ng-template  #formComponentResponsiveDiv  let-state="state" let-row="row" let-i="i">
    <div [svyContainerStyle]="state.formComponentProperties" [svyContainerLayout]="state.formComponentProperties.layout" [svyContainerClasses]="state.formComponentProperties.classes" [svyContainerAttributes]="state.formComponentProperties.attributes" class="svy-formcomponent">
        <ng-template *ngFor="let item of state.items" [ngTemplateOutlet]="getRowItemTemplate(item)" [ngTemplateOutletContext]="{ state:getRowItemState(item, row, i), callback:this, row:row, i:i}"></ng-template>  <!-- component  -->
    </div>
</ng-template>
<!-- structure template generate start -->
<!-- structure template generate end -->

`
})
export class ListFormComponent extends ServoyBaseComponent<HTMLDivElement> implements AfterViewInit, OnDestroy, IApiExecutor {

    @Input() containedForm: FormComponentValue;
    @Input() foundset: IFoundset;
    @Input() selectionClass: string;
    @Input() styleClass: string;
    @Input() rowStyleClass: string;
    @Input() rowStyleClassDataprovider: string[];
    @Input() rowEditableDataprovider: boolean[];
    @Input() rowEnableDataprovider: boolean[];
    @Input() responsivePageSize: number;
    @Input() responsiveHeight: number;
    @Input() pageLayout: string;
    @Input() readOnly: boolean;
    @Input() editable: boolean;
    @Input() onSelectionChanged: (event: any) => void;

    @ViewChild('svyResponsiveDiv', { static: true }) readonly svyResponsiveDiv: TemplateRef<any>;
    @ViewChild('formComponentAbsoluteDiv', { static: true }) readonly formComponentAbsoluteDiv: TemplateRef<any>;
    @ViewChild('formComponentResponsiveDiv', { static: true }) readonly formComponentResponsiveDiv: TemplateRef<any>;
    // structure viewchild template generate start
    // structure viewchild template generate end
    @ViewChild('element', { static: true }) elementRef: ElementRef;

    // used for paging
    @ViewChild('firstelement', { static: false }) elementFirstRef: ElementRef;
    @ViewChild('leftelement', { static: false }) elementLeftRef: ElementRef;
    @ViewChild('rightelement', { static: false }) elementRightRef: ElementRef;

    // used for scrolling with AGGGrid
    @ViewChild('aggrid', { static: false }) agGrid: AgGridAngular;

    // TODO: remove this when switching completely to scrollable LFC
    useScrolling = false;

    // used for scrolling with AGGGrid
    parent: AbstractFormComponent;
    agGridOptions: GridOptions;
    numberOfColumns = 1;

    // used for paging
    page = 0;
    numberOfCells = 0;
    selectionChangedByKey = false;

    cache: FormComponentCache;
    removeListenerFunction: () => void;
    private componentCache: Array<{ [property: string]: ServoyBaseComponent<any> }> = [];
    private log: LoggerService;
    private rowItems: Array<IChildComponentPropertyValue | FormComponentCache>;

    private designerViewportRows = [{} as ViewPortRow];

    // used for paging
    private waitingForLoad = false;

    constructor(protected readonly renderer: Renderer2,
        private formservice: FormService,
        private servoyService: ServoyService,
        private typesRegistry: TypesRegistry,
        private converterService: ConverterService<unknown>,
        cdRef: ChangeDetectorRef,
        logFactory: LoggerFactory,
        private _injector: Injector,
        @Inject(DOCUMENT) private doc: Document) {
        super(renderer, cdRef);
        try {
            this.parent = this._injector.get<FormComponent>(FormComponent);
        } catch (e) {
            //ignore
        }

        if (!this.parent) {
            this.parent = this._injector.get<DesignFormComponent>(DesignFormComponent);
        }
        this.log = logFactory.getLogger('ListFormComponent');
    }

    @HostListener('keydown', ['$event'])
    handleKeyDown(event: any) {
        if (!this.foundset.multiSelect && event.key === 'ArrowUp' || event.key === 'ArrowDown') {
            let selectedRowIndex = this.foundset.selectedRowIndexes[0];
            if (event.key === 'ArrowUp') {
                if (!this.useScrolling) {
                    // move to the previous page if the first element (not from the first page) is selected
                    if (this.page !== 0 && selectedRowIndex / (this.page) === this.numberOfCells) {
                        this.moveLeft();
                    }
                }
                selectedRowIndex--;
            } else if (event.key === 'ArrowDown') { // keydown
                selectedRowIndex++;
                if (!this.useScrolling) {
                    // move to the next page if the last element (not from the last page) is selected
                    if (selectedRowIndex / (this.page + 1) === this.numberOfCells) {
                        this.moveRight();
                    }
                }
            }
            // do not move the selection for the first or last element
            if (selectedRowIndex >= 0 && selectedRowIndex < this.foundset.serverSize) {
                this.foundset.requestSelectionUpdate([selectedRowIndex]);
                this.selectionChangedByKey = true;
                if (this.onSelectionChanged) {
                    this.onSelectionChanged(event);
                }
            }
        }
    }

    trackByFn(index: number, row: ViewPortRow) {
        return row._svyRowId;
    }

    onRowClick(row: any, event: Event) {
        for (let i = 0; i < this.foundset.viewPort.rows.length; i++) {
            if (this.foundset.viewPort.rows[i][ViewportService.ROW_ID_COL_KEY] === row['_svyRowId']) {
                const index = i + this.foundset.viewPort.startIndex;
                const selected = this.foundset.selectedRowIndexes;
                if (!selected || selected.indexOf(index) === -1) {
                    this.foundset.requestSelectionUpdate([index]);
                    if (this.onSelectionChanged) {
                        this.onSelectionChanged(event);
                    }
                }
                break;
            }
        }
    }

    ngOnChanges(changes: SimpleChanges) {
        super.ngOnChanges(changes);
        if (this.containedForm && this.containedForm.childElements) {
            this.rowItems = [];
            this.containedForm.childElements.forEach(elem => {
                if (elem.specName === 'servoycore-formcomponent') // TODO this used elem.type before which was always camel-case; does this mean that this if should be removed instead of making it work?
                    this.rowItems.push(this.parent.getFormCache().getFormComponent(elem.name));
                else this.rowItems.push(elem);
            });
        }
    }

    svyOnInit() {
        super.svyOnInit();

        this.cache = this.parent.getFormCache().getFormComponent(this.name);

        if (this.servoyApi.isInDesigner()) {
            return;
        }

        this.useScrolling = true;
        let pagingMode = this.servoyApi.getClientProperty('ListFormComponent.pagingMode');
        if (pagingMode === null || pagingMode === undefined) {
            pagingMode = this.servoyService.getUIProperties().getUIProperty('ListFormComponent.pagingMode');
        }
        if (pagingMode !== null && pagingMode !== undefined) {
            this.useScrolling = false;
        }

        if (this.useScrolling) {
            this.agGridOptions = {
                suppressContextMenu: true,
                context: {
                    componentParent: this
                },
                components: {
                    'row-renderer': RowRenderer
                },
                headerHeight: 0,
                defaultColDef: {
                    flex: 1
                },
                columnDefs: [
                    { cellRenderer: 'row-renderer', autoHeight: true }
                ],
                rowModelType: 'serverSide',
                cacheBlockSize: AGGRID_CACHE_BLOCK_SIZE,
                infiniteInitialRowCount: AGGRID_CACHE_BLOCK_SIZE,
                //maxBlocksInCache: AGGRID_MAX_BLOCKS_IN_CACHE,
                rowHeight: this.getRowHeight(),
                navigateToNextCell: (params: any) => {
                    const previousCell = params.previousCellPosition;
                    const suggestedNextCell = params.nextCellPosition;

                    const KEY_UP = 38;
                    const KEY_DOWN = 40;

                    let selectedIdx = this.foundset.selectedRowIndexes[0];

                    switch (params.key) {
                        case KEY_DOWN:
                            selectedIdx = selectedIdx + 1;
                            if (selectedIdx >= this.foundset.serverSize) selectedIdx = this.foundset.serverSize - 1;
                            suggestedNextCell.rowIndex = Math.floor(selectedIdx / this.getNumberOfColumns());
                            return suggestedNextCell;
                        case KEY_UP:
                            selectedIdx = selectedIdx - 1;
                            if (selectedIdx < 0) selectedIdx = 0;
                            suggestedNextCell.rowIndex = Math.floor(selectedIdx / this.getNumberOfColumns());
                            return suggestedNextCell;
                        default:
                            return previousCell;
                    }
                },
                onFirstDataRendered: (params: any) => {
                    this.scrollToSelection();
                }
            };
        }

        if (!this.useScrolling) {
            this.removeListenerFunction = this.foundset.addChangeListener((event: FoundsetChangeEvent) => {
                if (event.serverFoundsetSizeChanged) this.updatePagingControls();
                if (event.viewportRowsUpdated) {
                    const changes = event.viewportRowsUpdated;
                    let insertOrDeletes = false;
                    changes.forEach(change => {
                        insertOrDeletes = insertOrDeletes || change.type === ChangeType.ROWS_INSERTED || change.type === ChangeType.ROWS_DELETED;
                    });
                    if (insertOrDeletes) this.calculateCells();
                } else {
                    if (event.viewportRowsCompletelyChanged) {
                        this.calculateCells();
                        return;
                    } else if (event.fullValueChanged) {
                        this.foundset = event.fullValueChanged.newValue;
                        this.calculateCells();
                        return;
                    }

                    let viewportSizeAfterShiftingIsDone = this.foundset.viewPort.size;
                    if (event.viewPortStartIndexChanged) {
                        // an insert/delete before current page made viewport start index no longer match page start index; adjust
                        const shiftedPageDelta = this.page * this.numberOfCells - this.foundset.viewPort.startIndex; // can be negative (insert) or positive(delete)
                        if (shiftedPageDelta !== 0) {
                            const wantedVPSize = this.foundset.viewPort.size;
                            const wantedVPStartIndex = this.page * this.numberOfCells;
                            const serverSize = this.foundset.serverSize;

                            // so shifting means loading "shiftedPageDelta" more/less in one end of the viewport and "shiftedPageDelta" less/more at the other end

                            // when load extra would request more records after, there might not be enough records in the foundset (deleted before)
                            let loadExtraCorrected = shiftedPageDelta;
                            if (loadExtraCorrected > 0 /*so shift right*/ && wantedVPStartIndex + wantedVPSize > serverSize)
                                loadExtraCorrected -= (wantedVPStartIndex + wantedVPSize - serverSize);
                            if (loadExtraCorrected !== 0) {
                                this.foundset.loadExtraRecordsAsync(loadExtraCorrected, true);
                                viewportSizeAfterShiftingIsDone += Math.abs(loadExtraCorrected);
                            }

                            // load less if it happens at the end - might need to let more records slide-in the viewport if available (insert before)
                            let loadLessCorrected = shiftedPageDelta;
                            if (loadLessCorrected < 0 /*so shift left*/ && wantedVPSize < this.numberOfCells && wantedVPStartIndex + wantedVPSize < serverSize) //
                                loadLessCorrected += Math.min(serverSize - wantedVPStartIndex - wantedVPSize, this.numberOfCells - wantedVPSize);
                            if (loadLessCorrected !== 0) {
                                this.foundset.loadLessRecordsAsync(loadLessCorrected, true);
                                viewportSizeAfterShiftingIsDone -= Math.abs(loadLessCorrected);
                            }
                        }
                        this.updateSelection();
                    }

                    if (event.viewPortSizeChanged && this.foundset.serverSize > 0 && (this.page * this.numberOfCells >= this.foundset.serverSize)
                        && this.foundset.viewPort.size === 0 && this.numberOfCells > 0) {
                        this.page = Math.floor((this.foundset.serverSize - 1) / this.numberOfCells);
                        this.calculateCells();
                        return;
                    }


                    // ok now we know startIndex is corrected if needed already; check is size needs to be corrected as well
                    if (event.viewPortSizeChanged) {
                        // see if the new viewport size is larger or smaller then expected

                        // sometimes - due to custom components and services that show forms but they do not properly wait for the formWillShow promise to resolve
                        // before showing the form in the DOM - list form component might end up showing in a container that changed size so numberOfCells is now different
                        // (let's say decreased) but having old foundset viewport data (meanwhile solution server side might have changed foundset); then what happened
                        // is that browser-side list-form-component requested less records based on old foundset data while server-side already had only 1 or 2 records now
                        // in foundset => it got back a viewport of size 0

                        // so although this would not normally happen (viewport size getting changed incorrectly as if the component requested that) we check this to be
                        // resilient to such components/services as well; for example popupWindow used to show forms quickly before getting the updates from server before showing
                        // (a second show of a pop-up window with decreased size and also less records in the foundset); there are other components that could do this for example
                        // bootstrap tabless panel with waitForData property set to false

                        const vpStartIndexForCurrentCalcs = this.page * this.numberOfCells; // this might have already been requested in previous code; might not be the actual present one in browser
                        const vpSizeForCurrentCalcs = viewportSizeAfterShiftingIsDone; // this might have already been requested in previous code; might not be the actual present one in browser

                        const deltaSize = this.numberOfCells - vpSizeForCurrentCalcs;
                        if (deltaSize > 0) {
                            // we could show more records then currently in viewport; see if more are available
                            const availableExtraRecords = this.foundset.serverSize - (vpStartIndexForCurrentCalcs + vpSizeForCurrentCalcs);
                            if (availableExtraRecords > 0) this.foundset.loadExtraRecordsAsync(Math.min(deltaSize, availableExtraRecords), true);
                        } else if (deltaSize < 0) {
                            // we need to show less records; deltaSize is already negative; so it will load less from end of viewport
                            this.foundset.loadLessRecordsAsync(deltaSize, true);
                        } // else it's already ok
                    }

                    this.foundset.notifyChanged(); // let foundset send it's pending requests to server if any
                    this.cdRef.detectChanges();
                }
            });
        } else {
            this.removeListenerFunction = this.foundset.addChangeListener((event: FoundsetChangeEvent) => {
                if (event.requestInfos && event.requestInfos.includes('AGGridDatasourceGetRows')) {
                    return;
                }
                if (event.serverFoundsetSizeChanged ) {
                    this.agGrid.api.setRowCount(event.serverFoundsetSizeChanged.newValue);
                }
                if (event.viewportRowsUpdated) {
                    // copy the viewport data over to the cell
                    // TODO this only is working for "updates", what happens with deletes or inserts?
                    const changes = event.viewportRowsUpdated;
                    let insertOrDeletes = false;
                    changes.forEach(change => {
                        insertOrDeletes = insertOrDeletes || change.type === ChangeType.ROWS_INSERTED || change.type === ChangeType.ROWS_DELETED;
                    });
                    if (insertOrDeletes) {
                        this.agGrid.api.refreshServerSide({ purge: true });
                        this.agGrid.api.setRowCount(this.foundset.serverSize ? this.foundset.serverSize : 0);
                    }
                    else this.agGrid.api.refreshCells();
                } else if (event.viewportRowsCompletelyChanged || event.fullValueChanged) {
                    this.agGrid.api.refreshServerSide({ purge: true });
                }
                if(event.selectedRowIndexesChanged) {
                    this.scrollToSelection();
                }
            });
        }
    }

    private scrollToSelection() {
        if(this.foundset.selectedRowIndexes.length) {
            const rowCount = this.agGrid.api.getDisplayedRowCount();
            if(this.foundset.selectedRowIndexes[0] > rowCount - AGGRID_CACHE_BLOCK_SIZE) {
                this.agGrid.api.setRowCount(Math.min(this.foundset.selectedRowIndexes[0] + AGGRID_CACHE_BLOCK_SIZE, this.foundset.serverSize));
            }
            this.agGrid.api.ensureIndexVisible(this.foundset.selectedRowIndexes[0]);
        }        
    }

    ngAfterViewInit() {
        this.calculateCells();
        if (this.useScrolling) {
            this.agGrid.api.setGridOption('serverSideDatasource', new AGGridDatasource(this));
        }
    }

    ngOnDestroy() {
        if (this.removeListenerFunction != null) {
            this.removeListenerFunction();
            this.removeListenerFunction = null;
        }
        if (this.containedForm && this.containedForm.childElements) {
            this.containedForm.childElements.forEach(component => component.triggerNgOnChangeWithSameRefDueToSmartPropertyUpdate = null);
        }
        this.getViewportRows().forEach(elem => elem._cache = null);
    }

    getViewportRows(): ViewPortRow[] {
        if (this.servoyApi.isInDesigner()) {
            return this.designerViewportRows;
        }
        if (this.numberOfCells === 0) return [];
        return this.foundset.viewPort.rows;
    }

    getStyleClasses(): string[] {
        const classes: Array<string> = new Array();
        if (this.styleClass) {
            classes.push(this.styleClass);
        }
        return classes;
    }

    getRowHeight(): number {
        return this.containedForm.formHeight ? this.containedForm.formHeight : null;
    }

    getRowWidth(): string {
        if (this.pageLayout === 'listview') {
            return '100%';
        }
        if (!this.containedForm.absoluteLayout) {
            return null;
        }
        return this.containedForm.formWidth + 'px';
    }

    getRowItems(): Array<IChildComponentPropertyValue | FormComponentCache> {
        return this.rowItems;
    }

    moveLeft() {
        if (this.page > 0) {
            this.page--;
            this.calculateCells();
        }
    }

    moveRight() {
        this.page++;
        this.calculateCells();
    }

    firstPage() {
        if (this.page !== 0) {
            this.page = 0;
            this.calculateCells();
        }
    }

    getRowItemTemplate(item: StructureCache | FormComponentCache | ComponentCache): TemplateRef<any> {
        if (item instanceof StructureCache) {
            return item.tagname ? this[item.tagname] : this.svyResponsiveDiv;
        }
        if (item instanceof FormComponentCache) {
            return (item as FormComponentCache).responsive ? this.formComponentResponsiveDiv : this.formComponentAbsoluteDiv;
        }
        return this.parent.getTemplateForLFC(item);
    }

    findElement(items: Array<StructureCache | ComponentCache | FormComponentCache>, item: ComponentCache): IChildComponentPropertyValue {
        for (const elem of items) {
            if (elem['name'] === item.name) {
                return (elem as unknown) as IChildComponentPropertyValue;
            }
            if (elem['items']) {
                const found = this.findElement(elem['items'], item);
                if (found) {
                    return found;
                }
            }
        }
        return null;
    }

    getRowItemState(item: StructureCache | FormComponentCache | ComponentCache, row: ViewPortRow, rowIndex: number): Cell | StructureCache | FormComponentCache {
        if (item instanceof StructureCache || item instanceof FormComponentCache) {
            return item;
        }
        let cm: IChildComponentPropertyValue = null;
        if (item instanceof ComponentCache) {
            if (this.servoyApi.isInDesigner()) {
                cm = this.findElement(this.cache.items, item) as IChildComponentPropertyValue;
            } else {
                cm = this.getRowItems().find(elem => elem.name === item.name) as IChildComponentPropertyValue;
            }
        }
        if (row._cache) {
            const cache = row._cache.get(cm.name);
            if (cache && cache.rowIndex === rowIndex) return cache;
        }

        if (item?.model?.servoyAttributes) {
            if (!cm.model.servoyAttributes) {
                cm.model.servoyAttributes = {};
            }
            cm.model.servoyAttributes = Object.assign(cm.model.servoyAttributes, item.model.servoyAttributes);
        }

        if (!cm.triggerNgOnChangeWithSameRefDueToSmartPropertyUpdate) { // declare it only once for all rows in ComponentValue - so it can be used by component_converter.ts
            cm.triggerNgOnChangeWithSameRefDueToSmartPropertyUpdate = (propertiesChangedButNotByRef: { propertyName: string; newPropertyValue: any }[], relativeRowIndex: number) => {
                const triggerNgOnChangeForThisComponentInGivenRow = (rowObject: ({ [property: string]: ServoyBaseComponent<any> })) => {
                    const ui = rowObject[cm.name];
                    if (ui) {
                        const changes = {};
                        propertiesChangedButNotByRef.forEach((propertyChangedButNotByRef) => {
                            changes[propertyChangedButNotByRef.propertyName] = new SimpleChange(propertyChangedButNotByRef.newPropertyValue, propertyChangedButNotByRef.newPropertyValue, false);
                        });
                        ui.ngOnChanges(changes);
                        // no use to call detect changes here because it will be called in root parent form - because this is a result of a incomming server change for a child 'component' property
                    }
                };

                if (relativeRowIndex === -1 /*this means all rows*/) this.componentCache.forEach((rowObject) => triggerNgOnChangeForThisComponentInGivenRow(rowObject));
                else if (this.componentCache[this.foundset.viewPort.startIndex + relativeRowIndex] /* do we really need this check? we should not get a change event for a component at an inexistent position */) {
                    triggerNgOnChangeForThisComponentInGivenRow(this.componentCache[this.foundset.viewPort.startIndex + relativeRowIndex]);
                }
            };
        }

        const rowId = row[ViewportService.ROW_ID_COL_KEY];
        const handlers = {};
        const rowItem = new Cell(cm, handlers, rowId, this.foundset.viewPort.startIndex + rowIndex, rowIndex);

        if (cm.mappedHandlers) {
            cm.mappedHandlers.forEach((value, key) => {
                rowItem.handlers[key] = value.selectRecordHandler(rowId);
            });
        }

        const thisLFC = this;
        const idx = rowIndex;
        Object.defineProperty(rowItem.model, 'readOnly', {
            configurable: true,
            get() {
                let rowReadOnly = false;
                if (thisLFC.rowEditableDataprovider && thisLFC.rowEditableDataprovider.length > idx) {
                    rowReadOnly = !thisLFC.rowEditableDataprovider[idx];
                }
                return rowReadOnly || thisLFC.readOnly || !thisLFC.editable;
            },
        });
        if (this.rowEnableDataprovider && this.rowEnableDataprovider.length > idx) {
            Object.defineProperty(rowItem.model, 'enabled', {
                configurable: true,
                get() {
                    return thisLFC.rowEnableDataprovider[idx];
                },
            });
        }
        if (!row._cache) row._cache = new Map();
        row._cache.set(cm.name, rowItem);
        return rowItem;
    }

    callApi(componentName: string, apiName: string, args: any[], path?: string[]): any {
        if (path && componentName === 'containedForm' && path[0] === 'childElements') {
            const compModel = this.containedForm.childElements[parseInt(path[1], 10)];
            const selectedIndex = this.foundset.selectedRowIndexes[0];
            let row = this.componentCache[selectedIndex];
            if (!row) {
                this.log.warn('calling api ' + apiName + ' on' + componentName + ' in LFC:' + this.name + ' but selected record ' + selectedIndex +
                    '  is not in the view' + this.foundset.viewPort + ' fallback to the nearest visible row');
                // TODO is this a good idea? what if requestFocus() is called for example with the intention of showing and starting edit on selected row?
                let closestIndex = selectedIndex;
                let difference = Number.MAX_VALUE;
                Object.keys(this.componentCache).forEach(key => {
                    const intKey = parseInt(key, 10);
                    const dif = Math.abs(intKey - selectedIndex);
                    if (dif < difference) {
                        closestIndex = intKey;
                        difference = dif;
                    }
                });
                row = this.componentCache[closestIndex];
            }
            const uiComp = row[compModel.name];
            if (path.length > 2) {
                if (instanceOfApiExecutor(uiComp)) {
                    return uiComp.callApi(path[3], apiName, args, path.slice(3));
                } else {
                    this.log.error('trying to call api: ' + apiName + ' on component: ' + componentName + ' with path: ' + path +
                        ', but comp: ' + (uiComp == null ? ' is not found' : uiComp.name + ' doesnt implement IApiExecutor'));
                }
            } else {
                FormComponent.doCallApiOnComponent(uiComp, this.typesRegistry.getComponentSpecification(compModel.type),
                    apiName, args, this.converterService, this.log, componentName);
            }
        } else {
            this.log.error('got api call for ' + componentName + ' api ' + apiName + ' on LFC but path is wrong ' + path);
        }
    }

    getHandler(cell: Cell, name: string) {
        return cell.handlers[name];
    }

    getServoyApi(cell: Cell) {
        if (cell.api == null) {
            cell.api = new ListFormComponentServoyApi(cell, this.servoyApi.getFormName(), this.containedForm.absoluteLayout, this.formservice, this.servoyService, this, this.servoyApi.isInDesigner());
        }
        return cell.api;
    }

    getDesignNGClass(item: StructureCache): { [klass: string]: any } {
       if (this.parent instanceof DesignFormComponent){
          return this.parent.getNGClass(item);
       }
       return null;
    }
        
    datachange(component: Cell, property: string, value: any, dataprovider: boolean) {
        const model = component.model;
        const oldValue = model[property];
        model[property] = value;
        component.state.sendChanges(property, value, oldValue, component.rowId, dataprovider);
    }

    calculateCells() {
        // if it is still loading then don't calculate it right now again.
        if (this.waitingForLoad) return;
        if (this.servoyApi.isInDesigner()) {
            this.numberOfCells = 1;
            return;
        }
        this.numberOfCells = this.servoyApi.isInAbsoluteLayout() && this.containedForm && this.containedForm.absoluteLayout ? 0 : this.responsivePageSize;
        if (this.numberOfCells <= 0) {
            if (this.servoyApi.isInAbsoluteLayout() && this.containedForm && this.containedForm.absoluteLayout) {
                const parentWidth = this.elementRef.nativeElement.offsetWidth;
                const parentHeight = this.elementRef.nativeElement.offsetHeight;
                const height = this.containedForm.formHeight;
                const width = this.containedForm.formWidth;
                this.numberOfColumns = (this.pageLayout === 'listview') || parentWidth < width ? 1 : Math.floor(parentWidth / width);
                const numberOfRows = Math.floor(parentHeight / height);
                this.numberOfCells = numberOfRows * this.numberOfColumns;
                // always just render 1
                if (this.numberOfCells < 1) this.numberOfCells = 1;
            } else if (!this.useScrolling) {
                if (!this.servoyApi.isInAbsoluteLayout()) {
                    this.log.error('ListFormComponent ' + this.name + ' should have the responsivePageSize property set because it is used in a responsive form ' + this.servoyApi.getFormName());
                } else if (this.containedForm && !this.containedForm.absoluteLayout) {
                    this.log.error('ListFormComponent ' + this.name + ' should have the responsivePageSize property set because its containedForm is a responsive form');
                }
            }

        } else {
            this.numberOfColumns = this.pageLayout === 'listview' ? 1 : this.responsivePageSize;
        }

        if (!this.useScrolling) {
            const startIndex = this.page * this.numberOfCells;
            const foundset = this.foundset;
            if (foundset.viewPort.startIndex !== startIndex) {
                this.waitingForLoad = true;
                foundset.loadRecordsAsync(startIndex, this.numberOfCells).finally(() => this.waitingForLoad = false);
            } else {
                if (this.numberOfCells > foundset.viewPort.rows.length && foundset.viewPort.startIndex + foundset.viewPort.size < foundset.serverSize) {
                    this.waitingForLoad = true;
                    foundset.loadExtraRecordsAsync(Math.min(this.numberOfCells - foundset.viewPort.rows.length, foundset.serverSize - foundset.viewPort.startIndex - foundset.viewPort.size))
                        .finally(() => this.waitingForLoad = false);
                } else if (foundset.viewPort.size > this.numberOfCells) {
                    // the (initial) viewport  is bigger then the numberOfCells we have created rows for, adjust the viewport to be smaller.
                    this.waitingForLoad = true;
                    foundset.loadLessRecordsAsync(this.numberOfCells - foundset.viewPort.size).finally(() => this.waitingForLoad = false);
                }
            }
            this.updatePagingControls();
            foundset.setPreferredViewportSize(this.numberOfCells);
            this.cdRef.detectChanges();
        }
    }

    updatePagingControls() {
        this.renderer.setStyle(this.elementFirstRef.nativeElement, 'visibility', this.page > 0 ? 'visible' : 'hidden');
        this.renderer.setStyle(this.elementLeftRef.nativeElement, 'visibility', this.page > 0 ? 'visible' : 'hidden');
        const hasMorePages = this.foundset.hasMoreRows || (this.foundset.serverSize - (this.page * this.numberOfCells + Math.min(this.numberOfCells, this.foundset.viewPort.rows.length))) > 0;
        this.renderer.setStyle(this.elementRightRef.nativeElement, 'visibility', hasMorePages ? 'visible' : 'hidden');
    }

    getRowClasses(index: number) {
        let rowClasses = 'svy-listformcomponent-row';
        if (this.selectionClass) {
            if (this.foundset.selectedRowIndexes.indexOf(this.foundset.viewPort.startIndex + index) !== -1) {
                rowClasses += ' ' + this.selectionClass;
            }
        }
        if (this.rowStyleClass) {
            rowClasses += ' ' + this.rowStyleClass;
        }
        if (this.rowStyleClassDataprovider && this.rowStyleClassDataprovider[index]) {
            rowClasses += ' ' + this.rowStyleClassDataprovider[index];
        }
        return rowClasses;
    }

    updateSelection() {
        const selectedRowIndex = this.foundset.selectedRowIndexes[0];
        const element = this.elementRef.nativeElement.children[(this.page > 0) ? selectedRowIndex - this.numberOfCells * this.page : selectedRowIndex];
        if (element && !element.contains(this.doc.activeElement) && this.selectionChangedByKey && !element.className.includes('svyPagination')) {
            element.focus();
            this.selectionChangedByKey = false;
        }
    }

    registerComponent(component: ServoyBaseComponent<any>, rowIndex: number): void {
        let rowComponents = this.componentCache[rowIndex];
        if (!rowComponents) {
            rowComponents = {};
            this.componentCache[rowIndex] = rowComponents;
        }
        rowComponents[component.name] = component;
    }

    unRegisterComponent(component: ServoyBaseComponent<any>, rowIndex: number): void {
        const rowComponents = this.componentCache[rowIndex];
        if (rowComponents) {
            delete rowComponents[component.name];
            if (isEmpty(rowComponents)) {
                delete this.componentCache[rowIndex];
            }
        }
    }

    // used for scrolling with AGGGrid

    getNumberOfColumns(): number {
        return this.numberOfColumns;
    }

    getAGGridStyle(): any {
        const aggridStyle = {
            '--ag-row-height': 42,
            '--ag-header-height': 48,
            '--ag-list-item-height': 24
        };
        if (this.servoyApi.isInAbsoluteLayout() || this.responsiveHeight < 1) {
            aggridStyle['height'] = '100%';
        } else {
            aggridStyle['height.px'] = this.responsiveHeight;
        }
        return aggridStyle;
    }
}

class Cell {

    api: ServoyApi;
    name: string;
    /** this is the true cell viewport which is already composed inside IChildComponentPropertyValue of shared (non foundset dependent) part and row specific (foundset dependent props) part */
    readonly model: any;

    constructor(public readonly state: IChildComponentPropertyValue,
        public readonly handlers: any, public readonly rowId: any, public readonly rowIndex: number, relativeRowIndex: number) {
        this.name = state.name;
        this.model = state.modelViewport ? state.modelViewport[relativeRowIndex] : state.model; // modelViewport can be undefined in form editor/designer - that sends stuff as if it is a simple form component, not a list form component
        if (!this.model){
          this.model = {};  
        } 
    }

}

class ListFormComponentServoyApi extends ServoyApi {

    private markupId: string;
    constructor(private cell: Cell,
        formname: string,
        absolute: boolean,
        formservice: FormService,
        servoyService: ServoyService,
        private fc: ListFormComponent,
        isdesigner: boolean) {
        super(cell.state, formname, absolute, formservice, servoyService, isdesigner);
        this.markupId = super.getMarkupId() + '_' + this.cell.rowIndex;
    }

    registerComponent(comp: ServoyBaseComponent<any>) {
        this.fc.registerComponent(comp, this.cell.rowIndex);
    }

    unRegisterComponent(comp: ServoyBaseComponent<any>) {
        this.fc.unRegisterComponent(comp, this.cell.rowIndex);
    }

    getMarkupId(): string {
        return this.markupId;
    }

    startEdit(propertyName: string) {
        this.cell.state.startEdit(propertyName, this.cell.rowId); // this will automatically change selection to this row server-side as well if needed
        this.fireOnSelectionChangeIfNeeded();
    }

    apply(propertyName: string, value: any) {
        this.cell.state.sendChanges(propertyName, value, this.cell.model[propertyName],
            this.cell.rowId, true); // this will automatically change selection to this row server-side as well if needed
        this.fireOnSelectionChangeIfNeeded();
    }

    private fireOnSelectionChangeIfNeeded(): void {
        if (this.fc.onSelectionChanged) {
            const selected = this.fc.foundset.selectedRowIndexes;
            if (!selected || selected.indexOf(this.cell.rowIndex) === -1) {
                this.fc.onSelectionChanged(new CustomEvent('SelectionChanged'));
            }
        }
    }

}

class AGGridDatasource implements IServerSideDatasource {

    lfc: ListFormComponent;
    constructor(lfc: ListFormComponent) {
        this.lfc = lfc;
    }

    getRows(params: IServerSideGetRowsParams): void {
        // load record if endRow is not in viewPort
        const startIndex = Math.ceil(this.lfc.foundset.viewPort.startIndex / this.lfc.getNumberOfColumns()); // start index of view port (0-based)
        const endIndex = startIndex + Math.ceil(this.lfc.foundset.viewPort.size / this.lfc.getNumberOfColumns()); // end index of the view port (0-based)

        // index in the cached viewPort (0-based);
        let viewPortStartIndex = params.request.startRow - startIndex;
        let viewPortEndIndex = params.request.endRow - startIndex;

        if (params.request.startRow < startIndex || (params.request.endRow > endIndex && this.getLastRowIndex() === -1)) {
            let requestViewPortStartIndex: any;
            // keep the previous chunk in cache
            if (params.request.startRow >= AGGRID_CACHE_BLOCK_SIZE && params.request.endRow >= endIndex) {
                requestViewPortStartIndex = params.request.startRow - AGGRID_CACHE_BLOCK_SIZE;
            } else {
                requestViewPortStartIndex = params.request.startRow;
            }

            const size = (params.request.endRow - requestViewPortStartIndex) * this.lfc.getNumberOfColumns();
            const promise = this.loadExtraRecordsAsync(requestViewPortStartIndex * this.lfc.getNumberOfColumns(), size);
            promise.requestInfo = 'AGGridDatasourceGetRows';
            promise.then(() => {
                // load complete
                // get the index of the last row
                const lastRowIndex = this.getLastRowIndex();

                // update viewPortStatIndex
                viewPortStartIndex = params.request.startRow - Math.ceil(this.lfc.foundset.viewPort.startIndex / this.lfc.getNumberOfColumns());
                viewPortEndIndex = params.request.endRow - Math.ceil(this.lfc.foundset.viewPort.startIndex / this.lfc.getNumberOfColumns());

                params.success({
                    rowData: this.getViewPortData(viewPortStartIndex, viewPortEndIndex),
                    rowCount: lastRowIndex
                });
            }).catch((e) => {
                console.log(e);
            });
        } else {
            params.success({
                rowData: this.getViewPortData(viewPortStartIndex, viewPortEndIndex),
                rowCount: this.getLastRowIndex()
            });
        }
    }

    hasMoreRecordsToLoad() {
        return this.lfc.foundset.hasMoreRows || (this.lfc.foundset.viewPort.startIndex + this.lfc.foundset.viewPort.size) < this.lfc.foundset.serverSize;
    }

    getLastRowIndex() {
        if (this.hasMoreRecordsToLoad()) {
            return -1;
        } else {
            return Math.ceil(this.lfc.foundset.serverSize / this.lfc.getNumberOfColumns());
        }
    }

    loadExtraRecordsAsync(startIndex: number, size: number) {
        size = (size * AGGRID_MAX_BLOCKS_IN_CACHE * this.lfc.getNumberOfColumns()) + size;
        if (this.hasMoreRecordsToLoad() === false) {
            size = Math.min(size, this.lfc.foundset.serverSize - startIndex);
        }
        if (size < 0) {
            size = 0;
        }

        return this.lfc.foundset.loadExtraRecordsAsync(size);
    }

    getViewPortData(startIndex: number, endIndex: number) {
        const result = [];
        let fsStartIndex = startIndex ? startIndex * this.lfc.getNumberOfColumns() : 0;
        let fsEndIndex = endIndex * this.lfc.getNumberOfColumns();
        if (fsEndIndex > this.lfc.foundset.viewPort.rows.length) fsEndIndex = this.lfc.foundset.viewPort.rows.length;

        // index cannot exceed ServerSize
        fsStartIndex = Math.min(fsStartIndex, this.lfc.foundset.serverSize);
        fsEndIndex = Math.min(fsEndIndex, this.lfc.foundset.serverSize);

        let line = [];
        for (let j = fsStartIndex; j < fsEndIndex; j++) {
            line.push(this.lfc.foundset.viewPort.rows[j]);
            if (line.length === this.lfc.getNumberOfColumns()) {
                result.push(line);
                line = [];
            }
        }
        if (line.length > 0) {
            result.push(line);
        }

        return result;
    }
}
