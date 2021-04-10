import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyBootstrapCalendar } from './calendar';

import { ServoyPublicModule } from 'servoy-public';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { OwlDateTimeIntl, OwlDateTimeModule, OwlNativeDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { Renderer2 } from '@angular/core';
import { LocaleService } from '../../ngclient/locale.service';
import { I18NProvider } from '../../ngclient/services/i18n_provider.service';

describe('CalendarComponent', () => {
  let component: ServoyBootstrapCalendar;
  let fixture: ComponentFixture<ServoyBootstrapCalendar>;
  let i18nProvider;

  beforeEach(() => {
    i18nProvider = jasmine.createSpyObj('I18NProvider', ['getI18NMessages']);
    const promise = Promise.resolve({});
    i18nProvider.getI18NMessages.and.returnValue(promise);  TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapCalendar ],
      imports: [ServoyTestingModule,BrowserModule, ServoyPublicModule, OwlDateTimeModule, FormsModule, OwlNativeDateTimeModule],
      providers: [Renderer2, FormsModule, { provide: LocaleService, useValue: {getLocale: () => 'en' } }, { provide: I18NProvider, useValue: i18nProvider },
      OwlDateTimeIntl]
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
