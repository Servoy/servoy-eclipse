import { Injectable } from '@angular/core';
import { registerLocaleData } from '@angular/common';

import { AllServiceService } from './allservices.service';
import { WebsocketService } from '../sablo/websocket.service';
import { SabloService } from '../sablo/sablo.service';
import { ConverterService } from '../sablo/converter.service'
import { WindowRefService } from '../sablo/util/windowref.service';
import { LoggerService, LoggerFactory } from '../sablo/logger.service'
import { SabloDeferHelper } from '../sablo/defer.service';
import { Deferred } from '../sablo/util/deferred';

import { SessionStorageService } from 'angular-web-storage';

import { DateConverter } from './converters/date_converter'
import { JSONObjectConverter } from './converters/json_object_converter'
import { JSONArrayConverter } from './converters/json_array_converter'
import { ValuelistConverter } from './converters/valuelist_converter'
import { FoundsetConverter } from './converters/foundset_converter'
import { FoundsetLinkedConverter } from './converters/foundsetLinked_converter'
import { I18NProvider } from './services/i18n_provider.service'
import { ViewportService } from './services/viewport.service'

import { IterableDiffers, IterableDiffer } from '@angular/core';

import { SpecTypesService } from '../sablo/spectypes.service'

import { Subject } from 'rxjs';

import * as numeral from 'numeral';
import 'numeral/locales';

import * as moment from 'moment';
import 'moment/min/locales.min';

class UIProperties {
  private uiProperties;

  constructor(private sessionStorageService: SessionStorageService) {
  }

  private getUiProperties() {
    if (!this.uiProperties) {
      const json = this.sessionStorageService.get('uiProperties');
      if (json) {
        this.uiProperties = JSON.parse(json);
      } else {
        this.uiProperties = {};
      }
    }
    return this.uiProperties;
  }

  public getUIProperty(key) {
    let value = this.getUiProperties()[key];
    if (value === undefined) {
      value = null;
    }
    return value;
  }
  public setUIProperty(key, value) {
    const uiProps = this.getUiProperties();
    if (value == null) delete uiProps[key];
    else uiProps[key] = value;
    this.sessionStorageService.set('uiProperties', JSON.stringify(uiProps))
  }
}

class SolutionSettings {
  public solutionName: string;
  public windowName: string;
  public enableAnchoring = true;
  public ltrOrientation = true;
  public mainForm: FormSettings;
  public navigatorForm: FormSettings;
  public sessionProblem: SessionProblem;
}

@Injectable()
export class ServoyService {
  private solutionSettings: SolutionSettings = new SolutionSettings();
  private uiProperties: UIProperties;

  private findModeShortCutCallback: any = null;
  private log: LoggerService;
  private loadedLocale: Deferred<any>;
  reconnectingEmitter = new Subject<boolean>();

  constructor(private websocketService: WebsocketService,
    private sabloService: SabloService,
    private windowRefService: WindowRefService,
    private sessionStorageService: SessionStorageService,
    private i18nProvider: I18NProvider,
    converterService: ConverterService,
    specTypesService: SpecTypesService,
    sabloDeferHelper: SabloDeferHelper,
    iterableDiffers: IterableDiffers,
    private logFactory: LoggerFactory,
    private viewportService: ViewportService) {

    this.log = logFactory.getLogger('ServoyService');
    this.uiProperties = new UIProperties(sessionStorageService)
    const dateConverter = new DateConverter();
    converterService.registerCustomPropertyHandler('svy_date', dateConverter);
    converterService.registerCustomPropertyHandler('Date', dateConverter);
    converterService.registerCustomPropertyHandler('JSON_obj', new JSONObjectConverter(converterService, specTypesService));
    converterService.registerCustomPropertyHandler('JSON_arr', new JSONArrayConverter(converterService, specTypesService, iterableDiffers));
    converterService.registerCustomPropertyHandler('valuelist', new ValuelistConverter(sabloService, sabloDeferHelper));
    converterService.registerCustomPropertyHandler('foundset',
      new FoundsetConverter(converterService, sabloService, sabloDeferHelper, viewportService, logFactory));
    converterService.registerCustomPropertyHandler('fsLinked',
      new FoundsetLinkedConverter(converterService, sabloService, sabloDeferHelper, viewportService, logFactory));
  }

