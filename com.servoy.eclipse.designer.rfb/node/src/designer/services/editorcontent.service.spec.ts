import { describe, it, expect, beforeEach, vi } from 'vitest';

import { EditorContentService } from './editorcontent.service';

describe('EditorContentService', () => {
  let service: EditorContentService;

  beforeEach(() => {
    service = Object.create(EditorContentService.prototype);
    (service as any).afterInitCallbacks = [];
    (service as any).contentMessageListeners = [];
    (service as any).contentWasInit = false;
  });

  describe('executeOnlyAfterInit', () => {
    it('should queue callback when content is not yet initialized', () => {
      const callback = vi.fn();
      service.executeOnlyAfterInit(callback);
      expect(callback).not.toHaveBeenCalled();
      expect((service as any).afterInitCallbacks.length).toBe(1);
    });

    it('should execute callback immediately when content is already initialized', () => {
      (service as any).contentWasInit = true;
      const callback = vi.fn();
      service.executeOnlyAfterInit(callback);
      expect(callback).toHaveBeenCalledOnce();
    });

    it('should queue multiple callbacks and not execute them before init', () => {
      const cb1 = vi.fn();
      const cb2 = vi.fn();
      const cb3 = vi.fn();
      service.executeOnlyAfterInit(cb1);
      service.executeOnlyAfterInit(cb2);
      service.executeOnlyAfterInit(cb3);
      expect(cb1).not.toHaveBeenCalled();
      expect(cb2).not.toHaveBeenCalled();
      expect(cb3).not.toHaveBeenCalled();
      expect((service as any).afterInitCallbacks.length).toBe(3);
    });
  });

  describe('addContentMessageListener / removeContentMessageListener', () => {
    it('should add a listener', () => {
      const listener = { contentMessageReceived: vi.fn() };
      service.addContentMessageListener(listener);
      expect((service as any).contentMessageListeners.length).toBe(1);
    });

    it('should not add the same listener twice', () => {
      const listener = { contentMessageReceived: vi.fn() };
      service.addContentMessageListener(listener);
      service.addContentMessageListener(listener);
      expect((service as any).contentMessageListeners.length).toBe(1);
    });

    it('should remove a listener', () => {
      const listener = { contentMessageReceived: vi.fn() };
      service.addContentMessageListener(listener);
      service.removeContentMessageListener(listener);
      expect((service as any).contentMessageListeners.length).toBe(0);
    });

    it('should only remove the specified listener', () => {
      const listener1 = { contentMessageReceived: vi.fn() };
      const listener2 = { contentMessageReceived: vi.fn() };
      service.addContentMessageListener(listener1);
      service.addContentMessageListener(listener2);
      service.removeContentMessageListener(listener1);
      expect((service as any).contentMessageListeners.length).toBe(1);
      expect((service as any).contentMessageListeners[0]).toBe(listener2);
    });
  });
});
