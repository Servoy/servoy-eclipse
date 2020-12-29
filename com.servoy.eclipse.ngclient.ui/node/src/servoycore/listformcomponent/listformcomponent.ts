import {
    Component, Input, TemplateRef, ViewChild, ElementRef, AfterViewInit, Renderer2,
    HostListener, ChangeDetectorRef, OnDestroy, Inject, SimpleChange
} from '@angular/core';
import { ViewPortRow } from '../../sablo/spectypes.service';
import { FormComponent } from '../../ngclient/form/form_component.component';
import { ViewportService } from '../../ngclient/services/viewport.service';
import { ComponentConverter, ComponentModel } from '../../ngclient/converters/component_converter';
import { ServoyBaseComponent } from '../../ngclient/basecomponent';
import { Foundset, FoundsetChangeEvent } from '../../ngclient/converters/foundset_converter';
import { FormComponentState } from '../../ngclient/converters/formcomponent_converter';
import { ServoyApi } from '../../ngclient/servoy_public';
import { FormService } from '../../ngclient/form.service';
import { ServoyService } from '../../ngclient/servoy.service';
import { IApiExecutor, instanceOfApiExecutor } from '../../ngclient/types';
import { LoggerFactory, LoggerService } from '../../sablo/logger.service';

@Component({
    selector: 'servoycore-listformcomponent',
    templateUrl: './listformcomponent.html',
    styleUrls: ['./listformcomponent.css']
})
export class ListFormComponent extends ServoyBaseComponent<HTMLDivElement> implements AfterViewInit, OnDestroy, IApiExecutor {

    @Input() containedForm: FormComponentState;
    @Input() foundset: Foundset;
    @Input() selectionClass: string;
    @Input() styleClass: string;
    @Input() responsivePageSize: number;
    @Input() pageLayout: string;
    @Input() onSelectionChanged: (event:any) => void;

    @ViewChild('element', { static: true }) elementRef: ElementRef;
    @ViewChild('firstelement', { static: true }) elementFirstRef: ElementRef;
    @ViewChild('leftelement', { static: true }) elementLeftRef: ElementRef;
    @ViewChild('rightelement', { static: true }) elementRightRef: ElementRef;

    page = 0;
    numberOfCells = 0;
    selectionChangedByKey = false;
    removeListenerFunction: () => void;
    private componentCache: Array<{ [property: string]: ServoyBaseComponent<any> }> = [];
    private log: LoggerService;

    constructor(protected readonly renderer: Renderer2,
        private formservice: FormService,
        private servoyService: ServoyService,
        cdRef: ChangeDetectorRef,
        logFactory: LoggerFactory,
        @Inject(FormComponent) private parent: FormComponent) {
        super(renderer, cdRef);
         this.log = logFactory.getLogger('ListFormComponent');
    }

    @HostListener('keydown', ['$event'])
    handleKeyDown(event: any) {
        if (!this.foundset.multiSelect && (event.key === 'ArrowUp' || event.key === 'ArrowDown')
        // do not move the selection if typeahead or combobox is selected
        && !event.target.className.includes("bts-typeahead")
        && !event.target.className.includes("bts-combobox")) {
            let selectedRowIndex = this.foundset.selectedRowIndexes[0];
            if (event.key === 'ArrowUp') {
                // move to the previous page if the first element (not from the first page) is selected
                if (this.page !== 0 && selectedRowIndex / (this.page) === this.numberOfCells) {
                    this.moveLeft();
                }
                selectedRowIndex--;
            } else if (event.key === 'ArrowDown') { // keydown
                selectedRowIndex++;
                // move to the next page if the last element (not from the last page) is selected
                if (selectedRowIndex / (this.page + 1) === this.numberOfCells) {
                    this.moveRight();
                }
            }
            // do not move the selection for the first or last element
            if (selectedRowIndex >= 0 && selectedRowIndex < this.foundset.serverSize) {
                this.foundset.requestSelectionUpdate([selectedRowIndex]);
                this.selectionChangedByKey = true;
            }
        }
    }

    onRowClick(row: any) {
        for (let i = 0; i < this.foundset.viewPort.rows.length; i++) {
            if (this.foundset.viewPort.rows[i][ViewportService.ROW_ID_COL_KEY] === row['_svyRowId']) {
                this.foundset.requestSelectionUpdate([i + this.foundset.viewPort.startIndex]);
                break;
            }
        }
    }

