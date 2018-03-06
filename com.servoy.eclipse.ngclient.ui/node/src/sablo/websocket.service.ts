import { Injectable, EventEmitter } from '@angular/core';

import { IntervalObservable } from "rxjs/observable/IntervalObservable";
import { Subscription } from "rxjs/Subscription";


import { ReconnectingWebSocket, WebsocketCustomEvent } from './io/reconnecting.websocket';
import { WindowRefService } from './util/windowref.service'
import { Deferred } from './util/deferred'
import { ServicesService } from './services.service'
import { ConverterService } from './converter.service'

@Injectable()
export class WebsocketService {
    private pathname: string;
    private queryString: string;
    private wsSession: WebsocketSession;
    private wsSessionDeferred: Deferred<WebsocketSession>;
    private connectionArguments = {};
    private lastServerMessageNumber = null;

    constructor( private windowRef: WindowRefService, private services: ServicesService, private converterService: ConverterService ) {
    }

    private generateURL( context, args, queryArgs, websocketUri ) {
        var new_uri;
        if ( this.windowRef.nativeWindow.location.protocol === "https:" ) {
            new_uri = "wss:";
        } else {
            new_uri = "ws:";
        }
        new_uri += "//" + this.windowRef.nativeWindow.location.host;
        var pathname = this.getPathname();
        var lastIndex = pathname.lastIndexOf( "/" );
        if ( lastIndex > 0 ) {
            pathname = pathname.substring( 0, lastIndex );
        }
        if ( context && context.length > 0 ) {
            var lastIndex = pathname.lastIndexOf( context );
            if ( lastIndex >= 0 ) {
                pathname = pathname.substring( 0, lastIndex ) + pathname.substring( lastIndex + context.length )
            }
        }
        new_uri += pathname + ( websocketUri ? websocketUri : '/websocket' );
        for ( var a in args ) {
            if ( args.hasOwnProperty( a ) ) {
                new_uri += '/' + args[a]
            }
        }

        new_uri += "?";

        for ( var a in queryArgs ) {
            if ( queryArgs.hasOwnProperty( a ) ) {
                new_uri += a + "=" + queryArgs[a] + "&";
            }
        }

        if ( this.lastServerMessageNumber != null ) {
            new_uri += "lastServerMessageNumber=" + this.lastServerMessageNumber + "&";
        }

        var queryString = this.getQueryString();
        if ( queryString ) {
            new_uri += queryString;
        }
        else {
            new_uri = new_uri.substring( 0, new_uri.length - 1 );
        }
        return new_uri;
    }

    public connect( context, args, queryArgs, websocketUri ): WebsocketSession {

        this.connectionArguments = {
            context: context,
            args: args,
            queryArgs: queryArgs,
            websocketUri: websocketUri
        }

        // When ReconnectingWebSocket gets a function it will call the function to generate the url for each (re)connect.
        const websocket = new ReconnectingWebSocket(() => {
            return this.generateURL( this.connectionArguments['context'], this.connectionArguments['args'],
                this.connectionArguments['queryArgs'], this.connectionArguments['websocketUri'] );
        } );

        this.wsSession = new WebsocketSession( websocket, this, this.services, this.windowRef, this.converterService );
        // todo should we just merge $websocket and $services into $sablo that just has all
        // the public api of sablo (like connect, conversions, services)
        //$services.setSession(wsSession);
        if ( this.wsSessionDeferred != null ) {
            this.wsSessionDeferred.resolve( this.wsSession );
        }
        return this.wsSession
    }

    // update query arguments for next reconnect-call
    public setConnectionQueryArgument( arg, value ) {
        if ( value != undefined ) {
            if ( !this.connectionArguments['queryArgs'] ) this.connectionArguments['queryArgs'] = {};
            this.connectionArguments['queryArgs'][arg] = value;
        } else if ( this.connectionArguments['queryArgs'] ) {
            this.connectionArguments['queryArgs'].delete( arg );
        }
    }

