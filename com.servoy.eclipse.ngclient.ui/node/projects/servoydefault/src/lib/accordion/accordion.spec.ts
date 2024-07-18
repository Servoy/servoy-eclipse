import { ComponentFixture, TestBed, waitForAsync, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';

import { ServoyDefaultAccordion } from './accordion';
import { ServoyPublicTestingModule, WindowRefService, ServoyApi } from '@servoy/public'
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { By } from '@angular/platform-browser';
import { Tab } from '../tabpanel/basetabpanel';
import { SimpleChange } from '@angular/core';
import { runOnPushChangeDetection}  from '../testingutils';

describe('ServoyDefaultAccordion', () => {
    let component: ServoyDefaultAccordion;
    let fixture: ComponentFixture<ServoyDefaultAccordion>;
    const servoyApi: jasmine.SpyObj<ServoyApi> = jasmine.createSpyObj<ServoyApi>('ServoyApi', ['getMarkupId', 'formWillShow', 'hideForm', 'trustAsHtml', 'registerComponent', 'unRegisterComponent']);

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [ServoyDefaultAccordion],
            imports: [NgbModule, ServoyPublicTestingModule],
            providers: [WindowRefService]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(ServoyDefaultAccordion);
        component = fixture.componentInstance;
        component.servoyApi = servoyApi;
        servoyApi.formWillShow.and.returnValue(Promise.resolve(true));
        servoyApi.hideForm.and.returnValue(Promise.resolve(true));
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
    
    it('should handle tabs', fakeAsync(() => {
        component.onChangeMethodID = jasmine.createSpy('onChangeMethodID');
        let tabs = fixture.debugElement.queryAll((By.css('button')));
        expect(tabs.length).toBe(3);
        expect(tabs[0].nativeElement.textContent.trim()).toBe('tab1');
        expect(tabs[1].nativeElement.textContent.trim()).toBe('tab2');
        expect(tabs[2].nativeElement.textContent.trim()).toBe('tab3');
        tabs[1].triggerEventHandler('click', { target: tabs[1].nativeElement });
        tick();
        expect(component.onChangeMethodID).toHaveBeenCalled();
        expect(component.tabIndex).toBe(2);

        component.tabIndex = 1;
        component.svyOnChanges({ 'tabIndex': new SimpleChange(2, 1, false) });
        tick();
        expect(component.onChangeMethodID).toHaveBeenCalledTimes(2);
        discardPeriodicTasks()
        console.log("test")
    }));
    
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
