import { Component, OnInit, Renderer2, Input, ViewChild, ElementRef, HostListener, SimpleChanges, ChangeDetectorRef, ChangeDetectionStrategy, Inject } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';
import { IValuelist } from '../../sablo/spectypes.service';
import { ShowDisplayValuePipe } from '../lib/showDisplayValue.pipe';
import { DOCUMENT } from '@angular/common';

@Component({
  selector: 'bootstrapcomponents-list',
  templateUrl: './list.html',
  styleUrls: ['./list.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyBootstrapList extends ServoyBootstrapBasefield<HTMLDivElement> {

  @Input() valuelistID: IValuelist;

  constructor(renderer: Renderer2, cdRef: ChangeDetectorRef,
     private showDisplayValuePipe: ShowDisplayValuePipe, @Inject(DOCUMENT) doc: Document) {
    super(renderer, cdRef, doc);
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
      let listValue = (this.elementRef.nativeElement as HTMLInputElement).value;
      if (this.valuelistID) {
          for (const i of Object.keys(this.valuelistID)) {
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
          this.updateValue(listValue);
      }
  }

  updateValue(val: string) {
    this.dataProviderID = val;
    super.pushUpdate();
  }
}
