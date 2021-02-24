import { Injectable, EventEmitter, NgZone } from '@angular/core';

import { Subscription, interval, Subject } from 'rxjs';


import { ReconnectingWebSocket, WebsocketCustomEvent } from './io/reconnecting.websocket';
import { WindowRefService } from './util/windowref.service';
import { Deferred } from './util/deferred';
import { ServicesService } from './services.service';
import { ConverterService } from './converter.service';
import { LoggerService, LogLevel, LoggerFactory } from './logger.service';
import { LoadingIndicatorService } from './util/loading-indicator/loading-indicator.service';

@Injectable()
export class WebsocketService {
    public reconnectingEmitter = new Subject<boolean>();

    private pathname: string;
    private queryString: string;
    private wsSession: WebsocketSession;
    private wsSessionDeferred: Deferred<WebsocketSession>;
    private connectionArguments = {};
    private lastServerMessageNumber = null;
    private log: LoggerService;

    constructor(private windowRef: WindowRefService,
        private services: ServicesService,
        private converterService: ConverterService,
        private logFactory: LoggerFactory,
        private loadingIndicatorService: LoadingIndicatorService,
        private ngZone: NgZone) {
        this.log = logFactory.getLogger('WebsocketService');
    }

    public connect(context, args, queryArgs, websocketUri): WebsocketSession {

        this.connectionArguments = {
            context,
            args,
            queryArgs,
            websocketUri
        };

        this.ngZone.runOutsideAngular(() => {
            // When ReconnectingWebSocket gets a function it will call the function to generate the url for each (re)connect.
            const websocket = new ReconnectingWebSocket(() => this.generateURL(this.connectionArguments['context'], this.connectionArguments['args'],
                this.connectionArguments['queryArgs'], this.connectionArguments['websocketUri']), this.logFactory);

            this.wsSession = new WebsocketSession(websocket, this, this.services, this.windowRef, this.converterService, this.logFactory, this.loadingIndicatorService, this.ngZone);
            // todo should we just merge $websocket and $services into $sablo that just has all
            // the public api of sablo (like connect, conversions, services)
        });
        //$services.setSession(wsSession);
        if (this.wsSessionDeferred != null) {
            this.wsSessionDeferred.resolve(this.wsSession);
        }
        return this.wsSession;
    }

    // update query arguments for next reconnect-call
    public setConnectionQueryArgument(arg, value) {
        if (value !== undefined) {
            if (!this.connectionArguments['queryArgs']) this.connectionArguments['queryArgs'] = {};
            this.connectionArguments['queryArgs'][arg] = value;
        } else if (this.connectionArguments['queryArgs']) {
            this.connectionArguments['queryArgs'].delete(arg);
        }
    }

    public setConnectionPathArguments(args) {
        this.connectionArguments['args'] = args;
    }

    public getSession(): Promise<WebsocketSession> {
        if (this.wsSession != null)
            return new Promise<WebsocketSession>((resolve, reject) => {
                resolve(this.wsSession);
            });
        else if (this.wsSessionDeferred == null) {
            this.wsSessionDeferred = new Deferred<WebsocketSession>();
        }
        return this.wsSessionDeferred.promise;
    }

    public getURLParameter(name: string): string {
        return decodeURIComponent((new RegExp('[&]?' + name + '=' + '([^&;]+?)(&|#|;|$)').exec(this.getQueryString()) || [, ''])[1].replace(/\+/g, '%20')) || null;
    }

    public setPathname(name: string) {
        this.pathname = name;
    }

    public getPathname(): string {
        return this.pathname || this.windowRef.nativeWindow.location.pathname;
    }

    public setQueryString(qs: string) {
        this.queryString = qs;
    }

    public getQueryString(): string {
        if (this.queryString) {
            return this.queryString;
        }

        const search = this.windowRef.nativeWindow.location.search;
        if (search && search.indexOf('?') === 0) {
            return search.substring(1);
        }

        return search;
    }

    public setLastServerMessageNumber(num: number) {
        this.lastServerMessageNumber = num;
    }


