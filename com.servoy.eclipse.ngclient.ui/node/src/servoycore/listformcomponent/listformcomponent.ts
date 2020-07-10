import { Component, ViewChild, Input, Renderer2, ElementRef } from '@angular/core';

@Component({
  selector: 'servoycore-listformcomponent',
  templateUrl: './listformcomponent.html'
})
export class ListFormComponent {
  @Input() foundset;

  constructor() {
    console.log("lisformcomponent")
  }

  ngOnInit() {
    console.log("lisformcomponent")
  }
}