import { async, ComponentFixture, TestBed, fakeAsync } from '@angular/core/testing';

import { SabloModule } from '../../sablo/sablo.module';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';

import { ServoyDefaultListBox } from './listbox';
import {  FormattingService, TooltipService, ServoyApi} from '../../ngclient/servoy_public';

import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { DebugElement, ChangeDetectionStrategy, SimpleChange } from '@angular/core';
import { IValuelist } from '../../sablo/spectypes.service';

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
  let servoyApi;

  beforeEach(async(() => {
    servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId', 'isInDesigner']);

    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultListBox],
      imports: [SabloModule, FormsModule, ServoyPublicModule],
      providers: [FormattingService, TooltipService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.overrideComponent(ServoyDefaultListBox, {
      set: {
        selector: 'servoydefault-listbox',
        templateUrl: './listbox.html',
        changeDetection: ChangeDetectionStrategy.Default
      }
    }).createComponent(ServoyDefaultListBox);

    component = fixture.componentInstance;
    component.servoyApi =  servoyApi as ServoyApi;
    debugEl = fixture.debugElement;

    // set some default values
    component.valuelistID = mockDataValueList;
    component.multiselectListbox = false;
    component.dataProviderID = 1;

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('multiselect should be false by default', () => {
    expect(component.multiselectListbox).toBeFalse();
  });

  it('should show `Bucuresti Timisoara Cluj` as options', () => {
      const options = debugEl.queryAll(By.css('select option'));
      expect(options[0].nativeElement.text).toBe('Bucuresti');
      expect(options[1].nativeElement.text).toBe('Timisoara');
      expect(options[2].nativeElement.text).toBe('Cluj');
  });

  it('should call update method', () => {
    spyOn(component, 'update');
    const select = debugEl.query(By.css('select')).nativeElement;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(component.update).toHaveBeenCalled();
  });

  it('should call multiUpdate method', () => {
    component.multiselectListbox = true;
    fixture.detectChanges();
    spyOn(component, 'multiUpdate');
    const select = debugEl.query(By.css('select')).nativeElement;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(component.multiUpdate).toHaveBeenCalled();
  });

  it('should have data provider id = 1', () => {
    const select = debugEl.query(By.css('select')).nativeElement;
    select.value = select.options[0].value;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(component.dataProviderID).toEqual(1);
  });
 
  it('should test selectedValues', () => {
    component.multiselectListbox = true;
    const select = debugEl.query(By.css('select')).nativeElement;
    component.dataProviderID = "test1\ntest2"
        component.ngOnChanges({
            dataProviderID: new SimpleChange(null, component.dataProviderID, true)
        });
    fixture.detectChanges();
    expect(component.selectedValues[0]).toEqual('test1');
    expect(component.selectedValues[1]).toEqual('test2');
  });

  it('should call ngOnChanges', async() => {
    fixture.whenStable().then(() => {
      component.multiselectListbox = true;
      spyOn(component, 'ngOnChanges');
      component.ngOnChanges({dataProviderID: new SimpleChange(1, 2, false)});
      fixture.detectChanges();
      expect(component.ngOnChanges).toHaveBeenCalled();
    });
  });

  it( 'should render markupid ', () => {
    component.servoyApi.getMarkupId.and.returnValue( 'myid');
    const select = debugEl.query(By.css('select')).nativeElement;
    fixture.detectChanges();
    expect(select.id).toBe('myid');
  });

  it( 'should have called servoyApi.getMarkupId', () => {
     expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
  });

});
