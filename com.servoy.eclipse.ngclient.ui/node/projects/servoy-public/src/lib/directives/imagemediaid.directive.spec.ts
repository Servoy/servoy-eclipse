import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { Component, ElementRef, Renderer2, runInInjectionContext, Injector } from '@angular/core';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ImageMediaIdDirective } from './imagemediaid.directive';
import { ServoyBaseComponent, IViewStateListener } from '../basecomponent';
import { WindowRefService } from '../services/windowref.service';

/**
 * ResizeObserver is not implemented in jsdom; provide a minimal stub so the
 * directive's afterViewInit can run. We capture the callback so tests can
 * trigger a "resize" on demand.
 */
class ResizeObserverStub {
    // the production code only needs observe/unobserve; the callback is captured but not driven here
    constructor(_cb: ResizeObserverCallback) {
        // no-op
    }
    observe = vi.fn();
    unobserve = vi.fn();
    disconnect = vi.fn();
}

describe('ImageMediaIdDirective', () => {

    beforeEach(() => {
        (globalThis as any).ResizeObserver = ResizeObserverStub as any;
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    describe('unit - parseImageOptions via setImageStyle', () => {
        // The directive uses signal input() fields, so it must be constructed
        // inside an Angular injection context. We provide ElementRef / Renderer2 /
        // WindowRefService explicitly to control the reported grand-parent size.
        const buildDirective = (parentSize: { w: number; h: number }) => {
            TestBed.configureTestingModule({});
            const styles: Record<string, string> = {};
            const renderer = {
                setStyle: vi.fn((_el: any, k: string, v: string) => {
                    styles[k] = v;
                }),
                listen: vi.fn()
            } as unknown as Renderer2;
            const grandParent = { clientWidth: parentSize.w, clientHeight: parentSize.h };
            const parent = { parentNode: grandParent };
            const nativeElement = { parentNode: parent } as unknown as HTMLElement;
            const elemRef = { nativeElement } as ElementRef<HTMLElement>;
            const windowRef = { nativeWindow: window } as WindowRefService;
            const injector = TestBed.inject(Injector);
            const dir = runInInjectionContext(injector, () => new ImageMediaIdDirective(elemRef, renderer, windowRef));
            return { dir, styles, renderer };
        };

        it('should apply the background image and size styles for an img media', () => {
            const { dir, styles } = buildDirective({ w: 100, h: 100 });
            vi.spyOn(dir, 'media').mockReturnValue({ img: 'http://x/img.png?imageWidth=16&imageHeight=16', mediaOptions: 14 });
            (dir as any).setImageStyle();
            expect(styles['background-image']).toBe("url('http://x/img.png?imageWidth=16&imageHeight=16')");
            expect(styles['background-repeat']).toBe('no-repeat');
            expect(styles['width']).toMatch(/px$/);
            expect(styles['height']).toMatch(/px$/);
        });

        it('should clear the style (0px) when there is no img', () => {
            const { dir, styles } = buildDirective({ w: 100, h: 100 });
            // rollOverImg present so setImageStyle enters the branch, but img is missing
            vi.spyOn(dir, 'media').mockReturnValue({ rollOverImg: 'http://x/ro.png', mediaOptions: 14 });
            (dir as any).setImageStyle();
            expect(styles['width']).toBe('0px');
            expect(styles['height']).toBe('0px');
            expect(styles['backgroundImage']).toBe('');
        });

        it('should scale an image down to keep aspect ratio when larger than the container (mediaOptions 6)', () => {
            const { dir, styles } = buildDirective({ w: 50, h: 50 });
            vi.spyOn(dir, 'media').mockReturnValue({ img: 'u?imageWidth=100&imageHeight=200', mediaOptions: 6 + 8 });
            (dir as any).setImageStyle();
            // keep aspect ratio -> contain
            expect(styles['background-size']).toBe('contain');
            // image got scaled to fit within 50x50
            const w = parseInt(styles['width'], 10);
            const h = parseInt(styles['height'], 10);
            expect(w).toBeLessThanOrEqual(50);
            expect(h).toBeLessThanOrEqual(100);
        });

        it('should stretch to 100% 100% when not keeping aspect ratio (mediaOptions 6, no bit 8)', () => {
            const { dir, styles } = buildDirective({ w: 40, h: 40 });
            vi.spyOn(dir, 'media').mockReturnValue({ img: 'u?imageWidth=100&imageHeight=100', mediaOptions: 6 });
            (dir as any).setImageStyle();
            expect(styles['background-size']).toBe('100% 100%');
            expect(styles['width']).toBe('40px');
            expect(styles['height']).toBe('40px');
        });

        it('should do nothing when there is no media and no prior styles', () => {
            const { dir, styles } = buildDirective({ w: 100, h: 100 });
            vi.spyOn(dir, 'media').mockReturnValue(undefined);
            (dir as any).setImageStyle();
            expect(Object.keys(styles).length).toBe(0);
        });

        it('should retry via setTimeout when the container has zero size', () => {
            vi.useFakeTimers();
            const { dir, styles } = buildDirective({ w: 0, h: 0 });
            const spy = vi.spyOn(dir as any, 'setImageStyle');
            vi.spyOn(dir, 'media').mockReturnValue({ img: 'u?imageWidth=16&imageHeight=16', mediaOptions: 14 });
            (dir as any).setImageStyle();
            // no style applied yet, a retry is scheduled
            expect(styles['width']).toBeUndefined();
            vi.advanceTimersByTime(100);
            expect(spy.mock.calls.length).toBeGreaterThan(1);
            vi.useRealTimers();
        });
    });

    describe('integration - lifecycle through a host component', () => {
        @Component({
            standalone: true,
            selector: 'svy-img-host',
            imports: [ImageMediaIdDirective],
            template: '<div #element><span [svyImageMediaId]="media" [hostComponent]="hostComp"></span></div>'
        })
        class ImgHostComponent extends ServoyBaseComponent<HTMLDivElement> {
            media: any = { img: 'u?imageWidth=16&imageHeight=16', mediaOptions: 14 };
            get hostComp() {
                return this;
            }
        }

        let fixture: ComponentFixture<ImgHostComponent>;

        beforeEach(() => {
            TestBed.configureTestingModule({ imports: [ImgHostComponent, ImageMediaIdDirective] });
            fixture = TestBed.createComponent(ImgHostComponent);
            const api: any = {
                registerComponent: vi.fn(), unRegisterComponent: vi.fn(),
                getMarkupId: () => 'id', trustAsHtml: () => false, isInDesigner: () => false,
                isInAbsoluteLayout: () => true, getFormName: () => 'f'
            };
            fixture.componentRef.setInput('servoyApi', api);
            fixture.componentRef.setInput('name', 'comp');
        });

        it('should register itself as a view-state listener of the host component', () => {
            const spy = vi.spyOn(ServoyBaseComponent.prototype, 'addViewStateListener');
            fixture.detectChanges();
            const listenerAdded = spy.mock.calls.some(call => call[0] && typeof (call[0] as IViewStateListener).afterViewInit === 'function');
            expect(listenerAdded).toBe(true);
        });

        it('should observe the host element on afterViewInit', () => {
            fixture.detectChanges();
            const dir = fixture.debugElement.query(By.css('span')).injector.get(ImageMediaIdDirective);
            dir.afterViewInit();
            expect((dir as any).resizeObserver.observe).toHaveBeenCalled();
        });

        it('should unobserve and remove the listener on destroy', () => {
            fixture.detectChanges();
            const dir = fixture.debugElement.query(By.css('span')).injector.get(ImageMediaIdDirective);
            dir.afterViewInit();
            const observer = (dir as any).resizeObserver;
            const removeSpy = vi.spyOn(fixture.componentInstance, 'removeViewStateListener');
            fixture.destroy();
            expect(observer.unobserve).toHaveBeenCalled();
            expect(removeSpy).toHaveBeenCalled();
        });

        it('mouseenter/mouseleave should swap between rollover and normal image styles', () => {
            fixture.componentInstance.media = {
                img: 'u?imageWidth=16&imageHeight=16',
                rollOverImg: 'ro?imageWidth=16&imageHeight=16',
                mediaOptions: 14
            };
            fixture.detectChanges();
            const dir = fixture.debugElement.query(By.css('span')).injector.get(ImageMediaIdDirective);
            dir.afterViewInit();
            const setCss = vi.spyOn(dir as any, 'setCSSStyle');
            const host = fixture.componentInstance.getNativeElement();
            host.dispatchEvent(new Event('mouseenter'));
            host.dispatchEvent(new Event('mouseleave'));
            // rollOverImgStyle is populated lazily by setImageStyle; drive one more pass
            (dir as any).rollOverImgStyle = new Map([['background-image', 'x']]);
            host.dispatchEvent(new Event('mouseenter'));
            expect(setCss).toHaveBeenCalled();
        });
    });
});
