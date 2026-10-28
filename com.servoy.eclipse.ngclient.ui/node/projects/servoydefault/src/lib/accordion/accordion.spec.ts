import { TestBed, ComponentFixture } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { Component, provideZonelessChangeDetection, provideCheckNoChangesConfig } from '@angular/core';
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
        <servoydefault-accordion [servoyApi]="servoyApi" [name]="'testAccordion'" [tabs]="tabs" [tabIndex]="tabIndex()"
            (tabIndexChange)="onTabIndexChange($event)" [enabled]="true">
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
    readonly tabIndex = signal<any>(1);
    emissions: any[] = [];

    /** mirrors the generated template's callback.datachange(state,'tabIndex',$event) write-back */
    onTabIndexChange(value: any) {
        this.emissions.push(value);
        this.tabIndex.set(value);
    }
}

/** setFormVisible() defers the tabIndex write with setTimeout, which whenStable() does not await */
const flushDeferredWrites = async (fixture: ComponentFixture<unknown>) => {
    await fixture.whenStable();
    await new Promise(resolve => setTimeout(resolve, 0));
    await fixture.whenStable();
};

describe('ServoyDefaultAccordion', () => {
    let fixture: ComponentFixture<TestAccordionHostComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [TestAccordionHostComponent],
            providers: [
                provideZonelessChangeDetection(),
                provideCheckNoChangesConfig({ exhaustive: true }),
                { provide: ServoyPublicService, useValue: { generateUploadUrl: vi.fn(), showFileOpenDialog: vi.fn() } },
                { provide: FormattingService, useValue: {} },
                { provide: TooltipService, useValue: { isTooltipActive: new Subject<boolean>(), isTooltipActiveSignal: signal(false) } },
                { provide: WindowRefService, useValue: { nativeWindow: { event: null, console } } },
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

    describe('tabIndex writes outside change detection', () => {

        it('should not write tabIndex during the first render pass when tabIndex is a tab name', async () => {
            const host = fixture.componentInstance;
            host.tabIndex.set('tab2');
            expect(() => fixture.detectChanges()).not.toThrow();
            expect(host.emissions).toEqual([]);
            await flushDeferredWrites(fixture);
            expect(host.emissions).toEqual([2]);
            expect(host.tabIndex()).toBe(2);
        });

        it('should not write tabIndex during the pass when a tab name is set after init', async () => {
            const host = fixture.componentInstance;
            fixture.detectChanges();
            await flushDeferredWrites(fixture);
            host.emissions.length = 0;

            host.tabIndex.set('tab2');
            expect(() => fixture.detectChanges()).not.toThrow();
            expect(host.emissions).toEqual([]);
            await flushDeferredWrites(fixture);
            expect(host.emissions).toEqual([2]);
            expect(host.tabIndex()).toBe(2);
        });

        it('should not write tabIndex during the pass when a numeric tabIndex is set after init', async () => {
            const host = fixture.componentInstance;
            fixture.detectChanges();
            await flushDeferredWrites(fixture);
            host.emissions.length = 0;

            host.tabIndex.set(2);
            expect(() => fixture.detectChanges()).not.toThrow();
            expect(host.emissions).toEqual([]);
            await flushDeferredWrites(fixture);
            // the resolved selection already matches the requested index, so the model never changes value
            expect(host.emissions).toEqual([]);
            expect(host.tabIndex()).toBe(2);
        });

        it('should stay stable while tabIndex is driven through several values', async () => {
            const host = fixture.componentInstance;
            fixture.detectChanges();
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
