import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyDefaultSpinner } from './spinner';
import { IValuelist, FormattingService, ServoyApi, TooltipService, ServoyPublicService,
         TooltipDirective, SabloTabseq } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';


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
  let component: ServoyDefaultSpinner;
  let fixture: ComponentFixture<ServoyDefaultSpinner>;
  let buttonUp: any; let buttonDown: any;
  let servoyApi: any;

  beforeEach(async () => {
    servoyApi = { getMarkupId: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any;

    TestBed.configureTestingModule({
      declarations: [ServoyDefaultSpinner, TooltipDirective, SabloTabseq],
      imports: [FormsModule],
      providers: [FormattingService, TooltipService,
                  { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultSpinner);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;

    buttonUp = fixture.debugElement.queryAll(By.css('button'))[0];
    buttonDown = fixture.debugElement.queryAll(By.css('button'))[1];

    component = fixture.componentInstance;
    fixture.componentRef.setInput('valuelistID', mockData);
    fixture.componentRef.setInput('enabled', true);
    fixture.componentRef.setInput('editable', true);
    component.dataProviderID.set(1);
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it.skip('should click change value', () => {
    const input = fixture.debugElement.query(By.css('input')).nativeElement;
  });



  it('should got undefined dp if dp is not present in valuelist', () => {
    component.dataProviderID.set('Salut');
    const selection = component.getSelectionFromDataprovider();
    fixture.detectChanges();
    expect(selection).toBeFalsy();
  });

  it('should selection to match displayValue of dp 1', () => {
    const selection = component.getSelectionFromDataprovider();
    expect(selection).toBe('Timisoara');
  });

  it('should change dp when click the up button', () => {
    vi.useFakeTimers();
    buttonDown.nativeElement.click();
    fixture.detectChanges();
    vi.advanceTimersByTime(0);
    const selection = component.getSelectionFromDataprovider();
    expect(selection).toBe('Bucharest');
    vi.useRealTimers();
  });

  it('should getSelectionFromDP', () => {
    vi.useFakeTimers();
    buttonUp.nativeElement.click();
    fixture.detectChanges();
    vi.advanceTimersByTime(0);
    const selection = component.getSelectionFromDataprovider();
    expect(selection).toBe('Amsterdam');
    vi.useRealTimers();
  });

  it.skip('should change dp when press the down button', () => {
    vi.useFakeTimers();
    const input = fixture.debugElement.query(By.css('input'));
    input.nativeElement.focus();
    input.triggerEventHandler('keyup', { key: 'ArrowDown' });
    fixture.detectChanges();
    vi.advanceTimersByTime(0);
    const selection = component.getSelectionFromDataprovider();
    expect(selection).toBe('Bucharest');
    vi.useRealTimers();
  });

  it.skip('should change dp when press the up button', () => {
    vi.useFakeTimers();
    const input = fixture.debugElement.query(By.css('input'));
    input.nativeElement.focus();
    input.triggerEventHandler('keyup', { key: 'ArrowUp' });
    fixture.detectChanges();
    vi.advanceTimersByTime(0);
    const selection = component.getSelectionFromDataprovider();
    expect(selection).toBe('Amsterdam');
    vi.useRealTimers();
  });

  it.skip('should change dp when scroll  ', () => {
    vi.useFakeTimers();
    const input = fixture.debugElement.query(By.css('input'));
    input.nativeElement.scroll();
    fixture.detectChanges();
    vi.advanceTimersByTime(0);
    const selection = component.getSelectionFromDataprovider();
    expect(selection).toBe('Amsterdam');
    vi.useRealTimers();
  });


});