    public setConnectionPathArguments( args ) {
        this.connectionArguments['args'] = args;
    }

    public getSession(): Promise<WebsocketSession> {
        if ( this.wsSession != null )
            return new Promise<WebsocketSession>(( resolve, reject ) => {
                resolve( this.wsSession );
            } );
        else if ( this.wsSessionDeferred == null ) {
            this.wsSessionDeferred = new Deferred<WebsocketSession>();
        }
        return this.wsSessionDeferred.promise;
    }

    public getURLParameter( name: string ): string {
        return decodeURIComponent(( new RegExp( '[&]?' + name + '=' + '([^&;]+?)(&|#|;|$)' ).exec( this.getQueryString() ) || [, ""] )[1].replace( /\+/g, '%20' ) ) || null
    }

    public setPathname( name: string ) {
        this.pathname = name;
    }

    public getPathname(): string {
        return this.pathname || this.windowRef.nativeWindow.location.pathname;
    }

    public setQueryString( qs: string ) {
        this.queryString = qs;
    }

    public getQueryString(): string {
        if ( this.queryString ) {
            return this.queryString;
        }

        var search = this.windowRef.nativeWindow.location.search;
        if ( search && search.indexOf( '?' ) == 0 ) {
            return search.substring( 1 );
        }

        return search;
    }

    public setLastServerMessageNumber( num: number ) {
        this.lastServerMessageNumber = num;
    }
}


export class WebsocketSession {
    private connected = 'INITIAL';
    private heartbeatMonitor: Subscription = null;
    private lastHeartbeat: number;
    private onOpenHandlers: Array<( evt: WebsocketCustomEvent ) => void> = new Array()
    private onErrorHandlers: Array<( evt: WebsocketCustomEvent ) => void> = new Array()
    private onCloseHandlers: Array<( evt: WebsocketCustomEvent ) => void> = new Array()
    private onMessageObjectHandlers: Array<( msg: any, conversionInfo: any ) => void> = new Array()

    private functionsToExecuteAfterIncommingMessageWasHandled = undefined;

    private deferredEvents: Array<Deferred<any>> = new Array();

    private pendingMessages = undefined

    private currentEventLevelForServer;

    private nextMessageId = 1;


    constructor( private websocket: ReconnectingWebSocket, private websocketService: WebsocketService, private services: ServicesService, private windowRef: WindowRefService, private converterService: ConverterService ) {
        const me = this;
        this.websocket.onopen = ( evt ) => {
            me.setConnected();
            me.startHeartbeat();
            for ( let handler in me.onOpenHandlers ) {
                me.onOpenHandlers[handler]( evt );
            }
        }
        this.websocket.onerror = ( evt ) => {
            me.stopHeartbeat();
            for ( let handler in me.onErrorHandlers ) {
                me.onErrorHandlers[handler]( evt );
            }
        }
        this.websocket.onclose = ( evt ) => {
            me.stopHeartbeat();
            if ( me.connected != 'CLOSED' ) {
                me.connected = 'RECONNECTING';
                //                    if ($log.debugLevel === $log.SPAM) $log.debug("sbl * Connection mode (onclose receidev while not CLOSED): ... RECONNECTING (" + new Date().getTime() + ")");
            }
            for ( var handler in me.onCloseHandlers ) {
                me.onCloseHandlers[handler]( evt );
            }
        }
        this.websocket.onconnecting = ( evt ) => {
            // this event indicates we are trying to reconnect, the event has the close code and reason from the disconnect.
            if ( evt.code && evt.code != WsCloseCodes.CLOSED_ABNORMALLY && evt.code != WsCloseCodes.SERVICE_RESTART ) {

                me.websocket.close();

                if ( evt.reason == 'CLIENT-OUT-OF-SYNC' ) {
                    // Server detected that we are out-of-sync, reload completely
                    me.windowRef.nativeWindow.location.reload();
                    return;
                }

                // server disconnected, do not try to reconnect
                me.connected = 'CLOSED';
                //                    if ($log.debugLevel === $log.SPAM) $log.debug("sbl * Connection mode (onconnecting got a server disconnect/close with reason " + evt.reason + "): ... CLOSED (" + new Date().getTime() + ")");
            }
        }
        this.websocket.onmessage = ( message ) => {
            me.handleHeartbeat( message ) || me.handleMessage( message );
        }
    }
    // api
    public callService( serviceName, methodName, argsObject, async ) {
        var cmd = {
            service: serviceName,
            methodname: methodName,
            args: argsObject
        };
        if ( async ) {
            this.sendMessageObject( cmd );
        }
        else {
            var deferred = new Deferred<any>();
            var cmsgid = this.getNextMessageId()
            this.deferredEvents[cmsgid] = deferred
            //                $sabloLoadingIndicator.showLoading();
            cmd['cmsgid'] = cmsgid
            this.sendMessageObject( cmd )
            return deferred.promise;
        }
    }

