import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ServoyDefaultTypeahead } from './typeahead';
import { FormattingService, TooltipService, ServoyApi, Format, IValuelist, ServoyPublicService,
         TooltipDirective, SabloTabseq, StartEditDirective } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { of } from 'rxjs';
import { FormsModule } from '@angular/forms';


const mockData = [
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


describe('TypeaheadComponent', () => {
  let component: ServoyDefaultTypeahead;
  let fixture: ComponentFixture<ServoyDefaultTypeahead>;
  let servoyApi: any;

  beforeEach(async () => {
    servoyApi = { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn(), getClientProperty: vi.fn() } as any;
    mockData.hasRealValues = () => true;
    mockData.filterList = () => of(mockData);

    TestBed.configureTestingModule({
      imports: [ServoyDefaultTypeahead, TooltipDirective, SabloTabseq, StartEditDirective, NgbModule, FormsModule],
      providers: [FormattingService, TooltipService,
                  { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultTypeahead);
    fixture.componentRef.setInput('servoyApi', servoyApi);

    component = fixture.componentInstance;
    fixture.componentRef.setInput('valuelistID', mockData);
    component.dataProviderID.set(3);
    const fmt = new Format();
    fmt.type = 'NUMBER';
    fmt.display = '####';
    fixture.componentRef.setInput('format', fmt);
    component.ngOnInit();

    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should set initial list of values', () => {
    expect(component.valuelistID().length).toBe(3);
  });

  it('should set initial dropdown closed', () => {
    expect(component.instance().isPopupOpen()).toBeFalsy();
  });

  it('should open dropdown on container click', () => {
    vi.useFakeTimers();
    fixture.detectChanges();
    component.click$.next('');
    vi.advanceTimersByTime(100);
    fixture.detectChanges();
    expect(component.instance().isPopupOpen()).toBeTruthy();
    vi.useRealTimers();
  });


  it('should open dropdown on container focus', () => {
    vi.useFakeTimers();
    fixture.detectChanges();
    component.focus$.next('');
    vi.advanceTimersByTime(100);
    fixture.detectChanges();
    expect(component.instance().isPopupOpen()).toBeTruthy();
    vi.useRealTimers();
  });

  it('should set initial list of values', async () => {
    const values = await new Promise(resolve => {
      component.values(of('')).subscribe(v => {
        resolve(v);
      });
    });
    expect(values).toEqual(mockData);
  });

  describe('isEditable', () => {
    it('is false when the valuelist has real values', () => {
      expect(component.isEditable()).toBe(false);
    });

    it('is true when the valuelist has no real values', () => {
      const vl = [{ realValue: 1, displayValue: 'x' }] as IValuelist;
      vl.hasRealValues = () => false;
      fixture.componentRef.setInput('valuelistID', vl);
      fixture.detectChanges();
      expect(component.isEditable()).toBe(true);
    });
  });

  describe('resultFormatter', () => {
    it('returns a non-breaking space for empty display values', () => {
      expect(component.resultFormatter({ displayValue: '', realValue: 1 })).toBe('\u00A0');
    });

    it('formats a non-empty display value through the formatting service', () => {
      const fmtSpy = vi.spyOn(component.formattingService, 'format');
      component.resultFormatter({ displayValue: 'Cluj', realValue: 3 });
      expect(fmtSpy).toHaveBeenCalledWith('Cluj', component.format(), false);
    });
  });

  describe('inputFormatter', () => {
    it('returns empty string for a null input', () => {
      expect(component.inputFormatter(null)).toBe('');
    });

    it('uses the display value directly when present', () => {
      const fmtSpy = vi.spyOn(component.formattingService, 'format');
      component.inputFormatter({ displayValue: 'Timisoara', realValue: 2 });
      expect(fmtSpy).toHaveBeenCalledWith('Timisoara', component.format(), false);
    });

    it('resolves a real value to its display value via the valuelist', () => {
      const fmtSpy = vi.spyOn(component.formattingService, 'format');
      component.inputFormatter(3);
      // realValue 3 resolves to display value 'Cluj' before formatting
      expect(fmtSpy).toHaveBeenCalledWith('Cluj', component.format(), false);
    });
  });

  describe('valueChanged', () => {
    it('sets the real value and emits when a valuelist item is chosen', () => {
      const emitSpy = vi.spyOn(component.dataProviderIDChange, 'emit');
      component.valueChanged({ displayValue: 'Timisoara', realValue: 2 });
      expect(component.dataProviderID()).toBe(2);
      expect(emitSpy).toHaveBeenCalledWith(2);
    });

    it('sets a plain value when no realValue is present', () => {
      component.valueChanged('freeText' as any);
      expect(component.dataProviderID()).toBe('freeText');
    });

    it('clears the dataProviderID when value is falsy', () => {
      component.valueChanged(null as any);
      expect(component.dataProviderID()).toBeNull();
    });
  });

  describe('focusGained/onClick', () => {
    it('focusGained pushes focus$ when showPopupOnFocusGain and editable', () => {
      component.showPopupOnFocusGain = true;
      fixture.componentRef.setInput('editable', true);
      fixture.componentRef.setInput('readOnly', false);
      fixture.detectChanges();
      const spy = vi.spyOn(component.focus$, 'next');
      component.focusGained();
      expect(spy).toHaveBeenCalledWith('');
    });

    it('onClick fires onActionMethodID when not editable', () => {
      const action = vi.fn();
      fixture.componentRef.setInput('onActionMethodID', action);
      fixture.componentRef.setInput('editable', false);
      fixture.detectChanges();
      const evt = new Event('click');
      component.onClick(evt);
      expect(action).toHaveBeenCalledWith(evt);
    });
  });

  describe('svyOnChanges', () => {
    it('sets the maxlength attribute from format when not in findmode', () => {
      const fmt = new Format();
      fmt.type = 'TEXT';
      fmt.maxLength = 10;
      fixture.componentRef.setInput('format', fmt);
      fixture.componentRef.setInput('findmode', false);
      fixture.detectChanges();
      const spy = vi.spyOn(component.getRenderer(), 'setAttribute');
      component.svyOnChanges({ format: { currentValue: fmt, previousValue: undefined, firstChange: false, isFirstChange: () => false } } as any);
      expect(spy).toHaveBeenCalledWith(component.elementRef()!.nativeElement, 'maxlength', '10');
    });

    it('tracks the current dataProviderID value on change', () => {
      component.svyOnChanges({ dataProviderID: { currentValue: 2, previousValue: 3, firstChange: false, isFirstChange: () => false } } as any);
      expect(component.currentValue).toBe(2);
    });
  });

});
