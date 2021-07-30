import { Injectable } from '@angular/core';
import { EventLike, IFormCache, JSEvent, ServoyPublicService, PopupForm } from '@servoy/public';
import { SabloService } from '../../sablo/sablo.service';
import { FormService } from '../form.service';
import { LocaleService } from '../locale.service';
import { ServoyService } from '../servoy.service';
import { SvyUtilsService } from '../utils.service';
import { ApplicationService } from './application.service';
import { I18NProvider } from './i18n_provider.service';
import {PopupFormService} from './popupform.service';

@Injectable()
export class ServoyPublicServiceImpl extends ServoyPublicService {
    constructor(private sabloService: SabloService,
        private i18nProvider: I18NProvider,
        private utils: SvyUtilsService,
        private localeService: LocaleService,
        private applicationService: ApplicationService,
        private servoyService: ServoyService,
        private formService: FormService,
        private popupFormService : PopupFormService) {
        super();
    }

    executeInlineScript<T>(formname: string, script: string, params: any[]): Promise<T> {
        return this.servoyService.executeInlineScript(formname, script, params);
    }
    getI18NMessages(...keys: string[]): Promise<any> {
        return this.i18nProvider.getI18NMessages(...keys);
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
    createJSEvent(event: EventLike, eventType: string, contextFilter?: string, contextFilterElement?: any): JSEvent {
        return this.utils.createJSEvent(event, eventType, contextFilter, contextFilterElement);
    }
    showFileOpenDialog(title: string, multiselect: boolean, acceptFilter: string, url: string): void {
        this.applicationService.showFileOpenDialog(title, multiselect, acceptFilter, url);
    }
    generateServiceUploadUrl(serviceName: string, apiFunctionName: string): string {
       return this.applicationService.generateServiceUploadUrl(serviceName, apiFunctionName);
    }
    generateUploadUrl(formname: string, componentName: string, propertyName: string): string {
        return this.applicationService.generateUploadUrl(formname, componentName, propertyName);
    }
    generateMediaDownloadUrl(media : string) : string{
        return this.applicationService.generateMediaDownloadUrl(media);
    }
    getUIProperty(key : string) : any{
        return this.applicationService.getUIProperty(key);
    }
    getFormCacheByName(containedForm: string): IFormCache {
        return this.formService.getFormCacheByName(containedForm);
    }
    sendServiceChanges(serviceName: string,propertyName: string, propertyValue: any) {
        this.sabloService.sendServiceChanges(serviceName, propertyName, propertyValue);
    }
    
    showForm(popup: PopupForm) : void{
        this.popupFormService.showForm(popup);
    }
    
    cancelFormPopup(disableClearPopupFormCallToServer: boolean): void{
         this.popupFormService.cancelFormPopup(disableClearPopupFormCallToServer);
    }
}

