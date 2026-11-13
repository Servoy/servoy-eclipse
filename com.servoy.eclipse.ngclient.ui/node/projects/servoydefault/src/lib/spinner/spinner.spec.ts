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
      imports: [ServoyDefaultSpinner, TooltipDirective, SabloTabseq, FormsModule],
      providers: [FormattingService, TooltipService,
                  { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultSpinner);
    fixture.componentRef.setInput('servoyApi', servoyApi);

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

  describe('getFocusElement/requestFocus', () => {
    it('returns the inner input as the focus element', () => {
      expect(component.getFocusElement()).toBe(component.child()!.nativeElement);
    });

    it('requestFocus sets mustExecuteOnFocus and focuses the element', () => {
      const focusSpy = vi.spyOn(component.getFocusElement(), 'focus');
      component.requestFocus(false);
      expect(component.mustExecuteOnFocus).toBe(false);
      expect(focusSpy).toHaveBeenCalled();
    });
  });

  describe('isDisabled', () => {
    it('is disabled when not enabled', () => {
      fixture.componentRef.setInput('enabled', false);
      fixture.detectChanges();
      expect(component.isDisabled()).toBe(true);
    });

    it('is disabled when not editable', () => {
      fixture.componentRef.setInput('editable', false);
      fixture.detectChanges();
      expect(component.isDisabled()).toBe(true);
    });

    it('is enabled when both enabled and editable', () => {
      expect(component.isDisabled()).toBe(false);
    });
  });

  describe('increment/decrement', () => {
    it('increment moves to the next valuelist entry and wraps around', () => {
      component.dataProviderID.set(3); // counter 0
      component.getSelectionFromDataprovider();
      component.increment();
      expect(component.dataProviderID()).toBe(1); // index 1 realValue
    });

    it('decrement wraps to the last valuelist entry from the first', () => {
      component.dataProviderID.set(3); // counter 0
      component.getSelectionFromDataprovider();
      component.decrement();
      expect(component.dataProviderID()).toBe(2); // last entry realValue
    });
  });

  describe('scroll/keydown callbacks', () => {
    it('scrollCallback increments when scrolling up', () => {
      const incSpy = vi.spyOn(component, 'increment');
      component.scrollCallback({ wheelDelta: 10, preventDefault: vi.fn() });
      expect(incSpy).toHaveBeenCalled();
    });

    it('scrollCallback decrements when scrolling down', () => {
      const decSpy = vi.spyOn(component, 'decrement');
      component.scrollCallback({ deltaY: 10, preventDefault: vi.fn() });
      expect(decSpy).toHaveBeenCalled();
    });

    it('scrollCallback does nothing when disabled', () => {
      fixture.componentRef.setInput('enabled', false);
      fixture.detectChanges();
      const incSpy = vi.spyOn(component, 'increment');
      const decSpy = vi.spyOn(component, 'decrement');
      component.scrollCallback({ wheelDelta: 10, preventDefault: vi.fn() });
      expect(incSpy).not.toHaveBeenCalled();
      expect(decSpy).not.toHaveBeenCalled();
    });

    it('keydownKeypressCallback decrements on arrow down (40)', () => {
      const decSpy = vi.spyOn(component, 'decrement');
      component.keydownKeypressCallback({ which: 40 });
      expect(decSpy).toHaveBeenCalled();
    });

    it('keydownKeypressCallback increments on arrow up (38)', () => {
      const incSpy = vi.spyOn(component, 'increment');
      component.keydownKeypressCallback({ which: 38 });
      expect(incSpy).toHaveBeenCalled();
    });
  });

  describe('isScrollingUp', () => {
    it('detects scroll up via wheelDelta', () => {
      expect(component.isScrollingUp({ wheelDelta: 120 })).toBeTruthy();
    });

    it('detects scroll down via deltaY', () => {
      expect(component.isScrollingUp({ deltaY: 120 })).toBeFalsy();
    });
  });

});
