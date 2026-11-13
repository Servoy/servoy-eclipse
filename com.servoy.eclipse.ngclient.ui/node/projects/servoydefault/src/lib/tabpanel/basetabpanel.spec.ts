import { TestBed, ComponentFixture } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { Component, SimpleChange, SimpleChanges, provideZonelessChangeDetection, ChangeDetectionStrategy } from '@angular/core';
import { Subject } from 'rxjs';
import { ServoyPublicService, FormattingService, TooltipService, ServoyApi, WindowRefService } from '@servoy/public';
import { BaseTabpanel, Tab } from './basetabpanel';

function createMockServoyApi(): ServoyApi {
    return {
        registerComponent: vi.fn(),
        unRegisterComponent: vi.fn(),
        getMarkupId: vi.fn().mockReturnValue('mk'),
        trustAsHtml: vi.fn().mockReturnValue(false),
        startEdit: vi.fn(),
        apply: vi.fn(),
        callServerSideApi: vi.fn(),
        isInDesigner: vi.fn().mockReturnValue(false),
        isInAbsoluteLayout: vi.fn().mockReturnValue(false),
        getFormName: vi.fn().mockReturnValue('testForm'),
        getClientProperty: vi.fn(),
        formWillShow: vi.fn().mockReturnValue({ then: (cb: any) => cb(true), finally: (cb: any) => cb() }),
        hideForm: vi.fn().mockResolvedValue(true),
    } as any;
}

function createTabs(): Tab[] {
    return [
        { _id: '', name: 'tab1', containsFormId: 'form1', text: 'Tab 1', relationName: '', foreground: '', disabled: false, imageMediaID: '', mnemonic: '', toolTipText: '', getStateHolder: () => ({}), getWatchedProperties: () => [] } as any,
        { _id: '', name: 'tab2', containsFormId: 'form2', text: 'Tab 2', relationName: '', foreground: '', disabled: false, imageMediaID: '', mnemonic: '', toolTipText: '', getStateHolder: () => ({}), getWatchedProperties: () => [] } as any,
    ];
}

@Component({
    selector: 'servoydefault-test-basetabpanel',
    template: `<div #element></div>`,
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
})
class TestBaseTabpanelComponent extends BaseTabpanel {
    // expose protected members for testing
    publicGetRealTabIndex() { return (this as any).getRealTabIndex(); }
    publicGetSelectedFormName() { return (this as any).getSelectedFormName(); }
    publicSetFormVisible(tab: Tab, event: any, callShow: boolean) { return (this as any).setFormVisible(tab, event, callShow); }
    selectedTabValue() { return (this as any).selectedTab(); }
}

function change(current: any, previous: any = undefined, first = false): SimpleChange {
    return new SimpleChange(previous, current, first);
}

