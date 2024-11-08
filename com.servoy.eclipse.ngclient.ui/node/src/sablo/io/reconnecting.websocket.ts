import { LoggerFactory, LoggerService, LogLevel } from '@servoy/public';
import { CustomEventEmitter } from '../util/eventemitter';
import { IWebSocket, WebsocketCustomEvent } from './iwebsocket';


type UrlFunction = () => string;

export class ReconnectingWebSocket implements IWebSocket {
    /** Whether or not the websocket should attempt to connect immediately upon instantiation. */
    private automaticOpen = true;

    /** The number of milliseconds to delay before attempting to reconnect. */
    private reconnectInterval = 1000;

    /** The maximum number of milliseconds to delay a reconnection attempt. */
    private maxReconnectInterval = 30000;

    /** The rate of increase of the reconnect delay. Allows reconnect attempts to back off when problems persist. */
    private reconnectDecay = 1.5;

    /** The maximum time in milliseconds to wait for a connection to succeed before closing and retrying. */
    private timeoutInterval = 2000;

    /** The maximum number of reconnection attempts to make. Unlimited if null. */
    private maxReconnectAttempts = null;

    /** The number of attempted reconnects since starting, or the last successful connection. Read only. */
    private reconnectAttempts = 0;

    private url: string | UrlFunction;

    private ws: WebSocket;
    private forcedClose = false;
    private timedOut = false;

    private id = Math.random();


    private eventTarget = new CustomEventEmitter();


    /**
     * The current state of the connection.
     * Can be one of: WebSocket.CONNECTING, WebSocket.OPEN, WebSocket.CLOSING, WebSocket.CLOSED
     * Read only.
     */
    private readyState: number = WebSocket.CONNECTING;
    private log: LoggerService;

    constructor(url: string | UrlFunction, logFactory: LoggerFactory, options?: { [property: string]: string | number | boolean }) {
        this.log = logFactory.getLogger('ReconnectingWebSocket');
        // enable this to have debug log level
        // this.log.logLevel = LogLevel.DEBUG;
        // Default settings
        const settings = {

            /** Whether this instance should log debug messages. */
            debug: false,

            /** Whether or not the websocket should attempt to connect immediately upon instantiation. */
            automaticOpen: true,

            /** The number of milliseconds to delay before attempting to reconnect. */
            reconnectInterval: 1000,
            /** The maximum number of milliseconds to delay a reconnection attempt. */
            maxReconnectInterval: 30000,
            /** The rate of increase of the reconnect delay. Allows reconnect attempts to back off when problems persist. */
            reconnectDecay: 1.5,

            /** The maximum time in milliseconds to wait for a connection to succeed before closing and retrying. */
            timeoutInterval: 2000,

            /** The maximum number of reconnection attempts to make. Unlimited if null. */
            maxReconnectAttempts: null
        };
        if (!options) {
            options = {};
        }
        // Overwrite and define settings with options if they exist.
        for (const key in settings) {
            if (typeof options[key] !== 'undefined') {
                this[key] = options[key];
            } else {
                this[key] = settings[key];
            }
        }
        this.url = url;

        // Wire up "on*" properties as event handlers

        this.eventTarget.addEventListener('open', (event: WebsocketCustomEvent) => this.onopen(event));
        this.eventTarget.addEventListener('close', (event: WebsocketCustomEvent) => this.onclose(event));
        this.eventTarget.addEventListener('connecting', (event: WebsocketCustomEvent) => this.onconnecting(event));
        this.eventTarget.addEventListener('message', (event: WebsocketCustomEvent) => this.onmessage(event));
        this.eventTarget.addEventListener('error', (event: WebsocketCustomEvent) => this.onerror(event));

        // Whether or not to create a websocket upon instantiation
        if (this.automaticOpen === true) {
            this.open(false);
        }
    }

    /**
     * Additional public API method to refresh the connection if still open (close, re-open).
     * For example, if the app suspects bad data / missed heart beats, it can try to refresh.
     */
    public refresh() {
        if (this.ws) {
            this.ws.close();
        }
    }

    /**
     * Closes the WebSocket connection or connection attempt, if any.
     * If the connection is already CLOSED, this method does nothing.
     */
    public close(code?: number, reason?: string) {
        // Default CLOSE_NORMAL code
        if (typeof code == 'undefined') {
            code = 1000;
        }
        this.forcedClose = true;
        if (this.ws) {
            this.ws.close(code, reason);
        }
    }

