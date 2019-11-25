import {Component, Input, OnInit, Renderer2, SimpleChanges} from '@angular/core';
import {ServoyDefaultBaseField} from "../basefield";
import {FormattingService} from "../../ngclient/servoy_public";
import { AngularEditorConfig } from '@kolkov/angular-editor';

@Component({
  selector: 'servoydefault-htmlarea',
  templateUrl: './htmlarea.html',
})
export class ServoyDefaultHtmlarea extends ServoyDefaultBaseField implements OnInit {

  @Input() editable;

  config: AngularEditorConfig = {
          editable: true,
          spellcheck: true,
          translate: 'no',
          defaultParagraphSeparator: 'p'
        };
  
  constructor(renderer: Renderer2, formattingService: FormattingService) {
    super(renderer, formattingService);
  }
  
  ngOnInit(){
    super.ngOnInit();
  }

  ngOnChanges(changes: SimpleChanges){
    super.ngOnChanges(changes);
  }

}
