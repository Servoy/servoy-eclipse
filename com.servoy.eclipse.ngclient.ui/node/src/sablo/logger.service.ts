import { Injectable } from '@angular/core';

export abstract class Logger {

    debug: any;
    warn: any;
    error: any;
}

const noop = (): any => undefined;

//log levels for when debugEnabled(true) is called - if that is false, these levels are irrelevant
//any custom debug levels can be used as well - these are just stored here so that custom code can test the level and see if it should log its message
export enum LogLevel {
    DEBUG = 1, 
    SPAM
}

export class LogProvider {
    isDebugMode : boolean = false;
    level : LogLevel = LogLevel.DEBUG
}

declare global {
    interface Window { svyLogProvider: LogProvider; } //extend the existing window interface with the new log provider property
}

window.svyLogProvider = window.svyLogProvider || {isDebugMode:false, level:LogLevel.DEBUG};

@Injectable()
export class LoggerService implements Logger {
    
     get debug() {
         if (window.svyLogProvider.isDebugMode) {
             return console.debug.bind(console);
         } else {
             return noop;
         }
     }

     get warn() {
         if (window.svyLogProvider.isDebugMode) {
             return console.warn.bind(console);
         } else {
             return noop;
         }
     }
     
     get error() {
         if (window.svyLogProvider.isDebugMode) {
             return console.error.bind(console);
         } else {
             return noop;
         }
     }
     
     public debugLevel() : LogLevel {
         return window.svyLogProvider.level;
     }
     
     invokeConsoleMethod(type: string, args?: any): void {
         const logFn: Function = (console)[type] || console.log || noop;
         logFn.apply(console, [args]);
     }
 }