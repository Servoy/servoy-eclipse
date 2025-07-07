import { Injectable, Inject } from '@angular/core';
import { SabloService } from '../sablo/sablo.service';
import { Deferred, SessionStorageService, LoggerFactory, LoggerService, Locale } from '@servoy/public';
import { registerLocaleData } from '@angular/common';
import { DOCUMENT } from '@angular/common';
import { environment as env} from '../environments/environment';

import numbro from 'numbro';
import { Settings } from 'luxon';

import { I18NProvider } from './services/i18n_provider.service';

@Injectable({
  providedIn: 'root'
})
export class LocaleService {
    private locale = 'en';
    private loadedLocale: Deferred<any>;

    private readonly localeMap = { en: 'en-US' };
    private readonly log: LoggerService;

    constructor(private sabloService: SabloService,
        private i18nProvider: I18NProvider,
        private sessionStorageService: SessionStorageService,
        logFactory: LoggerFactory,
        @Inject(DOCUMENT) private doc: Document) {
            this.log = logFactory.getLogger('LocaleService');
    }

    public isLoaded(): Promise<any> {
        return this.loadedLocale.promise;
    }

    public getLocale(): string {
        return this.locale;
    }

    public getLocaleObject(): Locale {
        return this.sabloService.getLocale();;
    }

    public setLocale(language: string, country: string, initializing?: boolean) {
        // TODO angular $translate and our i18n service
        //            $translate.refresh();
        this.loadedLocale = new Deferred<any>();
        this.setAngularLocale(language, country).then(localeId => {
            this.i18nProvider.flush();
            const full = language + (country ? '-' + country.toUpperCase() : '');
            this.sabloService.setLocale({ language, country, full });
            if (!initializing) {
                // in the session storage we always have the value set via applicationService.setLocale
                this.sessionStorageService.set('locale', language + '-' + country);
            }
            // luxon default locale
            Settings.defaultLocale =  localeId;
            this.locale = localeId;
            // numbro wants with upper case counter but moment is all lower case
            this.setNumbroLocale(full, true).then(() =>
                this.loadedLocale.resolve(localeId)
            ).catch(() => this.loadedLocale.resolve(localeId));
        }, () => {
            this.loadedLocale.reject('Could not set Locale because angular locale could not be loaded.');
        });
    }

    private makeFullLocale(localeId: string): string {
        let locale = this.localeMap[localeId];
        if (!locale) locale = localeId + '-' + localeId.toUpperCase();
        return locale;
    }

