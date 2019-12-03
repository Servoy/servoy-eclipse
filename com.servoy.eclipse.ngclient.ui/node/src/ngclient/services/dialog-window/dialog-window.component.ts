import { Component } from '@angular/core';
import { SabloService } from '../../../sablo/sablo.service';
import { SvyWindow } from '../window.service';

@Component({
    selector: 'app-dialog-window',
    templateUrl: './dialog-window.component.html',
    styleUrls: ['./dialog-window.component.css']
  })
  export class DialogWindowComponent {

    window: SvyWindow;

    constructor(private sabloService: SabloService) {
    }

    setWindow(window: SvyWindow) {
      this.window = window;
    }

    getFormName(): string {
        return this.window.form.name;
    }

    getNavigatorFormName(): string {
      return (this.window.navigatorForm && this.window.navigatorForm.name && this.window.navigatorForm.name.lastIndexOf("default_navigator_container.html") == -1) ? this.window.navigatorForm.name : null;
    }

    hasDefaultNavigator() : boolean{
        return this.window.navigatorForm && this.window.navigatorForm.name && this.window.navigatorForm.name.lastIndexOf("default_navigator_container.html") >= 0;
    }
    
    isUndecorated(): boolean {
      return this.window.undecorated || ( this.window.opacity < 1 )
    }

    getOpacity(): number {
      return this.window.opacity;
    }

    getTitle(): string {
      return this.window.title;
    }

    getBackgroundColor(): string {
      return this.window.transparent ? "transparent" : "inherit";
    }

    getCSSClassName() {
      return this.window.cssClassName;
    }

    cancel() {
      this.sabloService.callService( "$windowService", "windowClosing", { window: this.window.name }, false )
    }
  }