import { Injectable } from '@angular/core';
import { getLocaleNumberSymbol, NumberSymbol } from '@angular/common';
import { EventLike, JSEvent } from '../jsevent';
import {PopupForm} from '../utils/popupform';

/**
 * This is the main servoy service that can be used by components or services to get a lot of information of the current client
 * or interact with the server by calling the server or generating the correct urls to the server.
 */
@Injectable()
export abstract class ServoyPublicService {
    /**
     * get the character for the NumberSymbol that is given like Decimal from the current locale of the client.
     */
    public getLocaleNumberSymbol(symbol: NumberSymbol): string {
        return getLocaleNumberSymbol(this.getLocale(), symbol);
    }
    /**
     * Let the system op the file dialog, this is used by the {@link UploadDirective}
     */
    public abstract showFileOpenDialog(title: string, multiselect: boolean, acceptFilter: string, url: string): void;

    public abstract showMessageDialog(dialogTitle: string, dialogMessage: string, styleClass: string, values: string[], buttonsText: string[], okButtonText?: string): Promise<string>;
    /**
     * This created the correct {@link JSEvent} object that is used for handler calls to the server, tries to fill in as much as it can.
     */
    public abstract createJSEvent(event: EventLike, eventType: string, contextFilter?: string, contextFilterElement?: unknown): JSEvent;
    /**
     * Returns the current locale string the client is in.
     */
    public abstract getLocale(): string;
    /**
     * Returns the full {@link Locale} object of the client.
     */
    public abstract getLocaleObject(): Locale;
    /**
     * Internal api, directives can use this to call server side registered services. (this is internal Servoy services).
     * Do not try to use this instead of callServiceServerSideApi() - which has to be used for calling server side JS scripting of custom services from packages.
     */
    public abstract callService<T>(serviceName: string, methodName: string, argsObject: unknown, async?: boolean): Promise<T>;
    /**
     * Get the i18n messages for the given keys.
     *
     * @deprecated use listenForI18NMessages(...keys: string[]):I18NListener
     */
    public abstract getI18NMessages(...keys: string[]): Promise<unknown>;
    /**
     * Get and listen for i18n message keys, this will call I18NListener.messages() for with an object of key value pairs for all the keys given for the current locale.
     * It will also call that same method again if the locale changed in the client with the updated values.
     * If the caller is destroyed the I18NListener.destroy() must be called to deregister this listener.
     */
    public abstract listenForI18NMessages(...keys: string[]): I18NListener;
    /**
     * Use this to call a script at the server that was given to use by a function spec property.
     */
    public abstract executeInlineScript<T>(formname: string, script: string, params: unknown[]): Promise<T>;
    /**
     * Services can use this to call there own server side api
     */
    public abstract callServiceServerSideApi<T>(servicename: string, methodName: string, args: Array<unknown>): Promise<T>;
    /**
     * Returns an upload url that can be used to push binary data to the given form/component/property combination. (MediaUpload component)
     */
    public abstract generateUploadUrl(formname: string, componentName: string, propertyName: string, tus?: boolean): string;
    /**
     * Returns an upload url to push binary data to a service serverside function (that gets the file as an argument)
     */
    public abstract generateServiceUploadUrl(serviceName: string, apiFunctionName: string, tus?: boolean): string;
    /**
     * Use this to get a url to a solutions media for download or src/href
     */
    public abstract generateMediaDownloadUrl(media: string): string;
    /**
     * returns the ui property value for the given key that is set as a ui property on the application.
     */
    public abstract getUIProperty(key: string): any;
    /** 
      * returns the {@link IFormCache} on the client to be able to get some extra data from it like size of that form.
      */
    public abstract getFormCacheByName(containedForm: string): IFormCache;
    /**
     * Services should use this function to push there changed model data to the server so the data is accesable in a servoy solution or stored for a browser refresh.
     * @deprecated please use the version of this method that also gives the oldPropertyValue as an argument - sendServiceChangeToServer
     */
    public abstract sendServiceChanges(serviceName: string, propertyName: string, propertyValue: unknown): void;
    /**
     * Services should use this function to push there changed model data to the server (when needed),
     * so the data is accesable in a servoy solution or stored for a browser refresh.
     *
     * @param propertyValue the new value that the service has (and should send to server) for the given propertyName; if you didn't assign it yet to the service's property,
     *                      this method will do it for you.
     * @param oldPropertValue the value that this property used to have before (or has if you did not change the reference - in this case it should be the same as „propertyValue”);
     *                        this value is used in case of smart types (custom array/custom objects) in order to detect if it's a full change by reference for example
     */
    public abstract sendServiceChangeToServer(serviceName: string, propertyName: string, propertyValue: unknown, oldPropertyValue: unknown): void;
    /**
     * show a form popup {@link PopupForm}
     */
    public abstract showForm(popup: PopupForm): void;
    /**
     * cancel/hide the form popup that is currently showing.
     */
    public abstract cancelFormPopup(disableClearPopupFormCallToServer: boolean): void;
    /**
     * Internal api, used by ngutils services to set the styleclass of forms, the argument is a map of formname->styleclases
     *
     * @internal
     */
    public abstract setFormStyleClasses(styleclasses: {property: string}): void;

    /**
     * Returns the value of the testing mode flag (servoy.ngclient.testingMode) from the admin page
     */
    public abstract isInTestingMode(): boolean;
}

/** 
 * Cache for a servoy form.
 * Simple interface around form data instances (not the actual form ui instances) on the client.
 */
export interface IFormCache {
    absolute: boolean;
    size: {width: number; height: number};
    getComponent(name: string): IComponentCache;
}

/**
 * Simple interface around component data instances on the client
 */
export interface IComponentCache {
    name: string;
    model: { [property: string]: any };
    layout: { [property: string]: any };
}

/**
 * Locale object that has the language and country fields.
 */
export interface Locale {
     language: string;
     country: string;
     full: string;
}

export interface I18NListener {
    messages( callback: (messages: Map<string,string>) => void): I18NListener;
    destroy(): void;
}
