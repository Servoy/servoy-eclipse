import { TestBed, ComponentFixture } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { Component } from '@angular/core';
import { Subject } from 'rxjs';
import { signal } from '@angular/core';
import { ServoyPublicService, FormattingService, TooltipService, ServoyApi, WindowRefService } from '@servoy/public';
import { ServoyDefaultAccordion } from './accordion';
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
    selector: 'test-accordion-host',
    template: `
        <servoydefault-accordion [servoyApi]="servoyApi" [name]="'testAccordion'" [tabs]="tabs" [tabIndex]="1" [enabled]="true">
            <ng-template let-name="name">
                <div class="form-placeholder">{{name}}</div>
            </ng-template>
        </servoydefault-accordion>
    `,
    standalone: true,
    imports: [ServoyDefaultAccordion],
})
class TestAccordionHostComponent {
    servoyApi = createMockServoyApi();
    tabs = createTabs();
}

describe('ServoyDefaultAccordion', () => {
    let fixture: ComponentFixture<TestAccordionHostComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [TestAccordionHostComponent],
            providers: [
                { provide: ServoyPublicService, useValue: { generateUploadUrl: vi.fn(), showFileOpenDialog: vi.fn() } },
                { provide: FormattingService, useValue: {} },
                { provide: TooltipService, useValue: { isTooltipActive: new Subject<boolean>(), isTooltipActiveSignal: signal(false) } },
                { provide: WindowRefService, useValue: { nativeWindow: { event: null } } },
            ],
        });
        fixture = TestBed.createComponent(TestAccordionHostComponent);
    });

    it('should create and render without template errors', () => {
        expect(() => fixture.detectChanges()).not.toThrow();
    });

    it('should have the svy-accordion class', () => {
        fixture.detectChanges();
        const accordion = fixture.nativeElement.querySelector('.svy-accordion');
        expect(accordion).toBeTruthy();
    });

    it('should render accordion items for each tab', () => {
        fixture.detectChanges();
        const items = fixture.nativeElement.querySelectorAll('[ngbaccordionitem]');
        expect(items.length).toBe(2);
    });
});
