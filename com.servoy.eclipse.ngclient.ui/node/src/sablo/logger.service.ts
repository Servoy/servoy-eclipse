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
    constructor(public isDebugMode : boolean = false, public level : LogLevel=LogLevel.ERROR){}
}

declare global {
    interface Window { svyLogConfiguration: LogConfiguration, logFactory: LoggerFactory, logLevels: {} } //extend the existing window interface with the new log provider property
}

export class LoggerService {
    private console: Console;

    constructor( private windowRefService: WindowRefService, private svyLogConfiguration: LogConfiguration, private className : string ) {
        this.console = windowRefService.nativeWindow.console;
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

    public toggleDebugMode() {
        return this.svyLogConfiguration.isDebugMode = !this.svyLogConfiguration.isDebugMode;
    }
    
    public setLogLevel(level: LogLevel) {
        this.svyLogConfiguration.level = level;
    }
    
    private getTime() : string {
        var time = new Date();
        return time.getHours() + ":" + time.getMinutes() + ":" + time.getSeconds();
    }
 }

@Injectable()
export class LoggerFactory {
    
    private instances: any = {};
    private defaultLogConfiguration: LogConfiguration;
    
    constructor(private windowRefService: WindowRefService ) {
        windowRefService.nativeWindow.logFactory = this;
        this.defaultLogConfiguration = windowRefService.nativeWindow.svyLogConfiguration;
        if ( this.defaultLogConfiguration == null ) {
            this.defaultLogConfiguration = new LogConfiguration();
            windowRefService.nativeWindow.svyLogConfiguration = this.defaultLogConfiguration;
            windowRefService.nativeWindow.logLevels = {error: LogLevel.ERROR, debug: LogLevel.DEBUG, info: LogLevel.INFO, warn: LogLevel.WARN, spam:LogLevel.SPAM};
        }
    }
    
    public getLogger(cls: any) : LoggerService {
        if (this.instances[cls] == undefined) {
            this.instances[cls] = new LoggerService(this.windowRefService, new LogConfiguration(this.defaultLogConfiguration.isDebugMode, this.defaultLogConfiguration.level), cls);
        }
        return this.instances[cls];
    }
}