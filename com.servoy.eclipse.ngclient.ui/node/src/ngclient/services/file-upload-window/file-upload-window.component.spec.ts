import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { FileUploadWindowComponent } from './file-upload-window.component';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { HttpClient, HttpHandler } from '@angular/common/http';
import { I18NProvider } from '../../../ngclient/servoy_public';

describe('FileUploadWindowComponent', () => {
  let component: FileUploadWindowComponent;
  let fixture: ComponentFixture<FileUploadWindowComponent>;
  let i18nProvider: any;
  beforeEach(waitForAsync(() => {
    i18nProvider = jasmine.createSpyObj('I18NProvider',['getI18NMessages']);
    const promise = Promise.resolve({});
    i18nProvider.getI18NMessages.and.returnValue(promise);

    TestBed.configureTestingModule({
      declarations: [ FileUploadWindowComponent ],
      providers: [ { provide: NgbActiveModal }, { provide: HttpClient }, {provide:HttpHandler}, {provide: I18NProvider, useValue: i18nProvider }]

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
