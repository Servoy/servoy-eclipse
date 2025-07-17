import { Component, OnDestroy, OnInit, ViewContainerRef } from '@angular/core';

import { ServoyService } from './servoy.service';
import { AllServiceService } from './allservices.service';
import { FormService } from './form.service';
import { WebsocketService } from '../sablo/websocket.service';
import { LoadingIndicatorService } from '../sablo/util/loading-indicator/loading-indicator.service';
import { ServerDataService } from './services/serverdata.service';
import { I18NProvider } from './services/i18n_provider.service';
import { I18NListener, MainViewRefService } from '@servoy/public';
import { WindowRefService } from '@servoy/public';

@Component({
    selector: 'svy-main',
    templateUrl: './main.component.html',
    standalone: false
})

export class MainComponent implements OnInit, OnDestroy {
  title = 'Servoy NGClient';
  i18n_reconnecting_feedback: string;
  formStyle = { position: 'absolute', top: '0px', bottom: '0px' };
  navigatorStyle = { position: 'absolute', top: '0px', bottom: '0px' };
  
  incudeAutoFillHack = !this.isSafariBrowser();

  private  listener: I18NListener = null;

  constructor(private servoyService: ServoyService,
          private i18nProvider: I18NProvider,
          private formservice: FormService,
          public websocketService: WebsocketService,
          public loadingIndicatorService: LoadingIndicatorService,
          allService: AllServiceService,
          serverData: ServerDataService,
          mainViewRefService: MainViewRefService,
          viewContainerRef: ViewContainerRef,
          private windowRef: WindowRefService) {
    this.servoyService.connect();
    mainViewRefService.mainContainer = viewContainerRef;
    allService.init();
    serverData.init();
    this.windowRef.nativeWindow['executeInlineScript'] = (formname, script, params) => this.servoyService.executeInlineScript(formname,script,params);
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
        navigatorForm.name.lastIndexOf('default_navigator_container.html') === -1)
        return navigatorForm.name;
    return null;
  }

  public get sessionProblem() {
    return this.servoyService.getSolutionSettings().sessionProblem;
  }

  public ngOnDestroy(): void {
    this.listener.destroy();
    }

  public ngOnInit() {
      this.listener = this.i18nProvider.listenForI18NMessages(
              'servoy.ngclient.reconnecting').messages((val)=> {
                this.i18n_reconnecting_feedback = val.get('servoy.ngclient.reconnecting');
      });
  }

  hasDefaultNavigator(): boolean {
    const cache = this.mainForm? this.formservice.getFormCacheByName(this.mainForm.toString()): null;
    return cache && cache.getComponent('svy_default_navigator') != null;
  }

  public getNavigatorStyle() {
    const ltrOrientation = this.servoyService.getSolutionSettings().ltrOrientation;
    const orientationVar1 = ltrOrientation ? 'left' : 'right';
    const orientationVar2 = ltrOrientation ? 'right' : 'left';

    this.navigatorStyle['width'] = this.servoyService.getSolutionSettings().navigatorForm.size.width + 'px';
    this.navigatorStyle[orientationVar1] = '0px';
    delete this.navigatorStyle[orientationVar2];
    return this.navigatorStyle;
  }

  public getFormStyle() {
    const ltrOrientation = this.servoyService.getSolutionSettings().ltrOrientation;
    const orientationVar1 = ltrOrientation ? 'right' : 'left';
    const orientationVar2 = ltrOrientation ? 'left' : 'right';
    this.formStyle[orientationVar1] = '0px';
    this.formStyle[orientationVar2] = this.servoyService.getSolutionSettings().navigatorForm.size.width + 'px';
    return this.formStyle;
  }
  
  private isSafariBrowser(): boolean {
    const userAgent = navigator.userAgent.toLowerCase();

    // Check for "Safari" in the user agent
    const isSafari = userAgent.includes('safari');

    // Check for the absence of "Chrome", "Chromium", or "CriOS" (Chrome on iOS)
    // This is crucial because Chrome's user agent often contains "Safari"
    const isChrome = userAgent.includes('chrome') || userAgent.includes('crios');
    const isChromium = userAgent.includes('chromium');
    const isFirefox = userAgent.includes('firefox');
    const isEdge = userAgent.includes('edge'); // Edge also includes Safari/Chrome parts

    // If it's Safari and NOT Chrome/Chromium/Firefox/Edge, it's likely Safari
    return isSafari && !isChrome && !isChromium && !isFirefox && !isEdge;
  }
}
