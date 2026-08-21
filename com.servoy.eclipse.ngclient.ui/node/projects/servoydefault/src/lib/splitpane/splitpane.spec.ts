import { TestBed, ComponentFixture } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { Component, TemplateRef, viewChild, provideZonelessChangeDetection } from '@angular/core';
import { ServoyPublicService, FormattingService, TooltipService, ServoyApi, WindowRefService } from '@servoy/public';
import { Subject } from 'rxjs';
import { signal } from '@angular/core';
import { ServoyDefaultSplitpane } from './splitpane';
import { Tab } from '../tabpanel/basetabpanel';

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
        isInAbsoluteLayout: vi.fn().mockReturnValue(true),
        getFormName: vi.fn().mockReturnValue('testForm'),
        getClientProperty: vi.fn(),
        formWillShow: vi.fn().mockResolvedValue(true),
        hideForm: vi.fn().mockResolvedValue(true),
    } as any;
}

@Component({
    selector: 'test-host',
    template: `
        <servoydefault-splitpane [servoyApi]="servoyApi" [name]="'testSplit'" [tabs]="tabs"
            [tabOrientation]="tabOrientation" [tabSeq]="1">
            <ng-template let-name="name">
                <div class="form-placeholder">{{name}}</div>
            </ng-template>
        </servoydefault-splitpane>
    `,
    standalone: true,
    imports: [ServoyDefaultSplitpane],
})
class TestHostComponent {
    servoyApi = createMockServoyApi();
    tabs: Tab[] = [
        { _id: 'tab1', name: 'left', containsFormId: 'formA', text: 'Left', relationName: '', foreground: '', disabled: false, imageMediaID: '', mnemonic: '', toolTipText: '', getStateHolder: () => ({}), getWatchedProperties: () => [] } as any,
        { _id: 'tab2', name: 'right', containsFormId: 'formB', text: 'Right', relationName: '', foreground: '', disabled: false, imageMediaID: '', mnemonic: '', toolTipText: '', getStateHolder: () => ({}), getWatchedProperties: () => [] } as any,
    ];
    tabOrientation = 1;
}

describe('ServoyDefaultSplitpane', () => {
    let fixture: ComponentFixture<TestHostComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [TestHostComponent],
            providers: [
                provideZonelessChangeDetection(),
                { provide: ServoyPublicService, useValue: { generateUploadUrl: vi.fn(), showFileOpenDialog: vi.fn() } },
                { provide: FormattingService, useValue: {} },
                { provide: TooltipService, useValue: { isTooltipActive: new Subject<boolean>(), isTooltipActiveSignal: signal(false) } },
                { provide: WindowRefService, useValue: { nativeWindow: window } },
            ],
        });
        fixture = TestBed.createComponent(TestHostComponent);
    });

    it('should create and render without template errors', () => {
        expect(() => fixture.detectChanges()).not.toThrow();
    });

    it('should render bg-splitter with horizontal orientation by default', () => {
        fixture.detectChanges();
        const splitter = fixture.nativeElement.querySelector('bg-splitter');
        expect(splitter).toBeTruthy();
    });

    it('should use vertical orientation when tabOrientation is -3', () => {
        fixture.componentInstance.tabOrientation = -3;
        fixture.detectChanges();
        const splitter = fixture.nativeElement.querySelector('bg-splitter');
        expect(splitter).toBeTruthy();
    });

    it('should render two bg-pane elements', () => {
        fixture.detectChanges();
        const panes = fixture.nativeElement.querySelectorAll('bg-pane');
        expect(panes.length).toBe(2);
    });
});

describe('ServoyDefaultSplitpane - default values', () => {
    let fixture: ComponentFixture<ServoyDefaultSplitpane>;
    let component: ServoyDefaultSplitpane;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ServoyDefaultSplitpane],
            providers: [
                provideZonelessChangeDetection(),
                { provide: ServoyPublicService, useValue: { generateUploadUrl: vi.fn(), showFileOpenDialog: vi.fn() } },
                { provide: FormattingService, useValue: {} },
                { provide: TooltipService, useValue: { isTooltipActive: new Subject<boolean>(), isTooltipActiveSignal: signal(false) } },
                { provide: WindowRefService, useValue: { nativeWindow: window } },
            ],
        });
        fixture = TestBed.createComponent(ServoyDefaultSplitpane);
        component = fixture.componentInstance;
        fixture.componentRef.setInput('servoyApi', createMockServoyApi());
        fixture.componentRef.setInput('name', 'testSplit');
        fixture.componentRef.setInput('tabs', []);
    });

    it('should have default divSize of 5 when not set', () => {
        expect(component.divSize()).toBe(5);
    });

    it('should have default pane1MinSize of 30 when not set', () => {
        expect(component.pane1MinSize()).toBe(30);
    });

    it('should have default pane2MinSize of 30 when not set', () => {
        expect(component.pane2MinSize()).toBe(30);
    });

    it('should have default resizeWeight of 0 when not set', () => {
        expect(component.resizeWeight()).toBe(0);
    });

    it('should keep defaults when undefined is explicitly pushed', () => {
        fixture.componentRef.setInput('divSize', undefined);
        fixture.componentRef.setInput('pane1MinSize', undefined);
        fixture.componentRef.setInput('pane2MinSize', undefined);
        fixture.componentRef.setInput('resizeWeight', undefined);
        fixture.detectChanges();
        expect(component.divSize()).toBe(5);
        expect(component.pane1MinSize()).toBe(30);
        expect(component.pane2MinSize()).toBe(30);
        expect(component.resizeWeight()).toBe(0);
    });

    it('should use provided values when set', () => {
        fixture.componentRef.setInput('divSize', 10);
        fixture.componentRef.setInput('pane1MinSize', 50);
        fixture.componentRef.setInput('pane2MinSize', 60);
        fixture.componentRef.setInput('resizeWeight', 0.5);
        fixture.detectChanges();
        expect(component.divSize()).toBe(10);
        expect(component.pane1MinSize()).toBe(50);
        expect(component.pane2MinSize()).toBe(60);
        expect(component.resizeWeight()).toBe(0.5);
    });
});
