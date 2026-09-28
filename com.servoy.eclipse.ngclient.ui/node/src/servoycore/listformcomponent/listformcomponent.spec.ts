import { ChangeDetectorRef, Renderer2 } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { ColDef, GridOptions } from 'ag-grid-community';
import { ListFormComponent } from './listformcomponent';
import { LoggerFactory } from '@servoy/public';

/**
 * SVY-21457 — per-row auto-height for the scrolling List Form Component.
 *
 * Final design (native AG Grid autoHeight was tried and reverted after live testing showed
 * it reintroduces the SVY-21244 flicker — see the spec, section 3.1): `columnDefs[].autoHeight`
 * stays exactly as it was before SVY-21457 (disabled only for the per-row auto-height path,
 * `isPerRowAutoHeight()` = `!isInAbsoluteLayout() && responsiveHeight() < 0`); each row is
 * measured explicitly once by `RowRenderer` and fed back to AG Grid via
 * `ListFormComponent.applyMeasuredRowHeight()` + a `getRowHeight` grid-options callback that
 * looks up `measuredRowHeights`. The SVY-21244 resize-observer guard was additionally
 * hardened (now driven by column-count only, not raw width) to fix a second flicker/loop
 * mechanism found during this fix's own testing. These tests exercise that logic directly:
 * they construct the component with mocked collaborators, stub the signal inputs / viewChild
 * getters it reads, and assert on the produced agGridOptions and on the resize-observer
 * behaviour.
 *
 * The component is NOT rendered through TestBed.createComponent on purpose: rendering would
 * instantiate ag-grid-angular and its real template (which needs the full DI graph) and only
 * adds flakiness. The observable surface the SVY-21457 change actually modifies is the
 * agGridOptions object and the ResizeObserver callback, both reachable without rendering.
 * The component is still constructed inside TestBed.runInInjectionContext so the input()/
 * viewChild() field initializers in its constructor have a valid injection context (avoids
 * NG0203); the signal getters are then overridden with plain functions for control.
 */
