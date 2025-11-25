import { Injectable } from '@angular/core';

import { ServoyService,SessionProblem } from '../servoy.service';
import { WindowService } from './window.service';

@Injectable()
export class SessionService {

    constructor(public servoyService: ServoyService, private windowService: WindowService) {
        }

    public expireSession(sessionExpired: SessionProblem){
        const exp = {
                viewUrl: 'templates/sessionExpiredView.html',
                redirectUrl : window.location.href.split('#')[0]
        };
        if(sessionExpired.viewUrl)  exp.viewUrl= sessionExpired.viewUrl;
        if(sessionExpired.redirectUrl)  exp.redirectUrl= sessionExpired.redirectUrl;

        this.servoyService.getSolutionSettings().sessionProblem = exp;
        this.windowService.switchToSessionProblemPage();
    }

    public setNoLicense(noLicense: SessionProblem){
        const noLic = {
                viewUrl : 'templates/serverTooBusyView.html',
                redirectUrl : window.location.href,
                redirectTimeout : -1
        };
        if(noLicense.viewUrl) noLic.viewUrl = noLicense.viewUrl;
        if(noLicense.redirectUrl) noLic.redirectUrl = noLicense.redirectUrl;
        if(noLicense.redirectTimeout) noLic.redirectTimeout = noLicense.redirectTimeout;

        this.servoyService.getSolutionSettings().sessionProblem = noLic;
        this.windowService.switchToSessionProblemPage();
    }

    public setMaintenanceMode(maintenanceMode: SessionProblem){
        const ment = {
                viewUrl : 'templates/maintenanceView.html',
                redirectUrl : window.location.href,
                redirectTimeout : -1
        };
        if(maintenanceMode.viewUrl) ment.viewUrl = maintenanceMode.viewUrl;
        if(maintenanceMode.redirectUrl) ment.redirectUrl = maintenanceMode.redirectUrl;
        if(maintenanceMode.redirectTimeout) ment.redirectTimeout = maintenanceMode.redirectTimeout;

        this.servoyService.getSolutionSettings().sessionProblem = ment;
        this.windowService.switchToSessionProblemPage();
    }

    public setInternalServerError(internalServerError: SessionProblem){
        const error = {viewUrl:'templates/serverInternalErrorView.html'};
        if(internalServerError.viewUrl)  error.viewUrl = internalServerError.viewUrl;
        if(internalServerError.stack) error['stack'] = internalServerError.stack;

        this.servoyService.getSolutionSettings().sessionProblem = error;
        this.windowService.switchToSessionProblemPage();
    }
}