    public sendMessageObject( obj ) {

        if ( this.getCurrentEventLevelForServer() ) {
            obj.prio = this.getCurrentEventLevelForServer();
        }
        var msg = JSON.stringify( obj )
        if ( this.isConnected() ) {
            //            if ($log.debugLevel === $log.SPAM) $log.debug("sbl * Sending message to server: " + msg);
            this.websocket.send( msg )
        }
        else {
            //            if ($log.debugLevel === $log.SPAM) $log.debug("sbl * Disconnected; will add the following to pending messages to be sent to server: " + msg);
            this.pendingMessages = this.pendingMessages || []
            this.pendingMessages.push( msg )
        }

    }

    public onopen( handler ) {
        this.onOpenHandlers.push( handler )
    }
    public onerror( handler ) {
        this.onErrorHandlers.push( handler )
    }
    public onclose( handler ) {
        this.onCloseHandlers.push( handler )
    }
    public onMessageObject( handler: ( message: any, conversionInfo: any ) => void ) {
        this.onMessageObjectHandlers.push( handler )
    }

    public isConnected() {
        return this.connected == 'CONNECTED';
    }

    public isReconnecting() {
        return this.connected == 'RECONNECTING';
    }

    public disconnect() {
        if ( this.websocket ) {
            this.websocket.close();
            this.connected = 'CLOSED';
            //            if ($log.debugLevel === $log.SPAM) $log.debug("sbl * Connection mode (disconnect): ... CLOSED (" + new Date().getTime() + ")");
        }
    }

    // eventLevelValue can be undefined for DEFAULT
    public setCurrentEventLevelForServer( eventLevelValue ) {
        this.currentEventLevelForServer = eventLevelValue;
    }

    public getCurrentEventLevelForServer() {
        return this.currentEventLevelForServer;
    }

    public addIncomingMessageHandlingDoneTask( func: () => any ) {
        if ( this.functionsToExecuteAfterIncommingMessageWasHandled ) this.functionsToExecuteAfterIncommingMessageWasHandled.push( func );
        else func(); // will not addPostIncommingMessageHandlingTask while not handling an incoming message; the task can execute right away then (maybe it was called due to a change detected in a watch instead of property listener)
    }

    private setConnected() {
        this.connected = 'CONNECTED';
        //        if ($log.debugLevel === $log.SPAM) $log.debug("sbl * Connection mode: ... CONNECTED (" + new Date().getTime() + ")");

        if ( this.pendingMessages ) {
            for ( let i in this.pendingMessages ) {
                //                if ($log.debugLevel === $log.SPAM) $log.debug("sbl * Connected; sending pending message to server: " + pendingMessages[i]);
                this.websocket.send( this.pendingMessages[i] )
            }
            this.pendingMessages = undefined
        }
    }

