import { Renderer2, SimpleChanges, Directive, ChangeDetectorRef } from '@angular/core';
import { FormattingService, PropertyUtils } from '../ngclient/servoy_public';
import { ServoyDefaultBaseField } from './basefield';

@Directive()
export abstract class ServoyDefaultBaseChoice extends ServoyDefaultBaseField {

  selection: any[] = [];
  allowNullinc = 0;

  constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, formattingService: FormattingService) {
    super(renderer, cdRef, formattingService);
  }

  svyOnInit() {
    this.onValuelistChange();
  }

  abstract setSelectionFromDataprovider();

  svyOnChanges(changes: SimpleChanges) {
    for (const property of Object.keys(changes)) {
      switch (property) {
        case 'dataProviderID':
          this.setSelectionFromDataprovider();
          break;

      }
    }
    super.svyOnChanges(changes);
  }

  setHandlersAndTabIndex() {
    for (let i = 0; i < this.getNativeElement().children.length; i++) {
      const elm: HTMLLabelElement = this.getNativeElement().children[i];
      this.attachEventHandlers(elm.children[0], i);
    }
  }

  onValuelistChange() {
    if (this.valuelistID)
      if (this.valuelistID.length > 0 && this.isValueListNull(this.valuelistID[0])) this.allowNullinc = 1;
    this.setHandlersAndTabIndex();
  }

  baseItemClicked(event, changed, dp) {
    if (event.target.localName === 'label' || event.target.localName === 'span') {
      event.preventDefault();
    }
    if (changed) {
      this.dataProviderID = dp;
      this.pushUpdate();
    }
    event.target.blur();
  }

  attachEventHandlers(element, index) {
    if (!element)
      element = this.getNativeElement();

    if (this.onFocusGainedMethodID) {
      this.renderer.listen(element, 'focus', (event) => {
        this.onFocusGainedMethodID(event);
      });
    }

    if (this.onFocusLostMethodID) {
      this.renderer.listen(element, 'blur', (event) => {
        this.onFocusLostMethodID(event);
      });
    }

    if (this.onRightClickMethodID) {
      this.renderer.listen(element, 'contextmenu', (e) => {
        this.onRightClickMethodID(e);
      });
    }
  }

  isValueListNull = function (item) {
    return (item.realValue == null || item.realValue === '') && item.displayValue === '';
  };

  /**
   * Gets the selected values (real values from valuelist) as array. The form element should have a dataProviderID assigned in order for this to work.
   * @example var values = %%prefix%%%%elementName%%.getSelectedElements();
   * @return array with selected values
   */
  getSelectedElements() {
    return this.selection
      .map((item, index) => {
        if (item === true) return this.valuelistID[index + this.allowNullinc].realValue;
      })
      .filter(item => item !== null);
  }

}
