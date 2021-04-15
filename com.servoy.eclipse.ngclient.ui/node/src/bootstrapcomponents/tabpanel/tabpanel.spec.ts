import { ComponentFixture, TestBed, waitForAsync, tick, fakeAsync } from '@angular/core/testing';
import { SimpleChange } from '@angular/core';

import { ServoyBootstrapTabpanel } from './tabpanel';
import { Tab } from '../bts_basetabpanel';
import { ServoyPublicModule, WindowRefService } from '@servoy/public';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { ServoyApi } from '@servoy/public';
import { By } from '@angular/platform-browser';
import { runOnPushChangeDetection } from '../../testing';

describe('TabpanelComponent', () => {
    let component: ServoyBootstrapTabpanel;
    let fixture: ComponentFixture<ServoyBootstrapTabpanel>;
    const servoyApi: jasmine.SpyObj<ServoyApi> = jasmine.createSpyObj<ServoyApi>('ServoyApi', ['getMarkupId', 'formWillShow', 'hideForm', 'trustAsHtml', 'registerComponent', 'unRegisterComponent']);

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [ServoyBootstrapTabpanel],
            imports: [NgbModule, ServoyTestingModule, ServoyPublicModule],
            providers: [WindowRefService]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(ServoyBootstrapTabpanel);
        component = fixture.componentInstance;
        component.servoyApi = servoyApi;
        servoyApi.formWillShow.and.returnValue(Promise.resolve(true));
        servoyApi.hideForm.and.returnValue(Promise.resolve(true));
        const tabs = [];
        let tab = new Tab();
        tab.name = 'tab1';
        tab.containedForm = 'form1';
        tab.text = 'tab1';
        tabs[0] = tab;
        tab = new Tab();
        tab.name = 'tab2';
        tab.containedForm = 'form2';
        tab.text = 'tab2';
        tabs[1] = tab;

        component.tabs = tabs;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeDefined();
    });

    it('should handle tabs', fakeAsync(() => {
        let clickSpy = jasmine.createSpy('onTabClickedMethodID');
        clickSpy.and.returnValue(Promise.resolve(true));
        component.onTabClickedMethodID = clickSpy;
        component.onChangeMethodID = jasmine.createSpy('onChangeMethodID');
        let tabs = fixture.debugElement.queryAll((By.css('.nav-link')));
        expect(tabs.length).toBe(2);
        expect(tabs[0].nativeElement.textContent).toBe('tab1');
        expect(tabs[1].nativeElement.textContent).toBe('tab2');
        tabs[1].children[0].triggerEventHandler('click', { target: tabs[1].children[0].nativeElement });
        tick();
        expect(component.onTabClickedMethodID).toHaveBeenCalled();
        expect(component.onChangeMethodID).toHaveBeenCalled();
        expect(component.tabIndex).toBe(2);

        component.tabIndex = 1;
        component.svyOnChanges({ 'tabIndex': new SimpleChange(2, 1, false) });
        tick();
        expect(component.onChangeMethodID).toHaveBeenCalledTimes(2);
    }));

    it('should handle tabs edit', async () => {
        component.svyOnChanges({ 'tabs': new SimpleChange(null, component.tabs, true) });
        await runOnPushChangeDetection(fixture);
        expect(component.tabIndex).toBe(1);
        component.selectTabAt(1);
        await runOnPushChangeDetection(fixture);
        expect(component.tabIndex).toBe(2);

        let tab = new Tab();
        tab.name = 'tab3';
        tab.containedForm = 'form3';
        tab.text = 'tab3';
        component.tabs.push(tab);
        component.svyOnChanges({ 'tabs': new SimpleChange(null, component.tabs, false) });
        await runOnPushChangeDetection(fixture);

        let tabs = fixture.debugElement.queryAll((By.css('.nav-link')));
        expect(tabs.length).toBe(3);
        expect(tabs[0].nativeElement.textContent).toBe('tab1');
        expect(tabs[1].nativeElement.textContent).toBe('tab2');
        expect(tabs[2].nativeElement.textContent).toBe('tab3');
        expect(component.tabIndex).toBe(2);

        component.tabs.splice(1, 1);
        component.svyOnChanges({ 'tabs': new SimpleChange(null, component.tabs, false) });
        await runOnPushChangeDetection(fixture);
        tabs = fixture.debugElement.queryAll((By.css('.nav-link')));
        expect(tabs.length).toBe(2);
        expect(tabs[0].nativeElement.textContent).toBe('tab1');
        expect(tabs[1].nativeElement.textContent).toBe('tab3');
        expect(component.tabIndex).toBe(2);
        
        component.tabs.splice(0, 1);
        component.svyOnChanges({ 'tabs': new SimpleChange(null, component.tabs, false) });
        await runOnPushChangeDetection(fixture);
        tabs = fixture.debugElement.queryAll((By.css('.nav-link')));
        expect(tabs.length).toBe(1);
        expect(tabs[0].nativeElement.textContent).toBe('tab3');
    });

});
