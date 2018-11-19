import {Component, SimpleChanges, Renderer2, ViewChild, ElementRef, Input, OnInit} from '@angular/core';
import {FormattingService} from "../../ngclient/servoy_public";
import {ServoyDefaultBaseChoice} from "../basechoice";

@Component({
  selector: 'servoydefault-checkgroup',
  templateUrl: './checkgroup.html',
  styleUrls: ['./checkgroup.css']
})
export class ServoyDefaultCheckGroup extends ServoyDefaultBaseChoice implements OnInit{

  constructor(renderer: Renderer2, formattingService: FormattingService) {
    super(renderer, formattingService);
  }
  ngOnInit(){
    this.setSelectionFromDataprovider();
    super.ngOnInit();
  }

  getDataproviderFromSelection() {
    let allowMultiselect = !this.format || this.format.type == "TEXT";
    let ret = allowMultiselect ? "" : null;
    this.selection.forEach((element, index) => {
      if(element == true)
        allowMultiselect ? ret+= this.valuelistID[index+this.allowNullinc].realValue+'\n': ret = this.valuelistID[index+this.allowNullinc].realValue
    });
    if (allowMultiselect) ret = ret.replace(/\n$/, "");//remove the last \n
    if(ret === "") ret = null;
    return ret;
  }

  setSelectionFromDataprovider() {
    this.selection = [];
    if (this.dataProviderID === null || this.dataProviderID === undefined) return;
    let arr = (typeof this.dataProviderID === "string") ? this.dataProviderID.split('\n') : [this.dataProviderID];
    arr.forEach( (element, index, array) => {
      for (let i = 0; i < this.valuelistID.length; i++) {
        let item = this.valuelistID[i];
        if (item.realValue + '' == element + '' &&!this.isValueListNull(item)) this.selection[i - this.allowNullinc] = true;
      }
    });
  }

  itemClicked(event, index) {
    let checkedTotal = this.selection.filter(a => a === true).length;
    let changed = true;
    if (event.target.checked) {
      if (!(!this.format || this.format.type == "TEXT") && checkedTotal > 1) {
        this.selection.map(() => false);
      }
      this.selection[index] = true;
    }
    else {
      event.target.checked = this.selection[index] = this.allowNullinc === 0 && checkedTotal <= 1 && !this.findmode;
      changed = !event.target.checked;
    }
    super.baseItemClicked(event,changed, this.getDataproviderFromSelection(),index);
  }

  attachEventHandlers(element, index){
    this.renderer.listen( element, 'click', ( event ) => {
      this.itemClicked(event,index);
      this.onActionMethodID( event );
    });
    super.attachEventHandlers(element,index);
  }
}

