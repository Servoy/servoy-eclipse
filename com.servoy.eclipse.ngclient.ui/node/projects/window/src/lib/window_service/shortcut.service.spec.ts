import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { ShortcutService, Shortcut } from './shortcut.service';

describe('ShortcutService', () => {
    let service: ShortcutService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [ShortcutService]
        });
        service = TestBed.inject(ShortcutService);
    });

    afterEach(() => {
        // clean up any listeners registered on the document during a test
        Object.keys(service.all_shortcuts).forEach(key => service.remove(key));
    });

    const dispatchKey = (target: EventTarget, init: KeyboardEventInit & { which?: number }) => {
        const event = new KeyboardEvent('keydown', { bubbles: true, cancelable: true, ...init });
        // jsdom does not derive keyCode/which from the init in all cases; set them explicitly
        if (init.keyCode !== undefined) {
            Object.defineProperty(event, 'keyCode', { value: init.keyCode });
        }
        if (init.which !== undefined) {
            Object.defineProperty(event, 'which', { value: init.which });
        }
        target.dispatchEvent(event);
        return event;
    };

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should register a shortcut in all_shortcuts with the document as default target and keydown type', () => {
        const callback = vi.fn().mockReturnValue(true);
        service.add('a', callback);

        const binding = service.all_shortcuts['a'];
        expect(binding).toBeTruthy();
        expect(binding.target).toBe(document);
        expect(binding.event).toBe('keydown');
        expect(typeof binding.callback).toBe('function');
    });

    it('should attach the listener to the document and invoke the callback on matching key', () => {
        const callback = vi.fn().mockReturnValue(true);
        service.add('A', callback, { propagate: true } as Shortcut);

        // 'A' -> String.fromCharCode(65)
        dispatchKey(document, { keyCode: 65 });

        expect(callback).toHaveBeenCalledTimes(1);
    });

    it('should not invoke the callback when a different key is pressed', () => {
        const callback = vi.fn().mockReturnValue(true);
        service.add('A', callback, { propagate: true } as Shortcut);

        dispatchKey(document, { keyCode: 66 }); // 'B'

        expect(callback).not.toHaveBeenCalled();
    });

    it('should resolve a string target via getElementById', () => {
        const el = document.createElement('div');
        el.id = 'myTarget';
        document.body.appendChild(el);
        const addSpy = vi.spyOn(el, 'addEventListener');
        const callback = vi.fn().mockReturnValue(true);

        service.add('A', callback, { target: 'myTarget' } as Shortcut);

        expect(service.all_shortcuts['A'].target).toBe(el);
        expect(addSpy).toHaveBeenCalledWith('keydown', expect.any(Function), false);

        document.body.removeChild(el);
    });

    it('should match ctrl modifier combinations', () => {
        const callback = vi.fn().mockReturnValue(true);
        service.add('CTRL+A', callback, { propagate: true } as Shortcut);

        // without ctrl -> no match
        dispatchKey(document, { keyCode: 65 });
        expect(callback).not.toHaveBeenCalled();

        // with ctrl -> match
        dispatchKey(document, { keyCode: 65, ctrlKey: true });
        expect(callback).toHaveBeenCalledTimes(1);
    });

    it('should match special keys by name (f1)', () => {
        const callback = vi.fn().mockReturnValue(true);
        service.add('f1', callback, { propagate: true } as Shortcut);

        dispatchKey(document, { keyCode: 112 });
        expect(callback).toHaveBeenCalledTimes(1);
    });

    it('should match using an explicit keycode option', () => {
        const callback = vi.fn().mockReturnValue(true);
        service.add('x', callback, { propagate: true, keycode: 112 } as unknown as Shortcut);

        dispatchKey(document, { keyCode: 112 });
        expect(callback).toHaveBeenCalledTimes(1);
    });

    it('should not fire when disable_in_input is set and the event target is an INPUT', () => {
        const input = document.createElement('input');
        document.body.appendChild(input);
        const callback = vi.fn().mockReturnValue(true);
        service.add('A', callback, { propagate: true, disable_in_input: true } as Shortcut);

        dispatchKey(input, { keyCode: 65 });
        expect(callback).not.toHaveBeenCalled();

        document.body.removeChild(input);
    });

    it('should fire when disable_in_input is set but the target is not an input', () => {
        const div = document.createElement('div');
        document.body.appendChild(div);
        const callback = vi.fn().mockReturnValue(true);
        service.add('A', callback, { propagate: true, disable_in_input: true } as Shortcut);

        dispatchKey(div, { keyCode: 65 });
        expect(callback).toHaveBeenCalledTimes(1);

        document.body.removeChild(div);
    });

    it('should stop the event when propagate is false and callback returns true', () => {
        const callback = vi.fn().mockReturnValue(true);
        service.add('A', callback, { propagate: false } as Shortcut);

        const event = dispatchKey(document, { keyCode: 65 });
        expect(callback).toHaveBeenCalledTimes(1);
        expect(event.defaultPrevented).toBe(true);
    });

    it('should stop the event when callback returns false even if propagate is true', () => {
        const callback = vi.fn().mockReturnValue(false);
        service.add('A', callback, { propagate: true } as Shortcut);

        const event = dispatchKey(document, { keyCode: 65 });
        expect(callback).toHaveBeenCalledTimes(1);
        expect(event.defaultPrevented).toBe(true);
    });

    it('should not prevent default when propagate is true and callback returns true', () => {
        const callback = vi.fn().mockReturnValue(true);
        service.add('A', callback, { propagate: true } as Shortcut);

        const event = dispatchKey(document, { keyCode: 65 });
        expect(callback).toHaveBeenCalledTimes(1);
        expect(event.defaultPrevented).toBe(false);
    });

    it('should translate punctuation keycodes (comma -> ,)', () => {
        const callback = vi.fn().mockReturnValue(true);
        service.add(',', callback, { propagate: true } as Shortcut);

        dispatchKey(document, { keyCode: 188 });
        expect(callback).toHaveBeenCalledTimes(1);
    });

    it('should not match a plain number key against its shifted symbol when shift is not wanted', () => {
        const callback = vi.fn().mockReturnValue(true);
        // '!' is the shifted form of '1'; pressing shift+1 sets shiftKey which is NOT
        // wanted by the '!' combination, so the modifier check must reject it
        service.add('!', callback, { propagate: true } as Shortcut);

        dispatchKey(document, { keyCode: 49, shiftKey: true });
        expect(callback).not.toHaveBeenCalled();
    });

    it('should remove a registered shortcut and detach the listener', () => {
        const callback = vi.fn().mockReturnValue(true);
        const removeSpy = vi.spyOn(document, 'removeEventListener');
        service.add('A', callback, { propagate: true } as Shortcut);

        service.remove('A');

        expect(service.all_shortcuts['A']).toBeUndefined();
        expect(removeSpy).toHaveBeenCalled();

        // after removal the callback no longer fires
        dispatchKey(document, { keyCode: 65 });
        expect(callback).not.toHaveBeenCalled();
        removeSpy.mockRestore();
    });

    it('should be a no-op when removing a non-existent shortcut', () => {
        expect(() => service.remove('does-not-exist')).not.toThrow();
    });

    it('should fill option defaults when a partial option object is passed', () => {
        const callback = vi.fn().mockReturnValue(true);
        const opt = { propagate: true } as Shortcut;
        service.add('A', callback, opt);

        // defaults merged into the passed-in object
        expect(opt['type']).toBe('keydown');
        expect(opt['disable_in_input']).toBe(false);
        expect(opt['target']).toBe(document);
    });
});
