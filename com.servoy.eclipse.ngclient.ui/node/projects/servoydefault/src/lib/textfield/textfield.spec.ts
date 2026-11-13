import { describe, it, expect, beforeEach, beforeAll, vi } from 'vitest';
import {  ComponentFixture, TestBed } from '@angular/core/testing';

import { FormattingService, TooltipService, Format, ServoyPublicService,
         TooltipDirective, SabloTabseq, StartEditDirective, FormatDirective,
         DecimalkeyconverterDirective } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';
import numbro from 'numbro';
import languages from 'numbro/dist/languages.min';
import { ServoyDefaultTextField } from './textfield';
import { By } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { runOnPushChangeDetection } from '../testingutils';

describe('ServoyDefaultTextField', () => {
  let component: ServoyDefaultTextField;
  let fixture: ComponentFixture<ServoyDefaultTextField>;
  let textField: any;
  let servoyPublicService: any;
   
  beforeAll(() => {
       numbro.registerLanguage(languages['en-GB']);
       numbro.registerLanguage(languages['nl-NL']);
       const lang = languages;
       const nl = lang['nl-NL'];
       console.log('numbro language', nl);
    });
    
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ServoyDefaultTextField, TooltipDirective, SabloTabseq, StartEditDirective, FormatDirective, DecimalkeyconverterDirective, FormsModule],
      providers: [FormattingService, TooltipService,
                  { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
    })
    .compileComponents();
    servoyPublicService = TestBed.inject(ServoyPublicService);
  });

  beforeEach(async () => {
    fixture = TestBed.createComponent(ServoyDefaultTextField);
    textField = fixture.debugElement.query(By.css('input'));
    component = fixture.componentInstance;
    fixture.componentRef.setInput('servoyApi', { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), startEdit: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any);
    const fmt = new Format();
    fmt.type = 'NUMBER';
    fmt.display = '#,###.00';
    fixture.componentRef.setInput('format', fmt);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have called servoyApi.getMarkupId', () => {
      expect( component.servoyApi().getMarkupId ).toHaveBeenCalled();
  });

  it('should use start edit directive', () => {
      textField.triggerEventHandler('focus', null);
      expect(component.servoyApi().startEdit).toHaveBeenCalled();
  });

  it('should have formatted value 1.000,00', () => {
      servoyPublicService.setLocale('nl', 'NL');
      numbro.setLanguage('nl-NL');
      component.dataProviderID.set(1000);
      runOnPushChangeDetection(fixture);
      fixture.whenStable().then(() => {
         expect(component.getNativeElement().value).toBe('1.000,00');
      });
  });

  it('should call update method', () => {
    vi.spyOn(component, 'pushUpdate');
    textField = fixture.debugElement.query(By.css('input'));
    textField.nativeElement.dispatchEvent(new Event('change'));
    expect(component.pushUpdate).toHaveBeenCalled();
  });

  it('onfocusgained and lost needs to be called method', () => {
      fixture.componentRef.setInput('onFocusGainedMethodID', vi.fn());
      fixture.componentRef.setInput('onFocusLostMethodID', vi.fn());
      component.attachFocusListeners(component.getFocusElement());
      textField.triggerEventHandler('focus', null);
      expect(component.onFocusGainedMethodID()).toHaveBeenCalled();
      expect(component.onFocusLostMethodID()).toHaveBeenCalledTimes(0);
      // textField.triggerEventHandler('blur', null);
      // expect(component.onFocusLostMethodID).toHaveBeenCalledTimes(1);
  });

  describe('onModelChange', () => {
    it('sets a plain value directly', () => {
      component.onModelChange('hello');
      expect(component.dataProviderID()).toBe('hello');
    });

    it('resets then re-applies an invalid date so the display is refreshed', () => {
      const invalidDate = new Date('invalid');
      component.onModelChange(invalidDate);
      expect(component.dataProviderID()).toBe(invalidDate);
    });

    it('keeps a valid date value', () => {
      const validDate = new Date(2020, 0, 1);
      component.onModelChange(validDate);
      expect(component.dataProviderID()).toBe(validDate);
    });
  });

  describe('onClick', () => {
    it('fires onActionMethodID when the field is not editable', () => {
      const action = vi.fn();
      fixture.componentRef.setInput('onActionMethodID', action);
      fixture.componentRef.setInput('editable', false);
      fixture.detectChanges();
      const evt = new Event('click');
      component.onClick(evt);
      expect(action).toHaveBeenCalledWith(evt);
    });

    it('does nothing when the field is editable', () => {
      const action = vi.fn();
      fixture.componentRef.setInput('onActionMethodID', action);
      fixture.componentRef.setInput('editable', true);
      fixture.detectChanges();
      component.onClick(new Event('click'));
      expect(action).not.toHaveBeenCalled();
    });
  });
});
