import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ServoyDefaultCombobox } from './combobox';
import { ServoyPublicTestingModule, Format, ServoyApi, IValuelist } from '@servoy/public';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { By } from '@angular/platform-browser';
import { DebugElement, SimpleChange } from '@angular/core';
import { FormsModule } from '@angular/forms';

describe('ComboboxComponent', () => {
  let component: ServoyDefaultCombobox;
  let fixture: ComponentFixture<ServoyDefaultCombobox>;
  let servoyApi;
  let combobox: DebugElement;

  beforeEach(waitForAsync(() => {
    servoyApi = jasmine.createSpyObj( 'ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent','registerComponent','unRegisterComponent']);


    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultCombobox],
      providers: [ ],
      imports: [ServoyPublicTestingModule, NgbModule, FormsModule]
    })
    .compileComponents();
  }));

  beforeEach(() => {

    fixture = TestBed.createComponent(ServoyDefaultCombobox);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;

    let dummyValuelist = new DummyValuelist();
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
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId', 'trustAsHtml', 'startEdit','registerComponent','unRegisterComponent']);
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

  xit('should call update method', () => {
     // test should be to click on the combo to open the tree and then click on an item.
    spyOn(component, 'updateValue');
    combobox.nativeElement.dispatchEvent(new Event('update'));
    fixture.detectChanges();
    expect(component.updateValue).toHaveBeenCalled();
  });

});

 class DummyValuelist extends Array<{ displayValue: string; realValue: any }> implements IValuelist
{
    filterList(filterString: string): Observable<any>{
        return null;
    }
    
    getDisplayValue(realValue: any): Observable<any>{
        return null;
    }
    hasRealValues(): boolean{
        return true;   
    }
    isRealValueDate(): boolean{
        return false;
    }
}