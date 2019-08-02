import { Component } from '@angular/core';
import { WpmService } from './wpm.service';

@Component({
  selector: 'app-wpm',
  templateUrl: './main.component.html',
  styleUrls: ['./main.component.css']
})
export class MainComponent {
  constructor(public wpmService: WpmService) {
  }

  isContentAvailable(): boolean {
    return this.wpmService.isContentAvailable();
  }
}
