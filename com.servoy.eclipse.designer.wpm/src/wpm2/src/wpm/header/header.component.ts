import { Component, OnInit } from '@angular/core';
import { WpmService } from '../wpm.service';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit {

  wpmService: WpmService;

  constructor(wpmService: WpmService) {
    this.wpmService = wpmService;
  }

  ngOnInit() {
  }

  getActiveSolution(): string {
    return this.wpmService.getActiveSolution();
  }

  isNeedRefresh(): boolean {
    return this.wpmService.isNeedRefresh();
  }
}
