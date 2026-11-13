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
      imports: [ServoyDefaultCheck, TooltipDirective, SabloTabseq, FormsModule],
      providers: [FormattingService, TooltipService,
                  { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultCheck);
    fixture.componentRef.setInput('servoyApi', servoyApi);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('text', 'Check me');
    fixture.componentRef.setInput('enabled', true);
    fixture.componentRef.setInput('editable', true);

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
      component.dataProviderID.set(1);
      expect(component.getSelectionFromDataprovider()).toBeTruthy();

      component.dataProviderID.set('1');
      expect(component.getSelectionFromDataprovider()).toBeTruthy();

      component.dataProviderID.set(0);
      expect(component.getSelectionFromDataprovider()).toBeFalsy();

      component.dataProviderID.set('0');
      expect(component.getSelectionFromDataprovider()).toBeFalsy();

      component.dataProviderID.set('');
      expect(component.getSelectionFromDataprovider()).toBeFalsy();

      component.dataProviderID.set('something');
      expect(component.getSelectionFromDataprovider()).toBeFalsy();

      component.dataProviderID.set(null);
      expect(component.getSelectionFromDataprovider()).toBeFalsy();

      component.dataProviderID.set(undefined);
      expect(component.getSelectionFromDataprovider()).toBeFalsy();
  });

  it('getSelectionFromDP uses the valuelist first real value when present', () => {
      fixture.componentRef.setInput('valuelistID', [{ realValue: 9, displayValue: 'nine' }] as any);
      component.dataProviderID.set(9);
      fixture.detectChanges();
      expect(component.getSelectionFromDataprovider()).toBe(true);
  });

  it('does not need scrollbar information', () => {
      expect(component.needsScrollbarInformation()).toBe(false);
  });

  describe('itemClicked value toggling', () => {
      it('toggles a numeric dataProviderID from 0 to 1 and pushes update', () => {
          vi.useFakeTimers();
          const pushSpy = vi.spyOn(component, 'pushUpdate');
          component.dataProviderID.set(0);
          component.itemClicked({ target: { localName: 'input' } });
          expect(component.dataProviderID()).toBe(1);
          vi.advanceTimersByTime(0);
          expect(pushSpy).toHaveBeenCalled();
          vi.useRealTimers();
      });

      it('toggles a string dataProviderID from "1" to "0"', () => {
          vi.useFakeTimers();
          component.dataProviderID.set('1');
          component.itemClicked({ target: { localName: 'input' } });
          expect(component.dataProviderID()).toBe('0');
          vi.useRealTimers();
      });

      it('uses the valuelist real value when a valuelist is present', () => {
          vi.useFakeTimers();
          fixture.componentRef.setInput('valuelistID', [{ realValue: 7, displayValue: 'seven' }] as any);
          fixture.detectChanges();
          component.dataProviderID.set(null);
          component.itemClicked({ target: { localName: 'input' } });
          expect(component.dataProviderID()).toBe(7);
          vi.useRealTimers();
      });

      it('fires onActionMethodID after the deferred push', () => {
          vi.useFakeTimers();
          const action = vi.fn();
          fixture.componentRef.setInput('onActionMethodID', action);
          fixture.detectChanges();
          component.dataProviderID.set(0);
          const evt = { target: { localName: 'input' } };
          component.itemClicked(evt);
          vi.advanceTimersByTime(0);
          expect(action).toHaveBeenCalledWith(evt);
          vi.useRealTimers();
      });
  });

  describe('setHorizontalAlignmentFlexbox', () => {
      it('centers for alignment 0', () => {
          const spy = vi.spyOn(component.getRenderer(), 'setStyle');
          const el = document.createElement('div');
          component.setHorizontalAlignmentFlexbox(el, component.getRenderer(), 0);
          expect(spy).toHaveBeenCalledWith(el, 'justify-content', 'center');
      });

      it('aligns to flex-end for alignment 4', () => {
          const spy = vi.spyOn(component.getRenderer(), 'setStyle');
          const el = document.createElement('div');
          component.setHorizontalAlignmentFlexbox(el, component.getRenderer(), 4);
          expect(spy).toHaveBeenCalledWith(el, 'justify-content', 'flex-end');
      });

      it('does not set a style for alignment -1', () => {
          const spy = vi.spyOn(component.getRenderer(), 'setStyle');
          const el = document.createElement('div');
          component.setHorizontalAlignmentFlexbox(el, component.getRenderer(), -1);
          expect(spy).not.toHaveBeenCalled();
      });
  });

});

async function clickOnElement(element: any, fixture: ComponentFixture<ServoyDefaultCheck>, checkInput: any, toTestFlag: any) {
  element.click();
  fixture.detectChanges();
  vi.advanceTimersByTime(0);
  expect(checkInput.checked).toBe(toTestFlag);
}
