import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ToolbarItemComponent } from './toolbaritem.component';

@Component({
    selector: 'designer-toolbar-spinner',
    templateUrl: './toolbarspinner.component.html',
    standalone: false
})
export class ToolbarSpinnerComponent extends ToolbarItemComponent implements OnInit, OnChanges {

  @Input() value: number;

  ngOnInit() {
    if(this.item.initialValue !== undefined) {
      this.value = this.item.initialValue;
    }
    else if(this.item.min !== undefined){
      this.value = this.item.min;
    }
  }

  ngOnChanges() {
    if(this.item.initialValue !== undefined) {
      this.value = this.item.initialValue;
    }
  }

  dec() {
    this.value--;
    this.item.onclick(''+this.value);
  }
  inc() {
    this.value++;
    this.item.onclick(''+this.value);
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
  
  onSet() {
      if (this.value)  this.item.onclick(''+this.value);
  }
}