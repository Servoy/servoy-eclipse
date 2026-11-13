import { TestBed, ComponentFixture } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { Component, SimpleChange, SimpleChanges, provideZonelessChangeDetection, ChangeDetectionStrategy } from '@angular/core';
import { Subject } from 'rxjs';
import { ServoyPublicService, FormattingService, TooltipService, ServoyApi, WindowRefService } from '@servoy/public';
import { ServoyDefaultBaseComponent } from './basecomponent';

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
    selector: 'servoydefault-test-basecomponent',
    template: `<div #element><span>content</span></div>`,
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
})
class TestBaseComponent extends ServoyDefaultBaseComponent<HTMLDivElement> {}

function change(current: any, previous: any = undefined, first = false): SimpleChange {
    return new SimpleChange(previous, current, first);
}

describe('ServoyDefaultBaseComponent', () => {
    let fixture: ComponentFixture<TestBaseComponent>;
    let component: TestBaseComponent;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [TestBaseComponent],
            providers: [
                provideZonelessChangeDetection(),
                { provide: ServoyPublicService, useValue: { getUIProperty: vi.fn() } },
                { provide: FormattingService, useValue: { format: (data: any) => data } },
                { provide: TooltipService, useValue: { isTooltipActive: new Subject<boolean>(), isTooltipActiveSignal: vi.fn() } },
                { provide: WindowRefService, useValue: { nativeWindow: window } },
            ],
        });
        fixture = TestBed.createComponent(TestBaseComponent);
        component = fixture.componentInstance;
        fixture.componentRef.setInput('servoyApi', createMockServoyApi());
        fixture.componentRef.setInput('name', 'testComponent');
        fixture.detectChanges();
    });

    it('creates and renders', () => {
        expect(component).toBeTruthy();
        expect(component.getNativeElement()).toBeTruthy();
    });

    describe('scroll accessors', () => {
        it('reads and writes scroll position through the native element', () => {
            const el = component.getNativeElement();
            Object.defineProperty(el, 'scrollLeft', { value: 0, writable: true, configurable: true });
            Object.defineProperty(el, 'scrollTop', { value: 0, writable: true, configurable: true });
            component.setScroll(5, 7);
            expect(el.scrollLeft).toBe(5);
            expect(el.scrollTop).toBe(7);
            expect(component.getScrollX()).toBe(5);
            expect(component.getScrollY()).toBe(7);
        });
    });

    describe('focus', () => {
        it('getFocusElement returns the native element and requestFocus focuses it', () => {
            expect(component.getFocusElement()).toBe(component.getNativeElement());
            const focusSpy = vi.spyOn(component.getFocusElement(), 'focus');
            component.requestFocus(true);
            expect(focusSpy).toHaveBeenCalled();
        });
    });

    it('needsScrollbarInformation defaults to true', () => {
        expect(component.needsScrollbarInformation()).toBe(true);
    });

    describe('svyOnChanges style branches', () => {
        it('applies foreground color to the native child', () => {
            const spy = vi.spyOn(component.getRenderer(), 'setStyle');
            component.svyOnChanges({ foreground: change('#ff0000') } as SimpleChanges);
            expect(spy).toHaveBeenCalledWith(component.getNativeChild(), 'color', '#ff0000');
        });

        it('sets backgroundColor to the color when not transparent', () => {
            fixture.componentRef.setInput('transparent', false);
            fixture.detectChanges();
            const spy = vi.spyOn(component.getRenderer(), 'setStyle');
            component.svyOnChanges({ background: change('#00ff00') } as SimpleChanges);
            expect(spy).toHaveBeenCalledWith(component.getNativeChild(), 'backgroundColor', '#00ff00');
        });

        it('sets backgroundColor to transparent when transparent is true', () => {
            fixture.componentRef.setInput('transparent', true);
            fixture.detectChanges();
            const spy = vi.spyOn(component.getRenderer(), 'setStyle');
            component.svyOnChanges({ transparent: change(true) } as SimpleChanges);
            expect(spy).toHaveBeenCalledWith(component.getNativeChild(), 'backgroundColor', 'transparent');
        });

        it('removes disabled attribute when enabled becomes true', () => {
            const spy = vi.spyOn(component.getRenderer(), 'removeAttribute');
            component.svyOnChanges({ enabled: change(true) } as SimpleChanges);
            expect(spy).toHaveBeenCalledWith(component.getFocusElement(), 'disabled');
        });

        it('sets disabled attribute when enabled becomes false', () => {
            const spy = vi.spyOn(component.getRenderer(), 'setAttribute');
            component.svyOnChanges({ enabled: change(false) } as SimpleChanges);
            expect(spy).toHaveBeenCalledWith(component.getFocusElement(), 'disabled', 'disabled');
        });

        it('applies each margin style to the native element', () => {
            const spy = vi.spyOn(component.getRenderer(), 'setStyle');
            component.svyOnChanges({ margin: change({ paddingTop: '5px', paddingLeft: '3px' }) } as SimpleChanges);
            expect(spy).toHaveBeenCalledWith(component.getNativeElement(), 'paddingTop', '5px');
            expect(spy).toHaveBeenCalledWith(component.getNativeElement(), 'paddingLeft', '3px');
        });

        it('does nothing for a null margin', () => {
            const spy = vi.spyOn(component.getRenderer(), 'setStyle');
            component.svyOnChanges({ margin: change(null) } as SimpleChanges);
            expect(spy).not.toHaveBeenCalled();
        });

        it('removes previous style classes and adds new ones', () => {
            const addSpy = vi.spyOn(component.getRenderer(), 'addClass');
            const removeSpy = vi.spyOn(component.getRenderer(), 'removeClass');
            component.svyOnChanges({ styleClass: change('new1 new2', 'old1 old2') } as SimpleChanges);
            expect(removeSpy).toHaveBeenCalledWith(component.getNativeChild(), 'old1');
            expect(removeSpy).toHaveBeenCalledWith(component.getNativeChild(), 'old2');
            expect(addSpy).toHaveBeenCalledWith(component.getNativeChild(), 'new1');
            expect(addSpy).toHaveBeenCalledWith(component.getNativeChild(), 'new2');
        });

        it('skips scrollbars handling when needsScrollbarInformation is false', () => {
            vi.spyOn(component, 'needsScrollbarInformation').mockReturnValue(false);
            expect(() => component.svyOnChanges({ scrollbars: change(0) } as SimpleChanges)).not.toThrow();
        });
    });

    describe('attachHandlers', () => {
        it('listens for contextmenu when onRightClickMethodID is set', () => {
            const handler = vi.fn();
            fixture.componentRef.setInput('onRightClickMethodID', handler);
            fixture.detectChanges();
            const listenSpy = vi.spyOn(component.getRenderer(), 'listen');
            (component as any).attachHandlers();
            expect(listenSpy).toHaveBeenCalledWith(component.getNativeElement(), 'contextmenu', expect.any(Function));
        });

        it('fires the right click handler and returns false from the listener', () => {
            const handler = vi.fn();
            fixture.componentRef.setInput('onRightClickMethodID', handler);
            fixture.detectChanges();
            let captured: (e: Event) => any = () => undefined;
            vi.spyOn(component.getRenderer(), 'listen').mockImplementation((_t: any, _e: any, cb: any) => {
                captured = cb;
                return () => undefined;
            });
            (component as any).attachHandlers();
            const evt = new Event('contextmenu');
            const result = captured(evt);
            expect(handler).toHaveBeenCalledWith(evt);
            expect(result).toBe(false);
        });
    });
});
