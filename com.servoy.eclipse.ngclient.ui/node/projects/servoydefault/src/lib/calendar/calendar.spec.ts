import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Format, ServoyPublicService, TooltipDirective, SabloTabseq, StartEditDirective,
         FormatDirective, FormattingService, TooltipService } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';

import { By, BrowserModule } from '@angular/platform-browser';
import { ServoyDefaultCalendar } from './calendar';
import { Renderer2 } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { runOnPushChangeDetection } from '../testingutils';

describe('ServoyDefaultCalendar', () => {
    let component: ServoyDefaultCalendar;
    let fixture: ComponentFixture<ServoyDefaultCalendar>;

    beforeEach(() => {
        TestBed.configureTestingModule({
      imports: [ServoyDefaultCalendar, TooltipDirective, SabloTabseq, StartEditDirective, FormatDirective, BrowserModule, FormsModule],
            providers: [Renderer2, FormattingService, TooltipService,
                        { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(ServoyDefaultCalendar);
        component = fixture.componentInstance;
        component.servoyApi =  { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), startEdit: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any;
        const fmt = new Format();
        fmt.type = 'DATETIME';
        fmt.display = 'dd-MM-yyyy';
        fixture.componentRef.setInput('format', fmt);
        fixture.detectChanges();
      });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should have called servoyApi.getMarkupId', () => {
        expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
    });

    it('should be showing a formatted a date', async () => {
        component.dataProviderID.set(new Date(2020, 10, 10));
        runOnPushChangeDetection(fixture);
        fixture.whenStable().then(() => {
            const input = fixture.debugElement.query(By.css('input'));
            const el = input.nativeElement;
            expect(el.value).toBe('10-11-2020');
        });
    });
});
