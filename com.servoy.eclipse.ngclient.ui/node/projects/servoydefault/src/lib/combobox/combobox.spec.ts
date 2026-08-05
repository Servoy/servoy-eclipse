import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ServoyDefaultCombobox } from './combobox';
import { Format, IValuelist, ServoyPublicService,
         TooltipDirective, SabloTabseq, StartEditDirective, FormatDirective,
         FormatFilterPipe, EmptyValueFilterPipe, FormattingService, TooltipService } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { By } from '@angular/platform-browser';
import { DebugElement, SimpleChange } from '@angular/core';
import { FormsModule } from '@angular/forms';

describe('ComboboxComponent', () => {
  let component: ServoyDefaultCombobox;
  let fixture: ComponentFixture<ServoyDefaultCombobox>;
  let servoyApi: any;
  let combobox: DebugElement;

  beforeEach(async () => {

    servoyApi = { formWillShow: vi.fn(), hideForm: vi.fn(), startEdit: vi.fn(), apply: vi.fn(), callServerSideApi: vi.fn(), isInDesigner: vi.fn(), trustAsHtml: vi.fn(), isInAbsoluteLayout: vi.fn(), getMarkupId: vi.fn(), getFormName: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn(), getClientProperty: vi.fn() } as any;


    TestBed.configureTestingModule({
        declarations: [ServoyDefaultCombobox, TooltipDirective, SabloTabseq,
                       StartEditDirective, FormatDirective, FormatFilterPipe,
                       EmptyValueFilterPipe],
        imports: [NgbModule, FormsModule],
        providers: [FormattingService, TooltipService,
                    { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultCombobox);
    fixture.componentInstance.servoyApi = servoyApi;

    const dummyValuelist = new DummyValuelist();
    dummyValuelist.push({
        realValue: 1,
        displayValue: 'Bucuresti'
    });
    dummyValuelist.push( {
       realValue: 2,
       displayValue: 'Timisoara'
    });
     dummyValuelist.push({
         realValue: 3,
         displayValue: 'Cluj'
    });

    component = fixture.componentInstance;
    component.valuelistID = dummyValuelist;
    component.servoyApi = { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), startEdit: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn(), getClientProperty: vi.fn() } as any;
    component.dataProviderID = 3;
    component.format = new Format();
    component.format.type = 'TEXT';
    component.ngOnInit();
    component.ngOnChanges({
      dataProviderID: new SimpleChange(null, 3, true)
    });
    combobox = fixture.debugElement.query(By.css('.svy-combobox'));
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial length = 3', () => {
    expect(component.valuelistID.length).toBe(3);
  });

  it('should have called servoyApi.getMarkupId', () => {
    expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
  });

  it('should use start edit directive', () => {
    combobox.triggerEventHandler('focus', null);
    fixture.detectChanges();
    expect(component.servoyApi.startEdit).toHaveBeenCalled();
  });

  it.skip('should call update method', () => {
    vi.spyOn(component, 'updateValue');
    combobox.nativeElement.dispatchEvent(new Event('update'));
    fixture.detectChanges();
    expect(component.updateValue).toHaveBeenCalled();
  });

});

class DummyValuelist extends Array<{ displayValue: string; realValue: any }> implements IValuelist {
    filterList(_filterString: string): Observable<any>{
        return of('');
    }

    getDisplayValue(_realValue: any): Observable<any>{
        return of('');
    }
    hasRealValues(): boolean{
        return true;
    }
    isRealValueDate(): boolean{
        return false;
    }
}
