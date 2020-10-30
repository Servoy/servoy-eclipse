import { Injectable } from '@angular/core';
import { SabloService } from '../sablo/sablo.service';
import { Deferred } from '../sablo/util/deferred';
import { registerLocaleData } from '@angular/common';
import { SessionStorageService } from '../sablo/webstorage/sessionstorage.service';

import * as moment from 'moment';
import numbro from 'numbro';

import { I18NProvider } from './services/i18n_provider.service';

@Injectable()
export class LocaleService {
    private locale = 'en';
    private loadedLocale: Deferred<any>;

    constructor(private sabloService: SabloService,
        private i18nProvider: I18NProvider,
        private sessionStorageService: SessionStorageService) {
    }

    public isLoaded(): Promise<any> {
        return this.loadedLocale.promise;
    }

    public getLocale(): string {
        return this.locale;
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
            this.locale = localeId;
            Promise.all([this.setMomentLocale(localeId), this.setNumbroLocale(localeId)]).then( () =>
                this.loadedLocale.resolve(localeId)
                ).catch( () => this.loadedLocale.resolve(localeId));
        }, () => {
            this.loadedLocale.reject('Could not set Locale because angular locale could not be loaded.');
        });
    }

    private setMomentLocale(localeId: string): Promise<void> {
        return import(`../../node_modules/moment/src/locale/${localeId}`).then(
            module => {
                if (moment.locale() !== localeId)
                    moment.defineLocale(localeId, module);
            }).catch(e => {
                if (localeId.indexOf('-') === -1) {
                    return this.setMomentLocale(localeId + '-' + localeId.toUpperCase());
                } else {
                    console.log('moment locale for ' + localeId + ' didn\'t resolve, fallback to default en-US');
                }
            });
    }

    private setNumbroLocale(localeId: string): Promise<void> {
        return import(`numbro/languages/${localeId}`).then(module => {
            numbro.registerLanguage(module.default);
            numbro.setLanguage(localeId);
        }).catch(e => {
            if (localeId.indexOf('-') === -1) {
                return this.setNumbroLocale(localeId + '-' + localeId.toUpperCase());
            } else {
                console.log('numbro locale for ' + localeId + ' didn\'t resolve, fallback to default en-US');
            }
        });
    }

    private setAngularLocale(language: string, country: string) {
        // angular locales are either <language lowercase> or <language lowercase> - <country uppercase>
        const localeId = country !== undefined && country.length > 0 ?
            language.toLowerCase() + '-' + country.toUpperCase() : language.toLowerCase();
        return new Promise<string>((resolve, reject) => {
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
}
