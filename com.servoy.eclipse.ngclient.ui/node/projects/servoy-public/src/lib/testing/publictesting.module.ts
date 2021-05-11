import { Injectable, NgModule } from '@angular/core';
import { EventLike, JSEvent } from '../jsevent';
import { IComponentCache, IFormCache, ServoyPublicService } from '../services/servoy_public.service';
import { ServoyPublicModule } from '../servoy_public.module';

@Injectable()
export class ServoyPublicServiceTestingImpl extends ServoyPublicService {
    private locale: string;
    private messages: { [key: string]: string } = {};
    private forms: { [key: string]: IFormCache } = {};

    public addForm(name: string, formCache: IFormCache) {
        this.forms[name] = formCache;
    }

    public getFormCacheByName(containedForm: string): IFormCache {
        if (this.forms[containedForm]) return this.forms[containedForm];
        const form: IFormCache= {
            absolute: true,
            size:  {width: 100, height: 100},
            getComponent: (name: string) => {
                const comp: IComponentCache = {
                    name,
                    model: {}
                };
                return comp;
            }
        };
        return form;
    }

    public generateServiceUploadUrl(serviceName: string, apiFunctionName: string): string {
         return 'resources/upload/1/svy_services/' + serviceName + '/' + apiFunctionName;
    }

    public generateUploadUrl(formname: string, componentName: string, propertyName: string): string {
         return 'resources/upload/1' +
            (formname ? '/' + formname : '') +
            (componentName ? '/' + componentName : '') +
            (propertyName ? '/' + propertyName : '');
    }

    public executeInlineScript<T>(formname: string, script: string, params: any[]): Promise<T> {
        throw new Error('Method not implemented.');
    }

    public addMessage(key: string, message: string) {
        this.messages[key] = message;
    }

    public getI18NMessages(...keys: string[]): Promise<any> {
        const resolvedMessages: {[key: string]: string } = {};
        keys.forEach(key => resolvedMessages[key] = this.messages[key]?this.messages[key]:'');
        return Promise.resolve(resolvedMessages); 
    }

    public callService<T>(serviceName: string, methodName: string, argsObject: any, async?: boolean): Promise<T> {
        throw new Error('Method not implemented.');
    }
    public setLocale(locale: string) {
        this.locale = locale;
    }
    public getLocale(): string {
        return this.locale ? this.locale : 'en';
    }
    public createJSEvent(event: EventLike, eventType: string, contextFilter?: string, contextFilterElement?: any): JSEvent {
        const ev = new JSEvent();
        ev.eventType = eventType;
        ev.svyType = eventType;
        ev.formName = 'test';
        ev.elementName = 'test';
        ev.timestamp = new Date().getTime();
        return ev;
    }
    public showFileOpenDialog(title: string, multiselect: boolean, acceptFilter: string, url: string): void {
        throw new Error('Method not implemented.');
    }

}
@NgModule({
    declarations: [

    ],
    imports: [
        ServoyPublicModule
    ],
    exports: [
        ServoyPublicModule
    ],
    providers: [
        ServoyPublicServiceTestingImpl, { provide: ServoyPublicService, useExisting: ServoyPublicServiceTestingImpl }
    ],
    schemas: [

    ]
})
export class ServoyPublicTestingModule { }
