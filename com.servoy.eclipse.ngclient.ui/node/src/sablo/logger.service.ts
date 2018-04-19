import { Injectable } from '@angular/core';

export abstract class Logger {

    debug: any;
    warn: any;
    error: any;
}

export let isDebugMode = false;//TODO provider?
const noop = (): any => undefined;

//log levels for when debugEnabled(true) is called - if that is false, these levels are irrelevant
//any custom debug levels can be used as well - these are just stored here so that custom code can test the level and see if it should log its message
export enum LogLevel {
    DEBUG = 1, 
    SPAM
}

@Injectable()
export class LoggerService implements Logger {

     private level : LogLevel = LogLevel.DEBUG;
    
     get debug() {
         if (isDebugMode) {
             return console.debug.bind(console);
         } else {
             return noop;
         }
     }

     get warn() {
         if (isDebugMode) {
             return console.warn.bind(console);
         } else {
             return noop;
         }
     }
     
     get error() {
         if (isDebugMode) {
             return console.error.bind(console);
         } else {
             return noop;
         }
     }
     
     public debugLevel() : LogLevel {
         return this.level;
     }
     
     invokeConsoleMethod(type: string, args?: any): void {
         const logFn: Function = (console)[type] || console.log || noop;
         logFn.apply(console, [args]);
     }
     
     
 }