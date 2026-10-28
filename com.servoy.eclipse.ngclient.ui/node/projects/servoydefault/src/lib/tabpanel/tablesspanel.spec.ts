import { TestBed, ComponentFixture } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { Component, provideZonelessChangeDetection, provideCheckNoChangesConfig } from '@angular/core';
import { Subject } from 'rxjs';
import { signal } from '@angular/core';
import { ServoyPublicService, FormattingService, TooltipService, ServoyApi, WindowRefService } from '@servoy/public';
import { ServoyDefaultTabpanel } from './tabpanel';
import { ServoyDefaultTablesspanel } from './tablesspanel';
import { Tab } from './basetabpanel';

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

function createTabs(): Tab[] {
    return [
        { _id: 'tab0', name: 'tab1', containsFormId: 'form1', text: 'Tab 1', relationName: '', foreground: '', disabled: false, imageMediaID: '', mnemonic: '', toolTipText: '', getStateHolder: () => ({}), getWatchedProperties: () => [] } as any,
        { _id: 'tab1', name: 'tab2', containsFormId: 'form2', text: 'Tab 2', relationName: '', foreground: '', disabled: false, imageMediaID: '', mnemonic: '', toolTipText: '', getStateHolder: () => ({}), getWatchedProperties: () => [] } as any,
    ];
}

@Component({
    selector: 'test-tabpanel-host',
    template: `
        <servoydefault-tabpanel [servoyApi]="servoyApi" [name]="'testTabpanel'" [tabs]="tabs" [tabIndex]="tabIndex()"
            (tabIndexChange)="onTabIndexChange($event)" [enabled]="true">
            <ng-template let-name="name">
                <div class="form-placeholder">{{name}}</div>
            </ng-template>
        </servoydefault-tabpanel>
    `,
    standalone: true,
    imports: [ServoyDefaultTabpanel],
})
class TestTabpanelHostComponent {
    servoyApi = createMockServoyApi();
    tabs = createTabs();
    readonly tabIndex = signal<any>(1);
    emissions: any[] = [];

    /** mirrors the generated template's callback.datachange(state,'tabIndex',$event) write-back */
    onTabIndexChange(value: any) {
        this.emissions.push(value);
        this.tabIndex.set(value);
    }
}

@Component({
    selector: 'test-tablesspanel-host',
    template: `
        <servoydefault-tablesspanel [servoyApi]="servoyApi" [name]="'testTablesspanel'" [tabs]="tabs" [tabIndex]="tabIndex()"
            (tabIndexChange)="onTabIndexChange($event)">
            <ng-template let-name="name">
                <div class="form-placeholder">{{name}}</div>
            </ng-template>
        </servoydefault-tablesspanel>
    `,
    standalone: true,
    imports: [ServoyDefaultTablesspanel],
})
class TestTablesspanelHostComponent {
    servoyApi = createMockServoyApi();
    tabs = createTabs();
    readonly tabIndex = signal<any>(1);
    emissions: any[] = [];

    /** mirrors the generated template's callback.datachange(state,'tabIndex',$event) write-back */
    onTabIndexChange(value: any) {
        this.emissions.push(value);
        this.tabIndex.set(value);
    }
}

const defaultProviders = [
    provideZonelessChangeDetection(),
    provideCheckNoChangesConfig({ exhaustive: true }),
    { provide: ServoyPublicService, useValue: { generateUploadUrl: vi.fn(), showFileOpenDialog: vi.fn(), getFormCacheByName: vi.fn().mockReturnValue(null) } },
    { provide: FormattingService, useValue: {} },
    { provide: TooltipService, useValue: { isTooltipActive: new Subject<boolean>(), isTooltipActiveSignal: signal(false) } },
    { provide: WindowRefService, useValue: { nativeWindow: { event: null, console } } },
];

