import { Injectable } from '@angular/core';

import { WindowRefService } from './services/windowref.service';

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
    constructor(public isDebugMode: boolean = false, public level: LogLevel= LogLevel.WARN) {
    }
}

export interface ServoyWindow extends Window {
    svyLogConfiguration: LogConfiguration;
    logFactory: LoggerFactory;
    logLevels: {[property: string]: LogLevel};
    console: Console;
}

const noop = (): unknown => undefined;

export class LoggerService {
    private console: Console;
    private enabled = false;

    constructor(windowRefService: WindowRefService, private svyLogConfiguration: LogConfiguration, private className: string ) {
        const win = windowRefService.nativeWindow as unknown as ServoyWindow;
        this.console = win.console;
    }

    public buildMessage(message: string | ( () => string )) {
        if (this.enabled) {
            return message instanceof Function ? message() : message;
        }
    }

    get spam(): (...data: unknown[])=> void {
        if ( this.svyLogConfiguration.level >= LogLevel.SPAM ) {
            this.enabled = true;
            return this.console.debug.bind(this.console, this.getTime() + ' SPAM ' + this.className + ' - ') as (...data: unknown[])=> void;
        }
        this.enabled = false;
        return noop;
    }

    get debug(): (...data: unknown[])=> void {
        if ( this.svyLogConfiguration.isDebugMode || this.svyLogConfiguration.level >= LogLevel.DEBUG ) {
            this.enabled = true;
            return this.console.debug.bind(this.console, this.getTime() + ' DEBUG ' + this.className + ' - ') as (...data: unknown[])=> void;
        }
        this.enabled = false;
        return noop;
    }

    get info(): (...data: unknown[])=> void {
        if ( this.svyLogConfiguration.isDebugMode || this.svyLogConfiguration.level >= LogLevel.INFO ) {
            this.enabled = true;
            return this.console.info.bind(this.console, this.getTime() + ' INFO ' + this.className + ' - ') as (...data: unknown[])=> void;
        }
        this.enabled = false;
        return noop;
    }

    get warn(): (...data: unknown[])=> void {
        if ( this.svyLogConfiguration.isDebugMode || this.svyLogConfiguration.level >= LogLevel.WARN ) {
            this.enabled = true;
            return this.console.warn.bind(this.console, this.getTime() + ' WARN ' + this.className + ' - ') as (...data: unknown[])=> void;
        }
        this.enabled = false;
        return noop;
    }

    get error():(...data: unknown[])=> void {
        if ( this.svyLogConfiguration.isDebugMode || this.svyLogConfiguration.level >= LogLevel.ERROR ) {
            this.enabled = true;
            return this.console.error.bind(this.console, this.getTime() + ' ERROR ' + this.className + ' - ') as (...data: unknown[])=> void;
        }
        this.enabled = false;
        return noop;
    }

    public toggleDebugMode() {
        return this.svyLogConfiguration.isDebugMode = !this.svyLogConfiguration.isDebugMode;
    }

    get logLevel() {
        return this.svyLogConfiguration.level;
    }

    set logLevel(level: LogLevel) {
        this.svyLogConfiguration.level = level;
    }

    private getTime(): string {
        const time = new Date();
        return time.getHours() + ':' + time.getMinutes() + ':' + time.getSeconds();
    }
 }

@Injectable({
  providedIn: 'root'
})
export class LoggerFactory {

    private instances: {[k: string]: LoggerService} = {};
    private defaultLogConfiguration: LogConfiguration;

    constructor(private windowRefService: WindowRefService ) {
        const win = windowRefService.nativeWindow as unknown as ServoyWindow;
        win.logFactory = this;
        this.defaultLogConfiguration = win.svyLogConfiguration;
        if ( this.defaultLogConfiguration == null ) {
            this.defaultLogConfiguration = new LogConfiguration();
            win.svyLogConfiguration = this.defaultLogConfiguration;
            win.logLevels = {error: LogLevel.ERROR, debug: LogLevel.DEBUG, info: LogLevel.INFO, warn: LogLevel.WARN, spam: LogLevel.SPAM};
        }
    }

    public getLogger(cls: string): LoggerService {
        if (this.instances[cls] === undefined) {
            this.instances[cls] = new LoggerService(this.windowRefService, new LogConfiguration(this.defaultLogConfiguration.isDebugMode, this.defaultLogConfiguration.level), cls);
        }
        return this.instances[cls];
    }
}
