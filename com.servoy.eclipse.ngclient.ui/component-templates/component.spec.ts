import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ##componentclassname## } from './##componentname##';

describe('##componentclassname##', () => {
  let component: ##componentclassname##;
  let fixture: ComponentFixture<##componentclassname##>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ##componentclassname## ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(##componentclassname##);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
