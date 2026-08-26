import { Component, inject, DOCUMENT, ChangeDetectionStrategy, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SabloTabseq } from '@servoy/public';
import { SabloService } from '../../../sablo/sablo.service';
import { SvyWindow } from '../window.service';
import { DefaultNavigator } from '../../../servoycore/default-navigator/default-navigator';
import { FormComponent } from '../../form/form_component.component';

import { FormService } from '../../form.service';

@Component({
  selector: 'servoycore-dialog-window',
  templateUrl: './dialog-window.component.html',
  styleUrls: ['./dialog-window.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  imports: [CommonModule, SabloTabseq, DefaultNavigator, FormComponent],
})
export class DialogWindowComponent {
  window!: SvyWindow;
  firstTimeFocus = true;

  private readonly sabloService = inject(SabloService);
  private readonly formservice = inject(FormService);
  private readonly doc = inject(DOCUMENT) as Document;

  readonly formName = computed(() => {
    const name = this.window?.form()?.name;
    return name && this.formservice.hasFormCacheEntry(name) ? name : null;
  });

  readonly navigatorFormName = computed(() => {
    const nav = this.window?.navigatorForm();
    const name = nav && nav.name && nav.name.lastIndexOf('default_navigator_container.html') === -1 ? nav.name : null;
    return name && this.formservice.hasFormCacheEntry(name) ? name : null;
  });

  readonly defaultNavigator = computed(() => {
    const nav = this.window?.navigatorForm();
    return !!(nav && nav.name && nav.name.lastIndexOf('default_navigator_container.html') >= 0);
  });

  readonly isUndecorated = computed(() => {
    return this.window?.undecorated() || this.window?.opacity() < 1;
  });

  constructor() {}

  setWindow(window: SvyWindow) {
    this.window = window;
  }

  getOpacity(): number {
    return this.window.opacity();
  }

  getTitle(): string {
    return this.window.title();
  }

  getBackgroundColor(): string | null {
    return this.window.transparent() ? 'transparent' : null;
  }

  getCSSClassName() {
    return this.window.cssClassName();
  }

  cancel() {
    this.sabloService.callService('$windowService', 'windowClosing', { window: this.window.name }, false);
  }

  firstElementFocused(event: Event) {
    const firstTabIndex = parseInt(this.doc.getElementById(this.window.name + '_tabStart')!.getAttribute('tabindex')!, 10);
    const lastTabIndex = parseInt(this.doc.getElementById(this.window.name + '_tabStop')!.getAttribute('tabindex')!, 10);
    if (this.firstTimeFocus === true) {
      for (let i = firstTabIndex + 1; i < lastTabIndex; i++) {
        const newTarget: any = this.doc.querySelector("[tabindex='" + i + "']");
        // if there is no focusable element in the window, then newTarget == e.target,
        // do a check here to avoid focus cycling
        if (this.isElementVisibleAndNotDisabled(newTarget) && event.target != newTarget) {
          newTarget.focus();
          this.firstTimeFocus = false;
          break;
        }
      }
    } else {
      for (let i = lastTabIndex - 1; i > firstTabIndex; i--) {
        const newTarget: any = this.doc.querySelector("[tabindex='" + i + "']");
        // if there is no focusable element in the window, then newTarget == e.target,
        // do a check here to avoid focus cycling
        if (this.isElementVisibleAndNotDisabled(newTarget) && event.target != newTarget) {
          newTarget.focus();
          this.firstTimeFocus = false;
          break;
        }
      }
    }
  }

  lastElementFocused(event: Event) {
    const firstTabIndex = parseInt(this.doc.getElementById(this.window.name + '_tabStart')!.getAttribute('tabindex')!, 10);
    const lastTabIndex = parseInt(this.doc.getElementById(this.window.name + '_tabStop')!.getAttribute('tabindex')!, 10);
    for (let i = firstTabIndex + 1; i < lastTabIndex; i++) {
      const newTarget: any = this.doc.querySelector("[tabindex='" + i + "']");
      // if there is no focusable element in the window, then newTarget == e.target,
      // do a check here to avoid focus cycling
      if (this.isElementVisibleAndNotDisabled(newTarget) && event.target != newTarget) {
        newTarget.focus();
        this.firstTimeFocus = false;
        break;
      }
    }
  }

  isElementVisibleAndNotDisabled(element: any): boolean {
    return (element.offsetWidth > 0 || element.offsetHeight > 0) && !element.disabled;
  }
}
