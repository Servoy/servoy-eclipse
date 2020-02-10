import { Component, OnInit, HostListener, Renderer2,ViewChild ,ElementRef} from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { ServoyDefaultBaseCombo, Item } from '../basecombo';
import { FormattingService } from '../../ngclient/servoy_public';
 
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
  isLabelFocused = false;
  
  // this is a hack so that this can be none static access because this references in this component to a conditional template
  @ViewChild('input') inputElement: ElementRef;

  constructor(renderer: Renderer2,    
              formattingService : FormattingService) {
    super(renderer,formattingService);
  }
  
  ngOnInit() {
    super.onChanges();
    super.setInitialStyles();

    this.isInputFocused.subscribe(isFocused => {
      this.isOpen = isFocused;
      if (isFocused) {
        this.focusElement(this.getNativeChild());
      }
    });
  }
 
  @HostListener('document:click', ['$event']) onClick(event): void {
    this.getNativeElement().contains(event.target) ? this.isInputFocused.next(true): this.isInputFocused.next(false);
  }
 
  onLabelKeyup(event: KeyboardEvent): void {
    const keyCode = event.keyCode;

    if (!this.isOpen) {
      if (keyCode === 13) {
        this.isInputFocused.next(true);
      } else if (keyCode > 47 && keyCode < 91) {
        this.isInputFocused.next(true);
        this.getNativeChild().value = event.key;
        this.filteredValueList = this.filterList(this.getNativeChild().value);
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
    this.scrollIntoView(this.getNativeElement());
  }
 
  selectItem(item: Item, index): void {
    this.selectedItemIndex = (index === 0 ? this.filteredValueList.length - 1 : index - 1);
    this.selectedItem.next(item);
    this.update(item.realValue);
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
    this.getNativeChild().value = "";
  }

  closeDropdown() {
    this.isInputFocused.next(false);
  };
  
  getFocusElement() : any{
      return this.inputElement.nativeElement;
  }
}