/** setFormVisible() defers the tabIndex write with setTimeout, which whenStable() does not await */
const flushDeferredWrites = async (fixture: ComponentFixture<unknown>) => {
    await fixture.whenStable();
    await new Promise(resolve => setTimeout(resolve, 0));
    await fixture.whenStable();
};

describe('ServoyDefaultTabpanel', () => {
    let fixture: ComponentFixture<TestTabpanelHostComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [TestTabpanelHostComponent],
            providers: defaultProviders,
        });
        fixture = TestBed.createComponent(TestTabpanelHostComponent);
    });

    it('should create and render without template errors', () => {
        expect(() => fixture.detectChanges()).not.toThrow();
    });

    it('should render tab navigation items', () => {
        fixture.detectChanges();
        const nav = fixture.nativeElement.querySelector('[ngbnav]');
        expect(nav).toBeTruthy();
    });

    it('should have the svy-tabpanel class', () => {
        fixture.detectChanges();
        const panel = fixture.nativeElement.querySelector('.svy-tabpanel');
        expect(panel).toBeTruthy();
    });
});

describe('ServoyDefaultTablesspanel', () => {
    let fixture: ComponentFixture<TestTablesspanelHostComponent>;
    let component: ServoyDefaultTablesspanel;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [TestTablesspanelHostComponent],
            providers: defaultProviders,
        });
        fixture = TestBed.createComponent(TestTablesspanelHostComponent);
        fixture.detectChanges();
        component = fixture.debugElement.children[0].componentInstance;
    });

    it('should create and render without template errors', () => {
        expect(component).toBeTruthy();
    });

    it('should have the svy-tablesspanel class', () => {
        const panel = fixture.nativeElement.querySelector('.svy-tablesspanel');
        expect(panel).toBeTruthy();
    });

    it('should select first tab by default when tabIndex is 1', () => {
        expect(component.getSelectedTab()).toBeTruthy();
        expect(component.getSelectedTab().containsFormId).toBe('form1');
    });

    it('should return form name from getForm()', () => {
        expect(component.getForm()).toBe('form1');
    });

    it('should return correct tab index', () => {
        const tab = component.getSelectedTab();
        expect(component.getTabIndex(tab)).toBe(1);
    });

    it('should switch tabs via select()', async () => {
        const tabs = fixture.componentInstance.tabs;
        component.select(tabs[1]);
        await fixture.whenStable();
        expect(component.getSelectedTab().containsFormId).toBe('form2');
    });

    it('should not throw NG0100 when selectedTab changes between checks', async () => {
        expect(component.getForm()).toBe('form1');
        const tabs = fixture.componentInstance.tabs;
        component.select(tabs[1]);
        await fixture.whenStable();
        fixture.detectChanges();
        expect(component.getForm()).toBe('form2');
        expect(() => fixture.detectChanges()).not.toThrow();
    });

    it('getForm() should return consistent value on repeated calls', () => {
        const result1 = component.getForm();
        const result2 = component.getForm();
        expect(result1).toBe(result2);
    });

    describe('tabIndex writes outside change detection', () => {

        it('should not write tabIndex during the pass when a tab name is set after init', async () => {
            const host = fixture.componentInstance;
            await flushDeferredWrites(fixture);
            host.emissions.length = 0;

            host.tabIndex.set('tab2');
            expect(() => fixture.detectChanges()).not.toThrow();
            expect(host.emissions).toEqual([]);
            await flushDeferredWrites(fixture);
            expect(host.emissions).toEqual([2]);
            expect(host.tabIndex()).toBe(2);
        });

        it('should stay stable while tabIndex is driven through several values', async () => {
            const host = fixture.componentInstance;
            await flushDeferredWrites(fixture);

            for (const value of [2, 'tab1', 1, 'tab2']) {
                host.emissions.length = 0;
                host.tabIndex.set(value);
                expect(() => fixture.detectChanges()).not.toThrow();
                expect(host.emissions).toEqual([]);
                await flushDeferredWrites(fixture);
            }
            expect(host.tabIndex()).toBe(2);
        });
    });
});