    private startHeartbeat() {
        if ( this.heartbeatMonitor == null ) {
            //            if ($log.debugLevel === $log.SPAM) $log.debug("sbl * Starting heartbeat... (" + new Date().getTime() + ")");

            this.lastHeartbeat = new Date().getTime();
            this.heartbeatMonitor = IntervalObservable.create( 4000 ).subscribe(() => {
                //                if ($log.debugLevel === $log.SPAM) $log.debug("sbl * Sending heartbeat... (" + new Date().getTime() + ")");
                if ( new Date().getTime() - this.lastHeartbeat >= 4000 ) {
                    this.websocket.send( "P" ); // ping
                    if ( this.isConnected() && new Date().getTime() - this.lastHeartbeat > 8000 ) {
                        // no response within 8 seconds
                        if ( this.connected !== 'RECONNECTING' ) {
                            //                            if ($log.debugLevel === $log.SPAM) $log.debug("sbl * Connection mode (Heartbeat timed out; connection lost; waiting to reconnect): ... RECONNECTING (" + new Date().getTime() + ")");
                            this.connected = 'RECONNECTING';
                        }
                    }
                }
            } )
        }
    }

    private stopHeartbeat() {
        if ( this.heartbeatMonitor != null ) {
            //            if ($log.debugLevel === $log.SPAM) $log.debug("sbl * Stopping heartbeat... (" + new Date().getTime() + ")");
            this.heartbeatMonitor.unsubscribe();
            this.heartbeatMonitor = undefined;
        }
    }

    private getNextMessageId() {
        return this.nextMessageId++;
    }

    private handleHeartbeat( message ) {
        //        if ($log.debugLevel === $log.SPAM) $log.debug("sbl * Received heartbeat... (" + new Date().getTime() + ")");
        this.lastHeartbeat = new Date().getTime(); // something is received, the server connection is up
        if ( this.isReconnecting() ) {
            //            if ($log.debugLevel === $log.SPAM) $log.debug("sbl * Heartbeat received, connection re-established...");
            this.setConnected();
        }
        if ( message.data == "P" ) {
            this.websocket.send( "p" );
        }
        return message.data == "p" || message.data == "P"; // pong or ping
    }

