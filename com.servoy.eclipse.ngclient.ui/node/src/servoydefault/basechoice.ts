import {AfterViewInit, ElementRef, OnInit, Renderer2, ViewChild} from "@angular/core";
import {FormattingService, PropertyUtils} from "../ngclient/servoy_public";
import {ServoyDefaultBaseField} from "./basefield";
import {forEach} from "@angular/router/src/utils/collection";

export class ServoyDefaultBaseChoice extends  ServoyDefaultBaseField implements OnInit, AfterViewInit{
  
  selection: any[] = [];
  allowNullinc = 0;

  constructor(renderer: Renderer2, formattingService: FormattingService){
    super(renderer, formattingService);
    PropertyUtils.getScrollbarsStyleObj(this.scrollbars);
    super.attachHandlers();
  }

  ngOnInit(){
    this.onValuelistChange();
    this.setInitialStyles();
  }

  ngAfterViewInit(){
      this.setHandlersAndTabIndex();
  }
  
  setHandlersAndTabIndex(){
      for(let i = 0; i < this.getNativeElement().children.length; i++){
          let elm:HTMLLabelElement = this.getNativeElement().children[i];
          this.attachEventHandlers(elm.children[0],i);
          this.tabIndexChanged(elm.children[0], this.tabSeq);
        }     
  }
  
  onValuelistChange(){
      if(this.valuelistID)
          if(this.valuelistID.length > 0 && this.isValueListNull(this.valuelistID[0])) this.allowNullinc=1;
        this.tabIndexChanged(this.getNativeElement(), this.tabSeq);
        this.setHandlersAndTabIndex();
  };
 
  baseItemClicked(event, changed, dp){
    if(event.target.localName == 'label' || event.target.localName == 'span'){
      event.preventDefault();
    }
    if (changed){
      this.update(dp);
    }
    event.target.blur();
  }

  attachEventHandlers(element, index){
    if(!element)
      element = this.getNativeElement();

    this.renderer.listen( element, 'blur', ( event ) => {
      if(this.onFocusLostMethodID) this.onFocusLostMethodID(event);
      this.valueBeforeChange = this.getNativeElement().value;
    });

    this.renderer.listen( element, 'contextmenu', ( e ) => {
      if(this.onRightClickMethodID) this.onRightClickMethodID(e);
    });
  }

  setInitialStyles(): void {
      if(this.size){      
          Object.keys(this.size).forEach(key => {
              this.getNativeElement().style[key] = this.size[key]+'px';
          });
      }
  } 

   tabIndexChanged(element, tabindex) {
       element.setAttribute("tabindex",tabindex);
   }

  isValueListNull = function(item) {
    return (item.realValue == null || item.realValue == '') && item.displayValue == '';
  };

  /**
   * Gets the selected values (real values from valuelist) as array. The form element should have a dataProviderID assigned in order for this to work.
   * @example var values = %%prefix%%%%elementName%%.getSelectedElements();
   * @return array with selected values
   */
  getSelectedElements()
  {
    return this.selection
      .filter(item => item === true)
      .map((item, index) =>  this.valuelistID[index+this.allowNullinc].realValue);
  }
}
