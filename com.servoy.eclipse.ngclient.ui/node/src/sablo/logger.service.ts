import { Injectable } from '@angular/core';

import { WindowRefService } from './util/windowref.service'

const noop = (): any => undefined;

//log levels for when debugEnabled(true) is called - if that is false, these levels are irrelevant
//any custom debug levels can be used as well - these are just stored here so that custom code can test the level and see if it should log its message
export enum LogLevel {
    ERROR = 1,
    WARN, 
    INFO,
    DEBUG,
    SPAM    
}

export class LogConfiguration {
    isDebugMode : boolean = false;
    level : LogLevel = LogLevel.ERROR
}

declare global {
    interface Window { svyLogConfiguration: LogConfiguration; } //extend the existing window interface with the new log provider property
}

@Injectable()
export class LoggerService  {
    private svyLogConfiguration:LogConfiguration;
    private console:Console;

    constructor(private windowRefService:WindowRefService) {
        this.svyLogConfiguration = windowRefService.nativeWindow.svyLogConfiguration;
        this.console = windowRefService.nativeWindow.console;
        if (this.svyLogConfiguration == null) {
            this.svyLogConfiguration = new LogConfiguration();
            windowRefService.nativeWindow.svyLogConfiguration = this.svyLogConfiguration;
        }
    }
    
     get debug() {
         if (this.svyLogConfiguration.isDebugMode || this.svyLogConfiguration.level >=LogLevel.DEBUG ) {
             return this.console.debug.bind(this.console);
         } else {
             return noop;
         }
     }
     
     get info() {
         if (this.svyLogConfiguration.isDebugMode|| this.svyLogConfiguration.level >=LogLevel.INFO) {
             return this.console.info.bind(this.console);
         } else {
             return noop;
         }
     }

     get warn() {
         if (this.svyLogConfiguration.isDebugMode || this.svyLogConfiguration.level >=LogLevel.WARN) {
             return this.console.warn.bind(this.console);
         } else {
             return noop;
         }
     }
     
     get error() {
         if (this.svyLogConfiguration.isDebugMode|| this.svyLogConfiguration.level >=LogLevel.ERROR) {
             return this.console.error.bind(this.console);
         } else {
             return noop;
         }
     }
     
     get  debugLevel() : LogLevel {
         return this.svyLogConfiguration.level;
     }
 }