import { TestBed, ComponentFixture } from '@angular/core/testing';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { Subject } from 'rxjs';
import { signal, provideZonelessChangeDetection, SimpleChange } from '@angular/core';
import { ServoyPublicService, FormattingService, TooltipService, ServoyApi, WindowRefService } from '@servoy/public';
import { ServoyDefaultImageMedia } from './imagemedia';

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

describe('ServoyDefaultImageMedia', () => {
    let fixture: ComponentFixture<ServoyDefaultImageMedia>;
    let component: ServoyDefaultImageMedia;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ServoyDefaultImageMedia],
            providers: [
                provideZonelessChangeDetection(),
                { provide: ServoyPublicService, useValue: { generateUploadUrl: vi.fn().mockReturnValue('/upload'), showFileOpenDialog: vi.fn() } },
                { provide: FormattingService, useValue: {} },
                { provide: TooltipService, useValue: { isTooltipActive: new Subject<boolean>(), isTooltipActiveSignal: signal(false) } },
                { provide: WindowRefService, useValue: { nativeWindow: window } },
            ],
        });
        fixture = TestBed.createComponent(ServoyDefaultImageMedia);
        component = fixture.componentInstance;
        fixture.componentRef.setInput('servoyApi', createMockServoyApi());
        fixture.componentRef.setInput('name', 'testImageMedia');
        fixture.componentRef.setInput('enabled', true);
        fixture.componentRef.setInput('editable', true);
    });

    it('should create and render without template errors', () => {
        expect(() => fixture.detectChanges()).not.toThrow();
        expect(component).toBeTruthy();
    });

    it('should render UploadDirective element', () => {
        fixture.detectChanges();
        const uploadDiv = fixture.nativeElement.querySelector('[svyUpload]');
        expect(uploadDiv).toBeTruthy();
    });

    it('should show upload/download/delete buttons when enabled and editable', () => {
        fixture.detectChanges();
        const buttons = fixture.nativeElement.querySelectorAll('.fas');
        expect(buttons.length).toBe(3);
    });

    it('should display EMPTY image by default', () => {
        fixture.detectChanges();
        const img = fixture.nativeElement.querySelector('img');
        expect(img.src).toContain(ServoyDefaultImageMedia.EMPTY);
    });

    describe('deleteMedia', () => {
        it('clears the dataProviderID, pushes the update and resets the image to EMPTY', () => {
            fixture.detectChanges();
            const emitSpy = vi.spyOn(component.dataProviderIDChange, 'emit');
            component.dataProviderID.set('something');
            component.deleteMedia();
            expect(component.dataProviderID()).toBeNull();
            expect(emitSpy).toHaveBeenCalled();
            expect(component.imageURL).toBe(ServoyDefaultImageMedia.EMPTY);
        });
    });

    describe('downloadMedia', () => {
        let openSpy: any;
        afterEach(() => {
            openSpy?.mockRestore();
        });

        it('opens a download window using the url of the data provider', () => {
            fixture.detectChanges();
            openSpy = vi.spyOn(window, 'open').mockReturnValue(null);
            component.dataProviderID.set({ url: 'http://example/media' });
            component.downloadMedia();
            expect(openSpy).toHaveBeenCalled();
            expect(openSpy.mock.calls[0][0]).toBe('http://example/media');
        });

        it('does nothing when there is no dataProviderID', () => {
            fixture.detectChanges();
            openSpy = vi.spyOn(window, 'open').mockReturnValue(null);
            component.dataProviderID.set(null);
            component.downloadMedia();
            expect(openSpy).not.toHaveBeenCalled();
        });
    });

    describe('svyOnChanges / updateImageURL', () => {
        it('uses the url directly for an image content type', () => {
            fixture.detectChanges();
            component.dataProviderID.set({ url: '/img/a.png', contentType: 'image/png' });
            component.svyOnChanges({ dataProviderID: new SimpleChange(undefined, component.dataProviderID(), false) } as any);
            expect(component.imageURL).toBe('/img/a.png');
        });

        it('uses the NOT_EMPTY placeholder for a non-image content type', () => {
            fixture.detectChanges();
            component.dataProviderID.set({ url: '/doc/a.pdf', contentType: 'application/pdf' });
            component.svyOnChanges({ dataProviderID: new SimpleChange(undefined, component.dataProviderID(), false) } as any);
            expect(component.imageURL).toBe(ServoyDefaultImageMedia.NOT_EMPTY);
        });

        it('uses the raw value when the data provider has no url', () => {
            fixture.detectChanges();
            component.dataProviderID.set('data:image/gif;base64,AAAA');
            component.svyOnChanges({ dataProviderID: new SimpleChange(undefined, component.dataProviderID(), false) } as any);
            expect(component.imageURL).toBe('data:image/gif;base64,AAAA');
        });

        it('resets to EMPTY when the data provider is cleared', () => {
            fixture.detectChanges();
            component.dataProviderID.set(null);
            component.svyOnChanges({ dataProviderID: new SimpleChange('x', null, false) } as any);
            expect(component.imageURL).toBe(ServoyDefaultImageMedia.EMPTY);
        });
    });

    describe('setCss', () => {
        it('centers horizontally when horizontalAlignment is 0 and the container is wider', () => {
            fixture.detectChanges();
            fixture.componentRef.setInput('horizontalAlignment', 0);
            fixture.detectChanges();
            const parent = document.createElement('div');
            Object.defineProperty(parent, 'clientHeight', { value: 100, configurable: true });
            Object.defineProperty(parent, 'clientWidth', { value: 200, configurable: true });
            const img = document.createElement('img');
            Object.defineProperty(img, 'clientHeight', { value: 50, configurable: true });
            Object.defineProperty(img, 'clientWidth', { value: 80, configurable: true });
            parent.appendChild(img);
            const spy = vi.spyOn(component.getRenderer(), 'setStyle');
            component.setCss({ target: img } as any);
            expect(spy).toHaveBeenCalledWith(img, 'left', '60px');
            expect(spy).toHaveBeenCalledWith(img, 'top', '25px');
        });

        it('right aligns when horizontalAlignment is 4', () => {
            fixture.detectChanges();
            fixture.componentRef.setInput('horizontalAlignment', 4);
            fixture.detectChanges();
            const parent = document.createElement('div');
            Object.defineProperty(parent, 'clientHeight', { value: 100, configurable: true });
            Object.defineProperty(parent, 'clientWidth', { value: 200, configurable: true });
            const img = document.createElement('img');
            Object.defineProperty(img, 'clientHeight', { value: 50, configurable: true });
            Object.defineProperty(img, 'clientWidth', { value: 80, configurable: true });
            parent.appendChild(img);
            const spy = vi.spyOn(component.getRenderer(), 'setStyle');
            component.setCss({ target: img } as any);
            expect(spy).toHaveBeenCalledWith(img, 'left', '120px');
        });
    });
});
