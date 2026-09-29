import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CUSTOM_ELEMENTS_SCHEMA, ElementRef } from '@angular/core';
import { RowRenderer } from './row-renderer.component';
import { ICellRendererParams } from 'ag-grid-community';

describe('RowRenderer', () => {
  let component: RowRenderer;
  let fixture: ComponentFixture<RowRenderer>;
  let mockLfc: any;

  beforeEach(async () => {
    mockLfc = {
      containedForm: vi.fn().mockReturnValue({ absoluteLayout: true, formHeight: 50, formWidth: 200 }),
      cache: {
        items: [{ name: 'field1', model: { visible: true }, layout: { top: '0px', left: '0px' } }],
      },
      _foundset: vi.fn().mockReturnValue({
        viewPort: { startIndex: 0, rows: [] },
        selectedRowIndexes: [0],
      }),
      numberOfColumns: 1,
      element: vi.fn().mockReturnValue({ nativeElement: { children: [{ dispatchEvent: vi.fn() }] } }),
      getRowItemTemplate: vi.fn(),
      getRowItemState: vi.fn(),
      getRowClasses: vi.fn().mockReturnValue('svy-listformcomponent-row'),
      getRowStyle: vi.fn().mockReturnValue({}),
      onRowClick: vi.fn(),
      isPerRowAutoHeight: vi.fn().mockReturnValue(false),
      applyMeasuredRowHeight: vi.fn(),
    };

    TestBed.configureTestingModule({
      imports: [RowRenderer],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    })
      .overrideComponent(RowRenderer, {
        set: { imports: [], schemas: [CUSTOM_ELEMENTS_SCHEMA] },
      })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(RowRenderer);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('agInit', () => {
    it('should initialize from AG Grid params', () => {
      const params = {
        context: { componentParent: mockLfc },
        data: [{ _svyRowId: 'row0' }, { _svyRowId: 'row1' }],
        node: { rowIndex: 2, id: 'node-2' },
      } as any;

      component.agInit(params);

      expect(component.lfc).toBe(mockLfc);
      expect(component.foundsetRows).toEqual([{ _svyRowId: 'row0' }, { _svyRowId: 'row1' }]);
      expect(component.startIndex).toBe(2);
    });

    it('should calculate startIndex based on viewport start and numberOfColumns', () => {
      mockLfc._foundset.mockReturnValue({ viewPort: { startIndex: 5, rows: [] }, selectedRowIndexes: [] });
      mockLfc.numberOfColumns = 3;

      const params = {
        context: { componentParent: mockLfc },
        data: [{ _svyRowId: 'rowX' }],
        node: { rowIndex: 4, id: 'node-4' },
      } as any;

      component.agInit(params);
      expect(component.startIndex).toBe((4 - 5) * 3);
    });
  });

  describe('getFoundsetRowIndex', () => {
    it('should return startIndex + i', () => {
      component.startIndex = 10;
      expect(component.getFoundsetRowIndex(0)).toBe(10);
      expect(component.getFoundsetRowIndex(2)).toBe(12);
    });
  });

  describe('refresh', () => {
    it('should return true (nop)', () => {
      expect(component.refresh({} as ICellRendererParams)).toBe(true);
    });
  });

  describe('ngAfterViewInit', () => {
    it('should not measure when not on the per-row auto-height path', () => {
      mockLfc.isPerRowAutoHeight.mockReturnValue(false);
      component.lfc = mockLfc;
      component.ngAfterViewInit();
      expect(mockLfc.applyMeasuredRowHeight).not.toHaveBeenCalled();
    });

    it('should measure and report the row height on the per-row auto-height path (SVY-21457)', async () => {
      mockLfc.isPerRowAutoHeight.mockReturnValue(true);
      component.lfc = mockLfc;

      const params = {
        context: { componentParent: mockLfc },
        data: [{ _svyRowId: 'row0' }],
        node: { rowIndex: 0, id: 'node-0', detail: false },
      } as any;
      component.agInit(params);

      // measureContentHeight takes cellGui's :first-child as the row wrapper (top edge),
      // then recurses into its descendants and takes the lowest bottom edge relative to
      // that top. Give the wrapper a measurable descendant so a positive height comes back.
      const rowEl = document.createElement('div');
      Object.defineProperty(rowEl, 'getBoundingClientRect', {
        value: () => ({ top: 0, bottom: 0, height: 0, width: 100 }) as DOMRect,
      });
      const inner = document.createElement('div');
      Object.defineProperty(inner, 'getBoundingClientRect', {
        value: () => ({ top: 0, bottom: 120, height: 120, width: 100 }) as DOMRect,
      });
      rowEl.appendChild(inner);
      const cellGui = document.createElement('div');
      cellGui.appendChild(rowEl);
      (component as any).elementRef = new ElementRef(cellGui);

      // run the two chained requestAnimationFrame callbacks synchronously
      const raf = vi.spyOn(window, 'requestAnimationFrame').mockImplementation((cb: FrameRequestCallback) => {
        cb(0);
        return 0;
      });
      component.ngAfterViewInit();
      raf.mockRestore();

      expect(mockLfc.applyMeasuredRowHeight).toHaveBeenCalled();
      const [rowId, node, measured] = mockLfc.applyMeasuredRowHeight.mock.calls[0];
      expect(rowId).toBe('node-0');
      expect(node.id).toBe('node-0');
      expect(measured).toBe(120);
    });
  });

  describe('registerCSTS', () => {
    it('should re-dispatch event on lfc element', () => {
      const mockDispatch = vi.fn();
      mockLfc.element.mockReturnValue({ nativeElement: { children: [{ dispatchEvent: mockDispatch }] } });
      component.lfc = mockLfc;

      const customEvent = new CustomEvent('registerCSTS', { detail: { key: 'value' } });
      component.registerCSTS(customEvent);

      expect(mockDispatch).toHaveBeenCalled();
      const dispatchedEvent = mockDispatch.mock.calls[0][0] as CustomEvent;
      expect(dispatchedEvent.type).toBe('registerCSTS');
      expect(dispatchedEvent.detail).toEqual({ key: 'value' });
      expect(dispatchedEvent.bubbles).toBe(true);
    });
  });
});
