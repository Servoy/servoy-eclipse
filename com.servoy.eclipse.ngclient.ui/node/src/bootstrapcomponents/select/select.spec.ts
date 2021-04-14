import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyBootstrapSelect } from './select';
import { ShowDisplayValuePipe } from '../lib/showDisplayValue.pipe';
import { IValuelist } from '../../sablo/spectypes.service';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { ServoyPublicModule } from 'servoy-public';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { runOnPushChangeDetection } from '../../testing';

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

describe('ServoyBootstrapSelect', () => {
    let component: ServoyBootstrapSelect;
    let fixture: ComponentFixture<ServoyBootstrapSelect>;
    let selectField;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [ServoyBootstrapSelect, ShowDisplayValuePipe],
            imports: [ServoyTestingModule, ServoyPublicModule]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(ServoyBootstrapSelect);
        component = fixture.componentInstance;
        component.servoyApi = jasmine.createSpyObj('ServoyApi', ['getMarkupId', 'trustAsHtml', 'registerComponent', 'unRegisterComponent']);
        component.valuelistID = mockData;
        mockData.getDisplayValue = (value) => { return of(value) };
        component.multiselect = false;
        fixture.detectChanges();
        selectField = fixture.debugElement.query(By.css('select'));
    });

    it('should create', () => {
        expect(component).toBeDefined();
    });

    it('handlers need to be called', () => {
        component.onFocusGainedMethodID = jasmine.createSpy('onFocusGainedMethodID');
        component.onFocusLostMethodID = jasmine.createSpy('onFocusLostMethodID');
        component.onActionMethodID = jasmine.createSpy('onActionMethodID');
        component.dataProviderID = 'Timisoara';
        component.svyOnInit();
        selectField.triggerEventHandler('focus', null);
        expect(component.onFocusGainedMethodID).toHaveBeenCalled();
        expect(component.onFocusLostMethodID).toHaveBeenCalledTimes(0);
        selectField.triggerEventHandler('blur', null);
        expect(component.onFocusLostMethodID).toHaveBeenCalledTimes(1);
        selectField.triggerEventHandler('change', null);
        expect(component.onActionMethodID).toHaveBeenCalled();
    });

    it('should call update method', () => {
        component.dataProviderID = 'Timisoara';
        spyOn(component, 'updateValue');
        selectField.nativeElement.dispatchEvent(new Event('change'));
        expect(component.updateValue).toHaveBeenCalled();
    });

    it('should apply dataprovider from UI', async () => {
        component.dataProviderID = 'Amsterdam';
        selectField.value = 3;
        selectField.nativeElement.dispatchEvent(new Event('change'));
        await runOnPushChangeDetection(fixture);
        expect(component.dataProviderID).toBe(3);
    });
    
     it('should apply dataprovider to UI', async () => {
        component.dataProviderID = 3;
        await  runOnPushChangeDetection(fixture);
        expect(component.getNativeElement().value).toBe('3');
    });
});
