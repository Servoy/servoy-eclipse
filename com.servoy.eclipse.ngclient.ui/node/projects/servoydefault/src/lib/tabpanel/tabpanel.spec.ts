import { TestBed, ComponentFixture } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { Component, provideZonelessChangeDetection, provideCheckNoChangesConfig, signal } from '@angular/core';
import { Subject } from 'rxjs';
import { ServoyPublicService, FormattingService, TooltipService, ServoyApi, WindowRefService } from '@servoy/public';
import { DefaultTabpanelActiveTabVisibilityListener, ServoyDefaultTabpanel } from './tabpanel';
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
    selector: 'test-tabpanel-signal-host',
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
class TestTabpanelSignalHostComponent {
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

describe('DefaultTabpanelActiveTabVisibilityListener', () => {
    let fixture: ComponentFixture<DefaultTabpanelActiveTabVisibilityListener>;
    let component: DefaultTabpanelActiveTabVisibilityListener;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [DefaultTabpanelActiveTabVisibilityListener],
        });
        fixture = TestBed.createComponent(DefaultTabpanelActiveTabVisibilityListener);
        component = fixture.componentInstance;
    });

    it('should create and render without template errors', () => {
        expect(() => fixture.detectChanges()).not.toThrow();
        expect(component).toBeTruthy();
    });

    it('should have elementRef signal available after view init', () => {
        fixture.detectChanges();
        expect(component.elementRef()).toBeTruthy();
        expect(component.elementRef()!.nativeElement).toBeTruthy();
    });

    it('should set up MutationObserver on ngAfterViewInit', () => {
        fixture.detectChanges();
        expect(component.observer).toBeTruthy();
    });

    it('should disconnect observer on destroy', () => {
        fixture.detectChanges();
        const disconnectSpy = vi.spyOn(component.observer, 'disconnect');
        component.ngOnDestroy();
        expect(disconnectSpy).toHaveBeenCalled();
    });
});

describe('ServoyDefaultTabpanel tabIndex writes outside change detection', () => {
    let fixture: ComponentFixture<TestTabpanelSignalHostComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [TestTabpanelSignalHostComponent],
            providers: [
                provideZonelessChangeDetection(),
                provideCheckNoChangesConfig({ exhaustive: true }),
                { provide: ServoyPublicService, useValue: { generateUploadUrl: vi.fn(), showFileOpenDialog: vi.fn(), getFormCacheByName: vi.fn().mockReturnValue(null) } },
                { provide: FormattingService, useValue: {} },
                { provide: TooltipService, useValue: { isTooltipActive: new Subject<boolean>(), isTooltipActiveSignal: signal(false) } },
                { provide: WindowRefService, useValue: { nativeWindow: { event: null, console } } },
            ],
        });
        fixture = TestBed.createComponent(TestTabpanelSignalHostComponent);
    });

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
} );

@Component({
    selector: 'test-tabpanel-border-host',
    template: `
        <servoydefault-tabpanel [servoyApi]="servoyApi" [name]="'testTabpanel'" [tabs]="tabs" [tabIndex]="1"
            [enabled]="true" [borderType]="borderType()">
            <ng-template let-name="name">
                <div class="form-placeholder">{{name}}</div>
            </ng-template>
        </servoydefault-tabpanel>
    `,
    standalone: true,
    imports: [ServoyDefaultTabpanel],
})
class TestTabpanelBorderHostComponent {
    servoyApi = createMockServoyApi();
    tabs = createTabs();
    readonly borderType = signal<any>(undefined);
}

