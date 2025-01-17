import { Component, Input } from '@angular/core';
import { FormService } from '../../ngclient/form.service';
import { ComponentCache } from '../../ngclient/types';

@Component({
    selector: 'svy-default-navigator',
    templateUrl: './default-navigator.html',
    styleUrls: ['./default-navigator.css'],
    standalone: false
})
export class DefaultNavigator {

  @Input() name: string;
  navigatorComponentCache: ComponentCache;
  sliderValue: number;

  constructor( private formservice: FormService ) {
  }

  ngOnInit() {
    this.navigatorComponentCache = this.formservice.getFormCacheByName( this.name ).getComponent('svy_default_navigator');
    this.sliderValue = -this.navigatorComponentCache.model.currentIndex;
  }

  setIndex(newIndex: any) {
    let i = parseInt(newIndex, 10);
    if (!i) {
      i = 1;
    }
    this.navigatorComponentCache.model.currentIndex = i;
    this.sliderValue = -i;
    this.formservice.executeEvent(this.name, this.navigatorComponentCache.name, 'setSelectedIndex', [i]);
  }
}
