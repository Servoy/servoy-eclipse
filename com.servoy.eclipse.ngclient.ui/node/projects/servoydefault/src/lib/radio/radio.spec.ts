import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyDefaultRadio } from './radio';

describe('ImageLabelComponent', () => {
  let component: ServoyDefaultRadio;
  let fixture: ComponentFixture<ServoyDefaultRadio>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultRadio ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultRadio);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  xit('should create', () => {
    expect(component).toBeTruthy();
  });
});