// SVY-21792: borderType must be applied (through PropertyUtils.setBorder) on the outer .svy-tabpanel div
describe('ServoyDefaultTabpanel borderType (SVY-21792)', () => {
    const matte = { type: 'MatteBorder', borderStyle: { borderWidth: '1px 2px 3px 4px ', borderStyle: 'solid', borderColor: 'red' } };
    const line = { type: 'LineBorder', borderStyle: { borderWidth: '2px', borderStyle: 'solid', borderColor: 'blue' } };
    const sides = ['Top', 'Right', 'Bottom', 'Left'];

    let fixture: ComponentFixture<TestTabpanelBorderHostComponent>;

    const outer = () => fixture.nativeElement.querySelector('.svy-tabpanel') as HTMLElement;
    const setBorder = async (value: any) => {
        fixture.componentInstance.borderType.set(value);
        fixture.detectChanges();
        await fixture.whenStable();
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [TestTabpanelBorderHostComponent],
            providers: [
                provideZonelessChangeDetection(),
                provideCheckNoChangesConfig({ exhaustive: true }),
                { provide: ServoyPublicService, useValue: { generateUploadUrl: vi.fn(), showFileOpenDialog: vi.fn(), getFormCacheByName: vi.fn().mockReturnValue(null) } },
                { provide: FormattingService, useValue: {} },
                { provide: TooltipService, useValue: { isTooltipActive: new Subject<boolean>(), isTooltipActiveSignal: signal(false) } },
                { provide: WindowRefService, useValue: { nativeWindow: { event: null, console } } },
            ],
        });
        fixture = TestBed.createComponent(TestTabpanelBorderHostComponent);
    });

    it('should apply a Matte border with a width per side', async () => {
        fixture.componentInstance.borderType.set(matte);
        fixture.detectChanges();
        await fixture.whenStable();

        const style = outer().style;
        expect(style.borderTopWidth).toBe('1px');
        expect(style.borderRightWidth).toBe('2px');
        expect(style.borderBottomWidth).toBe('3px');
        expect(style.borderLeftWidth).toBe('4px');
        sides.forEach(s => {
            expect((style as any)['border' + s + 'Style']).toBe('solid');
            expect((style as any)['border' + s + 'Color']).toBe('red');
        });
        expect(getComputedStyle(outer()).borderTopWidth).toBe('1px');
        expect(getComputedStyle(outer()).borderLeftWidth).toBe('4px');
    });

    it('should apply a Line border on all sides', async () => {
        fixture.componentInstance.borderType.set(line);
        fixture.detectChanges();
        await fixture.whenStable();

        const style = outer().style;
        sides.forEach(s => {
            expect((style as any)['border' + s + 'Width']).toBe('2px');
            expect((style as any)['border' + s + 'Style']).toBe('solid');
            expect((style as any)['border' + s + 'Color']).toBe('blue');
        });
    });

    [null, undefined].forEach(empty => {
        it('should not draw a border when borderType is ' + empty, async () => {
            fixture.componentInstance.borderType.set(empty);
            fixture.detectChanges();
            await fixture.whenStable();

            sides.forEach(s => expect((outer().style as any)['border' + s + 'Width']).toBe(''));
        });
    });

    it('should remove a previously applied border when borderType changes to null', async () => {
        fixture.componentInstance.borderType.set(line);
        fixture.detectChanges();
        await fixture.whenStable();
        expect(outer().style.borderTopWidth).toBe('2px');

        await setBorder(null);

        sides.forEach(s => {
            expect((outer().style as any)['border' + s + 'Width']).toBe('');
            expect((outer().style as any)['border' + s + 'Style']).toBe('');
        });
    });

    it('should update the widths when borderType changes from Matte to Line', async () => {
        fixture.componentInstance.borderType.set(matte);
        fixture.detectChanges();
        await fixture.whenStable();
        expect(outer().style.borderLeftWidth).toBe('4px');

        await setBorder(line);

        sides.forEach(s => {
            expect((outer().style as any)['border' + s + 'Width']).toBe('2px');
            expect((outer().style as any)['border' + s + 'Color']).toBe('blue');
        });
    });

    it('should not create a fieldset or move children for a TitledBorder and clear an earlier border', async () => {
        fixture.componentInstance.borderType.set(line);
        fixture.detectChanges();
        await fixture.whenStable();
        const before = Array.from(outer().childNodes);
        expect(outer().style.borderTopWidth).toBe('2px');

        await setBorder({ type: 'TitledBorder', title: 'a title', color: 'red', titleJustification: 'left' });

        expect(outer().querySelector('fieldset')).toBeNull();
        expect(Array.from(outer().childNodes)).toEqual(before);
        expect(outer().style.borderTopWidth).toBe('');
    });

    it('should put the border on the outer element, not on the tab strip or the content outlet', async () => {
        fixture.componentInstance.borderType.set(matte);
        fixture.detectChanges();
        await fixture.whenStable();

        const ul = outer().querySelector('ul') as HTMLElement;
        const outlet = outer().lastElementChild as HTMLElement;
        expect(ul.style.borderTopWidth).toBe('');
        expect(outlet.style.borderTopWidth).toBe('');
        expect(outer().style.borderTopWidth).toBe('1px');
    });
});