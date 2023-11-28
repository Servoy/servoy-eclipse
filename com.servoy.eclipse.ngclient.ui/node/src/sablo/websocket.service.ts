import { Injectable, NgZone } from '@angular/core';
import { Subscription, interval, Subject } from 'rxjs';
import { ReconnectingWebSocket, WebsocketCustomEvent } from './io/reconnecting.websocket';
import { WindowRefService } from '@servoy/public';
import { Deferred } from '@servoy/public';
import { ConverterService } from './converter.service';
import { LoggerService, LoggerFactory, RequestInfoPromise } from '@servoy/public';
import { LoadingIndicatorService } from './util/loading-indicator/loading-indicator.service';

@Injectable({
  providedIn: 'root'
})
export class WebsocketService {
    public reconnectingEmitter = new Subject<boolean>();

    private pathname: string;
    private queryString: string;
    private wsSession: WebsocketSession;
    private wsSessionDeferred: Deferred<WebsocketSession>;
    private connectionArguments = {};
    private lastServerMessageNumber = null;

    constructor(private windowRef: WindowRefService,
        private converterService: ConverterService<unknown>,
        private logFactory: LoggerFactory,
        private loadingIndicatorService: LoadingIndicatorService,
        private ngZone: NgZone) {
    }

    public connect(context: string, args: string[], queryArgs?: Record<string,unknown>, websocketUri?: string): WebsocketSession {

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

            this.wsSession = new WebsocketSession(websocket, this, this.windowRef, this.converterService, this.loadingIndicatorService, this.ngZone, this.logFactory );
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
        return decodeURIComponent((new RegExp('[&]?\\b' + name + '=' + '([^&;]+?)(&|#|;|$)').exec(this.getQueryString()) || [, ''])[1].replace(/\+/g, '%20')) || null;
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

    public disconnect(){
        this.wsSession.disconnect();
    }

    public getCurrentRequestInfo(): any {
        return this.wsSession.getCurrentRequestInfo();
    }

    private generateURL(context, args, queryArgs?, websocketUri?) {
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

        if (queryArgs) {
            for (const a of Object.keys(queryArgs)) {
                if (queryArgs.hasOwnProperty(a)) {
                    new_uri += a + '=' + queryArgs[a] + '&';
                }
            }
        }

        if (this.lastServerMessageNumber != null) {
            new_uri += 'lastServerMessageNumber=' + this.lastServerMessageNumber + '&';
        }

        const queryString = this.getQueryString();
        if (queryString) {
            new_uri += queryString;
        } else {
            new_uri = new_uri.substring(0, new_uri.length - 1);
        }
        return new_uri;
    }
}

export type MessageObjectHandler = (msg: {[k: string]: unknown}) => Promise<any> | void;

export interface ServicesHandler {
    handleServiceApisWithApplyFirst(serviceApisJSON: any, previousResponseValue: any): any;
    handlerServiceUpdatesFromServer(servicesUpdatesFromServerJSON: any): void;
    handleNormalServiceApis(serviceApisJSON: any, previousResponseValue: any): any;
}

export class WebsocketSession {
    private connected = 'INITIAL';
    private heartbeatMonitor: Subscription = null;
    private lastHeartbeat: number;
    private onOpenHandlers: Array<(evt: WebsocketCustomEvent) => void> = new Array();
    private onErrorHandlers: Array<(evt: WebsocketCustomEvent) => void> = new Array();
    private onCloseHandlers: Array<(evt: WebsocketCustomEvent) => void> = new Array();
    private onMessageObjectHandlers: Array<MessageObjectHandler> = new Array();
    private servicesHandler: ServicesHandler = undefined;

    private functionsToExecuteAfterIncommingMessageWasHandled: Array<() => void> = undefined;

    private deferredEvents: Array<Deferred<any>> = new Array();

    private pendingMessages = undefined;

    private currentEventLevelForServer;

    private nextMessageId = 1;
    private log: LoggerService;

    private currentRequestInfo = undefined;

