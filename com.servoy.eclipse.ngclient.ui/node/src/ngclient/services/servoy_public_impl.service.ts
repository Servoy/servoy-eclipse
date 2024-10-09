import { Injectable } from '@angular/core';
import { EventLike, IFormCache, JSEvent, ServoyPublicService, PopupForm, Locale, I18NListener, RequestInfoPromise } from '@servoy/public';
import { SabloService } from '../../sablo/sablo.service';
import { WebsocketService } from '../../sablo/websocket.service';
import { ServicesService } from '../../sablo/services.service';
import { FormService } from '../form.service';
import { LocaleService } from '../locale.service';
import { ServoyService } from '../servoy.service';
import { SvyUtilsService } from '../utils.service';
import { ApplicationService } from './application.service';
import { I18NProvider } from './i18n_provider.service';
import { PopupFormService } from './popupform.service';

@Injectable()
export class ServoyPublicServiceImpl extends ServoyPublicService {
    constructor(private sabloService: SabloService,
        private i18nProvider: I18NProvider,
        private utils: SvyUtilsService,
        private localeService: LocaleService,
        private applicationService: ApplicationService,
        private servoyService: ServoyService,
        private formService: FormService,
        private servicesService: ServicesService,
        private popupFormService: PopupFormService,
        private websocketService: WebsocketService) {
        super();
    }

    executeInlineScript<T>(formname: string, script: string, params: any[]): Promise<T> {
        return this.servoyService.executeInlineScript(formname, script, params);
    }

    callServiceServerSideApi<T>(servicename: string, methodName: string, args: Array<any>): Promise<T> {
        return this.servicesService.callServiceServerSideApi(servicename, methodName, args);
    }

    getI18NMessages(...keys: string[]): Promise<any> {
        return this.i18nProvider.getI18NMessages(...keys);
    }

    public listenForI18NMessages(...keys: string[]): I18NListener {
        return this.i18nProvider.listenForI18NMessages(...keys);
    }

    getClientnr(): string {
        return this.sabloService.getClientnr();
    }

    callService<T>(serviceName: string, methodName: string, argsObject: any, async?: boolean): RequestInfoPromise<T> {
        return this.sabloService.callService(serviceName, methodName, argsObject, async);
    }

    getLocale(): string {
        return this.localeService.getLocale();
    }

    getLocaleObject(): Locale {
        return this.sabloService.getLocale();
    }

    createJSEvent(event: EventLike, eventType: string, contextFilter?: string, contextFilterElement?: any): JSEvent {
        return this.utils.createJSEvent(event, eventType, contextFilter, contextFilterElement);
    }

    showFileOpenDialog(title: string, multiselect: boolean, acceptFilter: string, url: string): void {
        this.applicationService.showFileOpenDialog(title, multiselect, acceptFilter, url);
    }

    showMessageDialog(dialogTitle: string, dialogMessage: string, styleClass: string, values: string[], buttonsText: string[], inputType: string): Promise<string> {
        return this.applicationService.showMessageDialog(dialogTitle, dialogMessage, styleClass, values, buttonsText, inputType);
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

    /** 
     * @deprecated see interface jsDoc 
     */
    sendServiceChanges(serviceName: string, propertyName: string, propertyValue: any) {
        this.servicesService.sendServiceChangesWithValue(serviceName, propertyName, propertyValue, propertyValue);
    }

    sendServiceChangeToServer(serviceName: string, propertyName: string, propertyValue: any, oldPropertyValue: any): void {
        this.servicesService.sendServiceChangesWithValue(serviceName, propertyName, propertyValue, oldPropertyValue);
    }

    showForm(popup: PopupForm): void {
        this.popupFormService.showForm(popup);
    }

    cancelFormPopup(disableClearPopupFormCallToServer_or_name: boolean|string): void {
        this.popupFormService.cancelFormPopup(disableClearPopupFormCallToServer_or_name);
    }

    setFormStyleClasses(styleclasses: { property: string }): void {
        this.formService.setFormStyleClasses(styleclasses);
    }

    public isInTestingMode(): boolean {
        return this.getUIProperty('servoy.ngclient.testingMode');
    }
}
