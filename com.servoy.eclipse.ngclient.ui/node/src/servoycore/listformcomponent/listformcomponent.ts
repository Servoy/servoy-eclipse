import { Component, ViewChild, Input, Renderer2, ElementRef } from '@angular/core';
import { IFoundset } from '../../sablo/spectypes.service';

@Component({
  selector: 'servoycore-listformcomponent',
  templateUrl: './listformcomponent.html'
})
export class ListFormComponent {
  @Input() foundset: IFoundset;

  constructor() {
    console.log("lisformcomponent")
  }

  ngOnInit() {
    console.log("lisformcomponent")
  }
}