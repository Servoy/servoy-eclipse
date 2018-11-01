import { Injectable } from '@angular/core';
import { registerLocaleData } from '@angular/common';

import { AllServiceService } from './allservices.service';
import { WebsocketService } from '../sablo/websocket.service';
import { SabloService } from '../sablo/sablo.service';
import { ConverterService } from '../sablo/converter.service'
import { WindowRefService } from '../sablo/util/windowref.service';
import { LoggerService, LoggerFactory  } from '../sablo/logger.service'
import { SabloDeferHelper} from '../sablo/defer.service';
import { Deferred } from '../sablo/util/deferred';

import { SessionStorageService } from 'angular-web-storage';

import { DateConverter } from './converters/date_converter'
import { JSONObjectConverter } from './converters/json_object_converter'
import { JSONArrayConverter } from './converters/json_array_converter'
import { ValuelistConverter } from './converters/valuelist_converter'
import {I18NProvider} from './services/i18n_provider.service'

import { IterableDiffers, IterableDiffer } from '@angular/core';

import { SpecTypesService } from '../sablo/spectypes.service'

import * as numeral from 'numeral';
import  'numeral/locales'; 

import * as moment from 'moment';
import  'moment/min/locales.min'; 

@Injectable()
export class ServoyService {
    private solutionSettings: SolutionSettings = new SolutionSettings();
    private uiProperties: UIProperties;

    private findModeShortCutAdded = false;
    private log: LoggerService;
    private loadedLocale: Deferred<any>;

    constructor( private websocketService: WebsocketService,
            private sabloService: SabloService,
            private windowRefService: WindowRefService,
            private sessionStorageService: SessionStorageService,
            private i18nProvider:I18NProvider,
            converterService: ConverterService,
            specTypesService: SpecTypesService,
            sabloDeferHelper: SabloDeferHelper,
            iterableDiffers: IterableDiffers,
            private logFactory: LoggerFactory) {

        this.log = logFactory.getLogger("ServoyService");
        this.uiProperties = new UIProperties( sessionStorageService )
        const dateConverter = new DateConverter();
        converterService.registerCustomPropertyHandler( "svy_date", dateConverter );
        converterService.registerCustomPropertyHandler( "Date", dateConverter );
        converterService.registerCustomPropertyHandler( "JSON_obj", new JSONObjectConverter( converterService, specTypesService ) );
        converterService.registerCustomPropertyHandler( "JSON_arr", new JSONArrayConverter( converterService, specTypesService, iterableDiffers ) );
        converterService.registerCustomPropertyHandler( "valuelist", new ValuelistConverter( sabloService, sabloDeferHelper) );
    }

    public connect() {
        // maybe do this with defer ($q)
        var solName = this.websocketService.getURLParameter( 's' );
        if ( !solName ) this.solutionSettings.solutionName = /.*\/([\$\w]+)\/.*/.exec( this.websocketService.getPathname() )[1];
        else this.solutionSettings.solutionName = solName;
        this.solutionSettings.windowName = this.sabloService.getWindowId();
        var recordingPrefix;
        if ( this.windowRefService.nativeWindow.location.search.indexOf( "svy_record=true" ) > -1 ) {
            recordingPrefix = "/recording/websocket";

        }
        var wsSession = this.sabloService.connect( '/solution/' + this.solutionSettings.solutionName, { solution: this.solutionSettings.solutionName, clienttype: 2 }, recordingPrefix )
        // TODO find mode and anchors handling (anchors should be handles completely at the server side, css positioning should go over the line)
        wsSession.onMessageObject(( msg, conversionInfo ) => {

            if ( msg.sessionid && recordingPrefix ) {
                var btn = <HTMLAnchorElement>this.windowRefService.nativeWindow.document.createElement( "A" );        // Create a <button> element
                btn.href = "solutions/" + msg.sessionid + ".recording";
                btn.target = "_blank";
                btn.style.position = "absolute";
                btn.style.right = "0px";
                btn.style.bottom = "0px";
                var t = this.windowRefService.nativeWindow.document.createTextNode( "Download" );
                btn.appendChild( t );                                // Append the text to <button>
                this.windowRefService.nativeWindow.document.body.appendChild( btn );
            }
            if ( msg.windowid ) {
                this.solutionSettings.windowName = msg.windowid;
            }
        } );

        wsSession.onopen(( evt ) => {
            // update the main app window with the right size
            wsSession.callService( "$windowService", "resize", { size: { width: this.windowRefService.nativeWindow.innerWidth, height: this.windowRefService.nativeWindow.innerHeight } }, true );
            // set the correct locale, first test if it is set in the sessionstorage
            let locale = this.sessionStorageService.get("locale");
            if (locale) {
                const array = locale.split('-');
                this.setLocale(array[0], array[1] , true);
            }
            else {
                locale = this.sabloService.getLocale();
                this.setLocale(locale.language, locale.country, true);
            }
        } );
    }

    public getSolutionSettings(): SolutionSettings {
        return this.solutionSettings;
    }

    public getUIProperties(): UIProperties {
        return this.uiProperties;
    }
    
    public setLocale(language, country, initializing?) {
       // TODO angular $translate and our i18n service
//            $translate.refresh();
        this.loadedLocale = new Deferred<any>();
        this.setAngularLocale(language, country).then( localeId => {     
            this.i18nProvider.flush();
            this.sabloService.setLocale({ language : language, country : country , full: localeId});
            if (!initializing)
            {
                //in the session storage we always have the value set via applicationService.setLocale
                this.sessionStorageService.set("locale", language + '-' + country);
            }
       
            this.loadedLocale.resolve(localeId);
        }, () => { 
            this.loadedLocale.reject("Could not set Locale because angular locale could not be loaded.");
        });
        this.setNumeralAndMomentLocale(language, country);
    }
    
