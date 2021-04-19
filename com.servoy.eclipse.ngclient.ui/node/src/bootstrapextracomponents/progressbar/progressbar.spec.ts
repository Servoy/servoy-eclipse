import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapExtraProgressBar } from './progressbar';

describe('FileUploadComponent', () => {
  let component: ServoyBootstrapExtraProgressBar;
  let fixture: ComponentFixture<ServoyBootstrapExtraProgressBar>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapExtraProgressBar ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapExtraProgressBar);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
