import { async, ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ElementRef } from '@angular/core';
import { By } from '@angular/platform-browser';
import { Item } from '../basecombo';
import { ServoyDefaultTypeahead } from './typeahead';
import { FormattingService, ServoyApi } from '../../ngclient/servoy_public';

const eventEnter: KeyboardEvent = new KeyboardEvent('keyup', {'key': 'Enter'});
const eventC: KeyboardEvent = new KeyboardEvent('keyup', {'key': 'c'});
const eventInput: Event = new Event('input');

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

Object.defineProperty(eventEnter, 'keyCode', {
  get : function() {
    return 13;
  }
});
Object.defineProperty(eventC, 'keyCode', {
  get : function() {
    return 67;
  }
});
 
function addInputToEnterEvent(fixture) {
  Object.defineProperty(eventEnter, 'target', {
    get : function() {
      return fixture.debugElement.query(By.css('input')).nativeElement;
    }
  });
}
function addInputToKeyEvent(fixture) {
  Object.defineProperty(eventC, 'target', {
    get : function(): ElementRef {
      return fixture.debugElement.query(By.css('input')).nativeElement;
    }
  });
}
 
describe('TypeaheadComponent', () => {
  let component: ServoyDefaultTypeahead;
  let fixture: ComponentFixture<ServoyDefaultTypeahead>;
  let servoyApi;
 
  beforeEach(async(() => {
    servoyApi = jasmine.createSpyObj( "ServoyApi", ["getMarkupId", "trustAsHtml"]);

    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultTypeahead ],
      providers: [ FormattingService ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultTypeahead);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;

    addInputToEnterEvent(fixture);
    addInputToKeyEvent(fixture);
    
    component = fixture.componentInstance;
    component.valuelistID = mockData;
    component.dataProviderID = 3;
    component.ngOnInit();

    fixture.detectChanges();
  });
 
  it('should create component', () => {
    expect(component).toBeTruthy();
  });
 
  it('should set initial list of values', () => {
    expect(component.valuelistID).toEqual(component.filteredValueList);
  });
 
  it('should set initial selected item based on index', () => {
    const itemBasedOnIndex: Item = component.filteredValueList[component.selectedItemIndex];
    expect(component.selectedItem.getValue()).toEqual(itemBasedOnIndex);
  });
 
  it('should set initial dropdown closed', () => {
    expect(component.isOpen).toBeFalsy();
  });
 
  describe('on click events', () => {
    let bodyElement;
    beforeEach(() => {
      bodyElement = document.getElementsByTagName('body')[0];
 
      component.onInputFocus();
      fixture.detectChanges();
    });
 
    it('should open dropdown on container click', () => {
      expect(component.isOpen).toBeTruthy();
    });
 
    it('should set selected item on item click', () => {
      const indexOfItemToSelect = 2;
      const itemToSelectOnIndex = component.filteredValueList[indexOfItemToSelect];

      component.selectItem(itemToSelectOnIndex, indexOfItemToSelect);
      fixture.detectChanges();
 
      expect(component.dataProviderID).toEqual(itemToSelectOnIndex.realValue);
    });
 
    it('should close dropdown on outside click', () => {
      bodyElement.click();
      fixture.detectChanges();
 
      expect(component.isOpen).toBeFalsy();
    });
  });
 
  describe('on keyboard events', () => {
    describe('on Down key', () => {
      let itemPossitionAfterDownKey;
      beforeEach(() => {
        itemPossitionAfterDownKey = component.activeItemIndex + 1;

        component.activateNextListItem();
        fixture.detectChanges();
      });
 
      it('should select next item in available list', () => {
        const activeItemPossition = component.activeItemIndex;
        expect(activeItemPossition).toEqual(itemPossitionAfterDownKey);
      });
    });
 
    describe('on Up key', () => {
      let itemPossitionAfterUpKey;
      beforeEach(() => {
        itemPossitionAfterUpKey = component.filteredValueList.length - 1;

        component.activatePreviousListItem();
        fixture.detectChanges();
      });
 
      it('should select previous item in available list', () => {
        const activeItemPossition = component.activeItemIndex;
        expect(activeItemPossition).toEqual(itemPossitionAfterUpKey);
      });
 
    });
 
    describe('on input search successfull', () => {
      const keyToSearchBy = 'c';
      let inputElement;
      beforeEach(() => {
        inputElement = fixture.debugElement.query(By.css('input')).nativeElement;
        inputElement.dispatchEvent(eventInput);
        inputElement.value = keyToSearchBy;
        fixture.detectChanges();
      });
 
      it('should only show matching items', () => {
        const matchingItems = component.valuelistID.filter(d => d.displayValue.toLowerCase().indexOf(keyToSearchBy) !== -1);
        expect( component.filterList(inputElement.value)).toEqual(matchingItems);
      });
    });
 
    describe('on input search un-successfull', () => {
      const keyToSearchBy = 'xyz';
      let inputElement;
      beforeEach(() => {
        inputElement = fixture.debugElement.query(By.css('input')).nativeElement;
        inputElement.dispatchEvent(eventInput);
        inputElement.value = keyToSearchBy;
        fixture.detectChanges();
      });
 
      it('should have empty filtered list', () => {
        const matchingItems = component.valuelistID.filter(d => d.displayValue.toLowerCase().indexOf(keyToSearchBy) !== -1);
        expect(component.filterList(inputElement.value).length).toEqual(matchingItems.length);
      });
 
      it('should set active index to first element', () => {
        expect(component.activeItemIndex).toEqual(0);
      });
    });
  });
 
});