    constructor(private websocket: ReconnectingWebSocket,
        private websocketService: WebsocketService,
        private windowRef: WindowRefService,
        private converterService: ConverterService<unknown>,
        private loadingIndicatorService: LoadingIndicatorService,
        private ngZone: NgZone,
        logFactory: LoggerFactory) {
        this.log = logFactory.getLogger('WebsocketSession');
        this.websocket.onopen = (evt) => {
            this.setConnected();
            this.startHeartbeat();
            for (const handler of Object.keys( this.onOpenHandlers)) {
                this.onOpenHandlers[handler](evt);
            }
            this.ngZone.run( () => this.websocketService.reconnectingEmitter.next(false) );
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
                this.ngZone.run( () => this.websocketService.reconnectingEmitter.next(true) );
                this.log.spam(this.log.buildMessage(() => ('sbl * Connection mode (onclose receidev while not CLOSED): ... RECONNECTING (' + new Date().getTime() + ')')));
            }
            for (const handler of Object.keys(this.onCloseHandlers)) {
                this.onCloseHandlers[handler](evt);
            }
        };
        this.websocket.onconnecting = (evt) => {
                if (evt.reason === 'CLIENT-OUT-OF-SYNC') {
                    // Server detected that we are out-of-sync, reload completely
                    this.windowRef.nativeWindow.location.reload();
                } else if (evt.reason === 'CLIENT-SHUTDOWN') {
                    // client is shutdown just force close the websocket and set the connected state toe CLOSED so no reconnecting is shown
                    this.websocket.close();
                    // server disconnected, do not try to reconnect
                    this.connected = 'CLOSED';
                    this.log.spam(this.log.buildMessage(() => ('sbl * Connection mode (onconnecting got a server disconnect/close with reason '
                        + evt.reason + '): ... CLOSED (' + new Date().getTime() + ')')));
            }
        };
        this.websocket.onmessage = (message) => this.handleHeartbeat(message) || this.ngZone.run(() => this.handleMessage(message));
    }

    public createDeferredEvent<T>(): {deferred: Deferred<T>; cmsgid: number } {
          const deferred = new Deferred<T>();
            const cmsgid = this.getNextMessageId();
            this.deferredEvents[cmsgid] = deferred;
            return {deferred, cmsgid};
    }

    public resolveDeferedEvent(cmsgid: number, argument: unknown, success: boolean) {
            const deferred = this.deferredEvents[cmsgid];
            if (deferred) {
                delete this.deferredEvents[cmsgid];
                if (success) deferred.resolve(argument);
                else deferred.reject(argument);
            }
    }

