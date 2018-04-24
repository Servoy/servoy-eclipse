import { Injectable } from '@angular/core';

import { WindowRefService } from './util/windowref.service'

const noop = (): any => undefined;

//log levels for when debugEnabled(true) is called - if that is false, these levels are irrelevant
//any custom debug levels can be used as well - these are just stored here so that custom code can test the level and see if it should log its message
export enum LogLevel {
    ERROR = 1,
    WARN = 2, 
    INFO = 3,
    DEBUG = 4,
    SPAM = 5,
    
}

export class LogConfiguration {
    isDebugMode : boolean = false;
    level : LogLevel = LogLevel.ERROR
}

declare global {
    interface Window { svyLogProvider: LogConfiguration; } //extend the existing window interface with the new log provider property
}

@Injectable()
export class LoggerService  {
    private svyLogProvider:LogConfiguration;
    private console:Console;

    constructor( windowRefService:WindowRefService) {
        this.svyLogProvider = windowRefService.nativeWindow.svyLogProvider;
        this.console = windowRefService.nativeWindow.console;
        if (this.svyLogProvider == null) {
            this.svyLogProvider = new LogConfiguration();
            windowRefService.nativeWindow.svyLogProvider = this.svyLogProvider;
        }
    }
    
     get debug() {
         if (this.svyLogProvider.isDebugMode || this.svyLogProvider.level >=LogLevel.DEBUG ) {
             return this.console.debug.bind(this.console);
         } else {
             return noop;
         }
     }
     
     get info() {
         if (this.svyLogProvider.isDebugMode|| this.svyLogProvider.level >=LogLevel.INFO) {
             return this.console.info.bind(this.console);
         } else {
             return noop;
         }
     }

     get warn() {
         if (this.svyLogProvider.isDebugMode || this.svyLogProvider.level >=LogLevel.WARN) {
             return this.console.warn.bind(this.console);
         } else {
             return noop;
         }
     }
     
     get error() {
         if (this.svyLogProvider.isDebugMode|| this.svyLogProvider.level >=LogLevel.ERROR) {
             return this.console.error.bind(this.console);
         } else {
             return noop;
         }
     }
     
     get  debugLevel() : LogLevel {
         return this.svyLogProvider.level;
     }
 }