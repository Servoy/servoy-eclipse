import { Renderer2, ElementRef, ViewChild, SimpleChanges, AfterViewInit, AfterContentInit } from '@angular/core';

import { FormattingService } from '../ngclient/servoy_public'

import { ServoyDefaultBaseField } from './basefield';
import { interval, BehaviorSubject, Observable, defer } from 'rxjs';

export interface Item {
  displayValue: any,
  realValue: any
};

export class ServoyDefaultBaseCombo extends ServoyDefaultBaseField {
  @ViewChild('inputElement') inputElement: ElementRef;

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
    defer(() => this.servoyApi.getDataProviderID()).subscribe((d: number) => {
      this.selectedItemIndex = d - 1;
    });

    this.activeItemIndex = this.selectedItemIndex;
    this.valueList = this.servoyApi.getApiData();
    this.filteredValueList = this.valueList;
    this.selectTheItem();
  }

  selectTheItem() {
    this.selectedItem.next(this.filteredValueList[this.activeItemIndex]);
  }

  setInitialStyles(state): void {
    Object.keys(state.model.size).forEach(key => {
        this.styles[key.toString()] = state.model.size[key] + 'px';
    });
  } 

  public scrollIntoView(el): void {
    const elementToScrollTo = el.nativeElement.getElementsByClassName('ui-select-choices-row')[this.activeItemIndex];
    if (elementToScrollTo) {
      elementToScrollTo.scrollIntoView({behavior: 'smooth', block: 'nearest'});
    }
  }

  public focusElement(elementToFocusOn): void {
    const interv = interval(10).subscribe(() => {
      elementToFocusOn.focus();
      interv.unsubscribe();
    });
  }
  
  public onInputKeyDown(event: KeyboardEvent): void {
    const keyCode = event.keyCode;
    if (keyCode === 38 || keyCode === 40) {
      event.preventDefault();
    } else {
      this.onNavigateAvay(event);
    }
  }
  
  public filterList(valueToFilterBy) {
    return this.valueList.filter(d =>
      d.displayValue.toLowerCase().indexOf(valueToFilterBy.toLowerCase()) !== -1);
  }
  
  public isItemSelected(index): boolean {
    return this.activeItemIndex === index;
  }

  public activatePreviousListItem() {
    this.activeItemIndex = (this.activeItemIndex <= 1 ? this.filteredValueList.length - 1 : this.activeItemIndex - 1);
  }

  public activateNextListItem() {
    this.activeItemIndex = (this.activeItemIndex === this.filteredValueList.length - 1 ? 0 : this.activeItemIndex + 1);
  }

  public isElementFocused(target) {
    if (document.activeElement === target) {
      return true;
    }
    return false;
  }

  public getTabindex() {
    return this.isOpen ? 0 : -1;
  }

  public onNavigateAvay(event) {
    if (event.keyCode === 9 || (event.keyCode === 9 && event.shiftKey)) {
      this.closeDropdown();
    }
  }

  closeDropdown() {};
  
}