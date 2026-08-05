import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By }              from '@angular/platform-browser';
import { ServoyDefaultCheck } from './check';

import { FormattingService, ServoyApi, TooltipService, TooltipDirective, SabloTabseq,
         ServoyPublicService } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';
import {FormsModule} from '@angular/forms';

describe('CheckComponent', () => {
  let component: ServoyDefaultCheck;
  let fixture: ComponentFixture<ServoyDefaultCheck>;
  let servoyApi: any;
  let input: any; let label: any; let span: any;
  beforeEach(async () => {
  servoyApi =  { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any;
    TestBed.configureTestingModule({
      declarations: [ServoyDefaultCheck, TooltipDirective, SabloTabseq],
      imports: [FormsModule],
      providers: [FormattingService, TooltipService,
                  { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultCheck);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;
    component = fixture.componentInstance;
    component.text = 'Check me';
    component.enabled = true;
    component.editable = true;

    input = fixture.debugElement.query(By.css('input')).nativeElement;
    label = fixture.debugElement.query(By.css('label')).nativeElement;
    span = fixture.debugElement.query(By.css('span')).nativeElement;
    fixture.detectChanges();

  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should click change value', () => {
      expect(input.checked).toBeFalsy(); // default state
      input.click();

      fixture.detectChanges();
      expect(input.checked).toBeTruthy(); // state after click

      input.click();
      fixture.detectChanges();
      expect(input.checked).toBeFalsy();
  });

  it('should click on span and change value', () => {
    vi.useFakeTimers();
    expect(input.checked).toBeFalsy(); // default state
    clickOnElement(span, fixture, input,true);
    clickOnElement(span, fixture, input,false);
    vi.useRealTimers();
  });

  it('should click on label and change', () => {
    vi.useFakeTimers();
    expect(input.checked).toBeFalsy(); // default state
    clickOnElement(label, fixture, input, true);
    clickOnElement(label, fixture, input, false);
    vi.useRealTimers();
  });

  it('should getSelectionFromDP', () => {
      component.dataProviderID = 1;
      expect(component.getSelectionFromDataprovider()).toBeTruthy();

      component.dataProviderID = '1';
      expect(component.getSelectionFromDataprovider()).toBeTruthy();

      component.dataProviderID = 0;
      expect(component.getSelectionFromDataprovider()).toBeFalsy();

      component.dataProviderID = '0';
      expect(component.getSelectionFromDataprovider()).toBeFalsy();

      component.dataProviderID = '';
      expect(component.getSelectionFromDataprovider()).toBeFalsy();

      component.dataProviderID = 'something';
      expect(component.getSelectionFromDataprovider()).toBeFalsy();

      component.dataProviderID = null;
      expect(component.getSelectionFromDataprovider()).toBeFalsy();

      component.dataProviderID = undefined;
      expect(component.getSelectionFromDataprovider()).toBeFalsy();
  });

});

async function clickOnElement(element: any, fixture: ComponentFixture<ServoyDefaultCheck>, checkInput: any, toTestFlag: any) {
  element.click();
  fixture.detectChanges();
  vi.advanceTimersByTime(0);
  expect(checkInput.checked).toBe(toTestFlag);
}
