import { Component, signal } from '@angular/core';
import { I18NProvider } from '../i18n_provider.service';

@Component({
    selector: 'servoycore-alert-window',
    templateUrl: './alert-window.component.html',
    styleUrls: ['./alert-window.component.css'],
    standalone: false
})
export class AlertWindowComponent {
    readonly title = signal<string>(undefined);
    readonly message = signal<string>(undefined);
    onCloseCallback: () => void;
    constructor() {
    }

    dismiss(): void {
      this.onCloseCallback();
    }

    public setOnCloseCallback(callback) {
      this.onCloseCallback = callback;
    }

}
