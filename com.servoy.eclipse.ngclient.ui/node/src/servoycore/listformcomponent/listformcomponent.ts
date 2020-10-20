import { Component, Input, TemplateRef,ViewChild, ElementRef, AfterViewInit, Renderer2, HostListener, OnChanges, SimpleChanges, OnInit, ChangeDetectorRef, EventEmitter, Output, OnDestroy } from '@angular/core';
import { IFoundset, ViewPortRow } from '../../sablo/spectypes.service';
import { FormComponent } from '../../ngclient/form/form_component.component';
import { ListFormComponentCache, StructureCache, ComponentCache, FormComponentCache } from '../../ngclient/form.service';
import { ViewportService } from '../../ngclient/services/viewport.service';
import { ComponentConverter } from '../../ngclient/converters/component_converter';
import { ServoyBaseComponent } from '../../ngclient/basecomponent';
import { Foundset, FoundsetChangeEvent } from '../../ngclient/converters/foundset_converter';
import { constants } from 'buffer';

@Component({
  selector: 'servoycore-listformcomponent',
  templateUrl: './listformcomponent.html',
  styleUrls: ['./listformcomponent.css']
})
export class ListFormComponent extends ServoyBaseComponent implements AfterViewInit, OnDestroy{
  
  @Input() parentForm: FormComponent;
  @Input() listFormComponent: ListFormComponentCache;
  @Input() responsivePageSize : number;
  @Input() pageLayout : string;
  @Input() selectionChangedHandler;
  @Output() foundsetChange = new EventEmitter();
  
  @ViewChild('element', {static: true}) elementRef:ElementRef;
  @ViewChild('firstelement', {static: true}) elementFirstRef:ElementRef;
  @ViewChild('leftelement', {static: true}) elementLeftRef:ElementRef;
  @ViewChild('rightelement', {static: true}) elementRightRef:ElementRef;
  
  page :number = 0;
  numberOfCells: number = 0;
  selectionChangedByKey: boolean = false; 
  changeListener: (change: FoundsetChangeEvent) => void;
  
  constructor(protected readonly renderer: Renderer2, cdRef: ChangeDetectorRef) {
    super(renderer, cdRef);
  }
  
  @HostListener('window:keydown', ['$event'])
  handleKeyDown(event: KeyboardEvent) {
    const foundset = this.getFoundset();
    if(!foundset.multiSelect && event.key == 'ArrowUp' || event.key == 'ArrowDown') {
      let selectedRowIndex = foundset.selectedRowIndexes[0];
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
      if (selectedRowIndex >= 0 && selectedRowIndex < foundset.serverSize) {
        foundset.requestSelectionUpdate([selectedRowIndex]);
        this.foundsetChange.emit(foundset);
        this.selectionChangedByKey = true;
      } 
    }
  }
  onRowClick(row: any) {
    for ( let i = 0; i < this.getFoundset().viewPort.rows.length; i++ ) {
      if (this.getFoundset().viewPort.rows[i][ViewportService.ROW_ID_COL_KEY] == row["_svyRowId"])
      {
        this.getFoundset().requestSelectionUpdate([i + this.getFoundset().viewPort.startIndex]);
        this.foundsetChange.emit(this.getFoundset());
        break;
      }	
    }
  }

