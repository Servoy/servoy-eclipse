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
      const tabIndex = parseInt(this.doc.getElementById('tabStop').getAttribute('tabindex'), 10);
      const newTarget: any = this.doc.querySelector('[tabindex=\'' + ( tabIndex - 1 ) + '\']');
      // if there is no focusable element in the window, then newTarget == e.target,
      // do a check here to avoid focus cycling
      if(event.target !== newTarget) {
        newTarget.focus();
      }
    }

    lastElementFocused(event: Event) {
      const newTarget: any = this.doc.querySelector('[tabindex=\'2\']');
      // if there is no focusable element in the window, then newTarget == e.target,
      // do a check here to avoid focus cycling
      if(event.target !== newTarget) {
        newTarget.focus();
      }
    }
  }