describe('ListFormComponent (SVY-21457 per-row auto-height)', () => {

    // Minimal foundset stub - svyOnInit() registers a change listener on it for the
    // scrolling path; we only need addChangeListener to exist and return a remover.
    const foundsetStub = () => ({
        addChangeListener: (_cb: any) => (() => { /* remove listener */ }),
        serverSize: 0,
        selectedRowIndexes: [] as number[],
        viewPort: { rows: [], startIndex: 0, size: 0 }
    });

    // Parent FormComponent whose getFormCache().getFormComponent() returns the LFC cache
    // that svyOnInit() reads into this.cache. Only the touched members are implemented.
    const parentStub = {
        getFormCache: () => ({
            getFormComponent: (_name: string) => ({ items: [] })
        })
    };

    const newComponent = (): ListFormComponent => {
        const renderer = jasmine.createSpyObj<Renderer2>('Renderer2', ['setStyle', 'setAttribute', 'removeAttribute']);
        const cdRef = jasmine.createSpyObj<ChangeDetectorRef>('ChangeDetectorRef', ['detectChanges', 'markForCheck', 'detach', 'reattach', 'checkNoChanges']);
        const servoyService = { getUIProperties: () => ({ getUIProperty: () => null }) } as any;
        const logFactory = { getLogger: () => jasmine.createSpyObj('Logger', ['error', 'warn', 'debug', 'info', 'spam']) } as unknown as LoggerFactory;
        const injector = { get: () => parentStub } as any;
        // input()/viewChild() run in the constructor, so build inside an injection context.
        return TestBed.runInInjectionContext(() => new ListFormComponent(
            renderer, {} as any, servoyService, {} as any, {} as any, cdRef, logFactory, injector, document
        ));
    };

    beforeEach(() => {
        // A configured TestBed is required for runInInjectionContext to have a context.
        TestBed.configureTestingModule({});
    });

    const buildOptions = (absoluteLayout: boolean, responsiveHeight: number, formHeight: number | null): GridOptions => {
        const component = newComponent();

        component.servoyApi = jasmine.createSpyObj('ServoyApi', [
            'isInDesigner', 'isInAbsoluteLayout', 'getClientProperty', 'getFormName',
            'registerComponent', 'unRegisterComponent', 'getMarkupId', 'trustAsHtml'
        ]);
        (component.servoyApi.isInDesigner as jasmine.Spy).and.returnValue(false);
        (component.servoyApi.isInAbsoluteLayout as jasmine.Spy).and.returnValue(absoluteLayout);
        // no ListFormComponent.pagingMode client property -> useScrolling stays true
        (component.servoyApi.getClientProperty as jasmine.Spy).and.returnValue(null);

        // Override the signal inputs read by svyOnInit()/getRowHeight() with plain getters.
        (component as any).responsiveHeight = () => responsiveHeight;
        (component as any).containedForm = () => ({ formHeight, formWidth: 100, absoluteLayout });
        (component as any).foundset = () => foundsetStub();

        component.svyOnInit();
        return component.agGridOptions;
    };

    it('creates the scrolling grid options via svyOnInit', () => {
        const options = buildOptions(false, 200, 50);
        expect(options).toBeTruthy();
        expect(options.rowModelType).toBe('serverSide');
    });

    describe('columnDefs autoHeight', () => {

        // Native autoHeight is disabled ONLY for the per-row auto-height path (responsive,
        // responsiveHeight < 0) - that path measures each row explicitly instead (RowRenderer +
        // applyMeasuredRowHeight). Every other path keeps its pre-SVY-21457 autoHeight: true.
        it('disables native autoHeight on the row column in responsive layout with responsiveHeight < 0 (per-row auto-height path)', () => {
            const options = buildOptions(false, -1, 50);
            expect((options.columnDefs[0] as ColDef).autoHeight).toBe(false);
        });

        it('keeps native autoHeight: true on the row column in responsive layout with responsiveHeight >= 0 (unchanged)', () => {
            const options = buildOptions(false, 300, 50);
            expect((options.columnDefs[0] as ColDef).autoHeight).toBe(true);
        });

        it('keeps native autoHeight: true on the row column in absolute layout, even with responsiveHeight < 0 (unchanged)', () => {
            const options = buildOptions(true, -1, 50);
            expect((options.columnDefs[0] as ColDef).autoHeight).toBe(true);
        });
    });

    describe('rowHeight (initial/fallback value)', () => {

        it('uses the fixed getRowHeight() value as the initial rowHeight for responsive layout with responsiveHeight < 0 (used only until a row is measured)', () => {
            const options = buildOptions(false, -1, 50);
            expect(options.rowHeight).toBe(50);
        });

        it('uses the fixed getRowHeight() value for responsive layout with responsiveHeight >= 0 (unchanged)', () => {
            const options = buildOptions(false, 300, 50);
            expect(options.rowHeight).toBe(50);
        });

        it('uses the fixed getRowHeight() value for absolute layout even when responsiveHeight < 0 (unchanged)', () => {
            const options = buildOptions(true, -1, 50);
            expect(options.rowHeight).toBe(50);
        });

        it('is null when the contained form has no formHeight and responsiveHeight >= 0 (getRowHeight() fallback, unchanged)', () => {
            const options = buildOptions(false, 300, null);
            expect(options.rowHeight).toBeNull();
        });
    });

    describe('getRowHeight callback (per-row auto-height lookup)', () => {

        it('is only set for the per-row auto-height path (responsive, responsiveHeight < 0)', () => {
            expect(buildOptions(false, -1, 50).getRowHeight).toBeInstanceOf(Function);
            expect(buildOptions(false, 300, 50).getRowHeight).toBeUndefined();
            expect(buildOptions(true, -1, 50).getRowHeight).toBeUndefined();
        });

        it('falls back to getRowHeight()/42 before a row has been measured', () => {
            const optionsWithFormHeight = buildOptions(false, -1, 50);
            expect(optionsWithFormHeight.getRowHeight({ node: { id: 'row-1' } } as any)).toBe(50);

            const optionsNoFormHeight = buildOptions(false, -1, null);
            expect(optionsNoFormHeight.getRowHeight({ node: { id: 'row-1' } } as any)).toBe(42);
        });

        it('returns the measured height once applyMeasuredRowHeight has recorded one for that row id', () => {
            const component = newComponent();
            component.servoyApi = jasmine.createSpyObj('ServoyApi', ['isInDesigner', 'isInAbsoluteLayout', 'getClientProperty']);
            (component.servoyApi.isInDesigner as jasmine.Spy).and.returnValue(false);
            (component.servoyApi.isInAbsoluteLayout as jasmine.Spy).and.returnValue(false);
            (component.servoyApi.getClientProperty as jasmine.Spy).and.returnValue(null);
            (component as any).responsiveHeight = () => -1;
            (component as any).containedForm = () => ({ formHeight: 50, formWidth: 100, absoluteLayout: false });
            (component as any).foundset = () => foundsetStub();
            component.svyOnInit();
            const agGrid = { api: jasmine.createSpyObj('gridApi', ['isDestroyed', 'onRowHeightChanged']) } as any;
            (agGrid.api.isDestroyed as jasmine.Spy).and.returnValue(false);
            (component as any).agGrid = () => agGrid;

            const node = jasmine.createSpyObj('node', ['setRowHeight']);
            component.applyMeasuredRowHeight('row-7', node, 123);

            expect(component.agGridOptions.getRowHeight({ node: { id: 'row-7' } } as any)).toBe(123);
            expect(component.agGridOptions.getRowHeight({ node: { id: 'row-other' } } as any)).toBe(50);
        });
    });

    describe('applyMeasuredRowHeight', () => {

        const buildForApply = (absoluteLayout: boolean, responsiveHeight: number) => {
            const component = newComponent();
            component.servoyApi = jasmine.createSpyObj('ServoyApi', ['isInDesigner', 'isInAbsoluteLayout', 'getClientProperty']);
            (component.servoyApi.isInDesigner as jasmine.Spy).and.returnValue(false);
            (component.servoyApi.isInAbsoluteLayout as jasmine.Spy).and.returnValue(absoluteLayout);
            (component.servoyApi.getClientProperty as jasmine.Spy).and.returnValue(null);
            (component as any).responsiveHeight = () => responsiveHeight;
            (component as any).containedForm = () => ({ formHeight: 50, formWidth: 100, absoluteLayout });
            (component as any).foundset = () => foundsetStub();
            component.svyOnInit();
            const gridApi = jasmine.createSpyObj('gridApi', ['isDestroyed', 'onRowHeightChanged']);
            (gridApi.isDestroyed as jasmine.Spy).and.returnValue(false);
            const agGrid = { api: gridApi } as any;
            (component as any).agGrid = () => agGrid;
            const node = jasmine.createSpyObj('node', ['setRowHeight']);
            return { component, gridApi, node };
        };

        it('sets the row height and notifies AG Grid on the per-row auto-height path', () => {
            const { component, gridApi, node } = buildForApply(false, -1);
            component.applyMeasuredRowHeight('row-1', node, 200);
            expect(node.setRowHeight).toHaveBeenCalledWith(200);
            expect(gridApi.onRowHeightChanged).toHaveBeenCalled();
        });

        it('does nothing outside the per-row auto-height path (responsiveHeight >= 0)', () => {
            const { component, gridApi, node } = buildForApply(false, 300);
            component.applyMeasuredRowHeight('row-1', node, 200);
            expect(node.setRowHeight).not.toHaveBeenCalled();
            expect(gridApi.onRowHeightChanged).not.toHaveBeenCalled();
        });

        it('does nothing outside the per-row auto-height path (absolute layout)', () => {
            const { component, gridApi, node } = buildForApply(true, -1);
            component.applyMeasuredRowHeight('row-1', node, 200);
            expect(node.setRowHeight).not.toHaveBeenCalled();
            expect(gridApi.onRowHeightChanged).not.toHaveBeenCalled();
        });

        it('ignores a non-positive measured height', () => {
            const { component, node } = buildForApply(false, -1);
            component.applyMeasuredRowHeight('row-1', node, 0);
            expect(node.setRowHeight).not.toHaveBeenCalled();
        });

        it('is a no-op (does not call setRowHeight/onRowHeightChanged again) when the same height is re-applied to the same row', () => {
            const { component, gridApi, node } = buildForApply(false, -1);
            component.applyMeasuredRowHeight('row-1', node, 200);
            gridApi.onRowHeightChanged.calls.reset();
            (node.setRowHeight as jasmine.Spy).calls.reset();

            component.applyMeasuredRowHeight('row-1', node, 200);

            expect(node.setRowHeight).not.toHaveBeenCalled();
            expect(gridApi.onRowHeightChanged).not.toHaveBeenCalled();
        });

        it('re-applies when a different height is measured for the same row (e.g. after a filter)', () => {
            const { component, gridApi, node } = buildForApply(false, -1);
            component.applyMeasuredRowHeight('row-1', node, 200);
            gridApi.onRowHeightChanged.calls.reset();
            (node.setRowHeight as jasmine.Spy).calls.reset();

            component.applyMeasuredRowHeight('row-1', node, 350);

            expect(node.setRowHeight).toHaveBeenCalledWith(350);
            expect(gridApi.onRowHeightChanged).toHaveBeenCalled();
        });
    });

    describe('isPerRowAutoHeight', () => {

        it('is true only for responsive layout with responsiveHeight < 0', () => {
            const component = buildForApplyIsPerRowAutoHeight(false, -1);
            expect(component.isPerRowAutoHeight()).toBe(true);
        });

        it('is false for responsive layout with responsiveHeight >= 0', () => {
            const component = buildForApplyIsPerRowAutoHeight(false, 0);
            expect(component.isPerRowAutoHeight()).toBe(false);
        });

        it('is false for absolute layout even with responsiveHeight < 0', () => {
            const component = buildForApplyIsPerRowAutoHeight(true, -1);
            expect(component.isPerRowAutoHeight()).toBe(false);
        });

        function buildForApplyIsPerRowAutoHeight(absoluteLayout: boolean, responsiveHeight: number): ListFormComponent {
            const component = newComponent();
            component.servoyApi = jasmine.createSpyObj('ServoyApi', ['isInAbsoluteLayout']);
            (component.servoyApi.isInAbsoluteLayout as jasmine.Spy).and.returnValue(absoluteLayout);
            (component as any).responsiveHeight = () => responsiveHeight;
            return component;
        }
    });

    describe('domLayout', () => {

        it("is 'autoHeight' when responsiveHeight < 0 so the grid grows to fit all rows", () => {
            const options = buildOptions(false, -1, 50);
            expect(options.domLayout).toBe('autoHeight');
        });

        it("is 'normal' when responsiveHeight >= 0", () => {
            const options = buildOptions(false, 300, 50);
            expect(options.domLayout).toBe('normal');
        });

        it("is 'autoHeight' when responsiveHeight < 0 even in absolute layout", () => {
            const options = buildOptions(true, -1, 50);
            expect(options.domLayout).toBe('autoHeight');
        });
    });

    describe('regression - removed single-first-row measurement machinery (SVY-21244 row-height half)', () => {

        it('no longer exposes the old single-first-row onRowRendererAfterViewInit hook (replaced by applyMeasuredRowHeight, called per-row from RowRenderer)', () => {
            const component = newComponent();
            expect((component as any).onRowRendererAfterViewInit).toBeUndefined();
            expect((component as any).applyMeasuredRowHeight).toBeInstanceOf(Function);
        });

        it('no longer exposes the rowHeightMeasured latch field (replaced by a per-row measuredRowHeights map)', () => {
            const component = newComponent();
            expect((component as any).rowHeightMeasured).toBeUndefined();
        });
    });

    describe('SVY-21244 anti-flicker - grid hidden until first render settles', () => {

        // For the responsive auto-height path (responsiveHeight < 0) AG Grid measures each row
        // and re-lays them out after the first paint. To avoid that double render being visible
        // as a flicker, getAGGridStyle() hides the grid until onFirstDataRendered reveals it.
        const configureFlicker = (absoluteLayout: boolean, responsiveHeight: number): ListFormComponent => {
            const component = newComponent();
            component.servoyApi = jasmine.createSpyObj('ServoyApi', [
                'isInDesigner', 'isInAbsoluteLayout', 'getClientProperty', 'getFormName',
                'registerComponent', 'unRegisterComponent', 'getMarkupId', 'trustAsHtml'
            ]);
            (component.servoyApi.isInDesigner as jasmine.Spy).and.returnValue(false);
            (component.servoyApi.isInAbsoluteLayout as jasmine.Spy).and.returnValue(absoluteLayout);
            (component.servoyApi.getClientProperty as jasmine.Spy).and.returnValue(null);
            (component as any).responsiveHeight = () => responsiveHeight;
            (component as any).containedForm = () => ({ formHeight: 50, formWidth: 100, absoluteLayout });
            (component as any).foundset = () => foundsetStub();
            component.svyOnInit();
            return component;
        };

        it('hides the grid (visibility: hidden) before the first render on the responsive auto-height path', () => {
            const component = configureFlicker(false, -1);
            expect(component.getAGGridStyle().visibility).toBe('hidden');
        });

        it('reveals the grid after onFirstDataRendered fires (waits several animation frames for per-row measurements to land first)', () => {
            const component = configureFlicker(false, -1);
            expect(component.getAGGridStyle().visibility).toBe('hidden');

            // onFirstDataRendered chains multiple requestAnimationFrame calls (beyond RowRenderer's
            // own double-rAF measurement) before revealing; run the fake rAF synchronously so all of
            // them flush within this test.
            const raf = spyOn(window, 'requestAnimationFrame').and.callFake((cb: FrameRequestCallback) => { cb(0); return 0; });
            component.agGridOptions.onFirstDataRendered({ api: { getDisplayedRowCount: () => 0 } } as any);
            raf.and.callThrough();

            expect(component.getAGGridStyle().visibility).toBeUndefined();
        });

        it('does NOT hide the grid on the fixed-height responsive path (responsiveHeight >= 0)', () => {
            const component = configureFlicker(false, 300);
            expect(component.getAGGridStyle().visibility).toBeUndefined();
        });

        it('does NOT hide the grid in absolute layout', () => {
            const component = configureFlicker(true, -1);
            expect(component.getAGGridStyle().visibility).toBeUndefined();
        });

        it('reveals the grid via onModelUpdated when the foundset is empty and onFirstDataRendered never fires', () => {
            const component = configureFlicker(false, -1);
            expect(component.getAGGridStyle().visibility).toBe('hidden');

            const raf = spyOn(window, 'requestAnimationFrame').and.callFake((cb: FrameRequestCallback) => { cb(0); return 0; });
            component.agGridOptions.onModelUpdated({ api: { getDisplayedRowCount: () => 0 } } as any);
            raf.and.callThrough();

            expect(component.getAGGridStyle().visibility).toBeUndefined();
        });
    });

    describe('regression - SVY-21244 resize-observer guard (AC6)', () => {

        // Wire a component for the scrolling responsive path and run ngAfterViewInit so the
        // ResizeObserver is created. We replace the global ResizeObserver with a stub that
        // captures the callback, then fire resize notifications ourselves and assert the grid
        // is only refreshed when the computed column count actually changes. This directly
        // exercises the `newNumberOfColumns !== this.numberOfColumns` guard that must survive
        // the SVY-21457 change; removing it would make the "no change" case refresh the grid
        // and fail the first assertion.
        const buildForResize = (parentWidth: number, formWidth: number) => {
            const component = newComponent();

            component.servoyApi = jasmine.createSpyObj('ServoyApi', ['isInAbsoluteLayout']);
            (component.servoyApi.isInAbsoluteLayout as jasmine.Spy).and.returnValue(false); // responsive -> observer is installed
            component.useScrolling = true;

            const gridApi = jasmine.createSpyObj('gridApi', ['setGridOption', 'refreshServerSide', 'setRowCount', 'isDestroyed', 'getDisplayedRowCount', 'ensureIndexVisible']);
            (gridApi.isDestroyed as jasmine.Spy).and.returnValue(false);
            const agGridStub = { api: gridApi } as any;

            const elementRef = { nativeElement: { offsetWidth: parentWidth } } as any;

            // stub the signal inputs / viewChild getters read along the ngAfterViewInit path
            (component as any).element = () => elementRef;
            (component as any).agGrid = () => agGridStub;
            (component as any).containedForm = () => ({ formWidth, formHeight: 50, absoluteLayout: false });
            (component as any).containedFormMargin = () => undefined;
            (component as any).pageLayout = () => 'tableview'; // not listview, so column count depends on width
            (component as any)._foundset = () => ({ serverSize: 0, selectedRowIndexes: [] as number[] });
            // avoid running the ServoyBaseComponent super.ngAfterViewInit() and calculateCells side effects
            spyOn(Object.getPrototypeOf(Object.getPrototypeOf(component)), 'ngAfterViewInit').and.stub();
            spyOn(component, 'calculateCells').and.stub();

            return { component, gridApi, elementRef };
        };

        let originalResizeObserver: any;
        let capturedCallback: (entries: any[]) => void;

        beforeEach(() => {
            jasmine.clock().install();
            originalResizeObserver = (window as any).ResizeObserver;
            (window as any).ResizeObserver = class {
                constructor(cb: (entries: any[]) => void) { capturedCallback = cb; }
                observe() { /* nop */ }
                unobserve() { /* nop */ }
                disconnect() { /* nop */ }
            };
        });

        afterEach(() => {
            (window as any).ResizeObserver = originalResizeObserver;
            jasmine.clock().uninstall();
        });

        it('does NOT refresh the grid when a resize keeps the same column count', () => {
            // parentWidth 300, formWidth 100 -> 3 columns
            const { component, gridApi, elementRef } = buildForResize(300, 100);
            component.ngAfterViewInit();
            component.numberOfColumns = 3; // current state matches what a 300px width computes

            // new width 320 still yields floor(320/100) = 3 columns -> guard should suppress refresh
            elementRef.nativeElement.offsetWidth = 320;
            capturedCallback([{ contentRect: { width: 320 } }]);
            jasmine.clock().tick(500);

            expect(gridApi.refreshServerSide).not.toHaveBeenCalled();
        });

        it('DOES refresh the grid when a resize changes the column count', () => {
            const { component, gridApi, elementRef } = buildForResize(300, 100);
            component.ngAfterViewInit();
            component.numberOfColumns = 3;

            // new width 550 yields floor(550/100) = 5 columns -> guard passes, refresh fires
            elementRef.nativeElement.offsetWidth = 550;
            capturedCallback([{ contentRect: { width: 550 } }]);
            jasmine.clock().tick(500);

            expect(gridApi.refreshServerSide).toHaveBeenCalledWith({ purge: true });
        });
    });
});
