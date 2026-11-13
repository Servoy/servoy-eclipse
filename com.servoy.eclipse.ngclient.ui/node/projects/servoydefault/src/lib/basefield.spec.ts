import { TestBed, ComponentFixture } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { Component, SimpleChange, SimpleChanges, provideZonelessChangeDetection, ChangeDetectionStrategy } from '@angular/core';
import { Subject } from 'rxjs';
import { ServoyPublicService, FormattingService, TooltipService, ServoyApi, WindowRefService } from '@servoy/public';
import { ServoyDefaultBaseField } from './basefield';

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

@Component({
    selector: 'servoydefault-test-basefield',
    template: `<input #element type="text" />`,
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
})
class TestBaseFieldComponent extends ServoyDefaultBaseField<HTMLInputElement> {}

function change(current: any, previous: any = undefined, first = false): SimpleChange {
    return new SimpleChange(previous, current, first);
}

describe('ServoyDefaultBaseField', () => {
    let fixture: ComponentFixture<TestBaseFieldComponent>;
    let component: TestBaseFieldComponent;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [TestBaseFieldComponent],
            providers: [
                provideZonelessChangeDetection(),
                { provide: ServoyPublicService, useValue: { getUIProperty: vi.fn() } },
                { provide: FormattingService, useValue: { format: (data: any) => data, testKeyPressed: (e: any, code: number) => e.keyCode === code } },
                { provide: TooltipService, useValue: { isTooltipActive: new Subject<boolean>(), isTooltipActiveSignal: vi.fn() } },
                { provide: WindowRefService, useValue: { nativeWindow: window } },
            ],
        });
        fixture = TestBed.createComponent(TestBaseFieldComponent);
        component = fixture.componentInstance;
        fixture.componentRef.setInput('servoyApi', createMockServoyApi());
        fixture.componentRef.setInput('name', 'testField');
        fixture.detectChanges();
    });

    it('creates and defaults dataProviderID to null on init', () => {
        expect(component).toBeTruthy();
        expect(component.dataProviderID()).toBeNull();
    });

    describe('onDataChangeCallback', () => {
        it('marks invalid and stores tooltip for a string (error message) return value', () => {
            const addSpy = vi.spyOn(component.getRenderer(), 'addClass');
            const removeSpy = vi.spyOn(component.getRenderer(), 'removeClass');
            component.toolTipText.set('original');
            component.storedTooltip = false;
            component.onDataChangeCallback({}, 'validation error');
            expect(removeSpy).toHaveBeenCalledWith(component.elementRef()!.nativeElement, 'ng-valid');
            expect(addSpy).toHaveBeenCalledWith(component.elementRef()!.nativeElement, 'ng-invalid');
            expect(component.storedTooltip).toBe('original');
            expect(component.toolTipText()).toBe('validation error');
        });

        it('marks invalid without touching tooltip when returnval is false', () => {
            const addSpy = vi.spyOn(component.getRenderer(), 'addClass');
            component.storedTooltip = false;
            component.onDataChangeCallback({}, false);
            expect(addSpy).toHaveBeenCalledWith(component.elementRef()!.nativeElement, 'ng-invalid');
            expect(component.storedTooltip).toBe(false);
        });

        it('marks valid and restores a stored tooltip on a truthy non-string return', () => {
            const addSpy = vi.spyOn(component.getRenderer(), 'addClass');
            component.storedTooltip = 'restored';
            component.onDataChangeCallback({}, true);
            expect(addSpy).toHaveBeenCalledWith(component.elementRef()!.nativeElement, 'ng-valid');
            expect(component.toolTipText()).toBe('restored');
            expect(component.storedTooltip).toBe(false);
        });
    });

    describe('focus listeners', () => {
        it('executes onFocusGained when mustExecuteOnFocus is true and resets it', () => {
            const handler = vi.fn();
            fixture.componentRef.setInput('onFocusGainedMethodID', handler);
            fixture.detectChanges();
            let focusCb: (e: any) => void = () => undefined;
            vi.spyOn(component.getRenderer(), 'listen').mockImplementation((_t: any, evt: any, cb: any) => {
                if (evt === 'focus') focusCb = cb;
                return () => undefined;
            });
            component.mustExecuteOnFocus = true;
            component.attachFocusListeners(component.getFocusElement());
            const evt = new Event('focus');
            focusCb(evt);
            expect(handler).toHaveBeenCalledWith(evt);
            expect(component.mustExecuteOnFocus).toBe(true);
        });

        it('skips onFocusGained when mustExecuteOnFocus is false', () => {
            const handler = vi.fn();
            fixture.componentRef.setInput('onFocusGainedMethodID', handler);
            fixture.detectChanges();
            let focusCb: (e: any) => void = () => undefined;
            vi.spyOn(component.getRenderer(), 'listen').mockImplementation((_t: any, evt: any, cb: any) => {
                if (evt === 'focus') focusCb = cb;
                return () => undefined;
            });
            component.mustExecuteOnFocus = false;
            component.attachFocusListeners(component.getFocusElement());
            focusCb(new Event('focus'));
            expect(handler).not.toHaveBeenCalled();
            expect(component.mustExecuteOnFocus).toBe(true);
        });

        it('executes onFocusLost on blur', () => {
            const handler = vi.fn();
            fixture.componentRef.setInput('onFocusLostMethodID', handler);
            fixture.detectChanges();
            let blurCb: (e: any) => void = () => undefined;
            vi.spyOn(component.getRenderer(), 'listen').mockImplementation((_t: any, evt: any, cb: any) => {
                if (evt === 'blur') blurCb = cb;
                return () => undefined;
            });
            component.attachFocusListeners(component.getFocusElement());
            const evt = new Event('blur');
            blurCb(evt);
            expect(handler).toHaveBeenCalledWith(evt);
        });
    });

    describe('requestFocus', () => {
        it('sets mustExecuteOnFocus and focuses the element', () => {
            const focusSpy = vi.spyOn(component.getFocusElement(), 'focus');
            component.requestFocus(false);
            expect(component.mustExecuteOnFocus).toBe(false);
            expect(focusSpy).toHaveBeenCalled();
        });
    });

    describe('svyOnChanges', () => {
        it('sets placeholder attribute when placeholderText has a value', () => {
            const spy = vi.spyOn(component.getRenderer(), 'setAttribute');
            component.svyOnChanges({ placeholderText: change('type here') } as SimpleChanges);
            expect(spy).toHaveBeenCalledWith(component.getNativeElement(), 'placeholder', 'type here');
        });

        it('removes placeholder attribute when placeholderText is cleared', () => {
            const spy = vi.spyOn(component.getRenderer(), 'removeAttribute');
            component.svyOnChanges({ placeholderText: change(null) } as SimpleChanges);
            expect(spy).toHaveBeenCalledWith(component.getNativeElement(), 'placeholder');
        });

        it('removes readonly when editable and not readOnly', () => {
            fixture.componentRef.setInput('editable', true);
            fixture.componentRef.setInput('readOnly', false);
            fixture.detectChanges();
            const spy = vi.spyOn(component.getRenderer(), 'removeAttribute');
            component.svyOnChanges({ editable: change(true) } as SimpleChanges);
            expect(spy).toHaveBeenCalledWith(component.getFocusElement(), 'readonly');
        });

        it('sets readonly when not editable', () => {
            fixture.componentRef.setInput('editable', false);
            fixture.componentRef.setInput('readOnly', false);
            fixture.detectChanges();
            const spy = vi.spyOn(component.getRenderer(), 'setAttribute');
            component.svyOnChanges({ editable: change(false) } as SimpleChanges);
            expect(spy).toHaveBeenCalledWith(component.getFocusElement(), 'readonly', 'readonly');
        });

        it('removes readonly in findmode regardless of editable', () => {
            fixture.componentRef.setInput('findmode', true);
            fixture.componentRef.setInput('editable', false);
            fixture.detectChanges();
            const spy = vi.spyOn(component.getRenderer(), 'removeAttribute');
            component.svyOnChanges({ findmode: change(true) } as SimpleChanges);
            expect(spy).toHaveBeenCalledWith(component.getFocusElement(), 'readonly');
        });
    });

    describe('pushUpdate', () => {
        it('emits the current dataProviderID', () => {
            const emitSpy = vi.spyOn(component.dataProviderIDChange, 'emit');
            component.dataProviderID.set('abc');
            component.pushUpdate();
            expect(emitSpy).toHaveBeenCalledWith('abc');
        });
    });

    describe('selectAll', () => {
        it('selects the focus element content', () => {
            const selectSpy = vi.spyOn(component.getFocusElement() as HTMLInputElement, 'select');
            component.selectAll();
            expect(selectSpy).toHaveBeenCalled();
        });
    });

    describe('getSelectedText', () => {
        let originalGetSelection: typeof window.getSelection;
        afterEach(() => {
            window.getSelection = originalGetSelection;
        });

        it('returns the selection text when the anchor is the focus element', () => {
            originalGetSelection = window.getSelection;
            window.getSelection = vi.fn().mockReturnValue({
                anchorNode: component.getFocusElement(),
                toString: () => 'selected text',
            }) as any;
            expect(component.getSelectedText()).toBe('selected text');
        });

        it('returns empty string when the selection is outside the element', () => {
            originalGetSelection = window.getSelection;
            window.getSelection = vi.fn().mockReturnValue({
                anchorNode: document.createElement('div'),
                toString: () => 'other',
            }) as any;
            expect(component.getSelectedText()).toBe('');
        });
    });

    describe('replaceSelectedText', () => {
        it('inserts text at the current selection and dispatches a change event', () => {
            const input = component.getFocusElement() as HTMLInputElement;
            input.value = 'HelloWorld';
            input.selectionStart = 5;
            input.selectionEnd = 5;
            const dispatchSpy = vi.spyOn(input, 'dispatchEvent');
            component.replaceSelectedText('XXX');
            expect(input.value).toBe('HelloXXXWorld');
            expect(dispatchSpy).toHaveBeenCalled();
        });
    });

    describe('getAsPlainText', () => {
        it('strips html tags from the dataProviderID value', () => {
            component.dataProviderID.set('<b>bold</b> text');
            expect(component.getAsPlainText()).toBe('bold text');
        });

        it('returns the falsy dataProviderID unchanged', () => {
            component.dataProviderID.set(null);
            expect(component.getAsPlainText()).toBeNull();
        });
    });

    describe('attachHandlers action key', () => {
        it('fires onActionMethodID after enter keyup (keyCode 13)', () => {
            vi.useFakeTimers();
            const handler = vi.fn();
            fixture.componentRef.setInput('onActionMethodID', handler);
            fixture.detectChanges();
            let keyupCb: (e: any) => void = () => undefined;
            vi.spyOn(component.getRenderer(), 'listen').mockImplementation((_t: any, evt: any, cb: any) => {
                if (evt === 'keyup') keyupCb = cb;
                return () => undefined;
            });
            (component as any).attachHandlers();
            keyupCb({ keyCode: 13 });
            vi.advanceTimersByTime(100);
            expect(handler).toHaveBeenCalled();
            vi.useRealTimers();
        });
    });
});
