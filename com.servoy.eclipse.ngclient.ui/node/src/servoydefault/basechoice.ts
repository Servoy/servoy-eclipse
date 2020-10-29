import { Renderer2, SimpleChanges, Directive, ChangeDetectorRef } from '@angular/core';
import {FormattingService, PropertyUtils} from '../ngclient/servoy_public';
import {ServoyDefaultBaseField} from './basefield';

@Directive()
export abstract class ServoyDefaultBaseChoice extends  ServoyDefaultBaseField {

  selection: any[] = [];
  allowNullinc = 0;

  constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, formattingService: FormattingService) {
    super(renderer, cdRef, formattingService);
    super.attachHandlers();
  }

  svyOnInit() {
      this.onValuelistChange();
      this.setHandlersAndTabIndex();
  }

  abstract setSelectionFromDataprovider();

  svyOnChanges( changes: SimpleChanges ) {
      for ( const property in changes ) {
          switch ( property ) {
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

    this.renderer.listen( element, 'blur', ( event ) => {
      if (this.onFocusLostMethodID) this.onFocusLostMethodID(event);
    });

    this.renderer.listen( element, 'contextmenu', ( e ) => {
      if (this.onRightClickMethodID) this.onRightClickMethodID(e);
    });
  }

  isValueListNull = function(item) {
    return (item.realValue == null || item.realValue === '') && item.displayValue === '';
  };

  /**
   * Gets the selected values (real values from valuelist) as array. The form element should have a dataProviderID assigned in order for this to work.
   * @example var values = %%prefix%%%%elementName%%.getSelectedElements();
   * @return array with selected values
   */
  getSelectedElements() {
    return this.selection
      .map((item, index) =>  {
        if (item === true) return this.valuelistID[index + this.allowNullinc].realValue; })
      .filter(item => item !== null);
  }

}
