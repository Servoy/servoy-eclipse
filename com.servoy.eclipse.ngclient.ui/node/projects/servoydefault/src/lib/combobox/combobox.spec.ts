import { TestBed, ComponentFixture } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { Component, provideZonelessChangeDetection, signal } from '@angular/core';
import { Subject } from 'rxjs';
import { ServoyPublicService, FormattingService, TooltipService, ServoyApi, WindowRefService, IValuelist } from '@servoy/public';
import { ServoyDefaultCombobox } from './combobox';

function createMockServoyApi(): ServoyApi {
    return {
        registerComponent: vi.fn(),
        unRegisterComponent: vi.fn(),
        getMarkupId: vi.fn().mockReturnValue('test-id'),
        trustAsHtml: vi.fn().mockReturnValue(false),
        startEdit: vi.fn(),
        apply: vi.fn(),
        callServerSideApi: vi.fn(),
        isInDesigner: vi.fn().mockReturnValue(false),
        isInAbsoluteLayout: vi.fn().mockReturnValue(false),
        getFormName: vi.fn().mockReturnValue('testForm'),
        getClientProperty: vi.fn(),
        formWillShow: vi.fn().mockResolvedValue(true),
        hideForm: vi.fn().mockResolvedValue(true),
    } as any;
}

function createValuelist(items: Array<{ displayValue: string; realValue: any }>): IValuelist {
    const list = [...items] as any;
    list.hasRealValues = () => true;
    list.isRealValueDate = () => false;
    list.find = (predicate: (item: any) => boolean) => items.find(predicate);
    list.getDisplayValue = vi.fn().mockReturnValue(new Subject());
    list.filterList = vi.fn();
    return list as IValuelist;
}

@Component({
    selector: 'test-combobox-host',
    template: `
        <servoydefault-combobox [servoyApi]="servoyApi" [name]="'testCombobox'" [dataProviderID]="dataProviderID()"
            [valuelistID]="valuelistID()" [findmode]="findmode()" [enabled]="true" [editable]="true">
        </servoydefault-combobox>
    `,
    standalone: true,
    imports: [ServoyDefaultCombobox],
})
class TestComboboxHostComponent {
    servoyApi = createMockServoyApi();
    dataProviderID = signal<any>(null);
    valuelistID = signal<any>(undefined);
    findmode = signal(false);
}