    public isConnected() {
        return this.wsSession.isConnected();
    }

    private generateURL(context, args, queryArgs, websocketUri) {
        let new_uri: string;
        if (this.windowRef.nativeWindow.location.protocol === 'https:') {
            new_uri = 'wss:';
        } else {
            new_uri = 'ws:';
        }
        new_uri += '//' + this.windowRef.nativeWindow.location.host;
        let pathname = this.getPathname();
        let lastIndex = pathname.lastIndexOf('/');
        if (lastIndex > 0) {
            pathname = pathname.substring(0, lastIndex);
        }
        if (context && context.length > 0) {
            lastIndex = pathname.lastIndexOf(context);
            if (lastIndex >= 0) {
                pathname = pathname.substring(0, lastIndex) + pathname.substring(lastIndex + context.length);
            }
        }
        new_uri += pathname + (websocketUri ? websocketUri : '/websocket');
        for (const a of Object.keys(args)) {
            if (args.hasOwnProperty(a)) {
                new_uri += '/' + args[a];
            }
        }

        new_uri += '?connectNr=' + Math.floor((Math.random() * 10000000000000)) + '&';

        for (const a of Object.keys(queryArgs)) {
            if (queryArgs.hasOwnProperty(a)) {
                new_uri += a + '=' + queryArgs[a] + '&';
            }
        }

        if (this.lastServerMessageNumber != null) {
            new_uri += 'lastServerMessageNumber=' + this.lastServerMessageNumber + '&';
        }

        let queryString = this.getQueryString();
        if (queryString) {
            const index = queryString.indexOf(WebsocketConstants.CLEAR_SESSION_PARAM);
            if (index >= 0) {
                const params_arr = queryString.split('&');
                for (let i = params_arr.length - 1; i >= 0; i -= 1) {
                    if (params_arr[i].indexOf(WebsocketConstants.CLEAR_SESSION_PARAM) === 0) {
                        params_arr.splice(i, 1);
                    }
                }
                queryString = params_arr.join('&');
            }
            new_uri += queryString;
        } else {
            new_uri = new_uri.substring(0, new_uri.length - 1);
        }
        return new_uri;
    }
}


export class WebsocketSession {
    private connected = 'INITIAL';
    private heartbeatMonitor: Subscription = null;
    private lastHeartbeat: number;
    private onOpenHandlers: Array<(evt: WebsocketCustomEvent) => void> = new Array();
    private onErrorHandlers: Array<(evt: WebsocketCustomEvent) => void> = new Array();
    private onCloseHandlers: Array<(evt: WebsocketCustomEvent) => void> = new Array();
    private onMessageObjectHandlers: Array<(msg: any, conversionInfo: any) => any> = new Array();

    private functionsToExecuteAfterIncommingMessageWasHandled = undefined;

    private deferredEvents: Array<Deferred<any>> = new Array();

    private pendingMessages = undefined;

    private currentEventLevelForServer;

    private nextMessageId = 1;
    private log: LoggerService;


