import {  ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyPublicTestingModule, FormattingService, TooltipService, Format, ServoyPublicService } from '@servoy/public';
import numbro from 'numbro';
import languages from 'numbro/dist/languages.min';
import { ServoyDefaultTextField } from './textfield';
import { By } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { runOnPushChangeDetection } from '../testingutils';

describe('ServoyDefaultTextField', () => {
  let component: ServoyDefaultTextField;
  let fixture: ComponentFixture<ServoyDefaultTextField>;
  let textField;
  let servoyPublicService;
   
  beforeAll(() => {
       numbro.registerLanguage(languages['en-GB']);
       numbro.registerLanguage(languages['nl-NL']);
       const lang = languages;
       const nl = lang['nl-NL'];
       console.log('numbro language', nl);
    });
    
  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultTextField],
      imports: [ServoyPublicTestingModule, FormsModule],
      providers: [FormattingService, TooltipService]
    })
    .compileComponents();
    servoyPublicService = TestBed.inject(ServoyPublicService);
  });

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(ServoyDefaultTextField);
    textField = fixture.debugElement.query(By.css('input'));
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId', 'trustAsHtml', 'startEdit','registerComponent','unRegisterComponent']);
    component.format = new Format();
    component.format.type = 'NUMBER';
    component.format.display = '#,###.00';
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have called servoyApi.getMarkupId', () => {
      expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
  });

  it('should use start edit directive', () => {
      textField.triggerEventHandler('focus', null);
      expect(component.servoyApi.startEdit).toHaveBeenCalled();
  });

  it('should have formatted value 1.000,00', () => {
      servoyPublicService.setLocale('nl', 'NL');
      numbro.setLanguage('nl-NL');
      component.dataProviderID = 1000;
      runOnPushChangeDetection(fixture);
      fixture.whenStable().then(() => {
         expect(component.getNativeElement().value).toBe('1.000,00');
      });
  });

  it('should call update method', () => {
    spyOn(component, 'pushUpdate');
    textField = fixture.debugElement.query(By.css('input'));
    textField.nativeElement.dispatchEvent(new Event('change'));
    expect(component.pushUpdate).toHaveBeenCalled();
  });

  it('onfocusgained and lost needs to be called method', () => {
      component.onFocusGainedMethodID = jasmine.createSpy('onFocusGainedMethodID');
      component.onFocusLostMethodID = jasmine.createSpy('onFocusLostMethodID');
      component.attachFocusListeners(component.getFocusElement());
      textField.triggerEventHandler('focus', null);
      expect(component.onFocusGainedMethodID).toHaveBeenCalled();
      expect(component.onFocusLostMethodID).toHaveBeenCalledTimes(0);
      // textField.triggerEventHandler('blur', null);
      // expect(component.onFocusLostMethodID).toHaveBeenCalledTimes(1);
  });
});
