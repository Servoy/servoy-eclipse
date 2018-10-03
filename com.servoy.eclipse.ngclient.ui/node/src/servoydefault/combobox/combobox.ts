import { Component, OnInit, HostListener, ElementRef, Renderer2, Input } from '@angular/core';
import { BehaviorSubject, of } from 'rxjs';
import { ServoyDefaultBaseCombo, Item } from '../basecombo';
import { FormattingService, ServoyApi } from '../../ngclient/servoy_public';
 
@Component({
  selector: 'servoydefault-combo',
  templateUrl: './combobox.html',
  styleUrls: [
    '../basecombo.css',
    './combobox.css'
  ]
})
export class ServoyDefaultCombobox extends ServoyDefaultBaseCombo implements OnInit {
  isInputFocused: BehaviorSubject<boolean> = new BehaviorSubject(false);
  @Input() state: any;

  isLabelFocused = false;

  constructor(renderer: Renderer2,    
              formattingService : FormattingService,
              private el: ElementRef) { 
    super(renderer,formattingService);
  }
  
  ngOnInit() {
    super.onChanges();
    super.setInitialStyles(this.state);

    this.isInputFocused.subscribe(isFocused => {
      this.isOpen = isFocused;
      if (isFocused) {
        this.focusElement(this.inputElement.nativeElement);
      }
      this.filteredValueList = this.valueList;
    });

    this.selectedItem.subscribe(d => {
      this.update(d.realValue);
      this.isInputFocused.next(false);
    });
  }
 
  @HostListener('document:click', ['$event']) onClick(event): void {
    if (!this.el.nativeElement.contains(event.target) ) {
      this.isInputFocused.next(false);
    } else {
      this.isInputFocused.next(true);
    }
  }
 
  onLabelKeyup(event: KeyboardEvent): void {
    const keyCode = event.keyCode;

    if (!this.isOpen) {
      if (keyCode === 13) {
        this.isInputFocused.next(true);
      } else if (keyCode > 47 && keyCode < 91) {
        this.isInputFocused.next(true);
        this.inputElement.nativeElement.value = event.key;
        this.filteredValueList = this.filterList(this.inputElement.nativeElement.value);
        if (this.filteredValueList.length < this.activeItemIndex) {
          this.activeItemIndex = 0;
        }
      }
    }
  }
 
  onInputKeyup(event: KeyboardEvent, element): void {
    const keyCode = event.keyCode;
 
    if (keyCode === 13) { // Enter key
      this.selectedItem.next(this.filteredValueList[this.activeItemIndex] ? this.filteredValueList[this.activeItemIndex] :
        this.selectedItem.getValue());
      this.focusElement(element);
      event.target['value'] = '';
    } else if (keyCode === 38) { // Up key
      this.activatePreviousListItem();
    } else if (keyCode === 40) { // Down key
      this.activateNextListItem();
    }
    this.scrollIntoView(this.el);
  }
 
  selectItem(item: Item, index): void {
    this.selectedItemIndex = (index === 0 ? this.filteredValueList.length - 1 : index - 1);
    this.selectedItem.next(item);
  }
 
  onInput(event): void {
    if (event.target.value !== '') {
      this.filteredValueList = this.filterList(event.target.value);
      this.activeItemIndex = 0;
    } else {
      this.filteredValueList = this.valueList;
      this.activeItemIndex = 0;
    }
  }
 
  onLabelClick(): void {
    this.isInputFocused.next(true);
    this.inputElement.nativeElement.value = '';
  }

  closeDropdown() {
    this.isInputFocused.next(false);
  };
}