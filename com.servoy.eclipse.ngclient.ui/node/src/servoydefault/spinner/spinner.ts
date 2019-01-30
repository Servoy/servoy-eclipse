import {Component, Renderer2, SimpleChanges} from '@angular/core';
import {ServoyDefaultBaseField} from "../basefield";
import {FormattingService} from "../../ngclient/servoy_public";

@Component( {
  selector: 'servoydefault-spinner',
  templateUrl: './spinner.html',
  styleUrls: ['./spinner.css']
} )
export class ServoyDefaultSpinner extends ServoyDefaultBaseField {

  selection: any;
  private counter = 0;
  constructor(renderer: Renderer2, formattingService: FormattingService) {
    super(renderer, formattingService);
  }

  ngOnInit(){
    this.selection = this.getSelectionFromDataprovider();
    this.addHandlersToInputAndSpinnerButtons();
    super.ngOnInit();
  }

  ngOnChanges(changes: SimpleChanges){
    for ( let property in changes ) {
      switch (property) {
        case 'dataProviderID':
          this.selection = this.getSelectionFromDataprovider();
          break;
      }
    }
    super.ngOnChanges(changes);
  }

  addHandlersToInputAndSpinnerButtons(){
    let spinnerButtons = this.getNativeElement().getElementsByTagName('button');

    this.renderer.listen( this.getNativeChild(), 'scroll', e => this.scrollCallback(e));
    this.renderer.listen(this.getNativeChild(), 'keydown keypress',e => this.keydownKeypressCallback(e));

    this.renderer.listen(spinnerButtons[0], 'click', e => this.increment());
    this.renderer.listen(spinnerButtons[1], 'click', e => this.decrement());

    for(let i = 0; i < spinnerButtons.length; i++) {
        if (this.onActionMethodID)
          this.renderer.listen(spinnerButtons[i], 'click', e => this.onActionMethodID(e));

        if (this.onFocusLostMethodID)
          this.renderer.listen(spinnerButtons[i], 'blur', e => this.onFocusLostMethodID(e));

        if (this.onFocusGainedMethodID)
          this.renderer.listen(spinnerButtons[i], 'focus', e => this.onFocusGainedMethodID(e));
    }
  }

  //copied from angularui timepicker
  isScrollingUp(e) {
    if (e.originalEvent) {
      e = e.originalEvent;
    }
    //pick correct delta variable depending on event
    let delta = (e.wheelDelta) ? e.wheelDelta : -e.deltaY;
    return (e.detail || delta > 0);
  };

  scrollCallback(e){
    if (!this.isDisabled()) {
      this.isScrollingUp(e) ? this.increment() : this.decrement();
    }
    e.preventDefault();
  }

  keydownKeypressCallback(e){
    if (!this.isDisabled()) {
      if (e.which == 40)
        this.decrement();
      if (e.which == 38)
        this.increment();
    }
  }

  isDisabled() {
    return this.enabled == false || this.editable == false;
  };

  increment(){
    if (this.valuelistID) {
      this.counter = this.counter < this.valuelistID.length - 1 ? this.counter + 1 : 0;
      this.dataProviderID = this.valuelistID[this.counter].realValue
    }
    this.update(this.dataProviderID);
  };

  decrement() {
    if (this.valuelistID) {
      this.counter = this.counter > 0 ? this.counter - 1 : this.valuelistID.length - 1;
      this.dataProviderID = this.valuelistID[this.counter].realValue
    }
    this.update(this.dataProviderID);
  };

  /**
   * Request the focus to this spinner.
   * @example %%prefix%%%%elementName%%.requestFocus();
   * @param mustExecuteOnFocusGainedMethod (optional) if false will not execute the onFocusGained method; the default value is true
   */
  requestFocus(mustExecuteOnFocusGainedMethod) {
    if (mustExecuteOnFocusGainedMethod === false && this.onFocusGainedMethodID) {

      this.getNativeChild().removeEventListener('focus', this.onFocusGainedMethodID);
      this.getNativeChild().focus();
      this.renderer.listen(this.getNativeChild(),'focus', this.onFocusGainedMethodID);

    } else {
      this.getNativeChild().focus();
    }
  }

  getSelectionFromDataprovider() {
    if (!this.dataProviderID) {
      this.counter = 0;
      return undefined
    }

    for (let i = 0; i < this.valuelistID.length; i++) {
      let item = this.valuelistID[i];
      if (item && item.realValue && this.dataProviderID == item.realValue) {
        let displayFormat = undefined;
        let type = undefined;
        if (this.format && this.format.display)
          displayFormat = this.format.display;
        if (this.format && this.format.type)
          type = this.format.type;
        this.counter = i;
        return item.displayValue;
      }
    }
  }

}