    private handleMessage( message ) {
        var obj
        var responseValue
        this.functionsToExecuteAfterIncommingMessageWasHandled = [];

        try {
            //            if ($log.debugLevel === $log.SPAM) $log.debug("sbl * Received message from server: " + JSON.stringify(message));

            var message_data = message.data;
            var separator = message_data.indexOf( '#' );
            if ( separator >= 0 && separator < 5 ) {
                // the json is prefixed with a message number: 123#{bla: "hello"}
                this.websocketService.setLastServerMessageNumber( message_data.substring( 0, separator ) );
                message_data = message_data.substr( separator + 1 );
            }
            // else message has no seq-no

            obj = JSON.parse( message_data );

            if ( obj.services ) {
                // services call, first process the once with the flag 'apply_first'
                if ( obj[ConverterService.TYPES_KEY] && obj[ConverterService.TYPES_KEY].services ) {
                    obj.services = this.converterService.convertFromServerToClient( obj.services, obj[ConverterService.TYPES_KEY].services, undefined, undefined, undefined )
                }
                for ( var index in obj.services ) {
                    var service = obj.services[index];
                    if ( service['pre_data_service_call'] ) {
                        // responseValue keeps last services call return value
                        responseValue = this.services.callServiceApi( service );
                    }
                }
            }

            //            // if the indicator is showing and this object wants a return message then hide the indicator until we send the response
            //            var hideIndicator = obj && obj.smsgid && $sabloLoadingIndicator.isShowing();
            //            // if a request to a service is being done then this could be a blocking 
            //            if (hideIndicator) {
            //                $sabloLoadingIndicator.hideLoading();
            //            }

            // data got back from the server
            if ( obj.cmsgid ) { // response to event
                var deferredEvent = this.deferredEvents[obj.cmsgid];
                if ( deferredEvent != null ) {
                    if ( obj.exception ) {
                        // something went wrong
                        if ( obj[ConverterService.TYPES_KEY] && obj[ConverterService.TYPES_KEY].exception ) {
                            obj.exception = this.converterService.convertFromServerToClient( obj.exception, obj[ConverterService.TYPES_KEY].exception, undefined, undefined, undefined )
                        }
                        deferredEvent.reject( obj.exception );
                    } else {
                        if ( obj[ConverterService.TYPES_KEY] && obj[ConverterService.TYPES_KEY].ret ) {
                            obj.ret = this.converterService.convertFromServerToClient( obj.ret, obj[ConverterService.TYPES_KEY].ret, undefined, undefined, undefined )
                        }
                        deferredEvent.resolve( obj.ret );
                    }
                }
                //                else $log.warn("Response to an unknown handler call dismissed; can happen (normal) if a handler call gets interrupted by a full browser refresh.");
                delete this.deferredEvents[obj.cmsgid];
                //                $sabloTestability.testEvents();
                //                $sabloLoadingIndicator.hideLoading();
            }

            // message
            if ( obj.msg ) {
                for ( var handler in this.onMessageObjectHandlers ) {
                    var ret = this.onMessageObjectHandlers[handler]( obj.msg, obj[ConverterService.TYPES_KEY] ? obj[ConverterService.TYPES_KEY].msg : undefined )
                    if ( ret ) responseValue = ret;

                    //                    if ($log.debugLevel === $log.SPAM) $log.debug("sbl * Checking if any form scope changes need to be digested (obj.msg).");
                }
            }

            if ( obj.msg && obj.msg.services ) {
                this.services.updateServiceScopes( obj.msg.services, ( obj[ConverterService.TYPES_KEY] && obj[ConverterService.TYPES_KEY].msg ) ? obj[ConverterService.TYPES_KEY].msg.services : undefined );
            }

            if ( obj.services ) {
                // normal services call
                for ( var index in obj.services ) {
                    var service = obj.services[index];
                    if ( !service['pre_data_service_call'] ) {
                        // responseValue keeps last services call return value
                        responseValue = this.services.callServiceApi( service );
                    }
                }
            }

            // delayed calls
            if ( obj.calls ) {
                for ( var i = 0; i < obj.calls.length; i++ ) {
                    for ( var handler in this.onMessageObjectHandlers ) {
                        this.onMessageObjectHandlers[handler]( obj.calls[i], ( obj[ConverterService.TYPES_KEY] && obj[ConverterService.TYPES_KEY].calls ) ? obj[ConverterService.TYPES_KEY].calls[i] : undefined );
                    }

                    //                    if ($log.debugLevel === $log.SPAM) $log.debug("sbl * Checking if any (obj.calls) form scopes changes need to be digested (obj.calls).");
                }
            }
            if ( obj && obj.smsgid ) {
                //                if (isPromiseLike(responseValue)) {
                //                    if ($log.debugEnabled) $log.debug("sbl * Call from server with smsgid '" + obj.smsgid + "' returned a promise; will wait for it to get resolved.");
                //                    
                //                    // the server wants a response, this could be a promise so a dialog could be shown
                //                    // then just let protractor go through.
                //                    $sabloTestability.increaseEventLoop();
                //                }
                // server wants a response; responseValue may be a promise
                Promise.resolve( responseValue ).then(( ret ) => {
                    //                    if (isPromiseLike(responseValue)) {
                    //                        $sabloTestability.decreaseEventLoop();
                    //                        if ($log.debugEnabled) $log.debug("sbl * Promise returned by call from server with smsgid '" + obj.smsgid + "' is now resolved with value: -" + ret + "-. Sending value back to server...");
                    //                    } 
                    //                    else if ($log.debugEnabled) $log.debug("sbl * Call from server with smsgid '" + obj.smsgid + "' returned: -" + ret + "-. Sending value back to server...");

                    // success
                    var response = {
                        smsgid: obj.smsgid
                    }
                    if ( ret != undefined ) {
                        response['ret'] = this.converterService.convertClientObject( ret );
                    }
                    //                    if (hideIndicator) {
                    //                        $sabloLoadingIndicator.showLoading();
                    //                    }
                    this.sendMessageObject( response );
                }, ( reason ) => {
                    //                    if (isPromiseLike(responseValue)) $sabloTestability.decreaseEventLoop();
                    // error
                    //                    $log.error("Error (follows below) in parsing/processing this message with smsgid '" + obj.smsgid + "' (async): " + message_data);
                    //                    $log.error(reason);
                    // server wants a response; send failure so that browser side script doesn't hang
                    var response = {
                        smsgid: obj.smsgid,
                        err: "Error while executing ($q deferred) client side code. Please see browser console for more info. Error: " + reason
                    }
                    //                    if (hideIndicator) {
                    //                        $sabloLoadingIndicator.showLoading();
                    //                    }
                    this.sendMessageObject( response );
                } );
            }
        } catch ( e ) {
            console.log( e );
            //            $log.error("Error (follows below) in parsing/processing this message: " + message_data);
            //            $log.error(e);
            if ( obj && obj.smsgid ) {
                // server wants a response; send failure so that browser side script doesn't hang
                var response = {
                    smsgid: obj.smsgid,
                    err: "Error while executing client side code. Please see browser console for more info. Error: " + e
                }
                //                if (hideIndicator) {
                //                    $sabloLoadingIndicator.showLoading();
                //                }
                this.sendMessageObject( response );
            }
        } finally {
            var err;
            for ( var i = 0; i < this.functionsToExecuteAfterIncommingMessageWasHandled.length; i++ ) {
                try {
                    this.functionsToExecuteAfterIncommingMessageWasHandled[i]();
                } catch ( e ) {
                    //                    $log.error("Error (follows below) in executing PostIncommingMessageHandlingTask: " + this.functionsToExecuteAfterIncommingMessageWasHandled[i]);
                    //                    $log.error(e);
                    err = e;
                }
            }
            this.functionsToExecuteAfterIncommingMessageWasHandled = undefined;
            if ( err ) throw err;
        }
    }


}

