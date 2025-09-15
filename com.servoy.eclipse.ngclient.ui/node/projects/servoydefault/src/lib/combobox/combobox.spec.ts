import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ServoyDefaultCombobox } from './combobox';
import { ServoyPublicTestingModule, Format, ServoyApi, IValuelist, ServoyBaseComponent } from '@servoy/public';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { By } from '@angular/platform-browser';
import { DebugElement, SimpleChange } from '@angular/core';
import { FormsModule } from '@angular/forms';

describe('ComboboxComponent', () => {
  let component: ServoyDefaultCombobox;
  let fixture: ComponentFixture<ServoyDefaultCombobox>;
  let servoyApi: ServoyApi;
  let combobox: DebugElement;

  beforeEach(waitForAsync(() => {

    // the following construct does nothing but make sure that an error is generated at compile-time if ServoyApi changes
    // so that when that happens we can update the jasmine.createSpyObj below which is based on strings only
    class _X extends ServoyApi {
        formWillShow(_fn: string, _rn?: string, _fi?: number): Promise<boolean> { return undefined; }
        hideForm(_fn: string, _rn?: string, _fi?: number, _fntws?: string, _rntwbs?: string, _fitwbs?: number): Promise<boolean> { return undefined; }
        startEdit(_p: string) {}
        apply(_propertyName: string, _value: any) {}
        callServerSideApi(_methodName: string, _args: Array<any>) {}
        /**
         * @deprecated _propertyName is not used
         */
        getFormComponentElements(_propertyName: string, _formComponentValue: any) {}
        getFormComponentElements(_formComponentValue: any) {}
        isInDesigner(): boolean { return false; }
        trustAsHtml(): boolean { return false; }
        isInAbsoluteLayout(): boolean { return false; }
        getMarkupId(): string  { return undefined; }
        getFormName(): string  { return undefined; }
        registerComponent(_component: ServoyBaseComponent<any>) {}
        unRegisterComponent(_component: ServoyBaseComponent<any>) {}
        getClientProperty(_key:string): any { return undefined; }
    };
    new _X(); // just to remove unused class warning

    servoyApi = jasmine.createSpyObj( 'ServoyApi', [
        'formWillShow',
        'hideForm',
        'startEdit',
        'apply',
        'callServerSideApi',
        'getFormComponentElements',
        'isInDesigner',
        'trustAsHtml',
        'isInAbsoluteLayout',
        'getMarkupId',
        'getFormName',
        'registerComponent',
        'unRegisterComponent',
        'getClientProperty'
    ]);


    TestBed.configureTestingModule({
        declarations: [ ServoyDefaultCombobox ],
        providers: [ ],
        imports: [ServoyPublicTestingModule, NgbModule, FormsModule]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultCombobox);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;

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
    component.servoyApi = jasmine.createSpyObj('ServoyApi', ['getMarkupId', 'trustAsHtml', 'startEdit','registerComponent','unRegisterComponent','getClientProperty']);
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
