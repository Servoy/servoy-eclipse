import { Renderer2, ElementRef, ViewChild, SimpleChanges } from '@angular/core';

import { FormattingService } from '../ngclient/servoy_public'

import { ServoyDefaultBaseField } from './basefield';
import { interval, BehaviorSubject } from 'rxjs';

export interface Item {
  displayValue: any,
  realValue: any
};

export class ServoyDefaultBaseCombo extends ServoyDefaultBaseField {
  @ViewChild('input') inputElement: ElementRef;

  styles: Object = {};
  valueList: Item[];
  filteredValueList: Item[];
  selectedItem: BehaviorSubject<Item> = new BehaviorSubject(null);
  selectedItemIndex;
  activeItemIndex;
  isOpen = false;

  constructor(renderer: Renderer2,    
              formattingService : FormattingService) { 
    super(renderer,formattingService);
  }
  
  ngOnChanges( changes: SimpleChanges ) {
    this.onChanges();
  }
  
  onChanges() {
    this.filteredValueList = this.valuelistID;
    if(this.filteredValueList){
      this.selectedItemIndex = this.filteredValueList.map((a) => a.realValue).indexOf(this.dataProviderID);
      this.selectTheItem();
    }
  }

  selectTheItem() {
    this.selectedItem.next(this.filteredValueList.filter(item => item.realValue === this.dataProviderID)[0]);
  }

  setInitialStyles(): void {
    if(this.size)
      Object.keys(this.size).forEach(key => {
          this.styles[key.toString()] = this.size[key] + 'px';
      });
  } 

  public scrollIntoView(el): void {
    const elementToScrollTo = el.getElementsByClassName('ui-select-choices-row')[this.activeItemIndex];
    if (elementToScrollTo) {
      elementToScrollTo.scrollIntoView({behavior: 'smooth', block: 'nearest'});
    }
  }

  public focusElement(elementToFocusOn): void {
    const interv = interval(10).subscribe(() => {
      if(elementToFocusOn){
        elementToFocusOn.focus();
        interv.unsubscribe();
      }
    });
  }
  
  public onInputKeyDown(event: KeyboardEvent): void {
    const keyCode = event.keyCode;
    keyCode === 38 || keyCode === 40 ? event.preventDefault(): this.onNavigateAvay(event);
  }
  
  public filterList(valueToFilterBy) {
    return this.valuelistID.filter(d =>
      d.displayValue.toLowerCase().indexOf(valueToFilterBy.toLowerCase()) !== -1);
  }
  
  public isItemSelected(index): boolean {
    return this.activeItemIndex === index;
  }

  public activatePreviousListItem() {
    if(!this.activeItemIndex) this.activeItemIndex = 0;
    this.activeItemIndex = (this.activeItemIndex <= 1 ? this.filteredValueList.length - 1 : this.activeItemIndex - 1);
  }

  public activateNextListItem() {
    this.activeItemIndex = (this.activeItemIndex === this.filteredValueList.length - 1 ? 0 : this.activeItemIndex + 1);
  }

  public isElementFocused(target) {
     return document.activeElement === target
  }

  public getTabindex() {
    return this.isOpen ? 0 : -1;
  }

  public onNavigateAvay(event) {
    if (event.keyCode === 9 || (event.keyCode === 9 && event.shiftKey)) {
      this.closeDropdown();
    }
  }
  protected getNativeChild():any {
    if(this.inputElement)
      return this.inputElement.nativeElement;
  }

  closeDropdown() {};
  
}
