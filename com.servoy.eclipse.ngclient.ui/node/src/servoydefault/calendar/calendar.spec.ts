import { async, ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyPublicModule } from 'servoy-public';

import { LocaleService } from '../../ngclient/locale.service';
import { I18NProvider } from '../../ngclient/services/i18n_provider.service';

import { Format} from 'servoy-public';
import { By, BrowserModule } from '@angular/platform-browser';
import { ServoyDefaultCalendar } from './calendar';
import { OwlDateTimeIntl, OwlDateTimeModule, OwlNativeDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { Renderer2 } from '@angular/core';
import { FormsModule } from '@angular/forms';
const moment = require('moment');

import { runOnPushChangeDetection } from '../../testing';
import { ServoyTestingModule } from '../../testing/servoytesting.module';

describe('ServoyDefaultCalendar', () => {
    let component: ServoyDefaultCalendar;
    let fixture: ComponentFixture<ServoyDefaultCalendar>;
    let i18nProvider;
    // let dateTimeAdapter;
   // let owlDateTimeIntl;

    beforeEach(() => {
        i18nProvider = jasmine.createSpyObj('I18NProvider', ['getI18NMessages']);
        const promise = Promise.resolve({});
        i18nProvider.getI18NMessages.and.returnValue(promise);

        (<any>window).moment = moment;

        TestBed.configureTestingModule({
            declarations: [ServoyDefaultCalendar],
            imports: [ServoyTestingModule, BrowserModule, ServoyPublicModule, OwlDateTimeModule, FormsModule, OwlNativeDateTimeModule],
            providers: [Renderer2, FormsModule, { provide: LocaleService, useValue: {getLocale: () => 'en' } }, { provide: I18NProvider, useValue: i18nProvider },
                OwlDateTimeIntl]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(ServoyDefaultCalendar);
        component = fixture.componentInstance;
        component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId', 'trustAsHtml', 'startEdit','registerComponent','unRegisterComponent']);
        component.format = new Format();
        component.format.type = 'DATETIME';
        component.format.display = 'dd-MM-yyyy';
        fixture.detectChanges();
      });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have called servoyApi.getMarkupId', () => {
        expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
    });

    it('should be showing a formatted a date', waitForAsync(() => {
        component.dataProviderID = new Date(2020, 10, 10);
        runOnPushChangeDetection(fixture);
        fixture.whenStable().then(() => {
            const input = fixture.debugElement.query(By.css('input'));
            const el = input.nativeElement;
            expect(el.value).toBe('10-11-2020');
        });
    }));
});

