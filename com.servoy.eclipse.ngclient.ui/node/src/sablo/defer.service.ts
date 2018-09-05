import { Injectable } from '@angular/core';
import { Deferred } from '../sablo/util/deferred';

import {LoggerService, LoggerFactory} from './logger.service'

@Injectable()
export class SabloDeferHelper {
    
    private log: LoggerService;
    constructor(private logFactory:LoggerFactory) {
        this.log = logFactory.getLogger("SabloDeferHelper");
    }
    
    public initInternalStateForDeferringFromOldInternalState(state: IDeferedState, oldState: IDeferedState) {
        state.init(oldState.deferred, oldState.currentMsgId, oldState.timeoutRejectLogPrefix);
    }
    
    public initInternalStateForDeferring(state: IDeferedState, timeoutRejectLogPrefix: string) {
        state.init({}, 0, timeoutRejectLogPrefix);
    }
    
    public retrieveDeferForHandling(msgId: string, state: IDeferedState): Deferred<any> {
        let deferred = state.deferred[msgId];
        let defer = undefined;
        if (deferred) {
            defer = deferred.defer;
            clearTimeout(deferred.timeoutPromise);
            delete state.deferred[msgId];
            //if (Object.keys(internalState.deferred).length == 0) $sabloTestability.block(false);
        }
        return defer;
    }
    
    public getNewDeferId(state: IDeferedState) {
        //if (Object.keys(internalState.deferred).length == 0) $sabloTestability.block(true);
        
        let newMsgID = ++state.currentMsgId;
        let d = new Deferred<any>();
        state.deferred[newMsgID] = { defer: d, timeoutPromise : setTimeout(function() {
            // if nothing comes back for a while do cancel the promise to avoid memory leaks/infinite waiting
                let defer = this.retrieveDeferForHandling(newMsgID, state);
                if (defer) {
                    let rejMsg = "deferred req. with id " + newMsgID + " was rejected due to timeout...";
                    defer.reject(rejMsg);
                    this.log.spam((state.timeoutRejectLogPrefix ? state.timeoutRejectLogPrefix : "") + rejMsg);
                }
                }, 120000)
        }; // is 2 minutes cancel-if-not-resolved too high or too low?

        return newMsgID;
    }
    
    public cancelAll(state: IDeferedState) {        
        for (var id in state.deferred) {
            clearTimeout(state.deferred[id].timeoutPromise);
            state.deferred[id].reject(id+ ' cancelled!');
        }
        state.deferred = {};
    }
}

export interface IDeferedState {
    deferred: Object;
    currentMsgId: number;
    timeoutRejectLogPrefix: string;
    init(deferred: Object, currentMsgId: number, timeoutRejectLogPrefix: string);    
}