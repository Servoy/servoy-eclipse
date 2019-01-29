import {Component, EventEmitter, Input, OnInit, Output, Renderer2, SimpleChanges} from '@angular/core';
import {ServoyDefaultBaseComponent} from "../basecomponent";

@Component({
  selector: 'servoydefault-htmlarea',
  templateUrl: './htmlarea.html',
})
export class ServoyDefaultHtmlarea extends ServoyDefaultBaseComponent implements OnInit {
  lastServerValueAsSeenByTinyMCEContent;
  findMode = false;
  init = false;

  @Output() dataProviderIDChange = new EventEmitter();

  editor;
  @Input() editable;

  constructor(renderer: Renderer2) {
    super(renderer);
  }

  ngOnInit(){
    super.ngOnInit();
  }

  ngOnChanges(changes: SimpleChanges){
    super.ngOnChanges(changes);
    for ( let property in changes ) {
      let change = changes[property];
      switch (property) {
        case 'dataProviderID':
          if(this.editor) this.editor.setContent(change.currentValue);
          break;
      }
    }

  }

  setReadOnly(isEditable){
    isEditable ? this.editor.setMode('design') : this.editor.setMode('readonly');
  }

  ServoyTinyMCESettings = {
    menubar : false,
    statusbar : false,
    plugins: 'tabindex resizetocontainer',
    readonly: 1,
    skin_url: '../assets/tinymce/skins/lightgray',
    tabindex: 'element',
    toolbar: 'fontselect fontsizeselect | bold italic underline | superscript subscript | undo redo |alignleft aligncenter alignright alignjustify | styleselect | outdent indent bullist numlist',

    setup: editor => {

      editor.on('init', () => {
        this.init = true;
        this.editor = editor;
        this.setReadOnly(this.editable);
        if (!this.servoyApi.isInDesigner())
        {
          // see comment below where lastServerValueAsSeenByTinyMCEContent is set in dataProviderID watch
          this.editor.setContent(!this.servoyApi.isInDesigner() && this.dataProviderID ? this.dataProviderID : '' );
          this.lastServerValueAsSeenByTinyMCEContent = this.editor.getContent();
        }
      });

      editor.on('blur ExecCommand', () => {
        const edContent = editor.getContent();
        if (this.lastServerValueAsSeenByTinyMCEContent != edContent) {
          this.dataProviderID = '<html><body>' + edContent + '</body></html>';
          this.lastServerValueAsSeenByTinyMCEContent = edContent;
          this.dataProviderIDChange.emit(edContent);
        }
      });

      editor.on('click',function(e) {
        if (this.onActionMethodID)
        {
          this.onActionMethodID(createEvent(e));
        }
      });

      editor.on('focus',function(e) {
        if (this.mustExecuteOnFocusGainedMethod !== false && this.onFocusGainedMethodID)
        {
          this.onFocusGainedMethodID(createEvent(e));
        }
      });

      editor.on('blur',function(e) {
        if (this.onFocusLostMethodID)
        {
          this.onFocusLostMethodID(createEvent(e));
        }
      });

      let createEvent = function(e)
      {
        let ev = new MouseEvent(e.type);
        ev.initMouseEvent(e.type, e.bubbles, e.cancelable,null,e.detail, e.screenX, e.screenY, e.clientX, e.clientY, e.ctrlKey, e.altKey, e.shiftKey, e.metaKey, e.button, null);
        return ev;
      }
    }
  };

  /**
   * Sets the scroll location of an element. It takes as input the X (horizontal) and Y (vertical) coordinates - starting from the TOP LEFT side of the screen - only for an element where the height of the element is greater than the height of element content
   * NOTE: getScrollX() can be used with getScrollY() to return the current scroll location of an element; then use the X and Y coordinates with the setScroll function to set a new scroll location.
   * For Example:
   * //returns the X and Y coordinates
   * var x = forms.company.elements.myarea.getScrollX();
   * var y = forms.company.elements.myarea.getScrollY();
   * //sets the new location
   * forms.company.elements.myarea.setScroll(x+10,y+10);
   * @example
   * %%prefix%%%%elementName%%.setScroll(200,200);
   *
   * @param x the X coordinate of the htmlarea scroll location in pixels
   * @param y the Y coordinate of the htmlarea scroll location in pixels
   */

   setScroll(x, y) {
    this.editor.getWin().scrollTo(x,y);
  }

  /**
   * Returns the x scroll location of specified element - only for an element where height of element is less than the height of element content.
   * NOTE: getScrollX() can be used with getScrollY() to set the scroll location of an element using the setScroll function.
   * For Example:
   * //returns the X and Y scroll coordinates
   * var x = forms.company.elements.myarea.getScrollX();
   * var y = forms.company.elements.myarea.getScrollY();
   * //sets the new scroll location
   * forms.company.elements.myarea.setScroll(x+10,y+10);
   * @example
   * var x = %%prefix%%%%elementName%%.getScrollX();
   *
   * @return The x scroll location in pixels.
   */
   getScrollX() {
    return this.editor.getWin().scrollX;
  }

  /**
   * Returns the y scroll location of specified element - only for an element where height of element is less than the height of element content.
   * NOTE: getScrollY() can be used with getScrollX() to set the scroll location of an element using the setScroll function. For Example:
   * //returns the X and Y scroll coordinates
   * var x = forms.company.elements.myarea.getScrollX();
   * var y = forms.company.elements.myarea.getScrollY();
   * //sets the new scroll location
   * forms.company.elements.myarea.setScroll(x+10,y+10);
   * @example
   * var y = %%prefix%%%%elementName%%.getScrollY();
   * @return The y scroll location in pixels.
   */
  getScrollY = function() {
    return this.editor.scrollY;
  };

  /**
   * Replaces the selected text; if no text has been selected, the replaced value will be inserted at the last cursor position.
   * @example %%prefix%%%%elementName%%.replaceSelectedText('John');
   * @param s The replacement text.
   */
  replaceSelectedText(s){
    var selection = this.editor.selection;
    //useless 'getContent' call, do not remove though, setContent will not work if removed
    selection.getContent();
    selection.setContent(s);

    var edContent = this.editor.getContent();
    if (this.lastServerValueAsSeenByTinyMCEContent != edContent) {
      this.dataProviderID = '<html><body>' + edContent + '</body></html>';
      this.lastServerValueAsSeenByTinyMCEContent = edContent;
      this.dataProviderIDChange.emit(edContent);

    }
  }

  /**
   * Selects all the contents of the Html Area.
   * @example %%prefix%%%%elementName%%.selectAll();
   */
  selectAll() {
    this.editor.selection.select(this.editor.getBody(), true);
  }

  /**
   * Returns the currently selected text in the specified Html Area.
   * @example var my_text = %%prefix%%%%elementName%%.getSelectedText();
   * @return {String} The selected text in the Html Area.
   */
  getSelectedText() {
    return this.editor.selection.getContent();
  }

  /**
   * Gets the plain text for the formatted Html Area.
   * @example var my_text = %%prefix%%%%elementName%%.getAsPlainText();
   * @return the plain text
   */
  getAsPlainText() {
    return this.editor.getContent().replace(/<[^>]*>/g, '');
  }


}
