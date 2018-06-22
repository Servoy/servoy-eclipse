import { Injectable, } from '@angular/core';
import { SessionStorageService } from 'angular-web-storage';

import { Observable , of} from 'rxjs';
import {delay} from 'rxjs/operators'

import { Deferred } from './util/deferred'
import { WindowRefService } from './util/windowref.service'

import { WebsocketService, WebsocketSession } from '../sablo/websocket.service';
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
        this.log = logFactory.getLogger(SabloService.name);
    }

    public connect( context, queryArgs, websocketUri ): WebsocketSession {
        const wsSessionArgs = {
            context: context,
            queryArgs: queryArgs,
            websocketUri: websocketUri
        };
        this.wsSession = this.websocketService.connect( wsSessionArgs.context, [this.getSessionId(), this.getWindowName(), this.getWindowId()], wsSessionArgs.queryArgs, wsSessionArgs.websocketUri );

        this.wsSession.onMessageObject(( msg, conversionInfo ) => {
            // data got back from the server

            if ( conversionInfo && conversionInfo.call ) msg.call = this.converterService.convertFromServerToClient( msg.call, conversionInfo.call, undefined, undefined, undefined );

            if ( msg.sessionid ) {
                this.sessionStorage.set( "sessionid", msg.sessionid );
            }
            if ( msg.windowid ) {
                this.sessionStorage.set( "windowid", msg.windowid );
            }
            if ( msg.sessionid || msg.windowid ) {
                // update the arguments on the reconnection websocket.
                this.websocketService.setConnectionPathArguments( [this.getSessionId(), this.getWindowName(), this.getWindowId()] );
            }

            if ( msg.call ) {
                // {"call":{"form":"product","element":"datatextfield1","api":"requestFocus","args":[arg1, arg2]}, // optionally "viewIndex":1 
                // "{ svy_types : {product: {datatextfield1: {0: "Date"}}} }
                var call = msg.call;

                this.log.debug(this.log.buildMessage(() => ("sbl * Received API call from server: '" + call.api + "' to form " + call.form + ", component " + ( call.propertyPath ? call.propertyPath : call.bean ) )));


                var previousApiCallPromise = null;
                if ( !call.delayUntilFormLoads ) {
                    // make sure normal and async API calls are called in the same sequence that they were called in server side JS
                    if ( this.apiCallDeferredQueue.length > 0 ) {
                        previousApiCallPromise = this.apiCallDeferredQueue[this.apiCallDeferredQueue.length - 1].promise;
                    }
                    this.apiCallDeferredQueue.push( new Deferred() );
                } // else it's a delayed call which means it shouldn't force load (in hidden div) the form if not resolved nor should it block other APIs from execution; it just waits for form to resolve



                if ( previousApiCallPromise ) {
                    return previousApiCallPromise.then(
                        function() {
                            return this.resolveFormIfNeededAndExecuteAPICall();
                        },
                        function( err ) {
                            this.log.error(this.log.buildMessage(() => ("sbl * Error waiting for api call execute " + err )));
                            return Promise.reject( err );
                        } );
                }
                else {
                    return this.resolveFormIfNeededAndExecuteAPICall( call );
                }
            }
        } );

        return this.wsSession
    }

    public getSessionId() {
        var sessionId = this.sessionStorage.get( 'sessionid' )
        if ( sessionId ) {
            return sessionId;
        }
        return this.websocketService.getURLParameter( 'sessionid' );
    }

    public getWindowName() {
        return this.websocketService.getURLParameter( 'windowname' );
    }

    public getWindowId() {
        return this.sessionStorage.get( 'windowid' );
    }

    public getWindowUrl( windowname: string ) {
        return "index.html?windowname=" + encodeURIComponent( windowname ) + "&sessionid=" + this.getSessionId();
    }

    public getLanguageAndCountryFromBrowser() {
        var langAndCountry;
        var browserLanguages = this.windowRefService.nativeWindow.navigator['languages'];
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
            var langAndCountry = this.getLanguageAndCountryFromBrowser();
            var array = langAndCountry.split( "-" );
            this.locale = { language: array[0], country: array[1], full: langAndCountry };
        }
        return this.locale;
    }

    public setLocale( loc ) {
        this.locale = loc;
    }
    
    public callService(serviceName:string, methodName:string, argsObject, async?:boolean) {
        var promise = this.wsSession.callService(serviceName, methodName, argsObject, async)
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
            var tmp = this.currentServiceCallCallbacks
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
        var funcThis;
        if ( call.viewIndex != undefined ) {
            // I think this viewIndex' is never used; it was probably intended for components with multiple rows targeted by the same component if it want to allow calling API on non-selected rows, but it is not used
            funcThis = formState.api[call.bean][call.viewIndex];
        }
        else if ( call.propertyPath != undefined ) {
            // handle nested components; the property path is an array of string or int keys going
            // through the form's model starting with the root bean name, then it's properties (that could be nested)
            // then maybe nested child properties and so on 
            var obj = formState.model;
            var pn;
            for ( pn in call.propertyPath ) obj = obj[call.propertyPath[pn]];
            funcThis = obj.api;
        }
        else {
            funcThis = formState.api[call.bean];
        }
        return funcThis;
    }

    private executeAPICall( call, apiCallFunctions ) {
        var func = apiCallFunctions ? apiCallFunctions[call.api] : null;
        var returnValue;
        if ( !func ) {
            this.log.warn(this.log.buildMessage(() => ("sbl * Bean " + ( call.propertyPath ? call.propertyPath : call.bean ) + " on form " + call.form + " did not provide the called api: " + call.api )))
            returnValue = null;
        }
        else {
            this.log.debug(this.log.buildMessage(() => ("sbl * Api call '" + call.api + "' to form " + call.form + ", component " + ( call.propertyPath ? call.propertyPath : call.bean ) + " will be called now." )));
            returnValue = func.apply( apiCallFunctions, call.args );
        }
        return returnValue;
    }

    private executeAPICallInTimeout( call, formState, count, timeout ) {
        return of().pipe(delay( timeout )).toPromise().then(() => {
            var apiFunctions = this.getAPICallFunctions( call, formState );
            this.log.debug(this.log.buildMessage(() => ("sbl * Remaining wait cycles upon execution of API: '" + call.api + "' of form " + call.form + ", component " + ( call.propertyPath ? call.propertyPath : call.bean ) + ": " + count )));
            if ( ( apiFunctions && apiFunctions[call.api] ) || count < 1 ) {
                return this.executeAPICall( call, apiFunctions );
            } else {
                return this.executeAPICallInTimeout( call, formState, count - 1, timeout )
            }
        } ).then( function( result ) {
            return result;
        }, function( err ) {
            return Promise.reject( err );
        } );
    }

    private resolveFormIfNeededAndExecuteAPICall( call ) {
        // TODO API CALLS
    }
}