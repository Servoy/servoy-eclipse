import { Component, Input, ChangeDetectorRef, Renderer2, SimpleChanges, ViewChild, ElementRef } from '@angular/core';
import { ServoyDefaultBaseField } from "../../servoydefault/basefield";
import { FormattingService } from "../../ngclient/servoy_public";
import { AngularEditorConfig } from '@kolkov/angular-editor';

@Component({
  selector: 'servoyextra-htmlarea',
  templateUrl: './htmlarea.html',
})
export class ServoyExtraHtmlarea extends ServoyDefaultBaseField{

  @Input() editable;
  
  config: AngularEditorConfig = {
          editable: true,
          spellcheck: true,
          translate: 'no',
          defaultParagraphSeparator: 'p'
        };
  
  constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, formattingService: FormattingService) {
    super(renderer, cdRef, formattingService);
  }
  
  svyOnInit() {
      super.svyOnInit();
      // ugly hack to fix the height
      let nativeElement = this.getNativeElement();
      let componentHeight = nativeElement.offsetHeight;
      //let toolBarHeight = nativeElement.childNodes[0].childNodes[0].childNodes[1].childNodes[1].offsetHeight;
      let initialContentHeight = nativeElement.childNodes[0].childNodes[0].childNodes[2].childNodes[0].offsetHeight;
      let initialEditorHeight = nativeElement.childNodes[0].childNodes[0].offsetHeight;
      
      this.renderer.setStyle( nativeElement.childNodes[0].childNodes[0].childNodes[2].childNodes[0], "height",  (initialContentHeight + componentHeight - initialEditorHeight) +'px');
  }
}
