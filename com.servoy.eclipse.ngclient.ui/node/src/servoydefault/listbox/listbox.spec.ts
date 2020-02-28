import { async, ComponentFixture, TestBed, fakeAsync } from '@angular/core/testing';

import { SabloModule } from '../../sablo/sablo.module'
import { ServoyPublicModule } from '../../ngclient/servoy_public.module'

import { ServoyDefaultListBox } from './listbox';
import {  FormattingService, TooltipService, ServoyApi} from '../../ngclient/servoy_public'

import { FormsModule } from '@angular/forms';
import { Item } from '../basecombo';
import { By } from '@angular/platform-browser';
import { DebugElement, ChangeDetectionStrategy, SimpleChange } from '@angular/core';

const mockDataValueList: Item[] = [
  {
    realValue: 1,
    displayValue: "Bucuresti"
  },
  {
    realValue: 2,
    displayValue: "Timisoara"
  },
  {
    realValue: 3,
    displayValue: "Cluj"
  },
];


describe("ServoyDefaultListBox", () => {
  let component: ServoyDefaultListBox;
  let fixture: ComponentFixture<ServoyDefaultListBox>;
  let debugEl: DebugElement;
  let element: HTMLElement;
  let servoyApi;

  beforeEach(async(() => {
    servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","isInDesigner"]);

    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultListBox],
      imports: [SabloModule,FormsModule, ServoyPublicModule],
      providers: [FormattingService,TooltipService]
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
    element = debugEl.nativeElement;
    fixture.detectChanges();
    
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

  it('should show `Bucuresti Timisoara Cluj` as options', async(() => {
    fixture.whenStable().then(() => {
      const options = debugEl.queryAll(By.css('select option'));
      expect(options[0].nativeElement.text).toBe('Bucuresti');
      expect(options[1].nativeElement.text).toBe('Timisoara');
      expect(options[2].nativeElement.text).toBe('Cluj');
    });
  }));

  it('should call update method', () => {
    spyOn(component, 'update');
    const select = debugEl.query(By.css('select')).nativeElement;
    select.value = select.options[0].value;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(component.update).toHaveBeenCalled();
  });

  it('should call multiUpdate method', () => {
    component.multiselectListbox = true;
    fixture.detectChanges();
    spyOn(component, 'multiUpdate');
    const select = debugEl.query(By.css('select')).nativeElement;
    select.values = [select.options[0].value, select.options[1].value];
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

  // TODO : update test
  xit('should have as selectedValues: Bucuresti & Timisoara', () => {
    component.multiselectListbox = true;
    spyOn(component, 'ngOnChanges');
    fixture.detectChanges();
    const select = debugEl.query(By.css('select')).nativeElement;
    select.values = [select.options[0].value, select.options[1].value];
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(component.ngOnChanges).toHaveBeenCalled();
    expect(component.selectedValues).toEqual(['0: 1', '1: 2']);
  });

  // TODO : update test 
  it('should call ngOnChanges', fakeAsync(() => {
    fixture.whenStable().then(() => {
      component.multiselectListbox = true;
      fixture.detectChanges();
      const changes = {dataProviderID: new SimpleChange(1, 2, false)};
      spyOn(component, 'ngOnChanges');
      component.ngOnChanges(changes);
      fixture.detectChanges();
      expect(component.ngOnChanges).toHaveBeenCalled();
    });
  }));

  xit( 'should render markupid ', () => {
    component.servoyApi.getMarkupId.and.returnValue( "myid")
    fixture.detectChanges();
    expect(element.id).toBe("myid");
  });

  it( 'should have called servoyApi.getMarkupId', () => {
     expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
  });

});
