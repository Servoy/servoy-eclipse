import { async, ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyDefaultPassword } from './password';
import { SabloModule } from '../../sablo/sablo.module';
import { FormsModule } from '@angular/forms';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { FormattingService, ServoyApi, TooltipService} from '../../ngclient/servoy_public';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { TooltipDirective } from '../../ngclient/tooltip/tooltip.directive';

import { runOnPushChangeDetection } from '../../testing';

describe('PasswordComponent', () => {
  let component: ServoyDefaultPassword;
  let fixture: ComponentFixture<ServoyDefaultPassword>;
  let servoyApi;
  let inputEl: DebugElement;
  let directiveInstance: TooltipDirective;

  beforeEach(() => {
      servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);
      TestBed.configureTestingModule({
        declarations: [ServoyDefaultPassword, TooltipDirective],
        imports: [SabloModule, FormsModule, ServoyPublicModule],
        providers: [FormattingService, TooltipService]
      })
      .compileComponents();
    });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultPassword);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have called servoyApi.getMarkupId', () => {
      expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
  });

  it('should have value test', () => {
    component.dataProviderID = 'test';
     runOnPushChangeDetection(fixture);
    fixture.whenStable().then(() =>
      expect(component.getNativeElement().value).toBe('test'));
  });

  it('should call update method', () => {
    spyOn(component, 'pushUpdate');
    inputEl = fixture.debugElement.query(By.css('input'));
    inputEl.nativeElement.dispatchEvent(new Event('change'));
    expect(component.pushUpdate).toHaveBeenCalled();
  });

  it('should have a placeholder', () => {
      expect( component.placeholderText ).toBeUndefined();
      expect( component.toolTipText).toBeUndefined();
      inputEl = fixture.debugElement.query(By.css('input'));
      directiveInstance = inputEl.injector.get(TooltipDirective);
      component.placeholderText = 'placeholder';
       runOnPushChangeDetection(fixture);
      expect( inputEl.nativeElement.placeholder ).toEqual('placeholder');
  });

  it('should have a tooltip', () => {
      inputEl = fixture.debugElement.query(By.css('input'));
      directiveInstance = inputEl.injector.get(TooltipDirective);
      inputEl.nativeElement.dispatchEvent(new Event('mouseenter'));
      expect(directiveInstance.isActive).toBe(false); // false because the text is undefined
      directiveInstance.tooltipText = 'Hi';
      expect(directiveInstance.tooltipText).toBe('Hi');
  });

  it('should have class: svy-password form-control input-sm svy-padding-xs ng-untouched ng-pristine ng-valid', () => {
      inputEl = fixture.debugElement.query(By.css('input'));
      expect( inputEl.nativeElement.getAttribute('class')).toBe('svy-password form-control input-sm svy-padding-xs ng-untouched ng-pristine ng-valid');
  });

});
