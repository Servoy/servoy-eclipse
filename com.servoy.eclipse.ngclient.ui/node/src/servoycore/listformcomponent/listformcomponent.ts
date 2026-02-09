/* eslint-disable max-len */
import {
  Component, TemplateRef, ElementRef, AfterViewInit, Renderer2,
  HostListener, ChangeDetectorRef, OnDestroy, Inject, SimpleChange, ChangeDetectionStrategy, SimpleChanges, Injector,
  DOCUMENT,
  input,
  viewChild, signal
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

import { ServoyApi } from '../../ngclient/servoy_api';
import { GridOptions, IServerSideDatasource, IServerSideGetRowsParams } from 'ag-grid-community';
import { RowRenderer } from './row-renderer.component';
import { SabloTabseq } from '@servoy/public';
import { AgGridAngular } from 'ag-grid-angular';
import { TypesRegistry } from '../../sablo/types_registry';
import { ConverterService } from '../../sablo/converter.service';
import { animate } from '@angular/animations';

const AGGRID_CACHE_BLOCK_SIZE = 50;
const AGGRID_MAX_BLOCKS_IN_CACHE = 2;

@Component({
    selector: 'servoycore-listformcomponent',
    styleUrls: ['./listformcomponent.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
    <div class="svy-listformcomponent" [ngClass]='styleClass()' #element>
      @if (useScrolling) {
        <ag-grid-angular #aggrid
          [gridOptions]="agGridOptions"
          [ngStyle]="getAGGridStyle()"
          [sabloTabseq]="tabSeq()"
          [sabloTabseqConfig]="{container: true, reservedGap: 1000}">
        </ag-grid-angular>
      }
    
      @if (!useScrolling) {
        @if (containedForm()&&containedForm().absoluteLayout) {
          <div>
            @for (row of getViewportRows(); track row; let i = $index) {
              <div tabindex="-1" (click)="onRowClick(row, $event)" [class]="getRowClasses(i)" [ngStyle]="{'height.px': getRowHeight(), 'width' : getRowWidth()}" style="display:inline-block; position: relative">
                @for (item of cache.items; track item) {
                  <div [svyContainerStyle]="item" [svyContainerLayout]="item.layout" class="svy-wrapper" style="position:absolute"> <!-- wrapper div -->
                    <ng-template [ngTemplateOutlet]="getRowItemTemplate(item)" [ngTemplateOutletContext]="{ state:getRowItemState(item, row, i), callback:this, row:row, i:i }"></ng-template>  <!-- component  -->
                  </div>
                }
              </div>
            }
          </div>
        }
        @if (containedForm()&&!containedForm().absoluteLayout) {
          <div>
            @for (row of getViewportRows(); track trackByFn(i, row); let i = $index) {
              <div tabindex="-1" (click)="onRowClick(row, $event)" [class]="getRowClasses(i)" [ngStyle]="{'width' : getRowWidth()}" style="display:inline-block">
                @for (item of cache.items; track item) {
                  <ng-template [ngTemplateOutlet]="getRowItemTemplate(item)" [ngTemplateOutletContext]="{ state: getRowItemState(item, row, i), callback:this, row:row, i:i}"></ng-template>
                  }  <!-- component or responsive div  -->
                </div>
              }
            </div>
          }
          <div class='svyPagination'><div #firstelement style='text-align:center;cursor:pointer;display:inline;visibility:hidden;padding:3px;white-space:nowrap;vertical-align:middle;background-color:rgb(255, 255, 255, 0.6);' (click)='firstPage()' ><i class='fa fa-backward' aria-hidden="true"></i></div><div  #leftelement style='text-align:center;cursor:pointer;visibility:hidden;display:inline;padding:3px;white-space:nowrap;vertical-align:middle;background-color:rgb(255, 255, 255, 0.6);' (click)='moveLeft()' ><i class='fa fa-chevron-left' aria-hidden="true"></i></div><div #rightelement style='text-align:center;cursor:pointer;visibility:hidden;display:inline;padding:3px;white-space:nowrap;vertical-align:middle;background-color:rgb(255, 255, 255, 0.6);' (click)='moveRight()'><i class='fa fa-chevron-right' aria-hidden="true"></i></div></div>
        }
      </div>
    
      <ng-template  #svyResponsiveDiv  let-state="state" let-row="row" let-i="i">
        <div [svyContainerStyle]="state" [svyContainerClasses]="state.classes" [ngClass]="getDesignNGClass(state)" [svyContainerAttributes]="state.attributes" class="svy-layoutcontainer">
          @for (item of state.items; track item) {
            <ng-template [ngTemplateOutlet]="getRowItemTemplate(item)" [ngTemplateOutletContext]="{ state:getRowItemState(item, row, i), callback:this, row:row, i:i}"></ng-template>
          }
        </div>
      </ng-template>
      <ng-template  #formComponentAbsoluteDiv  let-state="state" let-row="row" let-i="i">
        <div [svyContainerStyle]="state.formComponentProperties" [svyContainerLayout]="state.formComponentProperties.layout" [svyContainerClasses]="state.formComponentProperties.classes" [svyContainerAttributes]="state.formComponentProperties.attributes" style="position:relative" class="svy-formcomponent">
          @for (item of state.items; track item) {
            <div [svyContainerStyle]="item" [svyContainerLayout]="item.layout" class="svy-wrapper" [ngStyle]="item.model.visible === false && {'display': 'none'}" style="position:absolute"> <!-- wrapper div -->
              <ng-template [ngTemplateOutlet]="getRowItemTemplate(item)" [ngTemplateOutletContext]="{ state:getRowItemState(item, row, i), callback:this, row:row, i:i}"></ng-template>  <!-- component  -->
            </div>
          }
        </div>
      </ng-template>
      <ng-template  #formComponentResponsiveDiv  let-state="state" let-row="row" let-i="i">
        <div [svyContainerStyle]="state.formComponentProperties" [svyContainerLayout]="state.formComponentProperties.layout" [svyContainerClasses]="state.formComponentProperties.classes" [svyContainerAttributes]="state.formComponentProperties.attributes" class="svy-formcomponent">
          @for (item of state.items; track item) {
            <ng-template [ngTemplateOutlet]="getRowItemTemplate(item)" [ngTemplateOutletContext]="{ state:getRowItemState(item, row, i), callback:this, row:row, i:i}"></ng-template>
            }  <!-- component  -->
          </div>
        </ng-template>
        <!-- structure template generate start -->
        <!-- structure template generate end -->
    
    `,
    standalone: false
})
export class ListFormComponent extends ServoyBaseComponent<HTMLDivElement> implements AfterViewInit, OnDestroy, IApiExecutor {

    readonly containedForm = input<FormComponentValue>(undefined);
    readonly containedFormMargin = input<any>(undefined);
    readonly foundset = input<IFoundset>(undefined);
    readonly selectionClass = input<string>(undefined);
    readonly styleClass = input<string>(undefined);
    readonly rowStyleClass = input<string>(undefined);
    readonly rowStyleClassDataprovider = input<string[]>(undefined);
    readonly rowEditableDataprovider = input<boolean[]>(undefined);
    readonly rowEnableDataprovider = input<boolean[]>(undefined);
    readonly responsivePageSize = input<number>(undefined);
    readonly responsiveHeight = input<number>(undefined);
    readonly pageLayout = input<string>(undefined);
    readonly readOnly = input<boolean>(undefined);
    readonly editable = input<boolean>(undefined);
    readonly tabSeq = input<number>(undefined);

    readonly onSelectionChanged = input<(event: any) => void>(undefined);
    readonly onListItemClick = input<(record: any, event: any) => void>(undefined);

    readonly svyResponsiveDiv = viewChild<TemplateRef<any>>('svyResponsiveDiv');
    readonly formComponentAbsoluteDiv = viewChild<TemplateRef<any>>('formComponentAbsoluteDiv');
    readonly formComponentResponsiveDiv = viewChild<TemplateRef<any>>('formComponentResponsiveDiv');
    // structure viewchild template generate start
    // structure viewchild template generate end
    readonly element = viewChild<ElementRef>('element');

    // used for paging
    readonly elementFirstRef = viewChild<ElementRef>('firstelement');
    readonly elementLeftRef = viewChild<ElementRef>('leftelement');
    readonly elementRightRef = viewChild<ElementRef>('rightelement');

    // used for scrolling with AGGGrid
    readonly agGrid = viewChild<AgGridAngular>('aggrid');

    // Reference to the sabloTabseq directive
    readonly sabloTabseqDirective = viewChild('aggrid', { read: SabloTabseq });
    
    _foundset = signal<IFoundset>(undefined);
    override elementRef!: ElementRef<HTMLDivElement>;

    // TODO: remove this when switching completely to scrollable LFC
    useScrolling = false;

    // used for scrolling with AGGGrid
    parent: AbstractFormComponent;
    agGridOptions: GridOptions;
    numberOfColumns = 1;
    resizeObserver: ResizeObserver;
    resizeTimeout: any;
    previousWidth = 0;

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
        const foundset = this._foundset();
        if (!foundset.multiSelect && event.key === 'ArrowUp' || event.key === 'ArrowDown') {
            let selectedRowIndex = foundset.selectedRowIndexes[0];
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
            if (selectedRowIndex >= 0 && selectedRowIndex < foundset.serverSize) {
                foundset.requestSelectionUpdate([selectedRowIndex]);
                this.selectionChangedByKey = true;
                const onSelectionChanged = this.onSelectionChanged();
                if (onSelectionChanged) {
                    onSelectionChanged(event);
                }
            }
        }
    }

    trackByFn(index: number, row: ViewPortRow) {
        return row._svyRowId;
    }

    onRowClick(row: any, event: Event) {
        for (let i = 0; i < this._foundset().viewPort.rows.length; i++) {
            const foundset = this._foundset();
            if (foundset.viewPort.rows[i][ViewportService.ROW_ID_COL_KEY] === row['_svyRowId']) {
                const index = i + foundset.viewPort.startIndex;
                const selected = foundset.selectedRowIndexes;
                if (!selected || selected.indexOf(index) === -1) {
                    foundset.requestSelectionUpdate([index]);
                    const onSelectionChanged = this.onSelectionChanged();
                    if (onSelectionChanged) {
                        onSelectionChanged(event);
                    }
                }
                const onListItemClick = this.onListItemClick();
                if(onListItemClick) {
                    onListItemClick(foundset.getRecordRefByRowID(row['_svyRowId']), event);
                }
                break;
            }
        }
    }

    ngOnChanges(changes: SimpleChanges) {
        super.ngOnChanges(changes);
        this._foundset.set(this.foundset());
        const containedForm = this.containedForm();
        if (containedForm && containedForm.childElements) {
            this.rowItems = [];
            containedForm.childElements.forEach(elem => {
                if (elem.specName === 'servoycore-formcomponent') // TODO this used elem.type before which was always camel-case; does this mean that this if should be removed instead of making it work?
                    this.rowItems.push(this.parent.getFormCache().getFormComponent(elem.name));
                else this.rowItems.push(elem);
            });
        }
    }

    svyOnInit() {
        super.svyOnInit();

        this._foundset.set(this.foundset());

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
                animateRows: false,
                suppressContextMenu: true,
                context: {
                    componentParent: this
                },
                components: {
                    'row-renderer': RowRenderer
                },
                headerHeight: 0,
                defaultColDef: {
                    flex: 1,
                    suppressKeyboardEvent: (params: any) => {
                        if(params.event.keyCode === 9) {
                            if(params.event.altKey) {
                                const nextIndex = this.sabloTabseqDirective().runtimeIndex.nextAvailableIndex;
                                const nextElement = this.doc.querySelector('[tabindex="' + nextIndex + '"]');
                                if (nextElement) {
                                    (nextElement as HTMLElement).focus();
                                }
                            }
                            return true;
                        }
                        return false;
                    }
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

                    let selectedIdx = this._foundset().selectedRowIndexes[0];

                    switch (params.key) {
                        case KEY_DOWN:
                            selectedIdx = selectedIdx + 1;
                            if (selectedIdx >= this._foundset().serverSize) selectedIdx = this._foundset().serverSize - 1;
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
                },
                domLayout: this.responsiveHeight() < 0 ? 'autoHeight' : 'normal'
            };
        }

        if (!this.useScrolling) {
            this.removeListenerFunction = this._foundset().addChangeListener((event: FoundsetChangeEvent) => {
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
                        this._foundset.set(event.fullValueChanged.newValue);
                        if (this._foundset().serverSize > 0 && this.numberOfCells > 0 && this.page * this.numberOfCells >= this._foundset().serverSize)
                        {
                            this.page = Math.floor((this._foundset().serverSize - 1) / this.numberOfCells);
                        }
                        this.calculateCells();
                        return;
                    }

                    let viewportSizeAfterShiftingIsDone = this._foundset().viewPort.size;
                    if (event.viewPortStartIndexChanged) {
                        // an insert/delete before current page made viewport start index no longer match page start index; adjust
                        const shiftedPageDelta = this.page * this.numberOfCells - this._foundset().viewPort.startIndex; // can be negative (insert) or positive(delete)
                        if (shiftedPageDelta !== 0) {
                            const wantedVPSize = this._foundset().viewPort.size;
                            const wantedVPStartIndex = this.page * this.numberOfCells;
                            const serverSize = this._foundset().serverSize;

                            // so shifting means loading "shiftedPageDelta" more/less in one end of the viewport and "shiftedPageDelta" less/more at the other end

                            // when load extra would request more records after, there might not be enough records in the foundset (deleted before)
                            let loadExtraCorrected = shiftedPageDelta;
                            if (loadExtraCorrected > 0 /*so shift right*/ && wantedVPStartIndex + wantedVPSize > serverSize)
                                loadExtraCorrected -= (wantedVPStartIndex + wantedVPSize - serverSize);
                            if (loadExtraCorrected !== 0) {
                                this._foundset().loadExtraRecordsAsync(loadExtraCorrected, true);
                                viewportSizeAfterShiftingIsDone += Math.abs(loadExtraCorrected);
                            }

                            // load less if it happens at the end - might need to let more records slide-in the viewport if available (insert before)
                            let loadLessCorrected = shiftedPageDelta;
                            if (loadLessCorrected < 0 /*so shift left*/ && wantedVPSize < this.numberOfCells && wantedVPStartIndex + wantedVPSize < serverSize) //
                                loadLessCorrected += Math.min(serverSize - wantedVPStartIndex - wantedVPSize, this.numberOfCells - wantedVPSize);
                            if (loadLessCorrected !== 0) {
                                this._foundset().loadLessRecordsAsync(loadLessCorrected, true);
                                viewportSizeAfterShiftingIsDone -= Math.abs(loadLessCorrected);
                            }
                        }
                        this.updateSelection();
                    }

                    const foundset = this._foundset();
                    if (event.viewPortSizeChanged && this._foundset().serverSize > 0 && (this.page * this.numberOfCells >= this._foundset().serverSize)
                        && foundset.viewPort.size === 0 && this.numberOfCells > 0) {
                        this.page = Math.floor((foundset.serverSize - 1) / this.numberOfCells);
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
                            const availableExtraRecords = foundset.serverSize - (vpStartIndexForCurrentCalcs + vpSizeForCurrentCalcs);
                            if (availableExtraRecords > 0) foundset.loadExtraRecordsAsync(Math.min(deltaSize, availableExtraRecords), true);
                        } else if (deltaSize < 0) {
                            // we need to show less records; deltaSize is already negative; so it will load less from end of viewport
                            foundset.loadLessRecordsAsync(deltaSize, true);
                        } // else it's already ok
                    }

                    foundset.notifyChanged(); // let foundset send it's pending requests to server if any
                    this.cdRef.detectChanges();
                }
            });
        } else {
            this.removeListenerFunction = this._foundset().addChangeListener((event: FoundsetChangeEvent) => {
                const agGrid = this.agGrid();
                if ((event.requestInfos && event.requestInfos.includes('AGGridDatasourceGetRows')) || agGrid.api.isDestroyed()) {
                    return;
                }
                if (event.serverFoundsetSizeChanged ) {
                    agGrid.api.setRowCount(Math.ceil(event.serverFoundsetSizeChanged.newValue / this.getNumberOfColumns()));
                }
                if (event.viewportRowsCompletelyChanged || event.fullValueChanged) {
                    agGrid.api.refreshServerSide({ purge: true });
                } else if (event.viewportRowsUpdated) {
                    // copy the viewport data over to the cell
                    const changes = event.viewportRowsUpdated;
                    let insertOrDeletes = false;
                    changes.forEach(change => {
                        insertOrDeletes = insertOrDeletes || change.type === ChangeType.ROWS_INSERTED || change.type === ChangeType.ROWS_DELETED;
                    });
                    if (insertOrDeletes) {
                        agGrid.api.refreshServerSide({ purge: true });
                        const foundset = this._foundset();
                        agGrid.api.setRowCount(foundset.serverSize ? Math.ceil(foundset.serverSize / this.getNumberOfColumns()) : 0);
                    }
                    else if (changes.length == 1 && changes[0].startIndex === changes[0].endIndex){
						agGrid.api.refreshCells();	
					}
					else {
						agGrid.api.refreshServerSide({ purge: true });	
					}
                }
                if(event.selectedRowIndexesChanged) {
                    this.scrollToSelection();
                }
            });
        }
    }

    private scrollToSelection() {
        const foundset = this._foundset();
        const agGrid = this.agGrid();
        if(foundset.selectedRowIndexes.length && !agGrid.api.isDestroyed()) {
            const rowCount = agGrid.api.getDisplayedRowCount();
            if(rowCount > 1) {
                const selectedIdx = Math.ceil(foundset.selectedRowIndexes[0] / this.getNumberOfColumns());
                if(selectedIdx > rowCount - AGGRID_CACHE_BLOCK_SIZE) {
                    agGrid.api.setRowCount(Math.min(selectedIdx + AGGRID_CACHE_BLOCK_SIZE, Math.ceil(foundset.serverSize / this.getNumberOfColumns())));
                }
                agGrid.api.ensureIndexVisible(selectedIdx < rowCount ? selectedIdx : rowCount - 1);
            }
        }        
    }

    private calculateNumberOfColumns(): number
    {
        const parentWidth = this.element().nativeElement.offsetWidth;
        let width = this.containedForm().formWidth;
        const containedFormMargin = this.containedFormMargin();
        if(containedFormMargin) {
            let left = parseInt(containedFormMargin.paddingLeft, 10);
            let right = parseInt(containedFormMargin.paddingRight, 10);
            width += (left + right);
        }
        return (this.pageLayout() === 'listview') || parentWidth < width ? 1 : Math.floor(parentWidth / width);
    }

    ngAfterViewInit() {
        this.elementRef = this.element();
        this.calculateCells();
        if (this.useScrolling) {
            this.agGrid().api.setGridOption('serverSideDatasource', new AGGridDatasource(this));
            if(!this.servoyApi.isInAbsoluteLayout()) {
                this.resizeObserver = new ResizeObserver((entries) => {
                    const newWidth = entries[0].contentRect.width;
                    if(newWidth !== this.previousWidth) {
                        if(this.resizeTimeout) {
                            clearTimeout(this.resizeTimeout);
                        }
                        this.resizeTimeout = setTimeout(() => {
                            const agGrid = this.agGrid();
                            if(!agGrid.api.isDestroyed()) {
                                this.numberOfColumns = this.calculateNumberOfColumns();
                                this.resizeTimeout = null;
                                agGrid.api.refreshServerSide({ purge: true });
                                const foundset = this._foundset();
                                agGrid.api.setRowCount(foundset.serverSize ? Math.ceil(foundset.serverSize / this.getNumberOfColumns()) : 0);
                                setTimeout(() => {
                                    this.scrollToSelection();
                                }, 200);
                            }
                        }, 200);
                        this.previousWidth = newWidth;
                    }
                });
                this.resizeObserver.observe(this.element().nativeElement);
            }
        }
    }

    ngOnDestroy() {
        if (this.resizeObserver) this.resizeObserver.unobserve(this.element().nativeElement);
        if (this.removeListenerFunction != null) {
            this.removeListenerFunction();
            this.removeListenerFunction = null;
        }
        const containedForm = this.containedForm();
        if (containedForm && containedForm.childElements) {
            containedForm.childElements.forEach(component => component.triggerNgOnChangeWithSameRefDueToSmartPropertyUpdate = null);
        }
        this.getViewportRows().forEach(elem => elem._cache = null);
    }

    getViewportRows(): ViewPortRow[] {
        if (this.servoyApi.isInDesigner()) {
            return this.designerViewportRows;
        }
        if (this.numberOfCells === 0) return [];
        return this._foundset().viewPort.rows;
    }

    getStyleClasses(): string[] {
        const classes: Array<string> = new Array();
        const styleClass = this.styleClass();
        if (styleClass) {
            classes.push(styleClass);
        }
        return classes;
    }

    getRowStyle(includeHeight: boolean): any {
        const rowStyle = {
            'width': this.getRowWidth()
        };
        const containedFormMargin = this.containedFormMargin();
        if(containedFormMargin) {
            rowStyle['margin-left'] = containedFormMargin.paddingLeft;
            rowStyle['margin-right'] = containedFormMargin.paddingRight;
            rowStyle['margin-top'] = containedFormMargin.paddingTop;
            rowStyle['margin-bottom'] = containedFormMargin.paddingBottom;
        }
        if(includeHeight) rowStyle['height.px'] = this.getRowHeight();
        return rowStyle;
    }

    getRowHeight(): number {
        const containedForm = this.containedForm();
        return containedForm.formHeight ? containedForm.formHeight : null;
    }

    getRowWidth(): string {
        if (this.pageLayout() === 'listview') {
            return '100%';
        }
        return this.containedForm().formWidth + 'px';
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
            return item.tagname ? this[item.tagname] : this.svyResponsiveDiv();
        }
        if (item instanceof FormComponentCache) {
            return (item as FormComponentCache).responsive ? this.formComponentResponsiveDiv() : this.formComponentAbsoluteDiv();
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
                const triggerNgOnChangeForThisComponentInGivenRow = (rowObject: ({ [property: string]: ServoyBaseComponent<any> }), componentModel: any) => {
                    const ui = rowObject[cm.name];
                    if (ui) {
                        const changes = {};
                        propertiesChangedButNotByRef.forEach((propertyChangedButNotByRef) => {
                            const newValue = componentModel && (componentModel[propertyChangedButNotByRef.propertyName] !== undefined) ? componentModel[propertyChangedButNotByRef.propertyName] : propertyChangedButNotByRef.newPropertyValue;
                            changes[propertyChangedButNotByRef.propertyName] = new SimpleChange(newValue, newValue, false);
                        });
                        ui.ngOnChanges(changes);
                        // no use to call detect changes here because it will be called in root parent form - because this is a result of a incomming server change for a child 'component' property
                    }
                };

                let relativeStartIndex = relativeRowIndex, relativeStopIndex = relativeRowIndex + 1;
                if (relativeRowIndex === -1 /*this means all rows*/) {
                    relativeStartIndex = this._foundset().viewPort.startIndex;
                    relativeStopIndex = this._foundset().viewPort.startIndex + this._foundset().viewPort.rows.length;
                }
                for(let relativeIndex = relativeStartIndex; relativeIndex < relativeStopIndex; relativeIndex++) {
                    if (this.componentCache[this._foundset().viewPort.startIndex + relativeIndex]) {
                        triggerNgOnChangeForThisComponentInGivenRow(this.componentCache[this._foundset().viewPort.startIndex + relativeIndex], cm.modelViewport ? cm.modelViewport[relativeIndex] : cm.model);
                    }
                }
            };
        }

        const rowId = row[ViewportService.ROW_ID_COL_KEY];
        const handlers = {};
        const rowItem = new Cell(cm, handlers, rowId, this._foundset().viewPort.startIndex + rowIndex, rowIndex);

        if (cm.mappedHandlers) {
            cm.mappedHandlers.forEach((value, key) => {
                rowItem.handlers[key] = value.selectRecordHandler(rowId);
            });
        }

        const thisLFC = this;
        const idx = rowIndex;
		let elementReadOnly = rowItem.model.readOnly;
        Object.defineProperty(rowItem.model, 'readOnly', {
            configurable: true,
            get() {
                let rowReadOnly = false;
                const rowEditableDataprovider = thisLFC.rowEditableDataprovider();
                if (rowEditableDataprovider && rowEditableDataprovider.length > idx) {
                    rowReadOnly = !rowEditableDataprovider[idx];
                }
                return elementReadOnly || rowReadOnly || thisLFC.readOnly() || !thisLFC.editable();
            },
			set(value: boolean) {
				elementReadOnly = value;
			}
        });
        // TODO: 'enabledDataProvider' and 'visibleDataProvider' should not be in the model - on the server side
        // they should be evaluated for each row and added to the model as regular 'enabled' and 'visible' properties
        let elementEnabled = rowItem.model.enabled;
        Object.defineProperty(rowItem.model, 'enabled', {
            configurable: true,
            get() {
                const rowEnableDataprovider = thisLFC.rowEnableDataprovider();
                if (rowEnableDataprovider && rowEnableDataprovider.length > idx) {
                    return thisLFC.rowEditableDataprovider()[idx]
                }
                if(this.enabledDataProvider !== undefined) {
                    return this.enabledDataProvider;
                }
                return elementEnabled;
            },
            set(value: boolean) {
				elementEnabled = value;
			}
        });
        let elementVisible = rowItem.model.visible;
        Object.defineProperty(rowItem.model, 'visible', {
            configurable: true,
            get() {
                if(this.visibleDataProvider !== undefined) {
                    return this.visibleDataProvider;
                }
                return elementVisible;
            },
            set(value: boolean) {
				elementVisible = value;
			}
        });

        if (!row._cache) row._cache = new Map();
        row._cache.set(cm.name, rowItem);
        return rowItem;
    }

    callApi(componentName: string, apiName: string, args: any[], path?: string[]): any {
        if (path && componentName === 'containedForm' && path[0] === 'childElements') {
            const compModel = this.containedForm().childElements[parseInt(path[1], 10)];
            const selectedIndex = this._foundset().selectedRowIndexes[0];
            let row = this.componentCache[selectedIndex];
            if (!row) {
                this.log.warn('calling api ' + apiName + ' on' + componentName + ' in LFC:' + this.name + ' but selected record ' + selectedIndex +
                    '  is not in the view' + this._foundset().viewPort + ' fallback to the nearest visible row');
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
            cell.api = new ListFormComponentServoyApi(cell, this.servoyApi.getFormName(), this.containedForm().absoluteLayout, this.formservice, this.servoyService, this, this.servoyApi.isInDesigner());
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

        if (!this.useScrolling) {
            const containedForm = this.containedForm();
            this.numberOfCells = this.servoyApi.isInAbsoluteLayout() && containedForm && containedForm.absoluteLayout ? 0 : this.responsivePageSize();
            if (this.numberOfCells <= 0) {
                const containedFormValue = this.containedForm();
                if (this.servoyApi.isInAbsoluteLayout() && containedFormValue && containedFormValue.absoluteLayout) {
                    const parentWidth = this.element().nativeElement.offsetWidth;
                    const parentHeight = this.element().nativeElement.offsetHeight;
                    const height = containedFormValue.formHeight;
                    const width = containedFormValue.formWidth;
                    const nrColumns = (this.pageLayout() === 'listview') || parentWidth < width ? 1 : Math.floor(parentWidth / width);
                    const numberOfRows = Math.floor(parentHeight / height);
                    this.numberOfCells = numberOfRows * nrColumns;
                    // always just render 1
                    if (this.numberOfCells < 1) this.numberOfCells = 1;
                } else {
                    if (!this.servoyApi.isInAbsoluteLayout()) {
                        this.log.error('ListFormComponent ' + this.name + ' should have the responsivePageSize property set because it is used in a responsive form ' + this.servoyApi.getFormName());
                    } else if (containedFormValue && !containedFormValue.absoluteLayout) {
                        this.log.error('ListFormComponent ' + this.name + ' should have the responsivePageSize property set because its containedForm is a responsive form');
                    }
                }
    
            }

            const startIndex = this.page * this.numberOfCells;
            const foundset = this._foundset();
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
        } else {
            this.numberOfColumns = this.calculateNumberOfColumns();
        }
    }

    updatePagingControls() {
        this.renderer.setStyle(this.elementFirstRef().nativeElement, 'visibility', this.page > 0 ? 'visible' : 'hidden');
        this.renderer.setStyle(this.elementLeftRef().nativeElement, 'visibility', this.page > 0 ? 'visible' : 'hidden');
        const foundset = this._foundset();
        const hasMorePages = foundset.hasMoreRows || (foundset.serverSize - (this.page * this.numberOfCells + Math.min(this.numberOfCells, foundset.viewPort.rows.length))) > 0;
        this.renderer.setStyle(this.elementRightRef().nativeElement, 'visibility', hasMorePages ? 'visible' : 'hidden');
    }

    getRowClasses(index: number) {
        let rowClasses = 'svy-listformcomponent-row';
        const selectionClass = this.selectionClass();
        if (selectionClass) {
            if (this._foundset().selectedRowIndexes.indexOf(this._foundset().viewPort.startIndex + index) !== -1) {
                rowClasses += ' ' + selectionClass;
            }
        }
        const rowStyleClass = this.rowStyleClass();
        if (rowStyleClass) {
            rowClasses += ' ' + rowStyleClass;
        }
        const rowStyleClassDataprovider = this.rowStyleClassDataprovider();
        if (rowStyleClassDataprovider && rowStyleClassDataprovider[index]) {
            rowClasses += ' ' + rowStyleClassDataprovider[index];
        }
        return rowClasses;
    }

    updateSelection() {
        const selectedRowIndex = this._foundset().selectedRowIndexes[0];
        const element = this.element().nativeElement.children[(this.page > 0) ? selectedRowIndex - this.numberOfCells * this.page : selectedRowIndex];
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
        if (this.servoyApi.isInAbsoluteLayout() || this.responsiveHeight() < 1) {
            aggridStyle['height'] = '100%';
        } else {
            aggridStyle['height.px'] = this.responsiveHeight();
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
        const onSelectionChanged = this.fc.onSelectionChanged();
        if (onSelectionChanged) {
            const selected = this.fc._foundset().selectedRowIndexes;
            if (!selected || selected.indexOf(this.cell.rowIndex) === -1) {
                onSelectionChanged(new CustomEvent('SelectionChanged'));
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
        const startIndex = Math.ceil(this.lfc._foundset().viewPort.startIndex / this.lfc.getNumberOfColumns()); // start index of view port (0-based)
        const endIndex = startIndex + Math.ceil(this.lfc._foundset().viewPort.size / this.lfc.getNumberOfColumns()); // end index of the view port (0-based)

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
                viewPortStartIndex = params.request.startRow - Math.ceil(this.lfc._foundset().viewPort.startIndex / this.lfc.getNumberOfColumns());
                viewPortEndIndex = params.request.endRow - Math.ceil(this.lfc._foundset().viewPort.startIndex / this.lfc.getNumberOfColumns());

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
        const foundset = this.lfc._foundset();
        return foundset.hasMoreRows || (foundset.viewPort.startIndex + foundset.viewPort.size) < foundset.serverSize;
    }

    getLastRowIndex() {
        if (this.hasMoreRecordsToLoad()) {
            return -1;
        } else {
            return Math.ceil(this.lfc._foundset().serverSize / this.lfc.getNumberOfColumns());
        }
    }

    loadExtraRecordsAsync(startIndex: number, size: number) {
        size = (size * AGGRID_MAX_BLOCKS_IN_CACHE * this.lfc.getNumberOfColumns()) + size;
        if (this.hasMoreRecordsToLoad() === false) {
            size = Math.min(size, this.lfc._foundset().serverSize - startIndex);
        }
        if (size < 0) {
            size = 0;
        }

        return this.lfc._foundset().loadExtraRecordsAsync(size);
    }

    getViewPortData(startIndex: number, endIndex: number) {
        const result = [];
        let fsStartIndex = startIndex ? startIndex * this.lfc.getNumberOfColumns() : 0;
        let fsEndIndex = endIndex * this.lfc.getNumberOfColumns();
        if (fsEndIndex > this.lfc._foundset().viewPort.rows.length) fsEndIndex = this.lfc._foundset().viewPort.rows.length;

        // index cannot exceed ServerSize
        fsStartIndex = Math.min(fsStartIndex, this.lfc._foundset().serverSize);
        fsEndIndex = Math.min(fsEndIndex, this.lfc._foundset().serverSize);

        let line = [];
        for (let j = fsStartIndex; j < fsEndIndex; j++) {
            line.push(this.lfc._foundset().viewPort.rows[j]);
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
