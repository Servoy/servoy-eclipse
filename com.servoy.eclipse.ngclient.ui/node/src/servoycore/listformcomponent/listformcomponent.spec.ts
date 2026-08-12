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
                    responsive: false
                })
            }),
            getTemplate: vi.fn(),
            getTemplateForLFC: vi.fn(),
            isDesigner: vi.fn().mockReturnValue(false),
            getNGClass: vi.fn().mockReturnValue(null)
        };

        mockFoundset = {
            serverSize: 10,
            selectedRowIndexes: [0],
            hasMoreRows: false,
            viewPort: {
                startIndex: 0,
                size: 5,
                rows: [
                    { _svyRowId: 'row0' },
                    { _svyRowId: 'row1' },
                    { _svyRowId: 'row2' },
                    { _svyRowId: 'row3' },
                    { _svyRowId: 'row4' }
                ]
            },
            requestSelectionUpdate: vi.fn(),
            addChangeListener: vi.fn().mockReturnValue(vi.fn()),
            loadRecordsAsync: vi.fn().mockReturnValue(Promise.resolve()),
            loadExtraRecordsAsync: vi.fn().mockReturnValue(Promise.resolve()),
            loadLessRecordsAsync: vi.fn().mockReturnValue(Promise.resolve()),
            setPreferredViewportSize: vi.fn(),
            notifyChanged: vi.fn(),
            getRecordRefByRowID: vi.fn()
        };

        mockServoyApi = {
            getMarkupId: vi.fn().mockReturnValue('lfc1'),
            registerComponent: vi.fn(),
            unRegisterComponent: vi.fn(),
            isInDesigner: vi.fn().mockReturnValue(false),
            isInAbsoluteLayout: vi.fn().mockReturnValue(true),
            getFormName: vi.fn().mockReturnValue('testForm'),
            trustAsHtml: vi.fn(),
            getClientProperty: vi.fn().mockReturnValue('paging')
        };

        TestBed.configureTestingModule({
            imports: [ListFormComponent],
            providers: [
                { provide: AbstractFormComponent, useValue: mockParent },
                { provide: FormService, useValue: {} },
                { provide: ServoyService, useValue: { getUIProperties: () => ({ getUIProperty: vi.fn() }) } },
                { provide: TypesRegistry, useValue: { getComponentSpecification: vi.fn() } },
                { provide: ConverterService, useValue: {} },
                { provide: LoggerFactory, useValue: { getLogger: () => ({ error: vi.fn(), warn: vi.fn(), debug: vi.fn(), info: vi.fn() }) } }
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA]
        }).overrideComponent(ListFormComponent, {
            set: { imports: [], schemas: [CUSTOM_ELEMENTS_SCHEMA] }
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(ListFormComponent);
        component = fixture.componentInstance;
        component.servoyApi = mockServoyApi;
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
                paddingLeft: '5px', paddingRight: '10px', paddingTop: '3px', paddingBottom: '3px'
            });
            const style = component.getRowStyle(false);
            expect(style['margin-left']).toBe('5px');
            expect(style['margin-right']).toBe('10px');
            expect(style['margin-top']).toBe('3px');
            expect(style['margin-bottom']).toBe('3px');
        });
    });

    describe('registerComponent / unRegisterComponent', () => {
        it('should register a component at the given row index', () => {
            const mockComp = { name: 'btn1' } as any;
            component.registerComponent(mockComp, 0);
            expect((component as any).componentCache[0]['btn1']).toBe(mockComp);
        });

        it('should unregister a component', () => {
            const mockComp = { name: 'btn1' } as any;
            component.registerComponent(mockComp, 0);
            component.unRegisterComponent(mockComp, 0);
            expect((component as any).componentCache[0]).toBeUndefined();
        });
    });
});
