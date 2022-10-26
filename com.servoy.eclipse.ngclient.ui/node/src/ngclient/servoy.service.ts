import { Injectable } from '@angular/core';

import { WebsocketService } from '../sablo/websocket.service';
import { SabloService } from '../sablo/sablo.service';
import { ConverterService } from '../sablo/converter.service';
import { SpecTypesService, WindowRefService, LoggerFactory, SessionStorageService } from '@servoy/public';
import { SabloDeferHelper } from '../sablo/defer.service';

import { DateConverter } from './converters/date_converter';
import { JSONObjectConverter } from './converters/json_object_converter';
import { JSONArrayConverter } from './converters/json_array_converter';
import { ValuelistConverter } from './converters/valuelist_converter';
import { FoundsetConverter } from './converters/foundset_converter';
import { FoundsetLinkedConverter } from './converters/foundsetLinked_converter';
import { ViewportService } from './services/viewport.service';

import { IterableDiffers } from '@angular/core';


import { FormcomponentConverter } from './converters/formcomponent_converter';
import { ComponentConverter } from './converters/component_converter';
import { LocaleService } from './locale.service';
import { FormSettings } from './types';
import { ClientFunctionConverter } from './converters/clientfunction_converter';
import { ClientFunctionService } from './services/clientfunction.service';
import { DeveloperService } from './developer.service';
import { UIBlockerService } from './services/ui_blocker.service';

class UIProperties {
    private uiProperties: { [property: string]: any};

    constructor(private sessionStorageService: SessionStorageService) {
    }

    public getUIProperty(key: string) {
        let value = this.getUiProperties()[key];
        if (value === undefined) {
            value = null;
        }
        return value;
    }
    public setUIProperty(key: string, value: any) {
        const uiProps = this.getUiProperties();
        if (value == null) delete uiProps[key];
        else uiProps[key] = value;
        this.sessionStorageService.set('uiProperties', JSON.stringify(uiProps));
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

@Injectable({
  providedIn: 'root'
})
export class ServoyService {
    private solutionSettings: SolutionSettings = new SolutionSettings();
    private uiProperties: UIProperties;
    private uiBlockerService: UIBlockerService;

    private findModeShortCutCallback: any = null;

    constructor(private websocketService: WebsocketService,
        private sabloService: SabloService,
        private windowRefService: WindowRefService,
        private sessionStorageService: SessionStorageService,
        private localeService: LocaleService,
        private clientFunctionService: ClientFunctionService,
        converterService: ConverterService,
        specTypesService: SpecTypesService,
        sabloDeferHelper: SabloDeferHelper,
        iterableDiffers: IterableDiffers,
        logFactory: LoggerFactory,
        viewportService: ViewportService) {

        this.uiProperties = new UIProperties(sessionStorageService);
        const dateConverter = new DateConverter();
        this.uiBlockerService = new UIBlockerService(this);
        converterService.registerCustomPropertyHandler('svy_date', dateConverter);
        converterService.registerCustomPropertyHandler('Date', dateConverter);
        converterService.registerCustomPropertyHandler('JSON_obj', new JSONObjectConverter(converterService, specTypesService));
        converterService.registerCustomPropertyHandler('JSON_arr', new JSONArrayConverter(converterService, specTypesService, iterableDiffers));
        converterService.registerCustomPropertyHandler('valuelist', new ValuelistConverter(sabloService, sabloDeferHelper));
        converterService.registerCustomPropertyHandler('foundset',
            new FoundsetConverter(converterService, sabloService, sabloDeferHelper, viewportService, logFactory));
        converterService.registerCustomPropertyHandler('fsLinked',
            new FoundsetLinkedConverter(converterService, sabloService, viewportService, logFactory));
        converterService.registerCustomPropertyHandler('formcomponent', new FormcomponentConverter(converterService));
        converterService.registerCustomPropertyHandler('component', new ComponentConverter(converterService, viewportService, this.sabloService, logFactory, this.uiBlockerService));
        converterService.registerCustomPropertyHandler('clientfunction', new ClientFunctionConverter(this.windowRefService));
    }

    public connect() {
        // maybe do this with defer ($q)
        const solName = this.websocketService.getURLParameter('s');
        if (!solName) this.solutionSettings.solutionName = /.*\/([\$\w]+)\/.*/.exec(this.websocketService.getPathname())[1];
        else this.solutionSettings.solutionName = solName;
        this.solutionSettings.windowName = this.sabloService.getWindownr();
        let recordingPrefix: string;
        if (this.windowRefService.nativeWindow.location.search.indexOf('svy_record=true') > -1) {
            recordingPrefix = '/recording/websocket';

        }
        const wsSession = this.sabloService.connect('/solution/' + this.solutionSettings.solutionName,
            { solution: this.solutionSettings.solutionName, clienttype: 2 }, recordingPrefix);
        // TODO find mode and anchors handling (anchors should be handles completely at the server side,
        // css positioning should go over the line)
        wsSession.onMessageObject((msg: {clientnr?: number; windownr?: string}) => {

            if (msg.clientnr && recordingPrefix) {
                const btn = this.windowRefService.nativeWindow.document.createElement('A')  as HTMLAnchorElement;      // Create a <button> element
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

        wsSession.onopen(() => {
            // update the main app window with the right size
            wsSession.callService('$windowService', 'resize',
                { size: { width: this.windowRefService.nativeWindow.innerWidth, height: this.windowRefService.nativeWindow.innerHeight } }, true);
            // set the correct locale, first test if it is set in the sessionstorage
            let locale = this.sessionStorageService.get('locale');
            if (locale) {
                const array = locale.split('-');
                this.localeService.setLocale(array[0], array[1], true);
            } else {
                locale = this.sabloService.getLocale();
                this.localeService.setLocale(locale.language, locale.country, true);
            }
        });
    }

    public getSolutionSettings(): SolutionSettings {
        return this.solutionSettings;
    }

    public getUIProperties(): UIProperties {
        return this.uiProperties;
    }

    public getUIBlockerService(): UIBlockerService {
        return this.uiBlockerService;
    }

    public executeInlineScript<T>(formname: string, script: string, params: any[]): Promise<T> {
        return this.sabloService.callService('formService', 'executeInlineScript',
            { formname, script, params }, false);
    }

    public loaded(): Promise<any> {
        return Promise.all([this.localeService.isLoaded(), this.clientFunctionService.waitForLoading()]);
    }

    public setFindMode(formName: string, findmode: boolean) {
        if (findmode && this.findModeShortCutCallback == null) {
            this.findModeShortCutCallback = (event: KeyboardEvent) => {
                // perform find on ENTER
                if (event.keyCode === 13) {
                    this.sabloService.callService('formService', 'performFind', { formname: formName, clear: true, reduce: true, showDialogOnNoResults: true }, true);
                }
            };
            this.windowRefService.nativeWindow.addEventListener('keyup', this.findModeShortCutCallback);
        } else if (findmode === false && this.findModeShortCutCallback !== null) {
            this.windowRefService.nativeWindow.removeEventListener('keyup', this.findModeShortCutCallback);
            this.findModeShortCutCallback = null;
        }
    }
}

export class SessionProblem {
    public viewUrl: string;
    public redirectUrl?: string;
    public redirectTimeout?: number;
    public stack?: string;
}


