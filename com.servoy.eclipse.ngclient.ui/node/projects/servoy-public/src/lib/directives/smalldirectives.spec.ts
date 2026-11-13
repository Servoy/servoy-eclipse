import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { Injector, runInInjectionContext } from '@angular/core';
import { TabFixDirective } from './tabfix.directive';
import { StartEditDirective } from './startedit.directive';
import { AutosaveDirective } from './autosave.directive';
import { UploadDirective } from './upload.directive';
import { ServoyPublicService } from '../services/servoy_public.service';
import { ServoyBaseComponent } from '../basecomponent';
import { LoggerFactory, LoggerService } from '../logger.service';

const makeLoggerFactory = (logger: LoggerService): LoggerFactory =>
    ({ getLogger: vi.fn().mockReturnValue(logger) } as unknown as LoggerFactory);

describe('TabFixDirective', () => {
    let dir: TabFixDirective;
    const popup = { closePopup: vi.fn() };

    beforeEach(() => {
        TestBed.configureTestingModule({});
        dir = TestBed.runInInjectionContext(() => new TabFixDirective());
        vi.spyOn(dir, 'typeahead').mockReturnValue(popup as any);
        popup.closePopup.mockClear();
    });

    it('focus resets the startedTyping flag', () => {
        dir.startedTyping = true;
        dir.onFocus(null);
        expect(dir.startedTyping).toBe(false);
    });

    it('a non-Tab key sets startedTyping', () => {
        dir.handleKeyDown({ key: 'a', stopPropagation: vi.fn() } as unknown as KeyboardEvent);
        expect(dir.startedTyping).toBe(true);
    });

    it('arrow keys stop propagation', () => {
        const stop = vi.fn();
        dir.handleKeyDown({ key: 'ArrowDown', stopPropagation: stop } as unknown as KeyboardEvent);
        expect(stop).toHaveBeenCalled();
    });

    it('Tab before typing closes the popup', () => {
        dir.startedTyping = false;
        dir.handleKeyDown({ key: 'Tab', stopPropagation: vi.fn() } as unknown as KeyboardEvent);
        expect(popup.closePopup).toHaveBeenCalled();
    });

    it('Tab after typing does not close the popup', () => {
        dir.startedTyping = true;
        dir.handleKeyDown({ key: 'Tab', stopPropagation: vi.fn() } as unknown as KeyboardEvent);
        expect(popup.closePopup).not.toHaveBeenCalled();
    });
});

describe('StartEditDirective', () => {
    let dir: StartEditDirective;
    let logger: LoggerService;

    beforeEach(() => {
        logger = { error: vi.fn() } as unknown as LoggerService;
        TestBed.configureTestingModule({});
        const injector = TestBed.inject(Injector);
        dir = runInInjectionContext(injector, () => new StartEditDirective(makeLoggerFactory(logger)));
    });

    it('logs an error when there is no host component', () => {
        vi.spyOn(dir, 'hostComponent').mockReturnValue(undefined as any);
        dir.onFocus({} as FocusEvent);
        expect(logger.error).toHaveBeenCalled();
    });

    it('calls startEdit when host, servoyApi and dataProviderID are present', () => {
        const startEdit = vi.fn();
        const host = { servoyApi: () => ({ startEdit }) } as unknown as ServoyBaseComponent<HTMLElement>;
        vi.spyOn(dir, 'hostComponent').mockReturnValue(host);
        vi.spyOn(dir, 'dataProviderID').mockReturnValue('myDP');
        dir.onFocus({} as FocusEvent);
        expect(startEdit).toHaveBeenCalledWith('myDP');
    });

    it('logs an error when dataProviderID is missing', () => {
        const host = { servoyApi: () => ({ startEdit: vi.fn() }) } as unknown as ServoyBaseComponent<HTMLElement>;
        vi.spyOn(dir, 'hostComponent').mockReturnValue(host);
        vi.spyOn(dir, 'dataProviderID').mockReturnValue(undefined as any);
        dir.onFocus({} as FocusEvent);
        expect(logger.error).toHaveBeenCalled();
    });
});

describe('AutosaveDirective', () => {
    it('calls autosave when the clicked target is the element itself', () => {
        const nativeElement = document.createElement('button');
        const callService = vi.fn();
        const service = { callService } as unknown as ServoyPublicService;
        const dir = new AutosaveDirective(service, { nativeElement } as any);
        dir.onClick(nativeElement);
        expect(callService).toHaveBeenCalledWith('applicationServerService', 'autosave', {}, true);
    });

    it('calls autosave when the clicked target is a direct child', () => {
        const nativeElement = document.createElement('div');
        const child = document.createElement('span');
        nativeElement.appendChild(child);
        const callService = vi.fn();
        const dir = new AutosaveDirective({ callService } as unknown as ServoyPublicService, { nativeElement } as any);
        dir.onClick(child);
        expect(callService).toHaveBeenCalled();
    });

    it('does not call autosave for unrelated targets', () => {
        const nativeElement = document.createElement('div');
        const callService = vi.fn();
        const dir = new AutosaveDirective({ callService } as unknown as ServoyPublicService, { nativeElement } as any);
        dir.onClick(document.createElement('p'));
        expect(callService).not.toHaveBeenCalled();
    });
});

describe('UploadDirective', () => {
    const buildUploadDir = (service: ServoyPublicService): UploadDirective => {
        TestBed.configureTestingModule({});
        const injector = TestBed.inject(Injector);
        return runInInjectionContext(injector, () => new UploadDirective(service));
    };

    it('ngOnInit resolves the upload url from the service', () => {
        const service = {
            generateUploadUrl: vi.fn().mockReturnValue('resources/upload/1/form/comp/dataProviderID'),
            showFileOpenDialog: vi.fn()
        } as unknown as ServoyPublicService;
        const dir = buildUploadDir(service);
        vi.spyOn(dir, 'formname').mockReturnValue('form');
        vi.spyOn(dir, 'componentName').mockReturnValue('comp');
        dir.ngOnInit();
        expect(service.generateUploadUrl).toHaveBeenCalledWith('form', 'comp', 'dataProviderID');
    });

    it('click shows the file-open dialog with the resolved url', () => {
        const showFileOpenDialog = vi.fn();
        const service = {
            generateUploadUrl: vi.fn().mockReturnValue('the-url'),
            showFileOpenDialog
        } as unknown as ServoyPublicService;
        const dir = buildUploadDir(service);
        vi.spyOn(dir, 'formname').mockReturnValue('form');
        vi.spyOn(dir, 'componentName').mockReturnValue('comp');
        dir.ngOnInit();
        dir.click(new Event('click'));
        expect(showFileOpenDialog).toHaveBeenCalledWith('Please select a file', false, null, 'the-url');
    });
});
