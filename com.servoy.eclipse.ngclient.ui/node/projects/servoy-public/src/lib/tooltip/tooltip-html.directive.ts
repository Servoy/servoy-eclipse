import { Directive, HostListener, input, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TooltipService } from './tooltip.service';


/**
 * It's meant to not depend on ServoyPublicService, but rather to be directly configurable via an htmlTooltipInitialDelay and htmlTooltipDismissDelay attrs/inputs.
 * It is useful for usage in the palette part of the form designer - which is not a Servoy client.
 */
@Directive({
    selector: '[htmlTooltip]',
    standalone: true
})
export class HTMLTooltipDirective {

    readonly tooltipText = input<string | undefined>(undefined, { alias: 'htmlTooltip' });
    readonly tooltipInitialDelay = input<number | undefined>(undefined);
    readonly tooltipDismissDelay = input<number | undefined>(undefined);
    isActive = false;
    
    protected tooltipService = inject(TooltipService);

    constructor() {
        this.tooltipService.isTooltipActive.pipe(takeUntilDestroyed()).subscribe(a => {
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
}
