import { async, ComponentFixture, TestBed, tick, fakeAsync } from '@angular/core/testing';

import { ServoyDefaultCombobox, Item } from './combobox';

import {SabloModule} from '../../sablo/sablo.module'
import {FormatFilterPipe, MnemonicletterFilterPipe, FormattingService, ServoyApi} from '../../ngclient/servoy_public'
import { By } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';

const mockData: Item[] = [
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

fdescribe('SvyCombobox', () => {
  let component: ServoyDefaultCombobox;
  let fixture: ComponentFixture<ServoyDefaultCombobox>;
  let servoyApi;
  let spy: any;

  beforeEach(async(() => {
    servoyApi = jasmine.createSpyObj( "ServoyApi", ["getMarkupId", "trustAsHtml", "getApiData"] )
    servoyApi.getApiData.and.returnValue( mockData );
    
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultCombobox ,FormatFilterPipe ,MnemonicletterFilterPipe ],
      imports:[ SabloModule, FormsModule ],
      providers: [ FormattingService ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultCombobox);
    
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;

    component = fixture.componentInstance;
    component.state = {};
    component.state.model = {};
    component.enabled = true;
    component.state.model.placeholderText = "";
    
    fixture.detectChanges();
  });

  describe('on create', () => {

    it('should create Combobox Component', () => {
      expect(component).toBeTruthy();
    });

    it('should set default values', () => {
      expect(component.isFocused).toEqual(false);
      expect(component.showDropdown).toEqual(false);
    });
  
    it('should call getApiData from servoyApi on ngOnInit', () => {
      expect(servoyApi.getApiData).toHaveBeenCalled(); 
    });
  
    it('should populate Combobox Component with values from servoyApi', () => {
      expect(component.valueList).toEqual(mockData);
    });

    it('should have default selected item', () => {
      const defaultSelectedItem = mockData[0];
      expect(component.selectedComboboxItem).toEqual(defaultSelectedItem);
    });

  });

  describe('on container select', () => {
    let element;
    beforeEach(() => {
      element = fixture.nativeElement.querySelector('input');
      fixture.nativeElement.click();
      fixture.detectChanges();
    });
    
    it('should focus on input field on container click', () => {
      expect(document.activeElement).toEqual(element);;
    });
    
    it('should open dropdown on container click', () => {
      fixture.detectChanges();
      expect(component.isFocused).toEqual(true);
      expect(component.showDropdown).toEqual(true);
    });

    it('should have active class on selected item', () => {
      const defaultSelectedItem = mockData[0];
      fixture.detectChanges();
      const selectedElement = fixture.debugElement.queryAll(By.css('.active'));

      expect(selectedElement.length).toEqual(1);
      expect(selectedElement[0].nativeElement.textContent).toEqual(defaultSelectedItem.displayValue);
    });

    describe('on new item select', () => {
      let listElements;
      let nativeElementToTest;
      beforeEach(() => {
        listElements = fixture.debugElement.queryAll(By.css('.ui-select-choices-row'));
        
        nativeElementToTest = listElements[1].nativeElement;
      });

      it('should set selectedComboboxItem as the selected item', () => {
        expect(nativeElementToTest.classList).not.toContain('active');
        
        nativeElementToTest.click();
        fixture.detectChanges();

        expect(nativeElementToTest.classList).toContain('active');
        expect(component.selectedComboboxItem.displayValue).toEqual(nativeElementToTest.textContent);
      });

      it('should close dropdown on item click', () => {
        nativeElementToTest.dispatchEvent(new Event("click"));
        fixture.detectChanges();

        expect(component.showDropdown).toBeFalsy();
      }); 

      it('should close dropdown on ouside click', () => {
        const tempBody = document.querySelector('body');
        tempBody.click();
        fixture.detectChanges();
        
        expect(component.showDropdown).toBeFalsy();;
      }); 
    });
    
    describe('on starting typing', () => {
      let valueToBeSet;
      let matchingDropdownItems;
      beforeEach(() => {
        valueToBeSet = 'clu';
        matchingDropdownItems = component.valueList.filter(v => v.displayValue.toLowerCase().indexOf(valueToBeSet) !== -1).length;
        component.comboboxInput.nativeElement.value = valueToBeSet;
        component.comboboxInput.nativeElement.dispatchEvent(new Event('input'));
      });
    
      it('should only show dropdown list with matching items', () => {
        expect(component.filteredValueList.length).toEqual(matchingDropdownItems);
      });

      it('should clear input field value and reset found values list on item select', () => {
        component.clearSearchInput();

        expect(component.comboboxInput.nativeElement.value).toEqual('');
        expect(component.filteredValueList.length).toEqual(component.valueList.length);
      }); 
    });

  });

});