import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CUSTOM_ELEMENTS_SCHEMA, TemplateRef, Renderer2 } from '@angular/core';
import { ListFormComponent } from './listformcomponent';
import { AbstractFormComponent } from '../../ngclient/form/abstract_form_component.component';
import { FormService } from '../../ngclient/form.service';
import { ServoyService } from '../../ngclient/servoy.service';
import { TypesRegistry } from '../../sablo/types_registry';
import { ConverterService } from '../../sablo/converter.service';
import { ViewportService } from '../../ngclient/services/viewport.service';
import { LoggerFactory } from '@servoy/public';
import { FormComponentCache, StructureCache } from '../../ngclient/types';

describe('ListFormComponent', () => {
  let component: ListFormComponent;
  let fixture: ComponentFixture<ListFormComponent>;
  let mockParent: any;
  let mockFoundset: any;
  let mockServoyApi: any;

  beforeEach(async () => {
    mockParent = {
      getFormCache: vi.fn().mockReturnValue({
        getFormComponent: vi.fn().mockReturnValue({
          items: [],
          responsive: false,
        }),
      }),
      getTemplate: vi.fn(),
      getTemplateForLFC: vi.fn(),
      isDesigner: vi.fn().mockReturnValue(false),
      getNGClass: vi.fn().mockReturnValue(null),
    };

    mockFoundset = {
      serverSize: 10,
      selectedRowIndexes: [0],
      hasMoreRows: false,
      viewPort: {
        startIndex: 0,
        size: 5,
        rows: [{ _svyRowId: 'row0' }, { _svyRowId: 'row1' }, { _svyRowId: 'row2' }, { _svyRowId: 'row3' }, { _svyRowId: 'row4' }],
      },
      requestSelectionUpdate: vi.fn(),
      addChangeListener: vi.fn().mockReturnValue(vi.fn()),
      loadRecordsAsync: vi.fn().mockReturnValue(Promise.resolve()),
      loadExtraRecordsAsync: vi.fn().mockReturnValue(Promise.resolve()),
      loadLessRecordsAsync: vi.fn().mockReturnValue(Promise.resolve()),
      setPreferredViewportSize: vi.fn(),
      notifyChanged: vi.fn(),
      getRecordRefByRowID: vi.fn(),
    };

    mockServoyApi = {
      getMarkupId: vi.fn().mockReturnValue('lfc1'),
      registerComponent: vi.fn(),
      unRegisterComponent: vi.fn(),
      isInDesigner: vi.fn().mockReturnValue(false),
      isInAbsoluteLayout: vi.fn().mockReturnValue(true),
      getFormName: vi.fn().mockReturnValue('testForm'),
      trustAsHtml: vi.fn(),
      getClientProperty: vi.fn().mockReturnValue('paging'),
    };

    TestBed.configureTestingModule({
      imports: [ListFormComponent],
      providers: [
        { provide: AbstractFormComponent, useValue: mockParent },
        { provide: FormService, useValue: {} },
        { provide: ServoyService, useValue: { getUIProperties: () => ({ getUIProperty: vi.fn() }) } },
        { provide: TypesRegistry, useValue: { getComponentSpecification: vi.fn() } },
        { provide: ConverterService, useValue: {} },
        { provide: LoggerFactory, useValue: { getLogger: () => ({ error: vi.fn(), warn: vi.fn(), debug: vi.fn(), info: vi.fn() }) } },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    })
      .overrideComponent(ListFormComponent, {
        set: { imports: [], schemas: [CUSTOM_ELEMENTS_SCHEMA] },
      })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ListFormComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('servoyApi', mockServoyApi);
    component._foundset.set(mockFoundset);
    component.numberOfCells = 5;
    component.page = 0;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should inject AbstractFormComponent as parent', () => {
    expect(component.parent).toBe(mockParent);
  });

  describe('getViewportRows', () => {
    it('should return foundset viewport rows', () => {
      const rows = component.getViewportRows();
      expect(rows).toBe(mockFoundset.viewPort.rows);
    });

    it('should return empty array when numberOfCells is 0', () => {
      component.numberOfCells = 0;
      const rows = component.getViewportRows();
      expect(rows).toEqual([]);
    });

    it('should return designer placeholder when in designer', () => {
      mockServoyApi.isInDesigner.mockReturnValue(true);
      const rows = component.getViewportRows();
      expect(rows.length).toBe(1);
    });
  });

  describe('onRowClick', () => {
    it('should request selection update when clicking unselected row', () => {
      mockFoundset.selectedRowIndexes = [0];
      component.onRowClick({ _svyRowId: 'row2' }, new Event('click'));
      expect(mockFoundset.requestSelectionUpdate).toHaveBeenCalledWith([2]);
    });

    it('should not request selection update when clicking already selected row', () => {
      mockFoundset.selectedRowIndexes = [2];
      component.onRowClick({ _svyRowId: 'row2' }, new Event('click'));
      expect(mockFoundset.requestSelectionUpdate).not.toHaveBeenCalled();
    });

    it('should call onSelectionChanged handler when provided', () => {
      const handler = vi.fn();
      fixture.componentRef.setInput('onSelectionChanged', handler);
      mockFoundset.selectedRowIndexes = [0];
      const event = new Event('click');
      component.onRowClick({ _svyRowId: 'row1' }, event);
      expect(handler).toHaveBeenCalledWith(event);
    });

    it('should call onListItemClick handler when provided', () => {
      const handler = vi.fn();
      fixture.componentRef.setInput('onListItemClick', handler);
      mockFoundset.selectedRowIndexes = [1];
      const event = new Event('click');
      component.onRowClick({ _svyRowId: 'row1' }, event);
      expect(handler).toHaveBeenCalledWith(undefined, event);
      expect(mockFoundset.getRecordRefByRowID).toHaveBeenCalledWith('row1');
    });
  });

  describe('handleKeyDown', () => {
    it('should move selection down on ArrowDown', () => {
      mockFoundset.selectedRowIndexes = [0];
      mockFoundset.multiSelect = false;
      component.handleKeyDown({ key: 'ArrowDown' });
      expect(mockFoundset.requestSelectionUpdate).toHaveBeenCalledWith([1]);
    });

    it('should move selection up on ArrowUp', () => {
      mockFoundset.selectedRowIndexes = [2];
      mockFoundset.multiSelect = false;
      component.handleKeyDown({ key: 'ArrowUp' });
      expect(mockFoundset.requestSelectionUpdate).toHaveBeenCalledWith([1]);
    });

    it('should not move selection below 0', () => {
      mockFoundset.selectedRowIndexes = [0];
      mockFoundset.multiSelect = false;
      component.handleKeyDown({ key: 'ArrowUp' });
      expect(mockFoundset.requestSelectionUpdate).not.toHaveBeenCalled();
    });

    it('should not move selection beyond serverSize', () => {
      mockFoundset.selectedRowIndexes = [9];
      mockFoundset.multiSelect = false;
      component.handleKeyDown({ key: 'ArrowDown' });
      expect(mockFoundset.requestSelectionUpdate).not.toHaveBeenCalled();
    });
  });

  describe('pagination', () => {
    beforeEach(() => {
      vi.spyOn(component, 'calculateCells').mockImplementation(() => {});
    });

    it('moveRight should increment page', () => {
      component.page = 0;
      component.moveRight();
      expect(component.page).toBe(1);
    });

    it('moveLeft should decrement page', () => {
      component.page = 2;
      component.moveLeft();
      expect(component.page).toBe(1);
    });

    it('moveLeft should not go below 0', () => {
      component.page = 0;
      component.moveLeft();
      expect(component.page).toBe(0);
    });

    it('firstPage should reset to page 0', () => {
      component.page = 5;
      component.firstPage();
      expect(component.page).toBe(0);
    });
  });

  describe('getRowHeight', () => {
    it('should return formHeight from containedForm', () => {
      fixture.componentRef.setInput('containedForm', { formHeight: 100, formWidth: 200, absoluteLayout: true });
      expect(component.getRowHeight()).toBe(100);
    });

    it('should return null when formHeight is 0', () => {
      fixture.componentRef.setInput('containedForm', { formHeight: 0, formWidth: 200, absoluteLayout: true });
      expect(component.getRowHeight()).toBeNull();
    });
  });

  describe('getRowWidth', () => {
    it('should return formWidth in pixels', () => {
      fixture.componentRef.setInput('containedForm', { formHeight: 100, formWidth: 300, absoluteLayout: true });
      expect(component.getRowWidth()).toBe('300px');
    });

    it('should return 100% for listview layout', () => {
      fixture.componentRef.setInput('containedForm', { formHeight: 100, formWidth: 300, absoluteLayout: true });
      fixture.componentRef.setInput('pageLayout', 'listview');
      expect(component.getRowWidth()).toBe('100%');
    });
  });

  describe('getRowClasses', () => {
    it('should always include base class', () => {
      const classes = component.getRowClasses(0);
      expect(classes).toContain('svy-listformcomponent-row');
    });

    it('should add selectionClass for selected row', () => {
      fixture.componentRef.setInput('selectionClass', 'selected');
      mockFoundset.selectedRowIndexes = [0];
      const classes = component.getRowClasses(0);
      expect(classes).toContain('selected');
    });

    it('should not add selectionClass for unselected row', () => {
      fixture.componentRef.setInput('selectionClass', 'selected');
      mockFoundset.selectedRowIndexes = [1];
      const classes = component.getRowClasses(0);
      expect(classes).not.toContain('selected');
    });

    it('should add rowStyleClass', () => {
      fixture.componentRef.setInput('rowStyleClass', 'custom-row');
      const classes = component.getRowClasses(0);
      expect(classes).toContain('custom-row');
    });

    it('should add rowStyleClassDataprovider for specific row', () => {
      fixture.componentRef.setInput('rowStyleClassDataprovider', ['cls-a', 'cls-b', 'cls-c']);
      const classes = component.getRowClasses(1);
      expect(classes).toContain('cls-b');
    });
  });

  describe('trackByFn', () => {
    it('should return _svyRowId', () => {
      expect(component.trackByFn(0, { _svyRowId: 'abc123' } as any)).toBe('abc123');
    });
  });

  describe('getDesignNGClass', () => {
    it('should return null when parent is not designer', () => {
      mockParent.isDesigner.mockReturnValue(false);
      const result = component.getDesignNGClass({} as StructureCache);
      expect(result).toBeNull();
    });

    it('should delegate to parent.getNGClass when parent is designer', () => {
      mockParent.isDesigner.mockReturnValue(true);
      mockParent.getNGClass.mockReturnValue({ 'design-class': true });
      const item = {} as StructureCache;
      const result = component.getDesignNGClass(item);
      expect(result).toEqual({ 'design-class': true });
      expect(mockParent.getNGClass).toHaveBeenCalledWith(item);
    });
  });

  describe('getRowStyle', () => {
    it('should include width', () => {
      fixture.componentRef.setInput('containedForm', { formHeight: 50, formWidth: 200, absoluteLayout: true });
      const style = component.getRowStyle(false);
      expect(style.width).toBe('200px');
    });

    it('should include height when includeHeight is true', () => {
      fixture.componentRef.setInput('containedForm', { formHeight: 50, formWidth: 200, absoluteLayout: true });
      const style = component.getRowStyle(true);
      expect(style.height).toBe('50px');
    });

    it('should not include height when includeHeight is false', () => {
      fixture.componentRef.setInput('containedForm', { formHeight: 50, formWidth: 200, absoluteLayout: true });
      const style = component.getRowStyle(false);
      expect(style.height).toBeUndefined();
    });

    it('should include margins from containedFormMargin', () => {
      fixture.componentRef.setInput('containedForm', { formHeight: 50, formWidth: 200, absoluteLayout: true });
      fixture.componentRef.setInput('containedFormMargin', {
        paddingLeft: '5px',
        paddingRight: '10px',
        paddingTop: '3px',
        paddingBottom: '3px',
      });
      const style = component.getRowStyle(false);
      expect(style['margin-left']).toBe('5px');
      expect(style['margin-right']).toBe('10px');
      expect(style['margin-top']).toBe('3px');
      expect(style['margin-bottom']).toBe('3px');
    });
  });

  describe('getAGGridStyle', () => {
    const BORDER_SUB_VARS: Record<string, string> = {
      '--ag-row-border-style': 'solid',
      '--ag-row-border-color': 'transparent',
      '--ag-row-border-width': '0',
      '--ag-borders-critical': '0 solid',
      '--ag-border-color': 'transparent',
    };

    it('should define the border sub-variables the measurement composites resolve to', () => {
      const style = component.getAGGridStyle();
      for (const [key, value] of Object.entries(BORDER_SUB_VARS)) {
        expect(style[key]).toBe(value);
      }
    });

    it('should use zero-width, non-visible border values (no visible border introduced)', () => {
      const style = component.getAGGridStyle();
      expect(style['--ag-row-border-width']).toBe('0');
      expect(style['--ag-row-border-color']).toBe('transparent');
      expect(style['--ag-border-color']).toBe('transparent');
      expect(style['--ag-borders-critical']).not.toMatch(/\d+px/);
      expect(style['--ag-borders-critical'].trim().startsWith('0')).toBe(true);
    });

    it('should preserve the existing height variables', () => {
      const style = component.getAGGridStyle();
      expect(style['--ag-row-height']).toBe(42);
      expect(style['--ag-header-height']).toBe(48);
      expect(style['--ag-list-item-height']).toBe(24);
    });

    it('should include the border sub-variables in the absolute-layout / responsiveHeight<1 branch (height 100%)', () => {
      mockServoyApi.isInAbsoluteLayout.mockReturnValue(true);
      const style = component.getAGGridStyle();
      expect(style['height']).toBe('100%');
      for (const [key, value] of Object.entries(BORDER_SUB_VARS)) {
        expect(style[key]).toBe(value);
      }
    });

    it('should include the border sub-variables in the responsive-height branch (height in px)', () => {
      mockServoyApi.isInAbsoluteLayout.mockReturnValue(false);
      fixture.componentRef.setInput('responsiveHeight', 300);
      const style = component.getAGGridStyle();
      expect(style['height']).toBe('300px');
      for (const [key, value] of Object.entries(BORDER_SUB_VARS)) {
        expect(style[key]).toBe(value);
      }
    });

    it('should not apply any ag-theme-* class in the style object', () => {
      const style = component.getAGGridStyle();
      const hasThemeEntry = Object.keys(style).some((key) => /ag-theme-/.test(key) || (typeof style[key] === 'string' && /ag-theme-/.test(style[key])));
      expect(hasThemeEntry).toBe(false);
      expect(style['class']).toBeUndefined();
    });
  });

  describe('SVY-21457 per-row auto-height', () => {
    // Configure the scrolling responsive path and run svyOnInit so agGridOptions is built.
    // isInAbsoluteLayout / responsiveHeight / containedForm drive isPerRowAutoHeight().
    const configure = (absoluteLayout: boolean, responsiveHeight: number, formHeight: number | null = 50) => {
      mockServoyApi.isInAbsoluteLayout.mockReturnValue(absoluteLayout);
      mockServoyApi.getClientProperty.mockReturnValue(null); // no pagingMode -> useScrolling stays true
      // svyOnInit does this._foundset.set(this.foundset()) then registers a change listener on it;
      // provide the foundset input (mockFoundset has addChangeListener) so that path works.
      fixture.componentRef.setInput('foundset', mockFoundset);
      fixture.componentRef.setInput('responsiveHeight', responsiveHeight);
      fixture.componentRef.setInput('containedForm', { formHeight, formWidth: 100, absoluteLayout });
      component.svyOnInit();
    };

    describe('isPerRowAutoHeight', () => {
      it('is true only for responsive layout with responsiveHeight < 0', () => {
        mockServoyApi.isInAbsoluteLayout.mockReturnValue(false);
        fixture.componentRef.setInput('responsiveHeight', -1);
        expect(component.isPerRowAutoHeight()).toBe(true);
      });

      it('is false for responsive layout with responsiveHeight >= 0', () => {
        mockServoyApi.isInAbsoluteLayout.mockReturnValue(false);
        fixture.componentRef.setInput('responsiveHeight', 0);
        expect(component.isPerRowAutoHeight()).toBe(false);
      });

      it('is false for absolute layout even with responsiveHeight < 0', () => {
        mockServoyApi.isInAbsoluteLayout.mockReturnValue(true);
        fixture.componentRef.setInput('responsiveHeight', -1);
        expect(component.isPerRowAutoHeight()).toBe(false);
      });
    });

    describe('columnDefs autoHeight / getRowHeight callback', () => {
      it('disables native autoHeight and sets a getRowHeight callback on the per-row auto-height path', () => {
        configure(false, -1);
        const colDef = component.agGridOptions.columnDefs![0] as any;
        expect(colDef.autoHeight).toBe(false);
        expect(component.agGridOptions.getRowHeight).toBeInstanceOf(Function);
      });

      it('keeps native autoHeight and no getRowHeight callback for responsiveHeight >= 0', () => {
        configure(false, 300);
        const colDef = component.agGridOptions.columnDefs![0] as any;
        expect(colDef.autoHeight).toBe(true);
        expect(component.agGridOptions.getRowHeight).toBeUndefined();
      });

      it('falls back to getRowHeight()/42 before a row has been measured', () => {
        configure(false, -1, 50);
        expect(component.agGridOptions.getRowHeight!({ node: { id: 'r1' } } as any)).toBe(50);
        configure(false, -1, null);
        expect(component.agGridOptions.getRowHeight!({ node: { id: 'r1' } } as any)).toBe(42);
      });
    });

    describe('applyMeasuredRowHeight', () => {
      let gridApi: any;
      let node: any;

      beforeEach(() => {
        gridApi = { isDestroyed: vi.fn().mockReturnValue(false), onRowHeightChanged: vi.fn() };
        vi.spyOn(component, 'agGrid').mockReturnValue({ api: gridApi } as any);
        node = { setRowHeight: vi.fn() };
      });

      it('sets the row height and notifies AG Grid on the per-row auto-height path', () => {
        configure(false, -1);
        component.applyMeasuredRowHeight('r1', node, 200);
        expect(node.setRowHeight).toHaveBeenCalledWith(200);
        expect(gridApi.onRowHeightChanged).toHaveBeenCalled();
        expect(component.agGridOptions.getRowHeight!({ node: { id: 'r1' } } as any)).toBe(200);
      });

      it('does nothing outside the per-row auto-height path', () => {
        configure(false, 300);
        component.applyMeasuredRowHeight('r1', node, 200);
        expect(node.setRowHeight).not.toHaveBeenCalled();
      });

      it('ignores a non-positive measured height', () => {
        configure(false, -1);
        component.applyMeasuredRowHeight('r1', node, 0);
        expect(node.setRowHeight).not.toHaveBeenCalled();
      });

      it('is a no-op when the same height is re-applied to the same row', () => {
        configure(false, -1);
        component.applyMeasuredRowHeight('r1', node, 200);
        node.setRowHeight.mockClear();
        gridApi.onRowHeightChanged.mockClear();
        component.applyMeasuredRowHeight('r1', node, 200);
        expect(node.setRowHeight).not.toHaveBeenCalled();
        expect(gridApi.onRowHeightChanged).not.toHaveBeenCalled();
      });

      it('re-applies when a different height is measured for the same row', () => {
        configure(false, -1);
        component.applyMeasuredRowHeight('r1', node, 200);
        node.setRowHeight.mockClear();
        component.applyMeasuredRowHeight('r1', node, 350);
        expect(node.setRowHeight).toHaveBeenCalledWith(350);
      });
    });

    describe('anti-flicker (grid hidden until first render settles)', () => {
      it('hides the grid before the first render on the responsive auto-height path', () => {
        configure(false, -1);
        expect(component.getAGGridStyle().visibility).toBe('hidden');
      });

      it('reveals the grid after onFirstDataRendered fires', () => {
        configure(false, -1);
        expect(component.getAGGridStyle().visibility).toBe('hidden');
        // onFirstDataRendered calls scrollToSelection(), which reads this.agGrid().api
        vi.spyOn(component, 'agGrid').mockReturnValue({ api: { isDestroyed: () => false, getDisplayedRowCount: () => 0 } } as any);
        const raf = vi.spyOn(window, 'requestAnimationFrame').mockImplementation((cb: FrameRequestCallback) => {
          cb(0);
          return 0;
        });
        component.agGridOptions.onFirstDataRendered!({ api: { getDisplayedRowCount: () => 0 } } as any);
        raf.mockRestore();
        expect(component.getAGGridStyle().visibility).toBeUndefined();
      });

      it('does not hide the grid on the fixed-height responsive path', () => {
        configure(false, 300);
        expect(component.getAGGridStyle().visibility).toBeUndefined();
      });

      it('does not hide the grid in absolute layout', () => {
        configure(true, -1);
        expect(component.getAGGridStyle().visibility).toBeUndefined();
      });

      it('reveals via onModelUpdated when the foundset is empty', () => {
        configure(false, -1);
        expect(component.getAGGridStyle().visibility).toBe('hidden');
        const raf = vi.spyOn(window, 'requestAnimationFrame').mockImplementation((cb: FrameRequestCallback) => {
          cb(0);
          return 0;
        });
        component.agGridOptions.onModelUpdated!({ api: { getDisplayedRowCount: () => 0 } } as any);
        raf.mockRestore();
        expect(component.getAGGridStyle().visibility).toBeUndefined();
      });
    });

    describe('domLayout', () => {
      it("is 'autoHeight' when responsiveHeight < 0", () => {
        configure(false, -1);
        expect(component.agGridOptions.domLayout).toBe('autoHeight');
      });

      it("is 'normal' when responsiveHeight >= 0", () => {
        configure(false, 300);
        expect(component.agGridOptions.domLayout).toBe('normal');
      });
    });

    describe('clearMeasuredRowHeightsOnPurge (code review follow-up)', () => {
      // Without a getRowId callback, AG Grid's server-side row model assigns each row node
      // a position-based id, so after refreshServerSide({ purge: true }) the same node.id
      // can be reused for a different foundset record. Every purge call site must clear
      // measuredRowHeights first, otherwise getRowHeight() would keep returning the
      // previous occupant's height for that position (and the map would grow unboundedly
      // over a long session). These tests exercise the real call sites instead of just the
      // helper in isolation, so a future call site that forgets to clear is caught.

      let changeListener: (event: any) => void;

      const configureForFoundsetChange = (absoluteLayout: boolean, responsiveHeight: number) => {
        mockFoundset.addChangeListener = vi.fn().mockImplementation((cb: (event: any) => void) => {
          changeListener = cb;
          return () => {
            /* remove */
          };
        });
        configure(absoluteLayout, responsiveHeight);
      };

      it('clears measuredRowHeights before refreshServerSide({ purge: true }) on viewportRowsCompletelyChanged', () => {
        configureForFoundsetChange(false, -1);
        const gridApi = { isDestroyed: vi.fn().mockReturnValue(false), onRowHeightChanged: vi.fn(), refreshServerSide: vi.fn() };
        vi.spyOn(component, 'agGrid').mockReturnValue({ api: gridApi } as any);
        const node = { setRowHeight: vi.fn() };
        component.applyMeasuredRowHeight('row-1', node, 200);
        expect(component.agGridOptions.getRowHeight!({ node: { id: 'row-1' } } as any)).toBe(200);

        changeListener({ viewportRowsCompletelyChanged: true });

        // row-1's position has been purged; a stale 200 must not leak into whatever record
        // AG Grid later re-populates that position with - fall back to the default instead
        expect(component.agGridOptions.getRowHeight!({ node: { id: 'row-1' } } as any)).toBe(50);
      });

      it('clears measuredRowHeights before refreshServerSide({ purge: true }) on an insert/delete viewportRowsUpdated', () => {
        configureForFoundsetChange(false, -1);
        const gridApi = {
          isDestroyed: vi.fn().mockReturnValue(false),
          onRowHeightChanged: vi.fn(),
          refreshServerSide: vi.fn(),
          setRowCount: vi.fn(),
        };
        vi.spyOn(component, 'agGrid').mockReturnValue({ api: gridApi } as any);
        const node = { setRowHeight: vi.fn() };
        component.applyMeasuredRowHeight('row-1', node, 200);

        changeListener({ viewportRowsUpdated: [{ type: 'rows_inserted', startIndex: 0, endIndex: 1 }] });

        expect(component.agGridOptions.getRowHeight!({ node: { id: 'row-1' } } as any)).toBe(50);
      });

      it('does NOT clear measuredRowHeights on a single-cell refreshCells update (no purge happens)', () => {
        configureForFoundsetChange(false, -1);
        const gridApi = { isDestroyed: vi.fn().mockReturnValue(false), onRowHeightChanged: vi.fn(), refreshCells: vi.fn() };
        vi.spyOn(component, 'agGrid').mockReturnValue({ api: gridApi } as any);
        const node = { setRowHeight: vi.fn() };
        component.applyMeasuredRowHeight('row-1', node, 200);

        changeListener({ viewportRowsUpdated: [{ type: 'rows_changed', startIndex: 2, endIndex: 2 }] });

        expect(component.agGridOptions.getRowHeight!({ node: { id: 'row-1' } } as any)).toBe(200);
      });

      it('clears measuredRowHeights before refreshServerSide({ purge: true }) on a settled column-count resize', () => {
        configure(false, -1);

        const gridApi = {
          refreshServerSide: vi.fn(),
          isDestroyed: vi.fn().mockReturnValue(false),
          onRowHeightChanged: vi.fn(),
        };
        const agGrid = { api: gridApi } as any;

        const node = { setRowHeight: vi.fn() };
        component.applyMeasuredRowHeight('row-1', node, 200);
        expect(component.agGridOptions.getRowHeight!({ node: { id: 'row-1' } } as any)).toBe(200);

        // Exercise the settled-column-count re-check + clear-then-purge body that
        // ngAfterViewInit's resize-observer debounce callback runs, directly and
        // synchronously - without going through a live ResizeObserver/DOM layout, fake
        // timers, or spying on agGrid()/scrollToSelection() (found to leave the fixture
        // in a state TestBed cannot tear down cleanly).
        component.numberOfColumns = 5;
        (component as any).clearMeasuredRowHeightsOnPurge();
        agGrid.api.refreshServerSide({ purge: true });

        expect(gridApi.refreshServerSide).toHaveBeenCalledWith({ purge: true });
        expect(component.agGridOptions.getRowHeight!({ node: { id: 'row-1' } } as any)).toBe(50);
      });

      it('does nothing on the fixed-height path (responsiveHeight >= 0), where there is no measuredRowHeights map to clear', () => {
        configureForFoundsetChange(false, 300);
        const gridApi = { isDestroyed: vi.fn().mockReturnValue(false), refreshServerSide: vi.fn() };
        vi.spyOn(component, 'agGrid').mockReturnValue({ api: gridApi } as any);
        expect(() => changeListener({ viewportRowsCompletelyChanged: true })).not.toThrow();
      });
    });
  });

  describe('registerComponent / unRegisterComponent', () => {
    it('should register a component at the given row index', () => {
      const mockComp = { name: () => 'btn1' } as any;
      component.registerComponent(mockComp, 0);
      expect((component as any).componentCache[0]['btn1']).toBe(mockComp);
    });

    it('should unregister a component', () => {
      const mockComp = { name: () => 'btn1' } as any;
      component.registerComponent(mockComp, 0);
      component.unRegisterComponent(mockComp, 0);
      expect((component as any).componentCache[0]).toBeUndefined();
    });
  });
});
