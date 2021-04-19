import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyBootstrapCalendar } from './calendar';

import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { SabloModule } from '../../sablo/sablo.module';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { OwlDateTimeIntl, OwlDateTimeModule, OwlNativeDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { Renderer2 } from '@angular/core';
import { LocaleService, I18NProvider } from '../../ngclient/servoy_public';
import { SabloService } from '../../sablo/sablo.service';

describe('CalendarComponent', () => {
  let component: ServoyBootstrapCalendar;
  let fixture: ComponentFixture<ServoyBootstrapCalendar>;
  let i18nProvider;

  beforeEach(() => {
    i18nProvider = jasmine.createSpyObj('I18NProvider', ['getI18NMessages']);
    const promise = Promise.resolve({});
    i18nProvider.getI18NMessages.and.returnValue(promise);  TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapCalendar ],
      imports: [BrowserModule, SabloModule, ServoyPublicModule, OwlDateTimeModule, FormsModule, OwlNativeDateTimeModule],
      providers: [Renderer2, FormsModule, { provide: LocaleService, useValue: {getLocale: () => 'en' } }, { provide: I18NProvider, useValue: i18nProvider },
      OwlDateTimeIntl, SabloService]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapCalendar);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml', 'startEdit','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
