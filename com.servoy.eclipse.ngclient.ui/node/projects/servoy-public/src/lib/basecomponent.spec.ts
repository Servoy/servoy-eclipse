import { Component, ElementRef } from '@angular/core';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ServoyBaseComponent, IViewStateListener, ComponentContributor, IComponentContributorListener } from './basecomponent';
import { ServoyApi } from './servoy_api';

@Component({
    selector: 'test-component',
    template: '<div #element>test</div>',
    standalone: true,
})
class TestComponent extends ServoyBaseComponent<HTMLDivElement> {
    svyOnInitCalled = false;
    svyOnChangesCalled = false;
    lastChanges: any = null;

    override svyOnInit() {
        super.svyOnInit();
        this.svyOnInitCalled = true;
    }

    override svyOnChanges(changes: any) {
        super.svyOnChanges(changes);
        this.svyOnChangesCalled = true;
        this.lastChanges = changes;
    }
}

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

describe('ServoyBaseComponent', () => {
    let fixture: ComponentFixture<TestComponent>;
    let component: TestComponent;
    let mockServoyApi: ServoyApi;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [TestComponent],
        });
        fixture = TestBed.createComponent(TestComponent);
        component = fixture.componentInstance;
        mockServoyApi = createMockServoyApi();
        fixture.componentRef.setInput('servoyApi', mockServoyApi);
        fixture.componentRef.setInput('name', 'testComp');
    });

    describe('initialization', () => {
        it('should call svyOnInit after view is ready', () => {
            expect(component.svyOnInitCalled).toBe(false);
            fixture.detectChanges();
            expect(component.svyOnInitCalled).toBe(true);
        });

        it('should register with servoyApi on init', () => {
            fixture.detectChanges();
            expect(mockServoyApi.registerComponent).toHaveBeenCalledWith(component);
        });

        it('should set svyHostComponent on native element', () => {
            fixture.detectChanges();
            expect((component.getNativeElement() as any)['svyHostComponent']).toBe(component);
        });

        it('should provide access to native element after init', () => {
            fixture.detectChanges();
            expect(component.getNativeElement()).toBeTruthy();
            expect(component.getNativeElement().tagName).toBe('DIV');
        });
    });

    describe('destroy', () => {
        it('should unregister with servoyApi on destroy', () => {
            fixture.detectChanges();
            fixture.destroy();
            expect(mockServoyApi.unRegisterComponent).toHaveBeenCalledWith(component);
        });

        it('should clear svyHostComponent on native element on destroy', () => {
            fixture.detectChanges();
            const el = component.getNativeElement();
            fixture.destroy();
            expect((el as any)['svyHostComponent']).toBeNull();
        });
    });

    describe('servoyAttributes', () => {
        it('should apply initial attributes on init', () => {
            fixture.componentRef.setInput('servoyAttributes', { 'data-id': '123', 'aria-label': 'test' });
            fixture.detectChanges();
            const el = component.getNativeElement();
            expect(el.getAttribute('data-id')).toBe('123');
            expect(el.getAttribute('aria-label')).toBe('test');
        });

        it('should update attributes when servoyAttributes signal changes', () => {
            fixture.componentRef.setInput('servoyAttributes', { 'data-id': '123' });
            fixture.detectChanges();

            fixture.componentRef.setInput('servoyAttributes', { 'data-id': '456', 'data-new': 'value' });
            fixture.detectChanges();

            const el = component.getNativeElement();
            expect(el.getAttribute('data-id')).toBe('456');
            expect(el.getAttribute('data-new')).toBe('value');
        });

        it('should remove previous attributes when servoyAttributes changes', () => {
            fixture.componentRef.setInput('servoyAttributes', { 'data-old': 'remove-me' });
            fixture.detectChanges();

            fixture.componentRef.setInput('servoyAttributes', { 'data-new': 'keep-me' });
            fixture.detectChanges();

            const el = component.getNativeElement();
            expect(el.getAttribute('data-old')).toBeNull();
            expect(el.getAttribute('data-new')).toBe('keep-me');
        });
    });

    describe('viewStateListeners', () => {
        it('should notify listeners on init', () => {
            const listener: IViewStateListener = { afterViewInit: vi.fn() };
            component.addViewStateListener(listener);
            fixture.detectChanges();
            expect(listener.afterViewInit).toHaveBeenCalled();
        });

        it('should not notify removed listeners', () => {
            const listener: IViewStateListener = { afterViewInit: vi.fn() };
            component.addViewStateListener(listener);
            component.removeViewStateListener(listener);
            fixture.detectChanges();
            expect(listener.afterViewInit).not.toHaveBeenCalled();
        });
    });

    describe('ComponentContributor', () => {
        it('should notify contributor listeners on component creation', () => {
            const contributor = new ComponentContributor();
            const listener: IComponentContributorListener = { componentCreated: vi.fn() };
            contributor.addComponentListener(listener);
            fixture.detectChanges();
            expect(listener.componentCreated).toHaveBeenCalledWith(component);
        });
    });

    describe('name signal', () => {
        it('should provide name value via signal', () => {
            fixture.detectChanges();
            expect(component.name()).toBe('testComp');
        });

        it('should update when name input changes', () => {
            fixture.detectChanges();
            fixture.componentRef.setInput('name', 'newName');
            fixture.detectChanges();
            expect(component.name()).toBe('newName');
        });
    });
});
