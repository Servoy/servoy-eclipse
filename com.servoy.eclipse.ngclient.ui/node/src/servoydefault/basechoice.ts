import {ElementRef, EventEmitter, Input, OnInit, Renderer2, SimpleChanges, ViewChild} from "@angular/core";
import {FormattingService, PropertyUtils} from "../ngclient/servoy_public";
import {ServoyDefaultBaseField} from "./basefield";

export class ServoyDefaultBaseChoice extends  ServoyDefaultBaseField{
  
  selection: any[] = [];
  allowNullinc = 0;
  @ViewChild('input') labelInput: ElementRef<any>;
  @ViewChild('element') element: ElementRef<any>;

  @Input() requestFocus;

  constructor(renderer: Renderer2, formattingService: FormattingService){
    super(renderer, formattingService);
    PropertyUtils.getScrollbarsStyleObj(this.scrollbars);
  }
  
  onValuelistChange(){
      if(this.valuelistID)
          if(this.valuelistID.length > 0 && this.isValueListNull(this.valuelistID[0])) this.allowNullinc=1;
      if(this.labelInput)
          this.tabIndexChanged(this.labelInput.nativeElement.attr("tabindex"));
 };
 
  itemClicked(event, changed, dp){
    this.dataProviderID = dp;
    if (changed){
      this.update(dp);
    }
   this.blur(event);
  }
  
  focus(event){
      this.onFocusGainedMethodID ?  this.onFocusGainedMethodID(event):null;
  }
  blur(event){
      this.onFocusLostMethodID ? this.onFocusLostMethodID(event):null;
  }
  
  setInitialStyles(): void {
      if(this.size){      
          Object.keys(this.size).forEach(key => {
              this.element.nativeElement.style[key] = this.size[key]+'px';
          });
      }
  } 

  ngOnChanges(changes: SimpleChanges){
    super.ngOnChanges(changes);
  }

  tabIndexChanged(tabindex) {
    if(this.labelInput)
      this.labelInput.nativeElement.attr("tabindex",tabindex);
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
