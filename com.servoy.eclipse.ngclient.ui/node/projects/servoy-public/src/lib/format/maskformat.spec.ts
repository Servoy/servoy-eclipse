import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { Renderer2 } from '@angular/core';
import { MaskFormat } from './maskformat';
import { Format, FormattingService } from './formatting.service';
import { ServoyPublicServiceTestingImpl } from '../testing/publictesting.module';

/**
 * MaskFormat wires raw DOM event listeners on a real input element through a
 * Renderer2. We use a real jsdom <input> and a renderer whose listen() attaches
 * handlers with addEventListener, so we can drive real keypress/keydown/focus
 * events and assert the masked buffer that gets written back into the element.
 */
const makeRenderer = (): Renderer2 => ({
    listen: vi.fn((target: any, event: string, cb: (e: any) => void) => {
        target.addEventListener(event, cb);
        return () => target.removeEventListener(event, cb);
    })
} as unknown as Renderer2);

const makeFormat = (edit: string, overrides: Partial<Format> = {}): Format =>
    Object.assign(new Format(), { type: 'TEXT', isMask: true, edit, placeHolder: '', ...overrides });

describe('MaskFormat', () => {
    let input: HTMLInputElement;
    let renderer: Renderer2;
    let formatService: FormattingService;

    beforeEach(() => {
        input = document.createElement('input');
        document.body.appendChild(input);
        renderer = makeRenderer();
        formatService = new FormattingService(new ServoyPublicServiceTestingImpl());
    });

    afterEach(() => {
        input.remove();
        vi.useRealTimers();
    });

    const create = (edit: string, overrides: Partial<Format> = {}): MaskFormat =>
        new MaskFormat(makeFormat(edit, overrides), renderer, input, formatService, document);

    it('should register input/focus/blur/keypress/keydown listeners', () => {
        create('##-##');
        const events = (renderer.listen as any).mock.calls.map((c: any[]) => c[1]);
        expect(events).toEqual(expect.arrayContaining(['input', 'focus', 'blur', 'keypress', 'keydown']));
    });

    it('should initialise the buffer with placeholders for mask positions and keep literals', () => {
        create('##-##', { placeHolder: '_' });
        // the input starts empty; focus should write the placeholder buffer
        input.dispatchEvent(new Event('focus'));
        expect(input.value).toBe('__-__');
    });

    it('should type numeric characters into the mask positions', () => {
        create('##-##', { placeHolder: '_' });
        input.dispatchEvent(new Event('focus'));
        input.setSelectionRange(0, 0); // caret at the start
        input.dispatchEvent(new KeyboardEvent('keypress', { keyCode: 49 } as any)); // '1'
        expect(input.value.charAt(0)).toBe('1');
    });

    it('should reject characters that do not match the mask definition', () => {
        create('##-##', { placeHolder: '_' });
        input.dispatchEvent(new Event('focus'));
        input.setSelectionRange(0, 0);
        // 'A' (keyCode 65) is not a digit, so position 0 should remain the placeholder
        input.dispatchEvent(new KeyboardEvent('keypress', { keyCode: 65 } as any));
        expect(input.value.charAt(0)).toBe('_');
    });

    it('should uppercase characters for a U mask definition via the converter', () => {
        create('UU', { placeHolder: '_' });
        input.dispatchEvent(new Event('focus'));
        input.setSelectionRange(0, 0);
        // type 'a' (97) -> should be converted to 'A'
        input.dispatchEvent(new KeyboardEvent('keypress', { keyCode: 97 } as any));
        expect(input.value.charAt(0)).toBe('A');
    });

    it('should honor allowedCharacters for the * definition', () => {
        create('*', { placeHolder: '_', allowedCharacters: 'xyz' });
        input.dispatchEvent(new Event('focus'));
        input.setSelectionRange(0, 0);
        // 'x' (120) allowed
        input.dispatchEvent(new KeyboardEvent('keypress', { keyCode: 120 } as any));
        expect(input.value.charAt(0)).toBe('x');
    });

    it('should treat Enter as a change trigger that dispatches a change event', () => {
        create('##-##');
        const changeSpy = vi.fn();
        input.addEventListener('change', changeSpy);
        input.dispatchEvent(new KeyboardEvent('keypress', { keyCode: 13, which: 13 } as any));
        expect(changeSpy).toHaveBeenCalled();
    });

    it('blur should dispatch a change event when the value changed since focus', () => {
        create('##-##', { placeHolder: '_' });
        const changeSpy = vi.fn();
        input.addEventListener('change', changeSpy);
        input.dispatchEvent(new Event('focus')); // focusText captured, buffer written
        // type a digit so the value differs from focusText on blur
        input.setSelectionRange(0, 0);
        input.dispatchEvent(new KeyboardEvent('keypress', { keyCode: 49 } as any));
        changeSpy.mockClear();
        input.dispatchEvent(new Event('blur'));
        expect(changeSpy).toHaveBeenCalled();
    });

    it('destroy should unregister all the listeners', () => {
        const unregister = vi.fn();
        (renderer.listen as any).mockReturnValue(unregister);
        const mask = create('##-##');
        mask.destroy();
        // five listeners were registered -> five unregister calls
        expect(unregister).toHaveBeenCalledTimes(5);
    });

    it('should skip escaped characters in the mask (quote literal)', () => {
        // the "'" escapes the next char as a literal, it is not a mask position
        create("##'x'##", { placeHolder: '_' });
        input.dispatchEvent(new Event('focus'));
        // literal x sits between the digit groups
        expect(input.value).toContain('x');
    });
});
