import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { SabloDeferHelper, IDeferedState } from './defer.service';
import { SabloService } from './sablo.service';
import { LoggerFactory, WindowRefService, Deferred } from '@servoy/public';

class DeferedState implements IDeferedState {
  deferred: Record<string, { defer: Deferred<any>; timeoutId: any }> = {};
  timeoutRejectLogPrefix!: string;
  init(deferred: Record<string, { defer: Deferred<any>; timeoutId: any }>, timeoutRejectLogPrefix: string): void {
    this.deferred = deferred;
    this.timeoutRejectLogPrefix = timeoutRejectLogPrefix;
  }
}

describe('SabloDeferHelper', () => {
  let helper: SabloDeferHelper;
  let sabloService: any;
  let nextCmsgid: number;

  beforeEach(() => {
    nextCmsgid = 1;
    sabloService = {
      createDeferredWSEvent: vi.fn().mockImplementation(() => ({ deferred: new Deferred<any>(), cmsgid: nextCmsgid++ })),
      resolveDeferedEvent: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [SabloDeferHelper, LoggerFactory, WindowRefService, { provide: SabloService, useFactory: () => sabloService }],
    });
    helper = TestBed.inject(SabloDeferHelper);
  });

  it('should be created', () => {
    expect(helper).toBeTruthy();
  });

  it('initInternalStateForDeferring should initialise the state with an empty deferred map and a prefix', () => {
    const state = new DeferedState();
    helper.initInternalStateForDeferring(state, 'prefix: ');
    expect(state.deferred).toEqual({});
    expect(state.timeoutRejectLogPrefix).toBe('prefix: ');
  });

  it('initInternalStateForDeferringFromOldInternalState should copy deferred map and prefix from old state', () => {
    const oldState = new DeferedState();
    const defer = new Deferred<any>();
    oldState.deferred = { 5: { defer, timeoutId: 1 } };
    oldState.timeoutRejectLogPrefix = 'old: ';
    const newState = new DeferedState();
    helper.initInternalStateForDeferringFromOldInternalState(newState, oldState);
    expect(newState.deferred).toBe(oldState.deferred);
    expect(newState.timeoutRejectLogPrefix).toBe('old: ');
  });

  it('getNewDeferId should register a new deferred keyed by cmsgid and return the id', () => {
    const state = new DeferedState();
    helper.initInternalStateForDeferring(state, '');
    const id = helper.getNewDeferId(state);
    expect(id).toBe(1);
    expect(state.deferred[id + '']).toBeDefined();
    expect(state.deferred[id + ''].defer).toBeInstanceOf(Deferred);
    expect(sabloService.createDeferredWSEvent).toHaveBeenCalledTimes(1);
  });

  it('retrieveDeferForHandling should return and remove the matching defer and clear its timeout', () => {
    vi.useFakeTimers();
    const clearSpy = vi.spyOn(globalThis, 'clearTimeout');
    const state = new DeferedState();
    helper.initInternalStateForDeferring(state, '');
    const id = helper.getNewDeferId(state);
    const deferRef = state.deferred[id + ''].defer;

    const defer = helper.retrieveDeferForHandling(id, state);
    expect(defer).toBe(deferRef);
    expect(state.deferred[id + '']).toBeUndefined();
    expect(clearSpy).toHaveBeenCalled();
    vi.useRealTimers();
  });

  it('retrieveDeferForHandling should return undefined when no defer is found', () => {
    const state = new DeferedState();
    helper.initInternalStateForDeferring(state, '');
    expect(helper.retrieveDeferForHandling(999, state)).toBeUndefined();
  });

  it('resolveDeferedEvent should resolve through sabloService only when the defer exists', () => {
    const state = new DeferedState();
    helper.initInternalStateForDeferring(state, '');
    const id = helper.getNewDeferId(state);

    helper.resolveDeferedEvent(id, state, 'result', true);
    expect(sabloService.resolveDeferedEvent).toHaveBeenCalledWith(id, 'result', true);
    // the defer was removed by retrieveDeferForHandling
    expect(state.deferred[id + '']).toBeUndefined();
  });

  it('resolveDeferedEvent should do nothing when the defer does not exist', () => {
    const state = new DeferedState();
    helper.initInternalStateForDeferring(state, '');
    helper.resolveDeferedEvent(123, state, 'result', true);
    expect(sabloService.resolveDeferedEvent).not.toHaveBeenCalled();
  });

  it('the auto-timeout should reject the deferred via sabloService after 2 minutes', () => {
    vi.useFakeTimers();
    const state = new DeferedState();
    helper.initInternalStateForDeferring(state, 'TIMEOUT: ');
    const id = helper.getNewDeferId(state);

    vi.advanceTimersByTime(120000);

    expect(sabloService.resolveDeferedEvent).toHaveBeenCalledWith(id, expect.stringContaining('rejected due to timeout'), false);
    expect(state.deferred[id + '']).toBeUndefined();
    vi.useRealTimers();
  });

  it('cancelAll should reject every pending deferred and clear the map', async () => {
    vi.useFakeTimers();
    const clearSpy = vi.spyOn(globalThis, 'clearTimeout');
    const state = new DeferedState();
    helper.initInternalStateForDeferring(state, '');
    const id1 = helper.getNewDeferId(state);
    const id2 = helper.getNewDeferId(state);
    const promise1 = state.deferred[id1 + ''].defer.promise;
    const promise2 = state.deferred[id2 + ''].defer.promise;
    promise1.catch(() => {});
    promise2.catch(() => {});

    helper.cancelAll(state);

    expect(state.deferred).toEqual({});
    expect(clearSpy).toHaveBeenCalledTimes(2);
    await expect(promise1).rejects.toContain('cancelled!');
    await expect(promise2).rejects.toContain('cancelled!');
    vi.useRealTimers();
  });
});
