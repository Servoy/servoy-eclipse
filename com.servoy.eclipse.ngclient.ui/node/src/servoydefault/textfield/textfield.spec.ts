import { async, ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';

import { SabloModule } from '../../sablo/sablo.module';
import { ServoyPublicModule, } from '../../ngclient/servoy_public.module';

import { ServoyDefaultTextField } from './textfield';
import { FormattingService, TooltipService, LocaleService, I18NProvider, Format} from '../../ngclient/servoy_public';
import { By } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';

import { runOnPushChangeDetection } from '../../testing';

describe('ServoyDefaultTextField', () => {
  let component: ServoyDefaultTextField;
  let fixture: ComponentFixture<ServoyDefaultTextField>;
  let textField;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultTextField],
      imports: [SabloModule, ServoyPublicModule, FormsModule],
      providers: [I18NProvider, FormattingService, TooltipService, LocaleService ]
    })
    .compileComponents();
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

  it('should have formatted value 1.000,00', waitForAsync(inject([LocaleService], (locationService: LocaleService) => {
      locationService.setLocale('nl', 'NL');
      locationService.isLoaded().then(() => {
        component.dataProviderID = 1000;
        runOnPushChangeDetection(fixture);
        fixture.whenStable().then(() => {
        expect(component.getNativeElement().value).toBe('1.000,00');
      });
    });
  })));

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
      textField.triggerEventHandler('blur', null);
      expect(component.onFocusLostMethodID).toHaveBeenCalledTimes(1);
  });
});
