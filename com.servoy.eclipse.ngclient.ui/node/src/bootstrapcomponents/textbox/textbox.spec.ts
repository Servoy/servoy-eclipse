import { ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import { ServoyBootstrapTextbox } from './textbox';
import { Format, FormattingService, TooltipService } from '@servoy/public';
import { ServoyPublicModule } from '@servoy/public';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { runOnPushChangeDetection } from '../../testing';
import { LocaleService } from '../../ngclient/locale.service';
import { I18NProvider } from '../../ngclient/services/i18n_provider.service';
import { ServoyTestingModule } from '../../testing/servoytesting.module';

describe('TextboxComponent', () => {
    let component: ServoyBootstrapTextbox;
    let fixture: ComponentFixture<ServoyBootstrapTextbox>;
    let textField;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapTextbox ],
      imports: [ServoyTestingModule, ServoyPublicModule, FormsModule],
      providers: [I18NProvider, FormattingService, TooltipService, LocaleService ]
    })
    .compileComponents();
  }));

    beforeEach(() => {
        fixture = TestBed.createComponent(ServoyBootstrapTextbox);
        textField = fixture.debugElement.query(By.css('input'));
        component = fixture.componentInstance;
        component.servoyApi = jasmine.createSpyObj('ServoyApi', ['getMarkupId', 'trustAsHtml', 'startEdit', 'registerComponent', 'unRegisterComponent']);
        component.inputType = 'text';
        component.format = new Format();
        component.format.type = 'TEXT';
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeDefined();
    });

    it('should call update method', () => {
        spyOn(component, 'pushUpdate');
        textField.nativeElement.dispatchEvent(new Event('change'));
        expect(component.pushUpdate).toHaveBeenCalled();
    });

    it('onfocusgained and lost handlers need to be called', () => {
        component.onFocusGainedMethodID = jasmine.createSpy('onFocusGainedMethodID');
        component.onFocusLostMethodID = jasmine.createSpy('onFocusLostMethodID');
        component.attachFocusListeners(component.getFocusElement());
        textField.triggerEventHandler('focus', null);
        expect(component.onFocusGainedMethodID).toHaveBeenCalled();
        expect(component.onFocusLostMethodID).toHaveBeenCalledTimes(0);
        textField.triggerEventHandler('blur', null);
        expect(component.onFocusLostMethodID).toHaveBeenCalledTimes(1);
        component.requestFocus(true);
        // not reliable outcome, how to fix this?

        //expect(component.onFocusGainedMethodID).toHaveBeenCalledTimes(2);
        component.requestFocus(false);
        //expect(component.onFocusGainedMethodID).toHaveBeenCalledTimes(2);
    });

    it('onaction and onrightclick handlers need to be called', () => {
        component.onActionMethodID = jasmine.createSpy('onActionMethodID');
        component.onRightClickMethodID = jasmine.createSpy('onRightClickMethodID');
        component.svyOnInit();
        textField.triggerEventHandler('contextmenu', null);
        expect(component.onRightClickMethodID).toHaveBeenCalled();
        textField.triggerEventHandler('keydown', { keyCode: 13 });
        expect(component.onActionMethodID).toHaveBeenCalled();
    });

    it('should apply inputType', () => {
        let ret = component.setInputType('tel');
        expect(ret).toBeTrue();
        ret = component.setInputType('wrong_value');
        expect(ret).toBeFalse();
        fixture.detectChanges();
        expect(textField.nativeElement.type).toBe('tel');
    });

    it('should apply dataprovider to UI', async () => {
        component.dataProviderID = 'mytest';
        await  runOnPushChangeDetection(fixture);
        expect(component.getNativeElement().value).toBe('mytest');
        component.dataProviderID = 'mytest2';
        await runOnPushChangeDetection(fixture);
        expect(component.getNativeElement().value).toBe('mytest2');
    });

     it('should apply dataprovider from UI', async () => {
        component.getNativeElement().value = 'uitest';
        textField.nativeElement.dispatchEvent(new Event('change'));
        await runOnPushChangeDetection(fixture);
        expect(component.dataProviderID).toBe('uitest');
    });
});