  public connect() {
    // maybe do this with defer ($q)
    const solName = this.websocketService.getURLParameter('s');
    if (!solName) this.solutionSettings.solutionName = /.*\/([\$\w]+)\/.*/.exec(this.websocketService.getPathname())[1];
    else this.solutionSettings.solutionName = solName;
    this.solutionSettings.windowName = this.sabloService.getWindownr();
    let recordingPrefix;
    if (this.windowRefService.nativeWindow.location.search.indexOf('svy_record=true') > -1) {
      recordingPrefix = '/recording/websocket';

    }
    const wsSession = this.sabloService.connect('/solution/' + this.solutionSettings.solutionName,
                      { solution: this.solutionSettings.solutionName, clienttype: 2 }, recordingPrefix)
    // TODO find mode and anchors handling (anchors should be handles completely at the server side,
    // css positioning should go over the line)
    wsSession.onMessageObject((msg, conversionInfo) => {

      if (msg.clientnr && recordingPrefix) {
        const btn = <HTMLAnchorElement>this.windowRefService.nativeWindow.document.createElement('A');        // Create a <button> element
        btn.href = 'solutions/' + msg.clientnr + '.recording';
        btn.target = '_blank';
        btn.style.position = 'absolute';
        btn.style.right = '0px';
        btn.style.bottom = '0px';
        const t = this.windowRefService.nativeWindow.document.createTextNode('Download');
        btn.appendChild(t);                                // Append the text to <button>
        this.windowRefService.nativeWindow.document.body.appendChild(btn);
      }
      if (msg.windownr) {
        this.solutionSettings.windowName = msg.windownr;
      }
    });

    wsSession.onopen((evt) => {
      // update the main app window with the right size
      wsSession.callService('$windowService', 'resize',
        { size: { width: this.windowRefService.nativeWindow.innerWidth, height: this.windowRefService.nativeWindow.innerHeight } }, true);
      // set the correct locale, first test if it is set in the sessionstorage
      let locale = this.sessionStorageService.get('locale');
      if (locale) {
        const array = locale.split('-');
        this.setLocale(array[0], array[1], true);
      } else {
        locale = this.sabloService.getLocale();
        this.setLocale(locale.language, locale.country, true);
      }
    });
  }

  public getSolutionSettings(): SolutionSettings {
    return this.solutionSettings;
  }

  public getUIProperties(): UIProperties {
    return this.uiProperties;
  }

  public executeInlineScript(formname: string, script: string, params: any[]) {
    return this.sabloService.callService('formService', 'executeInlineScript',
                                          { 'formname': formname, 'script': script, 'params': params }, false);
  }


  public setLocale(language, country, initializing?) {
    // TODO angular $translate and our i18n service
    //            $translate.refresh();
    this.loadedLocale = new Deferred<any>();
    this.setAngularLocale(language, country).then(localeId => {
      this.i18nProvider.flush();
      this.sabloService.setLocale({ language: language, country: country, full: localeId });
      if (!initializing) {
        // in the session storage we always have the value set via applicationService.setLocale
        this.sessionStorageService.set('locale', language + '-' + country);
      }

      this.loadedLocale.resolve(localeId);
    }, () => {
      this.loadedLocale.reject('Could not set Locale because angular locale could not be loaded.');
    });
    this.setNumeralAndMomentLocale(language, country);
  }

  public loaded(): Promise<any> {
    return this.loadedLocale.promise;
  }

