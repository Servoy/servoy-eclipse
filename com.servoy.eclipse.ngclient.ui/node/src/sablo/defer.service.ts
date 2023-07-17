import { Injectable } from '@angular/core';
import { Deferred, IDeferred } from '@servoy/public';

import { LoggerService, LoggerFactory } from '@servoy/public';
import { SabloService } from './sablo.service';

@Injectable({
  providedIn: 'root'
})
export class SabloDeferHelper {

    private log: LoggerService;

    constructor(logFactory: LoggerFactory, private sabloService: SabloService) {
        this.log = logFactory.getLogger('SabloDeferHelper');
    }

    public initInternalStateForDeferringFromOldInternalState(state: IDeferedState, oldState: IDeferedState) {
        state.init(oldState.deferred, oldState.timeoutRejectLogPrefix);
    }

    public initInternalStateForDeferring(state: IDeferedState, timeoutRejectLogPrefix: string) {
        state.init({}, timeoutRejectLogPrefix);
    }

    /**
     * this deletes the sate from the states.deferred list and returns it if it is found (and clears the timeout)
     * Use SabloService.resolveDeferedEvent to resolve or reject the Deferred, dont call those directly on them.
     */
    public retrieveDeferForHandling(msgId: number, state: IDeferedState): IDeferred<any> {
        const deferred = state.deferred[msgId];
        let defer: IDeferred<any>;
        if (deferred) {
            defer = deferred.defer;
            clearTimeout(deferred.timeoutId);
            delete state.deferred[msgId];
        }
        return defer;
    }

    /**
     * this one  gets the defered from the state, clears the timeout and resolved or rejects the defered if found.
     */
    public resolveDeferedEvent(msgId: number, state: IDeferedState, argument: any, resolve: boolean) {
        const defer = this.retrieveDeferForHandling(msgId, state);
        if (defer) {
             this.sabloService.resolveDeferedEvent(msgId, argument, resolve);
        }
    }

    public getNewDeferId(state: IDeferedState): number {
        const deferred = this.sabloService.createDeferredWSEvent();
        const newMsgID = deferred.cmsgid;
        const d = deferred.deferred;
        state.deferred[newMsgID + ''] = {
            defer: d, timeoutId: setTimeout(() => {
                // if nothing comes back for a while do cancel the promise to avoid memory leaks/infinite waiting
                const defer = this.retrieveDeferForHandling(newMsgID, state);
                if (defer) {
                    const rejMsg = 'deferred req. with id ' + newMsgID + ' was rejected due to timeout...';
                    this.sabloService.resolveDeferedEvent(newMsgID, rejMsg, false);
                    this.log.spam((state.timeoutRejectLogPrefix ? state.timeoutRejectLogPrefix : '') + rejMsg);
                }
            }, 120000)
        }; // is 2 minutes cancel-if-not-resolved too high or too low?

        return newMsgID;
    }

    public cancelAll(state: IDeferedState) {
        for (const id of Object.keys(state.deferred)) {
            clearTimeout(state.deferred[id].timeoutId);
            state.deferred[id].defer.reject(id + ' cancelled!');
        }
        state.deferred = {};
    }
}

export interface IDeferedState {
    deferred: { [key: string]: { defer: IDeferred<any>; timeoutId: any } };
    timeoutRejectLogPrefix: string;
    init(deferred: { [key: string]: { defer: IDeferred<any>; timeoutId: any } }, timeoutRejectLogPrefix: string): void;
}
