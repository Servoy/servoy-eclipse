import { describe, it, expect, beforeEach, vi } from 'vitest';
import { UIBlockerService } from './ui_blocker.service';

describe('UIBlockerService', () => {
  let servoyService: any;
  let service: UIBlockerService;
  let uiProperty: any;

  beforeEach(() => {
    uiProperty = null;
    servoyService = {
      getUIProperties: vi.fn().mockReturnValue({ getUIProperty: vi.fn(() => uiProperty) }),
    };
    service = new UIBlockerService(servoyService);
  });

  it('should not block when blocking is disabled', () => {
    uiProperty = false;
    expect(service.shouldBlockDuplicateEvents('c1', {}, 'onClick')).toBe(false);
    expect(service.shouldBlockDuplicateEvents('c1', {}, 'onClick')).toBe(false);
  });

  it('should block a duplicate event when blocking is enabled via UI property', () => {
    uiProperty = true;
    expect(service.shouldBlockDuplicateEvents('c1', {}, 'onClick')).toBe(false);
    expect(service.shouldBlockDuplicateEvents('c1', {}, 'onClick')).toBe(true);
  });

  it('should use the component clientProperty override over the UI property', () => {
    uiProperty = false;
    const model = { clientProperty: { ngBlockDuplicateEvents: true } };
    expect(service.shouldBlockDuplicateEvents('c1', model, 'onClick')).toBe(false);
    expect(service.shouldBlockDuplicateEvents('c1', model, 'onClick')).toBe(true);
  });

  it('should distinguish events by rowId', () => {
    uiProperty = true;
    expect(service.shouldBlockDuplicateEvents('c1', {}, 'onClick', 'row1')).toBe(false);
    expect(service.shouldBlockDuplicateEvents('c1', {}, 'onClick', 'row2')).toBe(false);
    expect(service.shouldBlockDuplicateEvents('c1', {}, 'onClick', 'row1')).toBe(true);
  });

  it('eventExecuted should remove an executing event so it is no longer blocked', () => {
    uiProperty = true;
    service.shouldBlockDuplicateEvents('c1', {}, 'onClick');
    service.eventExecuted('c1', {}, 'onClick');
    expect(service.shouldBlockDuplicateEvents('c1', {}, 'onClick')).toBe(false);
    expect(service.executingEvents.length).toBe(1);
  });

  it('eventExecuted should be a no-op for an unknown event', () => {
    service.shouldBlockDuplicateEvents('c1', {}, 'onClick');
    service.eventExecuted('other', {}, 'onBlur');
    expect(service.executingEvents.length).toBe(1);
  });
});
