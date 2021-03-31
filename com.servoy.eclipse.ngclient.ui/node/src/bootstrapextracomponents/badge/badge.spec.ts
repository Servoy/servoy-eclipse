import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapExtraBadge } from './badge';

describe('FileUploadComponent', () => {
  let component: ServoyBootstrapExtraBadge;
  let fixture: ComponentFixture<ServoyBootstrapExtraBadge>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapExtraBadge ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapExtraBadge);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  xit('should create', () => {
    expect(component).toBeTruthy();
  });
});
