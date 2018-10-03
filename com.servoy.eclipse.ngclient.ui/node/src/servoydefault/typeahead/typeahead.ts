import { Component, OnInit, HostListener, ElementRef, Renderer2, Input } from '@angular/core';
import { BehaviorSubject, interval, of } from 'rxjs';
import { ServoyDefaultBaseCombo, Item } from '../basecombo';
import { FormattingService, ServoyApi } from '../../ngclient/servoy_public';
 
@Component({
  selector: 'servoydefault-typeahead',
  templateUrl: './typeahead.html',
  styleUrls: [
    '../basecombo.css',
    './typeahead.css'
  ]
})
export class ServoyDefaultTypeahead extends ServoyDefaultBaseCombo implements OnInit {
  activeItemIndex = this.selectedItemIndex;
  @Input() state: any;
 
  constructor(renderer: Renderer2,    
              formattingService : FormattingService,
              private el: ElementRef) { 
    super(renderer,formattingService);
  }
 
  ngOnInit() {
    super.onChanges();
    super.setInitialStyles(this.state);
    
    this.selectedItem.subscribe(item => {
      if (item) {
        this.update(item.realValue);
        this.setInputValue();
        
        this.filteredValueList = this.valueList;

        const shortInterval = interval(100).subscribe(() => {
          this.isOpen = false;
          shortInterval.unsubscribe();
        });
      }
    });
  }
 
  @HostListener('document:click', ['$event']) onOutsideClick(event): void {
    if (!this.el.nativeElement.contains(event.target) ) {
      this.isOpen = false;
    } else {
      this.isOpen = true;
    }
  }
 
  onInputKeyup(event): void {
    const keyCode = event.keyCode;
    
    if (this.isOpen) {
      if (keyCode === 13) { // Enter key
        this.selectedItem.next(this.filteredValueList[this.activeItemIndex] ? this.filteredValueList[this.activeItemIndex] :
          this.selectedItem.getValue());
        this.setInputValue();
      } else if (keyCode === 38) { // Up key
        this.activatePreviousListItem();
      } else if (keyCode === 40) { // Down key
        this.activateNextListItem();
      } else if (keyCode <= 47 && keyCode >= 91) {
        this.isOpen = false;
      }
      this.scrollIntoView(this.el);
    }
  }
 
  selectItem(item, index): void {
    this.selectedItemIndex = index - 1;
    this.selectedItem.next(item);
    this.setInputValue();
    this.focusElement(this.inputElement.nativeElement);
  }
 
  onInput(event): void {
    if (event.target.value !== '') {
      this.filteredValueList = this.filterList(event.target.value);
      if (this.filteredValueList.length > 0) {
        this.isOpen = true;
        this.activeItemIndex = 0;
      }
    } else {
      this.isOpen = false;
      this.activeItemIndex = 0;
    }
  }

  closeDropdown() {
    this.isOpen = false;
  };

  onInputFocus() {
    this.isOpen = true;
  }
 
  onInputBlur(): void {
    this.setInputValue();
  }
 
  setInputValue(): void {
    const interv = interval(100).subscribe(() => {
      this.inputElement.nativeElement.value = this.selectedItem.getValue() ? this.selectedItem.getValue().displayValue : '';
      interv.unsubscribe();
    });
  }
}