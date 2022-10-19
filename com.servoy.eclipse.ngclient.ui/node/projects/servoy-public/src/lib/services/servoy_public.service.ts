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
    /** 
      * This created the correct {@link JSEvent} object that is used for handler calls to the server, tries to fill in as much as it can.
      */
    public abstract createJSEvent(event: EventLike, eventType: string, contextFilter?: string, contextFilterElement?: any): JSEvent;
    /** 
      * Returns the current locale string the client is in.
      */
    public abstract getLocale(): string;
    /** 
      * Returns the full {@link Locale} object of the client.
      */
    public abstract getLocaleObject(): Locale; 
    /** 
      * Internal api, directives can use this to call server side registered services 
      */
    public abstract callService<T>(serviceName: string, methodName: string, argsObject: any, async?: boolean): Promise<T>;
    /** 
      * Get the i18n messages for the given keys.
      */
    public abstract getI18NMessages(...keys: string[]): Promise<any>;
    /** 
      * Use this to call a script at the server that was given to use by a function spec property.
      */
    public abstract executeInlineScript<T>(formname: string, script: string, params: any[]): Promise<T>;
    /** 
      * Services can use this to call there own server side api 
      */
    public abstract callServiceServerSideApi<T>(servicename: string, methodName: string, args: Array<any>): Promise<T>;
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
      */
    public abstract sendServiceChanges(serviceName: string, propertyName: string, propertyValue: any): void;
    /** 
      * show a form popup {@link PopupForm}
      */
    public abstract showForm(popup: PopupForm) : void;
    /** 
      * cancel/hide the form popup that is currently showing.
      */
    public abstract cancelFormPopup(disableClearPopupFormCallToServer: boolean): void;
    /** 
      * Internal api, used by ngutils services to set the styleclass of forms, the argument is a map of formname->styleclases
      * @internal
      */
    public abstract setFormStyleClasses(styleclasses: {property : string}): void;
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
