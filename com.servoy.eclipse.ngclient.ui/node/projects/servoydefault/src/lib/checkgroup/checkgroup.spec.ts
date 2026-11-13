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
    fixture.componentRef.setInput('servoyApi', servoyApi);
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

  it('returns null from getDataproviderFromSelection when nothing is selected', () => {
    component.dataProviderID.set(null);
    component.setSelectionFromDataprovider();
    expect(component.getDataproviderFromSelection()).toBeNull();
  });

  it('getFocusElement falls back to the native element when there is no input', () => {
    // no #input rendered, so super.getFocusElement() returns null and it falls back
    const focusElement = component.getFocusElement();
    expect(focusElement).toBeTruthy();
  });

  describe('itemClicked', () => {
    it('updates the selection array and pushes the derived dataprovider when changed', () => {
      vi.useFakeTimers();
      fixture.detectChanges();
      vi.advanceTimersByTime(0);
      const baseSpy = vi.spyOn(Object.getPrototypeOf(Object.getPrototypeOf(component)), 'baseItemClicked');
      const checkbox = document.createElement('input');
      checkbox.type = 'checkbox';
      checkbox.checked = true;
      component.itemClicked({ target: checkbox } as any, 0);
      expect(component.selection[0]).toBe(true);
      expect(baseSpy).toHaveBeenCalled();
      vi.useRealTimers();
    });
  });

  describe('attachEventHandlers', () => {
    it('invokes itemClicked and onActionMethodID on click when enabled and not readOnly', () => {
      const action = vi.fn();
      fixture.componentRef.setInput('onActionMethodID', action);
      fixture.componentRef.setInput('enabled', true);
      fixture.componentRef.setInput('readOnly', false);
      fixture.detectChanges();
      const itemClickedSpy = vi.spyOn(component, 'itemClicked').mockImplementation(() => undefined);
      let clickCb: (e: any) => void = () => undefined;
      vi.spyOn(component.getRenderer(), 'listen').mockImplementation((_t: any, evt: any, cb: any) => {
        if (evt === 'click') clickCb = cb;
        return () => undefined;
      });
      component.attachEventHandlers(document.createElement('input'), 0);
      const evt = { target: {} };
      clickCb(evt);
      expect(itemClickedSpy).toHaveBeenCalledWith(evt, 0);
      expect(action).toHaveBeenCalledWith(evt);
    });
  });

});
