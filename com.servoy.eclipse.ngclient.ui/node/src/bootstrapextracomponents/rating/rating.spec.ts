import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapExtraRating } from './rating';

describe('FileUploadComponent', () => {
  let component: ServoyBootstrapExtraRating;
  let fixture: ComponentFixture<ServoyBootstrapExtraRating>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapExtraRating ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapExtraRating);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
