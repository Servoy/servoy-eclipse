import { Component, Input, TemplateRef } from '@angular/core';
import { ViewPortRow } from '../../sablo/spectypes.service';
import { FormComponent } from '../../ngclient/form/form_component.component';
import { ListFormComponentCache, StructureCache, ComponentCache, FormComponentCache } from '../../ngclient/form.service';

@Component({
  selector: 'servoycore-listformcomponent',
  templateUrl: './listformcomponent.html',
  styleUrls: ['./listformcomponent.css']
})
export class ListFormComponent {
  @Input() parentForm: FormComponent;
  @Input() listFormComponent: ListFormComponentCache;

  getViewportRows():ViewPortRow[] {
    return this.listFormComponent.foundset.viewPort.rows;
  }

  getStyleClasses(): string[] {
    return this.listFormComponent.formComponentProperties.classes;
  }

  getRowHeight(): number {
    return this.listFormComponent.formComponentType.formHeight;
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
      for(let element of this.listFormComponent.formComponentType.childElements) {
        if(element.name == item.name) {
          rowItem = Object.assign({}, element);
          if(element.foundsetConfig && element.foundsetConfig['recordBasedProperties'] instanceof Array && rowIndex < rowItem.model_vp.length) {
            (element.foundsetConfig['recordBasedProperties'] as Array<string>).forEach((p) => {
              rowItem.model[p] = rowItem.model_vp[rowIndex][p];
            })
          }
          rowItem.handlers = [];
          break;
        }
      }
    }
    return rowItem;
  }
}