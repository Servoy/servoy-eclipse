import { Injectable } from '@angular/core';
import { EventLike, IFormCache, JSEvent, ServoyPublicService, PopupForm, Locale, I18NListener } from '@servoy/public';
import { SabloService } from '../sablo/sablo.service';
import { FormService } from '../ngclient/form.service';
import { LocaleService } from '../ngclient/locale.service';
import { SvyUtilsService } from '../ngclient/utils.service';
import { ApplicationService } from '../ngclient/services/application.service';

@Injectable()
export class ServoyPublicServiceDesignerImpl extends ServoyPublicService {
    constructor(private sabloService: SabloService,
        private utils: SvyUtilsService,
        private localeService: LocaleService,
        private applicationService: ApplicationService,
        private formService: FormService) {
        super();
    }

    executeInlineScript<T>(formname: string, script: string, params: any[]): Promise<T> {
        return new Promise<any>(resolve => {
            resolve(null);
        })
    }
    callServiceServerSideApi<T>(servicename: string, methodName: string, args: Array<any>): Promise<T> {
        return new Promise<any>(resolve => {
            resolve(null);
        });
    }

    public listenForI18NMessages(...keys: string[]): I18NListener {
        throw new Error('Method not implemented.');
    }
    getI18NMessages(...keys: string[]): Promise<any> {
        return new Promise<any>(resolve => {
            resolve({});
        })
    }
    getClientnr(): string {
        return this.sabloService.getClientnr();
    }
    callService<T>(serviceName: string, methodName: string, argsObject: any, async?: boolean): Promise<T> {
        return this.sabloService.callService(serviceName, methodName, argsObject, async);
    }
    getLocale(): string {
        return this.localeService.getLocale();
    }
    getLocaleObject(): Locale {
        return this.localeService.getLocaleObject();
    }
    createJSEvent(event: EventLike, eventType: string, contextFilter?: string, contextFilterElement?: any): JSEvent {
        return this.utils.createJSEvent(event, eventType, contextFilter, contextFilterElement);
    }
    showFileOpenDialog(title: string, multiselect: boolean, acceptFilter: string, url: string): void {

    }
    showMessageDialog(dialogTitle: string, dialogMessage: string, styleClass: string, values: string[], buttonsText: string[]): Promise<string> {
        return this.applicationService.showMessageDialog(dialogTitle, dialogMessage, styleClass, values, buttonsText);
    }
    generateServiceUploadUrl(serviceName: string, apiFunctionName: string, tus?: boolean): string {
        return this.applicationService.generateServiceUploadUrl(serviceName, apiFunctionName, tus);
    }
    generateUploadUrl(formname: string, componentName: string, propertyName: string, tus?: boolean): string {
        return this.applicationService.generateUploadUrl(formname, componentName, propertyName, tus);
    }
    generateMediaDownloadUrl(media: string): string {
        return this.applicationService.generateMediaDownloadUrl(media);
    }
    getUIProperty(key: string): any {
        return this.applicationService.getUIProperty(key);
    }
    getFormCacheByName(containedForm: string): IFormCache {
        return this.formService.getFormCacheByName(containedForm);
    }
    
    /** @deprecated */
    sendServiceChanges(serviceName: string, propertyName: string, propertyValue: any) {

    }

    sendServiceChangeToServer(serviceName: string, propertyName: string, propertyValue: any, oldPropertyValue: any): void {
        
    }

    showForm(popup: PopupForm): void {

    }

    cancelFormPopup(disableClearPopupFormCallToServer: boolean): void {

    }

    setFormStyleClasses(styleclasses: { property: string }): void {
    }

    isInTestingMode(): boolean {
        return false;
    }
}

