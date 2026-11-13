import { describe, it, expect, beforeEach, vi } from 'vitest';
import { Renderer2 } from '@angular/core';
import { PropertyUtils } from './property_utils';

const createRendererMock = (): Renderer2 => ({
    setStyle: vi.fn(),
    removeStyle: vi.fn(),
    setAttribute: vi.fn(),
    createElement: vi.fn((name: string) => ({ tagName: name.toUpperCase(), children: [] as any[] })),
    createText: vi.fn((text: string) => ({ text })),
    appendChild: vi.fn(),
    insertBefore: vi.fn(),
    removeChild: vi.fn(),
    parentNode: vi.fn(),
    listen: vi.fn()
} as unknown as Renderer2);

describe('PropertyUtils', () => {
    let renderer: Renderer2;

    beforeEach(() => {
        renderer = createRendererMock();
    });

    describe('setHorizontalAlignment', () => {
        it('should set center for halign 0', () => {
            PropertyUtils.setHorizontalAlignment({}, renderer, 0);
            expect(renderer.setStyle).toHaveBeenCalledWith({}, 'text-align', 'center');
        });

        it('should set right for halign 4', () => {
            const el = {};
            PropertyUtils.setHorizontalAlignment(el, renderer, 4);
            expect(renderer.setStyle).toHaveBeenCalledWith(el, 'text-align', 'right');
        });

        it('should set left for any other value', () => {
            const el = {};
            PropertyUtils.setHorizontalAlignment(el, renderer, 2);
            expect(renderer.setStyle).toHaveBeenCalledWith(el, 'text-align', 'left');
        });

        it('should do nothing when halign is -1', () => {
            PropertyUtils.setHorizontalAlignment({}, renderer, -1);
            expect(renderer.setStyle).not.toHaveBeenCalled();
        });
    });

    describe('setRotation', () => {
        it('should apply transform styles for the rotation', () => {
            const el = {} as HTMLElement;
            PropertyUtils.setRotation(el, renderer, 45);
            expect(renderer.setStyle).toHaveBeenCalledWith(el, 'transform', 'rotate(45deg)');
            expect(renderer.setStyle).toHaveBeenCalledWith(el, 'position', 'absolute');
        });

        it('should swap width/height and offset when rotated 90 degrees with a size', () => {
            const el = {} as HTMLElement;
            PropertyUtils.setRotation(el, renderer, 90, { width: 100, height: 40 });
            expect(renderer.setStyle).toHaveBeenCalledWith(el, 'width', '40px');
            expect(renderer.setStyle).toHaveBeenCalledWith(el, 'height', '100px');
            expect(renderer.setStyle).toHaveBeenCalledWith(el, 'left', '30px');
            expect(renderer.setStyle).toHaveBeenCalledWith(el, 'top', '-30px');
        });

        it('should also handle 270 degrees with a size', () => {
            const el = {} as HTMLElement;
            PropertyUtils.setRotation(el, renderer, 270, { width: 80, height: 20 });
            expect(renderer.setStyle).toHaveBeenCalledWith(el, 'width', '20px');
            expect(renderer.setStyle).toHaveBeenCalledWith(el, 'height', '80px');
        });

        it('should not swap width/height for a non-perpendicular rotation', () => {
            const el = {} as HTMLElement;
            PropertyUtils.setRotation(el, renderer, 45, { width: 100, height: 40 });
            const widthCall = (renderer.setStyle as any).mock.calls.find((c: any[]) => c[1] === 'width');
            expect(widthCall).toBeUndefined();
        });
    });

    describe('setBorder', () => {
        it('should remove the border when newVal is null', () => {
            const el = {};
            PropertyUtils.setBorder(el, renderer, null);
            expect(renderer.removeStyle).toHaveBeenCalledWith(el, 'border');
        });

        it('should remove the border when newVal is not an object', () => {
            const el = {};
            PropertyUtils.setBorder(el, renderer, 'solid');
            expect(renderer.removeStyle).toHaveBeenCalledWith(el, 'border');
        });

        it('should apply borderStyle entries converting camelCase to kebab-case', () => {
            const el = { childNodes: [] };
            (renderer.parentNode as any).mockReturnValue({ nodeName: 'DIV' });
            PropertyUtils.setBorder(el, renderer, { borderStyle: { borderTopWidth: '2px', borderColor: 'red' } });
            expect(renderer.setStyle).toHaveBeenCalledWith(el, 'border-top-width', '2px');
            expect(renderer.setStyle).toHaveBeenCalledWith(el, 'border-color', 'red');
        });

        it('should build a fieldset for a TitledBorder', () => {
            const child = { nodeType: 1 };
            const el = { childNodes: [child] };
            (renderer.parentNode as any).mockReturnValue({ nodeName: 'DIV' });
            PropertyUtils.setBorder(el, renderer, {
                type: 'TitledBorder', color: 'blue', titleJustification: 'left',
                title: 'My Title', font: { fontSize: '12px' }
            });
            expect(renderer.createElement).toHaveBeenCalledWith('fieldset');
            expect(renderer.createElement).toHaveBeenCalledWith('legend');
            expect(renderer.createText).toHaveBeenCalledWith('My Title');
            // font key converted to kebab-case on the legend
            expect(renderer.setStyle).toHaveBeenCalledWith(expect.anything(), 'font-size', '12px');
        });

        it('should unwrap an existing fieldset parent before applying', () => {
            const el = { childNodes: [] };
            const fieldsetParent = { nodeName: 'FIELDSET' };
            const grandParent = { nodeName: 'DIV' };
            (renderer.parentNode as any)
                .mockReturnValueOnce(fieldsetParent)   // parentNode(element)
                .mockReturnValueOnce(fieldsetParent)   // parent assignment
                .mockReturnValueOnce(grandParent)      // parentNode(parent) for insertBefore
                .mockReturnValueOnce(grandParent)      // parentNode(parent) for removeChild
                .mockReturnValue({ nodeName: 'DIV' });
            PropertyUtils.setBorder(el, renderer, { borderStyle: { borderColor: 'green' } });
            expect(renderer.insertBefore).toHaveBeenCalled();
            expect(renderer.removeChild).toHaveBeenCalled();
        });
    });

    describe('setFont', () => {
        it('should remove the font when newVal is null', () => {
            const el = {};
            PropertyUtils.setFont(el, renderer, null);
            expect(renderer.removeStyle).toHaveBeenCalledWith(el, 'font');
        });

        it('should apply every font property', () => {
            const el = {};
            PropertyUtils.setFont(el, renderer, { 'font-size': '10px', 'font-weight': 'bold' });
            expect(renderer.removeStyle).toHaveBeenCalledWith(el, 'font');
            expect(renderer.setStyle).toHaveBeenCalledWith(el, 'font-size', '10px');
            expect(renderer.setStyle).toHaveBeenCalledWith(el, 'font-weight', 'bold');
        });
    });

    describe('setVisible', () => {
        it('should remove display when visible is true', () => {
            const el = { closest: vi.fn().mockReturnValue(null) };
            (renderer.parentNode as any).mockReturnValue({});
            PropertyUtils.setVisible(el, renderer, true);
            expect(renderer.removeStyle).toHaveBeenCalledWith(el, 'display');
        });

        it('should set display none when visible is false', () => {
            const el = { closest: vi.fn().mockReturnValue(null) };
            (renderer.parentNode as any).mockReturnValue({});
            PropertyUtils.setVisible(el, renderer, false);
            expect(renderer.setStyle).toHaveBeenCalledWith(el, 'display', 'none');
        });

        it('should target the wrapper grand-parent when element is inside a svy-wrapper', () => {
            const wrapper = { id: 'wrapper' };
            const el = { closest: vi.fn().mockReturnValue(wrapper) };
            // parentNode(parentNode(el)) must equal the wrapper returned by closest
            (renderer.parentNode as any)
                .mockReturnValueOnce({ id: 'parent' })  // parentNode(element)
                .mockReturnValueOnce(wrapper)            // parentNode(parent)
                .mockReturnValueOnce({ id: 'parent' })   // re-eval for correctElement assignment
                .mockReturnValueOnce(wrapper);
            PropertyUtils.setVisible(el, renderer, false);
            expect(renderer.setStyle).toHaveBeenCalledWith(wrapper, 'display', 'none');
        });

        it('should do nothing for a value that is neither true nor false', () => {
            const el = { closest: vi.fn().mockReturnValue(null) };
            (renderer.parentNode as any).mockReturnValue({});
            PropertyUtils.setVisible(el, renderer, undefined);
            expect(renderer.setStyle).not.toHaveBeenCalled();
            expect(renderer.removeStyle).not.toHaveBeenCalled();
        });
    });

    describe('addSelectOnEnter', () => {
        it('should register a focus listener', () => {
            const el = { select: vi.fn() };
            PropertyUtils.addSelectOnEnter(el, renderer, document);
            expect(renderer.listen).toHaveBeenCalledWith(el, 'focus', expect.any(Function));
        });

        it('should select the element when it is still the focused element', () => {
            vi.useFakeTimers();
            const el = { select: vi.fn() };
            const doc = { querySelector: vi.fn().mockReturnValue(el) } as unknown as Document;
            PropertyUtils.addSelectOnEnter(el, renderer, doc);
            const focusCb = (renderer.listen as any).mock.calls[0][2];
            focusCb();
            vi.advanceTimersByTime(0);
            expect(el.select).toHaveBeenCalled();
            vi.useRealTimers();
        });

        it('should not select when a different element is focused', () => {
            vi.useFakeTimers();
            const el = { select: vi.fn() };
            const doc = { querySelector: vi.fn().mockReturnValue({}) } as unknown as Document;
            PropertyUtils.addSelectOnEnter(el, renderer, doc);
            const focusCb = (renderer.listen as any).mock.calls[0][2];
            focusCb();
            vi.advanceTimersByTime(0);
            expect(el.select).not.toHaveBeenCalled();
            vi.useRealTimers();
        });
    });

    describe('getScrollbarsStyleObj', () => {
        it('should default to auto/auto when SCROLLBARS_WHEN_NEEDED (0)', () => {
            expect(PropertyUtils.getScrollbarsStyleObj(0)).toEqual({ overflowX: 'auto', overflowY: 'auto' });
        });

        it('should hide both when NEVER bits set (horizontal 32 + vertical 4)', () => {
            expect(PropertyUtils.getScrollbarsStyleObj(32 + 4)).toEqual({ overflowX: 'hidden', overflowY: 'hidden' });
        });

        it('should scroll both when ALWAYS bits set (horizontal 16 + vertical 2)', () => {
            expect(PropertyUtils.getScrollbarsStyleObj(16 + 2)).toEqual({ overflowX: 'scroll', overflowY: 'scroll' });
        });

        it('should handle mixed - horizontal never, vertical always', () => {
            expect(PropertyUtils.getScrollbarsStyleObj(32 + 2)).toEqual({ overflowX: 'hidden', overflowY: 'scroll' });
        });
    });

    describe('setScrollbars', () => {
        it('should apply each overflow style from the computed object', () => {
            const el = {};
            PropertyUtils.setScrollbars(el, renderer, 32 + 4);
            expect(renderer.setStyle).toHaveBeenCalledWith(el, 'overflowX', 'hidden');
            expect(renderer.setStyle).toHaveBeenCalledWith(el, 'overflowY', 'hidden');
        });
    });

    describe('getPropByStringPath', () => {
        it('should resolve a simple property', () => {
            expect(PropertyUtils.getPropByStringPath({ a: 42 }, 'a')).toBe(42);
        });

        it('should resolve a leading dot path', () => {
            expect(PropertyUtils.getPropByStringPath({ a: 1 }, '.a')).toBe(1);
        });

        it('should convert bracket index notation to dot', () => {
            expect(PropertyUtils.getPropByStringPath({ list: ['x', 'y'] }, 'list[0]')).toEqual(['x', 'y']);
        });

        it('should return undefined when the first segment is missing', () => {
            expect(PropertyUtils.getPropByStringPath({ a: 1 }, 'missing')).toBeUndefined();
        });
    });
});
