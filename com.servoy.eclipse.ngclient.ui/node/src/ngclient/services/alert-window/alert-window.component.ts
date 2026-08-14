import { Component, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { I18NProvider } from '../i18n_provider.service';

@Component({
  selector: 'servoycore-alert-window',
  templateUrl: './alert-window.component.html',
  styleUrls: ['./alert-window.component.css'],
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [CommonModule],
})
export class AlertWindowComponent {
  readonly title = signal<string>(undefined!);
  readonly message = signal<string>(undefined!);
  onCloseCallback!: () => void;

  dismiss(): void {
    this.onCloseCallback();
  }

  public setOnCloseCallback(callback: any) {
    this.onCloseCallback = callback;
  }
}