    constructor(private websocket: ReconnectingWebSocket,
        private websocketService: WebsocketService,
        private services: ServicesService,
        private windowRef: WindowRefService,
        private converterService: ConverterService,
        logFactory: LoggerFactory,
        private loadingIndicatorService: LoadingIndicatorService,
        private ngZone: NgZone) {
        this.log = logFactory.getLogger('WebsocketSession');
        this.websocket.onopen = (evt) => {
            this.setConnected();
            this.startHeartbeat();
            for (const handler of Object.keys( this.onOpenHandlers)) {
                this.onOpenHandlers[handler](evt);
            }
        };
        this.websocket.onerror = (evt) => {
            this.stopHeartbeat();
            for (const handler of Object.keys( this.onErrorHandlers)) {
                this.onErrorHandlers[handler](evt);
            }
        };
        this.websocket.onclose = (evt) => {
            this.stopHeartbeat();
            if (this.connected !== 'CLOSED') {
                this.connected = 'RECONNECTING';
                this.websocketService.reconnectingEmitter.next(true);
                this.log.spam(this.log.buildMessage(() => ('sbl * Connection mode (onclose receidev while not CLOSED): ... RECONNECTING (' + new Date().getTime() + ')')));
            }
            for (const handler of Object.keys(this.onCloseHandlers)) {
                this.onCloseHandlers[handler](evt);
            }
        };
        this.websocket.onconnecting = (evt) => {
            // this event indicates we are trying to reconnect, the event has the close code and reason from the disconnect.
            if (evt.code && evt.code !== WsCloseCodes.CLOSED_ABNORMALLY && evt.code !== WsCloseCodes.SERVICE_RESTART) {

                this.websocket.close();

                if (evt.reason === 'CLIENT-OUT-OF-SYNC') {
                    // Server detected that we are out-of-sync, reload completely
                    this.windowRef.nativeWindow.location.reload();
                    return;
                }

                // server disconnected, do not try to reconnect
                this.connected = 'CLOSED';
                this.log.spam(this.log.buildMessage(() => ('sbl * Connection mode (onconnecting got a server disconnect/close with reason '
                    + evt.reason + '): ... CLOSED (' + new Date().getTime() + ')')));
            }
        };
        this.websocket.onmessage = (message) => this.handleHeartbeat(message) || this.ngZone.run(() => this.handleMessage(message));
    }
    // api
    public callService(serviceName: string, methodName: string, argsObject, async: boolean): Promise<any> {
        const cmd = {
            service: serviceName,
            methodname: methodName,
            args: argsObject
        };
        if (async) {
            this.sendMessageObject(cmd);
        } else {
            const deferred = new Deferred<any>();
            const cmsgid = this.getNextMessageId();
            this.deferredEvents[cmsgid] = deferred;
            this.loadingIndicatorService.showLoading();
            cmd['cmsgid'] = cmsgid;
            this.sendMessageObject(cmd);
            return deferred.promise;
        }
    }

    public sendMessageObject(obj) {

        if (this.getCurrentEventLevelForServer()) {
            obj.prio = this.getCurrentEventLevelForServer();
        }
        const msg = JSON.stringify(obj);
        if (this.isConnected()) {
            this.log.spam(this.log.buildMessage(() => ('sbl * Sending message to server: ' + msg)));
            this.websocket.send(msg);
        } else {
            this.log.spam(this.log.buildMessage(() => ('sbl * Disconnected; will add the following to pending messages to be sent to server: ' + msg)));
            this.pendingMessages = this.pendingMessages || [];
            this.pendingMessages.push(msg);
        }

    }

    public onopen(handler) {
        this.onOpenHandlers.push(handler);
    }
    public onerror(handler) {
        this.onErrorHandlers.push(handler);
    }
    public onclose(handler) {
        this.onCloseHandlers.push(handler);
    }
    public onMessageObject(handler: (message: any, conversionInfo: any) => void) {
        this.onMessageObjectHandlers.push(handler);
    }

    public isConnected() {
        return this.connected === 'CONNECTED';
    }

    public isReconnecting() {
        return this.connected === 'RECONNECTING';
    }

    public disconnect() {
        if (this.websocket) {
            this.websocket.close();
            this.connected = 'CLOSED';
            this.log.spam(this.log.buildMessage(() => ('sbl * Connection mode (disconnect): ... CLOSED (' + new Date().getTime() + ')')));
        }
    }

    // eventLevelValue can be undefined for DEFAULT
    public setCurrentEventLevelForServer(eventLevelValue) {
        this.currentEventLevelForServer = eventLevelValue;
    }

    public getCurrentEventLevelForServer() {
        return this.currentEventLevelForServer;
    }

    public addIncomingMessageHandlingDoneTask(func: () => any) {
        if (this.functionsToExecuteAfterIncommingMessageWasHandled) this.functionsToExecuteAfterIncommingMessageWasHandled.push(func);
        else func(); // will not addPostIncommingMessageHandlingTask while not handling an incoming message;
                     // the task can execute right away then (maybe it was called due to a change detected in a watch instead of property listener)
    }

