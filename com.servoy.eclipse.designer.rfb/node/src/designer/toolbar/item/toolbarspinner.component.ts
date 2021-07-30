import { Component, Input } from "@angular/core";
import { ToolbarItemComponent } from "./toolbaritem.component";

@Component({
    selector: 'designer-toolbar-spinner',
    templateUrl: './toolbarspinner.component.html'
  })
export class ToolbarSpinnerComponent extends ToolbarItemComponent{

  @Input() value: number;

  dec() {
    this.value--;
  }
  inc() {
    this.value++;
  }
  checkInput() {
    if (this.value === undefined) {
      this.value = this.item.initialValue; 
    }
    if (this.value < this.item.min) {
      this.value = this.item.min;
    }
    if (this.value > this.item.max) {
      this.value = this.item.max;
    }
  }

}