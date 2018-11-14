import {Directive, HostListener, Input, OnDestroy} from "@angular/core";
import {TooltipService} from "./tooltip.service";
import {ServoyService} from "../servoy.service"

@Directive({ selector: '[svyTooltip]' })
export class TooltipDirective implements OnDestroy {

  @Input('svyTooltip') tooltipText: string;

  constructor(private tooltipService: TooltipService){
  }

  @HostListener('mouseover')
  onMouseEnter(event): void {
    if(this.tooltipText)  
        this.tooltipService.showTooltip(event, this.tooltipText,750, 5000);
  }

  @HostListener('mouseout')
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
