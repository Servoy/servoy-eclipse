import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyBootstrapChoicegroup, ChoiceElementDirective } from './choicegroup';
import { IValuelist } from '../../sablo/spectypes.service';
import { By } from '@angular/platform-browser';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { ServoyPublicModule, NotNullOrEmptyPipe } from '@servoy/public';
import { runOnPushChangeDetection } from '../../testing';
import { SimpleChange } from '@angular/core';

const mockData = [
    {
        realValue: 3,
        displayValue: 'Bucharest'
    },
    {
        realValue: 1,
        displayValue: 'Timisoara'
    },
    {
        realValue: 2,
        displayValue: 'Amsterdam'
    },
] as IValuelist;

describe('ChoicegroupComponent', () => {
    let component: ServoyBootstrapChoicegroup;
    let fixture: ComponentFixture<ServoyBootstrapChoicegroup>;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [ServoyBootstrapChoicegroup, ChoiceElementDirective],
            imports: [ServoyTestingModule, ServoyPublicModule],
            providers: [NotNullOrEmptyPipe]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(ServoyBootstrapChoicegroup);
        component = fixture.componentInstance;
        component.servoyApi = jasmine.createSpyObj('ServoyApi', ['getMarkupId', 'trustAsHtml', 'startEdit', 'registerComponent', 'unRegisterComponent']);
        component.enabled = true;
        component.readOnly = false;
        component.inputType = 'checkbox';
        component.valuelistID = mockData;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeDefined();
    });
    
     it('handlers need to be called', () => {
        let inputField = fixture.debugElement.query(By.css('input'));
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
    
     it('should apply dataprovider from UI', async () => {
        let inputFields = fixture.debugElement.queryAll(By.css('input'));
        inputFields[0].nativeElement.dispatchEvent(new Event('change'));
        inputFields[2].nativeElement.dispatchEvent(new Event('change'));
        await runOnPushChangeDetection(fixture);
        expect(component.dataProviderID).toBe('3\n2');
    });
    
    it('should apply dataprovider from UI (radio)', async () => {
        component.inputType = 'radio';
        await runOnPushChangeDetection(fixture);
        let inputFields = fixture.debugElement.queryAll(By.css('input'));
        inputFields[0].nativeElement.dispatchEvent(new Event('change'));
        inputFields[1].nativeElement.dispatchEvent(new Event('change'));
        await runOnPushChangeDetection(fixture);
        expect(component.dataProviderID).toBe(1);
    });
    
     it('should apply dataprovider to UI', async () => {
        component.dataProviderID = '3\n2';
        component.svyOnChanges({
            dataProviderID: new SimpleChange(null, component.dataProviderID, true)
        });
        await runOnPushChangeDetection(fixture);
        let inputFields = fixture.debugElement.queryAll(By.css('input'));
        expect(inputFields[0].nativeElement.checked).toBeTruthy();
        expect(inputFields[2].nativeElement.checked).toBeTruthy();
    });
});
