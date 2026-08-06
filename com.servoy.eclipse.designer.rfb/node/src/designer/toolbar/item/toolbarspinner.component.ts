import { Component, OnChanges, OnInit, ChangeDetectionStrategy, model } from '@angular/core';
import { ToolbarItemComponent } from './toolbaritem.component';

@Component({
    selector: 'designer-toolbar-spinner',
    templateUrl: './toolbarspinner.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class ToolbarSpinnerComponent extends ToolbarItemComponent implements OnInit, OnChanges {

  value = model<number>();

  ngOnInit() {
    if(this.item()!.initialValue !== undefined) {
      this.value.set(this.item()!.initialValue);
    } else if(this.item()!.min !== undefined){
      this.value.set(this.item()!.min);
    }
  }

  ngOnChanges() {
    if(this.item()!.initialValue !== undefined) {
      this.value.set(this.item()!.initialValue);
    }
  }

  dec() {
    this.value.set(this.value()! - 1);
    this.item()!.onclick!('' + this.value());
  }
  inc() {
    this.value.set(this.value()! + 1);
    this.item()!.onclick!('' + this.value());
  }
  checkInput() {
    if (this.value() === undefined) {
      this.value.set(this.item()!.initialValue);
    }
    if (this.value()! < this.item()!.min) {
      this.value.set(this.item()!.min);
    }
    if (this.value()! > this.item()!.max) {
      this.value.set(this.item()!.max);
    }
  }
  
  onSet() {
      if (this.value())  this.item()!.onclick!('' + this.value());
  }
}