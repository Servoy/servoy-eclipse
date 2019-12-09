import {AfterViewInit, OnInit, Renderer2, SimpleChanges} from "@angular/core";
import {FormattingService, PropertyUtils} from "../ngclient/servoy_public";
import {ServoyDefaultBaseField} from "./basefield";

export abstract class ServoyDefaultBaseChoice extends  ServoyDefaultBaseField implements OnInit, AfterViewInit{
  
  selection: any[] = [];
  allowNullinc = 0;
  
  constructor(renderer: Renderer2, formattingService: FormattingService){
    super(renderer, formattingService);
    PropertyUtils.getScrollbarsStyleObj(this.scrollbars);
    super.attachHandlers();
  }

  ngOnInit(){
    this.onValuelistChange();
  }

  ngAfterViewInit(){
      this.setHandlersAndTabIndex();
  }
  
  abstract setSelectionFromDataprovider();
  
  ngOnChanges( changes: SimpleChanges ) {
      for ( let property in changes ) {
          let change = changes[property];
          switch ( property ) {
              case "dataProviderID":
                  this.setSelectionFromDataprovider()
                  break;

          }
      }
      super.ngOnChanges(changes);
  }
  
  setHandlersAndTabIndex(){
      for(let i = 0; i < this.getNativeElement().children.length; i++){
          let elm:HTMLLabelElement = this.getNativeElement().children[i];
          this.attachEventHandlers(elm.children[0],i);
        }     
  }
  
  onValuelistChange(){
      if(this.valuelistID)
          if(this.valuelistID.length > 0 && this.isValueListNull(this.valuelistID[0])) this.allowNullinc=1;
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
      .map((item,index) =>  {
        if(item === true) return this.valuelistID[index+this.allowNullinc].realValue;})
      .filter(item => item !== null)
  }
  
}
