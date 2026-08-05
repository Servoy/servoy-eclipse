import { Directive, HostListener, input, OnDestroy, inject } from '@angular/core';
import { TooltipService } from './tooltip.service';
import { Subscription } from 'rxjs';


/**
 * It's meant to not depend on ServoyPublicService, but rather to be directly configurable via an htmlTooltipInitialDelay and htmlTooltipDismissDelay attrs/inputs.
 * It is useful for usage in the palette part of the form designer - which is not a Servoy client.
 */
@Directive({
    selector: '[htmlTooltip]',
    standalone: false
})
export class HTMLTooltipDirective implements OnDestroy {

    readonly tooltipText = input<string | undefined>(undefined, { alias: 'htmlTooltip' });
    readonly tooltipInitialDelay = input<number | undefined>(undefined);
    readonly tooltipDismissDelay = input<number | undefined>(undefined);
    isActive = false;
    
    private unsubscribeIsTooltipActive: Subscription;
    protected tooltipService: TooltipService;

    constructor(tooltipService?: TooltipService) {
        this.tooltipService = tooltipService ?? inject(TooltipService);
        this.unsubscribeIsTooltipActive = this.tooltipService.isTooltipActive.subscribe(a => {
            this.isActive = a;
        });
    }

    @HostListener('pointerenter',['$event'])
    onMouseEnter(event:PointerEvent ): void {
        if (this.tooltipText()) {
            let initialDelay = this.getInitialDelay();
            if (initialDelay === null || isNaN(initialDelay)) initialDelay = 750;
            let dismissDelay = this.getDismissDelay();
            if (dismissDelay === null || isNaN(dismissDelay)) dismissDelay = 5000;
            this.tooltipService.showTooltip(event, this.tooltipText()!, initialDelay, dismissDelay);
        }
    }
    
    protected getInitialDelay(): number {
        return this.tooltipInitialDelay()!; 
    }

    protected getDismissDelay(): number {
        return this.tooltipDismissDelay()!; 
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
        this.unsubscribeIsTooltipActive.unsubscribe();
    }
}
