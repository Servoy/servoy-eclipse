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

    // The methods below drive the live NgbDropdown / NgbTooltip / menu-item queries, which are
    // signal viewChild/viewChildren fields. We replace those signals on the instance with light
    // fakes so the behaviour can be exercised in jsdom without the real popup lifecycle.
    const makeDropdown = (open = false) => {
        const state = { open };
        return {
            isOpen: vi.fn(() => state.open),
            open: vi.fn(() => {
                state.open = true;
            }),
            close: vi.fn(() => {
                state.open = false;
            }),
        };
    };
    const makeTooltip = (open = false) => {
        const state = { open, autoClose: true as any, ngbTooltip: undefined as any };
        return {
            isOpen: vi.fn(() => state.open),
            open: vi.fn(() => {
                state.open = true;
            }),
            close: vi.fn(() => {
                state.open = false;
            }),
            get autoClose() {
                return state.autoClose;
            },
            set autoClose(v: any) {
                state.autoClose = v;
            },
            get ngbTooltip() {
                return state.ngbTooltip;
            },
            set ngbTooltip(v: any) {
                state.ngbTooltip = v;
            },
        };
    };
    // Build a fake NgbDropdownItem-like object wrapping a real DOM button so classList works.
    // focus is replaced with a spy (jsdom's prototype focus is otherwise a no-op to assert on),
    // and innerText is forced via a defined property because jsdom does not reflect the
    // layout-based innerText setter onto a detached element's getter the way the browser does.
    const makeMenuItem = (text: string, active = false): any => {
        const el = document.createElement('button');
        Object.defineProperty(el, 'innerText', { value: text, configurable: true });
        if (active) el.classList.add('active');
        const focus = vi.fn();
        el.focus = focus;
        return { nativeElement: el, focus };
    };
    const stub = (combo: ServoyDefaultCombobox, overrides: Record<string, any>) => {
        for (const [name, value] of Object.entries(overrides)) {
            (combo as any)[name] = () => value;
        }
    };

    describe('svyOnInit', () => {
        it('reads Combobox.showPopupOnFocusGain from the client property and disables tooltip auto-close', () => {
            fixture.detectChanges();
            const combo = getComboComponent();
            const tooltip = makeTooltip();
            stub(combo, { tooltip });
            (combo.servoyApi().getClientProperty as any).mockReturnValue(true);
            const superInit = vi.spyOn(Object.getPrototypeOf(Object.getPrototypeOf(combo)) as any, 'svyOnInit').mockImplementation(() => undefined);

            combo.svyOnInit();

            expect((combo as any).showPopupOnFocusGain).toBe(true);
            expect(tooltip.autoClose).toBe(false);
            superInit.mockRestore();
        });

        it('falls back to the UI property when the client property is not set', () => {
            fixture.detectChanges();
            const combo = getComboComponent();
            const tooltip = makeTooltip();
            stub(combo, { tooltip });
            (combo.servoyApi().getClientProperty as any).mockReturnValue(null);
            const uiSpy = vi.spyOn((combo as any).servoyService, 'getUIProperty').mockReturnValue(true);
            const superInit = vi.spyOn(Object.getPrototypeOf(Object.getPrototypeOf(combo)) as any, 'svyOnInit').mockImplementation(() => undefined);

            combo.svyOnInit();

            expect(uiSpy).toHaveBeenCalledWith('Combobox.showPopupOnFocusGain');
            expect((combo as any).showPopupOnFocusGain).toBe(true);
            superInit.mockRestore();
        });
    });

    describe('tooltip helpers', () => {
        it('refreshTooltip opens the tooltip only when it is closed', () => {
            fixture.detectChanges();
            const combo = getComboComponent();
            const tooltip = makeTooltip(false);
            stub(combo, { tooltip });
            combo.refreshTooltip();
            expect(tooltip.open).toHaveBeenCalledTimes(1);

            tooltip.open.mockClear();
            (tooltip.isOpen as any).mockReturnValue(true);
            combo.refreshTooltip();
            expect(tooltip.open).not.toHaveBeenCalled();
        });

        it('handleTooltip toggles the tooltip and changes its text', () => {
            fixture.detectChanges();
            const combo = getComboComponent();
            const tooltip = makeTooltip(false);
            stub(combo, { tooltip });

            combo.handleTooltip({} as KeyboardEvent);
            expect(tooltip.ngbTooltip).toBe('This is the CHANGED text');
            expect(tooltip.open).toHaveBeenCalledTimes(1);

            combo.handleTooltip({} as KeyboardEvent);
            expect(tooltip.close).toHaveBeenCalledTimes(1);
        });
    });

    describe('handleKeyDown', () => {
        it('opens the dropdown and builds the search value for a printable key, then refreshes the tooltip', () => {
            fixture.detectChanges();
            const combo = getComboComponent();
            const dropdown = makeDropdown(false);
            const tooltip = makeTooltip(false);
            stub(combo, { comboboxDropdown: dropdown, tooltip, menuItems: [] });
            vi.spyOn(combo as any, 'getFocusElement').mockReturnValue(document.activeElement as HTMLElement);

            combo.handleKeyDown({ key: 'c', stopPropagation: vi.fn() } as any);

            expect(dropdown.open).toHaveBeenCalled();
            expect(combo.keyboardSelectValue).toBe('c');
            expect(combo.lastSelectValue).toBe('c');
            expect(tooltip.open).toHaveBeenCalled();
        });

        it('removes the last char on Backspace and closes the tooltip when the search becomes empty', () => {
            fixture.detectChanges();
            const combo = getComboComponent();
            const dropdown = makeDropdown(true);
            const tooltip = makeTooltip(true);
            stub(combo, { comboboxDropdown: dropdown, tooltip, menuItems: [] });
            combo.keyboardSelectValue = 'c';

            combo.handleKeyDown({ key: 'Backspace', stopPropagation: vi.fn() } as any);

            // Backspace empties the search; the now-empty value triggers closeTooltip(),
            // which resets keyboardSelectValue to null and closes the tooltip.
            expect(combo.keyboardSelectValue).toBeNull();
            expect(tooltip.close).toHaveBeenCalled();
        });

        it('stops propagation for ArrowUp/ArrowDown and treats them as non printable', () => {
            fixture.detectChanges();
            const combo = getComboComponent();
            const dropdown = makeDropdown(true);
            const tooltip = makeTooltip(false);
            stub(combo, { comboboxDropdown: dropdown, tooltip, menuItems: [] });
            const stopPropagation = vi.fn();

            combo.handleKeyDown({ key: 'ArrowDown', stopPropagation } as any);

            expect(stopPropagation).toHaveBeenCalled();
            expect(dropdown.open).not.toHaveBeenCalled();
        });
    });

    describe('scrollToFirstMatchingItem', () => {
        it('focuses the first menu item whose text contains the search value', () => {
            fixture.detectChanges();
            const combo = getComboComponent();
            const first = makeMenuItem('Alpha');
            const match = makeMenuItem('Change');
            stub(combo, { menuItems: [first, match] });
            combo.openState = true;
            combo.lastSelectValue = 'ge'; // only matches 'Change', not 'Alpha'
            combo.firstItemFound = false;

            combo.scrollToFirstMatchingItem();

            expect(combo.firstItemFound).toBe(true);
            expect(match.focus).toHaveBeenCalled();
            expect(first.focus).not.toHaveBeenCalled();
        });

        it('does nothing when the popup is closed', () => {
            fixture.detectChanges();
            const combo = getComboComponent();
            const match = makeMenuItem('Chang');
            stub(combo, { menuItems: [match] });
            combo.openState = false;
            combo.lastSelectValue = 'ha';

            combo.scrollToFirstMatchingItem();

            expect(match.focus).not.toHaveBeenCalled();
        });
    });

    describe('openChange', () => {
        it('on open focuses the active menu item (via the scheduled task)', () => {
            vi.useFakeTimers();
            try {
                fixture.detectChanges();
                const combo = getComboComponent();
                const active = makeMenuItem('Chang', true);
                stub(combo, { menuItems: [makeMenuItem('Alpha'), active] });

                combo.openChange(true);
                expect(combo.openState).toBe(true);
                vi.runAllTimers();

                expect(active.focus).toHaveBeenCalled();
            } finally {
                vi.useRealTimers();
            }
        });

        it('on close dispatches a blur on the toggle button and closes the tooltip', () => {
            fixture.detectChanges();
            const combo = getComboComponent();
            const tooltip = makeTooltip(true);
            stub(combo, { tooltip });

            const btn = document.createElement('button');
            const dispatchSpy = vi.spyOn(btn, 'dispatchEvent');
            vi.spyOn(combo as any, 'elementRef').mockReturnValue({ nativeElement: { firstElementChild: btn } });

            combo.openChange(false);

            expect(combo.openState).toBe(false);
            expect(tooltip.close).toHaveBeenCalled();
            expect(dispatchSpy).toHaveBeenCalled();
        });
    });

    describe('attachFocusListeners / requestFocus', () => {
        it('opens the dropdown on focus gain when showPopupOnFocusGain is set', () => {
            fixture.detectChanges();
            const combo = getComboComponent();
            const dropdown = makeDropdown(false);
            stub(combo, { comboboxDropdown: dropdown, onFocusGainedMethodID: undefined, onFocusLostMethodID: undefined });
            (combo as any).showPopupOnFocusGain = true;

            // capture the registered focus handler instead of relying on a real focus event
            let focusHandler: (e: Event) => void = () => undefined;
            vi.spyOn((combo as any).renderer, 'listen').mockImplementation(((_el: any, type: string, cb: any) => {
                if (type === 'focus') focusHandler = cb;
                return () => undefined;
            }) as any);

            combo.attachFocusListeners(document.createElement('input'));
            focusHandler(new Event('focus'));

            expect(dropdown.open).toHaveBeenCalled();
        });

        it('requestFocus opens the dropdown when showPopupOnFocusGain is set', () => {
            fixture.detectChanges();
            const combo = getComboComponent();
            const dropdown = makeDropdown(false);
            stub(combo, { comboboxDropdown: dropdown });
            (combo as any).showPopupOnFocusGain = true;
            vi.spyOn(Object.getPrototypeOf(Object.getPrototypeOf(combo)) as any, 'requestFocus').mockImplementation(() => undefined);

            combo.requestFocus(true);

            expect(dropdown.open).toHaveBeenCalled();
        });
    });
});
