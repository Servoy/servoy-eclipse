import { TestBed, ComponentFixture } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { Component, SimpleChange, SimpleChanges, provideZonelessChangeDetection, ChangeDetectionStrategy } from '@angular/core';
import { Subject } from 'rxjs';
import { ServoyPublicService, FormattingService, TooltipService, ServoyApi, WindowRefService } from '@servoy/public';
import { ServoyDefaultBaseLabel } from './baselabel';

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
    selector: 'servoydefault-test-baselabel',
    template: `<div #element><span #child>label</span></div>`,
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
})
class TestBaseLabelComponent extends ServoyDefaultBaseLabel<HTMLDivElement> {}

function change(current: any, previous: any = undefined, first = false): SimpleChange {
    return new SimpleChange(previous, current, first);
}

describe('ServoyDefaultBaseLabel', () => {
    let fixture: ComponentFixture<TestBaseLabelComponent>;
    let component: TestBaseLabelComponent;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [TestBaseLabelComponent],
            providers: [
                provideZonelessChangeDetection(),
                { provide: ServoyPublicService, useValue: { getUIProperty: vi.fn() } },
                { provide: FormattingService, useValue: { format: (data: any) => data } },
                { provide: TooltipService, useValue: { isTooltipActive: new Subject<boolean>(), isTooltipActiveSignal: vi.fn() } },
                { provide: WindowRefService, useValue: { nativeWindow: window } },
            ],
        });
        fixture = TestBed.createComponent(TestBaseLabelComponent);
        component = fixture.componentInstance;
        fixture.componentRef.setInput('servoyApi', createMockServoyApi());
        fixture.componentRef.setInput('name', 'testLabel');
        fixture.detectChanges();
    });

    it('creates and resolves the #child view query', () => {
        expect(component).toBeTruthy();
        expect(component.child()).toBeTruthy();
    });

    describe('svyOnChanges', () => {
        it('sets cursor to pointer when rolloverCursor is 12', () => {
            const spy = vi.spyOn(component.getRenderer(), 'setStyle');
            component.svyOnChanges({ rolloverCursor: change(12) } as SimpleChanges);
            expect(spy).toHaveBeenCalledWith(component.elementRef()!.nativeElement, 'cursor', 'pointer');
        });

        it('sets cursor to default when rolloverCursor is not 12', () => {
            const spy = vi.spyOn(component.getRenderer(), 'setStyle');
            component.svyOnChanges({ rolloverCursor: change(0) } as SimpleChanges);
            expect(spy).toHaveBeenCalledWith(component.elementRef()!.nativeElement, 'cursor', 'default');
        });

        it('sets accesskey attribute when mnemonic has a value', () => {
            const spy = vi.spyOn(component.getRenderer(), 'setAttribute');
            component.svyOnChanges({ mnemonic: change('A') } as SimpleChanges);
            expect(spy).toHaveBeenCalledWith(component.elementRef()!.nativeElement, 'accesskey', 'A');
        });

        it('removes accesskey attribute when mnemonic is cleared', () => {
            const spy = vi.spyOn(component.getRenderer(), 'removeAttribute');
            component.svyOnChanges({ mnemonic: change('') } as SimpleChanges);
            expect(spy).toHaveBeenCalledWith(component.elementRef()!.nativeElement, 'accesskey');
        });

        it('aligns the child to the top when verticalAlignment is 1', () => {
            fixture.componentRef.setInput('verticalAlignment', 1);
            fixture.detectChanges();
            const spy = vi.spyOn(component.getRenderer(), 'setStyle');
            component.svyOnChanges({ verticalAlignment: change(1) } as SimpleChanges);
            expect(spy).toHaveBeenCalledWith(component.child()!.nativeElement, 'top', '0px');
        });

        it('aligns the child to the bottom when verticalAlignment is 3', () => {
            fixture.componentRef.setInput('verticalAlignment', 3);
            fixture.detectChanges();
            const spy = vi.spyOn(component.getRenderer(), 'setStyle');
            component.svyOnChanges({ verticalAlignment: change(3) } as SimpleChanges);
            expect(spy).toHaveBeenCalledWith(component.child()!.nativeElement, 'top', '100%');
            expect(spy).toHaveBeenCalledWith(component.child()!.nativeElement, 'transform', 'translateY(-100%)');
        });

        it('centers the child for any other verticalAlignment', () => {
            fixture.componentRef.setInput('verticalAlignment', 2);
            fixture.detectChanges();
            const spy = vi.spyOn(component.getRenderer(), 'setStyle');
            component.svyOnChanges({ verticalAlignment: change(2) } as SimpleChanges);
            expect(spy).toHaveBeenCalledWith(component.child()!.nativeElement, 'top', '50%');
            expect(spy).toHaveBeenCalledWith(component.child()!.nativeElement, 'transform', 'translateY(-50%)');
        });
    });

    describe('attachHandlers', () => {
        it('fires onActionMethodID directly on click when no double-click handler is set', () => {
            const action = vi.fn();
            fixture.componentRef.setInput('onActionMethodID', action);
            fixture.detectChanges();
            let clickCb: (e: any) => void = () => undefined;
            vi.spyOn(component.getRenderer(), 'listen').mockImplementation((_t: any, evt: any, cb: any) => {
                if (evt === 'click') clickCb = cb;
                return () => undefined;
            });
            (component as any).attachHandlers();
            const evt = new Event('click');
            clickCb(evt);
            expect(action).toHaveBeenCalledWith(evt);
        });

        it('defers the single click when a double-click handler is also set', () => {
            vi.useFakeTimers();
            const action = vi.fn();
            const dbl = vi.fn();
            fixture.componentRef.setInput('onActionMethodID', action);
            fixture.componentRef.setInput('onDoubleClickMethodID', dbl);
            fixture.detectChanges();
            let clickCb: (e: any) => void = () => undefined;
            vi.spyOn(component.getRenderer(), 'listen').mockImplementation((_t: any, evt: any, cb: any) => {
                if (evt === 'click') clickCb = cb;
                return () => undefined;
            });
            (component as any).attachHandlers();
            clickCb(new Event('click'));
            // not yet fired - still inside the 250ms window
            expect(action).not.toHaveBeenCalled();
            vi.advanceTimersByTime(250);
            expect(action).toHaveBeenCalled();
            vi.useRealTimers();
        });

        it('cancels the single click when a second click arrives (double click)', () => {
            vi.useFakeTimers();
            const action = vi.fn();
            const dbl = vi.fn();
            fixture.componentRef.setInput('onActionMethodID', action);
            fixture.componentRef.setInput('onDoubleClickMethodID', dbl);
            fixture.detectChanges();
            let clickCb: (e: any) => void = () => undefined;
            vi.spyOn(component.getRenderer(), 'listen').mockImplementation((_t: any, evt: any, cb: any) => {
                if (evt === 'click') clickCb = cb;
                return () => undefined;
            });
            (component as any).attachHandlers();
            clickCb(new Event('click'));
            clickCb(new Event('click'));
            vi.advanceTimersByTime(300);
            expect(action).not.toHaveBeenCalled();
            vi.useRealTimers();
        });

        it('fires onDoubleClickMethodID on dblclick', () => {
            const dbl = vi.fn();
            fixture.componentRef.setInput('onDoubleClickMethodID', dbl);
            fixture.detectChanges();
            let dblCb: (e: any) => void = () => undefined;
            vi.spyOn(component.getRenderer(), 'listen').mockImplementation((_t: any, evt: any, cb: any) => {
                if (evt === 'dblclick') dblCb = cb;
                return () => undefined;
            });
            (component as any).attachHandlers();
            const evt = new Event('dblclick');
            dblCb(evt);
            expect(dbl).toHaveBeenCalledWith(evt);
        });
    });
});
