import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CommonModule } from '@angular/common';

import { ServoyDefaultAccordion } from './accordion';
import { WindowRefService, ServoyApi, ServoyPublicService, SabloTabseq } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { By } from '@angular/platform-browser';
import { Tab } from '../tabpanel/basetabpanel';
import { SimpleChange } from '@angular/core';
import { runOnPushChangeDetection}  from '../testingutils';

describe('ServoyDefaultAccordion', () => {
    let component: ServoyDefaultAccordion;
    let fixture: ComponentFixture<ServoyDefaultAccordion>;
    const servoyApi: any = { getMarkupId: vi.fn(), formWillShow: vi.fn(), hideForm: vi.fn(), trustAsHtml: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any;

    beforeEach(async () => {
        TestBed.configureTestingModule({
            imports: [ServoyDefaultAccordion, SabloTabseq, NgbModule, CommonModule],
      providers: [WindowRefService,
                        { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
        })
            .compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(ServoyDefaultAccordion);
        component = fixture.componentInstance;
        component.servoyApi = servoyApi;
        servoyApi.formWillShow.mockReturnValue(Promise.resolve(true));
        servoyApi.hideForm.mockReturnValue(Promise.resolve(true));
        const tabs = [];
        let tab = new Tab();
        tab.name = 'tab1';
        tab.containsFormId = 'form1';
        tab.text = 'tab1';
        tabs[0] = tab;
        tab = new Tab();
        tab.name = 'tab2';
        tab.containsFormId = 'form2';
        tab.text = 'tab2';
        tabs[1] = tab;
        tab = new Tab();
        tab.name = 'tab3';
        tab.containsFormId = 'form3';
        tab.text = 'tab3';
        tabs[2] = tab;
        component.tabs = tabs;
        fixture.detectChanges();
    });


    it('should create', () => {
        expect(component).toBeDefined();
    });
    
    it('should handle tabs', async () => {
        vi.useFakeTimers();
        component.onChangeMethodID = vi.fn();
        let tabs = fixture.debugElement.queryAll((By.css('button')));
        expect(tabs.length).toBe(3);
        expect(tabs[0].nativeElement.textContent.trim()).toBe('tab1');
        expect(tabs[1].nativeElement.textContent.trim()).toBe('tab2');
        expect(tabs[2].nativeElement.textContent.trim()).toBe('tab3');
        tabs[1].triggerEventHandler('click', { target: tabs[1].nativeElement });
        await vi.advanceTimersByTimeAsync(0);
        expect(component.onChangeMethodID).toHaveBeenCalled();
        expect(component.tabIndex).toBe(2);

        component.tabIndex = 1;
        component.svyOnChanges({ 'tabIndex': new SimpleChange(2, 1, false) });
        await vi.advanceTimersByTimeAsync(0);
        expect(component.onChangeMethodID).toHaveBeenCalledTimes(2);
        vi.useRealTimers();
        console.log("test")
    });
    
     it('should handle tabs edit', async () => {
        component.svyOnChanges({ 'tabs': new SimpleChange(null, component.tabs, true) });
        await runOnPushChangeDetection(fixture);
        expect(component.tabIndex).toBe(1);
        component.selectTabAt(1);
        await runOnPushChangeDetection(fixture);
        expect(component.tabIndex).toBe(2);

        let tab = new Tab();
        tab.name = 'tab4';
        tab.containsFormId = 'form4';
        tab.text = 'tab4';
        component.tabs.push(tab);
        component.svyOnChanges({ 'tabs': new SimpleChange(null, component.tabs, false) });
        await runOnPushChangeDetection(fixture);

        let tabs = fixture.debugElement.queryAll((By.css('button')));
        expect(tabs.length).toBe(4);
        expect(tabs[0].nativeElement.textContent.trim()).toBe('tab1');
        expect(tabs[1].nativeElement.textContent.trim()).toBe('tab2');
        expect(tabs[2].nativeElement.textContent.trim()).toBe('tab3');
        expect(tabs[3].nativeElement.textContent.trim()).toBe('tab4');
        expect(component.tabIndex).toBe(2);

        component.tabs.splice(1, 1);
        component.svyOnChanges({ 'tabs': new SimpleChange(null, component.tabs, false) });
        await runOnPushChangeDetection(fixture);
        tabs = fixture.debugElement.queryAll((By.css('button')));
        expect(tabs.length).toBe(3);
        expect(tabs[0].nativeElement.textContent.trim()).toBe('tab1');
        expect(tabs[1].nativeElement.textContent.trim()).toBe('tab3');
        expect(tabs[2].nativeElement.textContent.trim()).toBe('tab4');
        expect(component.tabIndex).toBe(2);
        
        component.tabs.splice(0, 1);
        component.svyOnChanges({ 'tabs': new SimpleChange(null, component.tabs, false) });
        await runOnPushChangeDetection(fixture);
        tabs = fixture.debugElement.queryAll((By.css('button')));
        expect(tabs.length).toBe(2);
        expect(tabs[0].nativeElement.textContent.trim()).toBe('tab3');
        expect(tabs[1].nativeElement.textContent.trim()).toBe('tab4');
    });
});
