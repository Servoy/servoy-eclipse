import { ComponentFixture, TestBed, waitForAsync, tick, fakeAsync } from '@angular/core/testing';

import { ServoyBootstrapTypeahead } from './typeahead';

import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { FormattingService, TooltipService } from '@servoy/public';
import { ServoyPublicModule } from '@servoy/public';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { runOnPushChangeDetection } from '../../testing';
import { IValuelist } from '../../sablo/spectypes.service';
import { of } from 'rxjs';
import { Format } from '@servoy/public';

const mockData = [
  {
    realValue: 1,
    displayValue: 'Bucuresti'
  },
  {
    realValue: 2,
    displayValue: 'Timisoara'
  },
  {
    realValue: 3,
    displayValue: 'Cluj'
  },
] as IValuelist;


describe('TypeaheadComponent', () => {
    let component: ServoyBootstrapTypeahead;
    let fixture: ComponentFixture<ServoyBootstrapTypeahead>;
    let textField;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [ServoyBootstrapTypeahead],
            providers: [FormattingService, TooltipService],
            imports: [ServoyPublicModule, ServoyTestingModule, NgbModule, FormsModule]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(ServoyBootstrapTypeahead);
        textField = fixture.debugElement.query(By.css('input'));
        component = fixture.componentInstance;
        component.servoyApi = jasmine.createSpyObj('ServoyApi', ['getMarkupId', 'trustAsHtml', 'startEdit', 'registerComponent', 'unRegisterComponent']);
        mockData.hasRealValues = () => { return true};
        component.valuelistID = mockData;
        component.format = new Format();
        component.format.type = 'TEXT';
        component.dataProviderID = 3;
        runOnPushChangeDetection(fixture)
    });

    it('should create', () => {
        expect(component).toBeDefined();
    });

    it('should open dropdown on container click', async () => {
        textField.triggerEventHandler('click', null);
        await runOnPushChangeDetection(fixture);
        expect(component.instance.isPopupOpen()).toBeTruthy();
    });


    it('should open dropdown on container focus', async() => {
        textField.triggerEventHandler('focus', null);
        await runOnPushChangeDetection(fixture);
        expect(component.instance.isPopupOpen()).toBeTruthy();
    });

    it('should call update method', () => {
        spyOn(component, 'pushUpdate');
        textField.nativeElement.dispatchEvent(new Event('change'));
        expect(component.pushUpdate).toHaveBeenCalled();
    });

    it('handlers need to be called', () => {
        component.onFocusGainedMethodID = jasmine.createSpy('onFocusGainedMethodID');
        component.onFocusLostMethodID = jasmine.createSpy('onFocusLostMethodID');
        component.onActionMethodID = jasmine.createSpy('onActionMethodID');
        component.svyOnInit();
        textField.triggerEventHandler('focus', null);
        expect(component.onFocusGainedMethodID).toHaveBeenCalled();
        expect(component.onFocusLostMethodID).toHaveBeenCalledTimes(0);
        textField.triggerEventHandler('blur', null);
        expect(component.onFocusLostMethodID).toHaveBeenCalledTimes(1);
        textField.triggerEventHandler('keydown', { keyCode: 13 });
        expect(component.onActionMethodID).toHaveBeenCalled();
    });

    it('should set initial list of values', (done) => {
        component.filterValues(of('')).subscribe(values => {
           expect(values).toEqual(mockData);
            done();
         });
    });
    
    it('should update ui', async () => {
        await runOnPushChangeDetection(fixture);
        expect(textField.nativeElement.value).toBe('Cluj');
        component.dataProviderID = 2;
        await runOnPushChangeDetection(fixture);
        expect(textField.nativeElement.value).toBe('Timisoara');
    });

});
