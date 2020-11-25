import { Component, Input, TemplateRef, ViewChild, ElementRef, AfterViewInit, Renderer2, HostListener, ChangeDetectorRef, EventEmitter, Output, OnDestroy, Inject, NgZone, AfterViewChecked } from '@angular/core';
import { ViewPortRow } from '../../sablo/spectypes.service';
import { FormComponent } from '../../ngclient/form/form_component.component';
import { ViewportService } from '../../ngclient/services/viewport.service';
import { ComponentConverter, ComponentType } from '../../ngclient/converters/component_converter';
import { ServoyBaseComponent } from '../../ngclient/basecomponent';
import { Foundset, FoundsetChangeEvent } from '../../ngclient/converters/foundset_converter';
import { FormComponentType } from '../../ngclient/converters/formcomponent_converter';

@Component({
  selector: 'servoycore-listformcomponent',
  templateUrl: './listformcomponent.html',
  styleUrls: ['./listformcomponent.css']
})
export class ListFormComponent extends ServoyBaseComponent implements AfterViewInit, OnDestroy {

  @Input() containedForm: FormComponentType;
  @Input() foundset: Foundset;
  @Input() selectionClass: string; 
  @Input() styleClass: string;
  @Input() responsivePageSize: number;
  @Input() pageLayout: string;
  @Input() onSelectionChanged;
  @Output() foundsetChange = new EventEmitter();

  @ViewChild('element', {static: true}) elementRef: ElementRef;
  @ViewChild('firstelement', {static: true}) elementFirstRef: ElementRef;
  @ViewChild('leftelement', {static: true}) elementLeftRef: ElementRef;
  @ViewChild('rightelement', {static: true}) elementRightRef: ElementRef;

  page = 0;
  numberOfCells = 0;
  selectionChangedByKey = false;
  changeListener: (change: FoundsetChangeEvent) => void;

  constructor(protected readonly renderer: Renderer2,
     cdRef: ChangeDetectorRef,
     @Inject(FormComponent) private parent: FormComponent) {
    super(renderer, cdRef);
  }

