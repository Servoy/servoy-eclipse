import { Component, Input, TemplateRef } from '@angular/core';
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
export class ListFormComponent {
  @Input() parentForm: FormComponent;
  @Input() listFormComponent: ListFormComponentCache;

  getViewportRows():ViewPortRow[] {
    return this.listFormComponent.getFoundset().viewPort.rows;
  }

  getStyleClasses(): string[] {
    return this.listFormComponent.formComponentProperties.classes;
  }

  getRowHeight(): number {
    return this.listFormComponent.getFormComponentType().formHeight;
  }

  getRowItems(): Array<StructureCache | ComponentCache | FormComponentCache> {
    return this.listFormComponent.items
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
}