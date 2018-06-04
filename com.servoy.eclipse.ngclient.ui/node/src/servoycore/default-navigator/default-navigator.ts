import { Component, Input } from '@angular/core';
import { FormService, ComponentCache } from '../../ngclient/form.service';

@Component({
  selector: 'svy-default-navigator',
  templateUrl: './default-navigator.html',
  styleUrls: ['./default-navigator.css']
})
export class DefaultNavigator {

  @Input() readonly name;
  navigatorComponentCache: ComponentCache;
  sliderValue: number;

  constructor( private formservice: FormService ) {
  }

  ngOnInit() {
    this.navigatorComponentCache = this.formservice.getFormCacheByName( this.name ).getComponent('svy_default_navigator');
    this.sliderValue = -this.navigatorComponentCache.model.currentIndex;
  }

  setIndex(newIndex: string) {
    var i = parseInt(newIndex)
    if (!i) {
      i = 1;
    }
    this.navigatorComponentCache.model.currentIndex = i;
    this.sliderValue = -i;
    this.formservice.executeEvent(this.name, this.navigatorComponentCache.name, 'setSelectedIndex', [i]);
  }
}
