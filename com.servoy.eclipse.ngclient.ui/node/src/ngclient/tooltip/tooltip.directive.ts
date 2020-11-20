import {Directive, HostListener, Input, OnDestroy} from '@angular/core';
import {TooltipService} from './tooltip.service';
import {ServoyService} from '../servoy.service';

@Directive({ selector: '[svyTooltip]' })
export class TooltipDirective implements OnDestroy {

  @Input('svyTooltip') tooltipText: string;
  isActive = false;

  constructor(private tooltipService: TooltipService){
    this.tooltipService.isTooltipActive.subscribe(a => {
      this.isActive = a;
    });
  }

  @HostListener('mouseenter')
  onMouseEnter(event): void {
    if(this.tooltipText)
        this.tooltipService.showTooltip(event, this.tooltipText, 750, 5000);
  }

  @HostListener('mouseleave')
  onMouseLeave(): void {
    this.tooltipService.hideTooltip();
  }

  @HostListener('click')
  onClick(): void {
    this.tooltipService.hideTooltip();
  }

  @HostListener('contextmenu')
  onContextMenu(): void {
    this.tooltipService.hideTooltip();
  }

  ngOnDestroy(): void {
    this.tooltipService.hideTooltip();
  }
}
