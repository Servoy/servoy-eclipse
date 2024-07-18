import { Injectable } from '@angular/core';

import { SabloService } from '../../sablo/sablo.service';
import { wrapPromiseToPropagateCustomRequestInfoInternal } from '../../sablo/websocket.service';
import { Deferred, I18NListener, RequestInfoPromise } from '@servoy/public';

@Injectable({
  providedIn: 'root'
})
export class  I18NProvider {
    private cachedMessages = {};

    private cachedPromises: { [s: string]: { promise?: Promise<any>; value?: any } } = {};
    private defaultTranslations = {};

    private readonly listeners: Set<Listener> = new Set();

    constructor(private sabloService: SabloService) {
    }

    public addDefaultTranslations(translations: {[key: string]: string}) {
        for (const key of Object.keys(translations)) {
            this.defaultTranslations[key] = translations[key];
        }
    }
    public listenForI18NMessages(...keys: string[]): I18NListener {
        return new Listener(keys, this, this.listeners);
    }

    /**
     * Get the i18n messages for the given keys.
     *
     * @deprecated use listenForI18NMessages(...keys: string[]):I18NListener
     */
    public getI18NMessages(...keys: string[]): RequestInfoPromise<any> {
        const retValue = {};
        const serverKeys = {};
        let serverKeysCounter = 0;
        keys.forEach(key => {
            if (this.cachedMessages[key] != null) {
                retValue[key] = this.cachedMessages[key];
            } else {
                serverKeys[serverKeysCounter++] = key;
            }
        });
        if (serverKeysCounter > 0) {
            const promiseA = this.sabloService.callService('i18nService', 'getI18NMessages', serverKeys, false);
            return wrapPromiseToPropagateCustomRequestInfoInternal(promiseA, promiseA.then((result) => {
                for (const key of Object.keys(result)) {
                    this.cachedMessages[key] = result[key];
                    retValue[key] = result[key];
                }
                return retValue;
            }, (error) => Promise.reject(error)));
        } else {
            const defered = new Deferred<any>();
            defered.resolve(retValue);
            return defered.promise;
        }
    }

    public flush() {
        this.cachedMessages = {};
        for (const key in this.cachedPromises) {
            if (this.cachedPromises.hasOwnProperty(key) && this.cachedPromises[key].promise) {
                this.cachedPromises[key].promise['reject'] = true;
            }
        }
        this.cachedPromises = {};
        this.listeners.forEach( (listener) => listener.getMessages());
    }
}

class Listener implements I18NListener {
    private callback:  (messages: Map<string,string>) => void;

    constructor(private keys: string[], private service: I18NProvider, private listeners: Set<Listener>) {
        this.listeners.add(this);
        this.getMessages();
    }

    getMessages() {
        this.service.getI18NMessages(...this.keys).then( (value) => {
            const map = new Map<string, string>();
            for (const key of Object.keys(value)) {
                 map.set(key, value[key]);
            }
            this.postMessages(map);
        }
        ).catch((value)=> console.log(value));
    }

    destroy(): void {
        this.listeners.delete(this);
    }

    messages( callback: (messages: Map<string,string>) => void): I18NListener {
        this.callback = callback;
        return this;
    }

    postMessages(messages: Map<string,string>) {
        if (this.callback) this.callback(messages);
    }
}
