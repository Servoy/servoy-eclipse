import { TestBed, ComponentFixture } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { Subject } from 'rxjs';
import { signal, provideZonelessChangeDetection } from '@angular/core';
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
});
