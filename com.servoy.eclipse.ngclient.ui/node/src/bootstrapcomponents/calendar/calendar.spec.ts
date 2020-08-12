import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapCalendar } from './calendar';

import { ServoyPublicModule } from '../../ngclient/servoy_public.module'
import { SabloModule } from '../../sablo/sablo.module'

describe('CalendarComponent', () => {
  let component: ServoyBootstrapCalendar;
  let fixture: ComponentFixture<ServoyBootstrapCalendar>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapCalendar ],
      imports: [ServoyPublicModule,SabloModule],
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
