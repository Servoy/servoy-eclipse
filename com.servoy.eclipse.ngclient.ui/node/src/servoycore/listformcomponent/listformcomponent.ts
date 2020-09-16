import { Component, Input, TemplateRef,ViewChild, ElementRef, AfterViewInit, Renderer2 } from '@angular/core';
import { ViewPortRow } from '../../sablo/spectypes.service';
import { FormComponent } from '../../ngclient/form/form_component.component';
import { ListFormComponentCache, StructureCache, ComponentCache, FormComponentCache } from '../../ngclient/form.service';
import { ViewportService } from 'ngclient/services/viewport.service';
import { ComponentConverter } from 'ngclient/converters/component_converter';

@Component({
  selector: 'servoycore-listformcomponent',
  templateUrl: './listformcomponent.html',
  styleUrls: ['./listformcomponent.css']
})
export class ListFormComponent implements AfterViewInit {
  @Input() parentForm: FormComponent;
  @Input() listFormComponent: ListFormComponentCache;
  @Input() responsivePageSize : number;
  @Input() pageLayout : string;
  
  @ViewChild('element', {static: true}) elementRef:ElementRef;
  @ViewChild('firstelement', {static: true}) elementFirstRef:ElementRef;
  @ViewChild('leftelement', {static: true}) elementLeftRef:ElementRef;
  @ViewChild('rightelement', {static: true}) elementRightRef:ElementRef;
  
  page :number = 0;
  numberOfCells: number = 0;
  
  constructor(protected readonly renderer: Renderer2) {
  }
  
  ngAfterViewInit() {
      this.calculateCells();
  }
  
  getViewportRows():ViewPortRow[] {
    return this.listFormComponent.getFoundset().viewPort.rows;
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
          const rowId = this.listFormComponent.getFoundset().viewPort.rows[rowIndex][ViewportService.ROW_ID_COL_KEY];
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
  
  calculateCells(){
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
      let foundset = this.listFormComponent.getFoundset();
      if (foundset.viewPort.startIndex != startIndex) {
          foundset.loadRecordsAsync(startIndex, this.numberOfCells);
      } else {
          if (this.numberOfCells > foundset.viewPort.rows.length && foundset.viewPort.startIndex + foundset.viewPort.size < foundset.serverSize) {
              foundset.loadExtraRecordsAsync(Math.min(this.numberOfCells - foundset.viewPort.rows.length, foundset.serverSize - foundset.viewPort.startIndex - foundset.viewPort.size));
          }
          else if (foundset.viewPort.size > this.numberOfCells) {
              // the (initial) viewport  is bigger then the numberOfCells we have created rows for, adjust the viewport to be smaller.
              foundset.loadLessRecordsAsync(this.numberOfCells - foundset.viewPort.size);
          }
      }
      this.renderer.setStyle(  this.elementFirstRef.nativeElement, 'visibility' , this.page > 0 ? 'visible' : 'hidden' );
      this.renderer.setStyle(  this.elementLeftRef.nativeElement, 'visibility' , this.page > 0 ? 'visible' : 'hidden' );
      let hasMorePages = foundset.hasMoreRows || (foundset.serverSize - (this.page * this.numberOfCells + Math.min(this.numberOfCells, foundset.viewPort.rows.length))) > 0 ;
      this.renderer.setStyle(  this.elementRightRef.nativeElement, 'visibility' , hasMorePages ? 'visible' : 'hidden' );
      foundset.setPreferredViewportSize(this.numberOfCells);
  }
}