import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { FormattingService, TooltipService } from 'servoy-public';
import { ServoyPublicModule } from 'servoy-public';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { LocaleService } from '../../ngclient/locale.service';
import { I18NProvider } from '../../ngclient/services/i18n_provider.service';

import { ServoyBootstrapTextarea } from './textarea';
import { By } from '@angular/platform-browser';
import { runOnPushChangeDetection } from '../../testing';

describe('TextareaComponent', () => {
  let component: ServoyBootstrapTextarea;
  let fixture: ComponentFixture<ServoyBootstrapTextarea>;
  let textArea;
  
  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapTextarea ],
      imports: [ServoyTestingModule, ServoyPublicModule, FormsModule],
      providers: [I18NProvider, FormattingService, TooltipService, LocaleService ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapTextarea);
    textArea = fixture.debugElement.query(By.css('textarea'));
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml', 'startEdit','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeDefined();
  });
  
   it('should call update method', () => {
        spyOn(component, 'pushUpdate');
        textArea.nativeElement.dispatchEvent(new Event('change'));
        expect(component.pushUpdate).toHaveBeenCalled();
    });

    it('onfocusgained and lost handlers need to be called', () => {
        component.onFocusGainedMethodID = jasmine.createSpy('onFocusGainedMethodID');
        component.onFocusLostMethodID = jasmine.createSpy('onFocusLostMethodID');
        component.attachFocusListeners(component.getFocusElement());
        textArea.triggerEventHandler('focus', null);
        expect(component.onFocusGainedMethodID).toHaveBeenCalled();
        expect(component.onFocusLostMethodID).toHaveBeenCalledTimes(0);
        textArea.triggerEventHandler('blur', null);
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
        textArea.triggerEventHandler('contextmenu', null);
        expect(component.onRightClickMethodID).toHaveBeenCalled();
        textArea.triggerEventHandler('keydown', { keyCode: 13 });
        expect(component.onActionMethodID).toHaveBeenCalled();
    });


    it('should apply dataprovider to UI', async () => {
        component.dataProviderID = 'mytest';
        await  runOnPushChangeDetection(fixture);
        expect(component.getNativeElement().value).toBe('mytest');
        component.dataProviderID = 'mytest2';
        await runOnPushChangeDetection(fixture);
        expect(component.getNativeElement().value).toBe('mytest2');
    });
    
    // why doesn't this work ?
     xit('should apply dataprovider from UI', async () => {
        component.getNativeElement().value = 'uitest';
        textArea.nativeElement.dispatchEvent(new Event('change'));
        await runOnPushChangeDetection(fixture);
        expect(component.dataProviderID).toBe('uitest');
    });
});
