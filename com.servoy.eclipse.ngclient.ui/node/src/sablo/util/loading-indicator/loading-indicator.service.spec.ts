import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { LoadingIndicatorService, ICustomLoadingIndicator } from './loading-indicator.service';
import { LoggerFactory, WindowRefService, LogLevel } from '@servoy/public';

describe('LoadingIndicatorService', () => {
  let service: LoadingIndicatorService;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [LoadingIndicatorService, LoggerFactory, WindowRefService],
    });
    service = TestBed.inject(LoadingIndicatorService);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('isShowing should reflect the show counter', () => {
    expect(service.isShowing()).toBe(false);
    service.showLoading();
    expect(service.isShowing()).toBe(true);
    service.hideLoading();
    expect(service.isShowing()).toBe(false);
  });

  it('should emit true after 400ms when loading starts', () => {
    const next = vi.fn();
    service.showLoadingIndicator.subscribe(next);
    service.showLoading();
    expect(next).not.toHaveBeenCalled();
    vi.advanceTimersByTime(399);
    expect(next).not.toHaveBeenCalled();
    vi.advanceTimersByTime(1);
    expect(next).toHaveBeenCalledWith(true);
  });

  it('should emit false after the 50ms hide delay once the counter reaches 0', () => {
    const next = vi.fn();
    service.showLoadingIndicator.subscribe(next);
    service.showLoading();
    vi.advanceTimersByTime(400); // emitted true
    next.mockClear();

    service.hideLoading();
    expect(next).not.toHaveBeenCalled();
    vi.advanceTimersByTime(50);
    expect(next).toHaveBeenCalledWith(false);
  });

  it('should not show indicator if hide happens before the 400ms show timeout fires', () => {
    const next = vi.fn();
    service.showLoadingIndicator.subscribe(next);
    service.showLoading();
    service.hideLoading(); // counter back to 0, schedules hide timeout
    vi.advanceTimersByTime(50); // hide timeout fires, cancels the pending show
    vi.advanceTimersByTime(400);
    expect(next).not.toHaveBeenCalled();
  });

  it('should keep showing while nested show/hide calls keep the counter positive', () => {
    const next = vi.fn();
    service.showLoadingIndicator.subscribe(next);
    service.showLoading();
    service.showLoading();
    vi.advanceTimersByTime(400);
    expect(next).toHaveBeenCalledWith(true);
    next.mockClear();

    service.hideLoading();
    expect(service.isShowing()).toBe(true);
    vi.advanceTimersByTime(400);
    expect(next).not.toHaveBeenCalled();
  });

  it('should use the custom loading indicator when one is set', () => {
    const custom: ICustomLoadingIndicator = { showLoading: vi.fn(), hideLoading: vi.fn() };
    service.setCustomLoadingIndicator(custom);

    service.showLoading();
    vi.advanceTimersByTime(400);
    expect(custom.showLoading).toHaveBeenCalledTimes(1);

    service.hideLoading();
    vi.advanceTimersByTime(50);
    expect(custom.hideLoading).toHaveBeenCalledTimes(1);
  });

  it('should reject an invalid custom loading indicator and log a warning', () => {
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
    service.log.logLevel = LogLevel.WARN;
    service.setCustomLoadingIndicator({ showLoading: undefined } as any);
    expect(service.customLoadingIndicator).toBeNull();
    expect(warnSpy).toHaveBeenCalled();
    warnSpy.mockRestore();
  });

  it('should keep a valid custom loading indicator', () => {
    const custom: ICustomLoadingIndicator = { showLoading: vi.fn(), hideLoading: vi.fn() };
    service.setCustomLoadingIndicator(custom);
    expect(service.customLoadingIndicator).toBe(custom);
  });
});