  @HostListener('window:keydown', ['$event'])
  handleKeyDown(event: KeyboardEvent) {
    if (!this.foundset.multiSelect && event.key == 'ArrowUp' || event.key == 'ArrowDown') {
      let selectedRowIndex = this.foundset.selectedRowIndexes[0];
      if (event.key == 'ArrowUp') {
        // move to the previous page if the first element (not from the first page) is selected
        if (this.page != 0 && selectedRowIndex / (this.page) == this.responsivePageSize) {
          this.moveLeft();
        }
        selectedRowIndex--;
      } else if (event.key == 'ArrowDown') { // keydown
          selectedRowIndex++;
          // move to the next page if the last element (not from the last page) is selected
        if (selectedRowIndex / (this.page + 1) == this.responsivePageSize) {
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
    for ( let i = 0; i < this.foundset.viewPort.rows.length; i++ ) {
      if (this.foundset.viewPort.rows[i][ViewportService.ROW_ID_COL_KEY] == row['_svyRowId']) {
        this.foundset.requestSelectionUpdate([i + this.foundset.viewPort.startIndex]);
        this.foundsetChange.emit(this.foundset);
        break;
      }
    }
  }

  svyOnInit() {
    super.svyOnInit();
    this.changeListener = this.foundset.addChangeListener((event: FoundsetChangeEvent) => {
      let shouldUpdatePagingControls = false;
      if (event.selectedRowIndexesChanged) {
        this.updateSelection(event.selectedRowIndexesChanged.newValue, event.selectedRowIndexesChanged.oldValue);
        if (this.onSelectionChanged) {
          this.renderer.listen( this.elementRef.nativeElement, 'onselectionchanged', (e) => {
            this.onSelectionChanged(e);
          });
        }
      }
      if (event.serverFoundsetSizeChanged) {
        shouldUpdatePagingControls = true;
      }

      if (shouldUpdatePagingControls) {
        this.updatePagingControls();
      }
    });
  }

  ngAfterViewInit() {
    this.calculateCells();
  }

  ngOnDestroy() {
    this.foundset.removeChangeListener(this.changeListener);
  }

  getViewportRows(): ViewPortRow[] {
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
      if (this.pageLayout == 'listview')
      {
          return "100%";
      }    
      return this.containedForm.getStateHolder().formWidth + 'px';
  }
  
  getRowItems(): Array<ComponentType> {
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
      if (this.page != 0) {
          this.page = 0;
          this.calculateCells();
      }
  }

  getRowItemTemplate(item: ComponentType): TemplateRef<any> { 
    return this.parent.getTemplateForLFC(item.getStateHolder());
  }

  getRowItemState(item: ComponentType, rowIndex: number) {
    let rowItem:any = item;
    if(item instanceof ComponentType) {
      for(let element of this.containedForm.getStateHolder().childElements) {
        const state = element.getStateHolder();
        if(state.name == item.getStateHolder().name) {
          rowItem = Object.assign({}, state);
          if (state.foundsetConfig && state.foundsetConfig[ComponentConverter.RECORD_BASED_PROPERTIES] instanceof Array && rowIndex < rowItem[ComponentConverter.MODEL_VIEWPORT].length) {
            (state.foundsetConfig[ComponentConverter.RECORD_BASED_PROPERTIES] as Array<string>).forEach((p) => {
              rowItem.model[p] = rowItem[ComponentConverter.MODEL_VIEWPORT][rowIndex][p];
            });
          }
          rowItem.handlers = {};
          const rowId = this.foundset.viewPort.rows[rowIndex][ViewportService.ROW_ID_COL_KEY];
          Object.entries(state.handlers).forEach(([k, v]) => {
            const wrapperF: any = v;
            rowItem.handlers[k] = wrapperF.selectRecordHandler(rowId);
          });
          break;
        }
      }
    }
    return rowItem;
  }

  calculateCells() {
      this.numberOfCells = this.responsivePageSize;
      if (this.numberOfCells <= 0 ) {
          const parentWidth = this.elementRef.nativeElement.offsetWidth;
          const parentHeight = this.elementRef.nativeElement.offsetHeight;
          const height = this.containedForm.getStateHolder().formHeight;
          const width = this.containedForm.getStateHolder().formWidth; 
          const numberOfColumns =  (this.pageLayout == 'listview') ? 1 : Math.floor(parentWidth/width);
          const numberOfRows = Math.floor(parentHeight/height);
          this.numberOfCells  = numberOfRows * numberOfColumns;
          // always just render 1
          if (this.numberOfCells < 1) this.numberOfCells = 1;
      }
      const startIndex = this.page * this.numberOfCells;
      const foundset = this.foundset;
      if (foundset.viewPort.startIndex != startIndex) {
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
  }

  updatePagingControls() {
    this.renderer.setStyle(  this.elementFirstRef.nativeElement, 'visibility' , this.page > 0 ? 'visible' : 'hidden' );
    this.renderer.setStyle(  this.elementLeftRef.nativeElement, 'visibility' , this.page > 0 ? 'visible' : 'hidden' );
    const hasMorePages = this.foundset.hasMoreRows || (this.foundset.serverSize - (this.page * this.numberOfCells + Math.min(this.numberOfCells, this.foundset.viewPort.rows.length))) > 0 ;
    this.renderer.setStyle(  this.elementRightRef.nativeElement, 'visibility' , hasMorePages ? 'visible' : 'hidden' );
  }

  updateSelection(newValue, oldValue) {
    if (this.selectionClass) {
      const children = this.elementRef.nativeElement.children;
      if (oldValue) {
        for (let k = 0; k < oldValue.length; k++) {
          const idx = oldValue[k] - this.foundset.viewPort.startIndex;
          if (idx > -1 && idx < children.length - 1) {
            this.renderer.removeClass(this.elementRef.nativeElement.children[idx + 1], this.selectionClass);
          }
        }
      } else {
        for (let k = 1; k < children.length; k++) {
          this.renderer.removeClass(this.elementRef.nativeElement.children[k], this.selectionClass);
        }
      }
      for (let k = 0; k < newValue.length; k++) {
        const idx = newValue[k] - this.foundset.viewPort.startIndex;
        if (idx > -1 && idx < children.length - 1) {
          this.renderer.addClass(this.elementRef.nativeElement.children[idx + 1], this.selectionClass);
        }
      }
    }

    const selectedRowIndex = this.foundset.selectedRowIndexes[0];
    const element = this.elementRef.nativeElement.children[(this.page > 0) ? selectedRowIndex - this.responsivePageSize * this.page : selectedRowIndex];
    if (element && !element.contains(document.activeElement) && this.selectionChangedByKey && !element.className.includes('svyPagination')) {
      element.focus();
      this.selectionChangedByKey = false;
    }
  }
}

