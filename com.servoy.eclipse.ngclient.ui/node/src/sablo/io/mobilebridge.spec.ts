import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

import { MobileBridge } from './mobilebridge';
import { WebsocketCustomEvent } from './iwebsocket';

describe('MobileBridge', () => {
  let messageListener: ((event: any) => void) | null;
  let getElementByIdSpy: any;
  let iframeContentWindow: { postMessage: ReturnType<typeof vi.fn> };
  let windowRef: any;

  beforeEach(() => {
    vi.useFakeTimers();
    messageListener = null;
    iframeContentWindow = { postMessage: vi.fn() };
    getElementByIdSpy = vi.fn().mockReturnValue(null);
    windowRef = {
      nativeWindow: {
        addEventListener: (name: string, listener: (event: any) => void) => {
          if (name === 'message') messageListener = listener;
        },
        document: {
          getElementById: getElementByIdSpy,
        },
      },
    };
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  const createBridge = () => {
    const bridge = new MobileBridge(windowRef);
    bridge.onopen = vi.fn();
    bridge.onmessage = vi.fn();
    bridge.onclose = vi.fn();
    bridge.onerror = vi.fn();
    bridge.onconnecting = vi.fn();
    return bridge;
  };

  it('should register a message listener on the native window', () => {
    createBridge();
    expect(messageListener).toBeInstanceOf(Function);
  });

  it('should fire onopen after the initial timeout', () => {
    const bridge = createBridge();
    expect(bridge.onopen).not.toHaveBeenCalled();
    vi.advanceTimersByTime(10);
    expect(bridge.onopen).toHaveBeenCalledTimes(1);
    const evt = (bridge.onopen as any).mock.calls[0][0];
    expect(evt).toBeInstanceOf(WebsocketCustomEvent);
    expect(evt.name).toBe('open');
  });

  it('should forward GWT message events to onmessage', () => {
    const bridge = createBridge();
    vi.advanceTimersByTime(10);
    messageListener!({ data: { from: 'gwt', data: 'hello' } });
    expect(bridge.onmessage).toHaveBeenCalledTimes(1);
    const evt = (bridge.onmessage as any).mock.calls[0][0];
    expect(evt.name).toBe('message');
    expect(evt.data).toBe('hello');
  });

  it('should ignore message events that are not from gwt', () => {
    const bridge = createBridge();
    vi.advanceTimersByTime(10);
    messageListener!({ data: { from: 'other', data: 'ignored' } });
    expect(bridge.onmessage).not.toHaveBeenCalled();
  });

  it('should ignore message events without data', () => {
    const bridge = createBridge();
    vi.advanceTimersByTime(10);
    messageListener!({});
    expect(bridge.onmessage).not.toHaveBeenCalled();
  });

  it('should answer a ping (P) with a pong (p) message instead of posting to the iframe', () => {
    const bridge = createBridge();
    vi.advanceTimersByTime(10);
    bridge.send('P');
    expect(bridge.onmessage).toHaveBeenCalledTimes(1);
    const evt = (bridge.onmessage as any).mock.calls[0][0];
    expect(evt.name).toBe('message');
    expect(evt.data).toBe('p');
    expect(getElementByIdSpy).not.toHaveBeenCalled();
  });

  it('should post other data to the mobileclient iframe when present', () => {
    getElementByIdSpy.mockReturnValue({ contentWindow: iframeContentWindow });
    const bridge = createBridge();
    vi.advanceTimersByTime(10);
    bridge.send('some data');
    expect(getElementByIdSpy).toHaveBeenCalledWith('mobileclient');
    expect(iframeContentWindow.postMessage).toHaveBeenCalledWith('some data');
  });

  it('should not throw when sending data while the iframe is missing', () => {
    getElementByIdSpy.mockReturnValue(null);
    const bridge = createBridge();
    vi.advanceTimersByTime(10);
    expect(() => bridge.send('some data')).not.toThrow();
    expect(iframeContentWindow.postMessage).not.toHaveBeenCalled();
  });

  it('close should not throw', () => {
    const bridge = createBridge();
    vi.advanceTimersByTime(10);
    expect(() => bridge.close()).not.toThrow();
  });
});
