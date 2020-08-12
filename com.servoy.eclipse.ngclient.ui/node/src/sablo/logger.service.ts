import { Injectable } from '@angular/core';

import { WindowRefService } from './util/windowref.service';

// log levels for when debugEnabled(true) is called - if that is false, these levels are irrelevant
// any custom debug levels can be used as well - these are just stored here so that custom code can test the level and see if it should log its message
export enum LogLevel {
    ERROR = 1,
    WARN,
    INFO,
    DEBUG,
    SPAM
}

export class LogConfiguration {
    constructor(public isDebugMode: boolean = false, public level: LogLevel= LogLevel.ERROR) {}
}

declare global {
    interface Window { svyLogConfiguration: LogConfiguration; logFactory: LoggerFactory; logLevels: {}; console: Console; } // extend the existing window interface with the new log provider property
}

const noop = (): any => undefined;

export class LoggerService {
    private console: Console;
    private enabled = false;

    constructor( private windowRefService: WindowRefService, private svyLogConfiguration: LogConfiguration, private className: string ) {
        this.console = windowRefService.nativeWindow.console;
    }

    public buildMessage(message: string | ( () => string )) {
        if (this.enabled) {
            return message instanceof Function ? message() : message;
        }
    }

    get spam() {
        if ( this.svyLogConfiguration.level >= LogLevel.SPAM ) {
            this.enabled = true;
            return this.console.debug.bind(this.console, this.getTime() + ' SPAM ' + this.className + ' - ');
        }
        this.enabled = false;
        return noop;
    }

    get debug() {
        if ( this.svyLogConfiguration.isDebugMode || this.svyLogConfiguration.level >= LogLevel.DEBUG ) {
            this.enabled = true;
            return this.console.debug.bind(this.console, this.getTime() + ' DEBUG ' + this.className + ' - ');
        }
        this.enabled = false;
        return noop;
    }

    get info() {
        if ( this.svyLogConfiguration.isDebugMode || this.svyLogConfiguration.level >= LogLevel.INFO ) {
            this.enabled = true;
            return this.console.info.bind(this.console, this.getTime() + ' INFO ' + this.className + ' - ');
        }
        this.enabled = false;
        return noop;
    }

    get warn() {
        if ( this.svyLogConfiguration.isDebugMode || this.svyLogConfiguration.level >= LogLevel.WARN ) {
            this.enabled = true;
            return this.console.warn.bind(this.console, this.getTime() + ' WARN ' + this.className + ' - ');
        }
        this.enabled = false;
        return noop;
    }

    get error() {
        if ( this.svyLogConfiguration.isDebugMode || this.svyLogConfiguration.level >= LogLevel.ERROR ) {
            this.enabled = true;
            return this.console.error.bind(this.console, this.getTime() + ' ERROR ' + this.className + ' - ');
        }
        this.enabled = false;
        return noop;
    }

    public toggleDebugMode() {
        return this.svyLogConfiguration.isDebugMode = !this.svyLogConfiguration.isDebugMode;
    }

    public setLogLevel(level: LogLevel) {
        this.svyLogConfiguration.level = level;
    }

    private getTime(): string {
        let time = new Date();
        return time.getHours() + ':' + time.getMinutes() + ':' + time.getSeconds();
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
            windowRefService.nativeWindow.logLevels = {error: LogLevel.ERROR, debug: LogLevel.DEBUG, info: LogLevel.INFO, warn: LogLevel.WARN, spam: LogLevel.SPAM};
        }
    }

    public getLogger(cls: any): LoggerService {
        if (this.instances[cls] == undefined) {
            this.instances[cls] = new LoggerService(this.windowRefService, new LogConfiguration(this.defaultLogConfiguration.isDebugMode, this.defaultLogConfiguration.level), cls);
        }
        return this.instances[cls];
    }
}
