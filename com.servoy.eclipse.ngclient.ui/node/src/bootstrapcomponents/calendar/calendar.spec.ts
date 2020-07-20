import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapCalendar } from './calendar';

describe('CalendarComponent', () => {
  let component: ServoyBootstrapCalendar;
  let fixture: ComponentFixture<ServoyBootstrapCalendar>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapCalendar ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapCalendar);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
