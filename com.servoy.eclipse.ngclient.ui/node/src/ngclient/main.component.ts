import { Component, OnInit, ViewChild, ElementRef, AfterViewInit, Renderer2 } from '@angular/core';

import { ServoyService } from './servoy.service';
import { AllServiceService } from './allservices.service';
import { FormService } from './form.service';

@Component({
  selector: 'servoy-main',
  templateUrl: './main.component.html'
})

export class MainComponent implements OnInit, AfterViewInit {
  title = 'Servoy NGClient';
  isReconnecting: boolean = false;
  @ViewChild('mainBody') mainBody: ElementRef;

  constructor(private servoyService: ServoyService, 
          private allService: AllServiceService, 
          private formservice: FormService,
          private renderer: Renderer2) {
    this.servoyService.connect();
  }

  public get mainForm() {
    if (this.sessionProblem) return null;
    const mainForm = this.servoyService.getSolutionSettings().mainForm;
    if (mainForm && mainForm.name) return mainForm.name;
    return null;
  }

  public get navigatorForm() {
    if (this.sessionProblem) return null;
    const navigatorForm = this.servoyService.getSolutionSettings().navigatorForm;
    if (navigatorForm && navigatorForm.name &&
        navigatorForm.name.lastIndexOf('default_navigator_container.html') == -1)
        return navigatorForm.name;
    return null;
  }

  hasDefaultNavigator(): boolean {
    return this.mainForm && this.formservice.getFormCacheByName(this.mainForm.toString()).getComponent('svy_default_navigator') != null;
  }

  public get sessionProblem() {
    return this.servoyService.getSolutionSettings().sessionProblem;
  }

  public getNavigatorStyle() {
    const ltrOrientation = this.servoyService.getSolutionSettings().ltrOrientation;
    const orientationVar = ltrOrientation ? 'left' : 'right';
    const style = { 'position': 'absolute',
                    'top': '0px',
                    'bottom': '0px',
                    'width': this.servoyService.getSolutionSettings().navigatorForm.size.width + 'px'
                  }
    style[orientationVar] = '0px';
    return style;
  }
  public getFormStyle() {
    const ltrOrientation = this.servoyService.getSolutionSettings().ltrOrientation;
    const orientationVar1 = ltrOrientation ? 'right' : 'left';
    const orientationVar2 = ltrOrientation ? 'left' : 'right';
    const style = { 'position': 'absolute', 'top': '0px', 'bottom': '0px' }
    style[orientationVar1] = '0px';
    style[orientationVar2] = this.servoyService.getSolutionSettings().navigatorForm.size.width + 'px';
    return style;
  }
  
  ngOnInit() {
      this.servoyService.reconnectingEmitter.subscribe(isReconnecting => {
          this.isReconnecting = isReconnecting;
      })
  }
  
  ngAfterViewInit() {
      if (this.isReconnecting) {
          this.renderer.addClass(this.mainBody.nativeElement, 'svy-reconnecting');
      }
  }
}