  svyOnInit() {
    super.svyOnInit();
    this.changeListener = this.getFoundset().addChangeListener((event: FoundsetChangeEvent) => {
      let shouldUpdatePagingControls = false;
      if (event.selectedRowIndexesChanged) {
        this.updateSelection(event.selectedRowIndexesChanged.newValue, event.selectedRowIndexesChanged.oldValue);
        if(this.selectionChangedHandler) {
          this.renderer.listen( this.elementRef.nativeElement, 'onselectionchanged', (e) => {
            this.selectionChangedHandler(e);
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
    this.getFoundset().removeChangeListener(this.changeListener);
  }

  getFoundset(): Foundset {
    return this.listFormComponent.getFoundset();
  }

  getViewportRows():ViewPortRow[] {
    return this.getFoundset().viewPort.rows;
  }

  getStyleClasses(): string[] {
    return this.listFormComponent.formComponentProperties.classes;
  }

  getRowHeight(): number {
    return this.listFormComponent.getFormComponentType().formHeight;
  }

  getRowWidth(): string {
      if (this.pageLayout == 'listview')
      {
          return "100%";
      }    
      return this.listFormComponent.getFormComponentType().formWidth+'px';
  }
  
  getRowItems(): Array<StructureCache | ComponentCache | FormComponentCache> {
    return this.listFormComponent.items
  }

  moveLeft(){
      if (this.page > 0) {
          this.page--;
          this.calculateCells();
      }
  }
  
  moveRight(){
      this.page++;
      this.calculateCells();
  }
  
  firstPage(){
      if (this.page != 0) {
          this.page = 0;
          this.calculateCells();
      } 
  }
  
  getRowItemTemplate(item: StructureCache | ComponentCache | FormComponentCache): TemplateRef<any> {
    return this.parentForm.getTemplate(item);
  }

  getRowItemState(item: StructureCache | ComponentCache | FormComponentCache, rowIndex: number) {
    let rowItem:any = item;
    if(item instanceof ComponentCache) {
      for(let element of this.listFormComponent.getFormComponentType().childElements) {
        if(element.name == item.name) {
          rowItem = Object.assign({}, element);
          if(element.foundsetConfig && element.foundsetConfig[ComponentConverter.RECORD_BASED_PROPERTIES] instanceof Array && rowIndex < rowItem[ComponentConverter.MODEL_VIEWPORT].length) {
            (element.foundsetConfig[ComponentConverter.RECORD_BASED_PROPERTIES] as Array<string>).forEach((p) => {
              rowItem.model[p] = rowItem[ComponentConverter.MODEL_VIEWPORT][rowIndex][p];
            })
          }
          rowItem.handlers = {};
          const rowId = this.getFoundset().viewPort.rows[rowIndex][ViewportService.ROW_ID_COL_KEY];
          Object.entries(element.handlers).forEach(([k, v]) => {
            const wrapperF: any = v;
            rowItem.handlers[k] = wrapperF.selectRecordHandler(rowId);
          })
          break;
        }
      }
    }
    return rowItem;
  }
  
  calculateCells() {
      this.numberOfCells = this.responsivePageSize;
      if (this.numberOfCells <= 0 ) {
          let parentWidth = this.elementRef.nativeElement.offsetWidth;
          let parentHeight = this.elementRef.nativeElement.offsetHeight;
          const height = this.listFormComponent.getFormComponentType().formHeight;
          const width = this.listFormComponent.getFormComponentType().formWidth;
          const numberOfColumns =  (this.pageLayout == 'listview') ? 1 : Math.floor(parentWidth/width);
          const numberOfRows = Math.floor(parentHeight/height);
          this.numberOfCells  = numberOfRows * numberOfColumns;
          // always just render 1
          if (this.numberOfCells < 1) this.numberOfCells = 1;
      }
      const startIndex = this.page * this.numberOfCells;
      const foundset = this.getFoundset();
      let shouldEmitFoundsetChange = false;
      if (foundset.viewPort.startIndex != startIndex) {
          foundset.loadRecordsAsync(startIndex, this.numberOfCells);
          shouldEmitFoundsetChange = true;
      } else {
          if (this.numberOfCells > foundset.viewPort.rows.length && foundset.viewPort.startIndex + foundset.viewPort.size < foundset.serverSize) {
              foundset.loadExtraRecordsAsync(Math.min(this.numberOfCells - foundset.viewPort.rows.length, foundset.serverSize - foundset.viewPort.startIndex - foundset.viewPort.size));
              shouldEmitFoundsetChange = true;
          }
          else if (foundset.viewPort.size > this.numberOfCells) {
              // the (initial) viewport  is bigger then the numberOfCells we have created rows for, adjust the viewport to be smaller.
              foundset.loadLessRecordsAsync(this.numberOfCells - foundset.viewPort.size);
              shouldEmitFoundsetChange = true;
          }
      }
      if (shouldEmitFoundsetChange) this.foundsetChange.emit(foundset);
      this.updatePagingControls();
      foundset.setPreferredViewportSize(this.numberOfCells);
  }

  updatePagingControls() {
    const foundset = this.getFoundset();
    this.renderer.setStyle(  this.elementFirstRef.nativeElement, 'visibility' , this.page > 0 ? 'visible' : 'hidden' );
    this.renderer.setStyle(  this.elementLeftRef.nativeElement, 'visibility' , this.page > 0 ? 'visible' : 'hidden' );
    let hasMorePages = foundset.hasMoreRows || (foundset.serverSize - (this.page * this.numberOfCells + Math.min(this.numberOfCells, foundset.viewPort.rows.length))) > 0 ;
    this.renderer.setStyle(  this.elementRightRef.nativeElement, 'visibility' , hasMorePages ? 'visible' : 'hidden' );
  }

  updateSelection(newValue, oldValue) {
    const foundset = this.getFoundset();
    const selectionClass = this.listFormComponent.model.selectionClass;
    if (selectionClass) {
      const children = this.elementRef.nativeElement.children;
      if(oldValue) {
        for(let k = 0; k < oldValue.length; k++) {
          let idx = oldValue[k] - foundset.viewPort.startIndex;
          if(idx > -1 && idx < children.length - 1) {
            this.renderer.removeClass(this.elementRef.nativeElement.children[idx + 1], selectionClass);
          }
        }
      }
      else {
        for(let k = 1; k < children.length; k++) {
          this.renderer.removeClass(this.elementRef.nativeElement.children[k], selectionClass);
        }
      }
      for(let k = 0; k < newValue.length; k++) {
        let idx = newValue[k] - foundset.viewPort.startIndex;
        if(idx > -1 && idx < children.length - 1) {
          this.renderer.addClass(this.elementRef.nativeElement.children[idx + 1], selectionClass);
        }
      }
    }

    let selectedRowIndex = this.getFoundset().selectedRowIndexes[0];
    const element = this.elementRef.nativeElement.children[(this.page > 0) ? selectedRowIndex - this.responsivePageSize * this.page : selectedRowIndex]
    if (element && !element.contains(document.activeElement) && this.selectionChangedByKey && !element.className.includes("svyPagination")) { 
      element.focus(); 
      this.selectionChangedByKey = false; 
    } 
  }
}