  private setNumeralAndMomentLocale(language, country) {
    try {
      numeral.localeData((language + '-' + country).toLowerCase());
      numeral.locale((language + '-' + country).toLowerCase());
    } catch (e) {
      try {
        numeral.localeData(language + '-' + country);
        numeral.locale(language + '-' + country);
      } catch (e2) {
        try {
          numeral.localeData((country + '-' + country).toLowerCase());
          numeral.locale((country + '-' + country).toLowerCase());
        } catch (e3) {
          try {
            numeral.localeData(country.toLowerCase());
            numeral.locale(country.toLowerCase());
          } catch (e4) {
            try {
              // try it with just the language part
              numeral.localeData(language);
              numeral.locale(language);
            } catch (e5) {
              try {
                // try it with just the language part but lowercase
                numeral.localeData(language.toLowerCase());
                numeral.locale(language.toLowerCase());
              } catch (e6) {
                try {
                  // try to duplicate the language in case it's only defined like that
                  // nl-nl for example is defined but browser only says 'nl'
                  // (this won't work for all languages for example "en-en" I don't think even exists)
                  numeral.localeData(language.toLowerCase() + '-' + language.toLowerCase());
                  numeral.locale(language.toLowerCase() + '-' + language.toLowerCase());
                } catch (e7) {
                  // we can't find a suitable locale defined in locales.js; get the needed things from server (Java knows more locales)
                  // and create the locate info from that
                  const promise = this.sabloService.callService('i18nService', 'generateLocaleForNumeralJS',
                                    country ? { 'language': language, 'country': country } : { 'language': language }, false);
                  // TODO should we always do this (get stuff from server side java) instead of trying first to rely on numeral.js
                  // and locales.js provided langs?
                  const numeralLanguage = language + (country ? '-' + country : '');
                  promise.then(numeralLocaleInfo => {
                    this.log.debug(this.log.buildMessage(() => ('Locale \'' + numeralLanguage + '\' not found in client js lib, but it was constructed based on server Java locale-specific information: ' + JSON.stringify(numeralLocaleInfo))));
                    numeralLocaleInfo.ordinal = function (number) {
                      return '.';
                    };
                    numeral.register('locale', numeralLanguage, numeralLocaleInfo);
                    numeral.locale(numeralLanguage);
                    moment.locale(numeral.locale())
                  }, (reason) => {
                    this.log.warn(this.log.buildMessage(() => ('Cannot properly handle locale \'' + numeralLanguage + '\'. It is not available in js libs and it could not be loaded from server...')));
                  });
                }
              }
            }
          }
        }
      }
    }
    moment.locale(numeral.locale())
  }

  private setAngularLocale(language: string, country: string) {
    // angular locales are either <language lowercase> or <language lowercase> - <country uppercase>
    const localeId = country !== undefined && country.length > 0 ?
                      language.toLowerCase() + '-' + country.toUpperCase() : language.toLowerCase();
    return new Promise((resolve, reject) => {
      import(`@angular/common/locales/${localeId}.js`).then(
        module => {
          registerLocaleData(module.default, localeId);
          resolve(localeId);
        },
        () => {
          import(`@angular/common/locales/${language.toLowerCase()}.js`).then(module => {
            registerLocaleData(module.default, localeId.split('-')[0]);
            resolve(language.toLowerCase());
          }, reject);
        });
    });
  }


  public setFindMode(formName: string, findmode: boolean) {
    if (findmode && this.findModeShortCutCallback == null) {
      this.findModeShortCutCallback = (event: KeyboardEvent) => {
        // perform find on ENTER
        if (event.keyCode === 13) {
          this.sabloService.callService('formService', 'performFind', { 'formname': formName, 'clear': true, 'reduce': true, 'showDialogOnNoResults': true }, true);
        }
      }
      this.windowRefService.nativeWindow.addEventListener('keyup', this.findModeShortCutCallback);
    } else if (findmode == false && this.findModeShortCutCallback != null) {
      this.windowRefService.nativeWindow.removeEventListener('keyup', this.findModeShortCutCallback);
      this.findModeShortCutCallback = null;
    }
  }
}



class AnchorConstants {
  public static readonly NORTH = 1;
  public static readonly EAST = 2;
  public static readonly SOUTH = 4;
  public static readonly WEST = 8;
}

export class FormSettings {
  public name: String;
  public size: { width: number, height: number };
}

export class SessionProblem {
  public viewUrl: string;
  public redirectUrl?: string;
  public redirectTimeout?: number;
  public stack?: string;
}


