import {CustomEventEmitter,CustomEvent}from "../util/eventemitter"


export class ReconnectingWebSocket {
    /**
     * Whether all instances of ReconnectingWebSocket should log debug messages.
     * Setting this to true is the equivalent of setting all instances of ReconnectingWebSocket.debug to true.
     */
   public static debugAll = false;

    
    /** Whether this instance should log debug messages. */
   public  debug = false;

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
   
   private url:string|Function
   
   private ws:WebSocket;
   private forcedClose = false;
   private timedOut = false;
   
   private id =Math.random();

   
   private eventTarget = new CustomEventEmitter();

   
   /**
    * The current state of the connection.
    * Can be one of: WebSocket.CONNECTING, WebSocket.OPEN, WebSocket.CLOSING, WebSocket.CLOSED
    * Read only.
    */
   private readyState = WebSocket.CONNECTING;

   constructor(url:string|Function, options?:{[property:string]:string|number|boolean}) {
       // Default settings
       var settings = {

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
       }
       if (!options) { options = {}; }
       // Overwrite and define settings with options if they exist.
       for (var key in settings) {
           if (typeof options[key] !== 'undefined') {
               this[key] = options[key];
           } else {
               this[key] = settings[key];
           }
       }
       this.url = url;
       
       // Wire up "on*" properties as event handlers

      this.eventTarget.addEventListener('open',  (event)=> this.onopen(event));
      this.eventTarget.addEventListener('close',  (event)=> this.onclose(event));
      this.eventTarget.addEventListener('connecting', (event)=> this.onconnecting(event));
      this.eventTarget.addEventListener('message',    (event)=> this.onmessage(event) );
      this.eventTarget.addEventListener('error',      (event)=> this.onerror(event));
      
      // Whether or not to create a websocket upon instantiation
      if (this.automaticOpen == true) {
          this.open(false);
      }
   }
   
   /** The URL as resolved by the constructor, or a function to return the current url. This is always an absolute URL. Read only. */
   private getUrl():string {
       return typeof(this.url) === 'function' ? this.url(): this.url;
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
   public close(code?:number, reason?:string) {
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
           if (this.debug || ReconnectingWebSocket.debugAll) {
               console.debug('ReconnectingWebSocket', 'send', this.getUrl(), data);
           }
           return this.ws.send(data);
       } else {
           throw 'INVALID_STATE_ERR : Pausing to reconnect websocket';
       }
   };

   
   public open(reconnectAttempt) {
       this.ws = new WebSocket(this.getUrl());

       if (reconnectAttempt) {
           if (this.maxReconnectAttempts && this.reconnectAttempts > this.maxReconnectAttempts) {
               return;
           }
       } else {
           this.eventTarget.dispatchEvent(new CustomEvent('connecting'));
           this.reconnectAttempts = 0;
       }

       if (this.debug || ReconnectingWebSocket.debugAll) {
           console.debug('ReconnectingWebSocket', 'attempt-connect', this.getUrl());
       }
       const self = this;
       const localWs = this.ws;
       var timeout = setTimeout(function() {
           if (this.debug || ReconnectingWebSocket.debugAll) {
               console.debug('ReconnectingWebSocket', 'connection-timeout', this.getUrl());
           }
          this.timedOut = true;
           localWs.close();
           this.timedOut = false;
       }, this.timeoutInterval);

       this.ws.onopen = function(event) {
           clearTimeout(timeout);
           if (self.debug || ReconnectingWebSocket.debugAll) {
               console.debug('ReconnectingWebSocket', 'onopen', self.getUrl());
           }
           self.readyState = WebSocket.OPEN;
           self.reconnectAttempts = 0;
           const e = new WebsocketCustomEvent('open');
           e.isReconnect = reconnectAttempt;
           reconnectAttempt = false;
           self.eventTarget.dispatchEvent(e);
       };

       this.ws.onclose = function(event) {
           clearTimeout(timeout);
           self.ws = null;
           if (self.forcedClose) {
               self.readyState = WebSocket.CLOSED;
              self.eventTarget.dispatchEvent(new CustomEvent('close'));
           } else {
               self.readyState = WebSocket.CONNECTING;
               var e = new WebsocketCustomEvent('connecting');
               e.code = event.code;
               e.reason = event.reason;
               e.wasClean = event.wasClean;
               self.eventTarget.dispatchEvent(e);
               if (!reconnectAttempt && !self.timedOut) {
                   if (self.debug || ReconnectingWebSocket.debugAll) {
                       console.debug('ReconnectingWebSocket', 'onclose', self.getUrl());
                   }
                   self.eventTarget.dispatchEvent(new CustomEvent('close'));
               }

               var timeout = self.reconnectInterval * Math.pow(self.reconnectDecay, self.reconnectAttempts);
               setTimeout(function() {
                   if (!self.forcedClose) {
                       self.reconnectAttempts++;
                       self.open(true);
                   }
               }, timeout > self.maxReconnectInterval ? self.maxReconnectInterval : timeout);
           }
       };
       this.ws.onmessage = function(event) {
           if (self.debug || ReconnectingWebSocket.debugAll) {
               console.debug('ReconnectingWebSocket', 'onmessage', self.getUrl(), event.data);
           }
           var e = new WebsocketCustomEvent('message');
           e.data = event.data;
           self.eventTarget.dispatchEvent(e);
       };
       this.ws.onerror = function(event) {
           if (self.debug || ReconnectingWebSocket.debugAll) {
               console.debug('ReconnectingWebSocket', 'onerror', self.getUrl(), event);
           }
           self.eventTarget.dispatchEvent(new CustomEvent('error'));
       };
   }
   
   /**
    * An event listener to be called when the WebSocket connection's readyState changes to OPEN;
    * this indicates that the connection is ready to send and receive data.
    */
   public onopen(event:CustomEvent) {};
   /** An event listener to be called when the WebSocket connection's readyState changes to CLOSED. */
   public onclose(event:CustomEvent) {};
   /** An event listener to be called when a connection begins being attempted. */
   public onconnecting(event:CustomEvent) { };
   /** An event listener to be called when a message is received from the server. */
   public onmessage(event:CustomEvent) {};
   /** An event listener to be called when an error occurs. */
   public onerror(event:CustomEvent) {};

}

class WebsocketCustomEvent extends CustomEvent {
    public isReconnect:boolean;
    public code:number;
    public reason:string;
    public wasClean:boolean;
    public data:any;
}