    svyOnInit() {
        super.svyOnInit();
        this.removeListenerFunction = this.foundset.addChangeListener((event: FoundsetChangeEvent) => {
            let shouldUpdatePagingControls = false;

            if (event.selectedRowIndexesChanged) {
                this.updateSelection();
                if (this.onSelectionChanged) {
                  this.renderer.listen( this.elementRef.nativeElement, 'onselectionchanged', (e) => {
                    this.onSelectionChanged(e);
                  });
                }
            }

            if (event.viewportRowsCompletelyChanged) {
                this.calculateCells();
            } else if (event.fullValueChanged) {
                this.foundset = event.fullValueChanged.newValue;
                this.calculateCells();
                return;
            }

            if (event.serverFoundsetSizeChanged) {
                shouldUpdatePagingControls = true;
            }

            if (shouldUpdatePagingControls) {
                this.updatePagingControls();
            }

            if (event.viewPortSizeChanged && this.foundset.serverSize > 0 && (this.page * this.numberOfCells >= this.foundset.serverSize)
                    && this.foundset.viewPort.size === 0 && this.numberOfCells > 0) {
                this.page = Math.floor((this.foundset.serverSize - 1) / this.numberOfCells);
                this.calculateCells();
            }
            if (event.viewportRowsUpdated) {
                // copy the viewport data over to the cell
                // TODO this only is working for "updates", what happens with deletes or inserts?
                const changes = event.viewportRowsUpdated;
                changes.forEach(change => {
                    const vpRows = this.foundset.viewPort.rows;
                    for (let row = change.startIndex; row <= change.endIndex; row++) {
                        const cache = vpRows[row]._cache;
                        if (!cache) continue;
                        this.containedForm.childElements.forEach(cm => {
                            const cell: Cell = cache.get(cm.name);
                            if (cell) {
                                const mvp = cm.modelViewport[row];
                                for (const key of Object.keys(mvp)) {
                                    cell.model[key] = mvp[key];
                                }
                            }
                        });
                    }
                });
            } else {
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
                        // we need to show less records
                        this.foundset.loadLessRecordsAsync(-deltaSize, true);
                    } // else it's already ok
                }

                this.foundset.notifyChanged(); // let foundset send it's pending requests to server if any
            }
        });
    }

    ngAfterViewInit() {
        this.calculateCells();
    }

    ngOnDestroy() {
        if (this.removeListenerFunction != null) {
            this.removeListenerFunction();
            this.removeListenerFunction = null;
        }
        if (this.containedForm.childElements) {
            this.containedForm.childElements.forEach(component => component.nestedPropertyChange = null);
        }
    }

    getViewportRows(): ViewPortRow[] {
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
        return this.containedForm.getStateHolder().formHeight;
    }

    getRowWidth(): string {
        if (this.pageLayout === 'listview') {
            return '100%';
        }
        return this.containedForm.getStateHolder().formWidth + 'px';
    }

    getRowItems(): Array<ComponentModel> {
        return this.containedForm.getStateHolder().childElements;
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

    getRowItemTemplate(item: ComponentModel): TemplateRef<any> {
        return this.parent.getTemplateForLFC(item);
    }

    getRowItemState(item: ComponentModel, row: ViewPortRow, rowIndex: number): Cell {
        if (row._cache) {
            const cache = row._cache.get(item.name);
            if (cache) return cache;
        }

        if (!item.nestedPropertyChange) {
            item.nestedPropertyChange = (property: string, value: any) => {
                // should we really see if this was a foundset based property so for the selected index?
                this.componentCache.forEach(rowObject => {
                    const ui = rowObject[item.name];
                    if (ui) {
                        const change = {};
                        change[property] = new SimpleChange(value,value,false);
                        ui.ngOnChanges(change);
                    }
                });
            };
        }
        // eslint-disable-next-line prefer-arrow/prefer-arrow-functions
        function Model() {
        }
        Model.prototype = item.model;

        const rowId = row[ViewportService.ROW_ID_COL_KEY];
        const model = new Model();
        const handlers = {};
        const rowItem = new Cell(item, model, handlers, rowId, rowIndex);
        if (item.foundsetConfig && item.foundsetConfig[ComponentConverter.RECORD_BASED_PROPERTIES] instanceof Array) {
            (item.foundsetConfig[ComponentConverter.RECORD_BASED_PROPERTIES] as Array<string>).forEach((p) => {
                rowItem.model[p] = item[ComponentConverter.MODEL_VIEWPORT][rowIndex][p];
            });
        }
        item.mappedHandlers.forEach((value, key) => {
            rowItem.handlers[key] = value.selectRecordHandler(rowId);
        });

        if (!row._cache) row._cache = new Map();
        row._cache.set(item.name, rowItem);
        return rowItem;
    }

    callApi(componentName: string, apiName: string, args: any, path?: string[]): any  {
        if (path && componentName === 'containedForm' && path[0] === 'childElements') {
            const compModel = this.containedForm.childElements[parseInt(path[1], 10)];
            const selectedIndex = this.foundset.selectedRowIndexes[0];
            const row  = this.componentCache[selectedIndex];
            const uiComp = row[compModel.name];
            if (path.length > 2) {
                 if (instanceOfApiExecutor(uiComp)) {
                    uiComp.callApi(path[3], apiName, args, path.slice(3));
                    } else {
                        this.log.error('trying to call api: ' + apiName + ' on component: ' + componentName + ' with path: ' + path +
                         ', but comp: ' + (uiComp == null?' is not found':uiComp.name + ' doesnt implement IApiExecutor') );
                }
            } else {
                const proto = Object.getPrototypeOf(uiComp);
                if (proto[apiName]) {
                    return proto[apiName].apply(uiComp, args);
                } else {
                    this.log.error(this.log.buildMessage(() => ('Api ' + apiName + ' for component ' + componentName + ' was not found, please check component implementation.')));
                    return null;
                }
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
            cell.api = new ListFormComponentServoyApi(cell, this.servoyApi.getFormname(), this.containedForm.absoluteLayout, this.formservice, this.servoyService, this);
        }
        return cell.api;
    }

    datachange(component: Cell, property: string, value: any) {
        component.state.apply(property, value, component.rowId);
    }

    calculateCells() {
        this.numberOfCells = this.servoyApi.isInAbsoluteLayout() ? 0 : this.responsivePageSize;
        if (this.numberOfCells <= 0) {
            if (this.servoyApi.isInAbsoluteLayout()) {
                const parentWidth = this.elementRef.nativeElement.offsetWidth;
                const parentHeight = this.elementRef.nativeElement.offsetHeight;
                const height = this.containedForm.getStateHolder().formHeight;
                const width = this.containedForm.getStateHolder().formWidth;
                const numberOfColumns = (this.pageLayout === 'listview') ? 1 : Math.floor(parentWidth / width);
                const numberOfRows = Math.floor(parentHeight / height);
                this.numberOfCells = numberOfRows * numberOfColumns;
                // always just render 1
                if (this.numberOfCells < 1) this.numberOfCells = 1;
            } else {
                console.error('ListFormComponent ' + this.name + ' should have the responsivePageSize property set because it is used in a responsive form ' + this.servoyApi.getFormname());
            }

        }
        const startIndex = this.page * this.numberOfCells;
        const foundset = this.foundset;
        if (foundset.viewPort.startIndex !== startIndex) {
            foundset.loadRecordsAsync(startIndex, this.numberOfCells);
        } else {
            if (this.numberOfCells > foundset.viewPort.rows.length && foundset.viewPort.startIndex + foundset.viewPort.size < foundset.serverSize) {
                foundset.loadExtraRecordsAsync(Math.min(this.numberOfCells - foundset.viewPort.rows.length, foundset.serverSize - foundset.viewPort.startIndex - foundset.viewPort.size));
            } else if (foundset.viewPort.size > this.numberOfCells) {
                // the (initial) viewport  is bigger then the numberOfCells we have created rows for, adjust the viewport to be smaller.
                foundset.loadLessRecordsAsync(this.numberOfCells - foundset.viewPort.size);
            }
        }
        this.updatePagingControls();
        foundset.setPreferredViewportSize(this.numberOfCells);
        this.cdRef.detectChanges();
    }

    updatePagingControls() {
        this.renderer.setStyle(this.elementFirstRef.nativeElement, 'visibility', this.page > 0 ? 'visible' : 'hidden');
        this.renderer.setStyle(this.elementLeftRef.nativeElement, 'visibility', this.page > 0 ? 'visible' : 'hidden');
        const hasMorePages = this.foundset.hasMoreRows || (this.foundset.serverSize - (this.page * this.numberOfCells + Math.min(this.numberOfCells, this.foundset.viewPort.rows.length))) > 0;
        this.renderer.setStyle(this.elementRightRef.nativeElement, 'visibility', hasMorePages ? 'visible' : 'hidden');
    }

    getRowClasses(index: number) {
        if (this.selectionClass) {
            if (this.foundset.selectedRowIndexes.indexOf(this.foundset.viewPort.startIndex + index) !== -1) {
                return 'svy-listformcomponent-row ' + this.selectionClass;
            }
        }
        return 'svy-listformcomponent-row';
    }

    updateSelection() {
        const selectedRowIndex = this.foundset.selectedRowIndexes[0];
        const element = this.elementRef.nativeElement.children[(this.page > 0) ? selectedRowIndex - this.numberOfCells * this.page : selectedRowIndex];
        if (element && !element.contains(document.activeElement) && this.selectionChangedByKey && !element.className.includes('svyPagination')) {
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
        }
    }
}

class Cell {
    api: ServoyApi;
    name: string;
    constructor(public readonly state: ComponentModel, public readonly model: any,
        public readonly handlers: any, public readonly rowId: any,  public readonly rowIndex: number) {
        this.name = state.name;
    }

}

class ListFormComponentServoyApi extends ServoyApi {
    constructor(private cell: Cell,
                formname: string,
                absolute: boolean,
                formservice: FormService,
                servoyService: ServoyService,
                private fc: ListFormComponent) {
        super(cell.state,formname,absolute,formservice,servoyService);
    }

    registerComponent(comp: ServoyBaseComponent<any>) {
     this.fc.registerComponent(comp,this.cell.rowIndex);
    }

    unRegisterComponent(comp: ServoyBaseComponent<any>) {
     this.fc.unRegisterComponent(comp,this.cell.rowIndex);
    }

    startEdit(property: string) {
        this.cell.state.startEdit(property, this.cell.rowId);
    }
}

