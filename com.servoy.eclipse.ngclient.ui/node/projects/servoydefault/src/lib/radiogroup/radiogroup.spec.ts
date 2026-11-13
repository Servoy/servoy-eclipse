import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyDefaultRadiogroup } from './radiogroup';
import { IValuelist, FormattingService, TooltipService, NotNullOrEmptyPipe,
         TooltipDirective, SabloTabseq, ServoyPublicService } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';

import {ChoiceElementDirective} from '../basechoice';

import { runOnPushChangeDetection } from '../testingutils';

const mockData = [
                  {
                    realValue: 3,
                    displayValue: 'Bucharest'
                  },
                  {
                    realValue: 1,
                    displayValue: 'Timisoara'
                  },
                  {
                    realValue: 2,
                    displayValue: 'Amsterdam'
                  },
                ] as IValuelist;

describe('ServoyDefaultRadiogroup', () => {
  let component: ServoyDefaultRadiogroup;
  let fixture: ComponentFixture<ServoyDefaultRadiogroup>;
  let input: DebugElement;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [ServoyDefaultRadiogroup, TooltipDirective, SabloTabseq, NotNullOrEmptyPipe, ChoiceElementDirective, FormsModule],
      providers: [NotNullOrEmptyPipe, FormattingService, TooltipService,
                  { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultRadiogroup);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('servoyApi', { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any);
    fixture.componentRef.setInput('valuelistID', mockData);
    fixture.componentRef.setInput('enabled', true);
    fixture.componentRef.setInput('editable', true);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should click change value',() => {
      input = fixture.debugElement.query(By.css('input'));
      expect(input.nativeElement.checked).toBeFalsy(); // default state
      input.nativeElement.dispatchEvent(new Event('click'));
      runOnPushChangeDetection(fixture);
      expect(input.nativeElement.checked).toBeTruthy(); // state after click
    });

  it('should call itemClicked', () => {
      vi.spyOn(component, 'itemClicked');
      input = fixture.debugElement.query(By.css('input'));
      input.nativeElement.dispatchEvent(new Event('click'));
      fixture.detectChanges();
      expect(component.itemClicked).toHaveBeenCalled();
  });

  it ('should be enabled', () => {
      input = fixture.debugElement.query(By.css('input'));
      expect(input.nativeElement.disabled).toBe(false);
  });

  describe('setSelectionFromDataprovider', () => {
    it('resolves the matching valuelist real value into value', () => {
      component.dataProviderID.set(2);
      component.setSelectionFromDataprovider();
      expect(component.value).toBe(2);
    });

    it('keeps the raw dataProviderID when no valuelist entry matches', () => {
      component.dataProviderID.set('unmatched');
      component.setSelectionFromDataprovider();
      expect(component.value).toBe('unmatched');
    });
  });

  describe('getSelectedElements', () => {
    it('returns the current value wrapped in an array', () => {
      component.value = 7;
      expect(component.getSelectedElements()).toEqual([7]);
    });
  });

  describe('itemClicked', () => {
    it('reads the value from the target and delegates to baseItemClicked', () => {
      const baseSpy = vi.spyOn(Object.getPrototypeOf(Object.getPrototypeOf(component)), 'baseItemClicked');
      component.value = 'old';
      const target = { value: 'new', blur: vi.fn(), localName: 'input' };
      component.itemClicked({ target, preventDefault: vi.fn() } as any, 0);
      expect(component.value).toBe('new');
      expect(baseSpy).toHaveBeenCalledWith(expect.anything(), true, 'new');
    });

    it('falls back to innerText when the target has no value', () => {
      component.value = 'old';
      const target = { innerText: 'textValue', blur: vi.fn(), localName: 'label' };
      component.itemClicked({ target, preventDefault: vi.fn() } as any, 0);
      expect(component.value).toBe('textValue');
    });
  });

  describe('svyOnChanges scrollbars', () => {
    it('adds horizontaldirection class for scrollbars value 36', () => {
      component.svyOnChanges({ scrollbars: { currentValue: 36, previousValue: 0, firstChange: false, isFirstChange: () => false } } as any);
      expect(component.elementRef()!.nativeElement.classList.contains('horizontaldirection')).toBe(true);
    });

    it('removes horizontaldirection class for other scrollbars values', () => {
      component.elementRef()!.nativeElement.classList.add('horizontaldirection');
      component.svyOnChanges({ scrollbars: { currentValue: 0, previousValue: 36, firstChange: false, isFirstChange: () => false } } as any);
      expect(component.elementRef()!.nativeElement.classList.contains('horizontaldirection')).toBe(false);
    });
  });

  describe('getFocusElement', () => {
    it('falls back to the native element when there is no inner input query', () => {
      expect(component.getFocusElement()).toBeTruthy();
    });
  });
});