    /**
     * Transmits data to the server over the WebSocket connection.
     *
     * @param data a text string, ArrayBuffer or Blob to send to the server.
     */
    public send(data) {
        if (this.ws) {
            if (this.log.logLevel === LogLevel.DEBUG) {
                this.log.debug('ReconnectingWebSocket', 'send', this.getUrl(), data);
            }
            return this.ws.send(data);
        } else {
            throw new Error('INVALID_STATE_ERR : Pausing to reconnect websocket');
        }
    };


    public open(reconnectAttempt) {

        if (reconnectAttempt) {
            if (this.maxReconnectAttempts && this.reconnectAttempts > this.maxReconnectAttempts) {
                return;
            }
        } else {
            this.eventTarget.dispatchEvent(new WebsocketCustomEvent('connecting'));
            this.reconnectAttempts = 0;
        }

       if (this.log.logLevel === LogLevel.DEBUG) {
            this.log.debug('ReconnectingWebSocket', 'attempt-connect', this.getUrl());
        }
        this.ws = new WebSocket(this.getUrl());
        const self = this;
        const localWs = this.ws;
        const timeout = setTimeout(() => {
            if (this.log.logLevel === LogLevel.DEBUG) {
                this.log.debug('ReconnectingWebSocket', 'connection-timeout', this.getUrl());
            }
            this.timedOut = true;
            localWs.close();
            this.timedOut = false;
        }, this.timeoutInterval);

        this.ws.onopen = (event) => {
            clearTimeout(timeout);
            if (this.log.logLevel === LogLevel.DEBUG) {
                this.log.debug('ReconnectingWebSocket', 'onopen', self.getUrl());
            }
            self.readyState = WebSocket.OPEN;
            self.reconnectAttempts = 0;
            const e = new WebsocketCustomEvent('open');
            e.isReconnect = reconnectAttempt;
            reconnectAttempt = false;
            self.eventTarget.dispatchEvent(e);
        };

        this.ws.onclose = (event) => {
            clearTimeout(timeout);
            self.ws = null;
            if (self.forcedClose) {
                self.readyState = WebSocket.CLOSED;
                self.eventTarget.dispatchEvent(new WebsocketCustomEvent('close'));
            } else {
                self.readyState = WebSocket.CONNECTING;
                const e = new WebsocketCustomEvent('connecting');
                e.code = event.code;
                e.reason = event.reason;
                e.wasClean = event.wasClean;
                self.eventTarget.dispatchEvent(e);
                if (!reconnectAttempt && !self.timedOut) {
                    if (this.log.logLevel === LogLevel.DEBUG) {
                        this.log.debug('ReconnectingWebSocket', 'onclose', self.getUrl());
                    }
                    self.eventTarget.dispatchEvent(new WebsocketCustomEvent('close'));
                }

                const timeoutInteval = self.reconnectInterval * Math.pow(self.reconnectDecay, self.reconnectAttempts);
                setTimeout(() => {
                    if (!self.forcedClose) {
                        self.reconnectAttempts++;
                        self.open(true);
                    }
                }, timeoutInteval > self.maxReconnectInterval ? self.maxReconnectInterval : timeoutInteval);
            }
        };
        this.ws.onmessage = (event) => {
           if (this.log.logLevel === LogLevel.DEBUG) {
                this.log.debug('ReconnectingWebSocket', 'onmessage', self.getUrl(), event.data);
            }
            const e = new WebsocketCustomEvent('message');
            e.data = event.data;
            self.eventTarget.dispatchEvent(e);
        };
        this.ws.onerror = (event) => {
           if (this.log.logLevel === LogLevel.DEBUG) {
                this.log.debug('ReconnectingWebSocket', 'onerror', self.getUrl(), event);
            }
            self.eventTarget.dispatchEvent(new WebsocketCustomEvent('error'));
        };
    }

    /**
     * An event listener to be called when the WebSocket connection's readyState changes to OPEN;
     * this indicates that the connection is ready to send and receive data.
     */
    public onopen(_event: WebsocketCustomEvent) { };
    /** An event listener to be called when the WebSocket connection's readyState changes to CLOSED. */
    public onclose(_event: WebsocketCustomEvent) { };
    /** An event listener to be called when a connection begins being attempted. */
    public onconnecting(_event: WebsocketCustomEvent) { };
    /** An event listener to be called when a message is received from the server. */
    public onmessage(_event: WebsocketCustomEvent) { };
    /** An event listener to be called when an error occurs. */
    public onerror(_event: WebsocketCustomEvent) { };


    /** The URL as resolved by the constructor, or a function to return the current url. This is always an absolute URL. Read only. */
    private getUrl(): string {
        return typeof (this.url) === 'function' ? this.url() : this.url;
    }

}