    // api
    /**
     * IMPORTANT!
     * 
     * If the returned value is a promise and if the caller is INTERNAL code that chains more .then() or other methods and returns the new promise
     * to it's own callers, it MUST to wrap the new promise (returned by that then() for example) using wrapPromiseToPropagateCustomRequestInfoInternal() from websocket.service.ts.
     * 
     * This is so that the promise that ends up in (3rd party or our own) components and service code - that can then set .requestInfo on it - ends up to be
     * propagated into the promise that this callService(...) registered in "deferredEvents"; that is where any user set .requestInfo has to end up, because
     * that is where getCurrentRequestInfo() gets it from. And that is where special code - like foundset listeners also get the current request info from to
     * return it back to the user (component/service code).   
     */
    public callService<T>(serviceName: string, methodName: string, argsObject?: unknown, async?: boolean): RequestInfoPromise<T> {
        const cmd = {
            service: serviceName,
            methodname: methodName,
            args: argsObject
        };
        if (async) {
            this.sendMessageObject(cmd);
        } else {
            const deferred = this.createDeferredEvent<T>();
            this.loadingIndicatorService.showLoading();
            cmd['cmsgid'] = deferred.cmsgid;
            this.sendMessageObject(cmd);
            return deferred.deferred.promise;
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
    public onMessageObject(handler: MessageObjectHandler) {
        this.onMessageObjectHandlers.push(handler);
    }

    public setServicesHandler(servicesHandler: ServicesHandler) {
        this.servicesHandler = servicesHandler;
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
                     // the task can execute right away then (maybe it was called due to a change detected somewhere else instead of property listener)
    }

    public getCurrentRequestInfo(): any {
        return this.currentRequestInfo;
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
        let obj: any; // TODO should we type everything that can come from server?
        let responseValue: any;

        // normally this will always be null here; but in some browsers (FF) an alert on window can make these calls nest unexpectedly;
        // so oldFTEAIMWH avoids exceptions in those cases as well - as we do not restore to null but to old when nesting happens
        const oldFTEAIMWH = this.functionsToExecuteAfterIncommingMessageWasHandled;

        this.functionsToExecuteAfterIncommingMessageWasHandled = [];

        let message_data = message.data;
        let hideIndicator = false;
        try {

            const separator = message_data.indexOf('#');
            if (separator >= 0 && separator < 5) {
                // the json is prefixed with a message number: 123#{bla: "hello"}
                this.websocketService.setLastServerMessageNumber(message_data.substring(0, separator));
                this.log.spam(this.log.buildMessage(() => ('sbl * Received message from server with message number: ' + message_data.substring(0, separator))));
                message_data = message_data.substr(separator + 1);
            }
            // else message has no seq-no

            obj = JSON.parse(message_data);
            this.log.spam(this.log.buildMessage(() => ('sbl * Received message from server with message data: ' + JSON.stringify(obj, null, 2))));

            if (obj.cmsgid && this.deferredEvents[obj.cmsgid] && this.deferredEvents[obj.cmsgid].promise) {
                				this.currentRequestInfo = this.deferredEvents[obj.cmsgid].promise['requestInfo'];
            			}

            if (obj.serviceApis) {
                responseValue = this.servicesHandler.handleServiceApisWithApplyFirst(obj.serviceApis, responseValue);
            }

            // if the indicator is showing and this object wants a return message then hide the indicator until we send the response
            hideIndicator = obj && obj.smsgid && this.loadingIndicatorService.isShowing();
            // if a request to a service is being done then this could be a blocking
            if (hideIndicator) {
                this.loadingIndicatorService.hideLoading();
            }

            // message
            if (obj.msg) {
                for (const handler of Object.keys(this.onMessageObjectHandlers)) {
                    const ret = this.onMessageObjectHandlers[handler](obj.msg);
                    if (ret) responseValue = ret;
                }
            }

            if (obj.msg && obj.msg.services) {
                this.servicesHandler.handlerServiceUpdatesFromServer(obj.msg.services);
            }

            if (obj.serviceApis) {
                // normal services call
                responseValue = this.servicesHandler.handleNormalServiceApis(obj.serviceApis, responseValue);
            }

            // component api calls
            if (obj.componentApis) {
                for (const componentApiCall of obj.componentApis) {
                    for (const handler of this.onMessageObjectHandlers) {
                        const ret = handler({ call: componentApiCall });
                        // the handler call above handles both the arg type conversions and return value type conversion itself (wraps return value in a q.when);
                        // so no need to store/set "responseValueType" here, just responseValue
                        if (ret) responseValue = ret;
                    }
                }
            }

            if (obj && obj.smsgid) {
                // server wants a response; responseValue may be a promise
                Promise.resolve(responseValue).then((ret) => {
                    // success
                    const response = {
                        smsgid: obj.smsgid
                    };
                    if (ret !== undefined) {
                        response['ret'] = ret; // any needed conversions were already applied here
                    }
                    if (hideIndicator) {
                        this.loadingIndicatorService.showLoading();
                    }
                    this.sendMessageObject(response);
                }, (reason) => {
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

            // got the return value for a client-to-server call (that has a defer/waiting promise) back from the server
            if (obj.cmsgid) { // response to event
                const deferredEvent = this.deferredEvents[obj.cmsgid];
                if (deferredEvent != null) {
                    if (obj.exception) {
                        // something went wrong
                        // do a default conversion although I doubt it will do anything (don't think server will send client side type for exceptions)
                        obj.exception = this.converterService.convertFromServerToClient(obj.exception, undefined, undefined, undefined, undefined, undefined);
                        deferredEvent.reject(obj.exception);
                    } else {
                        // if it's a handler/server side api call that expects a return value, any type conversions should be done in code triggered by this resolve (in calling code)
                        deferredEvent.resolve(obj.ret);
                    }
                } else this.log.warn('Response to an unknown handler call dismissed; can happen (normal) if a handler call gets interrupted by a full browser refresh.');
                delete this.deferredEvents[obj.cmsgid];
                this.loadingIndicatorService.hideLoading();
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
            this.currentRequestInfo = undefined;
            let err: any;
            const toExecuteAfterIncommingMessageWasHandled = this.functionsToExecuteAfterIncommingMessageWasHandled;

            // clear/restore this before calling just in the unlikely case that some handlers want to add more such tasks (and we don't want to loose those but rather execute them right away)
            this.functionsToExecuteAfterIncommingMessageWasHandled = oldFTEAIMWH;

            for (const i of Object.keys(toExecuteAfterIncommingMessageWasHandled)) {
                try {
                    toExecuteAfterIncommingMessageWasHandled[i]();
                } catch (e) {
                    this.log.error(this.log.buildMessage(() => ('Error (follows below) in executing PostIncommingMessageHandlingTask: ' + toExecuteAfterIncommingMessageWasHandled[i])));
                    this.log.error(e);
                    err = e;
                }
            }
            // eslint-disable-next-line no-unsafe-finally
            if (err) throw err;
        }
    }

}

class WsCloseCodes {
    /** indicates a normal closure, meaning that the purpose for which the connection was established has been fulfilled. */
    static readonly NORMAL_CLOSURE = 1000;
    /** indicates that an endpoint is "going away", such as a server going down or a browser having navigated away from a page. */
    static readonly GOING_AWAY: 1001;
    /** indicates that an endpoint is terminating the connection due to a protocol error. */
    static readonly PROTOCOL_ERROR: 1002;
    /**
     * indicates that an endpoint is terminating the connection because it has received a type of data it cannot accept.
     *(e.g., an endpoint that understands only text data MAY send this if it receives a binary message).
     */
    static readonly CANNOT_ACCEPT: 1003;
    /** is a reserved value and MUST NOT be set as a status code in a Close control frame by an endpoint. */
    static readonly NO_STATUS_CODE: 1005;
    /** is a reserved value and MUST NOT be set as a status code in a Close control frame by an endpoint. */
    static readonly CLOSED_ABNORMALLY: 1006;
    /**
     * indicates that an endpoint is terminating the connection because it has received data within a message that
     * was not consistent with the type of the message (e.g., non-UTF-8 data within a text message).
     */
    static readonly NOT_CONSISTENT: 1007;
    /** indicates that an endpoint is terminating the connection because it has received a message that violates its policy. */
    static readonly VIOLATED_POLICY: 1008;
    /** indicates that an endpoint is terminating the connection because it has received a message that is too big for it to process. */
    static readonly TOO_BIG: 1009;
    /**
     * indicates that an endpoint (client) is terminating the connection because it has expected the server to negotiate
     * one or more extension, but the server didn't return them in the response message of the WebSocket handshake.
     */
    static readonly NO_EXTENSION: 1010;
    /** indicates that a server is terminating the connection because it encountered an unexpected condition that prevented it from fulfilling the request. */
    static readonly UNEXPECTED_CONDITION: 1011;
    /** indicates that the service will be restarted. */
    static readonly SERVICE_RESTART: 1012;
    /** is a reserved value and MUST NOT be set as a status code in a Close control frame by an endpoint. */
    static readonly TLS_HANDSHAKE_FAILURE: 1015;
    /** indicates that the service is experiencing overload. */
    static readonly TRY_AGAIN_LATER: 1013;
}

export class SabloUtils {

    // execution priority on server value used when for example a blocking API call from server needs to request more data from the server through this change
    // or whenever during a (blocking) API call to client we want some messages sent to the server to still be processed.
    static readonly EVENT_LEVEL_SYNC_API_CALL = 500;

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

export const wrapPromiseToPropagateCustomRequestInfoInternal = (originalPromise: RequestInfoPromise<any>,
    spawnedPromise: RequestInfoPromise<any>): RequestInfoPromise<any> => {
        return Object.defineProperty(spawnedPromise, "requestInfo", {
            set(value) {
                originalPromise.requestInfo = value;
            }
        });        
    }

