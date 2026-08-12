import { Component, input, ChangeDetectionStrategy, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FormService } from '../../ngclient/form.service';
import { ComponentCache } from '../../ngclient/types';

@Component({
  selector: 'svy-default-navigator',
  templateUrl: './default-navigator.html',
  styleUrls: ['./default-navigator.css'],
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [FormsModule]
})
export class DefaultNavigator {

  readonly name = input<string | null | undefined>(undefined);
  navigatorComponentCache!: any;
  sliderValue!: number;

  private readonly formservice = inject(FormService);

  constructor() {
  }

  ngOnInit() {
    this.navigatorComponentCache = this.formservice.getFormCacheByName( this.name()! ).getComponent('svy_default_navigator')! as ComponentCache;
    this.sliderValue = -(this.navigatorComponentCache.model as any).currentIndex;
  }

  setIndex(newIndex: any) {
    let i = parseInt(newIndex, 10);
    if (!i) {
      i = 1;
    }
    this.navigatorComponentCache.model.currentIndex = i;
    this.sliderValue = -i;
    this.formservice.executeEvent(this.name()!, this.navigatorComponentCache.name, 'setSelectedIndex', [i]);
  }
}
