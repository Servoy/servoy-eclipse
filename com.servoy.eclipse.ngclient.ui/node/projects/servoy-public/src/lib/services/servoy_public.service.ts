import { Injectable } from '@angular/core';
import { getLocaleNumberSymbol, NumberSymbol } from '@angular/common';
import { EventLike, JSEvent } from '../jsevent';

@Injectable()
export abstract class ServoyPublicService {
    public getLocaleNumberSymbol(symbol: NumberSymbol): string {
        return getLocaleNumberSymbol(this.getLocale(), symbol);
    }
    public abstract showFileOpenDialog(title: string, multiselect: boolean, acceptFilter: string, url: string): void;
    public abstract createJSEvent(event: EventLike, eventType: string, contextFilter?: string, contextFilterElement?: any): JSEvent;
    public abstract getLocale(): string;
    public abstract callService<T>(serviceName: string, methodName: string, argsObject: any, async?: boolean): Promise<T>;
    public abstract getI18NMessages(...keys: string[]): Promise<any>;
    public abstract executeInlineScript<T>(formname: string, script: string, params: any[]): Promise<T>;
    public abstract generateUploadUrl(formname: string, componentName: string, propertyName: string): string;
    public abstract generateServiceUploadUrl(serviceName: string, apiFunctionName: string): string;
    public abstract generateMediaDownloadUrl(media: string): string;
    public abstract getUIProperty(key: string): any;
    public abstract getFormCacheByName(containedForm: string): IFormCache;
    public abstract sendServiceChanges(serviceName: string,propertyName: string, propertyValue: any): void;
}

/** Cache for a servoy form. */
export interface IFormCache {
    absolute: boolean;
    size: {width: number; height: number};
    getComponent(name: string): IComponentCache;
}

export interface IComponentCache {
    name: string;
    model: { [property: string]: any };
}
