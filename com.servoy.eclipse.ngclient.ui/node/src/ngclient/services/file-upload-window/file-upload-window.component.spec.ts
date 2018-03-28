import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FileUploadWindowComponent } from './file-upload-window.component';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { HttpClient } from '@angular/common/http';

describe('FileUploadWindowComponent', () => {
  let component: FileUploadWindowComponent;
  let fixture: ComponentFixture<FileUploadWindowComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ FileUploadWindowComponent ],
      providers: [ { provide: NgbActiveModal }, { provide: HttpClient }]

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
