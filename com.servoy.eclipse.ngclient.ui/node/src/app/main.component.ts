import { Component } from '@angular/core';

import {AllServiceService} from './allservices.service';

@Component({
  selector: 'servoy-main',
  templateUrl: './main.component.html'
})
export class MainComponent {
  title = 'Servoy NGClient';
  
  constructor(allService:AllServiceService){
  }
}
