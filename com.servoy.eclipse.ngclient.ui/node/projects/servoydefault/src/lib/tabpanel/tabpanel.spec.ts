import { TestBed, ComponentFixture } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { DefaultTabpanelActiveTabVisibilityListener } from './tabpanel';

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
