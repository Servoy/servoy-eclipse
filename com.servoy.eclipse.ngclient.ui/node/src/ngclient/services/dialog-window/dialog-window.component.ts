import { Component, Inject } from '@angular/core';
import { SabloService } from '../../../sablo/sablo.service';
import { SvyWindow } from '../window.service';
import { DOCUMENT } from '@angular/common';

@Component({
    selector: 'app-dialog-window',
    templateUrl: './dialog-window.component.html',
    styleUrls: ['./dialog-window.component.css']
  })
  export class DialogWindowComponent {

    window: SvyWindow;

    constructor(private sabloService: SabloService, @Inject(DOCUMENT) private document: any) {
    }

    setWindow(window: SvyWindow) {
      this.window = window;
    }

    getFormName(): string {
        return this.window.form.name;
    }

    getNavigatorFormName(): string {
      return (this.window.navigatorForm && this.window.navigatorForm.name && this.window.navigatorForm.name.lastIndexOf('default_navigator_container.html') == -1) ? this.window.navigatorForm.name : null;
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
      return this.window.transparent ? 'transparent' : 'inherit';
    }

    getCSSClassName() {
      return this.window.cssClassName;
    }

    cancel() {
      this.sabloService.callService( '$windowService', 'windowClosing', { window: this.window.name }, false );
    }

    firstElementFocused(event) {
      const tabIndex = parseInt(this.document.getElementById('tabStop').getAttribute('tabindex'));
      const newTarget: any = document.querySelector('[tabindex=\'' + ( tabIndex - 1 ) + '\']');
      // if there is no focusable element in the window, then newTarget == e.target,
      // do a check here to avoid focus cycling
      if(event.target != newTarget) {
        newTarget.focus();
      }
    }

    lastElementFocused(event) {
      const newTarget: any = document.querySelector('[tabindex=\'2\']');
      // if there is no focusable element in the window, then newTarget == e.target,
      // do a check here to avoid focus cycling
      if(event.target != newTarget) {
        newTarget.focus();
      }
    }
  }
