import { async, ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { SabloModule } from '../../sablo/sablo.module';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';


import { LocaleService, I18NProvider, Format} from '../../ngclient/servoy_public';
import { By, BrowserModule } from '@angular/platform-browser';
import { ServoyDefaultCalendar } from './calendar';
import { OwlDateTimeIntl, OwlDateTimeModule, OwlNativeDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { Renderer2 } from '@angular/core';
import { SabloService } from '../../sablo/sablo.service';
import { FormsModule } from '@angular/forms';
const moment = require('moment');

import { runOnPushChangeDetection } from '../../testing';

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
            imports: [BrowserModule, SabloModule, ServoyPublicModule, OwlDateTimeModule, FormsModule, OwlNativeDateTimeModule],
            providers: [Renderer2, FormsModule, { provide: LocaleService, useValue: {getLocale: () => 'en' } }, { provide: I18NProvider, useValue: i18nProvider },
                OwlDateTimeIntl, SabloService]
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

