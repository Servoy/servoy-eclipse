import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { FileUploadWindowComponent } from './file-upload-window.component';
import { HttpClient, HttpHandler } from '@angular/common/http';
import { I18NProvider } from '../i18n_provider.service';
import { I18NListener } from '@servoy/public';

describe('FileUploadWindowComponent', () => {
  let component: FileUploadWindowComponent;
  let fixture: ComponentFixture<FileUploadWindowComponent>;
  let i18nProvider: any;
  const i18n: I18NListener = {
      messages: () =>i18n,
      destroy: () =>{}
  };
  beforeEach(waitForAsync(() => {
    i18nProvider = jasmine.createSpyObj('I18NProvider',['getI18NMessages', 'listenForI18NMessages']);
    const promise = Promise.resolve({});
    i18nProvider.getI18NMessages.and.returnValue(promise);
    i18nProvider.listenForI18NMessages.and.returnValue(i18n);

    TestBed.configureTestingModule({
      declarations: [ FileUploadWindowComponent ],
      providers: [ { provide: HttpClient }, {provide:HttpHandler}, {provide: I18NProvider, useValue: i18nProvider }]

    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FileUploadWindowComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('parsedFilter computed signal', () => {
    it('should return the original filter and maxUploadFileSize 0 when no maxUploadFileSize is present', () => {
      component.filter.set('.jpg,.png');
      const result = component.parsedFilter();
      expect(result.acceptFilter).toBe('.jpg,.png');
      expect(result.maxUploadFileSize).toBe(0);
    });

    it('should return acceptFilter without maxUploadFileSize entry and extract the size value', () => {
      component.filter.set('.jpg,.png,maxUploadFileSize=2048');
      const result = component.parsedFilter();
      expect(result.acceptFilter).toBe('.jpg,.png');
      expect(result.maxUploadFileSize).toBe(2048);
    });

    it('should handle filter with only maxUploadFileSize entry', () => {
      component.filter.set('maxUploadFileSize=512');
      const result = component.parsedFilter();
      expect(result.acceptFilter).toBe('');
      expect(result.maxUploadFileSize).toBe(512);
    });

    it('should return maxUploadFileSize 0 and acceptFilter undefined when filter is undefined', () => {
      component.filter.set(undefined);
      const result = component.parsedFilter();
      expect(result.acceptFilter).toBeUndefined();
      expect(result.maxUploadFileSize).toBe(0);
    });

    it('should ignore a non-numeric maxUploadFileSize value', () => {
      component.filter.set('.pdf,maxUploadFileSize=abc');
      const result = component.parsedFilter();
      expect(result.acceptFilter).toBe('.pdf');
      expect(result.maxUploadFileSize).toBe(0);
    });

    it('should recompute when filter signal changes', () => {
      component.filter.set('.jpg,maxUploadFileSize=1024');
      expect(component.parsedFilter().maxUploadFileSize).toBe(1024);

      component.filter.set('.png,maxUploadFileSize=2048');
      expect(component.parsedFilter().maxUploadFileSize).toBe(2048);
      expect(component.parsedFilter().acceptFilter).toBe('.png');
    });
  });

  describe('getAcceptFilter', () => {
    it('should return the cleaned filter without maxUploadFileSize', () => {
      component.filter.set('.jpg,.png,maxUploadFileSize=1000');
      expect(component.getAcceptFilter()).toBe('.jpg,.png');
    });

    it('should return original filter when no maxUploadFileSize is present', () => {
      component.filter.set('.jpg,.png');
      expect(component.getAcceptFilter()).toBe('.jpg,.png');
    });

    it('should not throw when called during template rendering (NG0600 regression)', () => {
      component.filter.set('image/*,maxUploadFileSize=2048');
      expect(() => fixture.detectChanges()).not.toThrow();
    });
  });

  describe('fileChange and oversized file detection', () => {
    const makeFile = (name: string, sizeKB: number): File =>
      new File([new ArrayBuffer(sizeKB * 1024)], name, { type: 'application/octet-stream' });

    const makeFileChangeEvent = (files: File[]): Event => {
      const dataTransfer = new DataTransfer();
      files.forEach(f => dataTransfer.items.add(f));
      const input = document.createElement('input');
      input.type = 'file';
      Object.defineProperty(input, 'files', { value: dataTransfer.files });
      return { target: input } as unknown as Event;
    };

    it('should not mark file as oversized when no maxUploadFileSize is set', () => {
      component.filter.set('.jpg');
      const event = makeFileChangeEvent([makeFile('photo.jpg', 500)]);
      component.fileChange(event);
      expect(component.oversizedFiles.size).toBe(0);
    });

    it('should mark file as oversized when it exceeds maxUploadFileSize', () => {
      component.filter.set('.jpg,maxUploadFileSize=100');
      const event = makeFileChangeEvent([makeFile('big.jpg', 200)]);
      component.fileChange(event);
      expect(component.oversizedFiles.has('big.jpg')).toBeTrue();
    });

    it('should not mark file as oversized when it is within maxUploadFileSize', () => {
      component.filter.set('.jpg,maxUploadFileSize=500');
      const event = makeFileChangeEvent([makeFile('small.jpg', 200)]);
      component.fileChange(event);
      expect(component.oversizedFiles.has('small.jpg')).toBeFalse();
    });

    it('should still add oversized files to uploadFiles for display', () => {
      component.filter.set('maxUploadFileSize=10');
      const event = makeFileChangeEvent([makeFile('huge.jpg', 500)]);
      component.fileChange(event);
      expect(component.uploadFiles.length).toBe(1);
      expect(component.oversizedFiles.has('huge.jpg')).toBeTrue();
    });
  });

  describe('getDisplayName', () => {
    it('should return plain name for valid files', () => {
      component.filter.set('maxUploadFileSize=100');
      const file = new File([], 'valid.jpg');
      expect(component.getDisplayName(file)).toBe('valid.jpg');
    });

    it('should append size info for oversized files', () => {
      component.filter.set('maxUploadFileSize=100');
      component.oversizedFiles.add('big.jpg');
      const file = new File([], 'big.jpg');
      expect(component.getDisplayName(file)).toBe('big.jpg ( > 100 KB )');
    });
  });

  describe('isFileValidForUpload', () => {
    it('should return true for files not in oversizedFiles', () => {
      const file = new File([], 'ok.pdf');
      expect(component.isFileValidForUpload(file)).toBeTrue();
    });

    it('should return false for files in oversizedFiles', () => {
      component.oversizedFiles.add('toobig.pdf');
      const file = new File([], 'toobig.pdf');
      expect(component.isFileValidForUpload(file)).toBeFalse();
    });
  });
});

