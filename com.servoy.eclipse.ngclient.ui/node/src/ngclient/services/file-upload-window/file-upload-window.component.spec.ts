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
});
