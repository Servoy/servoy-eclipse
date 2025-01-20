import { Component } from '@angular/core';
import { WpmService } from './wpm.service';

@Component({
    selector: 'app-wpm',
    templateUrl: './main.component.html',
    styleUrls: ['./main.component.css'],
    standalone: false
})
export class MainComponent {
  url: URL;
  darkTheme: boolean;

  constructor(public wpmService: WpmService) {
	this.url = new URL(window.location.href);
	this.darkTheme = this.wpmService.isDarkTheme();
	if (this.darkTheme) {
		document.body.classList.add('dark');
	}
  }

  isContentAvailable(): boolean {
    return this.wpmService.isContentAvailable();
  }
}