describe('ServoyDefaultCombobox', () => {
    let fixture: ComponentFixture<TestComboboxHostComponent>;
    let host: TestComboboxHostComponent;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [TestComboboxHostComponent],
            providers: [
                provideZonelessChangeDetection(),
                { provide: ServoyPublicService, useValue: { generateUploadUrl: vi.fn(), showFileOpenDialog: vi.fn(), getUIProperty: vi.fn() } },
                { provide: FormattingService, useValue: { format: (data: any) => data } },
                { provide: TooltipService, useValue: { isTooltipActive: new Subject<boolean>(), isTooltipActiveSignal: signal(false), showTooltip: vi.fn(), hideTooltip: vi.fn() } },
                { provide: WindowRefService, useValue: { nativeWindow: window } },
            ],
        });
        fixture = TestBed.createComponent(TestComboboxHostComponent);
        host = fixture.componentInstance;
    });

    function getComboComponent(): ServoyDefaultCombobox {
        return fixture.debugElement.children[0].componentInstance as ServoyDefaultCombobox;
    }

    it('should create and render without template errors', () => {
        expect(() => fixture.detectChanges()).not.toThrow();
    });

    describe('find mode with valuelist', () => {
        it('resolves formattedValue to the matching valuelist display value when findmode is true', () => {
            host.valuelistID.set(createValuelist([
                { displayValue: 'Chang', realValue: 2 },
                { displayValue: 'Other', realValue: 3 },
            ]));
            host.findmode.set(true);
            host.dataProviderID.set(2);
            fixture.detectChanges();

            const combo = getComboComponent();
            expect(combo.formattedValue).toBe('Chang');
        });

        it('updates formattedValue to the new matching display value when dataProviderID changes while in find mode', () => {
            host.valuelistID.set(createValuelist([
                { displayValue: 'Chang', realValue: 2 },
                { displayValue: 'Other', realValue: 3 },
            ]));
            host.findmode.set(true);
            host.dataProviderID.set(2);
            fixture.detectChanges();
            expect(getComboComponent().formattedValue).toBe('Chang');

            host.dataProviderID.set(3);
            fixture.detectChanges();
            expect(getComboComponent().formattedValue).toBe('Other');
        });
    });

    describe('find mode without a valuelist', () => {
        it('falls back to the raw dataProviderID value when there is no valuelist at all', () => {
            host.valuelistID.set(undefined);
            host.findmode.set(true);
            host.dataProviderID.set('searchExpr');
            fixture.detectChanges();

            const combo = getComboComponent();
            expect(combo.formattedValue).toBe('searchExpr');
        });
    });

    describe('normal mode (regression guard)', () => {
        it('still resolves formattedValue via the valuelist when not in find mode', () => {
            host.valuelistID.set(createValuelist([
                { displayValue: 'Chang', realValue: 2 },
                { displayValue: 'Other', realValue: 3 },
            ]));
            host.findmode.set(false);
            host.dataProviderID.set(2);
            fixture.detectChanges();

            const combo = getComboComponent();
            expect(combo.formattedValue).toBe('Chang');
        });

        it('falls back to the placeholder text when the resolved value is empty', () => {
            host.findmode.set(false);
            fixture.detectChanges();
            const combo = getComboComponent();
            (combo as any).placeholderText = () => 'pick one';
            combo.formattedValue = null;
            combo.svyOnChanges({ placeholderText: { currentValue: 'pick one', previousValue: undefined, firstChange: false, isFirstChange: () => false } } as any);
            expect(combo.formattedValue).toBe('pick one');
        });
    });

    describe('isPrintableChar', () => {
        it('returns true for a normal letter', () => {
            fixture.detectChanges();
            expect(getComboComponent().isPrintableChar('a')).toBe(true);
        });

        it('returns false for a non printable key', () => {
            fixture.detectChanges();
            expect(getComboComponent().isPrintableChar('Shift')).toBe(false);
        });
    });

    describe('updateValue', () => {
        it('sets the dataProviderID, emits and calls onActionMethodID', () => {
            const action = vi.fn();
            fixture.detectChanges();
            const combo = getComboComponent();
            (combo as any).onActionMethodID = () => action;
            const emitSpy = vi.spyOn(combo.dataProviderIDChange, 'emit');
            const evt = new Event('click');
            combo.updateValue(42, evt);
            expect(combo.dataProviderID()).toBe(42);
            expect(emitSpy).toHaveBeenCalledWith(42);
            expect(action).toHaveBeenCalledWith(evt);
        });
    });

    describe('highlight helpers', () => {
        it('splits the display value around the last select value when the popup is open', () => {
            fixture.detectChanges();
            const combo = getComboComponent();
            combo.openState = true;
            combo.lastSelectValue = 'ha';
            expect(combo.getRemainingValueBefore('Chang')).toBe('C');
            expect(combo.getStrongValue('Chang')).toBe('ha');
            expect(combo.getRemainingValueAfter('Chang')).toBe('ng');
        });

        it('returns neutral values when the popup is closed', () => {
            fixture.detectChanges();
            const combo = getComboComponent();
            combo.openState = false;
            combo.lastSelectValue = 'ha';
            expect(combo.getRemainingValueBefore('Chang')).toBe('');
            expect(combo.getStrongValue('Chang')).toBe('');
            expect(combo.getRemainingValueAfter('Chang')).toBe('Chang');
        });
    });

    describe('getDropDownWidth', () => {
        it('returns the input client width', () => {
            fixture.detectChanges();
            const combo = getComboComponent();
            expect(combo.getDropDownWidth()).toBe(combo.getFocusElement().clientWidth);
        });
    });
});
