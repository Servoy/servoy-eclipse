import { Injectable } from '@angular/core';

import { ServoyService,SessionProblem } from '../servoy.service'

@Injectable()
export class SessionService {
    
    constructor(public servoyService:ServoyService) {
        }
    
    public expireSession(sessionExpired : SessionProblem){
        var exp = { 
                viewUrl: 'templates/sessionExpiredView.html',
                redirectUrl : window.location.href
        }
        if(sessionExpired.viewUrl)  exp.viewUrl= sessionExpired.viewUrl;
        if(sessionExpired.redirectUrl)  exp.redirectUrl= sessionExpired.redirectUrl;

        this.servoyService.getSolutionSettings().sessionProblem = exp;
    }
    
    public setNoLicense(noLicense: SessionProblem){
        var noLic = {
                viewUrl : 'templates/serverTooBusyView.html',
                redirectUrl : window.location.href,
                redirectTimeout : -1
        }
        if(noLicense.viewUrl) noLic.viewUrl = noLicense.viewUrl 
        if(noLicense.redirectUrl) noLic.redirectUrl = noLicense.redirectUrl;
        if(noLicense.redirectTimeout) noLic.redirectTimeout = noLicense.redirectTimeout;

        this.servoyService.getSolutionSettings().sessionProblem = noLic;
    }
    
    public setMaintenanceMode(maintenanceMode : SessionProblem){
        var ment = {
                viewUrl : 'templates/maintenanceView.html',
                redirectUrl : window.location.href,
                redirectTimeout : -1
        }
        if(maintenanceMode.viewUrl) ment.viewUrl = maintenanceMode.viewUrl 
        if(maintenanceMode.redirectUrl) ment.redirectUrl = maintenanceMode.redirectUrl;
        if(maintenanceMode.redirectTimeout) ment.redirectTimeout = maintenanceMode.redirectTimeout;

        this.servoyService.getSolutionSettings().sessionProblem = ment;
    }
    
    public setInternalServerError(internalServerError: SessionProblem){
        var error = {viewUrl:'templates/serverInternalErrorView.html'}
        if(internalServerError.viewUrl)  error.viewUrl = internalServerError.viewUrl;
        if(internalServerError.stack) error['stack'] = internalServerError.stack;

        this.servoyService.getSolutionSettings().sessionProblem = error;
    }
}