class WsCloseCodes {
    static readonly NORMAL_CLOSURE = 1000; // indicates a normal closure, meaning that the purpose for which the connection was established has been fulfilled.
    static readonly GOING_AWAY: 1001; // indicates that an endpoint is "going away", such as a server going down or a browser having navigated away from a page.
    static readonly PROTOCOL_ERROR: 1002; // indicates that an endpoint is terminating the connection due to a protocol error.
    static readonly CANNOT_ACCEPT: 1003; // indicates that an endpoint is terminating the connection because it has received a type of data it cannot accept (e.g., an endpoint that understands only text data MAY send this if it receives a binary message).
    static readonly NO_STATUS_CODE: 1005; // is a reserved value and MUST NOT be set as a status code in a Close control frame by an endpoint.
    static readonly CLOSED_ABNORMALLY: 1006; // is a reserved value and MUST NOT be set as a status code in a Close control frame by an endpoint.
    static readonly NOT_CONSISTENT: 1007; // indicates that an endpoint is terminating the connection because it has received data within a message that was not consistent with the type of the message (e.g., non-UTF-8 data within a text message).
    static readonly VIOLATED_POLICY: 1008; // indicates that an endpoint is terminating the connection because it has received a message that violates its policy.
    static readonly TOO_BIG: 1009; // indicates that an endpoint is terminating the connection because it has received a message that is too big for it to process.
    static readonly NO_EXTENSION: 1010; // indicates that an endpoint (client) is terminating the connection because it has expected the server to negotiate one or more extension, but the server didn't return them in the response message of the WebSocket handshake.
    static readonly UNEXPECTED_CONDITION: 1011; // indicates that a server is terminating the connection because it encountered an unexpected condition that prevented it from fulfilling the request.
    static readonly SERVICE_RESTART: 1012; // indicates that the service will be restarted.
    static readonly TLS_HANDSHAKE_FAILURE: 1015; // is a reserved value and MUST NOT be set as a status code in a Close control frame by an endpoint.
    static readonly TRY_AGAIN_LATER: 1013; // indicates that the service is experiencing overload
}

