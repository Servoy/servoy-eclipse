import { Component, Inject } from '@angular/core';
import { SabloService } from '../../../sablo/sablo.service';
import { SvyWindow } from '../window.service';
import { DOCUMENT } from '@angular/common';
import { FormService } from '../../form.service';

@Component({
    selector: 'servoycore-dialog-window',
    templateUrl: './dialog-window.component.html',
    styleUrls: ['./dialog-window.component.css']
  })
  export class DialogWindowComponent {

    window: SvyWindow;
    firstTimeFocus = true;

    constructor(private sabloService: SabloService, private formservice: FormService, @Inject(DOCUMENT) private doc: Document) {
    }

    setWindow(window: SvyWindow) {
      this.window = window;
    }

    getFormName(): string {
        const name =  this.window.form ? this.window.form.name : undefined;
        if (name && this.formservice.hasFormCacheEntry(name))
            return name;
        return null;
    }

    getNavigatorFormName(): string {
        const name = (this.window.navigatorForm && this.window.navigatorForm.name && this.window.navigatorForm.name.lastIndexOf('default_navigator_container.html') === -1) ?
                this.window.navigatorForm.name : null;
        if (name && this.formservice.hasFormCacheEntry(name))
            return name;
        return null;
    }

    hasDefaultNavigator(): boolean{
        return this.window.navigatorForm && this.window.navigatorForm.name && this.window.navigatorForm.name.lastIndexOf('default_navigator_container.html') >= 0;
    }

    isUndecorated(): boolean {
      return this.window.undecorated || ( this.window.opacity < 1 );
    }

    getOpacity(): number {
      return this.window.opacity;
    }

    getTitle(): string {
      return this.window.title;
    }

    getBackgroundColor(): string {
      return this.window.transparent ? 'transparent' : null;
    }

    getCSSClassName() {
      return this.window.cssClassName;
    }

    cancel() {
      this.sabloService.callService( '$windowService', 'windowClosing', { window: this.window.name }, false );
    }

    firstElementFocused(event: Event) {
      const firstTabIndex = parseInt(this.doc.getElementById('tabStart').getAttribute('tabindex'), 10);
      const lastTabIndex = parseInt(this.doc.getElementById('tabStop').getAttribute('tabindex'), 10);
      if (this.firstTimeFocus === true) {						
        for(let i = firstTabIndex + 1; i < lastTabIndex; i++) {
          const newTarget: any = this.doc.querySelector('[tabindex=\'' + i + '\']');
          // if there is no focusable element in the window, then newTarget == e.target,
          // do a check here to avoid focus cycling
          if(this.isElementVisibleAndNotDisabled(newTarget) && (event.target != newTarget)) {
            newTarget.focus();
            this.firstTimeFocus = false;
            break;
          }
        }
      } else {
        for(let i = lastTabIndex - 1; i > firstTabIndex; i--) {
          const newTarget: any = this.doc.querySelector('[tabindex=\'' + i + '\']');
          // if there is no focusable element in the window, then newTarget == e.target,
          // do a check here to avoid focus cycling
          if(this.isElementVisibleAndNotDisabled(newTarget) && (event.target != newTarget)) {
            newTarget.focus();
            this.firstTimeFocus = false;
            break;
          }
        }
      }
    }

    lastElementFocused(event: Event) {
      const firstTabIndex = parseInt(this.doc.getElementById('tabStart').getAttribute('tabindex'), 10);
      const lastTabIndex = parseInt(this.doc.getElementById('tabStop').getAttribute('tabindex'), 10);
      for(let i = firstTabIndex + 1; i < lastTabIndex; i++) {
        const newTarget: any = this.doc.querySelector('[tabindex=\'' + i + '\']');
        // if there is no focusable element in the window, then newTarget == e.target,
        // do a check here to avoid focus cycling
        if(this.isElementVisibleAndNotDisabled(newTarget) && (event.target != newTarget)) {
          newTarget.focus();
          this.firstTimeFocus = false;
          break;
        }
      }
    }

    isElementVisibleAndNotDisabled(element): boolean {
      return (element.offsetWidth > 0 || element.offsetHeight > 0) && !element.disabled;
    }
  }
