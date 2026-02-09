import { Component, input, signal } from '@angular/core';
import { I18NProvider } from '../i18n_provider.service';

@Component({
    selector: 'servoycore-alert-window',
    templateUrl: './alert-window.component.html',
    styleUrls: ['./alert-window.component.css'],
    standalone: false
})
export class AlertWindowComponent {
    readonly title = input<string>(undefined);
    readonly message = input<string>(undefined);
    _title = signal<string>(undefined);
    _message = signal<string>(undefined);
    onCloseCallback: () => void;
    constructor() {
    }

    ngOnInit(): void {
        this._title.set(this.title());
        this._message.set(this.message());
    }

    dismiss(): void {
      this.onCloseCallback();
    }

    public setOnCloseCallback(callback) {
      this.onCloseCallback = callback;
    }

}
