import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyBootstrapCalendarinline } from './calendarinline';

describe('CalendarinlineComponent', () => {
  let component: ServoyBootstrapCalendarinline;
  let fixture: ComponentFixture<ServoyBootstrapCalendarinline>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapCalendarinline ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapCalendarinline);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  xit('should create', () => {
    expect(component).toBeTruthy();
  });
});
