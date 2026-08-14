import { vi, describe, beforeEach, it, expect } from 'vitest';
import { signal } from '@angular/core';

import { DynamicGuidesService, SnapData } from './dynamicguides.service';


describe('DynamicGuidesService — SVY-21129', () => {

    let service: DynamicGuidesService;
    let editorContent: Record<string, ReturnType<typeof vi.fn>>;
    let editorSession: any;

    beforeEach(() => {
        editorContent = {
            executeOnlyAfterInit: vi.fn(),
            getGlassPane: vi.fn(),
            getContentArea: vi.fn(),
            getContentElementById: vi.fn().mockReturnValue(null),
            getContentElementsFromPoint: vi.fn(),
            getAllContentElements: vi.fn(),
            getContentForm: vi.fn()
        };

        editorSession = {
            addDynamicGuidesChangedListener: vi.fn(),
            resizing: signal(true),
            dragging: signal(false),
            packages: signal([]),
            getSnapThreshold: vi.fn(),
            getSelection: vi.fn().mockReturnValue([]),
            setStatusBarText: vi.fn()
        };

        service = Object.create(DynamicGuidesService.prototype);
        (service as any).editorContentService = editorContent;
        (service as any).editorSession = editorSession;
    });

    // ------------------------------------------------------------------
    // §2.4 Direction matrix — getDraggedElementRect
    // ------------------------------------------------------------------
    describe('getDraggedElementRect direction matrix (§2.4)', () => {

        const INITIAL_RECT = new DOMRect(100, 50, 200, 100);
        const INITIAL_POINT = { x: 100, y: 50 };

        beforeEach(() => {
            const svc = service as any;
            svc.initialPoint = { ...INITIAL_POINT };
            svc.initialRectangle = INITIAL_RECT;
            svc.element = null;
            editorContent.getContentElementById.mockReturnValue(null);
        });

        const callRect = (point: { x: number, y: number }, direction: string): DOMRect =>
            (service as any).getDraggedElementRect(point, direction) as DOMRect;

        const cases: [string, number, number, number, number, number, number, string][] = [
            ['e',  40,  20, 100, 50,  240, 100, 'before midpoint'],
            ['e', 160,  80, 100, 50,  360, 100, 'past midpoint'],

            ['w',  40,  20, 140, 50,  160, 100, 'before midpoint'],
            ['w', 160,  80, 260, 50,   40, 100, 'past midpoint'],

            ['n',  40,  20, 100, 70,  200,  80, 'before midpoint'],
            ['n', 160,  80, 100, 130, 200,  20, 'past midpoint'],

            ['s',  40,  20, 100, 50,  200, 120, 'before midpoint'],
            ['s', 160,  80, 100, 50,  200, 180, 'past midpoint'],

            ['ne',  40,  20, 100, 70,  240,  80, 'before midpoint'],
            ['ne', 160,  80, 100, 130, 360,  20, 'past midpoint'],

            ['nw',  40,  20, 140, 70,  160,  80, 'before midpoint'],
            ['nw', 160,  80, 260, 130,  40,  20, 'past midpoint'],

            ['se',  40,  20, 100, 50,  240, 120, 'before midpoint'],
            ['se', 160,  80, 100, 50,  360, 180, 'past midpoint'],

            ['sw',  40,  20, 140, 50,  160, 120, 'before midpoint'],
            ['sw', 160,  80, 260, 50,   40, 180, 'past midpoint']
        ];

        cases.forEach(([dir, dx, dy, l, t, w, h, label]) => {
            it(`${dir}-resize delta (${dx},${dy}) ${label} -> left=${l} top=${t} width=${w} height=${h}`, () => {
                const point = { x: INITIAL_POINT.x + dx, y: INITIAL_POINT.y + dy };
                const rect = callRect(point, dir);
                expect(rect.left).toBe(l);
                expect(rect.top).toBe(t);
                expect(rect.width).toBe(w);
                expect(rect.height).toBe(h);
            });
        });

        it('non-resize drag moves left/top by the cursor delta and keeps width/height', () => {
            const point = { x: INITIAL_POINT.x + 70, y: INITIAL_POINT.y + 30 };
            const rect = callRect(point, null as any);
            expect(rect.left).toBe(170);
            expect(rect.top).toBe(80);
            expect(rect.width).toBe(200);
            expect(rect.height).toBe(100);
        });
    });

    // ------------------------------------------------------------------
    // §2.5 Snap target on the opposite edge
    // ------------------------------------------------------------------
    describe('handleHorizontalSnap snap-anchor behaviour (§2.5)', () => {

        const RECT = new DOMRect(100, 50, 200, 100);
        const FORM = new DOMRect(0, 0, 1000, 1000);

        beforeEach(() => {
            const svc = service as any;
            svc.snapThreshold = 10;
            svc.snapToEndEnabled = true;
            svc.formBounds = FORM;
            svc.leftPos = new Map<string, number>();
            svc.rightPos = new Map<string, number>();
            svc.topPos = new Map<string, number>();
            svc.bottomPos = new Map<string, number>();
            svc.parents = new Map<string, string>();
            svc.types = new Map<string, string>();
            svc.rectangles = [];
            svc.uuids = [];
        });

        it('e-resize with right-edge snap target: writes width and cssPosition.right; does NOT write properties.left or cssPosition.left', () => {
            const svc = service as any;
            svc.rightPos.set('neighbor', 305);
            svc.leftPos.set('neighbor', 250);
            svc.topPos.set('neighbor', 50);
            svc.bottomPos.set('neighbor', 150);

            const props = new SnapData({} as MouseEvent, RECT.top, RECT.left, {} as any, []);
            const point = { x: 305, y: 100 };

            const guide = svc.handleHorizontalSnap('e', true, point, 'self', RECT, props);

            expect(guide).toBeTruthy();
            expect(props.left).toBe(100);
            expect(props.width).toBe(205);
            expect(props.cssPosition['right']).toBeDefined();
            expect(props.cssPosition['left']).toBeUndefined();
        });

        it('w-resize with left-edge snap target: updates left and sets width = rect.right - left (right edge unchanged)', () => {
            const svc = service as any;
            svc.leftPos.set('neighbor', 95);
            svc.rightPos.set('neighbor', 195);
            svc.topPos.set('neighbor', 50);
            svc.bottomPos.set('neighbor', 150);

            const props = new SnapData({} as MouseEvent, RECT.top, RECT.left, {} as any, []);
            const point = { x: 95, y: 100 };

            const guide = svc.handleHorizontalSnap('w', true, point, 'self', RECT, props);

            expect(guide).toBeTruthy();
            expect(props.left).toBe(95);
            expect(props.width).toBe(205);
            expect(props.cssPosition['left']).toBeDefined();
            expect(props.cssPosition['right']).toBeUndefined();
        });

        it('e-resize with no neighbour close enough: returns no guide and leaves left/width untouched', () => {
            const svc = service as any;
            svc.rightPos.set('neighbor', 500);
            svc.leftPos.set('neighbor', 400);

            const props = new SnapData({} as MouseEvent, RECT.top, RECT.left, {} as any, []);
            const guide = svc.handleHorizontalSnap('e', true, { x: 305, y: 100 }, 'self', RECT, props);

            expect(guide).toBeNull();
            expect(props.left).toBe(100);
            expect(props.width).toBeUndefined();
            expect(props.cssPosition['right']).toBeUndefined();
            expect(props.cssPosition['left']).toBeUndefined();
        });
    });

    describe('handleVerticalSnap snap-anchor behaviour (§2.5)', () => {

        const RECT = new DOMRect(100, 50, 200, 100);
        const FORM = new DOMRect(0, 0, 1000, 1000);

        beforeEach(() => {
            const svc = service as any;
            svc.snapThreshold = 10;
            svc.snapToEndEnabled = true;
            svc.formBounds = FORM;
            svc.leftPos = new Map<string, number>();
            svc.rightPos = new Map<string, number>();
            svc.topPos = new Map<string, number>();
            svc.bottomPos = new Map<string, number>();
            svc.parents = new Map<string, string>();
            svc.types = new Map<string, string>();
            svc.rectangles = [];
            svc.uuids = [];
        });

        it('s-resize with bottom-edge snap target: writes height and cssPosition.bottom; does NOT write properties.top or cssPosition.top', () => {
            const svc = service as any;
            svc.bottomPos.set('neighbor', 155);
            svc.topPos.set('neighbor', 60);
            svc.leftPos.set('neighbor', 100);
            svc.rightPos.set('neighbor', 200);

            const props = new SnapData({} as MouseEvent, RECT.top, RECT.left, {} as any, []);
            const point = { x: 200, y: 155 };

            const guide = svc.handleVerticalSnap('s', true, point, 'self', RECT, props);

            expect(guide).toBeTruthy();
            expect(props.top).toBe(50);
            expect(props.height).toBe(105);
            expect(props.cssPosition['bottom']).toBeDefined();
            expect(props.cssPosition['top']).toBeUndefined();
        });

        it('n-resize with top-edge snap target: updates top and sets height = rect.bottom - top (bottom edge unchanged)', () => {
            const svc = service as any;
            svc.topPos.set('neighbor', 45);
            svc.bottomPos.set('neighbor', 145);
            svc.leftPos.set('neighbor', 100);
            svc.rightPos.set('neighbor', 200);

            const props = new SnapData({} as MouseEvent, RECT.top, RECT.left, {} as any, []);
            const point = { x: 200, y: 45 };

            const guide = svc.handleVerticalSnap('n', true, point, 'self', RECT, props);

            expect(guide).toBeTruthy();
            expect(props.top).toBe(45);
            expect(props.height).toBe(105);
            expect(props.cssPosition['top']).toBeDefined();
            expect(props.cssPosition['bottom']).toBeUndefined();
        });

        it('s-resize with no neighbour close enough: returns no guide and leaves top/height untouched', () => {
            const svc = service as any;
            svc.bottomPos.set('neighbor', 500);
            svc.topPos.set('neighbor', 400);

            const props = new SnapData({} as MouseEvent, RECT.top, RECT.left, {} as any, []);
            const guide = svc.handleVerticalSnap('s', true, { x: 200, y: 155 }, 'self', RECT, props);

            expect(guide).toBeNull();
            expect(props.top).toBe(50);
            expect(props.height).toBeUndefined();
            expect(props.cssPosition['bottom']).toBeUndefined();
            expect(props.cssPosition['top']).toBeUndefined();
        });
    });
});
