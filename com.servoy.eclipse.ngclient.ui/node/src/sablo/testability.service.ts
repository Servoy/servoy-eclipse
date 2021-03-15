import { Injectable } from '@angular/core';
import { LoggerFactory, LoggerService } from './logger.service';
import { Deferred } from './util/deferred';
import { WindowRefService } from './util/windowref.service';

@Injectable()
export class TestabilityService {
    private log: LoggerService;
    private blockEventLoop = 0;
    private deferredEvents: Array<Deferred<any>>;
    private deferredLength = 0;
    // add a special testability method to the window object so that protractor can ask if there are waiting server calls.
    private callbackForTesting: (param: boolean) => void;

    constructor(logFactory: LoggerFactory, windowRef: WindowRefService) {
        this.log = logFactory.getLogger('TestabilityService');
        windowRef.nativeWindow['testForDeferredSabloEvents'] = (callback: (param: boolean) => void) => {
            console.log('callback    '   +   !this.blockEventLoop && Object.keys(this.deferredEvents).length === this.deferredLength)
            if (!this.blockEventLoop && Object.keys(this.deferredEvents).length === this.deferredLength) callback(false); // false means there was no waiting deferred at all.
            else {
                this.callbackForTesting = callback;
            }
        };
    }

    setEventList(eventList: Array<Deferred<any>>) {
        this.deferredEvents = eventList;
    }
    testEvents() {
        if (!this.blockEventLoop && this.callbackForTesting && Object.keys(this.deferredEvents).length === this.deferredLength) {
            this.callbackForTesting(true); // true: let protractor know that an event did happen.
            this.callbackForTesting = null;
        }
    }

    increaseEventLoop() {
        this.deferredLength++;
        if (!this.blockEventLoop && this.callbackForTesting) {
            this.callbackForTesting(true);
            this.callbackForTesting = null;
        }
    }

    decreaseEventLoop() {
        if (this.deferredLength > 0) this.deferredLength--;
        else this.log.warn('decrease event loop called without an increase');
    }

    block(block: boolean) {
        if (block) this.blockEventLoop++;
        else this.blockEventLoop--;
        if (!this.blockEventLoop && this.callbackForTesting) {
            this.callbackForTesting(true);
            this.callbackForTesting = null;
        }
    }

}
