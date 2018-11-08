import {Component, Input, OnInit, Renderer2} from '@angular/core';
import {ServoyDefaultBaseChoice} from "../basechoice";
import {FormattingService, PropertyUtils} from "../../ngclient/servoy_public";

@Component({
  selector: 'servoydefault-radiogroup',
  templateUrl: './radiogroup.html',
  styleUrls: ['./radiogroup.css']
})
export class ServoyDefaultRadiogroup extends ServoyDefaultBaseChoice implements OnInit {

  formattingSvc: any;
  value: any[];
  constructor(renderer: Renderer2, formattingService: FormattingService) {
    super(renderer, formattingService);
  }
  ngOnInit(){
    this.onValuelistChange();
    this.setSelectionFromDataprovider();
    this.setInitialStyles();
  }

  setSelectionFromDataprovider(){
    this.value = [this.dataProviderID];
    if (this.valuelistID)
    {
      for(var i=0;i < this.valuelistID.length;i++){
        var item= this.valuelistID[i];
        if((item.realValue+'') === (this.dataProviderID+''))
        {
          this.value = [item.realValue];
          break;
        }
      }
    }
  }

  itemClicked (event,val) {
    super.itemClicked(event, true, val);
  }

}
