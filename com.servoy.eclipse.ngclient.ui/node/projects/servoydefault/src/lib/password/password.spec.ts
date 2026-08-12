import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyDefaultPassword } from './password';
import { FormsModule } from '@angular/forms';
import { FormattingService, ServoyApi, TooltipService, TooltipDirective, SabloTabseq,
         ServoyPublicService } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

import { runOnPushChangeDetection } from '../testingutils';

describe('PasswordComponent', () => {
  let component: ServoyDefaultPassword;
  let fixture: ComponentFixture<ServoyDefaultPassword>;
  let servoyApi: any;
  let inputEl: DebugElement;
  let directiveInstance: TooltipDirective;

  beforeEach(() => {
      servoyApi =  { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any;
      TestBed.configureTestingModule({
        imports: [ServoyDefaultPassword, TooltipDirective, SabloTabseq, FormsModule],
      providers: [FormattingService, TooltipService,
                    { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
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
    component.dataProviderID.set('test');
     runOnPushChangeDetection(fixture);
    fixture.whenStable().then(() =>
      expect(component.getNativeElement().value).toBe('test'));
  });

  it('should call update method', () => {
    vi.spyOn(component, 'pushUpdate');
    inputEl = fixture.debugElement.query(By.css('input'));
    inputEl.nativeElement.dispatchEvent(new Event('change'));
    expect(component.pushUpdate).toHaveBeenCalled();
  });

  it('should have a placeholder', () => {
      expect( component.placeholderText() ).toBeUndefined();
      expect( component.toolTipText()).toBeUndefined();
      inputEl = fixture.debugElement.query(By.css('input'));
      directiveInstance = inputEl.injector.get(TooltipDirective);
      fixture.componentRef.setInput('placeholderText', 'placeholder');
       runOnPushChangeDetection(fixture);
      expect( inputEl.nativeElement.placeholder ).toEqual('placeholder');
  });

  it('should have a tooltip', () => {
      inputEl = fixture.debugElement.query(By.css('input'));
      directiveInstance = inputEl.injector.get(TooltipDirective);
      inputEl.nativeElement.dispatchEvent(new Event('mouseenter'));
      expect(directiveInstance.isActive).toBe(false); // false because the text is undefined
      component.toolTipText.set('Hi');
      fixture.detectChanges();
      expect(directiveInstance.tooltipText()).toBe('Hi');
  });

  it('should have class: svy-password form-control input-sm svy-padding-xs ng-untouched ng-pristine ng-valid', () => {
      inputEl = fixture.debugElement.query(By.css('input'));
      expect( inputEl.nativeElement.getAttribute('class')).toBe('svy-password form-control input-sm svy-padding-xs ng-untouched ng-pristine ng-valid');
  });

});
