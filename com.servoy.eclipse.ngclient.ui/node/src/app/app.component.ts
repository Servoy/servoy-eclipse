import { Component } from '@angular/core';

import {AllServiceService} from './allservices.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'app';
  
  constructor(allService:AllServiceService){
  }
}
