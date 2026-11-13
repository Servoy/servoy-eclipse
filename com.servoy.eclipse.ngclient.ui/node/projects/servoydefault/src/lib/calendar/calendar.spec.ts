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
        fixture.componentRef.setInput('servoyApi', { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), startEdit: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any);
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
        expect( component.servoyApi().getMarkupId ).toHaveBeenCalled();
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

    it('getNativeChild and getFocusElement return the inner input element', () => {
        expect(component.getNativeChild()).toBe(component.getFocusElement());
        expect(component.getFocusElement().tagName).toBe('INPUT');
    });

    describe('dateChanged', () => {
        it('sets the dataProviderID to the event date for a change.td event and emits the change', () => {
            const emitSpy = vi.spyOn(component.dataProviderIDChange, 'emit');
            const date = new Date(2021, 5, 15);
            component.dataProviderID.set(null);
            component.dateChanged({ type: 'change.td', date });
            expect(component.dataProviderID()).toBe(date);
            expect(emitSpy).toHaveBeenCalled();
        });

        it('clears the dataProviderID for a non change.td event', () => {
            component.dataProviderID.set(new Date());
            component.dateChanged({ type: 'other' });
            expect(component.dataProviderID()).toBeNull();
        });

        it('does nothing (no change) when the new date equals the current one', () => {
            const date = new Date(2021, 5, 15);
            component.dataProviderID.set(date);
            const emitSpy = vi.spyOn(component.dataProviderIDChange, 'emit');
            component.dateChanged({ type: 'change.td', date: new Date(2021, 5, 15) });
            // same time -> early return before pushUpdate
            expect(emitSpy).not.toHaveBeenCalled();
        });
    });

    describe('modelChange', () => {
        it('sets the dataProviderID and emits in findmode', () => {
            fixture.componentRef.setInput('findmode', true);
            fixture.detectChanges();
            const emitSpy = vi.spyOn(component.dataProviderIDChange, 'emit');
            component.modelChange('searchText');
            expect(component.dataProviderID()).toBe('searchText');
            expect(emitSpy).toHaveBeenCalled();
        });

        it('reverts the format directive value for an invalid date when not in findmode', () => {
            fixture.componentRef.setInput('findmode', false);
            fixture.detectChanges();
            const writeSpy = vi.spyOn(component.svyFormat()!, 'writeValue');
            component.dataProviderID.set(new Date(2020, 0, 1));
            component.modelChange('Invalid Date');
            expect(writeSpy).toHaveBeenCalledWith(component.dataProviderID());
        });
    });
});
