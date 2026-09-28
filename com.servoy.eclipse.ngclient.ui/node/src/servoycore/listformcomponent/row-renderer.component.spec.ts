import { ElementRef } from '@angular/core';
import { RowRenderer } from './row-renderer.component';

/**
 * SVY-21457 — RowRenderer per-row auto-height measurement.
 *
 * `RowRenderer.ngAfterViewInit()` is the hook that actually measures and applies each row's
 * height for the per-row auto-height path (see ListFormComponent.applyMeasuredRowHeight and
 * the spec, section 3.2/3.3, for why this replaced AG Grid's native colDef.autoHeight).
 *
 * AG Grid's optional `afterGuiAttached` hook is never invoked for cell renderers (only for
 * filters, cell editors and date components), so this suite specifically covers the
 * `ngAfterViewInit` wiring and the recursive `measureContentHeight` logic that works around
 * the float-collapse issue in the row wrapper / nested bootstrap 12grid layout.
 */
describe('RowRenderer (SVY-21457 per-row auto-height measurement)', () => {

    const buildRenderer = (nativeElement: HTMLElement): { renderer: RowRenderer; lfc: any; node: any } => {
        const renderer = new RowRenderer({ nativeElement } as ElementRef);
        const lfc = {
            isPerRowAutoHeight: jasmine.createSpy('isPerRowAutoHeight').and.returnValue(true),
            applyMeasuredRowHeight: jasmine.createSpy('applyMeasuredRowHeight'),
            _foundset: () => ({ viewPort: { startIndex: 0 } }),
            numberOfColumns: 1
        };
        renderer.lfc = lfc as any;
        const node = { id: 'row-1', rowIndex: 0, detail: false };
        renderer.agInit({ context: { componentParent: lfc }, data: [], node } as any);
        return { renderer, lfc, node };
    };

    describe('ngAfterViewInit / measureAndApplyRowHeight', () => {

        it('does nothing when the LFC is not on the per-row auto-height path', () => {
            const nativeElement = document.createElement('div');
            const { renderer, lfc } = buildRenderer(nativeElement);
            (lfc.isPerRowAutoHeight as jasmine.Spy).and.returnValue(false);

            renderer.ngAfterViewInit();

            // guarded synchronously before any requestAnimationFrame is scheduled
            expect(lfc.applyMeasuredRowHeight).not.toHaveBeenCalled();
        });

        it('measures the row content and applies the height once the view has settled', (done) => {
            const nativeElement = document.createElement('div');
            const rowEl = document.createElement('div');
            const child = document.createElement('span');
            // jsdom/Karma DOM returns 0 for layout metrics by default; stub getBoundingClientRect
            // so the recursive measurement in measureContentHeight has real numbers to compare.
            spyOn(rowEl, 'getBoundingClientRect').and.returnValue({ top: 100, bottom: 100, left: 0, right: 0, width: 0, height: 0 } as DOMRect);
            spyOn(child, 'getBoundingClientRect').and.returnValue({ top: 100, bottom: 175, left: 0, right: 50, width: 50, height: 75 } as DOMRect);
            rowEl.appendChild(child);
            nativeElement.appendChild(rowEl);

            const { renderer, lfc, node } = buildRenderer(nativeElement);

            renderer.ngAfterViewInit();

            // measureAndApplyRowHeight chains two real requestAnimationFrame calls; wait for both
            // via nested rAF instead of faking the browser's scheduler, which is more robust
            // across environments than stubbing window.requestAnimationFrame.
            requestAnimationFrame(() => requestAnimationFrame(() => requestAnimationFrame(() => {
                expect(lfc.applyMeasuredRowHeight).toHaveBeenCalledWith('row-1', node, 75);
                done();
            })));
        });

        it('does not measure a detail/full-width row', (done) => {
            const nativeElement = document.createElement('div');
            const { renderer, lfc, node } = buildRenderer(nativeElement);
            node.detail = true;

            renderer.ngAfterViewInit();

            requestAnimationFrame(() => requestAnimationFrame(() => requestAnimationFrame(() => {
                expect(lfc.applyMeasuredRowHeight).not.toHaveBeenCalled();
                done();
            })));
        });
    });

    describe('measureContentHeight (float-collapse workaround)', () => {

        // The row wrapper and the nested form's bootstrap 12grid .row/.col-* structure are all
        // float: left with no clearfix, so a naive offsetHeight/scrollHeight read on the wrapper
        // collapses to 0 regardless of how much content is actually rendered inside. These tests
        // exercise the private measureContentHeight() through the public measurement flow to
        // confirm it recurses past that collapse.
        const measure = (renderer: RowRenderer, nativeElement: HTMLElement): number =>
            (renderer as any).measureContentHeight(nativeElement);

        it('returns the lowest bottom edge among nested descendants, not the (collapsed) wrapper height', () => {
            const nativeElement = document.createElement('div');
            const rowEl = document.createElement('div');
            spyOn(rowEl, 'getBoundingClientRect').and.returnValue({ top: 0, bottom: 0, left: 0, right: 0, width: 0, height: 0 } as DOMRect);

            const shallow = document.createElement('div');
            spyOn(shallow, 'getBoundingClientRect').and.returnValue({ top: 0, bottom: 40, left: 0, right: 100, width: 100, height: 40 } as DOMRect);

            const deepParent = document.createElement('div');
            spyOn(deepParent, 'getBoundingClientRect').and.returnValue({ top: 0, bottom: 0, left: 0, right: 0, width: 0, height: 0 } as DOMRect);
            const deepChild = document.createElement('span');
            spyOn(deepChild, 'getBoundingClientRect').and.returnValue({ top: 40, bottom: 220, left: 0, right: 100, width: 100, height: 180 } as DOMRect);
            deepParent.appendChild(deepChild);

            rowEl.appendChild(shallow);
            rowEl.appendChild(deepParent);
            nativeElement.appendChild(rowEl);

            const renderer = new RowRenderer({ nativeElement } as ElementRef);

            expect(measure(renderer, nativeElement)).toBe(220);
        });

        it('falls back to scrollHeight when the row wrapper has no children', () => {
            const nativeElement = document.createElement('div');
            const rowEl = document.createElement('div');
            spyOn(rowEl, 'getBoundingClientRect').and.returnValue({ top: 0, bottom: 0, left: 0, right: 0, width: 0, height: 0 } as DOMRect);
            Object.defineProperty(rowEl, 'scrollHeight', { value: 33 });
            nativeElement.appendChild(rowEl);

            const renderer = new RowRenderer({ nativeElement } as ElementRef);

            expect(measure(renderer, nativeElement)).toBe(33);
        });

        it('falls back to the cell GUI scrollHeight when there is no first-child row wrapper at all', () => {
            const nativeElement = document.createElement('div');
            Object.defineProperty(nativeElement, 'scrollHeight', { value: 12 });

            const renderer = new RowRenderer({ nativeElement } as ElementRef);

            expect(measure(renderer, nativeElement)).toBe(12);
        });
    });
});
