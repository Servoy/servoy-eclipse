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

    describe('showDynamicGuidesChanged', () => {
        it('should set guidesEnabled true', () => {
            service.showDynamicGuidesChanged(true);
            expect((service as any).guidesEnabled).toBe(true);
        });

        it('should set guidesEnabled false', () => {
            service.showDynamicGuidesChanged(false);
            expect((service as any).guidesEnabled).toBe(false);
        });
    });

    const makeEl = (attrs: Record<string, string | null>, classes: string[] = [], parent: any = null): any => {
        const classSet = new Set(classes);
        return {
            getAttribute: (n: string) => (n in attrs ? attrs[n] : null),
            classList: { contains: (c: string) => classSet.has(c) },
            parentElement: parent,
            getBoundingClientRect: () => new DOMRect(0, 0, 10, 10),
            childNodes: [],
        };
    };

    describe('isParentContainer', () => {
        it('should return false for null', () => {
            expect((service as any).isParentContainer(null)).toBe(false);
        });

        it('should return true for svy-formcomponent', () => {
            expect((service as any).isParentContainer(makeEl({}, ['svy-formcomponent']))).toBe(true);
        });

        it('should return true for svy-listformcomponent', () => {
            expect((service as any).isParentContainer(makeEl({}, ['svy-listformcomponent']))).toBe(true);
        });

        it('should return true for svy-container', () => {
            expect((service as any).isParentContainer(makeEl({}, ['svy-container']))).toBe(true);
        });

        it('should return false for a plain element', () => {
            expect((service as any).isParentContainer(makeEl({}, ['other']))).toBe(false);
        });
    });

    describe('getParent', () => {
        it('should return the ancestor with svy-form class', () => {
            const form = makeEl({}, ['svy-form']);
            const child = makeEl({}, [], form);
            expect((service as any).getParent(child)).toBe(form);
        });

        it('should return the ancestor that is a parent container', () => {
            const container = makeEl({}, ['svy-container']);
            const child = makeEl({}, [], container);
            expect((service as any).getParent(child)).toBe(container);
        });

        it('should return null when no matching ancestor', () => {
            const top = makeEl({}, ['plain']);
            const child = makeEl({}, [], top);
            expect((service as any).getParent(child)).toBeNull();
        });
    });

    describe('onMouseUp', () => {
        it('should clear state and reset status bar text', () => {
            const svc = service as any;
            svc.leftPos = new Map([['a', 1]]);
            svc.rightPos = new Map();
            svc.topPos = new Map();
            svc.bottomPos = new Map();
            svc.middleV = new Map();
            svc.middleH = new Map();
            svc.snapData = signal<SnapData | null>(new SnapData({} as MouseEvent, 0, 0, {}, []));
            service.onMouseUp();
            expect(svc.leftPos.size).toBe(0);
            expect(svc.snapData()).toBeNull();
            expect(editorSession.setStatusBarText).toHaveBeenCalledWith('');
        });
    });

    describe('clear', () => {
        it('should reset all maps, arrays and snapData', () => {
            const svc = service as any;
            svc.leftPos = new Map([['a', 1]]);
            svc.rightPos = new Map([['a', 1]]);
            svc.topPos = new Map([['a', 1]]);
            svc.bottomPos = new Map([['a', 1]]);
            svc.middleV = new Map([['a', 1]]);
            svc.middleH = new Map([['a', 1]]);
            svc.rectangles = [new DOMRect()];
            svc.uuids = ['a'];
            svc.initialPoint = { x: 1, y: 1 };
            svc.initialRectangle = new DOMRect();
            svc.properties = new SnapData({} as MouseEvent, 0, 0, {}, []);
            svc.snapData = signal<SnapData | null>(svc.properties);
            service.clear();
            expect(svc.leftPos.size).toBe(0);
            expect(svc.rectangles).toEqual([]);
            expect(svc.uuids).toEqual([]);
            expect(svc.initialPoint).toBeNull();
            expect(svc.initialRectangle).toBeNull();
            expect(svc.properties).toBeNull();
            expect(svc.snapData()).toBeNull();
        });
    });

    describe('isSnapInterval', () => {
        beforeEach(() => {
            const svc = service as any;
            svc.snapThreshold = 10;
            svc.parents = new Map<string, string>();
        });

        it('should return the matching key within threshold', () => {
            const svc = service as any;
            const posMap = new Map([['other', 100]]);
            const result = svc.isSnapInterval('self', 105, posMap);
            expect(result.uuid).toBe('other');
        });

        it('should skip the same uuid', () => {
            const svc = service as any;
            const posMap = new Map([['self', 100]]);
            expect(svc.isSnapInterval('self', 105, posMap)).toBeNull();
        });

        it('should return null when outside threshold', () => {
            const svc = service as any;
            const posMap = new Map([['other', 100]]);
            expect(svc.isSnapInterval('self', 200, posMap)).toBeNull();
        });

        it('should skip keys in a different container than the dragged component', () => {
            const svc = service as any;
            svc.parents.set('self', 'containerA');
            svc.parents.set('other', 'containerB');
            const posMap = new Map([['other', 100]]);
            expect(svc.isSnapInterval('self', 105, posMap)).toBeNull();
        });
    });

    describe('isOverlap', () => {
        it('should detect x-axis overlap', () => {
            const a = new DOMRect(0, 0, 100, 50);
            const b = new DOMRect(50, 0, 100, 50);
            expect((service as any).isOverlap(a, b, 'x')).toBe(true);
        });

        it('should detect no x-axis overlap', () => {
            const a = new DOMRect(0, 0, 40, 50);
            const b = new DOMRect(200, 0, 40, 50);
            expect((service as any).isOverlap(a, b, 'x')).toBe(false);
        });

        it('should detect y-axis overlap', () => {
            const a = new DOMRect(0, 0, 50, 100);
            const b = new DOMRect(0, 50, 50, 100);
            expect((service as any).isOverlap(a, b, 'y')).toBe(true);
        });

        it('should return false for an unknown axis', () => {
            const a = new DOMRect(0, 0, 50, 50);
            const b = new DOMRect(0, 0, 50, 50);
            expect((service as any).isOverlap(a, b, 'z' as any)).toBe(false);
        });
    });

    describe('getOverlappingRectangles', () => {
        it('should produce pairs ordered by the given property', () => {
            const r1 = new DOMRect(10, 0, 50, 50);
            const r2 = new DOMRect(5, 0, 50, 50);
            const pairs = (service as any).getOverlappingRectangles([r1, r2], 'left');
            expect(pairs).toHaveLength(1);
            // r2 has smaller left, so it comes first
            expect(pairs[0][0]).toBe(r2);
            expect(pairs[0][1]).toBe(r1);
        });

        it('should return an empty list for fewer than 2 rectangles', () => {
            expect((service as any).getOverlappingRectangles([new DOMRect()], 'top')).toEqual([]);
        });
    });

    describe('pointCloserToTopOrLeftSide', () => {
        const rect = new DOMRect(100, 50, 200, 100);

        it('x axis: closer to left returns true', () => {
            expect((service as any).pointCloserToTopOrLeftSide({ x: 110, y: 0 }, rect, 'x')).toBe(true);
        });

        it('x axis: closer to right returns false', () => {
            expect((service as any).pointCloserToTopOrLeftSide({ x: 290, y: 0 }, rect, 'x')).toBe(false);
        });

        it('y axis: closer to top returns true', () => {
            expect((service as any).pointCloserToTopOrLeftSide({ x: 0, y: 60 }, rect, 'y')).toBe(true);
        });

        it('y axis: closer to bottom returns false', () => {
            expect((service as any).pointCloserToTopOrLeftSide({ x: 0, y: 140 }, rect, 'y')).toBe(false);
        });
    });

    describe('isOutOfBounds', () => {
        beforeEach(() => {
            (service as any).formBounds = new DOMRect(0, 0, 500, 500);
        });

        it('should return false when inside the form bounds (x)', () => {
            const props = { left: 100, width: 50 } as any;
            expect((service as any).isOutOfBounds({ ...props }, props, new DOMRect(0, 0, 50, 50), 'x')).toBe(false);
        });

        it('should restore previous props and return true when out of bounds (x)', () => {
            const previous = { left: 10, cssPosition: {} } as any;
            const props = { left: -20, width: 50, cssPosition: {} } as any;
            const result = (service as any).isOutOfBounds(previous, props, new DOMRect(0, 0, 50, 50), 'x');
            expect(result).toBe(true);
            expect(props.left).toBe(10);
        });

        it('should return true when out of bounds (y)', () => {
            const previous = { top: 10 } as any;
            const props = { top: -20, height: 50 } as any;
            expect((service as any).isOutOfBounds(previous, props, new DOMRect(0, 0, 50, 50), 'y')).toBe(true);
        });
    });

    describe('setDimension', () => {
        it('should set the property when there is no initialRectangle', () => {
            const svc = service as any;
            svc.initialRectangle = null;
            const props = {} as any;
            svc.setDimension(120, 'width', props);
            expect(props.width).toBe(120);
        });

        it('should set the property when the new dimension is bigger than the initial', () => {
            const svc = service as any;
            svc.initialRectangle = new DOMRect(0, 0, 100, 100);
            const props = {} as any;
            svc.setDimension(120, 'width', props);
            expect(props.width).toBe(120);
        });

        it('should not set the property when the new dimension is smaller than the initial', () => {
            const svc = service as any;
            svc.initialRectangle = new DOMRect(0, 0, 100, 100);
            const props = {} as any;
            svc.setDimension(80, 'width', props);
            expect(props.width).toBeUndefined();
        });
    });

    describe('getDOMRect', () => {
        it('should build a DOMRect from the position maps', () => {
            const svc = service as any;
            svc.leftPos = new Map([['a', 10]]);
            svc.topPos = new Map([['a', 20]]);
            svc.rightPos = new Map([['a', 110]]);
            svc.bottomPos = new Map([['a', 120]]);
            const rect = svc.getDOMRect('a');
            expect(rect.left).toBe(10);
            expect(rect.top).toBe(20);
            expect(rect.width).toBe(100);
            expect(rect.height).toBe(100);
        });
    });

    describe('getGuideLength', () => {
        it('should add a 15px margin to the distance', () => {
            expect((service as any).getGuideLength(100, 40)).toBe(75);
        });
    });

    describe('findComponentPackage', () => {
        it('should find the package containing the given component type', () => {
            editorSession.packages.set([
                { packageName: 'commons', components: [{ name: 'x' }] },
                { packageName: 'mypkg', components: [{ name: 'mybean' }] },
            ]);
            const result = (service as any).findComponentPackage('mybean');
            expect(result.packageName).toBe('mypkg');
        });

        it('should ignore the commons package', () => {
            editorSession.packages.set([
                { packageName: 'commons', components: [{ name: 'mybean' }] },
            ]);
            expect((service as any).findComponentPackage('mybean')).toBeUndefined();
        });
    });

    describe('getDraggedComponentType', () => {
        it('should read svy-formelement-type from the selected element', () => {
            (service as any).element = makeEl({ 'svy-id': 'id1', 'svy-formelement-type': 'bootstrap-button' });
            expect((service as any).getDraggedComponentType()).toBe('bootstrap-button');
        });

        it('should fall back to the dragged element type attribute', () => {
            (service as any).element = null;
            editorContent.getContentElementById.mockReturnValue(
                makeEl({ 'svy-formelement-type': 'dragged-type' }));
            expect((service as any).getDraggedComponentType()).toBe('dragged-type');
        });
    });

    describe('getDraggedElementCategorySet', () => {
        it('should return component names from the matching package', () => {
            editorSession.packages.set([
                { packageName: 'mypkg', components: [{ name: 'a' }, { name: 'b' }] },
            ]);
            expect((service as any).getDraggedElementCategorySet('a')).toEqual(['a', 'b']);
        });

        it('should return null when componentType is falsy', () => {
            expect((service as any).getDraggedElementCategorySet('')).toBeNull();
        });

        it('should return an empty array when no package matches', () => {
            editorSession.packages.set([]);
            expect((service as any).getDraggedElementCategorySet('unknown')).toEqual([]);
        });
    });

    describe('shouldSnapToSize', () => {
        beforeEach(() => {
            const svc = service as any;
            svc.snapToEndEnabled = true;
            svc.rectangles = [new DOMRect(0, 0, 100, 40)];
            svc.uuids = ['target'];
            svc.types = new Map([['target', 'mytype']]);
            svc.initialRectangle = null;
            svc.element = makeEl({ 'svy-id': 'self', 'svy-formelement-type': 'mytype' });
            editorSession.packages.set([]);
        });

        it('should return false when uuid is empty', () => {
            expect(service.shouldSnapToSize('')).toBe(false);
        });

        it('should return false when snapToEnd is disabled', () => {
            (service as any).snapToEndEnabled = false;
            expect(service.shouldSnapToSize('target')).toBe(false);
        });

        it('should return true immediately when resizing', () => {
            expect(service.shouldSnapToSize('target', 'e')).toBe(true);
        });

        it('should snap the width when same type and value above threshold', () => {
            expect(service.shouldSnapToSize('target', undefined, 90, 'width')).toBe(true);
            expect(editorSession.setStatusBarText).toHaveBeenCalled();
        });

        it('should not snap width when the value exceeds the target width', () => {
            expect(service.shouldSnapToSize('target', undefined, 150, 'width')).toBe(false);
        });
    });

    describe('onMouseMove guard clauses', () => {
        beforeEach(() => {
            const svc = service as any;
            svc.guidesEnabled = true;
            svc.snapData = signal<SnapData | null>(null);
            svc.properties = null;
            svc.leftPos = new Map();
        });

        it('should bail out when multiple elements are selected', () => {
            editorSession.getSelection.mockReturnValue(['a', 'b']);
            (service as any).onMouseMove({ altKey: false, shiftKey: false } as MouseEvent);
            expect(editorSession.setStatusBarText).not.toHaveBeenCalled();
        });

        it('should bail out when not dragging and not resizing', () => {
            editorSession.getSelection.mockReturnValue([]);
            editorSession.resizing.set(false);
            editorSession.dragging.set(false);
            (service as any).onMouseMove({ altKey: false, shiftKey: false } as MouseEvent);
            expect(editorSession.setStatusBarText).not.toHaveBeenCalled();
        });

        it('should set status text and clear snapData when guides are disabled via ALT', () => {
            editorSession.getSelection.mockReturnValue([]);
            editorSession.resizing.set(true);
            const svc = service as any;
            svc.guidesEnabled = true;
            svc.properties = new SnapData({} as MouseEvent, 0, 0, {}, []);
            svc.onMouseMove({ altKey: true, shiftKey: false } as MouseEvent);
            expect(editorSession.setStatusBarText).toHaveBeenCalled();
            expect(svc.snapData()).toBeNull();
        });

        it('should set snapToEndEnabled based on the shift key', () => {
            editorSession.getSelection.mockReturnValue([]);
            editorSession.resizing.set(true);
            const svc = service as any;
            svc.guidesEnabled = false;
            svc.onMouseMove({ altKey: false, shiftKey: true } as MouseEvent);
            expect(svc.snapToEndEnabled).toBe(false);
        });
    });

    describe('addEqualWidth/HeightGuides', () => {
        beforeEach(() => {
            const svc = service as any;
            svc.equalSizeThreshold = 5;
            svc.uuids = ['other'];
            svc.rectangles = [new DOMRect(300, 50, 100, 40)];
            svc.formBounds = new DOMRect(0, 0, 1000, 1000);
        });

        it('addEqualWidthGuides should snap the width to a same-width neighbour and push guides', () => {
            const svc = service as any;
            // point closer to the right side so properties.left stays at rect.left (within bounds)
            const rect = new DOMRect(100, 60, 98, 40); // width 98 close to 100
            const props = new SnapData({} as MouseEvent, rect.top, rect.left, {}, []);
            const result = svc.addEqualWidthGuides({ x: 190, y: 70 }, rect, props, 'self');
            expect(result).not.toBeNull();
            expect(props.width).toBe(100);
            expect(props.cssPosition['sameWidth']).toBeDefined();
            expect(props.guides!.length).toBeGreaterThan(0);
        });

        it('addEqualWidthGuides should return null when no neighbour matches', () => {
            const svc = service as any;
            svc.rectangles = [new DOMRect(300, 50, 500, 40)];
            const rect = new DOMRect(100, 60, 98, 40);
            const props = new SnapData({} as MouseEvent, rect.top, rect.left, {}, []);
            expect(svc.addEqualWidthGuides({ x: 190, y: 70 }, rect, props, 'self')).toBeNull();
        });

        it('addEqualHeightGuides should snap the height to a same-height neighbour', () => {
            const svc = service as any;
            svc.rectangles = [new DOMRect(300, 50, 100, 42)];
            const rect = new DOMRect(100, 60, 100, 40); // height 40 close to 42
            const props = new SnapData({} as MouseEvent, rect.top, rect.left, {}, []);
            // point closer to the bottom so properties.top stays at rect.top (within bounds)
            const result = svc.addEqualHeightGuides({ x: 150, y: 95 }, rect, props, 'self');
            expect(result).not.toBeNull();
            expect(props.height).toBe(42);
            expect(props.cssPosition['sameHeight']).toBeDefined();
        });
    });

    describe('addEqualDistanceVerticalGuides / Horizontal', () => {
        beforeEach(() => {
            const svc = service as any;
            svc.equalDistanceThreshold = 10;
            svc.uuids = ['e1', 'e2'];
            svc.parents = new Map([['e1', ''], ['e2', '']]);
            svc.formBounds = new DOMRect(0, 0, 2000, 2000);
            svc.initialRectangle = null;
        });

        it('addEqualDistanceVerticalGuides should align the dragged element below two spaced siblings', () => {
            const svc = service as any;
            // e1 at top 0-50, e2 at 100-150 (distance 50). dragged rect at top 200 -> dist from e2 bottom is 50
            const e1 = new DOMRect(0, 0, 100, 50);
            const e2 = new DOMRect(0, 100, 100, 50);
            svc.rectangles = [e1, e2];
            const rect = new DOMRect(0, 200, 100, 50);
            const props = new SnapData({} as MouseEvent, rect.top, rect.left, {}, []);
            const result = svc.addEqualDistanceVerticalGuides(rect, props, [e1, e2], false);
            expect(result).not.toBeNull();
            expect(props.cssPosition['distY']).toBeDefined();
        });

        it('addEqualDistanceHorizontalGuides should align the dragged element to the right of two spaced siblings', () => {
            const svc = service as any;
            const e1 = new DOMRect(0, 0, 50, 100);
            const e2 = new DOMRect(100, 0, 50, 100);
            svc.rectangles = [e1, e2];
            const rect = new DOMRect(200, 0, 50, 100);
            const props = new SnapData({} as MouseEvent, rect.top, rect.left, {}, []);
            const result = svc.addEqualDistanceHorizontalGuides(rect, props, [e1, e2], false);
            expect(result).not.toBeNull();
            expect(props.cssPosition['distX']).toBeDefined();
        });
    });

    describe('getDraggedElementRect fallbacks', () => {
        it('should use the svy_draggedelement rect when the element has no svy-id', () => {
            const svc = service as any;
            svc.element = makeEl({});
            svc.initialRectangle = null;
            editorContent.getContentElementById.mockReturnValue({
                getBoundingClientRect: () => new DOMRect(0, 0, 80, 40)
            });
            const rect = svc.getDraggedElementRect({ x: 10, y: 20 }, '');
            expect(rect.left).toBe(10);
            expect(rect.top).toBe(20);
            expect(rect.width).toBe(80);
        });

        it('should return the element rect when there is no initial point', () => {
            const svc = service as any;
            svc.element = makeEl({ 'svy-id': 'self' });
            svc.element.getBoundingClientRect = () => new DOMRect(5, 6, 70, 30);
            svc.initialPoint = null;
            editorContent.getContentElementById.mockReturnValue(null);
            const rect = svc.getDraggedElementRect({ x: 10, y: 20 }, '');
            expect(rect.left).toBe(5);
            expect(rect.width).toBe(70);
        });
    });

    describe('init', () => {
        it('should collect component rectangles into the position maps', () => {
            const svc = service as any;
            svc.leftPos = new Map();
            svc.rightPos = new Map();
            svc.topPos = new Map();
            svc.bottomPos = new Map();
            svc.middleV = new Map();
            svc.middleH = new Map();
            svc.types = new Map();
            svc.parents = new Map();
            editorSession.getSelection.mockReturnValue([]);
            const comp = makeEl({ 'svy-id': 'c1', 'svy-formelement-type': 'bootstrap-button' });
            comp.getBoundingClientRect = () => new DOMRect(10, 20, 100, 40);
            const form = makeEl({}, ['svy-form']);
            form.getBoundingClientRect = () => new DOMRect(0, 0, 500, 500);
            editorContent.getContentElementsFromPoint.mockReturnValue([]);
            editorContent.getContentForm.mockReturnValue(form);
            editorContent.getAllContentElements.mockReturnValue([comp]);
            svc.init({ x: 50, y: 50 });
            expect(svc.leftPos.get('c1')).toBe(10);
            expect(svc.rightPos.get('c1')).toBe(110);
            expect(svc.types.get('c1')).toBe('bootstrap-button');
        });
    });

    describe('computeGuides', () => {
        beforeEach(() => {
            const svc = service as any;
            svc.snapThreshold = 10;
            svc.equalDistanceThreshold = 0;
            svc.equalSizeThreshold = 0;
            svc.snapToEndEnabled = true;
            svc.formBounds = new DOMRect(0, 0, 1000, 1000);
            svc.leftPos = new Map();
            svc.rightPos = new Map();
            svc.topPos = new Map();
            svc.bottomPos = new Map();
            svc.middleV = new Map();
            svc.middleH = new Map();
            svc.parents = new Map();
            svc.types = new Map();
            svc.rectangles = [];
            svc.uuids = [];
            svc.snapData = signal<SnapData | null>(null);
            svc.parentContainer = null;
            svc.element = makeEl({ 'svy-id': 'self' });
            editorSession.resizing.set(false);
            editorContent.getGlassPane.mockReturnValue({ style: { cursor: '' } });
            editorContent.getContentElementsFromPoint.mockReturnValue([]);
            editorContent.getContentElementById.mockReturnValue(null);
        });

        it('should clear snapData when inside a parent container', () => {
            const svc = service as any;
            svc.parentContainer = makeEl({}, ['svy-container']);
            svc.computeGuides({} as MouseEvent, { x: 10, y: 10 });
            expect(svc.snapData()).toBeNull();
        });

        it('should set snapData to null when there are no guides', () => {
            const svc = service as any;
            svc.element = makeEl({ 'svy-id': 'self' });
            svc.element.getBoundingClientRect = () => new DOMRect(500, 500, 50, 20);
            svc.computeGuides({} as MouseEvent, { x: 500, y: 500 });
            expect(svc.snapData()).toBeNull();
        });

        it('should produce a snap guide when an aligned neighbour is within threshold', () => {
            const svc = service as any;
            // neighbour left edge at 100; dragged element left edge also ~100
            svc.leftPos.set('neighbor', 100);
            svc.rightPos.set('neighbor', 200);
            svc.topPos.set('neighbor', 50);
            svc.bottomPos.set('neighbor', 150);
            const draggedEl = makeEl({ 'svy-id': 'self' });
            draggedEl.getBoundingClientRect = () => new DOMRect(103, 60, 50, 20);
            // computeGuides resolves the dragged element via getContentElementsFromPoint
            editorContent.getContentElementsFromPoint.mockReturnValue([draggedEl]);
            svc.computeGuides({} as MouseEvent, { x: 103, y: 60 });
            const data = svc.snapData();
            expect(data).not.toBeNull();
            expect(data.guides.length).toBeGreaterThan(0);
        });
    });
});
