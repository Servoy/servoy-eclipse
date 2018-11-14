import {Component, OnInit, Renderer2, SimpleChanges} from '@angular/core';
import {FormattingService} from "../../ngclient/servoy_public";
import {ServoyDefaultBaseField} from "../basefield";
@Component({
  selector: 'servoydefault-password',
  templateUrl: './password.html'
})
export class ServoyDefaultPassword extends ServoyDefaultBaseField implements OnInit {
  constructor(renderer: Renderer2, formattingService: FormattingService) {
    super(renderer, formattingService);
  }

  ngOnInit() {
  }

  ngOnChanges(changes: SimpleChanges){
    super.ngOnChanges(changes);
  }

  onClick(event) {
    if (this.editable == false && this.onActionMethodID) {
      this.onActionMethodID(event);
    }
  };
  
  focus(event){
    if(this.onFocusGainedMethodID){
        this.onFocusGainedMethodID(event);
    }
  }

  blur(event){
    if(this.onFocusLostMethodID)
      this.onFocusLostMethodID(event);
  }

  contextMenu(event){
    if(this.onRightClickMethodID)
      this.onRightClickMethodID(event);
  }

}
