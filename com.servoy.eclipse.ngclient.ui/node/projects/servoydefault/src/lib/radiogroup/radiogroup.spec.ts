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
      declarations: [ServoyDefaultRadiogroup, TooltipDirective, SabloTabseq,
                     NotNullOrEmptyPipe, ChoiceElementDirective],
      imports: [FormsModule],
      providers: [NotNullOrEmptyPipe, FormattingService, TooltipService,
                  { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultRadiogroup);
    component = fixture.componentInstance;
    component.servoyApi =  { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any;
    component.valuelistID = mockData;
    component.enabled = true;
    component.editable = true;
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
});
