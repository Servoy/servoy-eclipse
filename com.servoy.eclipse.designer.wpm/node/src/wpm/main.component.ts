import { Component, ChangeDetectionStrategy } from '@angular/core';
import { WpmService } from './wpm.service';
import { HeaderComponent } from './header/header.component';
import { ContentComponent } from './content/content.component';

@Component({
    selector: 'app-wpm',
    templateUrl: './main.component.html',
    styleUrls: ['./main.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [HeaderComponent, ContentComponent]
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