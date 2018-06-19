import { Injectable } from '@angular/core';

import { WindowRefService } from './util/windowref.service'

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
    isDebugMode: boolean = false;
    level: LogLevel = LogLevel.ERROR
}

declare global {
    interface Window { svyLogConfiguration: LogConfiguration, logFactory: LoggerFactory } //extend the existing window interface with the new log provider property
}

export class LoggerService {
    private svyLogConfiguration: LogConfiguration;
    private console: Console;

    constructor( private windowRefService: WindowRefService, private className : string ) {
        this.svyLogConfiguration = windowRefService.nativeWindow.svyLogConfiguration;
        this.console = windowRefService.nativeWindow.console;
        if ( this.svyLogConfiguration == null ) {
            this.svyLogConfiguration = new LogConfiguration();
            windowRefService.nativeWindow.svyLogConfiguration = this.svyLogConfiguration;
        }
    }

    public spam( message: string | ( () => string ) ) {
        if ( this.svyLogConfiguration.level >= LogLevel.SPAM ) {
            const msg = message instanceof Function ? message() : message;
            this.console.debug(this.getTime() +" SPAM " + this.className + " - " + msg);
        }
    }

    public debug(message: string | ( () => string ) ) {
        if ( this.svyLogConfiguration.isDebugMode || this.svyLogConfiguration.level >= LogLevel.DEBUG ) {
            const msg = message instanceof Function ? message() : message;
            this.console.debug(this.getTime() +" DEBUG " + this.className + " - " + msg );
        } 
    }

    public info(message: string | ( () => string ) ) {
        if ( this.svyLogConfiguration.isDebugMode || this.svyLogConfiguration.level >= LogLevel.INFO ) {
            const msg = message instanceof Function ? message() : message;
            this.console.info(this.getTime() +" INFO " + this.className + " - " + msg);
        }
    }

    public warn(message: string | ( () => string ) ) {
        if ( this.svyLogConfiguration.isDebugMode || this.svyLogConfiguration.level >= LogLevel.WARN ) {
            const msg = message instanceof Function ? message() : message;
            this.console.warn(this.getTime() +" WARN " + this.className + " - " + msg);
        } 
    }

    public error(message: string | ( () => string ) ) {
        if ( this.svyLogConfiguration.isDebugMode || this.svyLogConfiguration.level >= LogLevel.ERROR ) {
            const msg = message instanceof Function ? message() : message;
            this.console.error(this.getTime() +" ERROR " + this.className + " - " + msg);
        } 
    }

    get debugLevel(): LogLevel {
        return this.svyLogConfiguration.level;
    }
    
    private getTime() : string {
        var time = new Date();
        return time.getHours() + ":" + time.getMinutes() + ":" + time.getSeconds();
    }
 }

@Injectable()
export class LoggerFactory {
    
    private instances: any = {};
    
    constructor(private windowRefService: WindowRefService ) {
        windowRefService.nativeWindow.logFactory = this;
    }
    
    public getLogger(cls: any) : LoggerService {
        if (this.instances[cls] == undefined) {
            this.instances[cls] = new LoggerService(this.windowRefService, cls);
        }
        return this.instances[cls];
    }
}