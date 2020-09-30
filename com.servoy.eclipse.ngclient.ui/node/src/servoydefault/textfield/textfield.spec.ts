import { async, ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { SabloModule } from '../../sablo/sablo.module';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';

import { ServoyDefaultTextField } from './textfield';
import { FormattingService, TooltipService} from '../../ngclient/servoy_public';
import { By } from '@angular/platform-browser';


describe('ServoyDefaultTextField', () => {
  let component: ServoyDefaultTextField;
  let fixture: ComponentFixture<ServoyDefaultTextField>;
  let textField;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultTextField],
      imports: [SabloModule, ServoyPublicModule],
      providers: [FormattingService, TooltipService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultTextField);
    textField = fixture.debugElement.query(By.css('input'));
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml', 'startEdit']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have called servoyApi.getMarkupId', () => {
      expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
  });

  it('should use start edit directive', () => {
      textField.triggerEventHandler('focus', null);
      fixture.detectChanges();
      expect(component.servoyApi.startEdit).toHaveBeenCalled();
  });

  it('should have value test', () => {
      component.dataProviderID = 'test';
      fixture.detectChanges();
      expect(component.getNativeElement().value).toBe('test');
  });

  it('onfocusgained and lost needs to be called method', () => {
      expect(component.valueBeforeChange).toBe(undefined);
      component.onFocusGainedMethodID = jasmine.createSpy('onFocusGainedMethodID');
      component.onFocusLostMethodID = jasmine.createSpy('onFocusLostMethodID');
      component.attachFocusListeners(component.getFocusElement());
      textField.triggerEventHandler('focus', null);
      fixture.detectChanges();
      expect(component.onFocusGainedMethodID).toHaveBeenCalled();
      expect(component.onFocusLostMethodID).toHaveBeenCalledTimes(0);
      textField.triggerEventHandler('blur', null);
      fixture.detectChanges();
      expect(component.onFocusLostMethodID).toHaveBeenCalledTimes(1);
  });
});
