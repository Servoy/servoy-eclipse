import { Component, OnInit, Renderer2, Input, ViewChild, ElementRef, HostListener, SimpleChanges, AfterViewInit } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';
import { IValuelist } from '../../sablo/spectypes.service';
import { DatalistPolyFill } from './lib/purejs-datalist-polyfill/datalist.polyfill';
import { ShowDisplayValuePipe } from '../lib/showDisplayValue.pipe';

@Component({
  selector: 'servoybootstrap-list',
  templateUrl: './list.html',
  styleUrls: ['./list.scss']
})
export class ServoyBootstrapList extends ServoyBootstrapBasefield {

  @Input() valuelistID: IValuelist;
  @ViewChild('element') elementRef: ElementRef;
  polyFillFakeList = null;
  selectedValues: any;

  constructor(renderer: Renderer2,
     private datalistPolyfill: DatalistPolyFill,
     private showDisplayValuePipe: ShowDisplayValuePipe) {
    super(renderer);
  }

  ngAfterViewInit() {
    super.ngAfterViewInit();
    this.polyFillFakeList = this.datalistPolyfill.apply(this.elementRef.nativeElement);
  }

  ngOnChanges( changes: SimpleChanges ) {
    if (changes) {
      for ( const property of Object.keys(changes) ) {
          const change = changes[property];
          switch ( property ) {
              case 'dataProviderID':
                  if ( change.currentValue ) this.updateInput(change.currentValue);
                  break;
            }
        }
        super.ngOnChanges(changes);
    }
  }

  onKeydown(event) {
    /** 
      if($utils.testEnterKey(event)) {
         updateDataprovider();
		  }
     */
    this.updateDataprovider();
  }

  updateInput(listValue) {
    if (this.valuelistID) {
      listValue = this.showDisplayValuePipe.transform(listValue, this.valuelistID);
    }
    this.renderer.setProperty(this.elementRef, 'value', listValue);
  }

  updateDataprovider() {
    if(!this.isFakeListSelection()) {
      let listValue = this.elementRef.nativeElement.value;
      if (this.valuelistID) {
        for (let i = 0; i < this.valuelistID.length; i++) {
          let displayValue = this.valuelistID[i].displayValue;
          if (!displayValue || displayValue === '') {
            displayValue = ' ';
          }
          if (listValue === displayValue) {
            listValue = this.valuelistID[i].realValue;
            break;
          } 
        }
      }
      if (this.dataProviderID !== listValue) {
        this.update(listValue);
      } else {
        this.updateInput(listValue);
      }
    }
  }

  isFakeListSelection(): boolean {
    if (this.polyFillFakeList && (this.polyFillFakeList.style.display != 'none')) {
      const fakeItems = this.polyFillFakeList.childNodes;
      for (let i = 0; i < fakeItems.length; i++) {
        if (fakeItems[i].className == DatalistPolyFill.ACTIVE_CLASS) {
          return true;
        }
      }
    }
    return false;
  }
}