describe('BaseTabpanel', () => {
    let fixture: ComponentFixture<TestBaseTabpanelComponent>;
    let component: TestBaseTabpanelComponent;
    let getFormCacheByName: any;

    beforeEach(() => {
        getFormCacheByName = vi.fn().mockReturnValue(null);
        TestBed.configureTestingModule({
            imports: [TestBaseTabpanelComponent],
            providers: [
                provideZonelessChangeDetection(),
                { provide: ServoyPublicService, useValue: { getFormCacheByName } },
                { provide: FormattingService, useValue: {} },
                { provide: TooltipService, useValue: { isTooltipActive: new Subject<boolean>(), isTooltipActiveSignal: vi.fn() } },
                { provide: WindowRefService, useValue: { nativeWindow: { event: null } } },
            ],
        });
        fixture = TestBed.createComponent(TestBaseTabpanelComponent);
        component = fixture.componentInstance;
        fixture.componentRef.setInput('servoyApi', createMockServoyApi());
        fixture.componentRef.setInput('name', 'testTabpanel');
        fixture.componentRef.setInput('tabs', createTabs());
        fixture.detectChanges();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('creates and assigns ids to every tab on init', () => {
        expect(component).toBeTruthy();
        expect(component.tabs()[0]._id).toBe('mk_tab_0');
        expect(component.tabs()[1]._id).toBe('mk_tab_1');
    });

    describe('getTabIndex', () => {
        it('returns the 1-based index of a tab', () => {
            expect(component.getTabIndex(component.tabs()[0])).toBe(1);
            expect(component.getTabIndex(component.tabs()[1])).toBe(2);
        });

        it('returns -1 for a tab that is not part of the panel', () => {
            expect(component.getTabIndex({ name: 'ghost' } as any)).toBe(-1);
        });
    });

    describe('getRealTabIndex', () => {
        it('resolves a numeric tabIndex to a 0-based index', () => {
            fixture.componentRef.setInput('tabIndex', 2);
            fixture.detectChanges();
            expect(component.publicGetRealTabIndex()).toBe(1);
        });

        it('resolves a tab name to its index', () => {
            fixture.componentRef.setInput('tabIndex', 'tab2');
            fixture.detectChanges();
            expect(component.publicGetRealTabIndex()).toBe(1);
        });

        it('returns -1 for an unknown tab name', () => {
            fixture.componentRef.setInput('tabIndex', 'missing');
            fixture.detectChanges();
            expect(component.publicGetRealTabIndex()).toBe(-1);
        });

        it('defaults to the first tab when tabIndex is not set', () => {
            expect(component.publicGetRealTabIndex()).toBe(0);
        });
    });

    describe('select / setFormVisible', () => {
        it('select on a valid first tab makes its form visible', () => {
            const tab = component.tabs()[0];
            component.select(tab);
            expect(component.getSelectedTab()).toBe(tab);
            expect(component.servoyApi().formWillShow).toHaveBeenCalledWith('form1', '', 0);
        });

        it('select leaves the current selection unchanged for a tab that is not part of the panel', () => {
            const before = component.getSelectedTab();
            component.select({ name: 'ghost' } as any);
            expect(component.getSelectedTab()).toBe(before);
        });

        it('select does nothing when the same tab is already selected', () => {
            const tab = component.tabs()[0];
            component.select(tab);
            const hideSpy = component.servoyApi().hideForm as any;
            hideSpy.mockClear();
            component.select(tab);
            expect(hideSpy).not.toHaveBeenCalled();
        });

        it('setFormVisible selects the tab and updates the selectedTabID', () => {
            const tab = component.tabs()[1];
            component.publicSetFormVisible(tab, null, true);
            expect(component.getSelectedTab()).toBe(tab);
            expect(component.selectedTabID()).toBe(tab._id);
        });
    });

    describe('getForm', () => {
        it('returns the selected form id when no tab argument is given', () => {
            component.publicSetFormVisible(component.tabs()[0], null, true);
            expect(component.getForm()).toBe('form1');
        });

        it('returns the form id when the argument tab matches the selected one', () => {
            const tab = component.tabs()[0];
            component.publicSetFormVisible(tab, null, true);
            expect(component.getForm(tab)).toBe('form1');
        });

        it('returns null when the argument tab does not match the selected one', () => {
            component.publicSetFormVisible(component.tabs()[0], null, true);
            expect(component.getForm(component.tabs()[1])).toBeNull();
        });
    });

    describe('getSelectedTabId', () => {
        it('returns the selected tab id when one is selected', () => {
            component.publicSetFormVisible(component.tabs()[1], null, true);
            expect(component.getSelectedTabId()).toBe(component.tabs()[1]._id);
        });

        it('falls back to the first tab id when nothing is selected', () => {
            expect(component.getSelectedTabId()).toBe(component.tabs()[0]._id);
        });
    });

    describe('getSelectedFormName', () => {
        it('returns the selected form id', () => {
            component.publicSetFormVisible(component.tabs()[1], null, true);
            expect(component.publicGetSelectedFormName()).toBe('form2');
        });
    });

    describe('borderStyle getter', () => {
        it('returns an empty string when no border type is set', () => {
            expect(component.borderStyle).toBe('');
        });

        it('composes the border shorthand from the border type', () => {
            fixture.componentRef.setInput('borderType', { borderStyle: { borderWidth: '1px', borderStyle: 'solid', borderColor: 'red' } });
            fixture.detectChanges();
            expect(component.borderStyle).toBe('1px solid red');
        });
    });

    describe('applyOverflowFromForm', () => {
        it('applies overflow-x and overflow-y from the form cache layout', () => {
            component.publicSetFormVisible(component.tabs()[0], null, true);
            getFormCacheByName.mockReturnValue({ getBodyPartLayout: () => ({ 'overflow-x': 'scroll', 'overflow-y': 'hidden' }) });
            const containerStyle: any = { overflow: 'auto' };
            component.applyOverflowFromForm(containerStyle);
            expect(containerStyle.overflowX).toBe('scroll');
            expect(containerStyle.overflowY).toBe('hidden');
            expect(containerStyle.overflow).toBeUndefined();
        });

        it('removes overflow axes when the layout provides none', () => {
            component.publicSetFormVisible(component.tabs()[0], null, true);
            getFormCacheByName.mockReturnValue({ getBodyPartLayout: () => ({}) });
            const containerStyle: any = { overflowX: 'scroll', overflowY: 'scroll' };
            component.applyOverflowFromForm(containerStyle);
            expect(containerStyle.overflowX).toBeUndefined();
            expect(containerStyle.overflowY).toBeUndefined();
        });
    });

    describe('svyOnChanges', () => {
        it('reselects the current tab when the tabs input changes and a tab is selected', () => {
            component.publicSetFormVisible(component.tabs()[0], null, true);
            const selectSpy = vi.spyOn(component, 'select');
            component.svyOnChanges({ tabs: change(component.tabs()) } as SimpleChanges);
            expect(selectSpy).toHaveBeenCalled();
        });

        it('selects the resolved tab when tabIndex changes', () => {
            fixture.componentRef.setInput('tabIndex', 2);
            fixture.detectChanges();
            const selectSpy = vi.spyOn(component, 'select');
            component.svyOnChanges({ tabIndex: change(2) } as SimpleChanges);
            expect(selectSpy).toHaveBeenCalledWith(component.tabs()[1]);
        });
    });
});
