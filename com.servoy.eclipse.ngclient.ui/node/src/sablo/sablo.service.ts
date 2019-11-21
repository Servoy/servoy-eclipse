import { Injectable, } from '@angular/core';
import { SessionStorageService } from 'angular-web-storage';

import { Observable , of} from 'rxjs';
import {delay} from 'rxjs/operators'

import { Deferred } from './util/deferred'
import { WindowRefService } from './util/windowref.service'

import { WebsocketService, WebsocketSession, WebsocketConstants } from '../sablo/websocket.service';
import { ConverterService } from './converter.service'
import { LoggerService, LoggerFactory } from './logger.service'



@Injectable()
export class SabloService {

    private apiCallDeferredQueue = [];
    
    private locale = null;
    
    private wsSession:WebsocketSession;
        
    private currentServiceCallCallbacks = []
    private currentServiceCallDone;
    private currentServiceCallWaiting = 0
    private currentServiceCallTimeouts;
    private log: LoggerService;
    

    constructor( private websocketService: WebsocketService, private sessionStorage: SessionStorageService, private converterService: ConverterService, private windowRefService:WindowRefService, private logFactory : LoggerFactory ) {
        this.log = logFactory.getLogger("SabloService");
    }

    public connect( context, queryArgs, websocketUri ): WebsocketSession {
        const wsSessionArgs = {
            context: context,
            queryArgs: queryArgs,
            websocketUri: websocketUri
        };

        if(this.websocketService.getURLParameter(WebsocketConstants.CLEAR_SESSION_PARAM) == 'true'){
            this.clearSabloInfo();
        }
        this.wsSession = this.websocketService.connect( wsSessionArgs.context, [this.getClientnr(), this.getWindowName(), this.getWindownr()], wsSessionArgs.queryArgs, wsSessionArgs.websocketUri );

        this.wsSession.onMessageObject(( msg, conversionInfo ) => {
            // data got back from the server

            if ( conversionInfo && conversionInfo.call ) msg.call = this.converterService.convertFromServerToClient( msg.call, conversionInfo.call, undefined, undefined );

            if ( msg.clientnr ) {
                this.sessionStorage.set( "clientnr", msg.clientnr );
            }
            if ( msg.windownr ) {
                this.sessionStorage.set( "windownr", msg.windownr );
            }
            if ( msg.clientnr || msg.windownr ) {
                // update the arguments on the reconnection websocket.
                this.websocketService.setConnectionPathArguments( [this.getClientnr(), this.getWindowName(), this.getWindownr()] );
            }
        } );

        return this.wsSession
    }
    
    private clearSabloInfo() {
        this.sessionStorage.remove('windownr');
        this.sessionStorage.remove('clientnr');
    }

    public getClientnr() {
        const sessionnr = this.sessionStorage.get( 'clientnr' )
        if ( sessionnr ) {
            return sessionnr;
        }
        return this.websocketService.getURLParameter( 'clientnr' );
    }

    public getWindowName() {
        return this.websocketService.getURLParameter( 'windowname' );
    }

    public getWindownr() {
        return this.sessionStorage.get( 'windownr' );
    }

    public getWindowUrl( windowname: string ) {
        return "index.html?windowname=" + encodeURIComponent( windowname ) + "&clientnr=" + this.getClientnr();
    }

    public getLanguageAndCountryFromBrowser() {
        let langAndCountry;
        const browserLanguages = this.windowRefService.nativeWindow.navigator['languages'];
        // this returns first one of the languages array if the browser supports this (Chrome and FF) else it falls back to language or userLanguage (IE, and IE seems to return the right one from there)
        if ( browserLanguages && browserLanguages.length > 0 ) {
            langAndCountry = browserLanguages[0];
            if ( browserLanguages.length > 1 && langAndCountry.indexOf( '-' ) === -1
                && browserLanguages[1].indexOf( langAndCountry + '-' ) == 0 ) {
                // if the first language in the list doesn't specify country, see if the following one is the same language but with a country specified (for example browser could give a list of "en", "en-GB", ...)
                langAndCountry = browserLanguages[1];
            }
        } else {
            langAndCountry = ( this.windowRefService.nativeWindow.navigator.language || this.windowRefService.nativeWindow.navigator['userLanguage'] );
        }
        // in some weird scenario in firefox is not set, default it to en
        if ( !langAndCountry ) langAndCountry = 'en';
        return langAndCountry;
    }
    public getLocale() {
        if ( !this.locale ) {
            const langAndCountry = this.getLanguageAndCountryFromBrowser();
            const array = langAndCountry.split( "-" );
            this.locale = { language: array[0], country: array[1], full: langAndCountry };
        }
        return this.locale;
    }

    public setLocale( loc ) {
        this.locale = loc;
    }
    
    public callService(serviceName:string, methodName:string, argsObject, async?:boolean) {
        const promise = this.wsSession.callService(serviceName, methodName, argsObject, async)
        return async ? promise : this.waitForServiceCallbacks(promise, [100, 200, 500, 1000, 3000, 5000])
    }
    
    public addToCurrentServiceCall(func) {
        if (this.currentServiceCallWaiting == 0) {
            // No service call currently running, call the function now
            setTimeout(function() { func.apply(); })
        }
        else {
            this.currentServiceCallCallbacks.push(func)
        }
    }

    private  callServiceCallbacksWhenDone() {
        if (this.currentServiceCallDone || --this.currentServiceCallWaiting == 0) {
            this.currentServiceCallWaiting = 0
            this.currentServiceCallTimeouts.map(function(id) { return clearTimeout(id) })
            const tmp = this.currentServiceCallCallbacks
            this.currentServiceCallCallbacks = []
            tmp.map(function(func) { func.apply() })
        }
    }

    private waitForServiceCallbacks(promise:Promise<{}>, times) {
        if (this.currentServiceCallWaiting > 0) {
            // Already waiting
            return promise
        }

        this.currentServiceCallDone = false
        this.currentServiceCallWaiting = times.length
        this.currentServiceCallTimeouts = times.map((t)=> { return setTimeout(this.callServiceCallbacksWhenDone, t) })
        return promise.then((arg) => {
            this.currentServiceCallDone = true;
            return arg;
        }, (arg) => {
            this.currentServiceCallDone = true;
            return Promise.reject(arg);
        })
    }

    private getAPICallFunctions( call, formState ) {
        let funcThis;
        if ( call.viewIndex != undefined ) {
            // I think this viewIndex' is never used; it was probably intended for components with multiple rows targeted by the same component if it want to allow calling API on non-selected rows, but it is not used
            funcThis = formState.api[call.bean][call.viewIndex];
        }
        else if ( call.propertyPath != undefined ) {
            // handle nested components; the property path is an array of string or int keys going
            // through the form's model starting with the root bean name, then it's properties (that could be nested)
            // then maybe nested child properties and so on 
            let obj = formState.model;
            let pn;
            for ( pn in call.propertyPath ) obj = obj[call.propertyPath[pn]];
            funcThis = obj.api;
        }
        else {
            funcThis = formState.api[call.bean];
        }
        return funcThis;
    }
    
    public sendServiceChanges (serviceName : string, propertyName : string, value : any)
    {
        let changes = {};
        changes[propertyName] = this.converterService.convertClientObject(value);
        this.wsSession.sendMessageObject({ servicedatapush: serviceName, changes: changes });
    }
}