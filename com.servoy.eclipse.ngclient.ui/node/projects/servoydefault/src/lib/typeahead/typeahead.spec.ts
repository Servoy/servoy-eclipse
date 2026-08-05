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
      declarations: [ServoyDefaultTypeahead, TooltipDirective, SabloTabseq, StartEditDirective],
      imports: [NgbModule, FormsModule],
      providers: [FormattingService, TooltipService,
                  { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultTypeahead);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;

    component = fixture.componentInstance;
    component.valuelistID = mockData;
    component.dataProviderID = 3;
    component.format = new Format();
    component.format.type = 'NUMBER';
    component.format.display = '####';
    component.ngOnInit();

    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should set initial list of values', () => {
    expect(component.valuelistID.length).toBe(3);
  });

  it('should set initial dropdown closed', () => {
    expect(component.instance.isPopupOpen()).toBeFalsy();
  });

  it('should open dropdown on container click', () => {
    vi.useFakeTimers();
    fixture.detectChanges();
    component.click$.next('');
    vi.advanceTimersByTime(100);
    fixture.detectChanges();
    expect(component.instance.isPopupOpen()).toBeTruthy();
    vi.useRealTimers();
  });


  it('should open dropdown on container focus', () => {
    vi.useFakeTimers();
    fixture.detectChanges();
    component.focus$.next('');
    vi.advanceTimersByTime(100);
    fixture.detectChanges();
    expect(component.instance.isPopupOpen()).toBeTruthy();
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

});
