import { Component, OnDestroy, OnInit, ViewContainerRef, ChangeDetectionStrategy, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ServoyService } from './servoy.service';
import { AllServiceService } from './allservices.service';
import { FormService } from './form.service';
import { WebsocketService } from '../sablo/websocket.service';
import { LoadingIndicatorService } from '../sablo/util/loading-indicator/loading-indicator.service';
import { ServerDataService } from './services/serverdata.service';
import { I18NProvider } from './services/i18n_provider.service';
import { I18NListener, MainViewRefService, SabloTabseq } from '@servoy/public';
import { WindowRefService } from '@servoy/public';
import { DefaultNavigator } from '../servoycore/default-navigator/default-navigator';
import { SessionView } from '../servoycore/session-view/session-view';
import { LoadingIndicatorComponent } from '../sablo/util/loading-indicator/loading-indicator';
import { FormComponent } from './form/form_component.component';

@Component({
  selector: 'svy-main',
  templateUrl: './main.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [CommonModule, FormsModule, SabloTabseq, DefaultNavigator, SessionView, LoadingIndicatorComponent, FormComponent],
})
export class MainComponent implements OnInit, OnDestroy {
  title = 'Servoy NGClient';
  i18n_reconnecting_feedback!: string;
  formStyle: Record<string, string> = { position: 'absolute', top: '0px', bottom: '0px' };
  navigatorStyle: Record<string, string> = { position: 'absolute', top: '0px', bottom: '0px' };

  incudeAutoFillHack = !this.isSafariBrowser();

  private listener: I18NListener | null = null;

  private readonly servoyService = inject(ServoyService);
  private readonly i18nProvider = inject(I18NProvider);
  private readonly formservice = inject(FormService);
  readonly websocketService = inject(WebsocketService);
  readonly loadingIndicatorService = inject(LoadingIndicatorService);
  private readonly windowRef = inject(WindowRefService);

  constructor() {
    const allService = inject(AllServiceService);
    const serverData = inject(ServerDataService);
    const mainViewRefService = inject(MainViewRefService);
    const viewContainerRef = inject(ViewContainerRef);
    this.servoyService.connect();
    mainViewRefService.mainContainer = viewContainerRef;
    allService.init();
    serverData.init();
    (this.windowRef.nativeWindow as any)['executeInlineScript'] = (formname: any, script: any, params: any) => this.servoyService.executeInlineScript(formname, script, params);
  }

  public readonly mainForm = computed(() => {
    if (this.sessionProblem()) return null;
    const mainForm = this.servoyService.getSolutionSettings().mainForm();
    if (mainForm && mainForm.name) return mainForm.name;
    return null;
  });

  public readonly navigatorForm = computed(() => {
    if (this.sessionProblem()) return null;
    const navigatorForm = this.servoyService.getSolutionSettings().navigatorForm();
    if (navigatorForm && navigatorForm.name && navigatorForm.name.lastIndexOf('default_navigator_container.html') === -1) return navigatorForm.name;
    return null;
  });

  public readonly sessionProblem = computed(() => {
    return this.servoyService.getSolutionSettings().sessionProblem();
  });

  public ngOnDestroy(): void {
    this.listener!.destroy();
  }

  public ngOnInit() {
    this.listener = this.i18nProvider.listenForI18NMessages('servoy.ngclient.reconnecting').messages((val: any) => {
      this.i18n_reconnecting_feedback = val.get('servoy.ngclient.reconnecting');
    });
  }

  hasDefaultNavigator(): boolean {
    const name = this.mainForm();
    const cache = name ? this.formservice.getFormCacheByName(name.toString()) : null;
    return cache != null && cache.getComponent('svy_default_navigator') != null;
  }

  public getNavigatorStyle() {
    const ltrOrientation = this.servoyService.getSolutionSettings().ltrOrientation;
    const orientationVar1 = ltrOrientation ? 'left' : 'right';
    const orientationVar2 = ltrOrientation ? 'right' : 'left';

    this.navigatorStyle['width'] = this.servoyService.getSolutionSettings().navigatorForm().size.width + 'px';
    this.navigatorStyle[orientationVar1] = '0px';
    delete this.navigatorStyle[orientationVar2];
    return this.navigatorStyle;
  }

  public getFormStyle() {
    const ltrOrientation = this.servoyService.getSolutionSettings().ltrOrientation;
    const orientationVar1 = ltrOrientation ? 'right' : 'left';
    const orientationVar2 = ltrOrientation ? 'left' : 'right';
    this.formStyle[orientationVar1] = '0px';
    this.formStyle[orientationVar2] = this.servoyService.getSolutionSettings().navigatorForm().size.width + 'px';
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
