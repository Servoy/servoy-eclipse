import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyBootstrapList } from './list';
import { ShowDisplayValuePipe } from '../lib/showDisplayValue.pipe';
import { IValuelist } from '../../sablo/spectypes.service';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { ServoyPublicModule } from '@servoy/public';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { runOnPushChangeDetection } from '../../testing';
import { SimpleChange } from '@angular/core';

const mockData = [
    {
        realValue: 1,
        displayValue: 'Bucharest'
    },
    {
        realValue: 2,
        displayValue: 'Timisoara'
    },
    {
        realValue: 3,
        displayValue: 'Amsterdam'
    },
] as IValuelist;

describe('ServoyBootstrapList', () => {
    let component: ServoyBootstrapList;
    let fixture: ComponentFixture<ServoyBootstrapList>;
    let textField;

    beforeEach(waitForAsync(() => {

        TestBed.configureTestingModule({
            declarations: [ServoyBootstrapList, ShowDisplayValuePipe],
            imports: [ServoyTestingModule, ServoyPublicModule]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(ServoyBootstrapList);
        component = fixture.componentInstance;
        component.servoyApi = jasmine.createSpyObj('ServoyApi', ['getMarkupId', 'trustAsHtml', 'registerComponent', 'unRegisterComponent']);
        component.valuelistID = mockData;
        mockData.getDisplayValue = (value) => { return of(value) };
        fixture.detectChanges();
        textField = fixture.debugElement.query(By.css('input'));
    });

    it('should create', () => {
        expect(component).toBeDefined();
    });

    it('should call update method', () => {
        spyOn(component, 'updateValue');
        textField.nativeElement.dispatchEvent(new Event('change'));
        expect(component.updateValue).toHaveBeenCalled();
    });

    it('handlers need to be called', () => {
        component.onFocusGainedMethodID = jasmine.createSpy('onFocusGainedMethodID');
        component.onFocusLostMethodID = jasmine.createSpy('onFocusLostMethodID');
        component.onActionMethodID = jasmine.createSpy('onActionMethodID');
        component.dataProviderID = 'Timisoara';
        component.svyOnInit();
        textField.triggerEventHandler('focus', null);
        expect(component.onFocusGainedMethodID).toHaveBeenCalled();
        expect(component.onFocusLostMethodID).toHaveBeenCalledTimes(0);
        textField.triggerEventHandler('blur', null);
        expect(component.onFocusLostMethodID).toHaveBeenCalledTimes(1);
        textField.triggerEventHandler('keydown', { keyCode: 13 });
        expect(component.onActionMethodID).toHaveBeenCalled();
    });

    it('should apply dataprovider to UI', async () => {
        component.dataProviderID = 2;
        component.svyOnChanges({
            dataProviderID: new SimpleChange(null, component.dataProviderID, true)
        });
        await runOnPushChangeDetection(fixture);
        expect(component.getNativeElement().value).toBe('Timisoara');
    });

    it('should apply dataprovider from UI', async () => {
        component.getNativeElement().value = 'Bucharest';
        textField.nativeElement.dispatchEvent(new Event('change'));
        await runOnPushChangeDetection(fixture);
        expect(component.dataProviderID).toBe(1);
    });

});
