import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { PopupStateService } from './popup-state.service';

describe('PopupStateService', () => {
    let service: PopupStateService;

    beforeEach(() => {
        service = new PopupStateService();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('should have no active popups initially', () => {
        expect(service.isAnyPopupActive()).toBe(false);
        expect(service.isPopupActive()).toBe(false);
    });

    it('should mark a popup active when activated', () => {
        service.activatePopup('p1');
        expect(service.isAnyPopupActive()).toBe(true);
        expect(service.isPopupActive()).toBe(true);
    });

    it('should track multiple distinct popups', () => {
        service.activatePopup('p1');
        service.activatePopup('p2');
        expect(service.isAnyPopupActive()).toBe(true);
    });

    it('activating the same id twice should not double-count', () => {
        service.activatePopup('p1');
        service.activatePopup('p1');
        service.activatePopup('p2');
        // deactivate p2, p1 still active
        vi.useFakeTimers();
        service.deactivatePopup('p2');
        vi.advanceTimersByTime(200);
        expect(service.isAnyPopupActive()).toBe(true);
    });

    it('should deactivate a popup after the 200ms delay', () => {
        vi.useFakeTimers();
        service.activatePopup('p1');
        service.deactivatePopup('p1');
        // still active before the delay elapses
        expect(service.isAnyPopupActive()).toBe(true);
        vi.advanceTimersByTime(200);
        expect(service.isAnyPopupActive()).toBe(false);
        expect(service.isPopupActive()).toBe(false);
    });
});
