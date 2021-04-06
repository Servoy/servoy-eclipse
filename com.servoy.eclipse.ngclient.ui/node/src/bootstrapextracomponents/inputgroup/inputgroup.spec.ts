import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapExtraInputGroup } from './inputgroup';

describe('FileUploadComponent', () => {
  let component: ServoyBootstrapExtraInputGroup;
  let fixture: ComponentFixture<ServoyBootstrapExtraInputGroup>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapExtraInputGroup ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapExtraInputGroup);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  xit('should create', () => {
    expect(component).toBeTruthy();
  });
});
