import { Component, Input } from '@angular/core';
import { I18NProvider } from '../i18n_provider.service';

@Component({
    selector: 'servoycore-alert-window',
    templateUrl: './alert-window.component.html',
    styleUrls: ['./alert-window.component.css']
})
export class AlertWindowComponent {
    @Input() title: string;
    @Input() message: string;
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
