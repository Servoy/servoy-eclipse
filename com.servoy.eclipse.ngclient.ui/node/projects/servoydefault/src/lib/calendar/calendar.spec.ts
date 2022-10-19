import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { Format, ServoyPublicTestingModule } from '@servoy/public';

import { By, BrowserModule } from '@angular/platform-browser';
import { ServoyDefaultCalendar } from './calendar';
import { Renderer2 } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { runOnPushChangeDetection } from '../testingutils';

describe('ServoyDefaultCalendar', () => {
    let component: ServoyDefaultCalendar;
    let fixture: ComponentFixture<ServoyDefaultCalendar>;
    // let dateTimeAdapter;
   // let owlDateTimeIntl;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [ServoyDefaultCalendar],
            imports: [ServoyPublicTestingModule, BrowserModule,  FormsModule],
            providers: [Renderer2, FormsModule]
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

