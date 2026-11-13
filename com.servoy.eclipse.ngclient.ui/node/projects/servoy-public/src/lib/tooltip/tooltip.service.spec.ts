import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { TooltipService } from './tooltip.service';
import { WindowRefService } from '../services/windowref.service';

describe('TooltipService', () => {
    let service: TooltipService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [TooltipService, WindowRefService]
        });
        service = TestBed.inject(TooltipService);
    });

    afterEach(() => {
        vi.useRealTimers();
        // clean up any tooltip div the service created on the body
        document.getElementById('mktipmsg')?.remove();
    });

    const makeMouseEvent = (overrides: Partial<MouseEvent> = {}): MouseEvent => ({
        target: document.createElement('input'),
        pageX: 100,
        pageY: 100,
        ...overrides
    } as unknown as MouseEvent);

    it('isTooltipActiveSignal should start false', () => {
        expect(service.isTooltipActiveSignal()).toBe(false);
    });

    it('showTooltip should create the tooltip div and show it after the initial delay', () => {
        vi.useFakeTimers();
        service.showTooltip(makeMouseEvent(), 'my tip', 100, 5000);

        const div = document.getElementById('mktipmsg') as HTMLDivElement;
        expect(div).toBeTruthy();
        expect(div.innerHTML).toBe('my tip');
        // not shown yet before the initial delay
        expect(div.style.display).not.toBe('block');

        vi.advanceTimersByTime(100);
        expect(div.style.display).toBe('block');
        expect(service.isTooltipActiveSignal()).toBe(true);
    });

    it('should hide the tooltip after the dismiss delay', () => {
        vi.useFakeTimers();
        service.showTooltip(makeMouseEvent(), 'tip', 10, 500);
        vi.advanceTimersByTime(10);
        expect(service.isTooltipActiveSignal()).toBe(true);
        vi.advanceTimersByTime(500);
        const div = document.getElementById('mktipmsg') as HTMLDivElement;
        expect(div.style.display).toBe('none');
        expect(service.isTooltipActiveSignal()).toBe(false);
    });

    it('hideTooltip should hide a showing tooltip', () => {
        vi.useFakeTimers();
        service.showTooltip(makeMouseEvent(), 'tip', 10, 5000);
        vi.advanceTimersByTime(10);
        service.hideTooltip();
        const div = document.getElementById('mktipmsg') as HTMLDivElement;
        expect(div.style.display).toBe('none');
        expect(service.isTooltipActiveSignal()).toBe(false);
    });

    it('should not show a tooltip when hovering over an option element', () => {
        vi.useFakeTimers();
        const option = document.createElement('option');
        service.showTooltip(makeMouseEvent({ target: option }), 'tip', 10, 500);
        // getTooltipDiv is never created because it returns early
        expect(document.getElementById('mktipmsg')).toBeNull();
        vi.advanceTimersByTime(600);
        expect(service.isTooltipActiveSignal()).toBe(false);
    });

    it('should resolve a text node target to its parent (Safari bug defeat)', () => {
        vi.useFakeTimers();
        const parent = document.createElement('span');
        const textNode = document.createTextNode('x');
        parent.appendChild(textNode);
        service.showTooltip(makeMouseEvent({ target: textNode as unknown as EventTarget }), 'tip', 10, 500);
        vi.advanceTimersByTime(10);
        // tooltip still shows because the parent span is not an option
        expect(service.isTooltipActiveSignal()).toBe(true);
    });

    it('reusing showTooltip should reuse the same tooltip div', () => {
        vi.useFakeTimers();
        service.showTooltip(makeMouseEvent(), 'first', 10, 500);
        const div1 = document.getElementById('mktipmsg');
        service.hideTooltip();
        service.showTooltip(makeMouseEvent(), 'second', 10, 500);
        const div2 = document.getElementById('mktipmsg');
        expect(div2).toBe(div1);
        expect(div2!.innerHTML).toBe('second');
    });
});
