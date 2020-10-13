import { Component, OnInit, Renderer2, Input, ViewChild, ElementRef, HostListener, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';
import { IValuelist } from '../../sablo/spectypes.service';
import { ShowDisplayValuePipe } from '../lib/showDisplayValue.pipe';

@Component({
  selector: 'servoybootstrap-list',
  templateUrl: './list.html',
  styleUrls: ['./list.scss']
})
export class ServoyBootstrapList extends ServoyBootstrapBasefield {

  @Input() valuelistID: IValuelist;

  constructor(renderer: Renderer2,cdRef: ChangeDetectorRef,
     private showDisplayValuePipe: ShowDisplayValuePipe) {
    super(renderer, cdRef);
  }

  svyOnChanges( changes: SimpleChanges ) {
    if (changes) {
      for ( const property of Object.keys(changes) ) {
          const change = changes[property];
          switch ( property ) {
              case 'dataProviderID':
                  if ( change.currentValue ) this.updateInput(change.currentValue);
                  break;
            }
        }
        super.svyOnChanges(changes);
    }
  }

  updateInput(listValue) {
    if (this.valuelistID) {
      listValue = this.showDisplayValuePipe.transform(listValue, this.valuelistID);
    }
    this.renderer.setProperty(this.elementRef.nativeElement, 'value', listValue);
  }

  updateDataprovider() {
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
      }
  }
}
