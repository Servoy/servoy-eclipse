import { Directive, HostListener, Input, OnDestroy } from '@angular/core';
import { TooltipService } from './tooltip.service';
import { ServoyPublicService } from '../services/servoy_public.service';

@Directive({
    selector: '[svyTooltip]',
    standalone: false
})
export class TooltipDirective implements OnDestroy {

    @Input('svyTooltip') tooltipText: string;
    isActive = false;

    constructor(private tooltipService: TooltipService, private servoyService: ServoyPublicService) {
        this.tooltipService.isTooltipActive.subscribe(a => {
            this.isActive = a;
        });
    }

    @HostListener('pointerenter')
    onMouseEnter(event): void {
        if (this.tooltipText) {
            let initialDelay = this.servoyService.getUIProperty("tooltipInitialDelay");
            if (initialDelay === null || isNaN(initialDelay)) initialDelay = 750;
            let dismissDelay = this.servoyService.getUIProperty("tooltipDismissDelay");
            if (dismissDelay === null || isNaN(dismissDelay)) dismissDelay = 5000;
            this.tooltipService.showTooltip(event, this.tooltipText, initialDelay, dismissDelay);
        }
    }

    @HostListener('pointerleave')
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