    private setNumbroLocale(localeId: string, tryOnlyLanguage: boolean): Promise<void> {
        if (numbro.language() === localeId) return Promise.resolve();
        
        if (numbro.languages()[localeId]) {
            numbro.setLanguage(localeId);
            return Promise.resolve();
        }
        
        const moduleLoader =  (module: { default: numbro.NumbroLanguage }) => {
            numbro.registerLanguage(module.default);
            numbro.setLanguage(localeId);
        }
        const errorHandler = () => {
                this.log.warn('numbro locale for ' + localeId + ' didn\'t resolve, fallback to default en-US');
        }
        switch(localeId) {
            case 'bg': return import('numbro/languages/bg.js').then(moduleLoader,errorHandler);
            case 'cs-CZ': return import('numbro/languages/cs-CZ.js').then(moduleLoader,errorHandler);
            case 'da-DK': return import('numbro/languages/da-DK.js').then(moduleLoader,errorHandler);
            case 'de-AT': return import('numbro/languages/de-AT.js').then(moduleLoader,errorHandler);
            case 'de-CH': return import('numbro/languages/de-CH.js').then(moduleLoader,errorHandler);
            case 'de-DE': return import('numbro/languages/de-DE.js').then(moduleLoader,errorHandler);
            case 'de-LI': return import('numbro/languages/de-LI.js').then(moduleLoader,errorHandler);
            case 'el': return import('numbro/languages/el.js').then(moduleLoader,errorHandler);
            case 'en-AU': return import('numbro/languages/en-AU.js').then(moduleLoader,errorHandler);
            case 'en-GB': return import('numbro/languages/en-GB.js').then(moduleLoader, errorHandler);
            case 'en-IE': return import('numbro/languages/en-IE.js').then(moduleLoader, errorHandler);
            case 'en-NZ': return import('numbro/languages/en-NZ.js').then(moduleLoader, errorHandler);
            case 'en-ZA': return import('numbro/languages/en-ZA.js').then(moduleLoader, errorHandler);
            case 'es-AR': return import('numbro/languages/es-AR.js').then(moduleLoader, errorHandler);
            case 'es-CL': return import('numbro/languages/es-CL.js').then(moduleLoader, errorHandler);
            case 'es-CO': return import('numbro/languages/es-CO.js').then(moduleLoader, errorHandler);
            case 'es-CR': return import('numbro/languages/es-CR.js').then(moduleLoader, errorHandler);
            case 'es-ES': return import('numbro/languages/es-ES.js').then(moduleLoader, errorHandler);
            case 'es-MX': return import('numbro/languages/es-MX.js').then(moduleLoader, errorHandler);
            case 'es-NI': return import('numbro/languages/es-NI.js').then(moduleLoader, errorHandler);
            case 'es-PE': return import('numbro/languages/es-PE.js').then(moduleLoader, errorHandler);
            case 'es-PR': return import('numbro/languages/es-PR.js').then(moduleLoader, errorHandler);
            case 'es-SV': return import('numbro/languages/es-SV.js').then(moduleLoader, errorHandler);
            case 'et-EE': return import('numbro/languages/et-EE.js').then(moduleLoader, errorHandler);
            case 'fa-IR': return import('numbro/languages/fa-IR.js').then(moduleLoader, errorHandler);
            case 'fi-FI': return import('numbro/languages/fi-FI.js').then(moduleLoader, errorHandler);
            case 'fil-PH': return import('numbro/languages/fil-PH.js').then(moduleLoader, errorHandler);
            case 'fr-CA': return import('numbro/languages/fr-CA.js').then(moduleLoader, errorHandler);
            case 'fr-CH': return import('numbro/languages/fr-CH.js').then(moduleLoader, errorHandler);
            case 'fr-FR': return import('numbro/languages/fr-FR.js').then(moduleLoader, errorHandler);
            case 'he-IL': return import('numbro/languages/he-IL.js').then(moduleLoader, errorHandler);
            case 'hu-HU': return import('numbro/languages/hu-HU.js').then(moduleLoader, errorHandler);
            case 'id': return import('numbro/languages/id.js').then(moduleLoader, errorHandler);
            case 'it-CH': return import('numbro/languages/it-CH.js').then(moduleLoader, errorHandler);
            case 'it-IT': return import('numbro/languages/it-IT.js').then(moduleLoader, errorHandler);
            case 'ja-JP': return import('numbro/languages/ja-JP.js').then(moduleLoader, errorHandler);
            case 'ko-KR': return import('numbro/languages/ko-KR.js').then(moduleLoader, errorHandler);
            case 'lv-LV': return import('numbro/languages/lv-LV.js').then(moduleLoader, errorHandler);
            case 'nb-NO': return import('numbro/languages/nb-NO.js').then(moduleLoader, errorHandler);
            case 'nb': return import('numbro/languages/nb.js').then(moduleLoader, errorHandler);
            case 'nl-BE': return import('numbro/languages/nl-BE.js').then(moduleLoader, errorHandler);
            case 'nl-NL': return import('numbro/languages/nl-NL.js').then(moduleLoader, errorHandler);
            case 'nn': return import('numbro/languages/nn.js').then(moduleLoader, errorHandler);
            case 'pl-PL': return import('numbro/languages/pl-PL.js').then(moduleLoader, errorHandler);
            case 'pt-BR': return import('numbro/languages/pt-BR.js').then(moduleLoader, errorHandler);
            case 'pt-PT': return import('numbro/languages/pt-PT.js').then(moduleLoader, errorHandler);
            case 'ro-RO': return import('numbro/languages/ro-RO.js').then(moduleLoader, errorHandler);
            case 'ro': return import('numbro/languages/ro.js').then(moduleLoader, errorHandler);
            case 'ru-RU': return import('numbro/languages/ru-RU.js').then(moduleLoader, errorHandler);
            case 'ru-UA': return import('numbro/languages/ru-UA.js').then(moduleLoader, errorHandler);
            case 'sk-SK': return import('numbro/languages/sk-SK.js').then(moduleLoader, errorHandler);
            case 'sl': return import('numbro/languages/sl.js').then(moduleLoader, errorHandler);
            case 'sr-Cyrl-RS': return import('numbro/languages/sr-Cyrl-RS.js').then(moduleLoader, errorHandler);
            case 'sv-SE': return import('numbro/languages/sv-SE.js').then(moduleLoader, errorHandler);
            case 'th-TH': return import('numbro/languages/th-TH.js').then(moduleLoader, errorHandler);
            case 'tr-TR': return import('numbro/languages/tr-TR.js').then(moduleLoader, errorHandler);
            case 'uk-UA': return import('numbro/languages/uk-UA.js').then(moduleLoader, errorHandler);
            case 'zh-CN': return import('numbro/languages/zh-CN.js').then(moduleLoader, errorHandler);
            case 'zh-MO': return import('numbro/languages/zh-MO.js').then(moduleLoader, errorHandler);
            case 'zh-SG': return import('numbro/languages/zh-SG.js').then(moduleLoader, errorHandler);
            case 'zh-TW': return import('numbro/languages/zh-TW.js').then(moduleLoader, errorHandler);
            default: {
                const index = localeId.indexOf('-');
                if (index > 0 && tryOnlyLanguage) {
                    return this.setNumbroLocale(localeId.split('-')[0], false);
                }
                else if (index < 0) {
                    return this.setNumbroLocale(this.makeFullLocale(localeId), false);
                }
                break;
            }
        }
        return Promise.resolve();
    }
    
    private setAngularLocale(language: string, country: string) {
        // angular locales are either <language lowercase> or <language lowercase> - <country uppercase>
        const localeId = country !== undefined && country.length > 0 ?
            language.toLowerCase() + '-' + country.toUpperCase() : language.toLowerCase();
        let context: string;
        if (env.mobile) {
            const index = this.doc.baseURI.indexOf('index.html');
            context = index > 0 ? this.doc.baseURI.substring(0,index) : '/';
            
        } else {
            const index = this.doc.baseURI.indexOf('/',8);
            context = index > 0 ? this.doc.baseURI.substring(index) : '/';
        }
        
        return new Promise<string>((resolve, reject) => {
            return import(
                `${context}locales/angular/${localeId}.mjs?localeid=${localeId}`).then(
                module => {
                    registerLocaleData(module.default, localeId);
                    resolve(localeId);
                },
                () => {
                    import(`${context}locales/angular/${language.toLowerCase()}.mjs?localeid=${language.toLowerCase()}`).then(module => {
                        registerLocaleData(module.default, localeId.split('-')[0]);
                        resolve(language.toLowerCase());
                    }, reject);
                });
        });
    }
}
