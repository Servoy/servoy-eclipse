import { TestBed, ComponentFixture } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { Component, TemplateRef, viewChild } from '@angular/core';
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
