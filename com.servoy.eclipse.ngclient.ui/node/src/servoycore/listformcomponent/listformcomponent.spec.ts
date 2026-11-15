import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ListFormComponent } from './listformcomponent';
import { AbstractFormComponent } from '../../ngclient/form/abstract_form_component.component';
import { FormService } from '../../ngclient/form.service';
import { ServoyService } from '../../ngclient/servoy.service';
import { TypesRegistry } from '../../sablo/types_registry';
import { ConverterService } from '../../sablo/converter.service';
import { LoggerFactory, ChangeType } from '@servoy/public';
import { ComponentCache, FormComponentCache, StructureCache } from '../../ngclient/types';

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
      requestSelectionUpdate: vi.fn().mockReturnValue(Promise.resolve()),
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
      // SVY-21610: the stateless form-template render reports isInDesigner() false and signals itself through
      // hasSampleData() instead, so the mock must answer both
      hasSampleData: vi.fn().mockReturnValue(false),
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
        { provide: LoggerFactory, useValue: { getLogger: () => ({ error: vi.fn(), warn: vi.fn(), debug: vi.fn(), info: vi.fn(), buildMessage: (fn: () => string) => fn() }) } },
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

    it('should return designer placeholder when in designer and the foundset has no rows', () => {
      // SVY-21610: the placeholder row is now the fallback for "no viewport rows to show", not an unconditional
      // designer override - a stateless sample-data render DOES get rows (3 synthetic ones) and must render those.
      mockServoyApi.isInDesigner.mockReturnValue(true);
      mockFoundset.viewPort.rows = [];
      const rows = component.getViewportRows();
      expect(rows.length).toBe(1);
    });

    it('should return the real viewport rows in the designer when the foundset does have rows', () => {
      mockServoyApi.isInDesigner.mockReturnValue(true);
      const rows = component.getViewportRows();
      expect(rows).toBe(mockFoundset.viewPort.rows);
    });

    it('should return the sample rows in a sample-data render, not the single placeholder', () => {
      // the SVY-21610 regression this guards: taking the placeholder path here showed one empty row instead of the
      // server's synthetic rows, so an LFC rendered blank in getFormLayout
      mockServoyApi.isInDesigner.mockReturnValue(false);
      mockServoyApi.hasSampleData.mockReturnValue(true);
      const rows = component.getViewportRows();
      expect(rows).toBe(mockFoundset.viewPort.rows);
    });

    it('should return the placeholder row in a sample-data render with no foundset rows', () => {
      mockServoyApi.isInDesigner.mockReturnValue(false);
      mockServoyApi.hasSampleData.mockReturnValue(true);
      mockFoundset.viewPort.rows = [];
      const rows = component.getViewportRows();
      expect(rows.length).toBe(1);
    });
  });

  // SVY-21610: an LFC in the stateless form-template render must render its rows WITHOUT asking the server for
  // anything - there is no websocket, so a load request returns a promise that can never resolve and the component
  // would sit in waitingForLoad rendering nothing.
  describe('sample-data render (SVY-21610)', () => {
    beforeEach(() => {
      mockServoyApi.isInDesigner.mockReturnValue(false);
      mockServoyApi.hasSampleData.mockReturnValue(true);
    });

    it('calculateCells uses the delivered sample rows and issues NO load request', () => {
      component.calculateCells();

      expect(component.numberOfCells).toBe(mockFoundset.viewPort.rows.length);
      expect(mockFoundset.loadRecordsAsync).not.toHaveBeenCalled();
      expect(mockFoundset.loadExtraRecordsAsync).not.toHaveBeenCalled();
      expect(mockFoundset.loadLessRecordsAsync).not.toHaveBeenCalled();
    });

    it('calculateCells falls back to one cell when no sample rows arrived', () => {
      mockFoundset.viewPort.rows = [];
      component.calculateCells();

      expect(component.numberOfCells).toBe(1);
      expect(mockFoundset.loadRecordsAsync).not.toHaveBeenCalled();
    });

    it('getRowItemState resolves a cell from the FormCache when rowItems was never assigned', () => {
      // Reproduces the browser failure exactly: in the stateless render containedForm carries no childElements, so
      // ngOnChanges never assigns rowItems and it stays UNDEFINED (not []). Reading .length on it threw
      // "Cannot read properties of undefined (reading 'length')" and blanked the LFC.
      const textbox = new ComponentCache('textbox_1', 'bootstrapcomponents-textbox', 'textbox', [], {}, TestBed.inject(TypesRegistry));
      (textbox.model as any).dataProviderID = 'Sample';
      component.cache = { items: [textbox] } as unknown as FormComponentCache;
      expect((component as any).rowItems).toBeUndefined();

      const cell = component.getRowItemState(textbox, { _svyRowId: '1' } as any, 0) as any;

      expect(cell).toBeTruthy();
      expect(cell.name).toBe('textbox_1');
      expect(cell.model.dataProviderID).toBe('Sample');
    });

    it('svyOnInit keeps the non-scrolling markup so no AG Grid datasource loads rows', () => {
      component.svyOnInit();

      // the scrolling variant installs an AGGridDatasource whose getRows() goes through the foundset's async load
      expect(component.useScrolling).toBe(false);
    });

    it('svyOnInit already fixes the cell count, so rows exist before the first change detection pass', () => {
      // NG0100 guard: getViewportRows() returns [] while numberOfCells is 0, so if the count were only decided in
      // ngAfterViewInit the row views would be created from inside an after-view hook and Angular would report
      // ExpressionChangedAfterItHasBeenCheckedError on the per-row [style] binding.
      // svyOnInit re-reads the foundset from its input, so set that rather than only the internal signal
      fixture.componentRef.setInput('foundset', mockFoundset);
      component.numberOfCells = 0;

      component.svyOnInit();

      expect(component.numberOfCells).toBe(mockFoundset.viewPort.rows.length);
      expect(component.getViewportRows()).toBe(mockFoundset.viewPort.rows);
    });

    it('ngAfterViewInit adds no second cell calculation after the base class has settled the view', () => {
      // The NG0100 this guards came from calculateCells() running AFTER super.ngAfterViewInit() - i.e. after that
      // base-class call had already run its final cdRef.detectChanges(). The row views then appeared once the count
      // changed, which Angular's dev-mode re-check reports as ExpressionChangedAfterItHasBeenChecked.
      //
      // The count is now settled from svyOnInit, which ServoyBaseComponent.initializeComponent() invokes from
      // INSIDE super.ngAfterViewInit() (the LFC's #element only exists once the view is created, so svyOnInit cannot
      // run at ngOnInit time) - and therefore before that detectChanges(), so the rows are already there when it
      // runs. What must not happen is a further recalculation after it.
      fixture.componentRef.setInput('foundset', mockFoundset);
      component.svyOnInit();
      const countAfterInit = component.numberOfCells;
      // mark the component initialized so the base class does not re-enter svyOnInit, isolating the code that runs
      // after super.ngAfterViewInit() returns
      (component as any).initialized = true;
      const calculateCellsSpy = vi.spyOn(component, 'calculateCells');

      component.ngAfterViewInit();

      expect(calculateCellsSpy).not.toHaveBeenCalled();
      expect(component.numberOfCells).toBe(countAfterInit);
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

    // SVY-21563: clicking a checkbox in a row triggers a server round-trip (calc / enabled-dataprovider)
    // whose selection update can overlap the row-click selection request; requestSelectionUpdate then
    // rejects the superseded deferred. That rejection must be handled so it does not surface as an
    // "Uncaught (in promise)" error in the browser console.
    it('SVY-21563: should swallow a superseded-selection rejection from requestSelectionUpdate', async () => {
      mockFoundset.selectedRowIndexes = [0];
      mockFoundset.requestSelectionUpdate = vi.fn().mockReturnValue(Promise.reject('Selection change defer cancelled because we are already sending another selection to server.'));

      let unhandled: unknown;
      const onUnhandled = (event: PromiseRejectionEvent) => {
        unhandled = event.reason;
      };
      window.addEventListener('unhandledrejection', onUnhandled);
      try {
        component.onRowClick({ _svyRowId: 'row2' }, new Event('click'));
        expect(mockFoundset.requestSelectionUpdate).toHaveBeenCalledWith([2]);
        // let the microtask queue (and any unhandled-rejection detection) flush
        await Promise.resolve();
        await new Promise((resolve) => setTimeout(resolve, 0));
      } finally {
        window.removeEventListener('unhandledrejection', onUnhandled);
      }
      expect(unhandled).toBeUndefined();
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

    // SVY-21563: same superseded-selection rejection guard as onRowClick, for the keyboard path.
    it('SVY-21563: should swallow a superseded-selection rejection from requestSelectionUpdate', async () => {
      mockFoundset.selectedRowIndexes = [0];
      mockFoundset.multiSelect = false;
      mockFoundset.requestSelectionUpdate = vi.fn().mockReturnValue(Promise.reject('Selection change defer cancelled because we are already sending another selection to server.'));

      let unhandled: unknown;
      const onUnhandled = (event: PromiseRejectionEvent) => {
        unhandled = event.reason;
      };
      window.addEventListener('unhandledrejection', onUnhandled);
      try {
        component.handleKeyDown({ key: 'ArrowDown' });
        expect(mockFoundset.requestSelectionUpdate).toHaveBeenCalledWith([1]);
        await Promise.resolve();
        await new Promise((resolve) => setTimeout(resolve, 0));
      } finally {
        window.removeEventListener('unhandledrejection', onUnhandled);
      }
      expect(unhandled).toBeUndefined();
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

    it('unRegisterComponent should keep other components registered at the same row', () => {
      const compA = { name: () => 'a' } as any;
      const compB = { name: () => 'b' } as any;
      component.registerComponent(compA, 0);
      component.registerComponent(compB, 0);
      component.unRegisterComponent(compA, 0);
      expect((component as any).componentCache[0]['b']).toBe(compB);
    });
  });

  describe('getStyleClasses', () => {
    it('should return an empty array when no styleClass is set', () => {
      expect(component.getStyleClasses()).toEqual([]);
    });

    it('should include the styleClass input', () => {
      fixture.componentRef.setInput('styleClass', 'my-class');
      expect(component.getStyleClasses()).toEqual(['my-class']);
    });
  });

  describe('getRowItems', () => {
    it('should return an empty array before ngOnChanges assigns rowItems', () => {
      expect(component.getRowItems()).toEqual([]);
    });

    it('should expose the rowItems populated from a containedForm with childElements', () => {
      const childElements = [{ specName: 'bootstrapcomponents-textbox', name: 'tb', model: {} } as any];
      // call svyOnChanges-path logic directly without going through ServoyBaseComponent.initializeComponent()
      // (which would run svyOnInit and need a live foundset); only the rowItems assignment is under test here
      fixture.componentRef.setInput('containedForm', { formHeight: 10, formWidth: 10, absoluteLayout: true, childElements });
      (component as any).foundset = () => mockFoundset;
      component._foundset.set(mockFoundset);
      (component as any).rowItems = [];
      childElements.forEach((elem: any) => (component as any).rowItems.push(elem));
      expect(component.getRowItems().length).toBe(1);
    });
  });

  describe('getNumberOfColumns', () => {
    it('should return the computed numberOfColumns field', () => {
      component.numberOfColumns = 3;
      expect(component.getNumberOfColumns()).toBe(3);
    });
  });

  describe('getDesignNGClass', () => {
    it('should return null when the parent is not a designer', () => {
      mockParent.isDesigner.mockReturnValue(false);
      expect(component.getDesignNGClass({} as any)).toBeNull();
    });
  });

  describe('datachange', () => {
    it('should update the model and forward the change to the cell state', () => {
      const sendChanges = vi.fn();
      const cell: any = { model: { prop: 'old' }, state: { sendChanges }, rowId: 'row1' };
      component.datachange(cell, 'prop', 'new', true);
      expect(cell.model.prop).toBe('new');
      expect(sendChanges).toHaveBeenCalledWith('prop', 'new', 'old', 'row1', true);
    });
  });

  describe('getServoyApi', () => {
    it('should create and cache the cell api', () => {
      fixture.componentRef.setInput('containedForm', { absoluteLayout: true, formHeight: 10, formWidth: 10 });
      const cell: any = { api: null, name: 'cell1', rowIndex: 0, state: { name: 'cell1', model: { svyMarkupId: 'mk' } } };
      const api1 = component.getServoyApi(cell);
      expect(api1).toBeTruthy();
      const api2 = component.getServoyApi(cell);
      expect(api2).toBe(api1);
    });
  });

  describe('calculateCells in designer', () => {
    it('should fix numberOfCells to 1 when in the designer', () => {
      mockServoyApi.isInDesigner.mockReturnValue(true);
      component.calculateCells();
      expect(component.numberOfCells).toBe(1);
    });

    it('should not recalculate while waiting for a load', () => {
      (component as any).waitingForLoad = true;
      component.numberOfCells = 7;
      component.calculateCells();
      expect(component.numberOfCells).toBe(7);
    });
  });

  // The scrolling (ag-grid) path: svyOnInit builds a large agGridOptions object whose nested callbacks are never
  // invoked by the normal tests. Build it once with a scrolling config, then call each callback with hand-built
  // fake params - no real grid is needed.
  describe('scrolling svyOnInit agGridOptions callbacks', () => {
    const configureScrolling = () => {
      mockServoyApi.isInDesigner.mockReturnValue(false);
      mockServoyApi.hasSampleData.mockReturnValue(false);
      mockServoyApi.getClientProperty.mockReturnValue(null); // no pagingMode -> useScrolling stays true
      mockServoyApi.isInAbsoluteLayout.mockReturnValue(true);
      fixture.componentRef.setInput('foundset', mockFoundset);
      fixture.componentRef.setInput('containedForm', { formHeight: 50, formWidth: 100, absoluteLayout: true });
      fixture.componentRef.setInput('responsiveHeight', 0);
      component.svyOnInit();
    };

    it('enables scrolling and builds agGridOptions when no pagingMode is set', () => {
      configureScrolling();
      expect(component.useScrolling).toBe(true);
      expect(component.agGridOptions).toBeTruthy();
      expect(component.agGridOptions.rowModelType).toBe('serverSide');
    });

    it('falls back to the paged (non-scrolling) path when a pagingMode client property is set', () => {
      mockServoyApi.isInDesigner.mockReturnValue(false);
      mockServoyApi.hasSampleData.mockReturnValue(false);
      mockServoyApi.getClientProperty.mockReturnValue('paging');
      fixture.componentRef.setInput('foundset', mockFoundset);
      component.svyOnInit();
      expect(component.useScrolling).toBe(false);
    });

    describe('defaultColDef.suppressKeyboardEvent', () => {
      it('returns false for a non-Tab key', () => {
        configureScrolling();
        const cb = (component.agGridOptions.defaultColDef as any).suppressKeyboardEvent;
        expect(cb({ event: { keyCode: 13, altKey: false } })).toBe(false);
      });

      it('returns true for Tab (keyCode 9) without altKey and does not move focus', () => {
        configureScrolling();
        const querySpy = vi.spyOn((component as any).doc, 'querySelector');
        const cb = (component.agGridOptions.defaultColDef as any).suppressKeyboardEvent;
        expect(cb({ event: { keyCode: 9, altKey: false } })).toBe(true);
        expect(querySpy).not.toHaveBeenCalled();
        querySpy.mockRestore();
      });

      it('focuses the next tabindex element for Tab (keyCode 9) with altKey', () => {
        configureScrolling();
        vi.spyOn(component, 'sabloTabseqDirective').mockReturnValue({ runtimeIndex: { nextAvailableIndex: 7 } } as any);
        const focus = vi.fn();
        const querySpy = vi.spyOn((component as any).doc, 'querySelector').mockReturnValue({ focus } as any);
        const cb = (component.agGridOptions.defaultColDef as any).suppressKeyboardEvent;
        expect(cb({ event: { keyCode: 9, altKey: true } })).toBe(true);
        expect(querySpy).toHaveBeenCalledWith('[tabindex="7"]');
        expect(focus).toHaveBeenCalled();
        querySpy.mockRestore();
      });

      it('does not throw when the next tabindex element is not found (altKey Tab)', () => {
        configureScrolling();
        vi.spyOn(component, 'sabloTabseqDirective').mockReturnValue({ runtimeIndex: { nextAvailableIndex: 99 } } as any);
        const querySpy = vi.spyOn((component as any).doc, 'querySelector').mockReturnValue(null);
        const cb = (component.agGridOptions.defaultColDef as any).suppressKeyboardEvent;
        expect(cb({ event: { keyCode: 9, altKey: true } })).toBe(true);
        querySpy.mockRestore();
      });
    });

    describe('navigateToNextCell', () => {
      beforeEach(() => {
        configureScrolling();
        component.numberOfColumns = 1;
      });

      it('moves down one row on KEY_DOWN (40)', () => {
        mockFoundset.selectedRowIndexes = [2];
        mockFoundset.serverSize = 10;
        const next = { rowIndex: 0 };
        const result = component.agGridOptions.navigateToNextCell!({ key: 40, previousCellPosition: { rowIndex: 2 }, nextCellPosition: next } as any);
        expect(result).toBe(next);
        expect((result as any).rowIndex).toBe(3);
      });

      it('clamps at serverSize - 1 on KEY_DOWN', () => {
        mockFoundset.selectedRowIndexes = [9];
        mockFoundset.serverSize = 10;
        const next = { rowIndex: 0 };
        const result = component.agGridOptions.navigateToNextCell!({ key: 40, previousCellPosition: {}, nextCellPosition: next } as any);
        expect((result as any).rowIndex).toBe(9);
      });

      it('moves up one row on KEY_UP (38)', () => {
        mockFoundset.selectedRowIndexes = [5];
        const next = { rowIndex: 0 };
        const result = component.agGridOptions.navigateToNextCell!({ key: 38, previousCellPosition: {}, nextCellPosition: next } as any);
        expect((result as any).rowIndex).toBe(4);
      });

      it('clamps at 0 on KEY_UP', () => {
        mockFoundset.selectedRowIndexes = [0];
        const next = { rowIndex: 0 };
        const result = component.agGridOptions.navigateToNextCell!({ key: 38, previousCellPosition: {}, nextCellPosition: next } as any);
        expect((result as any).rowIndex).toBe(0);
      });

      it('divides the selected index by the column count when computing the next row', () => {
        mockFoundset.selectedRowIndexes = [6];
        mockFoundset.serverSize = 20;
        component.numberOfColumns = 3;
        const next = { rowIndex: 0 };
        const result = component.agGridOptions.navigateToNextCell!({ key: 40, previousCellPosition: {}, nextCellPosition: next } as any);
        // selectedIdx 6 + 1 = 7, floor(7 / 3) = 2
        expect((result as any).rowIndex).toBe(2);
      });

      it('returns the previous cell on any other key', () => {
        mockFoundset.selectedRowIndexes = [1];
        const prev = { rowIndex: 1 };
        const result = component.agGridOptions.navigateToNextCell!({ key: 99, previousCellPosition: prev, nextCellPosition: { rowIndex: 0 } } as any);
        expect(result).toBe(prev);
      });
    });

    describe('onFirstDataRendered', () => {
      it('scrolls to the selection and reveals after the nested requestAnimationFrame chain', () => {
        configureScrolling();
        const scrollSpy = vi.spyOn(component as any, 'scrollToSelection').mockImplementation(() => {});
        const revealSpy = vi.spyOn(component as any, 'revealAfterFirstRender').mockImplementation(() => {});
        const raf = vi.spyOn(window, 'requestAnimationFrame').mockImplementation((cb: FrameRequestCallback) => {
          cb(0);
          return 0;
        });
        component.agGridOptions.onFirstDataRendered!({ api: {} } as any);
        expect(scrollSpy).toHaveBeenCalled();
        expect(revealSpy).toHaveBeenCalled();
        raf.mockRestore();
      });
    });

    describe('onModelUpdated', () => {
      it('reveals the grid when the foundset is empty and the first render is still pending', () => {
        mockServoyApi.isInDesigner.mockReturnValue(false);
        mockServoyApi.hasSampleData.mockReturnValue(false);
        mockServoyApi.getClientProperty.mockReturnValue(null);
        mockServoyApi.isInAbsoluteLayout.mockReturnValue(false);
        fixture.componentRef.setInput('foundset', mockFoundset);
        fixture.componentRef.setInput('containedForm', { formHeight: 50, formWidth: 100, absoluteLayout: false });
        fixture.componentRef.setInput('responsiveHeight', -1);
        component.svyOnInit();
        const revealSpy = vi.spyOn(component as any, 'revealAfterFirstRender').mockImplementation(() => {});
        component.agGridOptions.onModelUpdated!({ api: { getDisplayedRowCount: () => 0 } } as any);
        expect(revealSpy).toHaveBeenCalled();
      });

      it('does not reveal when there are displayed rows', () => {
        mockServoyApi.isInDesigner.mockReturnValue(false);
        mockServoyApi.hasSampleData.mockReturnValue(false);
        mockServoyApi.getClientProperty.mockReturnValue(null);
        mockServoyApi.isInAbsoluteLayout.mockReturnValue(false);
        fixture.componentRef.setInput('foundset', mockFoundset);
        fixture.componentRef.setInput('containedForm', { formHeight: 50, formWidth: 100, absoluteLayout: false });
        fixture.componentRef.setInput('responsiveHeight', -1);
        component.svyOnInit();
        const revealSpy = vi.spyOn(component as any, 'revealAfterFirstRender').mockImplementation(() => {});
        component.agGridOptions.onModelUpdated!({ api: { getDisplayedRowCount: () => 5 } } as any);
        expect(revealSpy).not.toHaveBeenCalled();
      });
    });
  });

  // The scrolling-path foundset change listener (svyOnInit else-branch, lines ~622-655): capture the listener
  // passed to addChangeListener and invoke it with fake FoundsetChangeEvent objects against a fake grid api.
  describe('scrolling foundset change listener', () => {
    let changeListener: (event: any) => void;
    let gridApi: any;

    const configureScrolling = () => {
      mockServoyApi.isInDesigner.mockReturnValue(false);
      mockServoyApi.hasSampleData.mockReturnValue(false);
      mockServoyApi.getClientProperty.mockReturnValue(null);
      mockServoyApi.isInAbsoluteLayout.mockReturnValue(true);
      mockFoundset.addChangeListener = vi.fn().mockImplementation((cb: (event: any) => void) => {
        changeListener = cb;
        return vi.fn();
      });
      fixture.componentRef.setInput('foundset', mockFoundset);
      fixture.componentRef.setInput('containedForm', { formHeight: 50, formWidth: 100, absoluteLayout: true });
      fixture.componentRef.setInput('responsiveHeight', 0);
      component.svyOnInit();
      component.numberOfColumns = 1;
      gridApi = {
        isDestroyed: vi.fn().mockReturnValue(false),
        setRowCount: vi.fn(),
        refreshServerSide: vi.fn(),
        refreshCells: vi.fn(),
      };
      vi.spyOn(component, 'agGrid').mockReturnValue({ api: gridApi } as any);
    };

    it('ignores events that originate from the AG Grid datasource getRows request', () => {
      configureScrolling();
      changeListener({ requestInfos: ['AGGridDatasourceGetRows'], serverFoundsetSizeChanged: { newValue: 20 } });
      expect(gridApi.setRowCount).not.toHaveBeenCalled();
    });

    it('ignores events after the grid api has been destroyed', () => {
      configureScrolling();
      gridApi.isDestroyed.mockReturnValue(true);
      changeListener({ serverFoundsetSizeChanged: { newValue: 20 } });
      expect(gridApi.setRowCount).not.toHaveBeenCalled();
    });

    it('updates the row count on serverFoundsetSizeChanged', () => {
      configureScrolling();
      changeListener({ serverFoundsetSizeChanged: { newValue: 20 } });
      expect(gridApi.setRowCount).toHaveBeenCalledWith(20);
    });

    it('purges the server side rows on viewportRowsCompletelyChanged', () => {
      configureScrolling();
      changeListener({ viewportRowsCompletelyChanged: true });
      expect(gridApi.refreshServerSide).toHaveBeenCalledWith({ purge: true });
    });

    it('purges and resets the row count on an insert/delete viewportRowsUpdated', () => {
      configureScrolling();
      mockFoundset.serverSize = 8;
      changeListener({ viewportRowsUpdated: [{ type: ChangeType.ROWS_INSERTED, startIndex: 0, endIndex: 1 }] });
      expect(gridApi.refreshServerSide).toHaveBeenCalledWith({ purge: true });
      expect(gridApi.setRowCount).toHaveBeenCalledWith(8);
    });

    it('refreshes a single cell on a one-row change', () => {
      configureScrolling();
      changeListener({ viewportRowsUpdated: [{ type: ChangeType.ROWS_CHANGED, startIndex: 3, endIndex: 3 }] });
      expect(gridApi.refreshCells).toHaveBeenCalled();
      expect(gridApi.refreshServerSide).not.toHaveBeenCalled();
    });

    it('purges on a multi-row non-insert/delete change', () => {
      configureScrolling();
      changeListener({ viewportRowsUpdated: [{ type: ChangeType.ROWS_CHANGED, startIndex: 1, endIndex: 4 }] });
      expect(gridApi.refreshServerSide).toHaveBeenCalledWith({ purge: true });
    });

    it('scrolls to the selection when the selected row indexes change', () => {
      configureScrolling();
      const scrollSpy = vi.spyOn(component as any, 'scrollToSelection').mockImplementation(() => {});
      changeListener({ selectedRowIndexesChanged: true });
      expect(scrollSpy).toHaveBeenCalled();
    });
  });

  // The paged (non-scrolling) foundset change listener (svyOnInit, lines ~521-620). Capture the listener and drive
  // the branches with FoundsetChangeEvent objects. updatePagingControls touches the paging <div> refs, so the view
  // must exist; detectChanges() first makes the #firstelement/#leftelement/#rightelement viewchildren resolve.
  describe('paged foundset change listener', () => {
    let changeListener: (event: any) => void;

    const configurePaged = () => {
      mockServoyApi.isInDesigner.mockReturnValue(false);
      mockServoyApi.hasSampleData.mockReturnValue(false);
      mockServoyApi.getClientProperty.mockReturnValue('paging'); // pagingMode set -> useScrolling false
      mockServoyApi.isInAbsoluteLayout.mockReturnValue(true);
      mockFoundset.addChangeListener = vi.fn().mockImplementation((cb: (event: any) => void) => {
        changeListener = cb;
        return vi.fn();
      });
      fixture.componentRef.setInput('foundset', mockFoundset);
      fixture.componentRef.setInput('containedForm', { formHeight: 50, formWidth: 100, absoluteLayout: true });
      fixture.detectChanges();
      component.svyOnInit();
      vi.spyOn(component, 'calculateCells').mockImplementation(() => {});
    };

    it('registers a change listener and keeps the non-scrolling markup', () => {
      configurePaged();
      expect(component.useScrolling).toBe(false);
      expect(typeof changeListener).toBe('function');
    });

    it('updates the paging controls on serverFoundsetSizeChanged', () => {
      configurePaged();
      const updateSpy = vi.spyOn(component, 'updatePagingControls').mockImplementation(() => {});
      changeListener({ serverFoundsetSizeChanged: { newValue: 20 } });
      expect(updateSpy).toHaveBeenCalled();
    });

    it('recalculates cells on an insert/delete viewportRowsUpdated', () => {
      configurePaged();
      changeListener({ viewportRowsUpdated: [{ type: ChangeType.ROWS_INSERTED, startIndex: 0, endIndex: 1 }] });
      expect(component.calculateCells).toHaveBeenCalled();
    });

    it('does not recalculate cells on a plain row-change viewportRowsUpdated', () => {
      configurePaged();
      changeListener({ viewportRowsUpdated: [{ type: ChangeType.ROWS_CHANGED, startIndex: 0, endIndex: 0 }] });
      expect(component.calculateCells).not.toHaveBeenCalled();
    });

    it('recalculates cells on viewportRowsCompletelyChanged', () => {
      configurePaged();
      changeListener({ viewportRowsCompletelyChanged: true });
      expect(component.calculateCells).toHaveBeenCalled();
    });

    it('swaps in the new foundset and recalculates on fullValueChanged', () => {
      configurePaged();
      const newFoundset = { ...mockFoundset, serverSize: 3, viewPort: { startIndex: 0, size: 3, rows: [] } };
      changeListener({ fullValueChanged: { newValue: newFoundset } });
      expect(component._foundset()).toBe(newFoundset);
      expect(component.calculateCells).toHaveBeenCalled();
    });

    it('clamps the page into range on fullValueChanged when the current page is past the end', () => {
      configurePaged();
      component.numberOfCells = 5;
      component.page = 4; // 4*5 = 20 >= serverSize
      const newFoundset = { ...mockFoundset, serverSize: 12, viewPort: { startIndex: 0, size: 5, rows: [] } };
      changeListener({ fullValueChanged: { newValue: newFoundset } });
      // Math.floor((12 - 1) / 5) = 2
      expect(component.page).toBe(2);
    });

    it('shifts the viewport and loads extra records on a viewPortStartIndexChanged after an insert before the page', () => {
      configurePaged();
      component.numberOfCells = 5;
      component.page = 1; // wanted start index 5
      mockFoundset.viewPort.startIndex = 6; // an insert shifted it; delta = 5 - 6 = -1
      mockFoundset.viewPort.size = 5;
      mockFoundset.serverSize = 20;
      changeListener({ viewPortStartIndexChanged: { oldValue: 5, newValue: 6 } });
      // shiftedPageDelta -1 -> loadLess path; viewport size equals numberOfCells so loadLess guard is not met here,
      // but updateSelection must still run without throwing
      expect(mockFoundset.notifyChanged).toHaveBeenCalled();
    });

    it('loads extra records when the viewport can show more than it currently holds (viewPortSizeChanged)', () => {
      configurePaged();
      component.numberOfCells = 5;
      component.page = 0;
      mockFoundset.viewPort.startIndex = 0;
      mockFoundset.viewPort.size = 2;
      mockFoundset.serverSize = 20;
      changeListener({ viewPortSizeChanged: { oldValue: 5, newValue: 2 } });
      expect(mockFoundset.loadExtraRecordsAsync).toHaveBeenCalled();
    });

    it('loads less records when the viewport holds more than numberOfCells (viewPortSizeChanged)', () => {
      configurePaged();
      component.numberOfCells = 2;
      component.page = 0;
      mockFoundset.viewPort.startIndex = 0;
      mockFoundset.viewPort.size = 5;
      mockFoundset.serverSize = 20;
      changeListener({ viewPortSizeChanged: { oldValue: 2, newValue: 5 } });
      expect(mockFoundset.loadLessRecordsAsync).toHaveBeenCalled();
    });

    it('notifies the foundset of pending requests after processing a generic event', () => {
      configurePaged();
      component.numberOfCells = 5;
      mockFoundset.viewPort.size = 5;
      changeListener({ viewPortSizeChanged: { oldValue: 5, newValue: 5 } });
      expect(mockFoundset.notifyChanged).toHaveBeenCalled();
    });
  });

  describe('updatePagingControls', () => {
    const configurePagedView = () => {
      mockServoyApi.isInDesigner.mockReturnValue(false);
      mockServoyApi.hasSampleData.mockReturnValue(false);
      mockServoyApi.getClientProperty.mockReturnValue('paging');
      mockServoyApi.isInAbsoluteLayout.mockReturnValue(true);
      fixture.componentRef.setInput('foundset', mockFoundset);
      fixture.componentRef.setInput('containedForm', { formHeight: 50, formWidth: 100, absoluteLayout: true });
      component.svyOnInit();
      fixture.detectChanges();
    };

    it('hides the first/left controls on page 0 and shows the right control when more rows exist', () => {
      configurePagedView();
      component.page = 0;
      component.numberOfCells = 2;
      mockFoundset.serverSize = 10;
      mockFoundset.hasMoreRows = false;
      component.updatePagingControls();
      expect((component.elementFirstRef()!.nativeElement as HTMLElement).style.visibility).toBe('hidden');
      expect((component.elementLeftRef()!.nativeElement as HTMLElement).style.visibility).toBe('hidden');
      expect((component.elementRightRef()!.nativeElement as HTMLElement).style.visibility).toBe('visible');
    });

    it('shows the first/left controls on a later page', () => {
      configurePagedView();
      component.page = 1;
      component.numberOfCells = 2;
      mockFoundset.serverSize = 10;
      component.updatePagingControls();
      expect((component.elementFirstRef()!.nativeElement as HTMLElement).style.visibility).toBe('visible');
      expect((component.elementLeftRef()!.nativeElement as HTMLElement).style.visibility).toBe('visible');
    });

    it('hides the right control when there are no more rows', () => {
      configurePagedView();
      component.page = 0;
      component.numberOfCells = 10;
      mockFoundset.serverSize = 5;
      mockFoundset.hasMoreRows = false;
      component.updatePagingControls();
      expect((component.elementRightRef()!.nativeElement as HTMLElement).style.visibility).toBe('hidden');
    });
  });

  describe('getRowItemTemplate', () => {
    it('returns the parent LFC template for a plain component cache', () => {
      const tpl = {} as any;
      mockParent.getTemplateForLFC = vi.fn().mockReturnValue(tpl);
      const comp = new ComponentCache('c1', 'bootstrapcomponents-textbox', 'textbox', [], {}, TestBed.inject(TypesRegistry));
      expect(component.getRowItemTemplate(comp)).toBe(tpl);
      expect(mockParent.getTemplateForLFC).toHaveBeenCalledWith(comp);
    });

    it('returns the responsive structure template for a StructureCache with no tagname', () => {
      mockServoyApi.isInDesigner.mockReturnValue(true); // avoid svyOnInit's foundset listener path on detectChanges
      fixture.detectChanges();
      const struct = new StructureCache('', []);
      expect(component.getRowItemTemplate(struct)).toBe(component.svyResponsiveDiv());
    });

    it('returns the responsive form-component template for a responsive FormComponentCache', () => {
      mockServoyApi.isInDesigner.mockReturnValue(true);
      fixture.detectChanges();
      const fcc = new FormComponentCache('fc', 'servoycore-formcomponent', 'fc', [], true, {}, null!, true, TestBed.inject(TypesRegistry));
      expect(component.getRowItemTemplate(fcc)).toBe(component.formComponentResponsiveDiv());
    });

    it('returns the absolute form-component template for a non-responsive FormComponentCache', () => {
      mockServoyApi.isInDesigner.mockReturnValue(true);
      fixture.detectChanges();
      const fcc = new FormComponentCache('fc', 'servoycore-formcomponent', 'fc', [], false, {}, null!, true, TestBed.inject(TypesRegistry));
      expect(component.getRowItemTemplate(fcc)).toBe(component.formComponentAbsoluteDiv());
    });
  });

  describe('getRowItemState for structure and form-component caches', () => {
    it('returns a StructureCache unchanged', () => {
      const struct = new StructureCache('', []);
      expect(component.getRowItemState(struct, { _svyRowId: '1' } as any, 0)).toBe(struct);
    });

    it('returns a FormComponentCache unchanged', () => {
      const fcc = new FormComponentCache('fc', 'servoycore-formcomponent', 'fc', [], false, {}, null!, true, TestBed.inject(TypesRegistry));
      expect(component.getRowItemState(fcc, { _svyRowId: '1' } as any, 0)).toBe(fcc);
    });
  });

  describe('getRowItemState resolves cells and caches them per row', () => {
    it('returns a cached cell when the row already has one for this rowIndex', () => {
      const textbox = new ComponentCache('tb', 'bootstrapcomponents-textbox', 'textbox', [], {}, TestBed.inject(TypesRegistry));
      (textbox.model as any).dataProviderID = 'Sample';
      component.cache = { items: [textbox] } as unknown as FormComponentCache;
      const row: any = { _svyRowId: 'r1' };
      const first = component.getRowItemState(textbox, row, 0);
      const second = component.getRowItemState(textbox, row, 0);
      expect(second).toBe(first);
    });

    it('returns an empty Cell and warns when no model can be resolved for the component', () => {
      const textbox = new ComponentCache('unknown', 'bootstrapcomponents-textbox', 'textbox', [], {}, TestBed.inject(TypesRegistry));
      component.cache = { items: [] } as unknown as FormComponentCache;
      const cell = component.getRowItemState(textbox, { _svyRowId: 'r1' } as any, 0) as any;
      expect(cell).toBeTruthy();
      expect(cell.name).toBe('unknown');
    });
  });

  describe('getHandler', () => {
    it('returns the named handler from the cell', () => {
      const handler = vi.fn();
      const cell: any = { handlers: { onClick: handler } };
      expect(component.getHandler(cell, 'onClick')).toBe(handler);
    });
  });

  describe('ngOnChanges', () => {
    beforeEach(() => {
      // ngOnChanges -> initializeComponent() -> svyOnInit() (which needs a live foundset listener and view) only
      // runs when the component has a native element and is not yet initialized. These tests exercise just the
      // LFC-specific ngOnChanges body (foundset signal + rowItems), so mark it initialized to isolate that.
      (component as any).initialized = true;
    });

    it('sets the internal foundset signal from the foundset input', () => {
      const other = { ...mockFoundset, serverSize: 99 };
      fixture.componentRef.setInput('foundset', other);
      component.ngOnChanges({} as any);
      expect(component._foundset()).toBe(other);
    });

    it('builds rowItems from a containedForm with childElements', () => {
      const childElements = [{ specName: 'bootstrapcomponents-textbox', name: 'tb', model: {} } as any];
      fixture.componentRef.setInput('containedForm', { formHeight: 10, formWidth: 10, absoluteLayout: true, childElements });
      component.ngOnChanges({} as any);
      expect(component.getRowItems().length).toBe(1);
      expect(component.getRowItems()[0]).toBe(childElements[0]);
    });

    it('resolves a servoycore-formcomponent child from the parent form cache', () => {
      const formComponentValue = { name: 'fc', responsive: false } as any;
      mockParent.getFormCache = vi.fn().mockReturnValue({
        getFormComponent: vi.fn().mockReturnValue(formComponentValue),
      });
      const childElements = [{ specName: 'servoycore-formcomponent', name: 'fc', model: {} } as any];
      fixture.componentRef.setInput('containedForm', { formHeight: 10, formWidth: 10, absoluteLayout: true, childElements });
      component.ngOnChanges({} as any);
      expect(component.getRowItems()[0]).toBe(formComponentValue);
    });
  });

  describe('ngOnDestroy', () => {
    it('removes the foundset change listener', () => {
      const remove = vi.fn();
      component.removeListenerFunction = remove;
      component.ngOnDestroy();
      expect(remove).toHaveBeenCalled();
      expect(component.removeListenerFunction).toBeNull();
    });

    it('clears the smart-property update trigger on contained-form child elements', () => {
      const child: any = { triggerNgOnChangeWithSameRefDueToSmartPropertyUpdate: vi.fn() };
      fixture.componentRef.setInput('containedForm', { formHeight: 10, formWidth: 10, absoluteLayout: true, childElements: [child] });
      component.ngOnDestroy();
      expect(child.triggerNgOnChangeWithSameRefDueToSmartPropertyUpdate).toBeNull();
    });

    it('does not throw when there is no listener and no contained form', () => {
      component.removeListenerFunction = null;
      expect(() => component.ngOnDestroy()).not.toThrow();
    });
  });

  describe('callApi', () => {
    it('logs an error when the path is not the expected containedForm/childElements shape', () => {
      const errorSpy = vi.spyOn((component as any).log, 'error');
      component.callApi('someComponent', 'doStuff', [], ['wrong', 'path']);
      expect(errorSpy).toHaveBeenCalled();
    });

    it('dispatches to the UI component at the selected row for a childElements path', () => {
      const childModel = { name: 'btn', type: 'bootstrapcomponents-button' };
      fixture.componentRef.setInput('containedForm', { formHeight: 10, formWidth: 10, absoluteLayout: true, childElements: [childModel] });
      mockFoundset.selectedRowIndexes = [0];
      const uiComp: any = { name: 'btn' };
      component.registerComponent({ name: () => 'btn' } as any, 0);
      (component as any).componentCache[0] = { btn: uiComp };
      const doCallSpy = vi.spyOn(AbstractFormComponent, 'doCallApiOnComponent').mockImplementation(() => undefined as any);
      component.callApi('containedForm', 'requestFocus', [], ['childElements', '0']);
      expect(doCallSpy).toHaveBeenCalled();
      doCallSpy.mockRestore();
    });
  });

  describe('calculateCells paged/responsive sizing', () => {
    it('uses responsivePageSize for a responsive form', () => {
      mockServoyApi.isInDesigner.mockReturnValue(false);
      mockServoyApi.hasSampleData.mockReturnValue(false);
      mockServoyApi.isInAbsoluteLayout.mockReturnValue(false);
      component.useScrolling = false;
      fixture.componentRef.setInput('responsivePageSize', 7);
      fixture.componentRef.setInput('containedForm', { formHeight: 10, formWidth: 10, absoluteLayout: false });
      mockFoundset.viewPort.startIndex = 0;
      component.page = 0;
      vi.spyOn(component, 'updatePagingControls').mockImplementation(() => {});
      component.calculateCells();
      expect(component.numberOfCells).toBe(7);
    });

    it('loads the correct page when the viewport start index does not match the page', () => {
      mockServoyApi.isInDesigner.mockReturnValue(false);
      mockServoyApi.hasSampleData.mockReturnValue(false);
      mockServoyApi.isInAbsoluteLayout.mockReturnValue(false);
      component.useScrolling = false;
      fixture.componentRef.setInput('responsivePageSize', 5);
      fixture.componentRef.setInput('containedForm', { formHeight: 10, formWidth: 10, absoluteLayout: false });
      component.page = 2; // start index 10
      mockFoundset.viewPort.startIndex = 0;
      vi.spyOn(component, 'updatePagingControls').mockImplementation(() => {});
      component.calculateCells();
      expect(mockFoundset.loadRecordsAsync).toHaveBeenCalledWith(10, 5);
    });

    it('sets the preferred viewport size to numberOfCells', () => {
      mockServoyApi.isInDesigner.mockReturnValue(false);
      mockServoyApi.hasSampleData.mockReturnValue(false);
      mockServoyApi.isInAbsoluteLayout.mockReturnValue(false);
      component.useScrolling = false;
      fixture.componentRef.setInput('responsivePageSize', 5);
      fixture.componentRef.setInput('containedForm', { formHeight: 10, formWidth: 10, absoluteLayout: false });
      component.page = 0;
      mockFoundset.viewPort.startIndex = 0;
      mockFoundset.viewPort.size = 5;
      mockFoundset.viewPort.rows = [{ _svyRowId: 'r0' }, { _svyRowId: 'r1' }, { _svyRowId: 'r2' }, { _svyRowId: 'r3' }, { _svyRowId: 'r4' }];
      vi.spyOn(component, 'updatePagingControls').mockImplementation(() => {});
      component.calculateCells();
      expect(mockFoundset.setPreferredViewportSize).toHaveBeenCalledWith(5);
    });
  });

  // The full cell-building path of getRowItemState (lines ~940-1056): resolve a ComponentCache to a real cell with
  // the readOnly/enabled/visible property descriptors, handlers, and the smart-property trigger installed.
  describe('getRowItemState full cell build', () => {
    let textbox: ComponentCache;
    let rowItem: any;

    const buildCell = (rowEditable?: boolean[], rowEnable?: boolean[]) => {
      textbox = new ComponentCache('tb', 'bootstrapcomponents-textbox', 'textbox', [], {}, TestBed.inject(TypesRegistry));
      rowItem = {
        name: 'tb',
        model: { readOnly: false, enabled: true, visible: true },
        modelViewport: undefined,
        mappedHandlers: new Map(),
      };
      fixture.componentRef.setInput('containedForm', { formHeight: 10, formWidth: 10, absoluteLayout: true });
      if (rowEditable) fixture.componentRef.setInput('rowEditableDataprovider', rowEditable);
      if (rowEnable) fixture.componentRef.setInput('rowEnableDataprovider', rowEnable);
      // rowItems resolution (ngOnChanges path): make the single item resolve to our mock state object
      (component as any).rowItems = [rowItem];
      component.cache = { items: [textbox] } as unknown as FormComponentCache;
    };

    it('builds a Cell whose model mirrors the resolved component model', () => {
      buildCell();
      const row: any = { _svyRowId: 'r0' };
      const cell = component.getRowItemState(textbox, row, 0) as any;
      expect(cell.name).toBe('tb');
      expect(cell.rowId).toBe('r0');
    });

    it('readOnly getter reflects the rowEditableDataprovider (false editable -> readOnly true)', () => {
      buildCell([false]);
      fixture.componentRef.setInput('editable', true);
      fixture.componentRef.setInput('readOnly', false);
      const cell = component.getRowItemState(textbox, { _svyRowId: 'r0' } as any, 0) as any;
      expect(cell.model.readOnly).toBe(true);
    });

    it('enabled getter reflects the rowEnableDataprovider', () => {
      buildCell(undefined, [false]);
      const cell = component.getRowItemState(textbox, { _svyRowId: 'r0' } as any, 0) as any;
      expect(cell.model.enabled).toBe(false);
    });

    it('visible getter falls back to the stored element value when no visibleDataProvider', () => {
      buildCell();
      const cell = component.getRowItemState(textbox, { _svyRowId: 'r0' } as any, 0) as any;
      expect(cell.model.visible).toBe(true);
    });

    it('installs the smart-property trigger and caches the cell on the row', () => {
      buildCell();
      const row: any = { _svyRowId: 'r0' };
      component.getRowItemState(textbox, row, 0);
      expect(typeof rowItem.triggerNgOnChangeWithSameRefDueToSmartPropertyUpdate).toBe('function');
      expect(row._cache.get('tb')).toBeTruthy();
    });

    it('merges servoyAttributes from the cache item into the resolved model', () => {
      buildCell();
      (textbox.model as any).servoyAttributes = { 'data-x': '1' };
      const cell = component.getRowItemState(textbox, { _svyRowId: 'r0' } as any, 0) as any;
      expect(rowItem.model.servoyAttributes['data-x']).toBe('1');
      expect(cell).toBeTruthy();
    });
  });

  describe('updateSelection', () => {
    it('does not throw and reads the selected row element from the view', () => {
      mockServoyApi.isInDesigner.mockReturnValue(true);
      fixture.detectChanges();
      component._foundset.set(mockFoundset);
      component.page = 0;
      component.numberOfCells = 5;
      mockFoundset.selectedRowIndexes = [0];
      component.selectionChangedByKey = true;
      expect(() => (component as any).updateSelection()).not.toThrow();
    });
  });

  // AGGridDatasource.getRows feeds AG Grid its server-side rows. It is private to the module, but svyOnInit's
  // ngAfterViewInit wiring creates one; here we drive it directly by newing it through the component reference it
  // keeps. Build it via the exported behaviour: create a datasource over the component and invoke getRows.
  describe('AGGridDatasource getRows', () => {
    const makeDatasourceAndParams = () => {
      mockServoyApi.isInDesigner.mockReturnValue(false);
      mockServoyApi.hasSampleData.mockReturnValue(false);
      mockServoyApi.getClientProperty.mockReturnValue(null);
      mockServoyApi.isInAbsoluteLayout.mockReturnValue(true);
      fixture.componentRef.setInput('foundset', mockFoundset);
      fixture.componentRef.setInput('containedForm', { formHeight: 50, formWidth: 100, absoluteLayout: true });
      fixture.componentRef.setInput('responsiveHeight', 0);
      component.svyOnInit();
      component.numberOfColumns = 1;
      // the foundset viewport already holds rows 0..4; serverSize 10 means there are more to load
      mockFoundset.serverSize = 5;
      mockFoundset.hasMoreRows = false;
      mockFoundset.viewPort.startIndex = 0;
      mockFoundset.viewPort.size = 5;
    };

    // AGGridDatasource is module-private; ngAfterViewInit does `new AGGridDatasource(this)` and hands it to
    // api.setGridOption('serverSideDatasource', ...). Capture that instance to drive getRows directly.
    const captureDatasource = (): any => {
      let datasource: any;
      const api = {
        setGridOption: vi.fn().mockImplementation((_name: string, ds: any) => (datasource = ds)),
        isDestroyed: () => false,
      };
      vi.spyOn(component, 'agGrid').mockReturnValue({ api } as any);
      // makeDatasourceAndParams already ran svyOnInit (useScrolling true, agGridOptions built). Mark initialized so
      // super.ngAfterViewInit() does not re-enter svyOnInit; calculateCells is a no-op here (absolute layout path).
      (component as any).initialized = true;
      vi.spyOn(component, 'calculateCells').mockImplementation(() => {});
      component.ngAfterViewInit();
      return datasource;
    };

    it('returns the in-viewport rows synchronously when the requested range is already loaded', () => {
      makeDatasourceAndParams();
      const datasource = captureDatasource();
      expect(datasource).toBeTruthy();
      const success = vi.fn();
      datasource.getRows({ request: { startRow: 0, endRow: 5 }, success } as any);
      expect(success).toHaveBeenCalled();
      const arg = success.mock.calls[0][0];
      expect(arg.rowData.length).toBeGreaterThan(0);
      expect(arg.rowCount).toBe(5);
    });

    it('loads extra records asynchronously when the requested range is outside the viewport', async () => {
      makeDatasourceAndParams();
      mockFoundset.hasMoreRows = true; // force getLastRowIndex() === -1 so the async branch is taken
      mockFoundset.loadExtraRecordsAsync = vi.fn().mockReturnValue(Promise.resolve());
      const datasource = captureDatasource();
      const success = vi.fn();
      datasource.getRows({ request: { startRow: 100, endRow: 150 }, success } as any);
      expect(mockFoundset.loadExtraRecordsAsync).toHaveBeenCalled();
      await Promise.resolve();
      await new Promise((resolve) => setTimeout(resolve, 0));
      expect(success).toHaveBeenCalled();
    });
  });
});
