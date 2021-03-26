import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { SvyChartJS } from './chart';

describe('SvyChartJS', () => {
  let component: SvyChartJS;
  let fixture: ComponentFixture<SvyChartJS>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ SvyChartJS ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SvyChartJS);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  xit('should create', () => {
    expect(component).toBeTruthy();
  });
});
