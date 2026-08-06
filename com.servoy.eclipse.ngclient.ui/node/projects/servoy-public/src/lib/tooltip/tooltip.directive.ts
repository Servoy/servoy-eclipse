import { Directive, HostListener, input, OnDestroy, inject } from '@angular/core';
import { TooltipService } from './tooltip.service';
import { HTMLTooltipDirective } from './tooltip-html.directive';
import { ServoyPublicService } from '../services/servoy_public.service';

@Directive({
    selector: '[svyTooltip]',
    standalone: false
})
export class TooltipDirective extends HTMLTooltipDirective {

    override readonly tooltipText = input<string | undefined>(undefined, { alias: 'svyTooltip' });

    private servoyService = inject(ServoyPublicService);

    protected getInitialDelay(): number {
        let initialDelay = super.getInitialDelay();
        if (initialDelay === null || isNaN(initialDelay)) initialDelay = this.servoyService.getUIProperty("tooltipInitialDelay");
        return initialDelay;
    }

    protected getDismissDelay(): number {
        let dismissDelay = super.getDismissDelay();
        if (dismissDelay === null || isNaN(dismissDelay)) dismissDelay = this.servoyService.getUIProperty("tooltipDismissDelay");
        return dismissDelay;
    }

}
