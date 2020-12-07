import { Component, Input, ChangeDetectorRef, Renderer2, SimpleChanges, ViewChild, ElementRef } from '@angular/core';
import { ServoyDefaultBaseField } from '../basefield';
import { FormattingService, PropertyUtils } from '../../ngclient/servoy_public';
import { AngularEditorConfig, AngularEditorComponent } from '@kolkov/angular-editor';

@Component({
  selector: 'servoydefault-htmlarea',
  templateUrl: './htmlarea.html',
})
export class ServoyDefaultHtmlarea extends ServoyDefaultBaseField {

  @ViewChild(AngularEditorComponent) editor: AngularEditorComponent;

  config: AngularEditorConfig = {
    editable: true,
    spellcheck: true,
    translate: 'no',
    defaultParagraphSeparator: 'p'
  };

  constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, formattingService: FormattingService) {
    super(renderer, cdRef, formattingService);
  }

  attachFocusListeners() {
    if (this.onFocusGainedMethodID) {
      this.editor.focusEvent.subscribe(() => {
        this.onFocusGainedMethodID(new CustomEvent('focus'));
      });
    }

    this.editor.blurEvent.subscribe(() => {
      this.pushUpdate();
      if (this.onFocusLostMethodID) this.onFocusLostMethodID(new CustomEvent('blur'));
    });
  }

  svyOnInit() {
    super.svyOnInit();

    // ugly hack to fix the height
    const nativeElement = this.getNativeElement();
    const componentHeight = nativeElement.offsetHeight;
    // let toolBarHeight = nativeElement.childNodes[0].childNodes[0].childNodes[1].childNodes[1].offsetHeight;
    const initialContentHeight = nativeElement.childNodes[0].childNodes[0].childNodes[2].childNodes[0].offsetHeight;
    const initialEditorHeight = nativeElement.childNodes[0].childNodes[0].offsetHeight;

    this.renderer.setStyle(nativeElement.childNodes[0].childNodes[0].childNodes[2].childNodes[0], 'height', (initialContentHeight + componentHeight - initialEditorHeight) + 'px');
  }

  svyOnChanges(changes: SimpleChanges) {
    if (changes) {
      for (const property of Object.keys(changes)) {
        const change = changes[property];
        switch (property) {
          case 'styleClass':
            if (change.previousValue)
              this.renderer.removeClass(this.getNativeElement(), change.previousValue);
            if (change.currentValue)
              this.renderer.addClass(this.getNativeElement(), change.currentValue);
            break;
          case 'scrollbars':
            if (change.currentValue) {
              const element = this.getNativeChild().textArea;
              PropertyUtils.setScrollbars(element, this.renderer, change.currentValue);
            }
            break;
          case 'editable':
            this.config.editable = this.editable;
            break;

        }
      }
    }
    super.svyOnChanges(changes);
  }

  getFocusElement() {
    return this.editor.textArea.nativeElement;
  }

  requestFocus() {
      this.editor.focus();
  }

  public selectAll() {
    const range = document.createRange();
    range.selectNodeContents(this.getFocusElement());
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
  }

  public getScrollX(): number {
    return this.getNativeElement().scrollLeft;
  }

  public getScrollY(): number {
    return this.getNativeElement().scrollTop;
  }

  public setScroll(x: number, y: number) {
    this.getNativeElement().scrollLeft = x;
    this.getNativeElement().scrollTop = y;
  }
}
