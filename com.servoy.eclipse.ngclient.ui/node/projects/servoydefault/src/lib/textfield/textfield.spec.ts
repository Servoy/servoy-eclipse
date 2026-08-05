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
      declarations: [ServoyDefaultTextField, TooltipDirective, SabloTabseq,
                     StartEditDirective, FormatDirective, DecimalkeyconverterDirective],
      imports: [FormsModule],
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
    component.servoyApi =  { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), startEdit: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any;
    component.format = new Format();
    component.format.type = 'NUMBER';
    component.format.display = '#,###.00';
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
    vi.spyOn(component, 'pushUpdate');
    textField = fixture.debugElement.query(By.css('input'));
    textField.nativeElement.dispatchEvent(new Event('change'));
    expect(component.pushUpdate).toHaveBeenCalled();
  });

  it('onfocusgained and lost needs to be called method', () => {
      component.onFocusGainedMethodID = vi.fn();
      component.onFocusLostMethodID = vi.fn();
      component.attachFocusListeners(component.getFocusElement());
      textField.triggerEventHandler('focus', null);
      expect(component.onFocusGainedMethodID).toHaveBeenCalled();
      expect(component.onFocusLostMethodID).toHaveBeenCalledTimes(0);
      // textField.triggerEventHandler('blur', null);
      // expect(component.onFocusLostMethodID).toHaveBeenCalledTimes(1);
  });
});
