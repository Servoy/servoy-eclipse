import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyDefaultCheckGroup } from './checkgroup';
import { IValuelist, FormattingService, ServoyApi, TooltipService, NotNullOrEmptyPipe,
         TooltipDirective, SabloTabseq, ServoyPublicService } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';

import {ChoiceElementDirective} from '../basechoice';

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

describe('ServoyDefaultCheckGroup', () => {
  let component: ServoyDefaultCheckGroup;
  let fixture: ComponentFixture<ServoyDefaultCheckGroup>;
  let servoyApi: any;

  beforeEach(async () => {
    servoyApi = { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any;
    mockData.hasRealValues = () => true;

    TestBed.configureTestingModule({
      imports: [ServoyDefaultCheckGroup, TooltipDirective, SabloTabseq, NotNullOrEmptyPipe, ChoiceElementDirective, FormsModule],
      providers: [NotNullOrEmptyPipe, FormattingService, TooltipService,
                  { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
    })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultCheckGroup);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;
    component = fixture.componentInstance;
    fixture.componentRef.setInput('valuelistID', mockData);
    fixture.componentRef.setInput('enabled', true);
    fixture.componentRef.setInput('editable', true);
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it.skip ('should set initial styles', () => {

  });

  it.skip('should click change value', () => {
    const input = fixture.debugElement.query(By.css('input')).nativeElement;
    const label = fixture.debugElement.query(By.css('label')).nativeElement;
    const span = fixture.debugElement.query(By.css('span')).nativeElement;

    expect(input.checked).toBeFalsy(); // default state
    input.click();

    fixture.detectChanges();
    expect(input.checked).toBeTruthy(); // state after click

    input.click();
    fixture.detectChanges();
    expect(input.checked).toBeFalsy();
  });

  it('should getSelectionFromDP', () => {
    vi.useFakeTimers();
    component.dataProviderID.set(1);
    component.setSelectionFromDataprovider();
    fixture.detectChanges();
    vi.advanceTimersByTime(0);
    expect(component.getSelectedElements()[0]).toBe(component.dataProviderID());
    vi.useRealTimers();
  });

  it('shoud getSelectionFromMultipleDp', () => {
    vi.useFakeTimers();
    component.dataProviderID.set('1\n3');
    component.setSelectionFromDataprovider();
    fixture.detectChanges();
    vi.advanceTimersByTime(0);
    const selectedElements = component.getSelectedElements();
    expect(selectedElements).toContain(+component.dataProviderID()[0]);
    expect(selectedElements).toContain(+component.dataProviderID()[2]);
    vi.useRealTimers();
  });

  it('should get DP from selection', () => {
    vi.useFakeTimers();
    component.dataProviderID.set('1\n3');
    component.setSelectionFromDataprovider();
    fixture.detectChanges();
    vi.advanceTimersByTime(0);
    const selectedElements = component.getDataproviderFromSelection();
    expect(selectedElements).toContain(component.dataProviderID()[0]);
    expect(selectedElements).toContain(component.dataProviderID()[2]);
    expect(selectedElements!.length).toBe(component.dataProviderID().length);
    vi.useRealTimers();
  });


});