    public loaded() : Promise<any> {
        return this.loadedLocale.promise;
    }
    
    private setNumeralAndMomentLocale(language, country)
    {
        try{
            numeral.localeData((language + '-' + country).toLowerCase());
            numeral.locale((language + '-' + country).toLowerCase());
        } catch(e) {
            try {
                numeral.localeData(language + '-' + country);
                numeral.locale(language + '-' + country);
            } catch(e2) {
                try {
                    //try it with just the language part
                    numeral.localeData(language);
                    numeral.locale(language);
                } catch(e3) {
                    try {
                        //try it with just the language part but lowercase
                        numeral.localeData(language.toLowerCase());
                        numeral.locale(language.toLowerCase());
                    } catch(e4) {
                        try {
                            //try to duplicate the language in case it's only defined like that
                            numeral.localeData(language.toLowerCase() + "-" + language.toLowerCase()); // nl-nl for example is defined but browser only says 'nl' (this won't work for all languages for example "en-en" I don't think even exists)
                            numeral.locale(language.toLowerCase() + "-" + language.toLowerCase()); 
                        } catch(e5) {
                            // we can't find a suitable locale defined in locales.js; get the needed things from server (Java knows more locales)
                            // and create the locate info from that
                            var promise = this.sabloService.callService("i18nService", "generateLocaleForNumeralJS", country ? {'language' : language, 'country' : country} : {'language' : language}, false);
                            // TODO should we always do this (get stuff from server side java) instead of trying first to rely on numeral.js and locales.js provided langs?
                            var numeralLanguage = language + (country ? '-' + country : "");
                            promise.then(numeralLocaleInfo => {
                              this.log.debug(this.log.buildMessage(() => ("Locale '" + numeralLanguage + "' not found in client js lib, but it was constructed based on server Java locale-specific information: " + JSON.stringify(numeralLocaleInfo))));
                                numeralLocaleInfo.ordinal = function (number) {
                                    return ".";
                                };
                                numeral.register('locale',numeralLanguage,numeralLocaleInfo);
                                numeral.locale(numeralLanguage);
                                moment.locale( numeral.locale() )
                            }, (reason) => {
                                this.log.warn(this.log.buildMessage(() => ("Cannot properly handle locale '" + numeralLanguage + "'. It is not available in js libs and it could not be loaded from server...")));
                            });
                        }
                    }
                }
            }
        }
        moment.locale( numeral.locale())
    }
    
    private setAngularLocale(language: string, country: string){
       //angular locales are either <language lowercase> or <language lowercase> - <country uppercase> 
       var localeId =  country !== undefined && country.length > 0 ? language.toLowerCase() +"-"+country.toUpperCase() : language.toLowerCase();  
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
    

    private setFindMode( beanData ) {
        if ( beanData['findmode'] ) {
            if ( this.windowRefService.nativeWindow['shortcut'].all_shortcuts['ENTER'] === undefined ) {
                this.findModeShortCutAdded = true;

                this.windowRefService.nativeWindow['shortcut'].add( 'ENTER', this.performFind );
            }
        }
        else if ( beanData['findmode'] == false && this.findModeShortCutAdded ) {
            this.findModeShortCutAdded = false;
            this.windowRefService.nativeWindow['shortcut'].remove( 'ENTER' );
        }
    }

    private performFind( event ) {
        // TODO this was:  angular.element( event.srcElement ? event.srcElement : event.target );
        var element = event.srcElement ? event.srcElement : event.target
        // TODO this whole looking of ng-model and servoy api from the attribute...;
        //        if ( element && element.attr( 'ng-model' ) ) {
        //            var dataproviderString = element.attr( 'ng-model' );
        //            var index = dataproviderString.indexOf( '.' );
        //            if ( index > 0 ) {
        //                var modelString = dataproviderString.substring( 0, index );
        //                var propertyname = dataproviderString.substring( index + 1 );
        //                var svyServoyApi = $utils.findAttribute( element, element.scope(), "svy-servoyApi" );
        //                if ( svyServoyApi && svyServoyApi.apply ) {
        //                    svyServoyApi.apply( propertyname );
        //                }
        //            }
        //        }
        //
        //        this.sabloService.callService( "formService", "performFind", { 'formname': formname, 'clear': true, 'reduce': true, 'showDialogOnNoResults': true }, true );
    }
}

class UIProperties {
    private uiProperties;

    constructor( private sessionStorageService: SessionStorageService ) {
    }

    private getUiProperties() {
        if ( !this.uiProperties ) {
            var json = this.sessionStorageService.get( "uiProperties" );
            if ( json ) {
                this.uiProperties = JSON.parse( json );
            } else {
                this.uiProperties = {};
            }
        }
        return this.uiProperties;
    }

    public getUIProperty( key ) {
        var value = this.getUiProperties()[key];
        if ( value === undefined ) {
            value = null;
        }
        return value;
    }
    public setUIProperty( key, value ) {
        var uiProps = this.getUiProperties();
        if ( value == null ) delete uiProps[key];
        else uiProps[key] = value;
        this.sessionStorageService.set( "uiProperties", JSON.stringify( uiProps ) )
    }
}

class SolutionSettings {
    public solutionName: string;
    public windowName: string;
    public enableAnchoring: boolean = true;
    public ltrOrientation: boolean = true;
    public solutionTitle = "";
    public mainForm: FormSettings;
    public navigatorForm: FormSettings;
    public styleSheetPaths = [];
}

class AnchorConstants {
    public static readonly NORTH = 1;
    public static readonly EAST = 2;
    public static readonly SOUTH = 4;
    public static readonly WEST = 8;
}

class FormSettings {
    public name: String;
    public size: { width: number, height: number };
}