    private setConnected() {
        this.connected = 'CONNECTED';
        this.log.spam(this.log.buildMessage(() => ('sbl * Connection mode: ... CONNECTED (' + new Date().getTime() + ')')));

        if (this.pendingMessages) {
            for (const i of Object.keys(this.pendingMessages)) {
                this.log.spam(this.log.buildMessage(() => ('sbl * Connected; sending pending message to server: ' + this.pendingMessages[i])));
                this.websocket.send(this.pendingMessages[i]);
            }
            this.pendingMessages = undefined;
        }
    }

    private startHeartbeat() {
        if (this.heartbeatMonitor == null) {
            this.log.spam(this.log.buildMessage(() => ('sbl * Starting heartbeat... (' + new Date().getTime() + ')')));

            this.lastHeartbeat = new Date().getTime();
            this.heartbeatMonitor = interval(4000).subscribe(() => {
                this.log.spam(this.log.buildMessage(() => ('sbl * Sending heartbeat... (' + new Date().getTime() + ')')));
                if (new Date().getTime() - this.lastHeartbeat >= 4000) {
                    this.websocket.send('P'); // ping
                    if (this.isConnected() && new Date().getTime() - this.lastHeartbeat > 8000) {
                        // no response within 8 seconds
                        if (this.connected !== 'RECONNECTING') {
// this.log.spam(this.log.buildMessage(() => ("sbl * Connection mode (Heartbeat timed out; connection lost; waiting to reconnect): ... RECONNECTING (" + new Date().getTime() + ")")));
                            this.ngZone.run(() => this.connected = 'RECONNECTING');
                        }
                    }
                }
            });
        }
    }

    private stopHeartbeat() {
        if (this.heartbeatMonitor != null) {
            this.log.spam(this.log.buildMessage(() => ('sbl * Stopping heartbeat... (' + new Date().getTime() + ')')));
            this.heartbeatMonitor.unsubscribe();
            this.heartbeatMonitor = undefined;
        }
    }

    private getNextMessageId() {
        return this.nextMessageId++;
    }

    private handleHeartbeat(message) {
        this.log.spam(this.log.buildMessage(() => ('sbl * Received heartbeat... (' + new Date().getTime() + ')')));
        this.lastHeartbeat = new Date().getTime(); // something is received, the server connection is up
        if (this.isReconnecting()) {
            this.log.spam('sbl * Heartbeat received, connection re-established...');
            this.setConnected();
        }
        if (message.data === 'P') {
            this.websocket.send('p');
        }
        return message.data === 'p' || message.data === 'P'; // pong or ping
    }

