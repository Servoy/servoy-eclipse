import {Renderer2, Component, OnInit, SimpleChanges} from '@angular/core';
import {FormattingService} from "../../ngclient/servoy_public";
import {ServoyDefaultBaseChoice} from "../basechoice";

@Component({
  selector: 'servoydefault-check',
  templateUrl: './check.html',
  styleUrls: ['./check.css']
})
export class ServoyDefaultCheck extends ServoyDefaultBaseChoice implements OnInit{

  selected:boolean = false;
  constructor(renderer: Renderer2, formattingService: FormattingService) {
    super(renderer, formattingService);
  }

  ngOnInit() {
      this.selected = this.getSelectionFromDataprovider();
      super.ngOnInit();
  }
  

  attachEventHandlers(element, index){
    this.renderer.listen( element, 'click', ( e ) => {
      this.itemClicked(e);
      if(this.onFocusLostMethodID) this.onFocusLostMethodID(e);
      if(this.onActionMethodID) this.onActionMethodID(e);
    });
    super.attachEventHandlers(element,index);
  }

  itemClicked(event) {
      if (this.valuelistID && this.valuelistID[0]) 
          this.dataProviderID = this.dataProviderID == this.valuelistID[0].realValue ? null : this.valuelistID[0].realValue;
      else if (typeof this.dataProviderID  === "string")
          this.dataProviderID = this.dataProviderID == "1" ? "0" : "1";
      else
          this.dataProviderID = this.dataProviderID > 0 ? 0 : 1;      
      super.baseItemClicked(event,true, this.dataProviderID,0);
  }
  
  getSelectionFromDataprovider() {
      if (!this.dataProviderID)
          return false;
      if (this.valuelistID && this.valuelistID[0]) {
          return this.dataProviderID == this.valuelistID[0].realValue;
      } else if (typeof this.dataProviderID  === "string") {
          return this.dataProviderID == "1";
      } else {
          return this.dataProviderID > 0;
      }
  }
  
  //TODO refactor after tableview impl
  isInPortal() {
      return false;
    //return !!this.element.nativeElement.closest('.svy-portal') && !this.element.nativeElement.closest('.svy-listviewwrapper');
   }
}
