import {Component, Input, OnInit, Renderer2, SimpleChanges,ViewChild, ElementRef, AfterViewInit} from '@angular/core';
import {ServoyDefaultBaseField} from "../basefield";
import {FormattingService} from "../../ngclient/servoy_public";
import { AngularEditorConfig } from '@kolkov/angular-editor';

@Component({
  selector: 'servoydefault-htmlarea',
  templateUrl: './htmlarea.html',
})
export class ServoyDefaultHtmlarea extends ServoyDefaultBaseField implements AfterViewInit {

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
  
  ngAfterViewInit() {
      // ugly hack to fix the height
      let nativeElement = this.getNativeElement();
      let componentHeight = nativeElement.offsetHeight;
      //let toolBarHeight = nativeElement.childNodes[0].childNodes[0].childNodes[1].childNodes[1].offsetHeight;
      let initialContentHeight = nativeElement.childNodes[0].childNodes[0].childNodes[2].childNodes[0].offsetHeight;
      let initialEditorHeight = nativeElement.childNodes[0].childNodes[0].offsetHeight;
      
      this.renderer.setStyle( nativeElement.childNodes[0].childNodes[0].childNodes[2].childNodes[0], "height",  (initialContentHeight + componentHeight - initialEditorHeight) +'px');
  }
}
