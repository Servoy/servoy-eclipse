import { Injectable, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';

import { ServoyService } from '../servoy.service';

import { LoggerFactory, LoggerService, WindowRefService, LocalStorageService, MainViewRefService, setFirstDayOfWeek } from '@servoy/public';

import { SabloService } from '../../sablo/sablo.service';

import { BSWindowManager } from './bootstrap-window/bswindow_manager.service';
import { BSWindowOptions } from './bootstrap-window/bswindow';
import { DefaultLoginWindowComponent } from './default-login-window/default-login-window.component';
import { FileUploadWindowComponent } from './file-upload-window/file-upload-window.component';
import { MessageDialogWindowComponent } from './message-dialog-window/message-dialog-window.component';
import { LocaleService } from '../locale.service';
import { ServerDataService } from './serverdata.service';
import { AlertWindowComponent } from "./alert-window/alert-window.component";
import { resolve } from 'path';
import { reject } from 'lodash-es';

@Injectable()
export class ApplicationService {
    private userProperties: { [property: string]: any };
    private log: LoggerService;
    private minElectronVersion = '24.4.0';

    constructor(private servoyService: ServoyService,
        private localStorageService: LocalStorageService,
        private localeService: LocaleService,
        private windowRefService: WindowRefService,
        private mainViewRefService: MainViewRefService,
        private sabloService: SabloService,
        @Inject(DOCUMENT) private doc: Document,
        private bsWindowManager: BSWindowManager,
        private serverData: ServerDataService,
        logFactory: LoggerFactory) {
        this.log = logFactory.getLogger('ApplicationService');
    }

    public setLocale(language: string, country: string) {
        this.localeService.setLocale(language, country);
    }

    public setStyleSheets(paths: string[]) {
        // this can be called multiple times; e.g. overrideStyle, so delete the old ones
        this.doc.head.querySelectorAll('link').forEach(link => {
            if (link.getAttribute('svy-stylesheet')) {
                link.remove();
            }
        });
        if (paths) {
            for (const path of paths) {
                const link: HTMLLinkElement = this.doc.createElement('link');
                link.setAttribute('rel', 'stylesheet');
                this.doc.head.appendChild(link);
                link.setAttribute('href', path);
                link.setAttribute('svy-stylesheet', path);
            }
        }
    }

    public getUserProperty(key: string) {
        return this.getUserProperties()[key];
    }

    public setUserProperty(key: string, value: any) {
        const userProps = this.getUserProperties();
        if (value == null) delete userProps[key];
        else userProps[key] = value;
        this.localStorageService.set('userProperties', JSON.stringify(userProps));
    }

    public removeUserProperty(key: string) {
        const userProps = this.getUserProperties();
        delete userProps[key];
        this.localStorageService.set('userProperties', JSON.stringify(userProps));
    }

    public removeAllUserProperties() {
        this.localStorageService.set('userProperties', JSON.stringify({}));
    }

    public getUIProperty(key: string) {
        return this.servoyService.getUIProperties().getUIProperty(key);
    }

    public setUIProperty(key: string, value: any) {
        this.servoyService.getUIProperties().setUIProperty(key, value);
        if (key === ClientPropertyConstants.WINDOW_BRANDING_ICON_32) {
            this.setIcon(value, '32x32');
        } else if (key === ClientPropertyConstants.WINDOW_BRANDING_ICON_192) {
            this.setIcon(value, '192x192');
        } else if (key === ClientPropertyConstants.FIRST_DAY_OF_WEEK) {
            setFirstDayOfWeek(value);
        }

    }

    public setUIProperties(properties: { [property: string]: string }) {
        for (const key of Object.keys(properties)) {
            this.setUIProperty(key, properties[key]);
        }
    }

    public getUserPropertyNames() {
        return Object.getOwnPropertyNames(this.getUserProperties());
    }

    public showMessage(message: string) {
        const alertWindowComponent = this.mainViewRefService.mainContainer.createComponent(AlertWindowComponent);
        alertWindowComponent.instance.message = message;
        const origin: string = this.windowRefService.nativeWindow.location.origin;
        alertWindowComponent.instance.title = origin.substring(origin.indexOf('://') + 3);;

        const opt: BSWindowOptions = {
            id: 'svyalert',
            fromElement: alertWindowComponent.location.nativeElement.childNodes[0],
            title: '',
            resizable: false,
            isModal: true
        };

        const bsWindowInstance = this.bsWindowManager.createWindow(opt);
        alertWindowComponent.instance.setOnCloseCallback(() => {
            bsWindowInstance.close();
            alertWindowComponent.destroy();
        });
        bsWindowInstance.setActive(true);

        this.adjustDialogPosition();
    }

    private adjustDialogPosition() {
        //maintain alert-dialog in the center of the browser
        const dialog: HTMLElement = document.querySelector('.svy-alert') as HTMLElement;
        const windowHeight = window.innerHeight;
        const dialogHeight = dialog.offsetHeight;
        const topOffset = Math.max((windowHeight - dialogHeight) / 2, 0);
        dialog.style.top = topOffset + 'px';
    }

    public showUrl(pUrl: string, target: string, targetOptions: string, timeout: number) {
        let url = pUrl;
        // for now, if url starts with 'solutions' we replace it with 'solution'
        // in order for the security.logout to work, using the same server side code, on both ng1 and ng2
        if (url && url.startsWith('solutions')) {
            url = 'solution' + url.substring('solutions'.length);
        }
        if (!target) target = '_blank';
        if (!timeout) timeout = 0;
        setTimeout(() => {
            if (url.indexOf('resources/dynamic') === 0 && target === '_self') {
                let ifrm = this.doc.getElementById('srv_downloadframe');
                if (ifrm) {
                    ifrm.setAttribute('src', url);
                } else {
                    ifrm = this.doc.createElement('IFRAME');
                    ifrm.setAttribute('src', url);
                    ifrm.setAttribute('id', 'srv_downloadframe');
                    ifrm.setAttribute('name', 'srv_downloadframe');
                    ifrm.style.width = 0 + 'px';
                    ifrm.style.height = 0 + 'px';
                    this.doc.body.appendChild(ifrm);
                }
            } else {
                if (target == '_self' && this.isNgdesktopWithTargetSupport(this.windowRefService.nativeWindow.navigator.userAgent)) {
                    const r = this.windowRefService.nativeWindow['require'];
                    const ipcRenderer = r('electron').ipcRenderer;
                    ipcRenderer.send('open-url-with-target', this.getAbsoluteUrl(url));
                } else if (target === '_blank' && !targetOptions && url.indexOf(this.windowRefService.nativeWindow.location.hostname) >= 0) {
                    const a = this.doc.createElement('a');
                    a.href = url;
                    a.target = '_blank';
                    this.doc.body.appendChild(a);
                    a.click();
                    this.doc.body.removeChild(a);
                }
                else {
                    this.windowRefService.nativeWindow.open(url, target, targetOptions);

                }
            }
        }, timeout * 1000);
    }

    private getAbsoluteUrl(url: string) {
        if (this.isRelativeUrl(url)) {
            return new URL(url, document.baseURI).href;
        }
        return url;
    }

    private isRelativeUrl(url: string) {
        // Absolute URLs start with a protocol or are protocol-relative (start with //)
        return !(/^(?:[a-z]+:)?\/\//i.test(url));
    }

    private isNgdesktopWithTargetSupport(userAgent: string) {
        const electronVersion = userAgent.match(/Electron\/([0-9\.]+)/);
        if (!electronVersion) {
            return false;
        }

        const compare = this.compareVersions(electronVersion[1], this.minElectronVersion);
        return compare >= 0; // true if electronVersion >= this.minElectronVersion (24.4.0)
    }

    private compareVersions(v1: string, v2: string): number {
        let v1tokens = v1.split('.').map(Number);
        let v2tokens = v2.split('.').map(Number);

        for (let i = 0; i < v1tokens.length; ++i) {
            if (v1tokens[i] > v2tokens[i]) {
                return 1;
            }
            if (v1tokens[i] === v2tokens[i]) {
                continue;
            }
            if (v2tokens.length === i) {
                return 1;
            }
            return -1; //get here only if previous tokens (if any) were ==
        }
        if (v1tokens.length !== v2tokens.length) {
            return -1; //get here only if all tokens are equal but first version has less tokens
        }
        return 0;
    }

    public setStatusText(text: string) {
        this.windowRefService.nativeWindow.status = text;
    }

    public getScreenSize() {
        if (this.windowRefService.nativeWindow.screen) {
            return { width: this.windowRefService.nativeWindow.screen.width, height: this.windowRefService.nativeWindow.screen.height, orientation: this.windowRefService.nativeWindow.orientation };
        }
        return null;
    }

    public getUserAgentAndPlatform() {
        return { userAgent: this.windowRefService.nativeWindow.navigator.userAgent, platform: this.windowRefService.nativeWindow.navigator.platform };
    }

    public getClientBrowserInformation() {
        const locale = this.sabloService.getLocale();
        const userAgent = this.getUserAgentAndPlatform();
        let timeZone: string;
        try {
            timeZone = new Intl.DateTimeFormat().resolvedOptions().timeZone;
        } catch (e) {
            this.log.warn('Cant get client timeZone ' + e);
        }

        return {
            serverURL: this.getServerURL(),
            userAgent: userAgent.userAgent,
            platform: userAgent.platform,
            locale: locale.full,
            remote_ipaddress: this.serverData.getIPAdress(),
            remote_host: this.serverData.getHostAdress(),
            timeZone,
            utcOffset: (new Date(new Date().getFullYear(), 0, 1, 0, 0, 0, 0).getTimezoneOffset() / -60), utcDstOffset: (new Date(new Date().getFullYear(), 6, 1, 0, 0, 0, 0).getTimezoneOffset() / -60)
        };
    }

    public showInfoPanel(url: string, w: number, h: number, t: number, closeText: string) {
        const infoPanel = this.doc.createElement('div');
        infoPanel.innerHTML = '<iframe marginheight=0 marginwidth=0 scrolling=no frameborder=0 src=\'' + url + '\' width=\'100%\' height=\'' + (h - 25) +
            '\'></iframe><br><a href=\'#\' id =\'closePanelButton\'>' + closeText + '</a>';
        infoPanel.style.zIndex = '2147483647';
        infoPanel.id = 'infoPanel';
        const width = this.windowRefService.nativeWindow.innerWidth || this.doc.body.offsetWidth;
        infoPanel.style.position = 'absolute';
        infoPanel.style.left = ((width - w) - 30) + 'px';
        infoPanel.style.top = '10px';
        infoPanel.style.height = h + 'px';
        infoPanel.style.width = w + 'px';
        this.doc.body.appendChild(infoPanel);
        this.doc.getElementById('closePanelButton').addEventListener('click', (event: MouseEvent) => {
            this.doc.getElementById('infoPanel').style.display = 'none';
            event.preventDefault();
            event.stopPropagation();
            return false;
        });
        setTimeout(() => this.doc.getElementById('infoPanel').style.display = 'none', t);
    }

    public showDefaultLogin() {
        if (this.localStorageService.get('servoy_username') && this.localStorageService.get('servoy_password')) {
            const promise = this.sabloService.callService('applicationServerService', 'login',
                { username: this.localStorageService.get('servoy_username'), password: this.localStorageService.get('servoy_password'), encrypted: true }, false);
            promise.then((ok) => {
                if (!ok) {
                    this.localStorageService.remove('servoy_username');
                    this.localStorageService.remove('servoy_password');
                    this.showDefaultLoginWindow();
                }
            });
        } else {
            this.showDefaultLoginWindow();
        }
    }

    public clearDefaultLoginCredentials() {
        this.localStorageService.remove('servoy_username');
        this.localStorageService.remove('servoy_password');
        this.localStorageService.remove('servoy_id_token');
    }

    public rememberUser(u: { username: string, id_token: string }) {
        this.localStorageService.set('servoy_username', u.username);
        this.localStorageService.set('servoy_id_token', u.id_token);
    }

    public showFileOpenDialog(title: string, multiselect: boolean, acceptFilter: string, url: string) {
        if (!url) {
            url = this.generateUploadUrl(null, null, null);
        }
        const fileUploadWindowComponent = this.mainViewRefService.mainContainer.createComponent(FileUploadWindowComponent);

        fileUploadWindowComponent.instance.url = url;
        fileUploadWindowComponent.instance.title = title;
        fileUploadWindowComponent.instance.multiselect = multiselect;
        fileUploadWindowComponent.instance.filter = acceptFilter;

        const opt: BSWindowOptions = {
            id: 'svyfileupload',
            fromElement: fileUploadWindowComponent.location.nativeElement.childNodes[0],
            title: '',
            resizable: false,
            isModal: true
        };

        const bsWindowInstance = this.bsWindowManager.createWindow(opt);
        fileUploadWindowComponent.instance.setOnCloseCallback(() => {
            bsWindowInstance.close();
            fileUploadWindowComponent.destroy();
        });
        bsWindowInstance.setActive(true);
    }

    public showMessageDialog(dialogTitle: string, dialogMessage: string, styleClass: string, values: string[], buttonsText: string[],okButtonText?:string): Promise<string> {
        return new Promise((resolve, reject) => {
            const messageDialogWindowComponent = this.mainViewRefService.mainContainer.createComponent(MessageDialogWindowComponent);

            messageDialogWindowComponent.instance.message = dialogMessage;
            messageDialogWindowComponent.instance.styleClass = styleClass;
            messageDialogWindowComponent.instance.values = values;
            messageDialogWindowComponent.instance.buttonsText = buttonsText;
            messageDialogWindowComponent.instance.okButtonText = okButtonText

            const dialogWindowComponentEl = messageDialogWindowComponent.location.nativeElement.childNodes[0];
            const windowWidth = this.doc.documentElement.clientWidth;
            //const windowHeight = this.doc.documentElement.clientHeight;
            let top: number; let left: number;
            left = (windowWidth / 2) - (dialogWindowComponentEl.clientWidth / 2);
            top = 50; //(windowHeight / 2) - (dialogWindowComponentEl.clientHeight / 2);
            if (left < 0) left = 0;
            //if (top < 0) top = 0;

            const opt: BSWindowOptions = {
                id: 'svymessagedialog',
                fromElement: dialogWindowComponentEl,
                title: dialogTitle,
                resizable: false,
                isModal: true,
                location: {
                    left,
                    top
                }
            };

            const bsWindowInstance = this.bsWindowManager.createWindow(opt);
            messageDialogWindowComponent.instance.onCloseCallback = (r: string) => {
                bsWindowInstance.close();
                messageDialogWindowComponent.destroy();
                resolve(r);
            };
            bsWindowInstance.setActive(true);
        });
    }

    public getSolutionName() {
        return this.servoyService.getSolutionSettings().solutionName;
    }

    public trustAsHtml(beanModel: { clientProperty?: { trustDataAsHtml?: boolean } }) {

        if (beanModel && beanModel.clientProperty && beanModel.clientProperty.trustDataAsHtml) {
            return beanModel.clientProperty.trustDataAsHtml;
        }
        return this.servoyService.getUIProperties().getUIProperty('trustDataAsHtml');
    }

    public generateUploadUrl(formname: string, componentName: string, propertyName: string, tus?: boolean): string {
        let url = 'resources/upload/';
        if (tus) url = 'tus/upload/';
        return url + this.sabloService.getClientnr() +
            (formname ? '/' + formname : '') +
            (componentName ? '/' + componentName : '') +
            (propertyName ? '/' + propertyName : '') + '/';
    }

    public generateServiceUploadUrl(serviceName: string, apiFunctionName: string, tus?: boolean): string {
        // svy_services should be in sync with MediaResourceServlet.SERVICE_UPLOAD
        return this.generateUploadUrl('svy_services', serviceName, apiFunctionName, tus);
    }

    public generateMediaDownloadUrl(media: string): string {
        if (media && media.indexOf('media://') === 0) {
            media = media.substring(8);
        }
        return 'resources/fs/' + this.getSolutionName() + media;
    }

    public setClipboardContent(content: string): void {
        this.windowRefService.nativeWindow.navigator.clipboard.writeText(content);
    }

    public getClipboardContent(): Promise<string> {
        return this.windowRefService.nativeWindow.navigator.clipboard.readText();
    }

    public replaceUrlState() {
        history.replaceState({}, '', this.windowRefService.nativeWindow.location.href.split('?')[0]);
    }

    private showDefaultLoginWindow() {
        const defaultLoginWindowComponent = this.mainViewRefService.mainContainer.createComponent(DefaultLoginWindowComponent);
        defaultLoginWindowComponent.instance.setOnLoginCallback(() => defaultLoginWindowComponent.destroy());
    }

    private getUserProperties() {
        if (!this.userProperties) {
            const json = this.localStorageService.get('userProperties');
            if (json) {
                this.userProperties = JSON.parse(json);
            } else {
                this.userProperties = {};
            }
        }
        return this.userProperties;
    }

    private getServerURL() {
        // current remote address including the context (includes leading /)
        const context = this.doc.getElementsByTagName('base')[0].getAttribute('href');
        return this.windowRefService.nativeWindow.location.protocol + '//' + this.windowRefService.nativeWindow.location.host + context;
    }

    private setIcon(favicon: string, size: string) {
        const link: any = this.doc.querySelector('link[rel*=\'icon\'][sizes=\'' + size + '\']');
        if (link && link.href !== favicon) {
            link.href = favicon;
            this.doc.getElementsByTagName('head')[0].appendChild(link);
        }
    }
}

class ClientPropertyConstants {
    public static WINDOW_BRANDING_ICON_32 = 'window.branding.icon.32';
    public static WINDOW_BRANDING_ICON_192 = 'window.branding.icon.192';
    public static FIRST_DAY_OF_WEEK = 'firstDayOfWeek';
}
