import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyBootstrapCheckbox } from './checkbox';
import { By } from '@angular/platform-browser';
import { runOnPushChangeDetection } from '../../testing';
import { SimpleChange } from '@angular/core';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { ServoyPublicModule } from '@servoy/public';

describe('ServoyBootstrapCheckbox', () => {
    let component: ServoyBootstrapCheckbox;
    let fixture: ComponentFixture<ServoyBootstrapCheckbox>;
    let inputField;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [ServoyBootstrapCheckbox],
            imports: [ServoyTestingModule, ServoyPublicModule],
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(ServoyBootstrapCheckbox);
        inputField = fixture.debugElement.query(By.css('input'));
        component = fixture.componentInstance;
        component.servoyApi = jasmine.createSpyObj('ServoyApi', ['getMarkupId', 'trustAsHtml', 'startEdit', 'registerComponent', 'unRegisterComponent']);
        component.enabled = true;
        component.readOnly = false;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeDefined();
    });

    it('handlers need to be called', () => {
        component.onFocusGainedMethodID = jasmine.createSpy('onFocusGainedMethodID');
        component.onFocusLostMethodID = jasmine.createSpy('onFocusLostMethodID');
        component.onActionMethodID = jasmine.createSpy('onActionMethodID');
        component.svyOnInit();
        inputField.triggerEventHandler('focus', null);
        expect(component.onFocusGainedMethodID).toHaveBeenCalled();
        expect(component.onFocusLostMethodID).toHaveBeenCalledTimes(0);
        inputField.triggerEventHandler('blur', null);
        expect(component.onFocusLostMethodID).toHaveBeenCalledTimes(1);
        inputField.triggerEventHandler('click', { target: inputField.nativeElement });
        expect(component.onActionMethodID).toHaveBeenCalled();
    });

    it('should call update method', () => {
        spyOn(component, 'pushUpdate');
        inputField.nativeElement.dispatchEvent(new Event('click'));
        expect(component.pushUpdate).toHaveBeenCalled();
    });

    it('should click change value', () => {
        expect(inputField.nativeElement.checked).toBeFalsy(); // default state
        inputField.nativeElement.click();

        fixture.detectChanges();
        expect(inputField.nativeElement.checked).toBeTruthy(); // state after click

        inputField.nativeElement.click();
        fixture.detectChanges();
        expect(inputField.nativeElement.checked).toBeFalsy();
    });

    it('should getSelectionFromDP', () => {
        component.dataProviderID = 1;
        expect(component.getSelectionFromDataprovider()).toBeTruthy();

        component.dataProviderID = '1';
        expect(component.getSelectionFromDataprovider()).toBeTruthy();

        component.dataProviderID = 0;
        expect(component.getSelectionFromDataprovider()).toBeFalsy();

        component.dataProviderID = '0';
        expect(component.getSelectionFromDataprovider()).toBeFalsy();

        component.dataProviderID = '';
        expect(component.getSelectionFromDataprovider()).toBeFalsy();

        component.dataProviderID = 'something';
        expect(component.getSelectionFromDataprovider()).toBeFalsy();

        component.dataProviderID = null;
        expect(component.getSelectionFromDataprovider()).toBeFalsy();

        component.dataProviderID = undefined;
        expect(component.getSelectionFromDataprovider()).toBeFalsy();
    });

    it('should be showing the dpid', async () => {
        const el = inputField.nativeElement;
        
        component.dataProviderID = true;
        component.svyOnChanges({
            dataProviderID: new SimpleChange(null, component.dataProviderID, true)
        });
        await runOnPushChangeDetection(fixture);
        expect(el.checked).toBeTruthy();
        
        component.dataProviderID = 0;
        component.svyOnChanges({
            dataProviderID: new SimpleChange(null, component.dataProviderID, true)
        });
        await runOnPushChangeDetection(fixture);
        expect(el.checked).toBeFalsy();
        
        component.dataProviderID = 1;
        component.svyOnChanges({
            dataProviderID: new SimpleChange(null, component.dataProviderID, true)
        });
        await runOnPushChangeDetection(fixture);
        expect(el.checked).toBeTruthy();
        
        component.dataProviderID = '0';
        component.svyOnChanges({
            dataProviderID: new SimpleChange(null, component.dataProviderID, true)
        });
        await runOnPushChangeDetection(fixture);
        expect(el.checked).toBeFalsy();
        
        component.dataProviderID = 'myvalue';
        component.selectedValue = 'myvalue';
        component.svyOnChanges({
            dataProviderID: new SimpleChange(null, component.dataProviderID, true)
        });
        await runOnPushChangeDetection(fixture);
        expect(el.checked).toBeTruthy();
        
        component.dataProviderID = 'myvalue2';
        component.svyOnChanges({
            dataProviderID: new SimpleChange(null, component.dataProviderID, true)
        });
        await runOnPushChangeDetection(fixture);
        expect(el.checked).toBeFalsy();
    });

});
