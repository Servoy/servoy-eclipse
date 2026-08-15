import { TestBed, ComponentFixture } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { Component } from '@angular/core';
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
        <servoydefault-tabpanel [servoyApi]="servoyApi" [name]="'testTabpanel'" [tabs]="tabs" [tabIndex]="1" [enabled]="true">
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
}

@Component({
    selector: 'test-tablesspanel-host',
    template: `
        <servoydefault-tablesspanel [servoyApi]="servoyApi" [name]="'testTablesspanel'" [tabs]="tabs" [tabIndex]="tabIndex">
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
    tabIndex = 1;
}

const defaultProviders = [
    { provide: ServoyPublicService, useValue: { generateUploadUrl: vi.fn(), showFileOpenDialog: vi.fn() } },
    { provide: FormattingService, useValue: {} },
    { provide: TooltipService, useValue: { isTooltipActive: new Subject<boolean>(), isTooltipActiveSignal: signal(false) } },
    { provide: WindowRefService, useValue: { nativeWindow: { event: null } } },
];

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
});
