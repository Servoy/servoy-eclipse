import { Injectable } from '@angular/core';
import { EventLike, IFormCache, JSEvent, ServoyPublicService, PopupForm, Locale, I18NListener } from '@servoy/public';
import { LocaleService } from '../ngclient/locale.service';
import { SvyUtilsService } from '../ngclient/utils.service';

/**
 * Minimal, fully stateless ServoyPublicService for the form-template render route (SVY-21460).
 *
 * Unlike ServoyPublicServiceDesignerImpl it does NOT depend on ApplicationService / ServoyService /
 * ServerDataService / SabloService. Those pull in the whole runtime graph and, provided at module
 * scope, close a dependency cycle back onto ServoyPublicService (NG0200). This route needs almost
 * nothing from ServoyPublicService at render time - only locale lookups (getLocale / number symbol /
 * ag-grid locale) that components use while rendering with no data and no server. Everything that
 * would talk to a server or need a client is a no-op returning sensible stateless values.
 */
@Injectable()
export class ServoyPublicServiceFormTemplateImpl extends ServoyPublicService {

    constructor(private localeService: LocaleService,
        private utils: SvyUtilsService) {
        super();
    }

    executeInlineScript<T>(_formname: string, _script: string, _params: any[]): Promise<T> {
        return Promise.resolve(null);
    }

    callServiceServerSideApi<T>(_servicename: string, _methodName: string, _args: Array<any>): Promise<T> {
        return Promise.resolve(null);
    }

    public listenForI18NMessages(..._keys: string[]): I18NListener {
        // stateless render: no i18n resolution, return an inert listener
        const listener: I18NListener = {
            messages: () => listener,
            destroy: () => { /* nothing to clean up */ }
        };
        return listener;
    }

    getI18NMessages(..._keys: string[]): Promise<any> {
        return Promise.resolve({});
    }

    getClientnr(): string {
        return null;
    }

    callService<T>(_serviceName: string, _methodName: string, _argsObject: any, _async?: boolean): Promise<T> {
        // stateless render: never talk to a server
        return Promise.resolve(null);
    }

    getLocale(): string {
        return this.localeService.getLocale();
    }

    getLocaleObject(): Locale {
        return this.localeService.getLocaleObject();
    }

    getAGGridLocale(): { [key: string]: string; } {
        return this.localeService.getAgGridLocale();
    }

    createJSEvent(event: EventLike, eventType: string, contextFilter?: string, contextFilterElement?: any): JSEvent {
        return this.utils.createJSEvent(event, eventType, contextFilter, contextFilterElement);
    }

    showFileOpenDialog(_title: string, _multiselect: boolean, _acceptFilter: string, _url: string): void {
        // stateless render: no dialogs
    }

    showMessageDialog(_dialogTitle: string, _dialogMessage: string, _styleClass: string, _values: string[], _buttonsText: string[], _inputType: string, _defaultButtonIndex: number, _okButtonText?: string): Promise<string> {
        return Promise.resolve(null);
    }

    generateServiceUploadUrl(_serviceName: string, _apiFunctionName: string, _tus?: boolean): string {
        return null;
    }

    generateUploadUrl(_formname: string, _componentName: string, _propertyName: string, _tus?: boolean): string {
        return null;
    }

    generateMediaDownloadUrl(_media: string): string {
        return null;
    }

    getUIProperty(_key: string): any {
        return null;
    }

    getFormCacheByName(_containedForm: string): IFormCache {
        return null;
    }

    /** @deprecated */
    sendServiceChanges(_serviceName: string, _propertyName: string, _propertyValue: any) {
        // stateless render: nothing is sent to server
    }

    sendServiceChangeToServer(_serviceName: string, _propertyName: string, _propertyValue: any, _oldPropertyValue: any): void {
        // stateless render: nothing is sent to server
    }

    showForm(_popup: PopupForm): void {
        // stateless render: no popups
    }

    cancelFormPopup(_disableClearPopupFormCallToServer_or_name: boolean | string): void {
        // stateless render: no popups
    }

    setFormStyleClasses(_styleclasses: { property: string }): void {
        // stateless render: no ngutils style classes
    }

    isInTestingMode(): boolean {
        return false;
    }
}
