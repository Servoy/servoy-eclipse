import { Injectable } from '@angular/core';
import { SabloService } from '../../sablo/sablo.service';
import { WindowRefService } from '../../sablo/util/windowref.service';
import { WebsocketService } from '../../sablo/websocket.service';
import { ServoyService } from '../servoy.service';
import { I18NProvider } from './i18n_provider.service';

@Injectable()
export class  ServerDataService {

    private data: {pathName: string;querystring: string;ipaddr: string;hostaddr: string; orientation: number;defaultTranslations: {[key: string]: string}};
    constructor(windowRefService: WindowRefService,
                websocketService: WebsocketService,
                sabloService: SabloService,
                servoyService: ServoyService,
                i18NProvider: I18NProvider) {
        this.data = windowRefService.nativeWindow['svyData'];
        if (this.data.querystring) websocketService.setQueryString(this.data.querystring);
        if (this.data.pathName) websocketService.setPathname(this.data.pathName);

        const orientation = this.data.orientation;
        if (orientation === 2) {
            servoyService.getSolutionSettings().ltrOrientation = false;
        } else if (orientation === 3) {
            const language = sabloService.getLocale().language;
            if (language === 'iw' || language === 'ar' ||language === 'fa' ||language === 'ur') {
                servoyService.getSolutionSettings().ltrOrientation = false;
            } else {
                servoyService.getSolutionSettings().ltrOrientation = true;
            }
        } else {
            servoyService.getSolutionSettings().ltrOrientation = true;
        }
        if (this.data.defaultTranslations) i18NProvider.addDefaultTranslations(this.data.defaultTranslations);
    }

    init() {
        // just here is it can be called on.
    }


    public getIPAdress(): string {
        return this.data.ipaddr;
    }

    public getHostAdress(): string {
        return this.data.hostaddr;
    }
}
