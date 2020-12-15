import { Injectable } from '@angular/core';
import { Deferred, IDeferred } from '../sablo/util/deferred';

import {LoggerService, LoggerFactory} from './logger.service';

@Injectable()
export class SabloDeferHelper {

    private log: LoggerService;
    constructor(private logFactory: LoggerFactory) {
        this.log = logFactory.getLogger('SabloDeferHelper');
    }

    public initInternalStateForDeferringFromOldInternalState(state: IDeferedState, oldState: IDeferedState) {
        state.init(oldState.deferred, oldState.currentMsgId, oldState.timeoutRejectLogPrefix);
    }

    public initInternalStateForDeferring(state: IDeferedState, timeoutRejectLogPrefix: string) {
        state.init({}, 0, timeoutRejectLogPrefix);
    }

    public retrieveDeferForHandling(msgId: number, state: IDeferedState): Deferred<any> {
        const deferred = state.deferred[msgId];
        let defer;
        if (deferred) {
            defer = deferred.defer;
            clearTimeout(deferred.timeoutId);
            delete state.deferred[msgId];
            // if (Object.keys(internalState.deferred).length == 0) $sabloTestability.block(false);
        }
        return defer;
    }

    public getNewDeferId(state: IDeferedState): number {
        // if (Object.keys(internalState.deferred).length == 0) $sabloTestability.block(true);

        const newMsgID = ++state.currentMsgId;
        const d = new Deferred<any>();
        state.deferred[newMsgID + ''] = { defer: d, timeoutId : setTimeout(() => {
            // if nothing comes back for a while do cancel the promise to avoid memory leaks/infinite waiting
                const defer = this.retrieveDeferForHandling(newMsgID, state);
                if (defer) {
                    const rejMsg = 'deferred req. with id ' + newMsgID + ' was rejected due to timeout...';
                    defer.reject(rejMsg);
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
    deferred: {[key: string]: {defer: IDeferred<any>; timeoutId: any}};
    currentMsgId: number;
    timeoutRejectLogPrefix: string;
    init(deferred: {[key: string]: {defer: IDeferred<any>; timeoutId: any}}, currentMsgId: number, timeoutRejectLogPrefix: string): void;
}
