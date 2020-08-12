import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapCalendarinline } from './calendarinline';

describe('CalendarinlineComponent', () => {
  let component: ServoyBootstrapCalendarinline;
  let fixture: ComponentFixture<ServoyBootstrapCalendarinline>;

  beforeEach(async(() => {
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
