import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA, Component } from '@angular/core';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { DialogWindowComponent } from './dialog-window.component';
import { SvyWindow } from '../window.service';
import { FormService } from '../../form.service';
import { SabloService } from '../../../sablo/sablo.service';
import { FormComponent } from '../../form/form_component.component';
import { DefaultNavigator } from '../../../servoycore/default-navigator/default-navigator';

/* Mock components to avoid pulling in full FormComponent/DefaultNavigator dependencies */
@Component({ selector: 'svy-form', template: '', standalone: true, inputs: ['name'] })
class MockFormComponent {}

@Component({ selector: 'svy-default-navigator', template: '', standalone: true, inputs: ['name'] })
class MockDefaultNavigator {}

describe('DialogWindowComponent', () => {
    let fixture: ComponentFixture<DialogWindowComponent>;
    let component: DialogWindowComponent;
    let mockWindow: SvyWindow;
    let mockFormService: any;

    beforeEach(async () => {
        mockFormService = {
            hasFormCacheEntry: vi.fn().mockReturnValue(true),
            destroy: vi.fn(),
        };

        await TestBed.configureTestingModule({
            imports: [DialogWindowComponent],
            providers: [
                { provide: FormService, useValue: mockFormService },
                { provide: SabloService, useValue: { callService: vi.fn() } },
            ],
            schemas: [NO_ERRORS_SCHEMA],
        }).overrideComponent(DialogWindowComponent, {
            remove: { imports: [FormComponent, DefaultNavigator] },
            add: { imports: [MockFormComponent, MockDefaultNavigator] },
        }).compileComponents();

        fixture = TestBed.createComponent(DialogWindowComponent);
        component = fixture.componentInstance;

        // Create a real SvyWindow with mock dependencies
        mockWindow = new SvyWindow('testDialog', 0, { localStorageService: {} } as any, {} as any);
        mockWindow.form.set({ name: 'myForm', size: { width: 600, height: 400 } });
        mockWindow.navigatorForm.set({ name: null, size: { width: 0 } });
        mockWindow.title.set('Initial Title');

        component.setWindow(mockWindow);
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should reactively update undecorated state in template', () => {
        // Initial: not undecorated → window class present, header visible
        const dialog = fixture.nativeElement.querySelector('.svy-dialog');
        const header = fixture.nativeElement.querySelector('.window-header');
        const footer = fixture.nativeElement.querySelector('.window-footer');
        expect(dialog.classList.contains('window')).toBe(true);
        expect(header.style.display).not.toBe('none');
        expect(footer.style.display).not.toBe('none');

        // Change to undecorated
        mockWindow.undecorated.set(true);
        fixture.detectChanges();
        expect(dialog.classList.contains('window')).toBe(false);
        expect(header.style.display).toBe('none');
        expect(footer.style.display).toBe('none');

        // Change back
        mockWindow.undecorated.set(false);
        fixture.detectChanges();
        expect(dialog.classList.contains('window')).toBe(true);
        expect(header.style.display).toBe('block');
        expect(footer.style.display).toBe('block');
    });

    it('should reactively update opacity in template', () => {
        // Initial value
        const opacityDiv = fixture.nativeElement.querySelector('.svy-dialog > div');
        expect(opacityDiv.style.opacity).toBe('1');

        // Change signal
        mockWindow.opacity.set(0.5);
        fixture.detectChanges();
        expect(opacityDiv.style.opacity).toBe('0.5');

        // Change again
        mockWindow.opacity.set(0);
        fixture.detectChanges();
        expect(opacityDiv.style.opacity).toBe('0');
    });

    it('should reactively update cssClassName in template', () => {
        // Initial: no custom class
        const dialog = fixture.nativeElement.querySelector('.svy-dialog');
        expect(dialog.classList.contains('my-dialog')).toBe(false);

        // Set class
        mockWindow.cssClassName.set('my-dialog');
        fixture.detectChanges();
        expect(dialog.classList.contains('my-dialog')).toBe(true);

        // Change class
        mockWindow.cssClassName.set('other-dialog');
        fixture.detectChanges();
        expect(dialog.classList.contains('other-dialog')).toBe(true);
    });

    it('should reactively update transparent background in template', () => {
        // Initial: not transparent
        const dialog = fixture.nativeElement.querySelector('.svy-dialog');
        expect(dialog.style.backgroundColor).not.toBe('transparent');

        // Set transparent
        mockWindow.transparent.set(true);
        fixture.detectChanges();
        expect(dialog.style.backgroundColor).toBe('transparent');

        // Unset transparent
        mockWindow.transparent.set(false);
        fixture.detectChanges();
        expect(dialog.style.backgroundColor).not.toBe('transparent');
    });

    it('should reactively show/hide form based on form signal', () => {
        // Initial: form present
        expect(component.formName()).toBe('myForm');
        let svyForm = fixture.nativeElement.querySelector('svy-form');
        expect(svyForm).not.toBeNull();

        // Change form
        mockWindow.form.set({ name: 'newForm', size: { width: 800, height: 600 } });
        fixture.detectChanges();
        expect(component.formName()).toBe('newForm');
        svyForm = fixture.nativeElement.querySelector('svy-form');
        expect(svyForm).not.toBeNull();

        // Set form to unknown (not in cache)
        mockFormService.hasFormCacheEntry.mockReturnValue(false);
        mockWindow.form.set({ name: 'unknownForm', size: { width: 100, height: 100 } });
        fixture.detectChanges();
        expect(component.formName()).toBeNull();
    });

    it('should reactively show default navigator based on navigatorForm signal', () => {
        // Initial: no navigator
        expect(component.defaultNavigator()).toBe(false);
        let navigator = fixture.nativeElement.querySelector('svy-default-navigator');
        expect(navigator).toBeNull();

        // Set default navigator
        mockWindow.navigatorForm.set({ name: 'default_navigator_container.html', size: { width: 80 } });
        fixture.detectChanges();
        expect(component.defaultNavigator()).toBe(true);
        navigator = fixture.nativeElement.querySelector('svy-default-navigator');
        expect(navigator).not.toBeNull();

        // Remove navigator
        mockWindow.navigatorForm.set({ name: null, size: { width: 0 } });
        fixture.detectChanges();
        expect(component.defaultNavigator()).toBe(false);
        navigator = fixture.nativeElement.querySelector('svy-default-navigator');
        expect(navigator).toBeNull();
    });

    it('should reactively show custom navigator form based on navigatorForm signal', () => {
        // Initial: no navigator form
        expect(component.navigatorFormName()).toBeNull();

        // Set custom navigator
        mockWindow.navigatorForm.set({ name: 'myNavigator', size: { width: 100 } });
        fixture.detectChanges();
        expect(component.navigatorFormName()).toBe('myNavigator');
        const svyForms = fixture.nativeElement.querySelectorAll('svy-form');
        // Should have 2: navigator form + main form
        expect(svyForms.length).toBe(2);
    });

    it('should handle null navigatorForm gracefully without throwing', () => {
        mockWindow.navigatorForm.set(null);
        // Should not throw during change detection
        expect(() => fixture.detectChanges()).not.toThrow();
        expect(component.defaultNavigator()).toBe(false);
        expect(component.navigatorFormName()).toBeNull();
    });

    it('should set tabStart and tabStop ids based on window name', () => {
        const tabStart = fixture.nativeElement.querySelector('#testDialog_tabStart');
        const tabStop = fixture.nativeElement.querySelector('#testDialog_tabStop');
        expect(tabStart).not.toBeNull();
        expect(tabStop).not.toBeNull();
    });
});
