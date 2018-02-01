import { Component } from '@angular/core';

import {ServoyService} from './servoy.service';
import {AllServiceService} from './allservices.service';

@Component({
  selector: 'servoy-main',
  templateUrl: './main.component.html'
})
export class MainComponent {
  title = 'Servoy NGClient';
  
  constructor(private servoyService:ServoyService,    private allService: AllServiceService, ){
      this.servoyService.connect();
  }
}
