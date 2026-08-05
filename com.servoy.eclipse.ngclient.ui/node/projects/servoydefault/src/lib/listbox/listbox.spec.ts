import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';


import { ServoyDefaultListBox } from './listbox';
import { IValuelist, FormattingService, TooltipService, ServoyApi, FormatFilterPipe,
         TooltipDirective, SabloTabseq, ServoyPublicService } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';

import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { DebugElement, ChangeDetectionStrategy, SimpleChange } from '@angular/core';

const mockDataValueList = [
  {
    realValue: 1,
    displayValue: 'Bucuresti'
  },
  {
    realValue: 2,
    displayValue: 'Timisoara'
  },
  {
    realValue: 3,
    displayValue: 'Cluj'
  },
] as IValuelist;


describe('ServoyDefaultListBox', () => {
  let component: ServoyDefaultListBox;
  let fixture: ComponentFixture<ServoyDefaultListBox>;
  let debugEl: DebugElement;
  const servoyApi: any = { getMarkupId: vi.fn(), isInDesigner: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any;

  beforeEach(async () => {

    TestBed.configureTestingModule({
      declarations: [ServoyDefaultListBox, TooltipDirective, SabloTabseq, FormatFilterPipe],
      imports: [FormsModule],
      providers: [FormattingService, TooltipService,
                  { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.overrideComponent(ServoyDefaultListBox, {
      set: {
        selector: 'servoydefault-listbox',
        templateUrl: './listbox.html',
        changeDetection: ChangeDetectionStrategy.Default
      }
    }).createComponent(ServoyDefaultListBox);

    component = fixture.componentInstance;
    component.servoyApi =  servoyApi;
    debugEl = fixture.debugElement;

    // set some default values
    fixture.componentRef.setInput('valuelistID', mockDataValueList);
    fixture.componentRef.setInput('multiselectListbox', false);
    component.dataProviderID.set(1);

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('multiselect should be false by default', () => {
    expect(component.multiselectListbox()).toBe(false);
  });

  it('should show `Bucuresti Timisoara Cluj` as options', () => {
      const options = debugEl.queryAll(By.css('select option'));
      expect(options[0].nativeElement.text).toBe('Bucuresti');
      expect(options[1].nativeElement.text).toBe('Timisoara');
      expect(options[2].nativeElement.text).toBe('Cluj');
  });

  it('should call update method', () => {
    vi.spyOn(component, 'pushUpdate');
    const select = debugEl.query(By.css('select')).nativeElement;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(component.pushUpdate).toHaveBeenCalled();
  });

  it('should call multiUpdate method', () => {
    fixture.componentRef.setInput('multiselectListbox', true);
    fixture.detectChanges();
    vi.spyOn(component, 'multiUpdate');
    const select = debugEl.query(By.css('select')).nativeElement;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(component.multiUpdate).toHaveBeenCalled();
  });

  it('should have data provider id = 2', () => {
    const select = debugEl.query(By.css('select')).nativeElement;
    select.value = select.options[1].value;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(component.dataProviderID()).toEqual(2);
  });

  it('should test selectedValues', () => {
    fixture.componentRef.setInput('multiselectListbox', true);
    component.dataProviderID.set('test1\ntest2');
        component.ngOnChanges({
            dataProviderID: new SimpleChange(null, component.dataProviderID(), true)
        });
    fixture.detectChanges();
    expect(component.selectedValues[0]).toEqual('test1');
    expect(component.selectedValues[1]).toEqual('test2');
  });

  it('should call ngOnChanges', () => {
      fixture.componentRef.setInput('multiselectListbox', true);
      vi.spyOn(component, 'ngOnChanges');
      component.ngOnChanges({dataProviderID: new SimpleChange(1, 2, false)});
      fixture.detectChanges();
      expect(component.ngOnChanges).toHaveBeenCalled();
  });

  it( 'should render markupid ', () => {
    servoyApi.getMarkupId.mockReturnValue( 'myid');
    const select = debugEl.query(By.css('select')).nativeElement;
    fixture.detectChanges();
    expect(select.id).toBe('myid');
  });

  it( 'should have called servoyApi.getMarkupId', () => {
     expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
  });

});
