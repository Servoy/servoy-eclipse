import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyBootstrapExtraNavbar } from './navbar';

describe('ServoyBootstrapExtraNavbar', () => {
  let component: ServoyBootstrapExtraNavbar;
  let fixture: ComponentFixture<ServoyBootstrapExtraNavbar>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapExtraNavbar ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapExtraNavbar);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  xit('should create', () => {
    expect(component).toBeTruthy();
  });
});