    private handleMessage(message) {
        let obj;
        let responseValue;
        this.functionsToExecuteAfterIncommingMessageWasHandled = [];

        let message_data = message.data;
        let hideIndicator = false;
        try {
            this.log.spam(this.log.buildMessage(() => ('sbl * Received message from server: ' + JSON.stringify(message))));

            const separator = message_data.indexOf('#');
            if (separator >= 0 && separator < 5) {
                // the json is prefixed with a message number: 123#{bla: "hello"}
                this.websocketService.setLastServerMessageNumber(message_data.substring(0, separator));
                message_data = message_data.substr(separator + 1);
            }
            // else message has no seq-no

            obj = JSON.parse(message_data);

            if (obj.services) {
                // services call, first process the once with the flag 'apply_first'
                if (obj[ConverterService.TYPES_KEY] && obj[ConverterService.TYPES_KEY].services) {
                    obj.services = this.converterService.convertFromServerToClient(obj.services, obj[ConverterService.TYPES_KEY].services, undefined, undefined);
                }
                // eslint-disable-next-line guard-for-in
                for (const index of Object.keys(obj.services)) {
                    const srv = obj.services[index];
                    if (srv['pre_data_service_call']) {
                        // responseValue keeps last services call return value
                        responseValue = this.services.callServiceApi(srv);
                    }
                }
            }

            // if the indicator is showing and this object wants a return message then hide the indicator until we send the response
            hideIndicator = obj && obj.smsgid && this.loadingIndicatorService.isShowing();
            // if a request to a service is being done then this could be a blocking
            if (hideIndicator) {
                this.loadingIndicatorService.hideLoading();
            }

            // data got back from the server
            if (obj.cmsgid) { // response to event
                const deferredEvent = this.deferredEvents[obj.cmsgid];
                if (deferredEvent !== null) {
                    if (obj.exception) {
                        // something went wrong
                        if (obj[ConverterService.TYPES_KEY] && obj[ConverterService.TYPES_KEY].exception) {
                            obj.exception = this.converterService.convertFromServerToClient(obj.exception, obj[ConverterService.TYPES_KEY].exception, undefined, undefined);
                        }
                        deferredEvent.reject(obj.exception);
                    } else {
                        if (obj[ConverterService.TYPES_KEY] && obj[ConverterService.TYPES_KEY].ret) {
                            obj.ret = this.converterService.convertFromServerToClient(obj.ret, obj[ConverterService.TYPES_KEY].ret, undefined, undefined);
                        }
                        deferredEvent.resolve(obj.ret);
                    }
                } else this.log.warn('Response to an unknown handler call dismissed; can happen (normal) if a handler call gets interrupted by a full browser refresh.');
                delete this.deferredEvents[obj.cmsgid];
                //                $sabloTestability.testEvents();
                this.loadingIndicatorService.hideLoading();
            }

            // message
            if (obj.msg) {
                // eslint-disable-next-line guard-for-in
                for (const handler of Object.keys(this.onMessageObjectHandlers)) {
                    const ret = this.onMessageObjectHandlers[handler](obj.msg, obj[ConverterService.TYPES_KEY] ? obj[ConverterService.TYPES_KEY].msg : undefined);
                    if (ret) responseValue = ret;

                    this.log.spam('sbl * Checking if any form scope changes need to be digested (obj.msg).');
                }
            }

            if (obj.msg && obj.msg.services) {
                this.services.updateServiceScopes(obj.msg.services,
                    (obj[ConverterService.TYPES_KEY] && obj[ConverterService.TYPES_KEY].msg) ? obj[ConverterService.TYPES_KEY].msg.services : undefined);
            }

            if (obj.services) {
                // normal services call
                // eslint-disable-next-line guard-for-in
                for (const index of Object.keys(obj.services)) {
                    const srv = obj.services[index];
                    if (!srv['pre_data_service_call']) {
                        // responseValue keeps last services call return value
                        responseValue = this.services.callServiceApi(srv);
                    }
                }
            }

            // delayed calls
            if (obj.calls) {
                for (const i of Object.keys(obj.calls)) {
                    // eslint-disable-next-line guard-for-in
                    for (const handler of Object.keys(this.onMessageObjectHandlers)) {
                        this.onMessageObjectHandlers[handler](obj.calls[i],
                            (obj[ConverterService.TYPES_KEY] && obj[ConverterService.TYPES_KEY].calls) ? obj[ConverterService.TYPES_KEY].calls[i] : undefined);
                    }

                    this.log.spam('sbl * Checking if any (obj.calls) form scopes changes need to be digested (obj.calls).');
                }
            }
            if (obj && obj.smsgid) {
                //                if (isPromiseLike(responseValue)) {
                //                    if (this.log.debugEnabled) this.log.debug(this.log.buildMessage(() =>
                //                  ("sbl * Call from server with smsgid '" + obj.smsgid + "' returned a promise; will wait for it to get resolved.")));
                //
                //                    // the server wants a response, this could be a promise so a dialog could be shown
                //                    // then just let protractor go through.
                //                    $sabloTestability.increaseEventLoop();
                //                }
                // server wants a response; responseValue may be a promise
                Promise.resolve(responseValue).then((ret) => {
                    //                    if (isPromiseLike(responseValue)) {
                    //                        $sabloTestability.decreaseEventLoop();
                    //                        this.log.debug(this.log.buildMessage(() =>
                    //              ("sbl * Promise returned by call from server with smsgid '" + obj.smsgid + "' is now resolved with value: -" + ret + "-. Sending value back to server...")));
                    //                    }
                    //                    else this.log.debug(this.log.buildMessage(() =>
                    //              ("sbl * Call from server with smsgid '" + obj.smsgid + "' returned: -" + ret + "-. Sending value back to server...")));

                    // success
                    const response = {
                        smsgid: obj.smsgid
                    };
                    if (ret !== undefined) {
                        response['ret'] = this.converterService.convertClientObject(ret);
                    }
                    if (hideIndicator) {
                        this.loadingIndicatorService.showLoading();
                    }
                    this.sendMessageObject(response);
                }, (reason) => {
                    //                    if (isPromiseLike(responseValue)) $sabloTestability.decreaseEventLoop();
                    // error
                    //                    this.log.error(this.log.buildMessage(() =>
                    //          ("Error (follows below) in parsing/processing this message with smsgid '" + obj.smsgid + "' (async): " + message_data)));
                    //                    this.log.error(reason);
                    // server wants a response; send failure so that browser side script doesn't hang
                    const response = {
                        smsgid: obj.smsgid,
                        err: 'Error while executing ($q deferred) client side code. Please see browser console for more info. Error: ' + reason
                    };
                    if (hideIndicator) {
                        this.loadingIndicatorService.showLoading();
                    }
                    this.sendMessageObject(response);
                });
            }
        } catch (e) {
            this.log.error(this.log.buildMessage(() => ('Error (follows below) in parsing/processing this message: ' + message_data)));
            this.log.error(e);
            if (obj && obj.smsgid) {
                // server wants a response; send failure so that browser side script doesn't hang
                const response = {
                    smsgid: obj.smsgid,
                    err: 'Error while executing client side code. Please see browser console for more info. Error: ' + e
                };
                if (hideIndicator) {
                    this.loadingIndicatorService.showLoading();
                }
                this.sendMessageObject(response);
            }
        } finally {
            let err;
            for (const i of Object.keys(this.functionsToExecuteAfterIncommingMessageWasHandled)) {
                try {
                    this.functionsToExecuteAfterIncommingMessageWasHandled[i]();
                } catch (e) {
                    this.log.error(this.log.buildMessage(() => ('Error (follows below) in executing PostIncommingMessageHandlingTask: ' + this.functionsToExecuteAfterIncommingMessageWasHandled[i])));
                    this.log.error(e);
                    err = e;
                }
            }
            this.functionsToExecuteAfterIncommingMessageWasHandled = undefined;
            if (err) throw err;
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
export class WebsocketConstants {
    static readonly CLEAR_SESSION_PARAM = 'sabloClearSession';
}

export class SabloUtils {

    // execution priority on server value used when for example a blocking API call from server needs to request more data from the server through this change
    // or whenever during a (blocking) API call to client we want some messages sent to the server to still be processed.
    static readonly EVENT_LEVEL_SYNC_API_CALL = 500;

    // objects that have a function named like this in them will send to server the result of that function call when no conversion type is available (in case of
    // usage as handler arg. for example where we don't know the arg. types on client)
    static readonly DEFAULT_CONVERSION_TO_SERVER_FUNC = '_dctsf';

    /**
     * Makes a clone of "obj" (new object + iterates on properties and copies them over (so shallow clone)) that will have it's [[Prototype]] set to "newPrototype".
     * It is not aware of property descriptors. It uses plain property assignment when cloning.
     */
    public static cloneWithDifferentPrototype(obj, newPrototype) {
        // instead of using this impl., we could use Object.setPrototypeOf(), but that is slower in the long run due to missing JS engine optimizations for accessing props.
        // accorging to https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/setPrototypeOf
        const clone = Object.create(newPrototype);

        Object.keys(obj).forEach((prop) => clone[prop] = obj[prop]);

        return clone;
    }
}

