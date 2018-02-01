import { Injectable, } from '@angular/core';
import { SessionStorageService } from 'angular-web-storage';

import { Observable } from 'rxjs/Observable';
import "rxjs/add/observable/of"
import "rxjs/add/operator/delay"

import { Deferred } from './util/deferred'
import { WindowRefService } from './util/windowref.service'

import { WebsocketService, WebsocketSession } from '../sablo/websocket.service';
import { ConverterService } from './converter.service'



@Injectable()
export class SabloService {

    private formStates = {};
    private formStatesConversionInfo = {};

    private deferredFormStates = {};
    private deferredFormStatesWithData = {};
    private apiCallDeferredQueue = [];
    private formResolver = null;
    
    private locale = null;
    
    private wsSession;
        
    private currentServiceCallCallbacks = []
    private currentServiceCallDone;
    private currentServiceCallWaiting = 0
    private currentServiceCallTimeouts;
    

    constructor( private websocketService: WebsocketService, private sessionStorage: SessionStorageService, private converterService: ConverterService, private windowRefService:WindowRefService ) {
    }

    public connect( context, queryArgs, websocketUri ): WebsocketSession {
        const wsSessionArgs = {
            context: context,
            queryArgs: queryArgs,
            websocketUri: websocketUri
        };
        this.wsSession = this.websocketService.connect( wsSessionArgs.context, [this.getSessionId(), this.getWindowName(), this.getWindowId()], wsSessionArgs.queryArgs, wsSessionArgs.websocketUri );

        this.wsSession.onMessageObject(( msg, conversionInfo, scopesToDigest ) => {
            // data got back from the server
            for ( var formname in msg.forms ) {
                var formState = this.formStates[formname];
                if ( typeof ( formState ) == 'undefined' ) {
                    // if the form is not there yet, wait for the form state.
                    this.getFormState( formname ).then( this.getFormMessageHandler( formname, msg, conversionInfo ),
                        function( err ) {
                            //                            $log.error( "Error getting form state when trying to handle msg. from server: " + err ); 
                        } );
                } else {
                    // if the form is there apply it directly so that it is there when the form is recreated
                    this.getFormMessageHandler( formname, msg, conversionInfo )( formState );
                    if ( formState.getScope ) {
                        var s = formState.getScope();
                        if ( s ) scopesToDigest.putItem( s );
                    }
                }
            }

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

                //                if ( $log.debugEnabled ) $log.debug( "sbl * Received API call from server: '" + call.api + "' to form " + call.form + ", component " + ( call.propertyPath ? call.propertyPath : call.bean ) );


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
                            //                            $log.error( "sbl * Error waiting for api call execute " + err );
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

    public hasResolvedFormState( name ) {
        return typeof ( this.formStates[name] ) !== 'undefined' && this.formStates[name].resolved;
    }

    public getFormState( name ) {
        return this.getFormStateImpl( name, false );
    }

    public getFormStateWithData( name ) {
        return this.getFormStateImpl( name, true );
    }

    public contributeFormResolver( contributedFormResolver ) {
        this.formResolver = contributedFormResolver;
    }

    public getFormStateEvenIfNotYetResolved( name ) {
        return this.formStates[name];
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

    private markServiceCallDone(arg) {
       this. currentServiceCallDone = true;
        return arg;
    }

    private markServiceCallFailed(arg) {
        this.currentServiceCallDone = true;
        return Promise.reject(arg);
    }

    private waitForServiceCallbacks(promise, times) {
        if (this.currentServiceCallWaiting > 0) {
            // Already waiting
            return promise
        }

        this.currentServiceCallDone = false
        this.currentServiceCallWaiting = times.length
        this.currentServiceCallTimeouts = times.map(function(t) { return setTimeout(this.callServiceCallbacksWhenDone, t) })
        return promise.then(this.markServiceCallDone, this.markServiceCallFailed)
    }

    /*
     * Some code is interested in form state immediately after it's loaded/initialized (needsInitialData = false) in which case only some template values might be
     * available and some code is interested in using form state only after it got the initialData (via "requestData"'s response) from server (needsInitialData = true)
     */
    private getFormStateImpl( name, needsInitialData ) {
        // TODO should we also keep track here of time passed? (a timeout that is cleared if it does get resolved) (if it's not loaded/initialized in X min just reject the deferredState)
        var deferredStates = needsInitialData ? this.deferredFormStatesWithData : this.deferredFormStates;
        var defered;
        if ( deferredStates[name] ) {
            defered = deferredStates[name];
        } else {
            defered = new Deferred();
            deferredStates[name] = defered;
        }
        var formState = this.formStates[name];
        if ( formState && formState.resolved && !( formState.initializing && needsInitialData ) ) {
            defered.resolve( this.formStates[name] ); // then handlers are called even if they are applied after it is resolved
            delete deferredStates[name];
        }
        return defered.promise;
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
            //            $log.warn( "sbl * Bean " + ( call.propertyPath ? call.propertyPath : call.bean ) + " on form " + call.form + " did not provide the called api: " + call.api )
            returnValue = null;
        }
        else {
            //            if ( $log.debugEnabled ) $log.debug( "sbl * Api call '" + call.api + "' to form " + call.form + ", component " + ( call.propertyPath ? call.propertyPath : call.bean ) + " will be called now." );
            returnValue = func.apply( apiCallFunctions, call.args );
        }
        return returnValue;
    }

    private executeAPICallInTimeout( call, formState, count, timeout ) {
        return Observable.of().delay( timeout ).toPromise().then(() => {
            var apiFunctions = this.getAPICallFunctions( call, formState );
            //            if ( $log.debugEnabled ) $log.debug( "sbl * Remaining wait cycles upon execution of API: '" + call.api + "' of form " + call.form + ", component " + ( call.propertyPath ? call.propertyPath : call.bean ) + ": " + count );
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
        if ( !call.delayUntilFormLoads && this.formResolver != null && !this.hasResolvedFormState( call.form ) ) {
            // this means that the form was shown and is now hidden/destroyed; but we still must handle API call to it!
            // see if the form needs to be loaded;
            //            if ( $log.debugEnabled ) $log.debug( "sbl * Api call '" + call.api + "' to unresolved form " + call.form + ", component " + ( call.propertyPath ? call.propertyPath : call.bean ) + "  will call prepareUnresolvedFormForUse." );
            this.formResolver.prepareUnresolvedFormForUse( call.form );
        }

        return this.getFormStateWithData( call.form ).then(
            function( formState ) {
                var apiFunctions = this.getAPICallFunctions( call, formState );
                if ( apiFunctions && apiFunctions[call.api] ) {
                    return this.executeAPICall( call, apiFunctions );
                } else {
                    //                    if ( $log.debugEnabled ) $log.debug( "sbl * Waiting for API to be contributed before execution: '" + call.api + "' of form " + call.form + ", component " + ( call.propertyPath ? call.propertyPath : call.bean ) );
                    return this.executeAPICallInTimeout( formState, 10, 20 );
                }
            },
            function( err ) {
                //                $log.error( "sbl * Error getting form state: " + err );
                return Promise.reject( "Error getting form state: " + err );
            } ).finally( function() {
                if ( !call.delayUntilFormLoads && this.apiCallDeferredQueue.length > 0 ) {
                    this.apiCallDeferredQueue.shift().resolve();
                }
            } );

    }

    private getFormMessageHandler( formname, msg, conversionInfo ) {
        return ( formState ) => {
            var formModel = formState.model;
            var newFormData = msg.forms[formname];
            var newFormProperties = newFormData['']; // form properties
            var newFormConversionInfo = ( conversionInfo && conversionInfo.forms && conversionInfo.forms[formname] ) ? conversionInfo.forms[formname] : undefined;

            if ( newFormProperties ) {
                if ( newFormConversionInfo && newFormConversionInfo[''] ) newFormProperties = this.converterService.convertFromServerToClient( newFormProperties, newFormConversionInfo[''], formModel[''], formState.getScope(), function() { return formModel[''] } );
                if ( !formModel[''] ) formModel[''] = {};
                for ( var p in newFormProperties ) {
                    formModel[''][p] = newFormProperties[p];
                }
            }

            var watchesRemoved = formState.removeWatches ? formState.removeWatches( newFormData ) : false;
            try {
                for ( var beanname in newFormData ) {
                    // copy over the changes, skip for form properties (beanname empty)
                    if ( beanname != '' ) {
                        var newBeanConversionInfo = newFormConversionInfo ? newFormConversionInfo[beanname] : undefined;
                        var beanConversionInfo = newBeanConversionInfo ? this.converterService.getOrCreateInDepthProperty( this.formStatesConversionInfo, formname, beanname ) : this.converterService.getInDepthProperty( this.formStatesConversionInfo, formname, beanname );
                        this.applyBeanData( formModel[beanname], newFormData[beanname], formState.properties.designSize, null /*getChangeNotifierGenerator( formname, beanname ) */, beanConversionInfo, newBeanConversionInfo, formState.getScope ? formState.getScope() : undefined );
                    }
                }
            }
            finally {
                if ( watchesRemoved ) {
                    formState.addWatches( newFormData );
                }
            }
        }
    }

    private applyBeanData( beanModel, beanData, containerSize, changeNotifierGenerator, beanConversionInfo, newConversionInfo, componentScope ) {

        if ( newConversionInfo ) { // then means beanConversionInfo should also be defined - we assume that
            // beanConversionInfo will be granularly updated in the loop below
            // (to not drop other property conversion info when only one property is being applied granularly to the bean)
            beanData = this.converterService.convertFromServerToClient( beanData, newConversionInfo, beanModel, componentScope, function() { return beanModel } );
        }

        for ( var key in beanData ) {
            // remember conversion info for when it will be sent back to server - it might need special conversion as well
            if ( newConversionInfo && newConversionInfo[key] ) {
                // if the value changed and it wants to be in control of it's changes, or if the conversion info for this value changed (thus possibly preparing an old value for being change-aware without changing the value reference)
                //                if ((beanModel[key] !== beanData[key] || beanConversionInfo[key] !== newConversionInfo[key])
                //                    && beanData[key] && beanData[key][$sabloConverters.INTERNAL_IMPL] && beanData[key][$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
                //                    beanData[key][$sabloConverters.INTERNAL_IMPL].setChangeNotifier(changeNotifierGenerator(key));
                //                }
                beanConversionInfo[key] = newConversionInfo[key];
            } else if ( beanConversionInfo && beanConversionInfo[key] ) delete beanConversionInfo[key]; // this prop. no longer has conversion info!

            // also make location and size available in model
            beanModel[key] = beanData[key];
        }
        // if the model had a change notifier call it now after everything is set.
        //        var modelChangeFunction = beanModel[$sabloConstants.modelChangeNotifier];
        //        if (modelChangeFunction) {
        //            for (var key in beanData) {
        //                modelChangeFunction(key, beanModel[key]);
        //            }
        //        }
    }

}